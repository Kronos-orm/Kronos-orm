package kronos

import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SonatypeHost
import java.net.URI

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
        project.version = "0.0.6-SNAPSHOT"
        project.description = when (project.name) {
            "kronos-core" -> "Kronos is an easy-to-use, flexible, lightweight ORM framework designed for kotlin. Kronos core is the core module of Kronos, which provides basic ORM functions."
            "kronos-jdbc-wrapper" -> "Kronos 's built-in database operation plug-in based on the original jdbc supports variable templates and multiple databases."
            "kronos-logging" -> "Kronos 's built-in log plug-in, which supports multiple log frameworks and can be customized."
            "kronos-compiler-plugin" -> "Kotlin plugin provided by kronos for parsing SQL Criteria expressions at compile time."
            "kronos-maven-plugin" -> "Maven plugin provided by kronos for parsing SQL Criteria expressions at compile time."
            "kronos-gradle-plugin" -> "Gradle plugin provided by kronos for parsing SQL Criteria expressions at compile time."
            "kronos-codegen" -> "Kronos code generation library, used to read user database and table configurations, and convert them into Kotlin business code, such as KPojo Class."
            else -> "Kronos core"
        }
    }

    val group get() = project.group.toString()
    val name get() = project.name
    val version get() = project.version.toString()
    val description get() = project.description.toString()
}

val publish = PublishConfiguration(project)


class AliyunMvn(
    val url: URI = uri("https://packages.aliyun.com/6661af21262ae18c31667f7d/maven/kronos-orm"),
    val username: String? = System.getenv("ALIYUN_USERNAME") ?: project.findProperty("aliyunUsername") as String?,
    val password: String? = System.getenv("ALIYUN_PASSWORD") ?: project.findProperty("aliyunPassword") as String?,
) {
    val isPresent get() = username != null && password != null
}

class SnapshotMvn(
    val url: URI = uri("https://central.sonatype.com/repository/maven-snapshots/"),
    val username: String? = System.getenv("ORG_GRADLE_PROJECT_mavenCentralUsername")
        ?: project.findProperty("mavenCentralUsername") as String?,
    val password: String? = System.getenv("ORG_GRADLE_PROJECT_mavenCentralPassword")
        ?: project.findProperty("mavenCentralPassword") as String?
) {
    val isPresent get() = publish.version.endsWith("SNAPSHOT") && username != null && password != null
}

val aliyun = AliyunMvn()

val snapshot = SnapshotMvn()

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

    if (!snapshot.isPresent) {
        signAllPublications()
    }

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, true)
}

publishing {
    repositories {
        if (aliyun.isPresent) {
            maven {
                name = "aliyun"
                url = uri(aliyun.url)
                credentials {
                    username = aliyun.username
                    password = aliyun.password
                }
            }
        }
        if (snapshot.isPresent) {
            maven {
                name = "CentralPortalSnapshots"
                url = snapshot.url
                credentials {
                    username = snapshot.username
                    password = snapshot.password
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
            if (it.plugins.hasPlugin("com.vanniktech.maven.publish") && it.providers.gradleProperty("aliyunPackages").isPresent) {
                dependsOn(it.tasks.named("publishMavenPublicationToAliyunRepository"))
            }
        }
        dependsOn(gradle.includedBuild("kronos-gradle-plugin").task(":publishAllPublicationsToAliyunRepository"))
    }
    if (snapshot.isPresent) {
        tasks.register("publishAllToCentralSnapshots") {
            group = "kronos publishing"
            project.subprojects.forEach {
                if (it.plugins.hasPlugin("com.vanniktech.maven.publish")) {
                    dependsOn(it.tasks.named("publishAllPublicationsToCentralPortalSnapshotsRepository"))
                }
            }
            dependsOn(
                gradle.includedBuild("kronos-gradle-plugin").task(":publishAllPublicationsToCentralPortalSnapshotsRepository")
            )
        }
    }
}
if (project.name == "kronos-gradle-plugin") {
    afterEvaluate {
        if (!aliyun.isPresent) {
            tasks.register("publishAllPublicationsToAliyunRepository") {
                group = "kronos publishing"
            }
        }
        if (!snapshot.isPresent) {
            tasks.forEach {
                if (it.name.startsWith("publish")) {
                    it.dependsOn(tasks.getByName("signMavenPublication"))
                    it.dependsOn(tasks.getByName("signPluginMavenPublication"))
                }
            }
        }
    }
}