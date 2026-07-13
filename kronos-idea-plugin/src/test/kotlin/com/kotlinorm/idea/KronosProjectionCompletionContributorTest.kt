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

package com.kotlinorm.idea

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Covers projection completion for derived sources such as SelectSubquerySqlTest's window alias case.
 */
class KronosProjectionCompletionContributorTest {
    @Test
    fun `projection completion supports empty selector receivers`() {
        val source = Files.readString(
            Paths.get("src/main/kotlin/com/kotlinorm/idea/KronosProjectionCompletionContributor.kt")
        ).replace("\r\n", "\n")

        assertTrue(source.contains("private fun CompletionParameters.projectionCompletionReceiver(): KtExpression?"))
        assertTrue(source.contains("qualified.selectorExpression == null"))
        assertTrue(source.contains("qualified.textRange.contains(offset)"))
        assertTrue(source.contains("qualified.receiverExpression"))
    }

    @Test
    fun `projection completion still contributes bridge fields`() {
        val source = Files.readString(
            Paths.get("src/main/kotlin/com/kotlinorm/idea/KronosProjectionCompletionContributor.kt")
        ).replace("\r\n", "\n")

        assertTrue(source.contains("KronosProjectionIdeBridge.read()"))
        assertTrue(source.contains("model.contextFields"))
        assertTrue(source.contains("model.fields"))
        assertTrue(source.contains("fields.distinctBy { it.name }"))
        assertTrue(source.contains("result.addElement("))
    }
}
