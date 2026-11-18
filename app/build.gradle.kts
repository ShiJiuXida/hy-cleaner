plugins {
    // AGP 9.x 内置 Kotlin 支持：**不要再加 org.jetbrains.kotlin.android**，否则 apply 阶段直接失败
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
}

android {
    namespace = "com.sabac.hy.cleaner"
    // libxposed service/interface 与 coui 都要求 compileSdk >= 37
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.sabac.hy.cleaner"
        minSdk = 26          // libxposed api/service 的下限
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
            signingConfig = signingConfigs["debug"] // 自用模块直接复用 debug 签名，保持升级可覆盖
        }
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    // META-INF/xposed/* 不能被重复资源合并丢掉，否则 LSPosed 认不出模块
    packaging {
        resources {
            merges += "META-INF/xposed/*"
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
    }
}

dependencies {
    compileOnly(libs.libxposed.api)          // 框架运行时注入实现，绝不能打进 APK
    implementation(libs.libxposed.service)   // XposedService / RemotePreferences / XposedProvider

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.ui)
    implementation(compose.material3)        // 自带 NavigationBar / Switch / Scaffold
}
