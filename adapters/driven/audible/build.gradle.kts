// No org.jetbrains.kotlin.android plugin: AGP 9 has built-in Kotlin support and
// applying KGP alongside it is an error. See docs/LESSONS.md #13.
plugins {
    alias(libs.plugins.android.library)
}

// Phone only. Audible's playback session: what it is playing, and the title
// search that starts a book (docs/LESSONS.md #32).
android {
    namespace = "com.gymwatch.adapters.driven.audible"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.minSdk.get().toInt() }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":core:domain"))
    implementation(libs.androidx.core.ktx)

    // Pure JVM: the position arithmetic is tested without a device.
    testImplementation(libs.junit)
}
