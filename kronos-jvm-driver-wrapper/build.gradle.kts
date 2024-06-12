import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":kronos-core"))
    implementation(kotlin("reflect"))
    testImplementation(project(":kronos-core"))
    testImplementation("commons-dbcp:commons-dbcp:1.4")
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
        )
    )
    coordinates(project.group.toString(), project.name, project.version.toString())
    pom {
        name.set("${project.group}:${project.name}")
        description.set("Kronos 's built-in database operation plug-in based on the original jdbc supports variable templates and multiple databases.")
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
    if (!version.toString().endsWith("-SNAPSHOT")) {
        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
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

    signAllPublications()
}