dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("./libs.versions.toml"))
        }
    }
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://maven.aliyun.com/repository/public")
    }
}

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://maven.aliyun.com/repository/public")
    }

    includeBuild("build-logic")
    includeBuild("kronos-gradle-plugin")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "kronos-orm"

include("kronos-codegen")
include("kronos-compiler-plugin")
include("kronos-core")
include("kronos-jdbc-wrapper")
include("kronos-logging")
include("kronos-maven-plugin")
include("kronos-testing")