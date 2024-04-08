plugins {
    kotlin("jvm")
}

group = "com.kotoframework"
version = file("../koto.version").readText().trim()

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation(project(":koto-core"))
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(8)
}