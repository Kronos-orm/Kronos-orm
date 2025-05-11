package com.kotlinorm.orm.ddl

import com.kotlinorm.interfaces.KActionInfo

class DDLInfo(override val tableName: String, override val whereClause: String? = null) : KActionInfo