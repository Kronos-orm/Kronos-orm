plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kronos.publishing)
    alias(libs.plugins.kronos.dokka)
}

dependencies {
    kotlinCompilerPluginClasspathTest(project(":kronos-compiler-plugin"))
    testImplementation(libs.kotlin.test)
    testImplementation(libs.gson)
}
