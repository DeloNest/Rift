pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Orbit"

// :app
include(":app")

// :core modules
include(":core:designsystem")
include(":core:ui")
include(":core:icons")
include(":core:perf")

// :data modules
include(":data:apps")
include(":data:workspace")
include(":data:widgets")
include(":data:settings")
include(":data:usage")

// :feature modules
include(":feature:home")
include(":feature:drawer")
include(":feature:search")
include(":feature:widgets")
include(":feature:folders")
include(":feature:settings")
include(":feature:onboarding")
include(":feature:smart")
include(":feature:gestures")

// Testing
include(":benchmark")
include(":baselineprofile")
