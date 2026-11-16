plugins {
    id("orbit.android.library")
    id("orbit.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.orbit.data.settings"
}

dependencies {
    implementation(libs.androidx.datastore.proto)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
}
