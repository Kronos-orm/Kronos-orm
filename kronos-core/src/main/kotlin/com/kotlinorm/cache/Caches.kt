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
import com.kotlinorm.utils.buildRuntimeFieldMap
import com.kotlinorm.utils.createInstance
import com.kotlinorm.utils.resolvePrimaryKey
import com.kotlinorm.utils.runtimeBind
import com.kotlinorm.utils.runtimeColumns
import com.kotlinorm.utils.runtimeCreateTimeStrategy
import com.kotlinorm.utils.runtimeLogicDeleteStrategy
import com.kotlinorm.utils.runtimeOptimisticLockStrategy
import com.kotlinorm.utils.runtimeUpdateTimeStrategy
import com.kotlinorm.utils.toLinkedSet
import kotlin.reflect.KClass

val fieldsMapCache = LRUCache<KClass<KPojo>, Map<String, Field>> { kClass ->
    buildRuntimeFieldMap(kPojoAllFieldsCache[kClass]!!)
}

val insertSqlCache = LRUCache<Pair<KClass<out KPojo>, Boolean>, String>()
val namedSqlCache = LRUCache<String, ParsedSql>()
val kPojoInstanceCache = LRUCache<KClass<out KPojo>, KPojo> { it.createInstance() }
val kPojoAllFieldsCache = LRUCache<KClass<out KPojo>, LinkedHashSet<Field>> { kClass ->
    kPojoInstanceCache[kClass]!!.runtimeColumns().toLinkedSet()
}

val kPojoAllColumnsCache = LRUCache<KClass<out KPojo>, List<Field>> { kClass ->
    kPojoInstanceCache[kClass]!!.runtimeColumns().filter { it.isColumn }
}

val kPojoFieldMapCache = LRUCache<KClass<out KPojo>, Map<String, Field>> { kClass ->
    kPojoInstanceCache[kClass]!!.runtimeColumns().associateBy { it.name }
}

val kPojoPrimaryKeyCache = LRUCache<KClass<out KPojo>, Field> { kClass ->
    resolvePrimaryKey(kClass, kPojoAllColumnsCache[kClass]!!)
}

val kPojoCreateTimeCache = LRUCache<KClass<out KPojo>, KronosCommonStrategy?> { kClass ->
    kPojoInstanceCache[kClass]!!.let { kPojo ->
        kPojo.runtimeCreateTimeStrategy().runtimeBind(kPojo.__tableName, kPojoAllColumnsCache[kClass]!!)
    }
}

val kPojoUpdateTimeCache = LRUCache<KClass<out KPojo>, KronosCommonStrategy?> { kClass ->
    kPojoInstanceCache[kClass]!!.let { kPojo ->
        kPojo.runtimeUpdateTimeStrategy().runtimeBind(kPojo.__tableName, kPojoAllColumnsCache[kClass]!!)
    }
}

val kPojoLogicDeleteCache = LRUCache<KClass<out KPojo>, KronosCommonStrategy?> { kClass ->
    kPojoInstanceCache[kClass]!!.let { kPojo ->
        kPojo.runtimeLogicDeleteStrategy().runtimeBind(kPojo.__tableName, kPojoAllColumnsCache[kClass]!!)
    }
}

val kPojoOptimisticLockCache = LRUCache<KClass<out KPojo>, KronosCommonStrategy?> { kClass ->
    kPojoInstanceCache[kClass]!!.let { kPojo ->
        kPojo.runtimeOptimisticLockStrategy().runtimeBind(kPojo.__tableName, kPojoAllColumnsCache[kClass]!!)
    }
}
