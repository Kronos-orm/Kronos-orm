plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.dsl)
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.maven.publish)
    implementation(libs.dokka)
}

apply(from = "scripts/codegen.gradle.kts")
