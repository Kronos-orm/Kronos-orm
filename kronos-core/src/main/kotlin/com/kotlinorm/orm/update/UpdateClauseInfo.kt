package com.kotlinorm.orm.update

import com.kotlinorm.interfaces.KActionInfo

class UpdateClauseInfo(override val tableName: String, override val whereClause: String?) : KActionInfo