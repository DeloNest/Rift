plugins {
    id("rift.android.library")
    id("rift.android.hilt")
    alias(libs.plugins.room)
}

android {
    namespace = "com.rift.data.usage"
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.core)
}
