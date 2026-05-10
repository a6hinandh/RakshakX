plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {

    namespace = "com.security.rakshakx"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {

        applicationId = "com.security.rakshakx"

        minSdk = 26

        targetSdk = 36

        versionCode = 1

        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {

        release {

            isMinifyEnabled = false

            proguardFiles(

                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),

                "proguard-rules.pro"
            )
        }
    }

    compileOptions {

        sourceCompatibility =
            JavaVersion.VERSION_21

        targetCompatibility =
            JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    buildFeatures {
        compose = true
    }
}

dependencies {

    // ==========================================
    // VOSK OFFLINE ASR
    // ==========================================
    implementation(
        "com.alphacephei:vosk-android:0.3.32"
    )

    // IMPORTANT FIX FOR PointerType ERROR
    implementation(
        "net.java.dev.jna:jna:5.13.0"
    )

    // ==========================================
    // Compose
    // ==========================================
    implementation(
        platform(libs.androidx.compose.bom)
    )

    implementation(
        libs.androidx.activity.compose
    )

    implementation(
        libs.androidx.compose.material3
    )

    implementation(
        libs.androidx.compose.ui
    )

    implementation(
        libs.androidx.compose.ui.graphics
    )

    implementation(
        libs.androidx.compose.ui.tooling.preview
    )

    // ==========================================
    // Core Android
    // ==========================================
    implementation(
        libs.androidx.core.ktx
    )

    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )

    implementation(
        libs.androidx.appcompat
    )

    implementation(
        libs.androidx.recyclerview
    )

    implementation(
        libs.androidx.work.runtime.ktx
    )

    // ==========================================
    // Coroutines
    // ==========================================
    implementation(
        libs.kotlinx.coroutines.android
    )

    implementation(
        libs.kotlinx.coroutines.core
    )

    // ==========================================
    // Room Database
    // ==========================================
    implementation(
        libs.androidx.room.runtime
    )

    implementation(
        libs.androidx.room.ktx
    )

    ksp(
        libs.androidx.room.compiler
    )

    // ==========================================
    // Encryption
    // ==========================================
    implementation(
        libs.sqlcipher.android
    )

    implementation(
        libs.androidx.security.crypto
    )

    // ==========================================
    // Navigation
    // ==========================================
    implementation(
        libs.androidx.navigation.compose
    )

    // ==========================================
    // Extended Material Icons
    // ==========================================
    implementation(
        libs.androidx.compose.material.icons.extended
    )

    // ==========================================
    // Animation
    // ==========================================
    implementation(
        libs.androidx.compose.animation
    )

    // ==========================================
    // Legacy Views
    // ==========================================
    implementation(
        "androidx.constraintlayout:constraintlayout:2.1.4"
    )

    implementation(
        "com.google.android.material:material:1.11.0"
    )

    // ==========================================
    // On-device AI
    // ==========================================
    implementation(
        libs.tensorflow.lite
    )

    implementation(
        libs.onnxruntime.android
    )

    // ==========================================
    // CameraX
    // ==========================================
    implementation(
        libs.androidx.camera.core
    )

    implementation(
        libs.androidx.camera.camera2
    )

    implementation(
        libs.androidx.camera.lifecycle
    )

    implementation(
        libs.androidx.camera.view
    )

    // ==========================================
    // ML Kit Barcode
    // ==========================================
    implementation(
        libs.mlkit.barcode.scanning
    )

    // ==========================================
    // Testing
    // ==========================================
    testImplementation(
        libs.junit
    )

    androidTestImplementation(
        platform(libs.androidx.compose.bom)
    )

    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(
        libs.androidx.junit
    )

    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )

    debugImplementation(
        libs.androidx.compose.ui.tooling
    )
}

tasks.register("verifyRakshakOnnxAssets") {

    group = "verification"

    description =
        "Fails fast if ONNX weights are missing from assets"

    doLast {

        val base =
            layout.projectDirectory.dir(
                "src/main/assets/rakshakx_model"
            )

        val required =
            listOf(

                "distilbert/model.onnx" to 1_000_000L,

                "indicbert/model.onnx" to 500_000L,
            )

        for ((rel, minBytes) in required) {

            val f =
                base.file(rel).asFile

            if (!f.isFile) {

                throw GradleException(

                    "Missing $rel under ${base.asFile}.\n" +
                            "Run:\n" +
                            "python ml/copy_to_assets.py"
                )
            }

            if (f.length() < minBytes) {

                throw GradleException(

                    "${f.invariantSeparatorsPath} is too small (${f.length()} bytes)"
                )
            }
        }
    }
}

tasks.named("preBuild").configure {

    dependsOn(
        "verifyRakshakOnnxAssets"
    )
}