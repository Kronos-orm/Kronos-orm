plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktx.serialization)
}

dependencies {
    kotlinCompilerPluginClasspathTest(project(":kronos-compiler-plugin"))
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