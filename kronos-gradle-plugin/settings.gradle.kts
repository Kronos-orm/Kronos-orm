dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://kotlin.bintray.com/kotlinx")
    }
}

pluginManagement {
    repositories {
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
                useVersion(file("../kotlin.version").readText().trim())
            }
        }
    }

    includeBuild("../build-logic")
}

rootProject.name = "kronos-orm-gradle-plugin"
