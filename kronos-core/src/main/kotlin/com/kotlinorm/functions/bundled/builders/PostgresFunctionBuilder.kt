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

package com.kotlinorm.functions.bundled.builders

import com.kotlinorm.beans.dsl.FunctionField
import com.kotlinorm.enums.DBType
import com.kotlinorm.functions.bundled.builders.MathFunctionBuilder.buildField
import com.kotlinorm.interfaces.FunctionBuilder
import com.kotlinorm.interfaces.KronosDataSourceWrapper

object PostgresFunctionBuilder : FunctionBuilder {
    override val supportFunctionNames: (String) -> Array<DBType> = {
        when (it) {
            "any" -> arrayOf(DBType.Postgres)
            "all" -> arrayOf(DBType.Postgres)
            else -> emptyArray()
        }
    }

    override fun transform(
        field: FunctionField,
        dataSource: KronosDataSourceWrapper,
        showTable: Boolean,
        showAlias: Boolean
    ): String {
        return "${field.functionName.uppercase()}(ARRAY${
            buildField(field.fields.first(), dataSource, showTable)
        })"
    }
}