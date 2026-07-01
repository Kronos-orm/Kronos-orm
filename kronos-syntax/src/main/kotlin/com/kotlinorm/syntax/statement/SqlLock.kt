/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.statement

import com.kotlinorm.syntax.SqlNode

sealed interface SqlLock : SqlNode {
    val waitMode: SqlLockWaitMode?

    data class Update(override val waitMode: SqlLockWaitMode? = null) : SqlLock

    data class Share(override val waitMode: SqlLockWaitMode? = null) : SqlLock
}

enum class SqlLockWaitMode {
    NoWait,
    SkipLocked
}

