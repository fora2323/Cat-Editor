package org.cat.editor.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.api.RebaseResult
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.PushResult
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import org.eclipse.jgit.util.io.NullOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Date

/**
 * Robust, production-grade Git helper powered by JGit.
 * Handles branch management, status detection, commit/amend, push/pull/fetch,
 * and commit history with complete thread safety.
 */
object GitManager {

    enum class ChangeType {
        MODIFIED,
        ADDED,
        DELETED,
        UNTRACKED,
        MISSING,
        CONFLICTING
    }

    data class GitFileItem(
        val path: String,
        val fileName: String,
        val dirPath: String,
        val type: ChangeType,
        val isUnversioned: Boolean
    )

    data class GitStatusData(
        val currentBranch: String,
        val changes: List<GitFileItem>,      // Tracked changes (Modified, Deleted, Added)
        val unversioned: List<GitFileItem>,  // Untracked new files
        val totalCount: Int
    )

    data class BranchInfo(
        val current: String,
        val localBranches: List<String>,
        val remoteBranches: List<String>
    )

    data class GitCommitItem(
        val hash: String,
        val shortHash: String,
        val authorName: String,
        val authorEmail: String,
        val date: Date,
        val message: String,
        val fullMessage: String
    )

    fun isGitRepo(projectDir: File): Boolean {
        if (!projectDir.exists() || !projectDir.isDirectory) return false
        val gitDir = File(projectDir, ".git")
        return gitDir.exists()
    }

    private fun openGit(projectDir: File): Git {
        val gitDir = File(projectDir, ".git")
        if (!gitDir.exists()) {
            throw IllegalStateException("Not a git repository: ${projectDir.absolutePath}")
        }
        return Git.open(projectDir)
    }

    private fun getCredentialsProvider(context: Context): UsernamePasswordCredentialsProvider? {
        val username = SettingsManager.getGitUsername(context).trim()
        val token = SettingsManager.getGitToken(context).trim()
        if (username.isBlank() && token.isBlank()) return null

        return if (token.contains(":")) {
            val parts = token.split(":", limit = 2)
            UsernamePasswordCredentialsProvider(parts[0], parts[1])
        } else if (username.isNotBlank()) {
            UsernamePasswordCredentialsProvider(username, token)
        } else {
            UsernamePasswordCredentialsProvider(token, "")
        }
    }

    suspend fun getBranchInfo(projectDir: File): Result<BranchInfo> = withContext(Dispatchers.IO) {
        runCatching {
            openGit(projectDir).use { git ->
                val repo = git.repository
                val current = repo.branch ?: "HEAD"

                val local = git.branchList().call().map { ref ->
                    ref.name.removePrefix("refs/heads/")
                }.filter { it.isNotBlank() }

                val remote = git.branchList()
                    .setListMode(ListBranchCommand.ListMode.REMOTE)
                    .call()
                    .map { ref ->
                        ref.name.removePrefix("refs/remotes/")
                    }
                    .filter { !it.endsWith("/HEAD") }

                BranchInfo(
                    current = current,
                    localBranches = if (local.isEmpty()) listOf(current) else local,
                    remoteBranches = remote
                )
            }
        }
    }

    suspend fun checkoutBranch(projectDir: File, branchName: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            openGit(projectDir).use { git ->
                val cleanName = branchName.removePrefix("refs/heads/").removePrefix("refs/remotes/")
                val isRemote = branchName.startsWith("origin/") || cleanName.contains('/')

                val cmd = git.checkout().setName(cleanName)
                if (isRemote) {
                    val localName = cleanName.substringAfterLast('/')
                    cmd.setCreateBranch(true)
                        .setName(localName)
                        .setStartPoint(cleanName)
                }
                cmd.call()
                "Switched to branch '$cleanName'"
            }
        }
    }

    suspend fun createAndCheckoutBranch(
        projectDir: File,
        newBranchName: String,
        baseBranch: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            openGit(projectDir).use { git ->
                val cleanNew = newBranchName.trim()
                if (cleanNew.isBlank()) throw IllegalArgumentException("Branch name cannot be empty")

                val createCmd = git.branchCreate().setName(cleanNew)
                if (!baseBranch.isNullOrBlank()) {
                    createCmd.setStartPoint(baseBranch)
                }
                createCmd.call()

                git.checkout().setName(cleanNew).call()
                "Created and checked out '$cleanNew'"
            }
        }
    }

    suspend fun renameBranch(
        projectDir: File,
        oldName: String,
        newName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            openGit(projectDir).use { git ->
                val cleanNew = newName.trim()
                if (cleanNew.isBlank()) throw IllegalArgumentException("New branch name cannot be empty")
                git.branchRename().setOldName(oldName).setNewName(cleanNew).call()
                "Renamed '$oldName' to '$cleanNew'"
            }
        }
    }

    suspend fun deleteBranch(
        projectDir: File,
        branchName: String,
        force: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            openGit(projectDir).use { git ->
                val current = git.repository.branch
                if (current == branchName) {
                    throw IllegalStateException("Cannot delete the currently checked out branch '$branchName'")
                }
                git.branchDelete().setBranchNames(branchName).setForce(force).call()
                "Deleted branch '$branchName'"
            }
        }
    }

    suspend fun mergeBranch(projectDir: File, branchToMerge: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            openGit(projectDir).use { git ->
                val repo = git.repository
                val ref = repo.findRef(branchToMerge)
                    ?: repo.findRef("refs/heads/$branchToMerge")
                    ?: repo.findRef("refs/remotes/$branchToMerge")
                    ?: throw IllegalArgumentException("Ref not found for '$branchToMerge'")

                val res = git.merge().include(ref).call()
                when (res.mergeStatus) {
                    MergeResult.MergeStatus.FAST_FORWARD -> "Fast-forward merge with '$branchToMerge' completed"
                    MergeResult.MergeStatus.ALREADY_UP_TO_DATE -> "Already up to date with '$branchToMerge'"
                    MergeResult.MergeStatus.MERGED -> "Merged '$branchToMerge' successfully"
                    MergeResult.MergeStatus.CONFLICTING -> "Merge conflicts detected: ${res.conflicts?.keys?.joinToString()}"
                    else -> "Merge status: ${res.mergeStatus}"
                }
            }
        }
    }

    suspend fun rebaseBranch(projectDir: File, upstreamBranch: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            openGit(projectDir).use { git ->
                val res = git.rebase().setUpstream(upstreamBranch).call()
                when (res.status) {
                    RebaseResult.Status.OK -> "Rebased onto '$upstreamBranch' successfully"
                    RebaseResult.Status.UP_TO_DATE -> "Already up to date with '$upstreamBranch'"
                    RebaseResult.Status.FAST_FORWARD -> "Fast-forward rebase onto '$upstreamBranch'"
                    RebaseResult.Status.STOPPED -> "Rebase paused for conflict resolution"
                    else -> "Rebase status: ${res.status}"
                }
            }
        }
    }

    suspend fun getStatus(projectDir: File): Result<GitStatusData> = withContext(Dispatchers.IO) {
        runCatching {
            openGit(projectDir).use { git ->
                val branch = git.repository.branch ?: "HEAD"
                val status = git.status().call()

                val changesList = mutableListOf<GitFileItem>()
                val unversionedList = mutableListOf<GitFileItem>()

                fun createItem(path: String, type: ChangeType, isUnversioned: Boolean): GitFileItem {
                    val f = File(path)
                    val parent = f.parent?.replace('\\', '/') ?: ""
                    return GitFileItem(
                        path = path,
                        fileName = f.name,
                        dirPath = parent,
                        type = type,
                        isUnversioned = isUnversioned
                    )
                }

                // Added / Staged
                for (p in status.added) {
                    changesList.add(createItem(p, ChangeType.ADDED, false))
                }
                // Modified (staged or unstaged)
                val modifiedPaths = (status.modified + status.changed).distinct()
                for (p in modifiedPaths) {
                    if (changesList.none { it.path == p }) {
                        changesList.add(createItem(p, ChangeType.MODIFIED, false))
                    }
                }
                // Removed / Missing
                val deletedPaths = (status.removed + status.missing).distinct()
                for (p in deletedPaths) {
                    if (changesList.none { it.path == p }) {
                        changesList.add(createItem(p, ChangeType.DELETED, false))
                    }
                }
                // Conflicting
                for (p in status.conflicting) {
                    if (changesList.none { it.path == p }) {
                        changesList.add(createItem(p, ChangeType.CONFLICTING, false))
                    }
                }

                // Untracked / Unversioned files
                for (p in status.untracked) {
                    unversionedList.add(createItem(p, ChangeType.UNTRACKED, true))
                }

                changesList.sortBy { it.path }
                unversionedList.sortBy { it.path }

                GitStatusData(
                    currentBranch = branch,
                    changes = changesList,
                    unversioned = unversionedList,
                    totalCount = changesList.size + unversionedList.size
                )
            }
        }
    }

    suspend fun getLastCommitMessage(projectDir: File): String? = withContext(Dispatchers.IO) {
        try {
            openGit(projectDir).use { git ->
                val repo = git.repository
                val head = repo.resolve(Constants.HEAD) ?: return@withContext null
                RevWalk(repo).use { walk ->
                    val commit = walk.parseCommit(head)
                    commit.fullMessage
                }
            }
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun commit(
        projectDir: File,
        message: String,
        selectedFiles: List<String>,
        amend: Boolean,
        context: Context
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (message.isBlank()) {
                throw IllegalArgumentException("Commit message cannot be empty")
            }
            if (selectedFiles.isEmpty() && !amend) {
                throw IllegalArgumentException("No files selected for commit")
            }

            openGit(projectDir).use { git ->
                // Stage selected files
                if (selectedFiles.isNotEmpty()) {
                    for (filePath in selectedFiles) {
                        val file = File(projectDir, filePath)
                        if (file.exists()) {
                            git.add().addFilepattern(filePath).call()
                        } else {
                            try {
                                git.rm().addFilepattern(filePath).call()
                            } catch (_: Throwable) {
                                git.add().addFilepattern(filePath).setUpdate(true).call()
                            }
                        }
                    }
                }

                // Configure author
                val authorName = SettingsManager.getGitAuthorName(context).trim()
                val authorEmail = SettingsManager.getGitAuthorEmail(context).trim()

                val commitCmd = git.commit()
                    .setMessage(message)
                    .setAmend(amend)

                if (authorName.isNotBlank() && authorEmail.isNotBlank()) {
                    val ident = PersonIdent(authorName, authorEmail)
                    commitCmd.setAuthor(ident).setCommitter(ident)
                }

                val rev = commitCmd.call()
                "Committed [${rev.name.take(7)}]: ${rev.shortMessage}"
            }
        }
    }

    suspend fun push(projectDir: File, context: Context): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            openGit(projectDir).use { git ->
                val pushCmd = git.push()
                val credentials = getCredentialsProvider(context)
                if (credentials != null) {
                    pushCmd.setCredentialsProvider(credentials)
                }

                val results = pushCmd.call()
                val messages = mutableListOf<String>()

                for (res in results) {
                    for (update in res.remoteUpdates) {
                        when (update.status) {
                            RemoteRefUpdate.Status.OK -> messages.add("Pushed ${update.srcRef} -> ${update.remoteName}")
                            RemoteRefUpdate.Status.UP_TO_DATE -> messages.add("Remote branch already up to date")
                            RemoteRefUpdate.Status.NON_EXISTING -> messages.add("Remote ref doesn't exist")
                            RemoteRefUpdate.Status.REJECTED_NODELETE -> messages.add("Rejected: cannot delete remote")
                            RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD -> messages.add("Rejected: Non-fast-forward. Pull changes first.")
                            RemoteRefUpdate.Status.REJECTED_REMOTE_CHANGED -> messages.add("Rejected: remote changed concurrently")
                            else -> messages.add("Push status: ${update.status}")
                        }
                    }
                }

                if (messages.isEmpty()) "Push completed" else messages.joinToString("\n")
            }
        }
    }

    suspend fun pull(projectDir: File, context: Context): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            openGit(projectDir).use { git ->
                val pullCmd = git.pull()
                val credentials = getCredentialsProvider(context)
                if (credentials != null) {
                    pullCmd.setCredentialsProvider(credentials)
                }

                val res = pullCmd.call()
                if (res.isSuccessful) {
                    val mergeResult = res.mergeResult
                    if (mergeResult != null) {
                        "Pull successful: ${mergeResult.mergeStatus}"
                    } else {
                        "Pull completed successfully"
                    }
                } else {
                    "Pull finished: ${res.mergeResult?.mergeStatus ?: "Check remote status"}"
                }
            }
        }
    }

    suspend fun fetch(projectDir: File, context: Context): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            openGit(projectDir).use { git ->
                val fetchCmd = git.fetch()
                val credentials = getCredentialsProvider(context)
                if (credentials != null) {
                    fetchCmd.setCredentialsProvider(credentials)
                }

                val res = fetchCmd.call()
                val updates = res.trackingRefUpdates
                if (updates.isNotEmpty()) {
                    "Fetched updates for ${updates.size} branch(es)"
                } else {
                    "Fetch completed: repository up to date"
                }
            }
        }
    }

    suspend fun getCommitHistory(
        projectDir: File,
        maxCount: Int = 100
    ): Result<List<GitCommitItem>> = withContext(Dispatchers.IO) {
        runCatching {
            openGit(projectDir).use { git ->
                val list = mutableListOf<GitCommitItem>()
                try {
                    val commits = git.log().setMaxCount(maxCount).call()
                    for (c in commits) {
                        val ident = c.authorIdent
                        list.add(
                            GitCommitItem(
                                hash = c.name,
                                shortHash = c.name.take(7),
                                authorName = ident?.name ?: "Unknown",
                                authorEmail = ident?.emailAddress ?: "",
                                date = ident?.getWhen() ?: Date(c.commitTime * 1000L),
                                message = c.shortMessage ?: "",
                                fullMessage = c.fullMessage ?: ""
                            )
                        )
                    }
                } catch (_: Throwable) {
                    // In empty repos with no commits yet
                }
                list
            }
        }
    }

    suspend fun getCommitChangedFiles(
        projectDir: File,
        commitHash: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            openGit(projectDir).use { git ->
                val repo = git.repository
                val commitId = repo.resolve(commitHash) ?: return@use emptyList<String>()
                RevWalk(repo).use { walk ->
                    val commit = walk.parseCommit(commitId)
                    val out = mutableListOf<String>()

                    if (commit.parentCount > 0) {
                        val parent = walk.parseCommit(commit.getParent(0))
                        val oldTree = CanonicalTreeParser().apply {
                            repo.newObjectReader().use { reader ->
                                reset(reader, parent.tree)
                            }
                        }
                        val newTree = CanonicalTreeParser().apply {
                            repo.newObjectReader().use { reader ->
                                reset(reader, commit.tree)
                            }
                        }

                        DiffFormatter(NullOutputStream.INSTANCE).use { df ->
                            df.setRepository(repo)
                            val diffs = df.scan(oldTree, newTree)
                            for (entry in diffs) {
                                out.add(entry.newPath.takeIf { it != "/dev/null" } ?: entry.oldPath)
                            }
                        }
                    } else {
                        // First commit
                        val newTree = CanonicalTreeParser().apply {
                            repo.newObjectReader().use { reader ->
                                reset(reader, commit.tree)
                            }
                        }
                        DiffFormatter(NullOutputStream.INSTANCE).use { df ->
                            df.setRepository(repo)
                            val diffs = df.scan(EmptyTreeIterator(), newTree)
                            for (entry in diffs) {
                                out.add(entry.newPath)
                            }
                        }
                    }
                    out
                }
            }
        }
    }
}
