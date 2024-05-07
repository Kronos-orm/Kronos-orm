plugins {
    kotlin("jvm")
    kotlin("kapt")
 //   id("com.kotlinorm.kronos-compiler-plugin")
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