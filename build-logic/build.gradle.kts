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
apply(from = "generate-join-clause.gradle.kts")

@Suppress("UNCHECKED_CAST")
val generateJoin = extra["export"] as () -> Unit

tasks.create("generateJoinClauses") {
    group = "publishing"
    description = "Generate join clauses"
    doLast {
        generateJoin()
    }
}

tasks.create("publishAllToAliMaven") {
    group = "publishing"
    description = "Publishes all the plugins to Aliyun Maven"
}

tasks.create("publishAllToMavenLocal") {
    group = "publishing"
    description = "Publishes all the plugins to Aliyun Maven"
}