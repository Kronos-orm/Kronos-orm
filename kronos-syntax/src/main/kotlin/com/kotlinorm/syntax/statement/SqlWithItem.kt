/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.statement

import com.kotlinorm.syntax.SqlNode

data class SqlWithItem(
    val name: String,
    val columnNames: List<String> = emptyList(),
    val query: SqlQuery
) : SqlNode

