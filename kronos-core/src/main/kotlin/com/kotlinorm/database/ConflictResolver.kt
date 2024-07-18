package com.kotlinorm.database

import com.kotlinorm.beans.dsl.Field

data class ConflictResolver(
    val tableName: String,
    val onFields: LinkedHashSet<Field>,
    val toUpdateFields: LinkedHashSet<Field>,
    val toInsertFields: LinkedHashSet<Field>
)