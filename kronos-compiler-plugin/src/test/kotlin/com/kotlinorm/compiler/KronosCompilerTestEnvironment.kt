/**
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotlinorm.compiler

import java.io.File
import java.net.URLClassLoader

/** Supplies the official compiler test framework from the actual JUnit runtime. */
internal object KronosCompilerTestEnvironment {
    val projectDir: File by lazy {
        System.getProperty("kronos.compiler.plugin.projectDir")?.let(::File)
            ?: generateSequence(File(System.getProperty("user.dir")).absoluteFile, File::getParentFile)
                .flatMap { sequenceOf(it, File(it, "kronos-compiler-plugin")) }
                .firstOrNull { File(it, "testData/box").isDirectory }
            ?: error("Cannot locate kronos-compiler-plugin/testData from the test working directory")
    }

    val classpath: List<File> by lazy {
        System.getProperty("kronos.compiler.test.classpath")
            ?.split(File.pathSeparator)?.filter(String::isNotBlank)?.map(::File)
            ?: buildList {
                System.getProperty("java.class.path").split(File.pathSeparator)
                    .filter(String::isNotBlank).mapTo(this, ::File)
                listOf(Thread.currentThread().contextClassLoader, javaClass.classLoader).forEach { initial ->
                    generateSequence(initial, ClassLoader::getParent).forEach { loader ->
                        if (loader is URLClassLoader) loader.urLs
                            .filter { it.protocol == "file" }.mapTo(this) { File(it.toURI()) }
                    }
                }
            }.distinctBy { it.absolutePath }
    }

    fun prepare() {
        listOf("kotlin-stdlib", "kotlin-test", "kotlin-reflect", "kotlin-script-runtime", "kotlin-annotations-jvm")
            .forEach { name ->
                val property = "org.jetbrains.kotlin.test.$name"
                if (System.getProperty(property) == null) {
                    val jar = classpath.firstOrNull { it.name.matches(Regex("${Regex.escape(name)}-\\d.*\\.jar")) }
                        ?: error("Missing $name in the compiler test runtime classpath")
                    System.setProperty(property, jar.absolutePath)
                }
            }
        listOf("home", "config", "system", "plugins").forEach { directory ->
            val property = "idea.$directory.path"
            if (System.getProperty(property) == null) {
                val path = File(projectDir, "build/idea-test/$directory")
                check(path.isDirectory || path.mkdirs()) { "Cannot create compiler test directory $path" }
                System.setProperty(property, path.absolutePath)
            }
        }
    }
}
