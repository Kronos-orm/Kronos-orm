plugins {
    kotlin("jvm")
    id("kronos.publishing")
}

description = "Kronos logging plugin, supports the most common log types."

dependencies {
    compileOnly(project(":kronos-core"))
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(8)
}
