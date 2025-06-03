/**
 * Copyright 2022-2025 kronos-orm
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
package com.kotlinorm.codegen

/**
 * Configure the parameters related to code generation output
 *
 * @property targetDir Target directory, used to store generated code files.
 * @property packageName Package name, specifies the package path for generating code, optional.
 * @property tableCommentLineWords The line character limit for table comments is optional.
 */
class OutputConfig(
    val targetDir: String,
    val packageName: String?,
    val tableCommentLineWords: Int?,
)