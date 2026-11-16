plugins {
    id("rift.android.feature")
}

android {
    namespace = "com.rift.feature.onboarding"
}

dependencies {
    implementation(project(":data:settings"))
}
