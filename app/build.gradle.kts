plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.novamesh"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.novamesh.messenger"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        // Room schema export directory
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.camera.core.ExperimentalCameraApi",
            "-opt-in=androidx.media3.common.util.UnstableApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
        )
    }

    buildFeatures {
        compose = true
        viewBinding = false
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/INDEX.LIST"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

// BOM versions
val composeBom = "2024.02.00"
val roomVersion = "2.6.1"
val cameraVersion = "1.3.1"
val mlkitVersion = "16.1.5"
val lifecycleVersion = "2.7.0"
val navigationVersion = "2.7.7"
val retrofitVersion = "2.9.0"
val okhttpVersion = "4.12.0"
val coilVersion = "2.5.0"
val webrtcVersion = "1.0.3"
val matrixSdkVersion = "1.6.10"

dependencies {
    // ─── Core Android ───
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
    implementation("androidx.activity:activity-compose:1.8.2")

    // ─── Jetpack Compose (via BOM) ───
    implementation(platform("androidx.compose:compose-bom:$composeBom"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime-livedata")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // ─── Navigation ───
    implementation("androidx.navigation:navigation-compose:$navigationVersion")

    // ─── Window (foldables / large screens) ───
    implementation("androidx.window:window:1.2.0")

    // ─── CameraX ───
    implementation("androidx.camera:camera-core:$cameraVersion")
    implementation("androidx.camera:camera-camera2:$cameraVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraVersion")
    implementation("androidx.camera:camera-view:$cameraVersion")
    implementation("androidx.camera:camera-video:$cameraVersion")
    implementation("androidx.camera:camera-extensions:$cameraVersion")

    // ─── ML Kit ───
    implementation("com.google.mlkit:face-detection:${mlkitVersion}")
    implementation("com.google.mlkit:segmentation-selfie:16.0.0-beta4")
    implementation("com.google.mlkit:translate:17.0.2")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("com.google.mlkit:pose-detection:18.0.0-beta3")

    // ─── Security / Encryption ───
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.biometric:biometric:1.1.0")

    // ─── Matrix SDK (Sync) ───
    implementation("org.matrix.android:matrix-android-sdk2:$matrixSdkVersion")

    // ─── WebRTC ───
    implementation("io.getstream:stream-webrtc-android:$webrtcVersion")

    // ─── Networking ───
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-gson:$retrofitVersion")
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttpVersion")
    implementation("com.squareup.okhttp3:okhttp-tls:$okhttpVersion")

    // ─── Kotlin Serialization ───
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // ─── Coroutines ───
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ─── Timber (logging) ───
    implementation("com.jakewharton.timber:timber:5.0.1")

    // ─── Coil (image loading) ───
    implementation("io.coil-kt:coil-compose:$coilVersion")
    implementation("io.coil-kt:coil-video:$coilVersion")
    implementation("io.coil-kt:coil-gif:$coilVersion")

    // ─── Media3 (ExoPlayer) ───
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("androidx.media3:media3-common:1.2.1")

    // ─── Room (local database) ───
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.room:room-paging:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // ─── Firebase (free tier) ───
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // ─── DataStore (preferences) ───
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // ─── QR Code scanning ───
    implementation("com.google.zxing:core:3.5.3")

    // ─── Accompanist (permissions, etc.) ───
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0")
    implementation("com.google.accompanist:accompanist-adaptive:0.34.0")

    // ─── Testing ───
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:$composeBom"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
