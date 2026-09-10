pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "gym-watch"

// --- core: pure Kotlin JVM, no Android on the classpath by construction ---
include(":core:domain")
include(":core:application")

// --- driven adapters: the core's outbound edges ---
include(":adapters:driven:platform")
include(":adapters:driven:persistence")
include(":adapters:driven:health")

// --- driving adapters: what calls into the core ---
include(":adapters:driving:ui-compose")
include(":adapters:driving:service")

// --- composition root ---
include(":app")
