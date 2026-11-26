plugins {
    // AGP 9.x 内置 Kotlin 支持：**不要再加 org.jetbrains.kotlin.android**，否则 apply 阶段直接失败
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
}

android {
    namespace = "com.halo.mirror"
    // libxposed service/interface 要求 compileSdk >= 37
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.halo.mirror"
        minSdk = 26          // libxposed api/service 的下限
        targetSdk = 37
        // versionName 是用户看到的版本号；versionCode 必须单调递增才能覆盖安装，
        // 所以对外首发叫 1.0，内部仍然接着之前的序号往上走。
        versionCode = 5
        versionName = "1.0"
    }

    buildTypes {
        debug {
            // 排错用：保留类名，日志正常输出
            isMinifyEnabled = false
        }
        release {
            // 混淆是防检测的一部分：类名/字符串/诊断代码都在这一步消失
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
            signingConfig = signingConfigs["debug"] // 自用模块复用 debug 签名，保持升级可覆盖
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

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
