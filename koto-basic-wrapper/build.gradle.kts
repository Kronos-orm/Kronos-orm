plugins {
    kotlin("jvm")
}

group = "com.kotoframework"
version = "2.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":koto-core"))
    testImplementation("commons-dbcp:commons-dbcp:1.4")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(8)
}