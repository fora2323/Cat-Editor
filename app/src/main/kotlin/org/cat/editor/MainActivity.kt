package org.cat.editor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.FileObserver
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.cat.editor.ui.IdeMainScreen
import org.cat.editor.ui.theme.AppTheme
import org.cat.editor.ui.preview.ComposeNode
import org.cat.editor.utility.IdComposeParser
import org.cat.editor.utils.LocaleHelper
import org.cat.editor.utils.StorageUtils
import java.io.File

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val intentPath = intent.getStringExtra("project_path")
        if (!intentPath.isNullOrEmpty()) {
            projectPath = intentPath
            getSharedPreferences("app_data", Context.MODE_PRIVATE)
                .edit().putString("last_project", intentPath).apply()
            showProjectPicker = false
            startAppLogic()
        }
    }

    private var fileObserver: FileObserver? = null

    private var currentRootNode by mutableStateOf<ComposeNode>(ComposeNode("Root"))
    private var projectPath by mutableStateOf("")
    private var activeFilePath by mutableStateOf("")
    private var openedFiles by mutableStateOf<List<String>>(emptyList())
    private var showProjectPicker by mutableStateOf(false)

    private fun getProjectTabsKey(path: String): String {
        return "opened_files_" + path.hashCode()
    }

    private fun getProjectActiveFileKey(path: String): String {
        return "active_file_" + path.hashCode()
    }

    private fun loadProjectTabs(path: String) {
        if (path.isBlank() || !File(path).exists()) {
            openedFiles = emptyList()
            activeFilePath = ""
            currentRootNode = ComposeNode("Empty")
            return
        }

        val prefs = getSharedPreferences("app_data", Context.MODE_PRIVATE)
        val savedTabs = prefs.getString(getProjectTabsKey(path), "") ?: ""
        val rawList = if (savedTabs.isNotEmpty()) savedTabs.split(",").filter { it.isNotBlank() } else emptyList()

        val projectDir = try { File(path).canonicalFile } catch (_: Throwable) { File(path) }
        val validTabs = rawList.filter { filePath ->
            try {
                val f = File(filePath)
                f.exists() && f.canonicalFile.startsWith(projectDir)
            } catch (_: Throwable) {
                false
            }
        }

        openedFiles = validTabs

        val savedActive = prefs.getString(getProjectActiveFileKey(path), "") ?: ""
        activeFilePath = if (savedActive.isNotEmpty() && validTabs.contains(savedActive) && File(savedActive).exists()) {
            savedActive
        } else {
            validTabs.lastOrNull() ?: ""
        }

        if (activeFilePath.isNotEmpty()) {
            setupPreview(activeFilePath)
        } else {
            currentRootNode = ComposeNode("Empty")
        }
    }

    private fun switchProject(newPath: String) {
        if (newPath == projectPath && openedFiles.isNotEmpty()) return
        fileObserver?.stopWatching()
        projectPath = newPath
        getSharedPreferences("app_data", Context.MODE_PRIVATE)
            .edit()
            .putString("last_project", newPath)
            .remove("opened_files")
            .remove("last_preview")
            .apply()
        showProjectPicker = false
        loadProjectTabs(newPath)
    }

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val path = getRealPathFromURI(it)
            if (path != null) {
                switchProject(path)
                startAppLogic()
            } else {
                Toast.makeText(this, getString(R.string.cannot_access_folder), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        org.cat.editor.utils.SettingsManager.applyFullscreen(this)

        val prefs = getSharedPreferences("app_data", Context.MODE_PRIVATE)
        val savedProject = prefs.getString("last_project", "") ?: ""
        val intentPath = intent.getStringExtra("project_path")
        val initialProject = if (!intentPath.isNullOrEmpty()) intentPath else savedProject

        projectPath = initialProject
        if (initialProject.isNotEmpty()) {
            prefs.edit().putString("last_project", initialProject).apply()
        }

        loadProjectTabs(initialProject)

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    IdeMainScreen(
                        projectPath = projectPath,
                        openedFiles = openedFiles,
                        activeFilePath = activeFilePath,
                        currentRootNode = currentRootNode,
                        onProjectPicked = { path ->
                            switchProject(path)
                        },
                        onFileSelected = { file ->
                            openFileInTab(file.absolutePath)
                        },
                        onTabSelected = { path ->
                            setupPreview(path)
                        },
                        showProjectPicker = showProjectPicker,
                        onTogglePicker = { show ->
                            if (show) folderPickerLauncher.launch(null)
                            showProjectPicker = show
                        },
                        onCloseFile = { path ->
                            openedFiles = openedFiles.filter { it != path }
                            if (projectPath.isNotBlank()) {
                                prefs.edit()
                                    .putString(getProjectTabsKey(projectPath), openedFiles.joinToString(","))
                                    .apply()
                            }
                            activeFilePath = openedFiles.lastOrNull() ?: ""
                            if (projectPath.isNotBlank()) {
                                prefs.edit()
                                    .putString(getProjectActiveFileKey(projectPath), activeFilePath)
                                    .apply()
                            }
                            if (activeFilePath.isNotEmpty()) {
                                setupPreview(activeFilePath)
                            } else {
                                currentRootNode = ComposeNode("Empty")
                            }
                        },
                        onAddFile = {
                            Toast.makeText(this@MainActivity, getString(R.string.add_file_coming_soon), Toast.LENGTH_SHORT).show()
                        },
                        onSaveFile = {
                            Toast.makeText(this@MainActivity, getString(R.string.file_updated), Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        checkPermissionsAndStart()
    }

    private fun getRealPathFromURI(uri: Uri): String? {
        return StorageUtils.getRealPathFromTreeUri(this, uri)
    }

    private fun openFileInTab(path: String) {
        if (projectPath.isBlank()) return
        val file = File(path)
        if (!file.exists()) return

        if (!openedFiles.contains(path)) {
            openedFiles = openedFiles + path
            getSharedPreferences("app_data", Context.MODE_PRIVATE)
                .edit()
                .putString(getProjectTabsKey(projectPath), openedFiles.joinToString(","))
                .apply()
        }
        setupPreview(path)
    }

    private fun setupPreview(path: String) {
        activeFilePath = path
        if (projectPath.isNotBlank()) {
            getSharedPreferences("app_data", Context.MODE_PRIVATE)
                .edit()
                .putString(getProjectActiveFileKey(projectPath), path)
                .apply()
        }
        loadFileAndParse()

        fileObserver?.stopWatching()
        val file = File(path)
        val parentDir = file.parent

        if (parentDir != null) {
            fileObserver = object : FileObserver(parentDir, MODIFY or DELETE or MOVED_TO) {
                override fun onEvent(event: Int, pathEvent: String?) {
                    if (pathEvent == file.name) {
                        if ((event and DELETE) != 0) {
                            runOnUiThread {
                                fileObserver?.stopWatching()
                                openedFiles = openedFiles.filter { it != path }
                                if (projectPath.isNotBlank()) {
                                    getSharedPreferences("app_data", Context.MODE_PRIVATE)
                                        .edit()
                                        .putString(getProjectTabsKey(projectPath), openedFiles.joinToString(","))
                                        .apply()
                                }
                                activeFilePath = openedFiles.lastOrNull() ?: ""
                                if (projectPath.isNotBlank()) {
                                    getSharedPreferences("app_data", Context.MODE_PRIVATE)
                                        .edit()
                                        .putString(getProjectActiveFileKey(projectPath), activeFilePath)
                                        .apply()
                                }
                                if (activeFilePath.isNotEmpty()) setupPreview(activeFilePath)
                                Toast.makeText(this@MainActivity, getString(R.string.file_preview_deleted), Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            window.decorView.postDelayed({ loadFileAndParse() }, 300)
                        }
                    }
                }
            }
            fileObserver?.startWatching()
        }
    }

    private fun loadFileAndParse() {
        if (activeFilePath.isBlank()) return
        try {
            val file = File(activeFilePath)
            if (!file.exists()) return

            if (org.cat.editor.ui.preview.isBinaryFile(file.name)) {
                // Do not read binary or image files as text
                runOnUiThread {
                    currentRootNode = ComposeNode("ImageFile")
                }
                return
            }

            val lower = file.name.lowercase()
            val isKotlin = lower.endsWith(".kt") || lower.endsWith(".kts")
            val isXml = lower.endsWith(".xml")

            if (!isKotlin && !isXml) {
                // Non-previewable text file (e.g. Java, HTML, Gradle, JSON, CMake, etc.)
                runOnUiThread {
                    currentRootNode = ComposeNode("TextFile")
                }
                return
            }

            val code = file.readText()
            val parsed = if (isXml) {
                if (org.cat.editor.ui.preview.XmlLayoutParser.isLayoutXml(code)) {
                    org.cat.editor.ui.preview.XmlLayoutParser.parse(code)
                } else {
                    ComposeNode("XmlResource")
                }
            } else {
                if (org.cat.editor.ui.preview.ComposePreviewParser.isComposeSource(code)) {
                    org.cat.editor.ui.preview.ComposeAstParser.parse(code, code)
                } else {
                    ComposeNode("KotlinFile")
                }
            }
            runOnUiThread {
                currentRootNode = parsed
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivityForResult(intent, 2000)
                } catch (e: Exception) {
                    startActivityForResult(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION), 2000)
                }
            } else startAppLogic()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_DENIED) {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 1000)
            } else startAppLogic()
        } else startAppLogic()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2000 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            startAppLogic()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000 && grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            startAppLogic()
        }
    }

    private fun startAppLogic() {
        if (projectPath.isEmpty() || !File(projectPath).exists()) {
            showProjectPicker = true
        } else if (activeFilePath.isNotEmpty() && File(activeFilePath).exists()) {
            setupPreview(activeFilePath)
        }
    }

    override fun onResume() {
        super.onResume()
        org.cat.editor.utils.SettingsManager.applyFullscreen(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        fileObserver?.stopWatching()
    }
}