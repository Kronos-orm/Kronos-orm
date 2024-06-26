plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") apply false
    id("com.vanniktech.maven.publish") apply false
//    id("com.kotlinorm.kronos-compiler-plugin")
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(kotlin("reflect"))
    testImplementation(project(":kronos-core"))
    testImplementation(project(":kronos-logging"))
    testImplementation(project(":kronos-jvm-driver-wrapper"))
    testImplementation("commons-dbcp:commons-dbcp:1.4")
    // mysql
    testImplementation("com.mysql:mysql-connector-j:8.4.0")
    // sqlite
    testImplementation("org.xerial:sqlite-jdbc:3.46.0.0")
    // SQLServer
    testImplementation("com.microsoft.sqlserver:mssql-jdbc:12.7.0.jre8-preview")
    // postgresql
    testImplementation("org.postgresql:postgresql:42.7.3")
    // oracle
    testImplementation("com.oracle.database.jdbc:ojdbc8:23.2.0.0")
    testImplementation("com.google.code.gson:gson:2.11.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}