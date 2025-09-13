import org.gradle.kotlin.dsl.invoke
import org.jetbrains.dokka.gradle.DokkaTask


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
        val root = project.rootProject.layout.projectDirectory.dir("docs").asFile
        doFirst {
            root.deleteRecursively()
            root.mkdirs()
        }
        project.subprojects.forEach { subproject ->
            if (subproject.plugins.hasPlugin("org.jetbrains.dokka")) {
                dependsOn(subproject.tasks.named("dokkaGenerate"))
            }
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
} else {
    tasks.withType<DokkaTask>().configureEach {
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
