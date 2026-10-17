import kronos.kronosNativeTargets

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.kronos.publishing)
    alias(libs.plugins.kronos.dokka)
}

kotlin {
    jvm()
    kronosNativeTargets()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":kronos-core"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotlin.reflect)
                implementation(libs.kotlin.test)
            }
        }
        val iosSimulatorArm64Test by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

// Preserve the task name used by existing CI and consumer scripts.
tasks.register("test") {
    group = "verification"
    dependsOn("jvmTest")
}
