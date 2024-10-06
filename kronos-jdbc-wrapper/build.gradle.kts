plugins {
    kotlin("jvm")
    id("kronos.publishing")
}

description = "Kronos 's built-in database operation plug-in based on the original jdbc supports variable templates and multiple databases."

dependencies {
    compileOnly(project(":kronos-core"))
    implementation(kotlin("reflect"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}
