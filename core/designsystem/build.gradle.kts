plugins {
    id("rift.android.library")
    id("rift.android.compose")
}

android {
    namespace = "com.rift.core.designsystem"
}

dependencies {
    api(libs.androidx.material3)
    implementation(libs.androidx.core.ktx)
}
