import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:2.3.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
    }
}

plugins {
    kotlin("jvm")
    id("signing")
    id("maven-publish")
    id("java-gradle-plugin")
    kotlin("kapt")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
    }
}

base.archivesName = "kronos-compiler-plugin"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
    implementation("com.google.auto.service:auto-service:1.1.1")
    kapt("com.google.auto.service:auto-service:1.1.1")
    testImplementation(kotlin("test"))
    testImplementation(project(":kronos-core"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("dev.zacsweers.kctfork:core:0.4.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

gradlePlugin {
    plugins {
        create("kronosCompilerPlugin") {
            id = "com.kotlinorm.kronos-compiler-plugin"
            implementationClass = "com.kotlinorm.plugins.KronosGradlePlugin"
        }
    }
}

//2. 设置发布相关配置
publishing {
    publications {
        //3. 将插件发布到 maven 仓库
        create<MavenPublication>("dist") {
            //4. 设置插件的 maven 坐标
            groupId = project.group.toString()
            artifactId = "kronos-compiler-plugin"
            version = project.version.toString()

            pom {
                name.set("${project.group}:${project.name}")
                description.set("A gradle plugin provided by kronos for parsing SQL Criteria expressions at compile time.")
                url.set("https://www.kotlinorm.com")
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                scm {
                    url.set("https://github.com/kotlinorm/kotlinorm")
                    connection.set("scm:git:https://github.com/kotlinorm/kotlinorm.git")
                    developerConnection.set("scm:git:ssh://git@github.com:kotlinorm/kotlinorm.git")
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
    }
    //5. 设置发布仓库
    repositories {
        // 6. 发布到本地 maven 仓库
        mavenLocal()
    }
}