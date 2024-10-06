dependencyResolutionManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://kotlin.bintray.com/kotlinx")
    }
}

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://maven.aliyun.com/repository/public")
        if (providers.gradleProperty("aliyunMvnPackages").isPresent) {
            maven {
                url = uri(providers.gradleProperty("aliyunMvnPackages").get())
                credentials {
                    username = providers.gradleProperty("aliyunUsername").get()
                    password = providers.gradleProperty("aliyunPassword").get()
                }
            }
        }
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.namespace == "org.jetbrains.kotlin") {
                useVersion(file("kotlin.version").readText().trim())
            }
            if (requested.id.namespace == "com.kotlinorm") {
                useVersion(file("kronos.version").readText().trim())
            }
        }
    }

    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "kronos-orm"

include("kronos-core")
include("kronos-logging")
include("kronos-compiler-plugin")
include("kronos-jdbc-wrapper")
include("kronos-testing")
include("kronos-gradle-plugin")
include("kronos-maven-plugin")
