import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktx.serialization)
    alias(libs.plugins.kronos.gradle)
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
    implementation(project(":kronos-syntax"))
    testImplementation(libs.kotlin.test)
    testImplementation(libs.gson)
    testImplementation(libs.mockk)
    testImplementation(libs.ktx.datetime)
    testImplementation(libs.bundles.ktx.serialization)
    testImplementation(libs.kotlin.reflect)
}

kover {
    reports {
        total {
            verify {
                rule("kronos-core coverage guard") {
                    minBound(90, CoverageUnit.LINE)
                    minBound(70, CoverageUnit.BRANCH)
                }
            }
        }
    }
}
