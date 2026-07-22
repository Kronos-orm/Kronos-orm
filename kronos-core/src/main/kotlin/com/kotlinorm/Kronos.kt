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

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.config.LineHumpNamingStrategy
import com.kotlinorm.beans.config.NoneNamingStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.logging.BundledSimpleLoggerAdapter
import com.kotlinorm.beans.parser.NoneDataSourceWrapper
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.KLoggerType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.interfaces.KronosNamingStrategy
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.ValueCodec
import com.kotlinorm.interfaces.ValueCodecRegistration
import com.kotlinorm.types.KLoggerFactory
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.KPojoFactory
import com.kotlinorm.utils.KPojoFactoryRegistration
import com.kotlinorm.utils.codec.ValueCodecRegistry
import com.kotlinorm.utils.createKPojo as createRegisteredKPojo
import com.kotlinorm.utils.registerKPojoFactory as registerExactKPojoFactory
import java.time.ZoneId
import kotlin.reflect.KType

object Kronos {
    /**
     * Registers one bidirectional value codec at highest priority.
     *
     * The returned handle is idempotent; closing it removes only this codec and
     * restores the previous matching registration. SQL `NULL` is handled by the
     * registry, and codec failures include direction/origin/field context.
     *
     * @param codec stateless matcher/converter to install at highest current priority
     * @return an idempotent handle that removes only this registration on close
     */
    fun registerValueCodec(codec: ValueCodec): ValueCodecRegistration = ValueCodecRegistry.register(codec)

    /**
     * Registers a user KPojo factory for the exact complete [type].
     * Later registrations have priority; the returned handle removes only this
     * registration and restores the previous user or generated factory on close.
     *
     * @param type concrete, non-generic KPojo type for this release
     * @param factory fresh-instance constructor that must return a type-compatible KPojo
     * @return an idempotent handle that restores the previous exact-type factory on close
     * @throws com.kotlinorm.exceptions.UnsupportedType when [type] is not a supported concrete KPojo
     */
    fun registerKPojoFactory(type: KType, factory: KPojoFactory): KPojoFactoryRegistration =
        registerExactKPojoFactory(type, factory)

    /**
     * Creates a fresh KPojo through the highest-priority user factory for [type],
     * falling back to compiler-generated metadata. Missing or incompatible
     * factories fail with a contextual construction exception.
     *
     * @param type exact concrete KPojo type; top-level nullability is ignored for lookup
     * @return a fresh instance whose generated `__kType` matches [type]
     * @throws com.kotlinorm.exceptions.KPojoFactoryException when construction metadata is missing or invalid
     */
    fun createKPojo(type: KType): KPojo = createRegisteredKPojo(type)

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
    var logPath = ["console"]

    // 数据源
    var dataSource: () -> KronosDataSourceWrapper = { NoneDataSourceWrapper }

    /**
     * Executes a block of code within a database transaction.
     *
     * @param wrapper The data source wrapper to use, or `null` to use the default data source.
     * @param isolation The transaction isolation level, or `null` to use the connection default.
     * @param timeout The transaction timeout in seconds, or `null` for no timeout.
     * @param block The block of code to execute within the transaction, with [TransactionScope] as receiver.
     * @return The result of the block execution.
     */
    fun transact(
        wrapper: KronosDataSourceWrapper? = null,
        isolation: TransactionIsolation? = null,
        timeout: Int? = null,
        block: TransactionScope.() -> Any?
    ) = wrapper.orDefault().transact(isolation, timeout, block)

    // 严格模式（将提高性能，但当数据库类型与字段类型不匹配时会抛出异常，而不是尝试进行转换）
    var strictSetValue = false

    // 当前时区
    var timeZone: ZoneId = ZoneId.systemDefault()

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
}
