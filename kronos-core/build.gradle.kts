plugins {
    id("kronos.jvm")
    id("kronos.publishing")
}

description = "An easy-to-use, flexible, lightweight ORM framework designed for kotlin."

dependencies {
    api(kotlin("reflect"))
    testImplementation(kotlin("test"))
}
