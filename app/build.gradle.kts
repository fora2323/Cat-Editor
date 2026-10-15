plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "org.cat.editor"
    compileSdk = 37

    signingConfigs {
        val keystoreFile = rootProject.file("test-compose.jks")
        if (keystoreFile.exists()) {
            getByName("debug") {
                storeFile = keystoreFile
                storePassword = "testcompose"
                keyAlias = "TestCompose"
                keyPassword = "testcompose"
            }
            
            create("release") {
                storeFile = keystoreFile
                storePassword = "testcompose"
                keyAlias = "TestCompose"
                keyPassword = "testcompose"
            }
        } else {
            create("release") {
                // Fallback or leave unconfigured if keystore not found
            }
        }
    }

    defaultConfig {
        applicationId = "org.cat.editor"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0-alpha"

        val gitHash = try {
            val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD").start()
            val hash = process.inputStream.bufferedReader().readText().trim()
            if (hash.isNotEmpty()) hash else "3ebb02f"
        } catch (_: Exception) {
            "3ebb02f"
        }
        buildConfigField("String", "GIT_HASH", "\"$gitHash\"")
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true 
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    
    implementation("io.github.Rosemoe.sora-editor:editor:0.23.4")
    implementation("io.github.Rosemoe.sora-editor:language-textmate:0.23.4")
    
    implementation("com.github.Netsnake-TN:ExpressivepullToRefresh:v1.0.5")
    implementation("com.google.android.material:material:1.14.0")
    
    implementation("androidx.compose.material:material-icons-extended:1.5.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.4.0-alpha14")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.8.0.202311291450-r")
    
    implementation("io.coil-kt.coil3:coil-compose:3.1.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.1.0")
    implementation("io.coil-kt.coil3:coil-svg:3.1.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
}