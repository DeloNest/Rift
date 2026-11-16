plugins {
    id("rift.android.library")
    id("rift.android.compose")
}

android {
    namespace = "com.rift.core.ui"
}

dependencies {
    api(project(":core:designsystem"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.collections.immutable)
}
