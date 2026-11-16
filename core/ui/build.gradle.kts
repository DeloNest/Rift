plugins {
    id("rift.android.library")
    id("rift.android.compose")
}

android {
    namespace = "com.rift.core.ui"
}

dependencies {
    api(project(":core:designsystem"))
    api(project(":core:icons"))
    api(project(":data:apps"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.collections.immutable)
}
