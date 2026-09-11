// No org.jetbrains.kotlin.android plugin: AGP 9 has built-in Kotlin support and
// applying KGP alongside it is an error. See docs/LESSONS.md #13.
plugins {
    alias(libs.plugins.android.library)
}

// The Wearable Data Layer, used from both ends: the phone publishes the recent
// audiobooks and answers play requests; the watch reads the list and asks.
android {
    namespace = "com.gymwatch.adapters.driven.wearsync"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.minSdk.get().toInt() }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":core:domain"))
    api(libs.play.services.wearable)
    // Play services drags in fragment 1.0.0; anything that uses
    // registerForActivityResult on top of this module needs 1.3.0 or later.
    api(libs.androidx.fragment)
    implementation(libs.kotlinx.coroutines.play.services)

    // Pure JVM: AudiobookWireTest checks the encoding, never the Data Layer.
    testImplementation(libs.junit)
}
