plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kronos.publishing)
    alias(libs.plugins.kronos.dokka)
}

dependencies {
    implementation(project(":kronos-core"))
    implementation(libs.toml4j)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.dbcp2)
    testImplementation(libs.driver.jdbc.mysql)
}
