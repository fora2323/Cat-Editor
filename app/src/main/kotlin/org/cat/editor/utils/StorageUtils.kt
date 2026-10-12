package org.cat.editor.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
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
            } catch (e: Exception) {
                // Ignore if already taken or not supported
            }

            // Case 1: Tree URI (ActivityResultContracts.OpenDocumentTree)
            if (DocumentsContract.isTreeUri(uri)) {
                val treeDocId = DocumentsContract.getTreeDocumentId(uri)
                val resolved = resolveFromDocId(treeDocId)
                if (resolved != null) return resolved
            }

            // Case 2: Standard document URI
            if (DocumentsContract.isDocumentUri(context, uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val resolved = resolveFromDocId(docId)
                if (resolved != null) return resolved
            }

            // Case 3: Parse from uri.path / encoded path
            val rawPath = uri.path
            if (rawPath != null) {
                val resolved = resolveFromRawPath(rawPath)
                if (resolved != null) return resolved
            }

            // Case 4: File scheme
            if (uri.scheme == "file") {
                return uri.path
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun resolveFromDocId(docId: String): String? {
        val decoded = try {
            URLDecoder.decode(docId, "UTF-8")
        } catch (e: Exception) {
            docId
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

        if (decoded.contains(":")) {
            val parts = decoded.split(":", limit = 2)
            val storageId = parts[0]
            val subPath = if (parts.size > 1) parts[1].trimStart('/') else ""
            val candidate = File("/storage/$storageId", subPath)
            if (candidate.exists()) {
                return candidate.absolutePath
            }
        }

        return null
    }

    private fun resolveFromRawPath(rawPath: String): String? {
        val decoded = try {
            URLDecoder.decode(rawPath, "UTF-8")
        } catch (e: Exception) {
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

        return null
    }
}
