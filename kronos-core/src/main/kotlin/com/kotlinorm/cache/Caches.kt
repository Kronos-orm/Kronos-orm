package com.kotlinorm.cache

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.parser.ParsedSql
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.LRUCache
import com.kotlinorm.utils.createInstance
import kotlin.reflect.KClass

val fieldsMapCache = LRUCache<KClass<KPojo>, Map<String, Field>> { kClass->
    kClass.createInstance().kronosColumns().let { instance ->
        instance.associateBy { it.name } + instance.associateBy { it.columnName }
    }
}

val insertSqlCache = LRUCache<Pair<KClass<KPojo>, Boolean>, String>()
val namedSqlCache = LRUCache<String, ParsedSql>()