import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kronos.publishing)
    alias(libs.plugins.kronos.dokka)
    alias(libs.plugins.kover)
}

dependencies {
    testImplementation(libs.kotlin.test)
}

kover {
    reports {
        total {
            verify {
                rule("kronos-syntax coverage guard") {
                    minBound(90, CoverageUnit.LINE)
                    minBound(70, CoverageUnit.BRANCH)
                }
            }
        }
    }
}
