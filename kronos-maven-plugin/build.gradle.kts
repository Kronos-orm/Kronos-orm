
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("kronos.jvm")
    kotlin("kapt")
    id("kronos.publishing")
}

description = "Maven plugin provided by kronos for parsing SQL Criteria expressions at compile time."

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

dependencies {
    implementation(kotlin("maven-plugin"))
    api(project(":kronos-compiler-plugin"))
    implementation("org.apache.maven:maven-core:3.9.9")
}

// A bit of a hack to copy over the META-INF services information so that Maven knows about the NullDefaultsComponentRegistrar
val servicesDirectory = "META-INF/services"
val copyServices =
    tasks.register<Copy>("copyServices") {
        dependsOn(":kronos-compiler-plugin:kaptKotlin")
        val kotlinPlugin = project(":kronos-compiler-plugin")
        from(kotlinPlugin.kaptGeneratedServicesDir)
        into(kaptGeneratedServicesDir)
    }

tasks.withType<KotlinCompile> {
    dependsOn(copyServices)
    compilerOptions {
        freeCompilerArgs.add("-Xopt-in=kotlin.RequiresOptIn")
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs = listOf("-Xinline-classes")
}

val Project.kaptGeneratedServicesDir: File
    get() =
        Kapt3GradleSubplugin.getKaptGeneratedClassesDir(this, sourceSets.main.get().name).resolve(
            servicesDirectory
        )
