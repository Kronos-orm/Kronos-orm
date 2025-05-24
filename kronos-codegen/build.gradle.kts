plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kronos.publishing)
    alias(libs.plugins.kronos.dokka)
}

dependencies {
    implementation(project(":kronos-core"))
    implementation(project(":kronos-jdbc-wrapper"))
    implementation(libs.toml4j)
    testImplementation(libs.kotlin.test)
}
