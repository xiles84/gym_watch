// No org.jetbrains.kotlin.android plugin: AGP 9 has built-in Kotlin support and
// applying KGP alongside it is an error. See docs/LESSONS.md #13.
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.gymwatch.adapters.driven.persistence"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.minSdk.get().toInt() }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":core:domain"))
    implementation(libs.androidx.datastore.preferences)
}
