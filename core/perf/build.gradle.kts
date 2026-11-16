plugins {
    id("rift.android.library")
}

android {
    namespace = "com.rift.core.perf"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.tracing)
    implementation(libs.androidx.metrics.jankstats)
}
