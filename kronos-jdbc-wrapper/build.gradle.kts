plugins {
    id("kronos.jvm")
    id("kronos.publishing")
}

description = "Kronos 's built-in database operation plug-in based on the original jdbc supports variable templates and multiple databases."

dependencies {
    compileOnly(project(":kronos-core"))
    implementation(kotlin("reflect"))
}
