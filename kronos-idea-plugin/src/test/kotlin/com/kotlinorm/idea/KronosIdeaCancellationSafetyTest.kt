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

import com.intellij.openapi.progress.ProcessCanceledException
import java.util.concurrent.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KronosIdeaCancellationSafetyTest {
    @Test
    fun `cancellation preserving result helper rethrows control flow exceptions`() {
        val processCancellation = ProcessCanceledException()
        val coroutineCancellation = CancellationException("cancelled")

        assertEquals(
            processCancellation,
            assertFailsWith<ProcessCanceledException> {
                runCatchingPreservingCancellation<Unit> { throw processCancellation }
            },
        )
        assertEquals(
            coroutineCancellation,
            assertFailsWith<CancellationException> {
                runCatchingPreservingCancellation<Unit> { throw coroutineCancellation }
            },
        )
    }

    @Test
    fun `cancellation preserving result helper captures ordinary failures`() {
        val result = runCatchingPreservingCancellation<Unit> { error("ordinary failure") }

        assertTrue(result.isFailure)
        assertEquals("ordinary failure", result.exceptionOrNull()?.message)
    }

}
