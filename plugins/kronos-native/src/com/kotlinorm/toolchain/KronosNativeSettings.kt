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

import java.nio.file.Path
import org.jetbrains.amper.plugins.Configurable

/** Inputs that the Toolchain plugin API does not expose as Native artifact requests. */
@Configurable
interface KronosNativeSettings {
    /** Runtime modules without a dependency on kronos-core disable source generation. */
    val generateMetadata: Boolean get() = true
    val dependencyMetadata: List<Path> get() = emptyList()
    /** Empty for JVM-only modules; @jvm for multiplatform modules. */
    val jvmResourceFragment: String get() = "@jvm"
    /** Stable source-set naming, independent of a publication projection's resource fragment. */
    val jvmSourceSetPrefix: String get() = "jvm"

    /** Optional existing compiler; otherwise the matching catalog-version official SDK is provisioned and SHA256-verified. */
    val compilerExecutable: Path?
    val sourceDirectories: Map<String, List<Path>> get() = emptyMap()
    val watchLibraries: List<Path> get() = emptyList()
    /** Root libs.versions.toml aliases whose actual watchos_x64 variants are resolved transitively. */
    val watchExternalDependencies: List<String> get() = emptyList()
    val compilerPlugin: Path?
    val compilerPluginId: String get() = "kronos-compiler-plugin"
    val compilerOptions: Map<String, String> get() = emptyMap()
    val freeCompilerArgs: List<String> get() = emptyList()

    /** Publishing runs against a private unsigned Maven stage before signatures are created. */
    val stageRepository: Path?
    /** Initial stage populated by a preceding CLI publication, outside this task graph. */
    val bootstrapStageRepository: Path?
    val publicationTemplateTarget: String get() = "macosX64"
}
