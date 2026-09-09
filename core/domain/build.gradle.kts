import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

// This is a kotlin("jvm") module on purpose, NOT com.android.library.
// The Android SDK is not on this classpath, so `import android.*` cannot
// compile here. That is the architecture boundary — enforced by the build,
// not by discipline. See docs/ARCHITECTURE.md.
kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // Coroutines is pure Kotlin with no Android dependency — safe in the core.
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test { useJUnitPlatform() }
