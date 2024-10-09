plugins {
    id("kronos.jvm")
    id("kronos.publishing")
}

description = "Kronos logging plugin, supports the most common log types."

dependencies {
    compileOnly(project(":kronos-core"))
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
}
