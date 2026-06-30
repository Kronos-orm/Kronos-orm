plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kronos.publishing)
    alias(libs.plugins.kronos.dokka)
    alias(libs.plugins.kover)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-nowarn")
        freeCompilerArgs.add("-Xcollection-literals")
    }
}

dependencies {
    implementation(project(":kronos-core"))
    implementation(libs.jackson.dataformat.toml)
    testImplementation(project(":kronos-jdbc-wrapper"))
    testImplementation(libs.kotlin.test)
    testImplementation(libs.dbcp2)
    testImplementation(libs.driver.jdbc.mysql)
}
