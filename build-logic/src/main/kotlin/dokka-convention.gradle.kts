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
    tasks.register("dokkaGenerateAll") {
        group = "dokka"
        doFirst {
            project.rootProject.layout.buildDirectory.dir("dokka").get().asFile.deleteRecursively()
        }
        dependsOn(project.tasks.named("dokkaGeneratePublicationHtml"))
        doLast {
            project.subprojects.forEach { subproject ->
                if (subproject.plugins.hasPlugin("org.jetbrains.dokka")) {
                    val dokkaDir = subproject.layout.buildDirectory.dir("dokka/html").get().asFile
                    if (dokkaDir.exists()) {
                        val targetDir =
                            project.rootProject.layout.buildDirectory.dir("dokka/${subproject.name}").get().asFile
                        dokkaDir.copyRecursively(targetDir, overwrite = true)
                    }
                }
            }
            project.rootProject.layout.buildDirectory.dir("dokka/html").get().asFile.deleteOnExit()
        }
    }
} else {
    tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
        dokkaSourceSets {
            configureEach {
                includes.from("README.md")
                reportUndocumented.set(true)
                jdkVersion.set(17)
                sourceRoots.from(file("src/main/kotlin"))
            }
        }
    }
}
