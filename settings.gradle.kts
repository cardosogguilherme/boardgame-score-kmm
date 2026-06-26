pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Auto-provisions a JDK matching the toolchain (we compile against JDK 21 even
    // though the Gradle daemon runs on whatever JDK is installed locally).
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "boardgame-score-kmm"

include(":deepsea-scoring", ":ui")
