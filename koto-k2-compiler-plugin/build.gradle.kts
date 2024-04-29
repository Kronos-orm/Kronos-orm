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
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.23")
    }
}

plugins {
    kotlin("jvm")
    id("signing")
    id("maven-publish")
    id("java-gradle-plugin")
    id("org.jetbrains.kotlin.kapt")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
    }
}

base.archivesName = "koto-k2-compiler-plugin"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
    implementation("com.google.auto.service:auto-service:1.1.1")
    kapt("com.google.auto.service:auto-service:1.1.1")
    testImplementation(kotlin("test"))
    testImplementation(project(":koto-core"))
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
        create("kotoK2CompilerPlugin") {
            id = "com.kotoframework.koto-k2-compiler-plugin"
            implementationClass = "com.kotoframework.plugins.KotoK2GradlePlugin"
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
            artifactId = "koto-k2-compiler-plugin"
            version = project.version.toString()

            pom {
                name.set("${project.group}:${project.name}")
                description.set("A gradle plugin provided by koto for parsing SQL Criteria expressions at compile time.")
                url.set("https://www.kotoframework.com")
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                scm {
                    url.set("https://github.com/kotoframework/kotoframework")
                    connection.set("scm:git:https://github.com/kotoframework/kotoframework.git")
                    developerConnection.set("scm:git:ssh://git@github.com:kotoframework/kotoframework.git")
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