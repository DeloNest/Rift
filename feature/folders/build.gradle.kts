plugins {
    id("rift.android.feature")
}

android {
    namespace = "com.rift.feature.folders"
}

dependencies {
    implementation(project(":data:workspace"))
    implementation(project(":core:icons"))
}
