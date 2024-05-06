plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":kronos-core"))
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(8)
}