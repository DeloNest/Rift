plugins {
    id("rift.android.feature")
}

android {
    namespace = "com.rift.feature.widgets"
}

dependencies {
    implementation(project(":data:widgets"))
    implementation(project(":data:workspace"))
    implementation(project(":data:settings"))
    implementation(project(":data:apps"))
}
