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

val insertSqlCache = LRUCache<Pair<KClass<KPojo>, Boolean>, String>()
val namedSqlCache = LRUCache<String, ParsedSql>()
val kPojoInstanceCache = LRUCache<KClass<KPojo>, KPojo> { it.createInstance() }
val kPojoAllFieldsCache = LRUCache<KClass<KPojo>, LinkedHashSet<Field>> { kClass ->
    kPojoInstanceCache[kClass]!!.kronosColumns().toLinkedSet()
}

val kPojoAllColumnsCache = LRUCache<KClass<KPojo>, List<Field>> { kClass ->
    kClass.createInstance().kronosColumns().filter { it.isColumn }
}

val kPojoFieldMapCache = LRUCache<KClass<KPojo>, Map<String, Field>> { kClass ->
    kClass.createInstance().kronosColumns().associateBy { it.name }
}

val kPojoPrimaryKeyCache = LRUCache<KClass<KPojo>, Field> { kClass ->
    kPojoAllColumnsCache[kClass]!!.firstOrNull { it.primaryKey != PrimaryKeyType.NOT } ?: primaryKeyStrategy
        .takeIf { it.enabled }?.field.takeIf { field ->
            kPojoAllColumnsCache[kClass]!!.any { it.name == field?.name }
        }
    ?: error("No primary key found for ${kClass.simpleName}!")
}

val kPojoCreateTimeCache = LRUCache<KClass<KPojo>, KronosCommonStrategy?> { kClass ->
    kClass.createInstance().let { kPojo ->
        kPojo.kronosCreateTime().takeIf { strategy -> strategy.enabled }?.bind(kPojo.kronosTableName())?.apply {
            kPojoAllColumnsCache[kClass]!!.any { it.name == field.name }
        }
    }
}

val kPojoUpdateTimeCache = LRUCache<KClass<KPojo>, KronosCommonStrategy?> { kClass ->
    kClass.createInstance().let {
        it.kronosUpdateTime().takeIf { strategy -> strategy.enabled }?.bind(it.kronosTableName())?.apply {
            kPojoAllColumnsCache[kClass]!!.any { it.name == field.name }
        }
    }
}

val kPojoLogicDeleteCache = LRUCache<KClass<KPojo>, KronosCommonStrategy?> { kClass ->
    kClass.createInstance().let {
        it.kronosLogicDelete().takeIf { strategy -> strategy.enabled }?.bind(it.kronosTableName())?.apply {
            kPojoAllColumnsCache[kClass]!!.any { it.name == field.name }
        }
    }
}

val kPojoOptimisticLockCache = LRUCache<KClass<KPojo>, KronosCommonStrategy?> { kClass ->
    kClass.createInstance().let {
        it.kronosOptimisticLock().takeIf { strategy -> strategy.enabled }?.bind(it.kronosTableName())?.apply {
            kPojoAllColumnsCache[kClass]!!.any { it.name == field.name }
        }
    }
}