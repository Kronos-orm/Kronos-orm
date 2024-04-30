plugins {
    kotlin("jvm")
    kotlin("kapt")
}

dependencies {
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(8)
}