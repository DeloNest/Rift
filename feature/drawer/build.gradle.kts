plugins {
    id("orbit.android.feature")
}

android {
    namespace = "com.orbit.feature.drawer"
}

dependencies {
    implementation(project(":data:apps"))
    implementation(project(":data:settings"))
    implementation(project(":core:icons"))
}
