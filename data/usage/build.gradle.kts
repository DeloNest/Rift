plugins {
    id("orbit.android.library")
    id("orbit.android.hilt")
    alias(libs.plugins.room)
}

android {
    namespace = "com.orbit.data.usage"
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
