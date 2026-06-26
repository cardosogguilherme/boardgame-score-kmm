plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(21)

    jvm()
    // iOS targets (iosX64/iosArm64/iosSimulatorArm64) are deferred: this host is Windows
    // with no macOS/Xcode, so Kotlin/Native iOS builds can't run here. The module stays
    // pure-Kotlin `commonMain`, so adding them later is a build-file-only change.

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
