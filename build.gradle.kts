plugins {
    kotlin("jvm")
    id("com.kotoframework.koto-k2-compiler-plugin") version "2.0.0-SNAPSHOT"
}

allprojects {
    group = "com.kotoframework"
    version = "2.0.0-SNAPSHOT"

    repositories {
        mavenLocal()
        mavenCentral()
    }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(project(":koto-core"))
    testImplementation(project(":koto-logging"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}