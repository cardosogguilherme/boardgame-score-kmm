plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "com.example.app"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.example.boardgamescore"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":ui"))
    implementation(project(":deepsea-scoring"))
    implementation(project(":data"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.activity.compose)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    // Koin is the app's DI framework. Unlike Hilt it needs no compiler plugin and its module DSL
    // is plain Kotlin, so wiring can migrate to commonMain for an iOS target later. The KMM
    // domain (:deepsea-scoring) and :ui commonMain stay framework-free and are wired here.
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    testImplementation(kotlin("test"))
    testImplementation(libs.koin.test)
}
