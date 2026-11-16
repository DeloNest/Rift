plugins {
    id("orbit.android.library")
    id("orbit.android.hilt")
}

android {
    namespace = "com.orbit.data.widgets"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
