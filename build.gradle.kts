import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.11.0")
        classpath("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.11.0")
    }
    configurations.classpath {
        resolutionStrategy {
            force(
                "org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.11.0",
                "org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.11.0",
            )
        }
    }
}

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kapt)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kronos.dokka)
    alias(libs.plugins.kronos.publishing)
    alias(libs.plugins.kover)
}

dependencies {
    kover(project(":kronos-core"))
    kover(project(":kronos-syntax"))
    kover(project(":kronos-compiler-plugin"))
    kover(project(":kronos-codegen"))
    kover(project(":kronos-testing"))
}

allprojects {
    configurations.configureEach {
        resolutionStrategy.dependencySubstitution {
            substitute(module("com.kotlinorm:kronos-compiler-plugin")).using(project(":kronos-compiler-plugin"))
        }
    }

    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
    }

    tasks.withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
            apiVersion.set(KotlinVersion.KOTLIN_2_4)
            languageVersion.set(KotlinVersion.KOTLIN_2_4)
            freeCompilerArgs.add("-Xcollection-literals")
            freeCompilerArgs.add("-Xcontext-sensitive-resolution")
        }
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.addAll(listOf("--release", "8"))
    }
}
