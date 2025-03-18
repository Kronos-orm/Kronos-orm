plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kronos.publishing)
    alias(libs.plugins.kronos.dokka)
}

dependencies {
    compileOnly(project(":kronos-core"))
    implementation(libs.kotlin.reflect)
    testImplementation(libs.kotlin.test)
}
