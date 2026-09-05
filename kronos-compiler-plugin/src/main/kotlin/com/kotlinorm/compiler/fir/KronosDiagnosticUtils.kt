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

package com.kotlinorm.compiler.fir

import kotlin.reflect.KClass

internal fun kronosDiagnosticPsiElementClass(): KClass<*> {
    val classNames = listOf(
        "com.intellij.psi.PsiElement",
        "org.jetbrains.kotlin.com.intellij.psi.PsiElement",
    )
    val psiClass = classNames.firstNotNullOfOrNull { name ->
        runCatching { Class.forName(name).kotlin }.getOrNull()
    }
    return requireNotNull(psiClass) { "Unable to find IntelliJ PsiElement class for Kronos diagnostics" }
}
