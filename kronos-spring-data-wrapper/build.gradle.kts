plugins {
    kotlin("jvm")
}

group = "com.kotlinorm"
version = "2.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":kronos-core"))
    testImplementation(project(":kronos-core"))
    testImplementation(kotlin("test"))
    compileOnly("org.springframework:spring-jdbc:5.3.23")
    compileOnly("org.springframework:spring-tx:5.3.23")
    compileOnly("org.springframework:spring-beans:5.3.23")
    compileOnly("org.springframework:spring-core:6.1.3")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(8)
}