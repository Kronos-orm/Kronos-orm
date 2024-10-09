package kronos

import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SonatypeHost

/*
 * This plugin configures publishing for a project.
 *
 * To use it, add
 * ```
 * plugins {
 *     id("kronos.publishing")
 * }
 * ```
 * in your build.gradle.kts file.
 */

plugins {
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    configure(KotlinJvm(JavadocJar.Dokka("dokkaHtml"), sourcesJar = true))
    coordinates(project.group.toString(), project.name, project.version.toString())

    pom {
        name.set("${project.group}:${project.name}")
        description.set("${project.description}")
        url.set("https://www.kotlinorm.com")
        inceptionYear.set("2024")

        licenses {
            license {
                name.set("The Apache Software License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("ousc")
                name.set("ousc")
                email.set("sundaiyue@foxmail.com")
            }
            developer {
                id.set("FOYU")
                name.set("FOYU")
                email.set("2456416562@qq.com")
            }
            developer {
                id.set("yf")
                name.set("yf")
                email.set("1661264104@qq.com")
            }
        }

        scm {
            url.set("https://github.com/Kronos-orm/Kronos-orm")
            connection.set("scm:git:https://github.com/Kronos-orm/Kronos-orm.git")
            developerConnection.set("scm:git:ssh://git@github.com:Kronos-orm/Kronos-orm.git")
        }
    }

    signAllPublications()

    if (!version.toString().endsWith("-SNAPSHOT")) {
        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, true)
    }
}

publishing {
    repositories {
        if (providers.gradleProperty("aliyunMvnPackages").isPresent) {
            maven {
                name = "aliyun"
                url = uri(providers.gradleProperty("aliyunMvnPackages").get())
                credentials {
                    username = providers.gradleProperty("aliyunUsername").get()
                    password = providers.gradleProperty("aliyunPassword").get()
                }
            }
        }
        mavenLocal()
    }
}
