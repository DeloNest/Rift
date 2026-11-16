plugins {
    id("orbit.android.feature")
}

android {
    namespace = "com.orbit.feature.folders"
}

dependencies {
    implementation(project(":data:workspace"))
    implementation(project(":core:icons"))
}
