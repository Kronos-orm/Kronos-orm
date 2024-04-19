plugins {
    kotlin("jvm") version "1.9.23"
}

group = "com.kotoframework"
version = file("koto.version").readText().trim()

repositories {
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