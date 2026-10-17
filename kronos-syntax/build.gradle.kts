import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kronos.kronosNativeTargets

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.kronos.publishing)
    alias(libs.plugins.kronos.dokka)
    alias(libs.plugins.kover)
}

kotlin {
    jvm()
    kronosNativeTargets()

    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        val jvmTest by getting {
            kotlin.srcDir("src/test/kotlin")
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

// Preserve the task name used by the 0.3.x CI and consumer scripts.
tasks.register("test") {
    group = "verification"
    dependsOn("jvmTest")
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
