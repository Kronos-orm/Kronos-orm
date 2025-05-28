plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kronos.publishing)
    alias(libs.plugins.kronos.dokka)
    alias(libs.plugins.kover)
}

dependencies {
    implementation(project(":kronos-core"))
    implementation(libs.jackson.dataformat.toml)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.dbcp2)
    testImplementation(libs.driver.jdbc.mysql)
}
