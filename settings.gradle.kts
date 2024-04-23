pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.namespace == "org.jetbrains.kotlin") {
                useVersion(file("kotlin.version").readText().trim())
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "kotoframework"
rootProject.children.forEach { project ->
    project.buildFileName = "${project.name}.gradle.kts"
}
include("koto-core")
include("koto-logging")
include("koto-k2-compiler-plugin")
