plugins {
    id("orbit.android.feature")
}

android {
    namespace = "com.orbit.feature.settings"
}

dependencies {
    implementation(project(":data:settings"))
}
