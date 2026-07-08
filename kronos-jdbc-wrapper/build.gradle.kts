plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kronos.publishing)
    alias(libs.plugins.kronos.dokka)
}

dependencies {
    compileOnly(project(":kronos-core"))
    testImplementation(project(":kronos-core"))
    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockk)
}
