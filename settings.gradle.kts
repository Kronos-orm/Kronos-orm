pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.namespace == "org.jetbrains.kotlin") {
                useVersion(file("kotlin.version").readText().trim())
            }
            if (requested.id.namespace == "com.kotoframework") {
                useVersion(file("kronos.version").readText().trim())
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "kronos-orm"
rootProject.children.forEach { project ->
    project.buildFileName = "${project.name}.gradle.kts"
}
include("kronos-core")
include("kronos-logging")
include("kronos-compiler-plugin")
include("kronos-basic-wrapper")
