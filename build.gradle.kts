allprojects {
    group = "com.kotlinorm"
    version = File(rootDir, "kronos.version").readText().trim()

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://kotlin.bintray.com/kotlinx")
    }
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "com.vanniktech.maven.publish")
}

plugins {
    kotlin("jvm")
    id("com.kotlinorm.kronos-compiler-plugin")
    id("org.jetbrains.dokka") version "latest.release"
    id("com.vanniktech.maven.publish") version "latest.release"
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(project(":kronos-core"))
    testImplementation(project(":kronos-logging"))
    testImplementation(project(":kronos-jvm-driver-wrapper"))
    testImplementation("commons-dbcp:commons-dbcp:1.4")
    testImplementation("com.mysql:mysql-connector-j:8.4.0")
    testImplementation("com.google.code.gson:gson:2.11.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}