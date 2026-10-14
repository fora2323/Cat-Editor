package org.cat.editor.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.core.content.ContextCompat
import java.io.File
import java.net.URLDecoder

object StorageUtils {

    /**
     * Converts a SAF tree URI or document URI to an absolute filesystem path.
     * Persists URI read/write permissions where possible.
     */
    fun getRealPathFromTreeUri(context: Context, uri: Uri): String? {
        try {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {
                // Ignore if already taken or not supported
            }

            // Case 1: Tree URI (ActivityResultContracts.OpenDocumentTree)
            if (DocumentsContract.isTreeUri(uri)) {
                val treeDocId = DocumentsContract.getTreeDocumentId(uri)
                val resolved = resolveFromDocId(context, treeDocId)
                if (resolved != null) return resolved
            }

            // Case 2: Standard document URI
            if (DocumentsContract.isDocumentUri(context, uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val resolved = resolveFromDocId(context, docId)
                if (resolved != null) return resolved
            }

            // Case 3: Parse from uri.path / encoded path
            val rawPath = uri.path
            if (rawPath != null) {
                val resolved = resolveFromRawPath(context, rawPath)
                if (resolved != null) return resolved
            }

            // Case 4: File scheme
            if (uri.scheme == "file") {
                return uri.path
            }

            // Case 5: Query content resolver _data column
            try {
                val proj = arrayOf(android.provider.MediaStore.MediaColumns.DATA)
                context.contentResolver.query(uri, proj, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                        if (idx != -1) {
                            val dataPath = cursor.getString(idx)
                            if (!dataPath.isNullOrBlank()) return dataPath
                        }
                    }
                }
            } catch (_: Exception) {}
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun resolveFromDocId(context: Context, docId: String): String? {
        val decoded = try {
            URLDecoder.decode(docId, "UTF-8")
        } catch (_: Exception) {
            docId
        }

        // Check for downloads provider root
        if (decoded.equals("downloads", ignoreCase = true)) {
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
        }
        if (decoded.startsWith("downloads/", ignoreCase = true)) {
            val sub = decoded.substringAfter("downloads/").trimStart('/')
            return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), sub).absolutePath
        }

        if (decoded.startsWith("primary:", ignoreCase = true)) {
            val rel = decoded.substringAfter("primary:").trimStart('/')
            val base = Environment.getExternalStorageDirectory().absolutePath
            val file = if (rel.isEmpty()) File(base) else File(base, rel)
            return file.absolutePath
        }

        if (decoded.startsWith("raw:", ignoreCase = true)) {
            val raw = decoded.substringAfter("raw:")
            return File(raw).absolutePath
        }

        if (decoded.startsWith("home:", ignoreCase = true)) {
            val rel = decoded.substringAfter("home:").trimStart('/')
            val base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath
            val file = if (rel.isEmpty()) File(base) else File(base, rel)
            return file.absolutePath
        }

        if (decoded.contains(":")) {
            val parts = decoded.split(":", limit = 2)
            val storageId = parts[0]
            val subPath = if (parts.size > 1) parts[1].trimStart('/') else ""
            val candidate = File("/storage/$storageId", subPath)
            if (candidate.exists()) {
                return candidate.absolutePath
            }
            val candidateMnt = File("/mnt/media_rw/$storageId", subPath)
            if (candidateMnt.exists()) {
                return candidateMnt.absolutePath
            }

            // Find matching volume from external files dirs
            val extDirs = ContextCompat.getExternalFilesDirs(context, null)
            for (dir in extDirs) {
                if (dir != null) {
                    val p = dir.absolutePath
                    if (p.contains(storageId)) {
                        val root = p.substringBefore("/Android")
                        val file = File(root, subPath)
                        return file.absolutePath
                    }
                }
            }
            return candidate.absolutePath
        }

        return null
    }

    private fun resolveFromRawPath(context: Context, rawPath: String): String? {
        val decoded = try {
            URLDecoder.decode(rawPath, "UTF-8")
        } catch (_: Exception) {
            rawPath
        }

        if (decoded.contains("primary:", ignoreCase = true)) {
            val rel = decoded.substringAfter("primary:").trimStart('/')
            val base = Environment.getExternalStorageDirectory().absolutePath
            val file = if (rel.isEmpty()) File(base) else File(base, rel)
            return file.absolutePath
        }

        if (decoded.contains("raw:", ignoreCase = true)) {
            val rel = decoded.substringAfter("raw:")
            return File(rel).absolutePath
        }

        if (decoded.startsWith("/storage/")) {
            return File(decoded).absolutePath
        }

        // Handle /tree/XXXX-XXXX:path or /document/XXXX-XXXX:path
        val colonIndex = decoded.lastIndexOf(':')
        if (colonIndex != -1) {
            val subPath = decoded.substring(colonIndex + 1).trimStart('/')
            val prefix = decoded.substring(0, colonIndex)
            val storageId = prefix.substringAfterLast('/').substringAfterLast('%')
            if (storageId.isNotBlank()) {
                val candidate = File("/storage/$storageId", subPath)
                if (candidate.exists()) {
                    return candidate.absolutePath
                }
                val extDirs = ContextCompat.getExternalFilesDirs(context, null)
                for (dir in extDirs) {
                    if (dir != null && dir.absolutePath.contains(storageId)) {
                        val root = dir.absolutePath.substringBefore("/Android")
                        return File(root, subPath).absolutePath
                    }
                }
            }
        }

        return null
    }
}
