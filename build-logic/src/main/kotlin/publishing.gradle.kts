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
    id("com.vanniktech.maven.publish")
}

@JvmInline
value class PublishConfiguration(val project: Project) {
    init {
        project.group = "com.kotlinorm"
        project.version = "0.0.2-SNAPSHOT"
        project.description = when (project.name) {
            "kronos-core" -> "Kronos is an easy-to-use, flexible, lightweight ORM framework designed for kotlin. Kronos core is the core module of Kronos, which provides basic ORM functions."
            "kronos-jdbc-wrapper" -> "Kronos 's built-in database operation plug-in based on the original jdbc supports variable templates and multiple databases."
            "kronos-logging" -> "Kronos 's built-in log plug-in, which supports multiple log frameworks and can be customized."
            "kronos-compiler-plugin" -> "Kotlin plugin provided by kronos for parsing SQL Criteria expressions at compile time."
            "kronos-maven-plugin" -> "Maven plugin provided by kronos for parsing SQL Criteria expressions at compile time."
            "kronos-gradle-plugin" -> "Gradle plugin provided by kronos for parsing SQL Criteria expressions at compile time."
            else -> "Kronos core"
        }
    }

    val group get() = project.group.toString()
    val name get() = project.name
    val version get() = project.version.toString()
    val description get() = project.description.toString()
}

val publish = PublishConfiguration(project)

mavenPublishing {
    configure(KotlinJvm(JavadocJar.Javadoc(), sourcesJar = true))
    coordinates(publish.group, publish.name, publish.version)

    pom {
        name.set("${publish.group}:${publish.name}")
        description.set(publish.description)
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

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, true)
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
if (project.name == "kronos-orm") {
    tasks.register("publishAllToMavenLocal") {
        group = "kronos publishing"
        project.subprojects.forEach {
            if (it.plugins.hasPlugin("com.vanniktech.maven.publish")) {
                dependsOn(it.tasks.named("publishMavenPublicationToMavenLocalRepository"))
            }
        }
        dependsOn(gradle.includedBuild("kronos-gradle-plugin").task(":publishAllPublicationsToMavenLocalRepository"))
    }
    tasks.register("publishAllToMavenCentral") {
        group = "kronos publishing"
        project.subprojects.forEach {
            if (it.plugins.hasPlugin("com.vanniktech.maven.publish")) {
                dependsOn(it.tasks.named("publishMavenPublicationToMavenCentralRepository"))
            }
        }
        dependsOn(gradle.includedBuild("kronos-gradle-plugin").task(":publishAllPublicationsToMavenCentralRepository"))
    }
    tasks.register("publishAllToAliyun") {
        group = "kronos publishing"
        project.subprojects.forEach {
            if (it.plugins.hasPlugin("com.vanniktech.maven.publish") && it.providers.gradleProperty("aliyunMvnPackages").isPresent) {
                dependsOn(it.tasks.named("publishMavenPublicationToAliyunRepository"))
            }
        }
        dependsOn(gradle.includedBuild("kronos-gradle-plugin").task(":publishAllPublicationsToAliyunRepository"))
    }
}
if (project.name == "kronos-gradle-plugin") {
    afterEvaluate {
        if (!project.providers.gradleProperty("aliyunMvnPackages").isPresent) {
            tasks.register("publishAllPublicationsToAliyunRepository") {
                group = "kronos publishing"
            }
        }
        tasks.forEach {
            if (it.name.startsWith("publish")) {
                it.dependsOn(tasks.getByName("signMavenPublication"))
                it.dependsOn(tasks.getByName("signPluginMavenPublication"))
            }
        }
    }
}