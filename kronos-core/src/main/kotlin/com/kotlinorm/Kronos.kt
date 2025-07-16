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

package com.kotlinorm

import com.kotlinorm.annotations.KronosInit
import com.kotlinorm.beans.config.DefaultNoValueStrategy
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.config.LineHumpNamingStrategy
import com.kotlinorm.beans.config.NoneNamingStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.logging.BundledSimpleLoggerAdapter
import com.kotlinorm.beans.logging.log
import com.kotlinorm.beans.parser.NoneDataSourceWrapper
import com.kotlinorm.beans.serialize.NoneSerializeProcessor
import com.kotlinorm.enums.KLoggerType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.interfaces.KronosNamingStrategy
import com.kotlinorm.interfaces.KronosSerializeProcessor
import com.kotlinorm.interfaces.NoValueStrategy
import com.kotlinorm.plugins.LastInsertIdPlugin
import com.kotlinorm.types.KLoggerFactory
import com.kotlinorm.utils.DataSourceUtil.orDefault
import java.time.ZoneId

object Kronos {
    // 默认日志适配器
    var defaultLogger: KLoggerFactory =
        {
            BundledSimpleLoggerAdapter(
                it as? String ?: it::class.simpleName!!
            )
        }

    // 日志类型
    var loggerType: KLoggerType = KLoggerType.DEFAULT_LOGGER

    // 日志路径
    var logPath = listOf("console")

    // 无值策略
    var noValueStrategy: NoValueStrategy = DefaultNoValueStrategy

    // 数据源
    var dataSource: () -> KronosDataSourceWrapper = { NoneDataSourceWrapper }

    fun transact(wrapper: KronosDataSourceWrapper? = null, block: () -> Any?) = wrapper.orDefault().transact(block)

    // 严格模式（将提高性能，但当数据库类型与字段类型不匹配时会抛出异常，而不是尝试进行转换）
    var strictSetValue = false

    // 当前时区
    var timeZone: ZoneId = ZoneId.systemDefault()

    // 序列化
    var serializeProcessor: KronosSerializeProcessor = NoneSerializeProcessor

    val lineHumpNamingStrategy by lazy { LineHumpNamingStrategy() }

    @Suppress("MemberVisibilityCanBePrivate")
    val noneNamingStrategy by lazy { NoneNamingStrategy() }

    // 列名策略
    var fieldNamingStrategy: KronosNamingStrategy = noneNamingStrategy

    // 表名策略
    var tableNamingStrategy: KronosNamingStrategy = noneNamingStrategy

    // 主键策略
    var primaryKeyStrategy = KronosCommonStrategy(false, Field("id", "id", primaryKey = PrimaryKeyType.IDENTITY))

    // 更新时间策略
    var updateTimeStrategy = KronosCommonStrategy(false, Field("update_time", "updateTime"))

    // 创建时间策略
    var createTimeStrategy = KronosCommonStrategy(false, Field("create_time", "createTime"))

    // 逻辑删除策略
    var logicDeleteStrategy = KronosCommonStrategy(false, Field("deleted"))

    var optimisticLockStrategy = KronosCommonStrategy(false, Field("version"))

    // 默认日期格式
    var defaultDateFormat = "yyyy-MM-dd HH:mm:ss"

    var coroutineOprtEnable = true

    var coroutineOprtSize = 1000

    @KronosInit
    fun init(action: Kronos.() -> Unit) {
        LastInsertIdPlugin.enabled = true
        this.action()
        defaultLogger(this).info(
            log {
                +"Kronos ORM Framework started."[green]
            }
        )
    }
}