/**
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.beans.generator

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.enums.PrimaryKeyType

internal fun Field.resolveGeneratedPrimaryKeyValue(currentValue: Any?): Any? {
    if (currentValue != null) return currentValue
    return when (primaryKey) {
        PrimaryKeyType.UUID -> UUIDGenerator.nextId()
        PrimaryKeyType.SNOWFLAKE -> SnowflakeIdGenerator.nextId()
        PrimaryKeyType.CUSTOM -> customIdGenerator?.nextId()
        else -> null
    }
}
