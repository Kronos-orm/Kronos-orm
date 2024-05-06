import org.jetbrains.kotlin.fir.declarations.builder.buildScript

allprojects {
    group = "com.kotoframework"
    version = File(rootDir, "kronos.version").readText().trim()

    repositories {
        mavenLocal()
        mavenCentral()
    }
}

plugins {
    kotlin("jvm")
    id("com.kotoframework.kronos-compiler-plugin")
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(project(":kronos-core"))
    testImplementation(project(":kronos-logging"))
    testImplementation(project(":kronos-basic-wrapper"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}