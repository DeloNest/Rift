plugins {
    id("orbit.android.feature")
}

android {
    namespace = "com.orbit.feature.home"
}

dependencies {
    implementation(project(":data:workspace"))
    implementation(project(":data:apps"))
    implementation(project(":data:widgets"))
    implementation(project(":data:settings"))
    implementation(project(":core:icons"))
}
