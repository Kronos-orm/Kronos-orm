plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.kapt")
}

dependencies {
    implementation("com.alibaba.fastjson2:fastjson2:2.0.49")
    implementation("com.alibaba.fastjson2:fastjson2-kotlin:2.0.49")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(8)
}