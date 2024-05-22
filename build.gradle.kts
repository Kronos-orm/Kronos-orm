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
    testImplementation(project(":kronos-jvm-basic-wrapper"))
    testImplementation("commons-dbcp:commons-dbcp:1.4")
    testImplementation("com.mysql:mysql-connector-j:8.4.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}