import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kapt)
    alias(libs.plugins.ktx.serialization)
    alias(libs.plugins.kronos.publishing)
    alias(libs.plugins.kronos.dokka)
    alias(libs.plugins.kover)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs = listOf("-Xmx2048m")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-sensitive-resolution")
        freeCompilerArgs.add("-Xsuppress-deprecated-jvm-target-warning")
        freeCompilerArgs.add("-Xskip-prerelease-check")
        freeCompilerArgs.add("-Xallow-unstable-dependencies")
        allWarningsAsErrors.set(false)
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xsuppress-version-warnings")
        freeCompilerArgs.add("-Xskip-prerelease-check")
        allWarningsAsErrors.set(true)
    }
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    compilerOptions {
        freeCompilerArgs.set(listOf(
            "-Xcontext-sensitive-resolution",
            "-Xcollection-literals"
        ))
    }
}

dependencies {
    compileOnly(libs.kotlin.compiler.embeddable)
    compileOnly(libs.auto.service)
    kapt(libs.auto.service)
    
    implementation(libs.bundles.ktx.serialization)
    
    testImplementation(libs.kotlin.test)
    testImplementation(project(":kronos-core"))
    testImplementation(libs.kct)
}

kover {
    reports {
        total {
            html {
                onCheck = true
            }
            verify {
                rule {
                    minBound(80)
                }
            }
        }
    }
}
