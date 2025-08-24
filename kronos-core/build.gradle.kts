plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kronos.publishing)
    alias(libs.plugins.kronos.dokka)
    alias(libs.plugins.kover)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-nowarn")
    }
}

dependencies {
    kotlinCompilerPluginClasspathTest(project(":kronos-compiler-plugin"))
    testImplementation(libs.kotlin.test)
    testImplementation(libs.gson)
    testImplementation(libs.mockk)
    testImplementation(libs.ktx.datetime)
    testImplementation(libs.bundles.ktx.serialization)
    testImplementation(libs.kotlin.reflect)
}
