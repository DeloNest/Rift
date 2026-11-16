plugins {
    id("orbit.android.feature")
}

android {
    namespace = "com.orbit.feature.smart"
}

dependencies {
    implementation(project(":data:usage"))
    implementation(project(":data:apps"))
    implementation(project(":data:settings"))
}
