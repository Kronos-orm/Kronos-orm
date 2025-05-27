plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kronos.publishing)
    alias(libs.plugins.kronos.dokka)
}

dependencies {
    implementation(project(":kronos-core"))
    implementation(libs.jackson.dataformat.toml)
    kotlinCompilerPluginClasspathTest(project(":kronos-compiler-plugin"))
    testImplementation(libs.kotlin.test)
    testImplementation(project(":kronos-jdbc-wrapper"))
    testImplementation(libs.dbcp2)
    testImplementation(libs.driver.jdbc.mysql)
}
