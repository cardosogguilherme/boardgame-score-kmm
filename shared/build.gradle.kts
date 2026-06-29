plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    jvmToolchain(21)

    androidTarget()
    // iOS: this module hosts the ViewModels + NavHost + Koin graph + App() composable, so the whole
    // app runs from commonMain on iPhone. Linking is macOS-only; the Android build is unaffected.
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":deepsea-scoring"))
            implementation(project(":data"))
            implementation(project(":ui"))

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)

            // Multiplatform lifecycle (ViewModel/viewModelScope + collectAsStateWithLifecycle) and
            // navigation, so AppNavHost and the ViewModels compile for iOS as well as Android.
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel.compose.mp)
            implementation(libs.androidx.lifecycle.runtime.compose.mp)
            implementation(libs.androidx.navigation.compose.mp)

            // Multiplatform Koin for Compose.
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            // androidContext() for the Android platform Koin module + Main dispatcher for viewModelScope.
            implementation(libs.koin.android)
            implementation(libs.kotlinx.coroutines.android)
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test"))
                // koin-test's verify() is JVM-only, so the graph check runs on the Android target.
                implementation(libs.koin.test)
            }
        }
    }
}

android {
    // Distinct namespace (not the Kotlin package) so this library's R/BuildConfig don't collide
    // with :androidApp's com.example.app.* classes.
    namespace = "com.example.shared"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}
