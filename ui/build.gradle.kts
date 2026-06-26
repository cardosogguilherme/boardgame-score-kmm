plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(21)

    jvm()
    // Android + iOS targets are deferred (no Android SDK / no macOS on this host).
    // All Screen A code lives in commonMain, so wiring those targets later is additive.

    sourceSets {
        commonMain.dependencies {
            implementation(project(":deepsea-scoring"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
