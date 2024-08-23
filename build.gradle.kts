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
    id("org.jetbrains.dokka") version "latest.release"
    id("com.vanniktech.maven.publish")
}

kotlin {
    jvmToolchain(8)
}