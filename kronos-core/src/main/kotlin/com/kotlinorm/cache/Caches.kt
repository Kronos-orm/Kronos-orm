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

package com.kotlinorm.cache

import com.kotlinorm.Kronos.primaryKeyStrategy
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.parser.ParsedSql
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.LinkedHashSet
import com.kotlinorm.utils.LRUCache
import com.kotlinorm.utils.KTypeKey
import com.kotlinorm.utils.buildRuntimeFieldMap
import com.kotlinorm.utils.createKPojo
import com.kotlinorm.utils.resolvePrimaryKey
import com.kotlinorm.utils.runtimeBind
import com.kotlinorm.utils.runtimeColumns
import com.kotlinorm.utils.runtimeCreateTimeStrategy
import com.kotlinorm.utils.runtimeLogicDeleteStrategy
import com.kotlinorm.utils.runtimeOptimisticLockStrategy
import com.kotlinorm.utils.runtimeUpdateTimeStrategy
import com.kotlinorm.utils.toLinkedSet
import kotlin.reflect.KType

private fun <R> kPojoMetadataCache(defaultValue: (KType) -> R?): LRUCache<KType, R> = LRUCache(
    keySelector = { type -> KTypeKey.from(type, ignoreTopLevelNullability = true) },
    defaultValue = defaultValue
)

val fieldsMapCache = kPojoMetadataCache<Map<String, Field>> { type ->
    buildRuntimeFieldMap(kPojoAllFieldsCache[type]!!)
}

val namedSqlCache = LRUCache<String, ParsedSql>()
val kPojoInstanceCache = kPojoMetadataCache<KPojo> { createKPojo(it) }
val kPojoAllFieldsCache = kPojoMetadataCache<LinkedHashSet<Field>> { type ->
    kPojoInstanceCache[type]!!.runtimeColumns().toLinkedSet()
}

val kPojoAllColumnsCache = kPojoMetadataCache<List<Field>> { type ->
    kPojoInstanceCache[type]!!.runtimeColumns().filter { it.isColumn }
}

val kPojoFieldMapCache = kPojoMetadataCache<Map<String, Field>> { type ->
    kPojoInstanceCache[type]!!.runtimeColumns().associateBy { it.name }
}

val kPojoPrimaryKeyCache = kPojoMetadataCache<Field> { type ->
    resolvePrimaryKey(type, kPojoAllColumnsCache[type]!!)
}

val kPojoCreateTimeCache = kPojoMetadataCache<KronosCommonStrategy?> { type ->
    kPojoInstanceCache[type]!!.let { kPojo ->
        kPojo.runtimeCreateTimeStrategy().runtimeBind(kPojo.__tableName, kPojoAllColumnsCache[type]!!)
    }
}

val kPojoUpdateTimeCache = kPojoMetadataCache<KronosCommonStrategy?> { type ->
    kPojoInstanceCache[type]!!.let { kPojo ->
        kPojo.runtimeUpdateTimeStrategy().runtimeBind(kPojo.__tableName, kPojoAllColumnsCache[type]!!)
    }
}

val kPojoLogicDeleteCache = kPojoMetadataCache<KronosCommonStrategy?> { type ->
    kPojoInstanceCache[type]!!.let { kPojo ->
        kPojo.runtimeLogicDeleteStrategy().runtimeBind(kPojo.__tableName, kPojoAllColumnsCache[type]!!)
    }
}

val kPojoOptimisticLockCache = kPojoMetadataCache<KronosCommonStrategy?> { type ->
    kPojoInstanceCache[type]!!.let { kPojo ->
        kPojo.runtimeOptimisticLockStrategy().runtimeBind(kPojo.__tableName, kPojoAllColumnsCache[type]!!)
    }
}
