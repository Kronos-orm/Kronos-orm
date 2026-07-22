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

package com.kotlinorm.exceptions

/**
 * Indicates that result metadata contains labels which differ only by case.
 *
 * JDBC labels are driver-dependent in their case normalization, so silently
 * selecting one of these entries would make typed result mapping nondeterministic.
 */
class ConflictingResultColumnLabels(val labels: List<String>) : IllegalArgumentException(
    "Result column labels must be unique ignoring case: ${labels.joinToString()}")
