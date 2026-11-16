plugins {
    id("rift.android.library")
    id("rift.android.hilt")
}

android {
    namespace = "com.rift.data.widgets"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
