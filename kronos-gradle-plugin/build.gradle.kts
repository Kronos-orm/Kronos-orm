import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:2.3.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${file("../kotlin.version").readText().trim()}")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

plugins {
    id("kronos.jvm")
    id("java-gradle-plugin")
    kotlin("kapt")
    id("kronos.publishing")
}

description = "Gradle plugin provided by kronos for parsing SQL Criteria expressions at compile time."

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api")
    api(project(":kronos-compiler-plugin"))
}

gradlePlugin {
    plugins {
        create("kronosCompilerPlugin") {
            id = "com.kotlinorm.kronos-gradle-plugin"
            implementationClass = "com.kotlinorm.plugins.KronosGradlePlugin"
        }
    }
}
