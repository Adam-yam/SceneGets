plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// CI(release.yml)가 workflow_dispatch로 입력받은 버전을
// -PreleaseVersionName=x.y.z 형태로 넘겨준다. 로컬 빌드처럼 값이 없으면 1.0.0으로 폴백.
// versionCode는 "major.minor.patch"를 major*10000 + minor*100 + patch로 환산해서
// 별도 입력 없이도 버전이 올라갈 때마다 자동으로 함께 증가하게 한다.
val releaseVersionName = (project.findProperty("releaseVersionName") as String?)?.takeIf { it.isNotBlank() }
    ?: "1.0.0"
val releaseVersionCode = releaseVersionName
    .split(".")
    .mapNotNull { it.toIntOrNull() }
    .let { parts ->
        val major = parts.getOrElse(0) { 1 }
        val minor = parts.getOrElse(1) { 0 }
        val patch = parts.getOrElse(2) { 0 }
        major * 10_000 + minor * 100 + patch
    }

android {
    namespace = "com.adamyam.scenegets"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.adamyam.scenegets"
        minSdk = 26
        targetSdk = 35
        versionCode = releaseVersionCode
        versionName = releaseVersionName

        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    val releaseKeystorePath = System.getenv("RELEASE_KEYSTORE_PATH")

    signingConfigs {
        if (releaseKeystorePath != null) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseKeystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }


    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")

    // Glance (Jetpack Compose 기반 위젯)
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    // 백그라운드 자동 갱신
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // 로컬 캐시
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // 네트워크 + JSON 파싱
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Modern maintained yt-dlp Android wrapper (MIT). It embeds yt-dlp + Python 3.13.
    // The compat shim keeps the stable YoutubeDL/YoutubeDLRequest API used by the service.
    implementation("dev.ffmpegkit-maintained:yt-dlp-android:2.0.2")
}
