package kronos

/*
 * This plugin configures the Dokka documentation generator.
 *
 * To use it, add
 * ```
 * plugins {
 *     id("kronos.dokka")
 * }
 * ```
 * in your build.gradle.kts file.
 */

plugins {
    id("org.jetbrains.dokka")
}

@Suppress("IMPLICIT_CAST_TO_ANY")
if (project == project.rootProject) {
    val dokkaAggregate = tasks.register("dokkaGenerateAll") {
        group = "dokka"
        val root = project.rootProject.layout.projectDirectory.dir("docs").asFile
        doFirst {
            root.deleteRecursively()
            root.mkdirs()
        }
        doLast {
            project.subprojects.forEach { subproject ->
                if (subproject.plugins.hasPlugin("org.jetbrains.dokka")) {
                    val dokkaDir = subproject.layout.buildDirectory.dir("dokka/html").get().asFile
                    if (dokkaDir.exists()) {
                        println("${subproject.name} build successful, copying documentation to ${root.resolve(subproject.name)}")
                        val targetDir = root.resolve(subproject.name).also { it.mkdirs() }
                        dokkaDir.copyRecursively(targetDir, overwrite = true)
                        dokkaDir.deleteRecursively()
                    } else {
                        println("${subproject.name} build failed, skipping documentation")
                    }
                }
            }
        }
    }

    // Subprojects are evaluated after the root project. Register the dependency
    // when Dokka is actually applied so KMP publications cannot be skipped.
    gradle.projectsEvaluated {
        project.subprojects.forEach { evaluatedProject ->
            if (evaluatedProject.plugins.hasPlugin("org.jetbrains.dokka")) {
                dokkaAggregate.configure {
                    dependsOn(evaluatedProject.tasks.named("dokkaGeneratePublicationHtml"))
                }
            }
        }
    }
} else {
    // DGP v2 configuration using top-level dokka {} DSL
    configure<org.jetbrains.dokka.gradle.DokkaExtension> {
        dokkaSourceSets.configureEach {
            includes.from("README.md")
            reportUndocumented.set(true)
            jdkVersion.set(17)
            // Kotlin Multiplatform source roots are supplied by the Kotlin
            // plugin (commonMain, jvmMain, and Native target source sets).
            // JVM-only modules retain the historical src/main/kotlin layout.
            if (!project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
                sourceRoots.from(file("src/main/kotlin"))
            }
        }
    }
}
