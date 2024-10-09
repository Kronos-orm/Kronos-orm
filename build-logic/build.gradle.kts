import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${file("../kotlin.version").readText().trim()}")
    implementation("com.vanniktech.maven.publish:com.vanniktech.maven.publish.gradle.plugin:latest.release")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:latest.release")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}
