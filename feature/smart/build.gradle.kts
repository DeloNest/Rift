plugins {
    id("rift.android.feature")
}

android {
    namespace = "com.rift.feature.smart"
}

dependencies {
    implementation(project(":data:usage"))
    implementation(project(":data:apps"))
    implementation(project(":data:settings"))
}
