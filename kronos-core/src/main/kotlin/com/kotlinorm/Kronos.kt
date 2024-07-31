/**
 * Copyright 2022-2024 kronos-orm
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

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsw.NoneDataSourceWrapper
import com.kotlinorm.beans.logging.BundledSimpleLoggerAdapter
import com.kotlinorm.beans.logging.KLogMessage.Companion.kMsgOf
import com.kotlinorm.beans.namingStrategy.NoneNamingStrategy
import com.kotlinorm.beans.serializeResolver.NoneSerializeResolver
import com.kotlinorm.enums.ColorPrintCode.Companion.Green
import com.kotlinorm.enums.KLoggerType
import com.kotlinorm.enums.NoValueStrategy
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.interfaces.KronosNamingStrategy
import com.kotlinorm.interfaces.KronosSerializeResolver
import com.kotlinorm.types.KLoggerFactory
import kotlinx.datetime.TimeZone.Companion.currentSystemDefault

object Kronos {
    // 默认日志适配器
    var defaultLogger: KLoggerFactory =
        { BundledSimpleLoggerAdapter(it::class.simpleName!!) }

    // 日志类型
    var loggerType: KLoggerType = KLoggerType.DEFAULT_LOGGER

    // 日志路径
    var logPath = listOf("console")

    // 无值策略
    internal var noValueStrategy = NoValueStrategy.Smart

    // 数据源
    var dataSource: () -> KronosDataSourceWrapper = { NoneDataSourceWrapper }

    // 严格模式（将提高性能，但当数据库类型与字段类型不匹配时会抛出异常，而不是尝试进行转换）
    var strictSetValue = false

    // 当前时区
    var timeZone = currentSystemDefault()

    // 序列化
    var serializeResolver: KronosSerializeResolver = NoneSerializeResolver

    // 列名策略
    var fieldNamingStrategy: KronosNamingStrategy = NoneNamingStrategy()

    // 表名策略
    var tableNamingStrategy: KronosNamingStrategy = NoneNamingStrategy()

    // 更新时间策略
    var updateTimeStrategy: KronosCommonStrategy = KronosCommonStrategy(false, Field("update_time", "updateTime"))

    // 创建时间策略
    var createTimeStrategy: KronosCommonStrategy = KronosCommonStrategy(false, Field("create_time", "createTime"))

    // 逻辑删除策略
    var logicDeleteStrategy: KronosCommonStrategy = KronosCommonStrategy(false, Field("deleted"))

    var optimisticLockStrategy: KronosCommonStrategy = KronosCommonStrategy(false, Field("version"))

    // 默认日期格式
    var defaultDateFormat: String = "yyyy-MM-dd HH:mm:ss"

    init {
        defaultLogger(this).info(
            kMsgOf("Kronos ORM Framework started.", Green).endl().toArray()
        )
    }
}