import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kapt)
    alias(libs.plugins.kronos.publishing)
    alias(libs.plugins.kronos.dokka)
    alias(libs.plugins.kover)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs = listOf("-Xmx2048m")
}
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
        freeCompilerArgs.add("-Xcontext-sensitive-resolution")
    }
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xannotation-default-target=param-property"))
}

dependencies {
    compileOnly(libs.kotlin.compiler.embeddable)
    compileOnly(libs.auto.service)
    kapt(libs.auto.service)
    testImplementation(libs.kotlin.test)
    testImplementation(project(":kronos-core"))
    testImplementation(libs.kct)
}