plugins {
    id("orbit.android.library")
}

android {
    namespace = "com.orbit.core.perf"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.tracing)
    implementation(libs.androidx.metrics.jankstats)
}
