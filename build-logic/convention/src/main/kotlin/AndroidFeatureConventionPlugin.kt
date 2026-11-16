import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("orbit.android.library")
            pluginManager.apply("orbit.android.compose")
            pluginManager.apply("orbit.android.hilt")

            val libs = versionCatalog()
            dependencies {
                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                add("implementation", libs.findLibrary("androidx-navigation-compose").get())
                add("implementation", libs.findLibrary("androidx-hilt-navigation-compose").get())
                add("implementation", libs.findLibrary("kotlinx-collections-immutable").get())
                add("implementation", project(":core:designsystem"))
                add("implementation", project(":core:ui"))
            }
        }
    }
}
