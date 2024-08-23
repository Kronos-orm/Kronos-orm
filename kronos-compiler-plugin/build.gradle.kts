import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("kapt")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
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

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

kronosPublishing(
    mavenPublishing,
    publishing,
    KotlinJvm(JavadocJar.Dokka("dokkaHtml"), sourcesJar = true),
    "Kotlin plugin provided by kronos for parsing SQL Criteria expressions at compile time."
)