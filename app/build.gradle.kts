plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.r7tkeysx.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.r7tkeysx.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0"
        // Chỉ đóng gói thư viện máy (.so) cho điện thoại 64-bit ARM, bỏ bản x86/32-bit cho APK nhẹ
        ndk { abiFilters += "arm64-v8a" }
    }

    buildTypes {
        release {
            // Bản dùng hằng ngày: chạy nhanh hơn bản debug, không có màn thử nghiệm, không lưu âm thanh mic.
            // Ký bằng khoá debug sẵn có trên máy này để cài đè lên bản đang dùng mà không mất cài đặt / model giọng nói đã tải.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false // không rút gọn code: thư viện giọng nói (sherpa-onnx) gọi Kotlin qua JNI theo tên
        }
    }

    // Model giọng nói không còn đóng sẵn trong file cài: người dùng chọn tiếng Việt / tiếng Anh rồi app tải về (xem voice/VoiceModels.kt)

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // sherpa-onnx chỉ cần bản JNI + onnxruntime; hai thư viện C/C++ API kia không dùng tới
    packaging {
        jniLibs.excludes += listOf("**/libsherpa-onnx-c-api.so", "**/libsherpa-onnx-cxx-api.so")
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    // Liquid glass (Apache-2.0): kính mờ + khúc xạ, theo yêu cầu của người dùng
    implementation("io.github.kyant0:backdrop:2.0.1")
    // Nhận diện giọng nói offline (Apache-2.0): sherpa-onnx 1.13.8, tải sẵn từ GitHub vào app/libs
    implementation(files("libs/sherpa-onnx-1.13.8.aar"))
    debugImplementation("androidx.compose.ui:ui-tooling")
    // Chỉ dùng khi chạy kiểm thử trên máy tính (./gradlew testDebugUnitTest), KHÔNG đóng vào file cài
    testImplementation("junit:junit:4.13.2")
}
