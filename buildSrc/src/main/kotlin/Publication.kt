import com.vanniktech.maven.publish.*
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.PublishingExtension

fun MavenPom.basicInformation() = apply {
    url.set("https://www.kotlinorm.com")
    inceptionYear.set("2024")
}

fun MavenPom.apacheLicense2() = licenses {
    license {
        name.set("The Apache Software License, Version 2.0")
        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
        distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
    }
}

fun MavenPom.projectDevelopers() = developers {
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
}

fun MavenPom.sourceControlManagement() = scm {
    url.set("https://github.com/Kronos-orm/Kronos-orm")
    connection.set("scm:git:https://github.com/Kronos-orm/Kronos-orm.git")
    developerConnection.set("scm:git:ssh://git@github.com:Kronos-orm/Kronos-orm.git")
}

fun Project.snapshot(mavenExt: MavenPublishBaseExtension) {
    if (!version.toString().endsWith("-SNAPSHOT")) {
        mavenExt.publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, true)
    }
}

fun Project.repositories(publishing: PublishingExtension) {
    publishing.repositories {
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

fun Project.kronosPublishing(
    maven: MavenPublishBaseExtension,
    publishing: PublishingExtension,
    platform: Platform,
    description: String
) {
    maven.configure(platform)
    maven.coordinates(project.group.toString(), project.name, project.version.toString())
    maven.pom{
        name.set("${project.group}:${project.name}")
        this.description.set(description)
        basicInformation()
        apacheLicense2()
        projectDevelopers()
        sourceControlManagement()
    }
    repositories(publishing)
    snapshot(maven)
    maven.signAllPublications()
}