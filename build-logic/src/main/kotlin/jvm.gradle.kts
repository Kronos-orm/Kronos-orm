package kronos

/*
 * This plugin configures Kotlin targeting the JVM for a project.
 *
 * To use it, add
 * ```
 * plugins {
 *     id("kronos.jvm")
 * }
 * ```
 * in your build.gradle.kts file.
 */

plugins {
    kotlin("jvm")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}
