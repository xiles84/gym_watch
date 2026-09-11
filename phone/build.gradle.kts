import java.util.Properties

// No org.jetbrains.kotlin.android plugin: AGP 9 has built-in Kotlin support.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

/**
 * The phone companion. It tracks what Audible plays, keeps the recent list the
 * watch shows, and starts a book when the watch asks.
 *
 * It must be the **same app** as the watch's to Android's Data Layer: same
 * `applicationId`, signed with the same key. Otherwise the two never see each
 * other's data or messages, and nothing reports an error.
 */
android {
    namespace = "com.gymwatch.phone"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.gymwatch"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 3
        versionName = "0.3.0"
    }

    // Same keys as :app, read the same way — see app/build.gradle.kts and
    // docs/RELEASING.md. A debug-signed phone app cannot talk to a
    // release-signed watch app.
    val localProperties = rootProject.file("local.properties").takeIf { it.exists() }
        ?.let { file -> Properties().apply { file.inputStream().use { load(it) } } }

    val releaseStore = localProperties?.getProperty("releaseStoreFile")
        ?.let { rootProject.file(it) }
        ?.takeIf { it.exists() }

    signingConfigs {
        if (releaseStore != null) {
            create("release") {
                storeFile = releaseStore
                storePassword = localProperties.getProperty("releaseStorePassword")
                keyAlias = localProperties.getProperty("releaseKeyAlias")
                keyPassword = localProperties.getProperty("releaseKeyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = if (releaseStore != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:application"))
    implementation(project(":adapters:driven:wearsync"))
    implementation(project(":adapters:driven:audible"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.core.ktx)
}
