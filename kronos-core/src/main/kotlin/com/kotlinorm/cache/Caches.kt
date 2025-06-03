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
import com.kotlinorm.utils.LRUCache
import com.kotlinorm.utils.createInstance
import com.kotlinorm.utils.toLinkedSet
import kotlin.reflect.KClass

val fieldsMapCache = LRUCache<KClass<KPojo>, Map<String, Field>> { kClass->
    kClass.createInstance().kronosColumns().let { instance ->
        instance.associateBy { it.name } + instance.associateBy { it.columnName }
    }
}

val insertSqlCache = LRUCache<Pair<KClass<out KPojo>, Boolean>, String>()
val namedSqlCache = LRUCache<String, ParsedSql>()
val kPojoInstanceCache = LRUCache<KClass<out KPojo>, KPojo> { it.createInstance() }
val kPojoAllFieldsCache = LRUCache<KClass<out KPojo>, LinkedHashSet<Field>> { kClass ->
    kPojoInstanceCache[kClass]!!.kronosColumns().toLinkedSet()
}

val kPojoAllColumnsCache = LRUCache<KClass<out KPojo>, List<Field>> { kClass ->
    kClass.createInstance().kronosColumns().filter { it.isColumn }
}

val kPojoFieldMapCache = LRUCache<KClass<out KPojo>, Map<String, Field>> { kClass ->
    kClass.createInstance().kronosColumns().associateBy { it.name }
}

val kPojoPrimaryKeyCache = LRUCache<KClass<out KPojo>, Field> { kClass ->
    kPojoAllColumnsCache[kClass]!!.firstOrNull { it.primaryKey != PrimaryKeyType.NOT } ?: primaryKeyStrategy
        .takeIf { it.enabled }?.field.takeIf { field ->
            kPojoAllColumnsCache[kClass]!!.any { it.name == field?.name }
        }
    ?: error("No primary key found for ${kClass.simpleName}!")
}

val kPojoCreateTimeCache = LRUCache<KClass<out KPojo>, KronosCommonStrategy?> { kClass ->
    kClass.createInstance().let { kPojo ->
        kPojo.kronosCreateTime().takeIf { strategy -> strategy.enabled }?.bind(kPojo.kronosTableName())?.apply {
            kPojoAllColumnsCache[kClass]!!.any { it.name == field.name }
        }
    }
}

val kPojoUpdateTimeCache = LRUCache<KClass<out KPojo>, KronosCommonStrategy?> { kClass ->
    kClass.createInstance().let { kPojo ->
        kPojo.kronosUpdateTime().takeIf { strategy -> strategy.enabled }?.bind(kPojo.kronosTableName())?.apply {
            kPojoAllColumnsCache[kClass]!!.any { it.name == field.name }
        }
    }
}

val kPojoLogicDeleteCache = LRUCache<KClass<out KPojo>, KronosCommonStrategy?> { kClass ->
    kClass.createInstance().let { kPojo ->
        kPojo.kronosLogicDelete().takeIf { strategy -> strategy.enabled }?.bind(kPojo.kronosTableName())?.apply {
            kPojoAllColumnsCache[kClass]!!.any { it.name == field.name }
        }
    }
}

val kPojoOptimisticLockCache = LRUCache<KClass<out KPojo>, KronosCommonStrategy?> { kClass ->
    kClass.createInstance().let { kPojo ->
        kPojo.kronosOptimisticLock().takeIf { strategy -> strategy.enabled }?.bind(kPojo.kronosTableName())?.apply {
            kPojoAllColumnsCache[kClass]!!.any { col-> col.name == field.name }
        }
    }
}