plugins {
    id("rift.android.feature")
}

android {
    namespace = "com.rift.feature.settings"
}

dependencies {
    implementation(project(":data:settings"))
}
