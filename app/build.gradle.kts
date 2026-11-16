plugins {
    id("rift.android.application")
    id("rift.android.compose")
    id("rift.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.rift.app"

    defaultConfig {
        applicationId = "com.rift.launcher"
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.tracing)
    implementation(libs.androidx.window)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.collections.immutable)

    // Modules — core
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:icons"))
    implementation(project(":core:perf"))

    // Modules — data
    implementation(project(":data:apps"))
    implementation(project(":data:workspace"))
    implementation(project(":data:widgets"))
    implementation(project(":data:settings"))
    implementation(project(":data:usage"))

    // Modules — features
    implementation(project(":feature:home"))
    implementation(project(":feature:drawer"))
    implementation(project(":feature:search"))
    implementation(project(":feature:widgets"))
    implementation(project(":feature:folders"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:smart"))
    implementation(project(":feature:gestures"))

    // Debug
    debugImplementation(libs.leakcanary.android)
}
