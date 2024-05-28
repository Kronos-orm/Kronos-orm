package com.kotlinorm.buildSrc

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class SelectFromNGeneratorTask : DefaultTask() {
    @TaskAction
    fun runTask() {
        println("Running custom task")
    }
}