import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("kronos.jvm")
    kotlin("kapt")
    id("kronos.publishing")
}

description = "Kotlin plugin provided by kronos for parsing SQL Criteria expressions at compile time."

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs = listOf("-Xmx2048m")
}

base.archivesName = "kronos-compiler-plugin"

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable")
    compileOnly("com.google.auto.service:auto-service:1.1.1")
    kapt("com.google.auto.service:auto-service:1.1.1")
    testImplementation(kotlin("test"))
    testImplementation(project(":kronos-core"))
    testImplementation("dev.zacsweers.kctfork:core:0.5.1")
}
