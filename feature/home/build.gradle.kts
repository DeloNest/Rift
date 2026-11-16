plugins {
    id("rift.android.feature")
}

android {
    namespace = "com.rift.feature.home"
}

dependencies {
    implementation(project(":data:workspace"))
    implementation(project(":data:apps"))
    implementation(project(":data:widgets"))
    implementation(project(":data:settings"))
    implementation(project(":core:icons"))
    implementation(project(":feature:widgets"))
}
