plugins {
    kotlin("jvm")
    id("signing")
    id("maven-publish")
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

val jarSources by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.map { it.allSource })
}

val jarJavadoc by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        create<MavenPublication>("dist") {
            from(components["java"])
            artifact(jarSources)
            artifact(jarJavadoc)

            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            pom {
                name.set("${project.group}:${project.name}")
                description.set("An easy-to-use, flexible, lightweight ORM framework designed for kotlin.")
                url.set("https://www.kotlinorm.com")
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                scm {
                    url.set("https://github.com/Kronos-orm/Kronos-orm")
                    connection.set("scm:git:https://github.com/Kronos-orm/Kronos-orm.git")
                    developerConnection.set("scm:git:ssh://git@github.com:Kronos-orm/Kronos-orm.git")
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
            }
        }

        repositories {
            maven {
                name = "central"
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2")
                credentials {
                    username = System.getenv("OSSRH_USER")
                    password = System.getenv("OSSRH_PASSWORD")
                }
            }
            maven {
                name = "snapshot"
                url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
                credentials {
                    username = System.getenv("OSSRH_USER")
                    password = System.getenv("OSSRH_PASSWORD")
                }
            }
            mavenLocal()
        }
    }
}

signing {
    //sign publishing.publications.mavenJava
    sign(publishing.publications["dist"])
}