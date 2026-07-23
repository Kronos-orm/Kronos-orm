plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kronos.gradle)
    alias(libs.plugins.ktx.serialization)
    alias(libs.plugins.kover)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-nowarn")
    }
}

dependencies {
    kover(project(":kronos-core"))
    kover(project(":kronos-codegen"))
    kover(project(":kronos-compiler-plugin"))
    testImplementation(project(":kronos-core"))
    testImplementation(project(":kronos-syntax"))
    testImplementation(project(":kronos-codegen"))
    testImplementation(project(":kronos-logging"))
    testImplementation(project(":kronos-jdbc-wrapper"))
    testImplementation(libs.kotlin.test)
    testImplementation(libs.bundles.ktx.serialization)
    testImplementation(libs.dbcp2)
    testImplementation(libs.driver.jdbc.mysql)
    testImplementation(libs.driver.jdbc.sqlite)
    testImplementation(libs.driver.jdbc.h2)
    testImplementation(libs.driver.jdbc.mssql)
    testImplementation(libs.driver.jdbc.postgresql)
    testImplementation(libs.driver.jdbc.oracle)
    testImplementation(libs.driver.jdbc.dameng)
}
