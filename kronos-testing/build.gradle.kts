plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kronos.gradle.plugin)
    alias(libs.plugins.ktx.serialization)
}

dependencies {
    testImplementation(project(":kronos-core"))
    testImplementation(project(":kronos-logging"))
    testImplementation(project(":kronos-jdbc-wrapper"))
    testImplementation(libs.ktx.datetime)
    testImplementation(libs.bundles.ktx.serialization)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.reflect)
    testImplementation(libs.dbcp2)
    testImplementation(libs.driver.jdbc.mysql)
    testImplementation(libs.driver.jdbc.sqlite)
    testImplementation(libs.driver.jdbc.mssql)
    testImplementation(libs.driver.jdbc.postgresql)
    testImplementation(libs.driver.jdbc.oracle)
    testImplementation(libs.gson)
}

configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("com.kotlinorm:kronos-compiler-plugin")).using(project(":kronos-compiler-plugin"))
    }
}
