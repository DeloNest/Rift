plugins {
    id("orbit.android.feature")
}

android {
    namespace = "com.orbit.feature.search"
}

dependencies {
    implementation(project(":data:apps"))
    implementation(project(":data:settings"))
    testImplementation(libs.junit)
}
