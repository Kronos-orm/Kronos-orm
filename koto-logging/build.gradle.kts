plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":koto-core"))
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(8)
}