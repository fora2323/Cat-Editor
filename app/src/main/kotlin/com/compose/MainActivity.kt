package com.compose

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.FileObserver
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.util.Stack
import java.util.regex.Pattern

// --- 1. STRUKTUR DATA NODE UNTUK AST (Abstract Syntax Tree) ---
sealed class ComposeNode {
    data class Layout(
        val name: String,
        val children: MutableList<ComposeNode> = mutableListOf(),
        val isHorizontal: Boolean = false,
        val colorHex: String = "#757575"
    ) : ComposeNode()

    data class Leaf(
        var text: String,
        val colorHex: String = "#B0BEC5"
    ) : ComposeNode()
}

class MainActivity : ComponentActivity() {

    private var fileObserver: FileObserver? = null
    
    // State Compose untuk Re-render Real-time
    private var currentRootNode by mutableStateOf(ComposeNode.Layout("Root"))
    private var showDialog by mutableStateOf(false)
    private var currentPath by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Gunakan layout XML bawaan Anda
        setContentView(R.layout.main)

        val prefs = getSharedPreferences("app_data", Context.MODE_PRIVATE)
        currentPath = prefs.getString("last_path", "") ?: ""

        // Menemukan linear1 di XML dan menyematkan UI Compose di dalamnya
        val linear1 = findViewById<LinearLayout>(R.id.linear1)
        val composeView = ComposeView(this).apply {
            setContent {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    AppContent(
                        prefs = prefs,
                        onCheckPermission = { checkPermissionsAndStart() }
                    )
                }
            }
        }
        linear1.addView(composeView)

        checkPermissionsAndStart()
    }

    // --- 2. MANAJEMEN PERIZINAN ---
    private fun checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivityForResult(intent, 2000)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivityForResult(intent, 2000)
                }
            } else {
                startAppLogic()
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                    requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1000)
                } else {
                    startAppLogic()
                }
            } else {
                startAppLogic()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2000) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                startAppLogic()
            } else {
                Toast.makeText(this, "Izin akses ditolak!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startAppLogic()
        }
    }

    // --- 3. LOGIKA FILE & OBSERVER ---
    private fun startAppLogic() {
        if (currentPath.isEmpty() || !File(currentPath).exists()) {
            showDialog = true
        } else {
            setupPreview(currentPath)
        }
    }

    private fun setupPreview(path: String) {
        currentPath = path
        loadFileAndParse()

        fileObserver?.stopWatching()
        val file = File(path)
        val parentDir = file.parent
        val fileName = file.name

        if (parentDir != null) {
            fileObserver = object : FileObserver(parentDir, MODIFY or DELETE or MOVED_TO) {
                override fun onEvent(event: Int, pathEvent: String?) {
                    if (pathEvent == fileName) {
                        if ((event and DELETE) != 0) {
                            runOnUiThread {
                                fileObserver?.stopWatching()
                                getSharedPreferences("app_data", Context.MODE_PRIVATE).edit().remove("last_path").apply()
                                Toast.makeText(this@MainActivity, "File terhapus!", Toast.LENGTH_SHORT).show()
                                showDialog = true
                            }
                        } else {
                            // Delay sebentar agar save editor selesai
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
            val file = File(currentPath)
            if (!file.exists()) return
            
            val code = file.readText()
            // Parsing dilakukan di thread utama agar UI langsung merespons
            runOnUiThread {
                currentRootNode = parseComposeCode(code)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- 4. ENGINE PARSER STRING KE DATA NODE ---
    private fun parseComposeCode(rawCode: String): ComposeNode.Layout {
        // Bersihkan Komentar
        var code = rawCode.replace(Regex("/\\*([\\s\\S]*?)\\*/"), "")
        code = code.replace(Regex("//.*"), "")

        val root = ComposeNode.Layout("Root", colorHex = "#121212")
        val stack = Stack<ComposeNode.Layout>()
        stack.push(root)

        var lastCreatedLayout: ComposeNode.Layout? = null
        var lastCreatedLeaf: ComposeNode.Leaf? = null

        val p = Pattern.compile("\"([^\"]*)\"|\\b([A-Z][a-zA-Z0-9_]+)\\b(?=\\s*[\\(\\{])|(\\{|\\})")
        val m = p.matcher(code)

        while (m.find()) {
            val strGrp = m.group(1)
            val compGrp = m.group(2)
            val braceGrp = m.group(3)

            val currentParent = stack.peek()

            if (strGrp != null) {
                lastCreatedLeaf?.text = strGrp
            } else if (braceGrp != null) {
                if (braceGrp == "{") {
                    if (lastCreatedLayout != null) {
                        stack.push(lastCreatedLayout)
                    } else {
                        stack.push(currentParent)
                    }
                    lastCreatedLayout = null
                    lastCreatedLeaf = null
                } else if (braceGrp == "}") {
                    if (stack.size > 1) {
                        stack.pop()
                    }
                    lastCreatedLayout = null
                    lastCreatedLeaf = null
                }
            } else if (compGrp != null) {
                var isLayout = true
                var colorHex = "#757575"
                var isHorizontal = false

                when (compGrp) {
                    "Column", "LazyColumn" -> colorHex = "#FF9800"
                    "Row", "LazyRow" -> { colorHex = "#E040FB"; isHorizontal = true }
                    "Box", "Surface", "Card" -> colorHex = "#03A9F4"
                    "Button", "OutlinedButton" -> { colorHex = "#00E676"; isHorizontal = true }
                    "Text", "Icon", "Image", "Spacer" -> { isLayout = false; colorHex = "#B0BEC5" }
                    else -> colorHex = "#F48FB1" // Custom Composable
                }

                if (isLayout) {
                    val newLayout = ComposeNode.Layout(compGrp, colorHex = colorHex, isHorizontal = isHorizontal)
                    currentParent.children.add(newLayout)
                    lastCreatedLayout = newLayout
                } else {
                    val newLeaf = ComposeNode.Leaf(compGrp, colorHex = colorHex)
                    currentParent.children.add(newLeaf)
                    lastCreatedLeaf = newLeaf
                }
            }
        }
        return root
    }

    override fun onDestroy() {
        super.onDestroy()
        fileObserver?.stopWatching()
    }

    // --- 5. UI JETPACK COMPOSE UTAMA ---
    @Composable
    fun AppContent(prefs: android.content.SharedPreferences, onCheckPermission: () -> Unit) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
            
            // Tampilan Wireframe
            Column(modifier = Modifier.fillMaxSize()) {
                Button(
                    onClick = { showDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Text("GANTI PATH CLASS", color = Color.White)
                }

                val scrollState = rememberScrollState()
                Column(modifier = Modifier.verticalScroll(scrollState).padding(16.dp)) {
                    // Render isi dari Root (hindari merender kotak Root itu sendiri)
                    currentRootNode.children.forEach { childNode ->
                        WireframeNode(childNode)
                    }
                }
            }

            // Dialog Input Path
            if (showDialog) {
                var inputText by remember { mutableStateOf(currentPath) }
                
                AlertDialog(
                    onDismissRequest = { 
                        if (currentPath.isNotEmpty()) showDialog = false 
                    },
                    title = { Text("Input Path Compose (.kt)") },
                    text = {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            label = { Text("/storage/emulated/0/...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (File(inputText).exists()) {
                                prefs.edit().putString("last_path", inputText).apply()
                                showDialog = false
                                setupPreview(inputText)
                            } else {
                                Toast.makeText(this@MainActivity, "File tidak ditemukan!", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Text("Preview")
                        }
                    }
                )
            }
        }
    }

    // --- 6. RENDER WIREFRAME NODE SECARA REKURSIF ---
    @Composable
    fun WireframeNode(node: ComposeNode) {
        when (node) {
            is ComposeNode.Layout -> {
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .border(1.dp, Color(AndroidColor.parseColor(node.colorHex)), RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1E1E))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = node.name,
                            color = Color(AndroidColor.parseColor(node.colorHex)),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        if (node.children.isNotEmpty()) {
                            if (node.isHorizontal) {
                                Row {
                                    node.children.forEach { WireframeNode(it) }
                                }
                            } else {
                                Column {
                                    node.children.forEach { WireframeNode(it) }
                                }
                            }
                        }
                    }
                }
            }
            
            is ComposeNode.Leaf -> {
                Text(
                    text = node.text,
                    color = Color(AndroidColor.parseColor(node.colorHex)),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}