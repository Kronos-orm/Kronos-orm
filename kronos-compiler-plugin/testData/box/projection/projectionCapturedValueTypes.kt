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

// Verifies aliases of captured non-source values keep the resolved receiver type.

import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

@Table("tb_projection_captured_value")
data class ProjectionCapturedValueUser(
    var id: Int? = null,
) : KPojo

data class ProjectionCapturedValueHolder(
    val score: Int,
    val label: String?,
)

private fun compileCapturedProjection(holder: ProjectionCapturedValueHolder) {
    val clause = ProjectionCapturedValueUser().select {
        [
            holder.score.alias("capturedScore"),
            holder.label.alias("capturedLabel"),
            7.alias("constantScore"),
        ]
    }

    @Suppress("UNREACHABLE_CODE")
    if (false) {
        val row = clause.first()
        val capturedScore: Int = row.capturedScore
        val capturedLabel: String? = row.capturedLabel
        val constantScore: Int = row.constantScore
        error("generated values were $capturedScore/$capturedLabel/$constantScore")
    }
}

fun box(): String {
    compileCapturedProjection(ProjectionCapturedValueHolder(score = 7, label = null))
    return "OK"
}
