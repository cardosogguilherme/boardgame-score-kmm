plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    jvmToolchain(21)

    androidTarget()
    jvm()
    // iOS targets: the domain is pure Kotlin (serialization + coroutines, both KMP), so it compiles
    // for Apple targets unchanged. Linking the iOS frameworks is macOS-only; on a non-Mac host these
    // targets configure fine and the Android/JVM builds are unaffected.
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

// The domain stays pure Kotlin (no Android/Compose imports in source); the Android target only
// exists so the :ui Android variant can resolve this module through the dependency graph.
android {
    namespace = "com.example.scoring"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
