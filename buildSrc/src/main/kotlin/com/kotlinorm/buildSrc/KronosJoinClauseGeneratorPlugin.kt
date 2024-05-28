package com.kotlinorm.buildSrc

import org.gradle.api.Plugin
import org.gradle.api.Project

class KronosJoinClauseGeneratorPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val task = project.tasks.create("selectFromNGeneratorTask", SelectFromNGeneratorTask::class.java)

        project.extensions.create(
            "SelectFromNGeneratorExtension",
            SelectFromNGeneratorExtension::class.java,
            task
        )
    }
}

open class SelectFromNGeneratorExtension(val generateSelectFrom: SelectFromNGeneratorTask)