plugins {
    id("rift.android.feature")
}

android {
    namespace = "com.rift.feature.search"
}

dependencies {
    implementation(project(":data:apps"))
    implementation(project(":data:settings"))
    implementation(project(":core:ui"))
    testImplementation(libs.junit)
}
