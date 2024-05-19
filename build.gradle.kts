allprojects {
    group = "com.kotlinorm"
    version = File(rootDir, "kronos.version").readText().trim()

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://kotlin.bintray.com/kotlinx")
    }
}

plugins {
    kotlin("jvm")
    id("com.kotlinorm.kronos-compiler-plugin")
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(project(":kronos-core"))
    testImplementation(project(":kronos-logging"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}