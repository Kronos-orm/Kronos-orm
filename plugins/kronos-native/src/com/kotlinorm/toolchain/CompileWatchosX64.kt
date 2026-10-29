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

package com.kotlinorm.toolchain

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.Output
import org.jetbrains.amper.plugins.TaskAction

/** Compiles the target present in Kotlin 2.4.21 but absent from the Toolchain 0.13.0 platform enum. */
@TaskAction
fun compileWatchosX64(
    moduleName: String,
    group: String?,
    generateMetadata: Boolean,
    @Input compilerExecutable: Path?,
    @Input sourceDirectories: Map<String, List<Path>>,
    @Input generatedSources: Path,
    @Input libraries: List<Path>,
    @Input resolvedDependencies: Path,
    @Input compilerPlugin: Path?,
    compilerPluginId: String,
    compilerOptions: Map<String, String>,
    freeCompilerArgs: List<String>,
    @Output outputDirectory: Path
) {
    val names = listOf("common", "native", "apple", "watchos", "watchosX64")
    require(sourceDirectories.isNotEmpty() && sourceDirectories.keys.all { it in names }) {
        "sourceDirectories must map common/native/apple/watchos/watchosX64 to real source directories"
    }
    val fragments = names.associateWith { name ->
        val directories = sourceDirectories[name].orEmpty() + if (name == "common") listOf(generatedSources) else emptyList()
        directories.flatMap { directory ->
            require(Files.isDirectory(directory)) { "Source directory does not exist: $directory" }
            Files.walk(directory).use { stream ->
                stream.iterator().asSequence().filter {
                    Files.isRegularFile(it) && it.fileName.toString().endsWith(".kt")
                }.sortedBy { it.toString() }.toList()
            }
        }.distinct()
    }
    require(fragments.getValue("common").isNotEmpty()) { "Common sources are required" }
    val allSources = fragments.values.flatten()
    require(allSources.distinct().size == allSources.size) { "A source file belongs to multiple fragments" }
    val kotlinVersion = resolvedDependencies.resolve("kotlin-version.txt").toFile().readText().trim()
    val plugin = compilerPlugin ?: resolvedDependencies.resolve("compiler-plugin.jar").takeIf(Files::isRegularFile)
    val externalLibraries = Files.list(resolvedDependencies).use { stream ->
        stream.iterator().asSequence().filter { it.fileName.toString().endsWith(".klib") }.sortedBy(Path::toString).toList()
    }
    val options = compilerOptions.toMutableMap()
    if (generateMetadata) {
        requireNotNull(plugin) { "Native metadata generation requires the Kronos compiler plugin" }
        options[MODULE_COORDINATE_OPTION_NAME] = "${requireNotNull(group)}:$moduleName"
    }
    val compiler = compilerExecutable ?: provisionKotlinNativeCompiler(outputDirectory.resolve("compiler"), kotlinVersion)
    require(Files.isRegularFile(compiler)) { "Kotlin/Native compiler does not exist: $compiler" }
    val versionProcess = compilerProcess(listOf(compiler.toString(), "-version")).redirectErrorStream(true).start()
    val version = versionProcess.inputStream.bufferedReader().use { it.readText() }
    check(versionProcess.waitFor() == 0 && Regex("\\bkotlinc-native\\s+${Regex.escape(kotlinVersion)}(?:\\s|$)").containsMatchIn(version)) {
        "Expected Kotlin/Native $kotlinVersion: $version"
    }
    val output = outputDirectory.resolve("$moduleName-watchosx64.klib")
    outputDirectory.toFile().mkdirs()
    val arguments = buildList {
        add(compiler.toString())
        addAll(listOf("-produce", "library", "-target", "watchos_x64", "-o", output.toString(), "-module-name=$moduleName", "-Xmulti-platform"))
        names.forEach { add("-Xfragments=$it") }
        names.zipWithNext().forEach { (parent, child) -> add("-Xfragment-refines=$child:$parent") }
        fragments.forEach { (fragment, files) -> files.forEach { add("-Xfragment-sources=$fragment:$it") } }
        (libraries + externalLibraries).distinct().forEach { library ->
            require(Files.exists(library)) { "Dependency KLIB does not exist: $library" }
            addAll(listOf("-library", library.toString()))
        }
        plugin?.let {
            require(Files.isRegularFile(it)) { "Compiler plugin does not exist: $it" }
            add("-Xplugin=$it")
        }
        options.forEach { (key, value) -> addAll(listOf("-P", "plugin:$compilerPluginId:$key=$value")) }
        addAll(freeCompilerArgs)
        addAll(allSources.map(Path::toString))
    }
    println(version.trim())
    check(compilerProcess(arguments).inheritIO().start().waitFor() == 0) { "watchos_x64 compilation failed" }
    require(Files.isRegularFile(output)) { "Compiler did not produce $output" }
    outputDirectory.resolve("compiler-version.txt").toFile().writeText(kotlinVersion)
    ZipOutputStream(Files.newOutputStream(outputDirectory.resolve("$moduleName-watchosx64-sources.jar"))).use { archive ->
        for ((fragment, files) in fragments) {
            val directories = sourceDirectories[fragment].orEmpty() +
                if (fragment == "common") listOf(generatedSources) else emptyList()
            for (file in files) {
                val directory = directories.first { file.startsWith(it) }
                val name = "${fragment}Main/${directory.relativize(file).toString().replace('\\', '/')}"
                archive.putNextEntry(ZipEntry(name).apply { time = 0 })
                Files.copy(file, archive)
                archive.closeEntry()
            }
        }
    }
}

/** Use the Toolchain-provisioned runtime rather than requiring another Java installation on PATH. */
private fun compilerProcess(arguments: List<String>): ProcessBuilder = ProcessBuilder(arguments).apply {
    environment()["JAVA_HOME"] = System.getProperty("java.home")
}
