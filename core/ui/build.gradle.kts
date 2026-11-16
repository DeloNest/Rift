plugins {
    id("orbit.android.library")
    id("orbit.android.compose")
}

android {
    namespace = "com.orbit.core.ui"
}

dependencies {
    api(project(":core:designsystem"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.collections.immutable)
}
