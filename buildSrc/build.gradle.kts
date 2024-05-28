plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
}


apply(from = "generate-join-clause.gradle.kts")

@Suppress("UNCHECKED_CAST")
val generateJoin = extra["export"] as () -> Unit

tasks.create("generateJoinClause") {
    doLast {
        generateJoin()
    }
}