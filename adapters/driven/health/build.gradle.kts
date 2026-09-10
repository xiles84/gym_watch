// No org.jetbrains.kotlin.android plugin: AGP 9 has built-in Kotlin support.
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.gymwatch.adapters.driven.health"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.minSdk.get().toInt() }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":core:domain"))
    implementation(libs.androidx.health.services.client)
    // Health Services returns ListenableFuture; this provides `await()`.
    implementation(libs.kotlinx.coroutines.guava)
}
