package org.cat.editor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.FileObserver
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import org.cat.editor.ui.theme.AppTheme
import org.cat.editor.ui.IdeMainScreen
import org.cat.editor.utility.ComposeNode
import org.cat.editor.utility.IdComposeParser
import java.io.File

class MainActivity : ComponentActivity() {

    private var fileObserver: FileObserver? = null
    
    // Global States
    private var currentRootNode by mutableStateOf<ComposeNode.Layout>(ComposeNode.Layout("Root"))
    private var projectPath by mutableStateOf("")
    private var activeFilePath by mutableStateOf("") // File yang sedang dibuka (Aktif)
    private var openedFiles by mutableStateOf<List<String>>(emptyList()) // Daftar semua file di Tab
    private var showProjectPicker by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Mengaktifkan Edge-to-Edge (Tampilan layar penuh menembus Status Bar)
        enableEdgeToEdge() 
        setContentView(R.layout.main)

        val prefs = getSharedPreferences("app_data", Context.MODE_PRIVATE)
        projectPath = prefs.getString("last_project", "") ?: ""
        activeFilePath = prefs.getString("last_preview", "") ?: ""
        
        // Memuat tab yang sebelumnya terbuka
        val savedTabs = prefs.getString("opened_files", "") ?: ""
        if (savedTabs.isNotEmpty()) {
            openedFiles = savedTabs.split(",")
        }

        val linear1 = findViewById<LinearLayout>(R.id.linear1)
        val composeView = ComposeView(this).apply {
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
                                projectPath = path
                                prefs.edit().putString("last_project", path).apply()
                            },
                            onFileSelected = { file ->
                                openFileInTab(file.absolutePath)
                            },
                            onTabSelected = { path ->
                                setupPreview(path)
                            },
                            showProjectPicker = showProjectPicker,
                            onTogglePicker = { show -> showProjectPicker = show }
                        )
                    }
                }
            }
        }
        linear1.addView(composeView)
        checkPermissionsAndStart()
    }

    private fun openFileInTab(path: String) {
        // Jika file belum ada di list tab, tambahkan
        if (!openedFiles.contains(path)) {
            openedFiles = openedFiles + path
            getSharedPreferences("app_data", Context.MODE_PRIVATE)
                .edit()
                .putString("opened_files", openedFiles.joinToString(","))
                .apply()
        }
        // Jadikan file tersebut sebagai tab aktif
        setupPreview(path)
    }

    private fun setupPreview(path: String) {
        activeFilePath = path
        getSharedPreferences("app_data", Context.MODE_PRIVATE).edit().putString("last_preview", path).apply()
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
                                // Hapus dari tab jika file terhapus di memori
                                openedFiles = openedFiles.filter { it != path }
                                getSharedPreferences("app_data", Context.MODE_PRIVATE).edit()
                                    .putString("opened_files", openedFiles.joinToString(",")).apply()
                                
                                activeFilePath = openedFiles.lastOrNull() ?: ""
                                if (activeFilePath.isNotEmpty()) setupPreview(activeFilePath)
                                Toast.makeText(this@MainActivity, "File Preview Terhapus", Toast.LENGTH_SHORT).show()
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
        try {
            val file = File(activeFilePath)
            if (!file.exists()) return
            
            val code = file.readText()
            runOnUiThread { 
                currentRootNode = IdComposeParser.parse(code)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // ... (Fungsi Perizinan checkPermissionsAndStart, onActivityResult, onRequestPermissionsResult, startAppLogic TETAP SAMA SEPERTI SEBELUMNYA)
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
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
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
        if (requestCode == 1000 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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

    override fun onDestroy() {
        super.onDestroy()
        fileObserver?.stopWatching()
    }
}