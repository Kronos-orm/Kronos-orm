/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.validate

import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlAssignmentTarget
import com.kotlinorm.syntax.statement.SqlUpdateSetPair

internal fun id(name: String): SqlIdentifier =
    SqlIdentifier.of(name)

internal fun cols(vararg names: String): List<SqlIdentifier> =
    names.map { id(it) }

internal fun set(column: String, value: SqlExpr): SqlUpdateSetPair =
    SqlUpdateSetPair(SqlAssignmentTarget.Column(id(column)), value)
