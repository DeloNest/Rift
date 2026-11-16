plugins {
    id("rift.android.feature")
}

android {
    namespace = "com.rift.feature.gestures"
}

dependencies {
    implementation(project(":data:settings"))
}
