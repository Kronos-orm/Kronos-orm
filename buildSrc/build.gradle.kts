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
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}


apply(from = "generate-join-clause.gradle.kts")

@Suppress("UNCHECKED_CAST")
val generateJoin = extra["export"] as () -> Unit

tasks.create("generateJoinClause") {
    doLast {
        generateJoin()
    }
}