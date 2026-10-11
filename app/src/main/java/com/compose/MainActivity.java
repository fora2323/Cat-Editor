package com.compose;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    
    private LinearLayout linear1;
    private SharedPreferences prefs;
    private FileObserver fileObserver;
    private String currentPath = "";
    
    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        setContentView(R.layout.main);
        initialize();
        initializeLogic();
    }
    
    private void initialize() {
        linear1 = findViewById(R.id.linear1);
        linear1.setOrientation(LinearLayout.VERTICAL);
        prefs = getSharedPreferences("app_data", Activity.MODE_PRIVATE);
    }
    
    private void initializeLogic() {
        checkPermissionsAndStart();
    }
    
    // --- 1. PERIZINAN STORAGE (ANDROID 11+ & LAWAS) ---
    private void checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, 2000);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivityForResult(intent, 2000);
                }
            } else {
                startAppLogic();
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1000);
                } else {
                    startAppLogic();
                }
            } else {
                startAppLogic();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2000) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                startAppLogic();
            } else {
                Toast.makeText(this, "Izin akses ditolak!", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startAppLogic();
        }
    }

    // --- 2. LOGIKA UTAMA & DIALOG ---
    private void startAppLogic() {
        String savedPath = prefs.getString("last_path", "");
        if (savedPath.isEmpty() || !new File(savedPath).exists()) {
            showPathInputDialog();
        } else {
            setupPreview(savedPath);
        }
    }
    
    private void showPathInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Input Path Compose (.kt)");
        
        final EditText input = new EditText(this);
        input.setHint("/storage/emulated/0/.../Screen.kt");
        input.setText(prefs.getString("last_path", ""));
        
        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(dp(20), dp(10), dp(20), 0);
        layout.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        builder.setView(layout);
        
        builder.setPositiveButton("Preview", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String path = input.getText().toString().trim();
                if (new File(path).exists()) {
                    prefs.edit().putString("last_path", path).apply();
                    setupPreview(path);
                } else {
                    Toast.makeText(MainActivity.this, "File tidak ditemukan!", Toast.LENGTH_SHORT).show();
                    showPathInputDialog(); 
                }
            }
        });
        builder.setCancelable(false);
        builder.show();
    }
    
    // --- 3. PREVIEW & FILE OBSERVER (REALTIME) ---
    private void setupPreview(String path) {
        currentPath = path;
        loadFileAndRenderUI();
        
        if (fileObserver != null) fileObserver.stopWatching();
        File file = new File(path);
        String parentDir = file.getParent();
        final String fileName = file.getName();
        
        if (parentDir != null) {
            fileObserver = new FileObserver(parentDir, FileObserver.MODIFY | FileObserver.DELETE | FileObserver.MOVED_TO) {
                @Override
                public void onEvent(int event, String pathEvent) {
                    if (pathEvent != null && pathEvent.equals(fileName)) {
                        if ((event & FileObserver.DELETE) != 0) {
                            runOnUiThread(new Runnable() { @Override public void run() { handleFileDeleted(); } });
                        } else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override public void run() { loadFileAndRenderUI(); }
                            }, 300);
                        }
                    }
                }
            };
            fileObserver.startWatching();
        }
    }
    
    private void handleFileDeleted() {
        if (fileObserver != null) fileObserver.stopWatching();
        prefs.edit().remove("last_path").apply();
        Toast.makeText(this, "File terhapus!", Toast.LENGTH_SHORT).show();
        showPathInputDialog();
    }

    private void loadFileAndRenderUI() {
        try {
            File file = new File(currentPath);
            if (!file.exists()) return;
            
            StringBuilder text = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line).append('\n');
            }
            br.close();
            
            final String code = text.toString();
            runOnUiThread(new Runnable() {
                @Override
                public void run() { renderPresisiComposeUI(code); }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // --- 4. MESIN PARSER PRESISI (GAYA WIREFRAME / BOUNDING BOX) ---
    private void renderPresisiComposeUI(String rawCode) {
        linear1.removeAllViews();
        linear1.setBackgroundColor(Color.parseColor("#121212")); // Tema Gelap
        
        // Header Tombol
        Button btnChangePath = new Button(this);
        btnChangePath.setText("GANTI PATH CLASS");
        btnChangePath.setBackgroundColor(Color.parseColor("#333333"));
        btnChangePath.setTextColor(Color.WHITE);
        btnChangePath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fileObserver != null) fileObserver.stopWatching();
                showPathInputDialog();
            }
        });
        linear1.addView(btnChangePath);
        
        // Container Utama ScrollView
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            
        LinearLayout rootPreview = new LinearLayout(this);
        rootPreview.setOrientation(LinearLayout.VERTICAL);
        rootPreview.setPadding(dp(16), dp(16), dp(16), dp(16));
        scrollView.addView(rootPreview);
        linear1.addView(scrollView);
        
        // Bersihkan Komentar
        String code = rawCode.replaceAll("/\\*([\\s\\S]*?)\\*/", "").replaceAll("//.*", "");
        
        // REGEX 4 PILAR
        Pattern p = Pattern.compile(
            "\"([^\"]*)\"" + 
            "|\\b([A-Z][a-zA-Z0-9_]+)\\b(?=\\s*[\\(\\{])" + 
            "|\\b(fillMaxWidth|fillMaxSize|Center|CenterHorizontally|CenterVertically|weight)\\b" + 
            "|(\\{|\\})"
        );
        Matcher m = p.matcher(code);
        
        Stack<ViewGroup> stack = new Stack<>();
        stack.push(rootPreview);
        
        ViewGroup currentParent = rootPreview;
        View lastCreatedLayout = null;
        View lastCreatedLeaf = null;
        View lastCreatedView = null; 
        
        while (m.find()) {
            if (m.group(1) != null) {
                // PILAR 1: STRING
                String str = m.group(1);
                if (lastCreatedLeaf instanceof TextView) {
                    ((TextView) lastCreatedLeaf).setText(str);
                }
                
            } else if (m.group(3) != null) {
                // PILAR 3: MODIFIER / ALIGNMENT
                String mod = m.group(3);
                if (lastCreatedView != null && lastCreatedView.getLayoutParams() != null) {
                    ViewGroup.LayoutParams lp = lastCreatedView.getLayoutParams();
                    
                    if (mod.equals("fillMaxWidth")) {
                        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    } else if (mod.equals("fillMaxSize")) {
                        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                        lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    } else if (mod.equals("weight")) {
                        if (lp instanceof LinearLayout.LayoutParams) {
                            ((LinearLayout.LayoutParams) lp).weight = 1.0f;
                            // FIX ERROR getOrientation: Cek tipe data parent
                            if (currentParent instanceof LinearLayout) {
                                if (((LinearLayout) currentParent).getOrientation() == LinearLayout.HORIZONTAL) {
                                    lp.width = 0;
                                } else {
                                    lp.height = 0;
                                }
                            }
                        }
                    }
                    lastCreatedView.setLayoutParams(lp);
                }
                
            } else if (m.group(4) != null) {
                // PILAR 4: KURUNG KURAWAL SCOPE
                String brace = m.group(4);
                if (brace.equals("{")) {
                    if (lastCreatedLayout instanceof ViewGroup) {
                        stack.push((ViewGroup) lastCreatedLayout);
                    } else {
                        stack.push(currentParent);
                    }
                    currentParent = stack.peek();
                    lastCreatedLayout = null;
                    lastCreatedLeaf = null;
                } else if (brace.equals("}")) {
                    if (stack.size() > 1) {
                        stack.pop();
                        currentParent = stack.peek();
                    }
                    lastCreatedLayout = null;
                    lastCreatedLeaf = null;
                }
                
            } else if (m.group(2) != null) {
                // PILAR 2: NAMA COMPOSABLE (DIBUAT JADI WIREFRAME)
                String kw = m.group(2);
                View v = null;
                boolean isLayout = true; 
                
                String colorHex = "#757575"; // Default Gray
                boolean isHorizontal = false;
                
                // Menyesuaikan Warna Wireframe
                if (kw.equals("Column") || kw.equals("LazyColumn")) {
                    colorHex = "#FF9800"; // Orange
                } else if (kw.equals("Row") || kw.equals("LazyRow")) {
                    colorHex = "#E040FB"; // Purple
                    isHorizontal = true;
                } else if (kw.equals("Box") || kw.equals("Surface") || kw.equals("Card")) {
                    colorHex = "#03A9F4"; // Blue
                } else if (kw.contains("Button")) {
                    colorHex = "#00E676"; // Green
                    isHorizontal = true;
                } else if (kw.equals("Text") || kw.equals("Icon") || kw.equals("Image") || kw.equals("Spacer")) {
                    // Teks murni / Leaf Component
                    TextView leaf = new TextView(this);
                    leaf.setText(kw);
                    leaf.setTextColor(Color.parseColor("#B0BEC5"));
                    leaf.setTextSize(12f);
                    leaf.setPadding(dp(8), dp(4), dp(8), dp(4));
                    v = leaf;
                    isLayout = false;
                } else {
                    // Custom Composable (Fallback)
                    colorHex = "#F48FB1"; // Pink
                }
                
                // Buat Wireframe Box
                if (isLayout) {
                    v = createWireframeContainer(kw, colorHex, isHorizontal);
                }
                
                if (v != null) {
                    if (v.getTag() instanceof View) {
                        currentParent.addView((View) v.getTag());
                    } else {
                        currentParent.addView(v); 
                    }
                    
                    lastCreatedView = v;
                    if (isLayout) {
                        lastCreatedLayout = v; 
                    } else {
                        lastCreatedLeaf = v;
                    }
                }
            }
        }
    }
    
    // --- 5. FUNGSI BANTUAN MEMBUAT KOTAK WIREFRAME ---
    private LinearLayout createWireframeContainer(String title, String colorHex, boolean isHorizontal) {
        // Wrapper Utama (Garis luar)
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpWrapper = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpWrapper.setMargins(dp(4), dp(4), dp(4), dp(4));
        wrapper.setLayoutParams(lpWrapper);
        wrapper.setPadding(dp(8), dp(8), dp(8), dp(8));
        
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#1E1E1E")); // Transparan gelap
        gd.setStroke(dp(1), Color.parseColor(colorHex));
        gd.setCornerRadius(dp(8));
        wrapper.setBackground(gd);
        
        // Label Nama Composable
        TextView label = new TextView(this);
        label.setText(title);
        label.setTextColor(Color.parseColor(colorHex));
        label.setTextSize(10f);
        label.setTypeface(null, Typeface.BOLD);
        label.setPadding(0, 0, 0, dp(4));
        wrapper.addView(label);
        
        // Area Konten Anak (Tempat UI selanjutnya masuk)
        LinearLayout contentArea = new LinearLayout(this);
        contentArea.setOrientation(isHorizontal ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        contentArea.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            
        wrapper.addView(contentArea);
        
        // Simpan wrapper di dalam tag agar terbaca parser
        contentArea.setTag(wrapper);
        
        return contentArea;
    }
    
    // --- UTILS ---
    private int dp(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fileObserver != null) fileObserver.stopWatching();
    }
}