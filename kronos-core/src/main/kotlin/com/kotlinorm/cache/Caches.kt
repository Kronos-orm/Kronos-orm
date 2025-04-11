package com.kotlinorm.cache

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.LRUCache
import com.kotlinorm.utils.createInstance
import kotlin.reflect.KClass

val fieldsMapCache = LRUCache<KClass<KPojo>, Map<String, Field>> {
    it.createInstance().kronosColumns().let { instance ->
        instance.associate {
            it.name to it
        } + instance.associate {
            it.columnName to it
        }
    }
}

val insertSqlCache = LRUCache<KClass<KPojo>, String>()