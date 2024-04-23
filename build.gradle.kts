plugins {
    kotlin("jvm") version "1.9.23"
    id("com.kotoframework.koto-k2-compiler-plugin") version "2.0.0-SNAPSHOT"
}

group = "com.kotoframework"
version = file("koto.version").readText().trim()

repositories {
    mavenLocal()
    mavenCentral()
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