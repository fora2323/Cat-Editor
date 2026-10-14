package org.cat.editor.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Data model for a project opened via SAF or external storage.
 */
data class SafProjectRecord(
    val path: String,
    val uriString: String? = null,
    val name: String = File(path).name,
    val addedTime: Long = System.currentTimeMillis()
)

/**
 * Manages persistence and discovery of projects opened via SAF (Storage Access Framework)
 * or external locations outside the default CatStudioProjects directory.
 */
object SafProjectsManager {

    private const val PREFS_NAME = "saf_projects_prefs"
    private const val KEY_SAF_PROJECTS = "saved_saf_projects"

    private fun normalizePath(rawPath: String): String {
        return try {
            File(rawPath).canonicalPath
        } catch (_: Exception) {
            File(rawPath).absolutePath
        }
    }

    /**
     * Adds or updates a SAF project record in persistent storage.
     */
    fun addProject(context: Context, path: String, uriString: String? = null) {
        if (path.isBlank()) return
        val normalized = normalizePath(path)
        val file = File(normalized)
        if (!file.exists() || !file.isDirectory) return

        val currentList = getStoredProjects(context).toMutableList()
        val existingIndex = currentList.indexOfFirst { normalizePath(it.path) == normalized }

        val newRecord = SafProjectRecord(
            path = normalized,
            uriString = uriString ?: if (existingIndex != -1) currentList[existingIndex].uriString else null,
            name = file.name.ifBlank { "Project" },
            addedTime = System.currentTimeMillis()
        )

        if (existingIndex != -1) {
            currentList[existingIndex] = newRecord
        } else {
            currentList.add(0, newRecord)
        }

        saveStoredProjects(context, currentList)
    }

    /**
     * Removes a SAF project record by path and revokes persisted URI permission if available.
     */
    fun removeProject(context: Context, path: String) {
        if (path.isBlank()) return
        val normalized = normalizePath(path)
        val currentList = getStoredProjects(context).toMutableList()
        val toRemove = currentList.filter { normalizePath(it.path) == normalized }

        toRemove.forEach { record ->
            record.uriString?.let { uriStr ->
                try {
                    context.contentResolver.releasePersistableUriPermission(
                        Uri.parse(uriStr),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (_: Exception) {}
            }
        }

        currentList.removeAll { normalizePath(it.path) == normalized }
        saveStoredProjects(context, currentList)
    }

    /**
     * Retrieves all valid SAF projects from:
     * 1. Saved preferences
     * 2. System persisted URI permissions
     * 3. Last opened project in app_data (if external)
     */
    fun getAllSafProjects(context: Context): List<SafProjectRecord> {
        val stored = getStoredProjects(context).toMutableList()
        val seenPaths = stored.map { normalizePath(it.path) }.toMutableSet()
        var updated = false

        // 1. Recover from system persisted URI permissions
        try {
            val persistedPerms = context.contentResolver.persistedUriPermissions
            for (perm in persistedPerms) {
                if (perm.isReadPermission) {
                    val resolvedPath = StorageUtils.getRealPathFromTreeUri(context, perm.uri)
                    if (!resolvedPath.isNullOrBlank()) {
                        val normalized = normalizePath(resolvedPath)
                        val file = File(normalized)
                        if (file.exists() && file.isDirectory && seenPaths.add(normalized)) {
                            stored.add(
                                SafProjectRecord(
                                    path = normalized,
                                    uriString = perm.uri.toString(),
                                    name = file.name.ifBlank { "Project" },
                                    addedTime = perm.persistedTime
                                )
                            )
                            updated = true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Recover from last_project in app_data if external
        try {
            val appDataPrefs = context.getSharedPreferences("app_data", Context.MODE_PRIVATE)
            val lastProject = appDataPrefs.getString("last_project", "") ?: ""
            if (lastProject.isNotBlank()) {
                val normalized = normalizePath(lastProject)
                val file = File(normalized)
                val defaultPrefix = normalizePath(ConfigPath.VAR_PROJECTS_PATH)
                if (file.exists() && file.isDirectory && !normalized.startsWith(defaultPrefix) && seenPaths.add(normalized)) {
                    stored.add(
                        SafProjectRecord(
                            path = normalized,
                            uriString = null,
                            name = file.name.ifBlank { "Project" },
                            addedTime = System.currentTimeMillis()
                        )
                    )
                    updated = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Filter out any non-existent directories
        val validList = stored.filter {
            val f = File(it.path)
            f.exists() && f.isDirectory
        }

        if (updated || validList.size != stored.size) {
            saveStoredProjects(context, validList)
        }

        return validList
    }

    private fun getStoredProjects(context: Context): List<SafProjectRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_SAF_PROJECTS, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<SafProjectRecord>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val path = obj.getString("path")
                list.add(
                    SafProjectRecord(
                        path = path,
                        uriString = obj.optString("uriString").takeIf { !it.isNullOrBlank() },
                        name = obj.optString("name", File(path).name),
                        addedTime = obj.optLong("addedTime", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveStoredProjects(context: Context, list: List<SafProjectRecord>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("path", item.path)
                put("uriString", item.uriString ?: "")
                put("name", item.name)
                put("addedTime", item.addedTime)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_SAF_PROJECTS, array.toString()).apply()
    }
}
