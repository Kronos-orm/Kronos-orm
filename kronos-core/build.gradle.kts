import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:latest.release")
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:latest.release")
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

mavenPublishing {
    configure(
        KotlinJvm(
            javadocJar = JavadocJar.Javadoc(),
            sourcesJar = true,
        ),
//        KotlinMultiplatform()
    )
    coordinates(project.group.toString(), project.name, project.version.toString())
    pom {
        name.set("${project.group}:${project.name}")
        description.set("An easy-to-use, flexible, lightweight ORM framework designed for kotlin.")
        inceptionYear.set("2024")
        url.set("https://www.kotlinorm.com")
        licenses {
            license {
                name.set("The Apache Software License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
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
        scm {
            url.set("https://github.com/Kronos-orm/Kronos-orm")
            connection.set("scm:git:https://github.com/Kronos-orm/Kronos-orm.git")
            developerConnection.set("scm:git:ssh://git@github.com:Kronos-orm/Kronos-orm.git")
        }
    }
    if (version.toString().endsWith("-SNAPSHOT")) {
        publishing {
            repositories {
                maven {
                    name = "snapshot"
                    val githubPackageUsername: String? by project
                    val githubPackagePassword: String? by project
                    url = uri("https://maven.pkg.github.com/Kronos-orm/Kronos-orm/packages")
                    credentials {
                        username = githubPackageUsername ?: System.getenv("GITHUB_ACTOR")
                        password = githubPackagePassword ?: System.getenv("GITHUB_TOKEN")
                    }
                }
            }
        }
    } else {
        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    }
    signAllPublications()
}