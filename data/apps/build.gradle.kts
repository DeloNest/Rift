plugins {
    id("orbit.android.library")
    id("orbit.android.hilt")
}

android {
    namespace = "com.orbit.data.apps"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
