plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "ir.mrpc.hotshare"
    compileSdk = 35

    defaultConfig {
        applicationId = "ir.mrpc.hotshare"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "2.3.0"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }

    // موتور تونل (hev-socks5-tunnel) فقط وقتی کامپایل می‌شود که ساب‌ماژول دانلود شده باشد
    if (file("src/main/jni/hev-socks5-tunnel/Android.mk").exists()) {
        ndkVersion = "27.2.12479018"
        externalNativeBuild {
            ndkBuild { path = file("src/main/jni/Android.mk") }
        }
    }

    // امضای release از متغیرهای محیطی (برای GitHub Actions یا build محلی)
    val ksPath = System.getenv("KEYSTORE_PATH")
    if (ksPath != null) {
        signingConfigs {
            create("release") {
                storeFile = file(ksPath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (System.getenv("KEYSTORE_PATH") != null) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging { jniLibs { useLegacyPackaging = false } }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.android.gms:play-services-location:21.3.0")
}
