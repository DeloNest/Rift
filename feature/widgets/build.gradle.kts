plugins {
    id("rift.android.feature")
}

android {
    namespace = "com.rift.feature.widgets"
}

dependencies {
    implementation(project(":data:widgets"))
    implementation(project(":data:workspace"))
}
