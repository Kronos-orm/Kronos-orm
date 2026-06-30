import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinCompilerTestRuntimeJars = mapOf(
    "org.jetbrains.kotlin.test.kotlin-stdlib" to "kotlin-stdlib",
    "org.jetbrains.kotlin.test.kotlin-test" to "kotlin-test",
    "org.jetbrains.kotlin.test.kotlin-script-runtime" to "kotlin-script-runtime",
    "org.jetbrains.kotlin.test.kotlin-annotations-jvm" to "kotlin-annotations-jvm",
)

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
    systemProperty("kronos.compiler.plugin.projectDir", projectDir.absolutePath)
    systemProperty("kronos.compiler.test.classpath", sourceSets.test.get().runtimeClasspath.asPath)
    kotlinCompilerTestRuntimeJars.forEach { (propertyName, jarName) ->
        setKotlinTestRuntimeJar(propertyName, jarName)
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-sensitive-resolution")
        freeCompilerArgs.add("-Xsuppress-deprecated-jvm-target-warning")
        freeCompilerArgs.add("-Xskip-prerelease-check")
        freeCompilerArgs.add("-Xallow-unstable-dependencies")
        freeCompilerArgs.add("-Xsuppress-version-warnings")
        freeCompilerArgs.add("-Xskip-prerelease-check")
        freeCompilerArgs.add("-Xcollection-literals")
        allWarningsAsErrors.set(false)
    }
}

dependencies {
    compileOnly(libs.kotlin.compiler.embeddable)
    compileOnly(libs.auto.service)
    kapt(libs.auto.service)
    
    implementation(libs.bundles.ktx.serialization)
    
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.compiler)
    testImplementation(libs.kotlin.compiler.internal.test.framework)
    testImplementation(project(":kronos-core"))
    testRuntimeOnly(libs.kotlin.script.runtime)
    testRuntimeOnly(libs.kotlin.annotations.jvm)
}

fun Test.setKotlinTestRuntimeJar(propertyName: String, jarName: String) {
    val jar = sourceSets.test.get().runtimeClasspath.files
        .firstOrNull { it.name.matches("""$jarName-\d.*\.jar""".toRegex()) }
        ?: return
    systemProperty(propertyName, jar.absolutePath)
}

kover {
    reports {
        total {
            filters {
                excludes {
                    packages("com.kotlinorm.compiler.plugin.fir")
                }
            }
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
