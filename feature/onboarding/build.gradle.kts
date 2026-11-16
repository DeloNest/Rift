plugins {
    id("orbit.android.feature")
}

android {
    namespace = "com.orbit.feature.onboarding"
}

dependencies {
    implementation(project(":data:settings"))
}
