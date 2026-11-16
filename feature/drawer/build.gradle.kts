plugins {
    id("rift.android.feature")
}

android {
    namespace = "com.rift.feature.drawer"
}

dependencies {
    implementation(project(":data:apps"))
    implementation(project(":data:settings"))
    implementation(project(":core:icons"))
}
