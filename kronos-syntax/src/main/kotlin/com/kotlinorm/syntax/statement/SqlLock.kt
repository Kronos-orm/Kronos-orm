/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.statement

import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.SqlNode

sealed interface SqlLock : SqlNode {
    val waitMode: SqlLockWaitMode?
    val targets: List<SqlIdentifier>

    data class Update(
        override val waitMode: SqlLockWaitMode? = null,
        override val targets: List<SqlIdentifier> = emptyList()
    ) : SqlLock

    data class Share(
        override val waitMode: SqlLockWaitMode? = null,
        override val targets: List<SqlIdentifier> = emptyList()
    ) : SqlLock
}

enum class SqlLockWaitMode {
    NoWait,
    SkipLocked
}
