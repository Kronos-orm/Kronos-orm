plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kronos.publishing)
    alias(libs.plugins.kronos.dokka)
    alias(libs.plugins.kover)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    kotlinCompilerPluginClasspathTest(project(":kronos-compiler-plugin"))
    testImplementation(libs.kotlin.test)
    testImplementation(libs.gson)
    testImplementation(libs.mockk)
    testImplementation(libs.ktx.datetime)
    testImplementation(libs.bundles.ktx.serialization)
    testImplementation(libs.kotlin.reflect)
}
