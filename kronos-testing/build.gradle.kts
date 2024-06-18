plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") apply false
    id("com.vanniktech.maven.publish") apply false
//    id("com.kotlinorm.kronos-compiler-plugin")
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(project(":kronos-core"))
    testImplementation(project(":kronos-logging"))
    testImplementation(project(":kronos-jvm-driver-wrapper"))
    testImplementation("commons-dbcp:commons-dbcp:1.4")
    testImplementation("com.mysql:mysql-connector-j:8.4.0")
    testImplementation("org.xerial:sqlite-jdbc:3.46.0.0")
    testImplementation("com.google.code.gson:gson:2.11.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}