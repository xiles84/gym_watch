import java.util.Properties

// No org.jetbrains.kotlin.android plugin: AGP 9 has built-in Kotlin support.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.gymwatch"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.gymwatch"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    /**
     * Release signing, configured from `local.properties`.
     *
     * If the release keys are absent the build falls back to the debug keystore
     * so a release variant can still be assembled and sideloaded. An app signed
     * with a different key counts as a different app to Android, so switching
     * keys means uninstalling first.
     *
     * See docs/RELEASING.md to create the keystore from scratch.
     */
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
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:application"))
    implementation(project(":adapters:driven:platform"))
    implementation(project(":adapters:driven:persistence"))
    implementation(project(":adapters:driven:health"))
    implementation(project(":adapters:driving:ui-compose"))
    implementation(project(":adapters:driving:service"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.core.ktx)
}
