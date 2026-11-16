plugins {
    id("orbit.android.feature")
}

android {
    namespace = "com.orbit.feature.widgets"
}

dependencies {
    implementation(project(":data:widgets"))
    implementation(project(":data:workspace"))
}
