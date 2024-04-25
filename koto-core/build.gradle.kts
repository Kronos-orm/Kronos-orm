plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.kapt")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(8)
}