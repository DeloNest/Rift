plugins {
    id("rift.android.library")
    id("rift.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.rift.data.settings"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.proto)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
}
