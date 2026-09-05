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

import com.intellij.lang.annotation.HighlightSeverity
import org.jetbrains.kotlin.psi.KtFile

class KronosProjectionRealFixtureTest : KronosIdeaFixtureTestCase() {
    fun testRealFirPluginHighlightsDuplicateJoinProjection() {
        val file = myFixture.configureByText(
            "DuplicateJoinProjection.kt",
            """
            import com.kotlinorm.annotations.Table
            import com.kotlinorm.interfaces.KPojo
            import com.kotlinorm.orm.join.join

            @Table("tb_idea_user")
            data class IdeaUser(
                var id: Int? = null,
                var companyId: Int? = null,
            ) : KPojo

            @Table("tb_idea_company")
            data class IdeaCompany(
                var id: Int? = null,
            ) : KPojo

            fun duplicateJoinProjection() {
                IdeaUser().join(IdeaCompany()) { user, company ->
                    leftJoin { user.companyId == company.id }
                        .select { [user.id, company.id] }
                }
            }
            """.trimIndent(),
        ) as KtFile

        assertKronosFirPluginLoaded(file)

        val documentText = myFixture.editor.document.text
        val conflictingIdStart = documentText.indexOf(
            "company.id]",
            startIndex = documentText.indexOf(".select"),
        ) +
            "company.".length
        assertTrue("Could not locate the conflicting projection field", conflictingIdStart >= "company.".length)
        val conflictingIdEnd = conflictingIdStart + "id".length
        val matchingErrors = myFixture.doHighlighting().filter { highlight ->
            highlight.severity == HighlightSeverity.ERROR &&
                highlight.startOffset == conflictingIdStart &&
                highlight.endOffset == conflictingIdEnd
        }

        assertEquals(
            "Expected exactly one compiler-plugin diagnostic on the second duplicate projection field",
            1,
            matchingErrors.size,
        )
        val diagnosticText = listOfNotNull(
            matchingErrors.single().description,
            matchingErrors.single().toolTip,
        ).joinToString("\n")
        assertTrue(
            "Expected UnsafeProjectionOverride opt-in diagnostic, but was: $diagnosticText",
            diagnosticText.contains("Projection output names conflict") ||
                diagnosticText.contains("UnsafeProjectionOverride"),
        )
    }
}
