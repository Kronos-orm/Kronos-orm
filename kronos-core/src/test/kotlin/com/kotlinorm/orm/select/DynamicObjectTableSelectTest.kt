package com.kotlinorm.orm.select

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Ignore
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.enums.IgnoreAction
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicObjectTableSelectTest {

    @Test
    fun `dynamic object table builds select from instance metadata without KClass cache`() {
        val temp = object : KPojo {
            @Ignore([IgnoreAction.ALL])
            override var __kType = typeOf<KPojo>()
            @Ignore([IgnoreAction.ALL])
            override var __tableName = "tmp_runtime_user"
            @Ignore([IgnoreAction.ALL])
            override var __tableComment = ""
            @Ignore([IgnoreAction.ALL])
            override var __columns = mutableListOf(
                Field("id", "id", primaryKey = PrimaryKeyType.DEFAULT, nullable = false),
                Field("name", "name")
            )
            @Ignore([IgnoreAction.ALL])
            override var __tableIndexes = mutableListOf<com.kotlinorm.beans.dsl.KTableIndex>()
            @Ignore([IgnoreAction.ALL])
            override var __createTime = Kronos.createTimeStrategy
            @Ignore([IgnoreAction.ALL])
            override var __updateTime = Kronos.updateTimeStrategy
            @Ignore([IgnoreAction.ALL])
            override var __logicDelete = Kronos.logicDeleteStrategy
            @Ignore([IgnoreAction.ALL])
            override var __optimisticLock = Kronos.optimisticLockStrategy

            var id: Int? = 6
            var name: String? = null
        }

        val task = temp
            .select { it.name }
            .where { it.id == 6 }
            .build(SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper)
            .atomicTask

        assertEquals(
            "SELECT `name` FROM `tmp_runtime_user` WHERE `tmp_runtime_user`.`id` = :id",
            task.sql
        )
        assertEquals(mapOf("id" to 6), task.paramMap)
    }
}
