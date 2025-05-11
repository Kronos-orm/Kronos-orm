package com.kotlinorm.orm.delete

import com.kotlinorm.interfaces.KActionInfo

class DeleteClauseInfo(override val tableName: String, override val whereClause: String?) : KActionInfo