plugins {
    id("rift.android.feature")
}

android {
    namespace = "com.rift.feature.settings"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":data:settings"))
    implementation(project(":data:apps"))
}
