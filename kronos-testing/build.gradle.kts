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
    testImplementation("org.apache.commons:commons-dbcp2:2.12.0")
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
    testImplementation("org.springframework:spring-jdbc:5.3.23")
    testImplementation("org.springframework:spring-tx:5.3.23")
    testImplementation("org.springframework:spring-beans:5.3.23")
    testImplementation("org.springframework:spring-core:5.3.23")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}