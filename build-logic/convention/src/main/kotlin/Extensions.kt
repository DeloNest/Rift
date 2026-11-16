import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Retrieves the version catalog named "libs".
 */
internal fun Project.versionCatalog(): VersionCatalog =
    extensions.getByType<VersionCatalogsExtension>().named("libs")

/**
 * Configures Kotlin JVM target for Android projects.
 */
internal fun Project.configureKotlinJvm() {
    extensions.findByType(KotlinAndroidProjectExtension::class.java)?.apply {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=kotlinx.coroutines.FlowPreview",
            )
        }
    }
}
