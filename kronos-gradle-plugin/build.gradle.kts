plugins {
    id("kronos.jvm")
    id("java-gradle-plugin")
    id("kronos.publishing")
}

group = "com.kotlinorm"
version = file("../kronos.version").readText().trim()
description = "Gradle plugin provided by kronos for parsing SQL Criteria expressions at compile time."

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api")
}

gradlePlugin {
    plugins {
        create("kronosCompilerPlugin") {
            id = "com.kotlinorm.kronos-gradle-plugin"
            implementationClass = "com.kotlinorm.compiler.fir.KronosGradlePlugin"
        }
    }
}
