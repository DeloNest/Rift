plugins {
    id("orbit.android.library")
    id("orbit.android.compose")
}

android {
    namespace = "com.orbit.core.designsystem"
}

dependencies {
    api(libs.androidx.material3)
    implementation(libs.androidx.core.ktx)
}
