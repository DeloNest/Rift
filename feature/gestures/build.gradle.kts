plugins {
    id("orbit.android.feature")
}

android {
    namespace = "com.orbit.feature.gestures"
}

dependencies {
    implementation(project(":data:settings"))
}
