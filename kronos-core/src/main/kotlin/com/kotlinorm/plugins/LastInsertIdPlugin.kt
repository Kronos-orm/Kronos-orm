package com.kotlinorm.plugins

import com.kotlinorm.Kronos
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.beans.task.registerTaskEventPlugin
import com.kotlinorm.beans.task.unregisterTaskEventPlugin
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.TaskEventPlugin
import com.kotlinorm.orm.insert.InsertClause
import com.kotlinorm.types.ActionTaskEvent
import com.kotlinorm.types.QueryTaskEvent

object LastInsertIdPlugin : TaskEventPlugin {
    private var loaded = false
    var enabled
        get() = loaded
        set(value){
            if(value && !loaded){
                registerTaskEventPlugin(LastInsertIdPlugin)
                loaded = true
            }
            if(!value && loaded){
                unregisterTaskEventPlugin(LastInsertIdPlugin)
                loaded = false
            }
        }

    val KronosOperationResult.lastInsertId
        get() = this.stash["lastInsertId"] as? Long

    // Generates the SQL statement needed to obtain the last inserted ID based on the provided database type.
    fun lastInsertIdObtainSql(dbType: DBType): String {
        return when (dbType) {
            DBType.Mysql, DBType.H2, DBType.OceanBase -> "SELECT LAST_INSERT_ID()"
            DBType.Oracle -> "SELECT * FROM DUAL"
            DBType.Mssql -> "SELECT SCOPE_IDENTITY()"
            DBType.Postgres -> "SELECT LASTVAL()"
            DBType.DB2 -> "SELECT IDENTITY_VAL_LOCAL() FROM SYSIBM.SYSDUMMY1"
            DBType.Sybase -> "SELECT @@IDENTITY"
            DBType.SQLite -> "SELECT last_insert_rowid()"
            else -> throw UnsupportedOperationException("Unsupported database type: $dbType")
        }
    }

    override val doBeforeQuery: QueryTaskEvent? = null
    override val doAfterQuery: QueryTaskEvent? = null
    override val doBeforeAction: ActionTaskEvent? = null

    override val doAfterAction: ActionTaskEvent? = { wrapper ->
        if (operationType == KOperationType.INSERT &&
            (stash["queryId"] == true || (stash["queryId"] == null && enabled))
            && stash["useIdentity"] == true // 目前仅支持自增主键
        ) {
            stash["lastInsertId"] = (wrapper.forObject(
                KronosAtomicQueryTask(lastInsertIdObtainSql(wrapper.dbType)),
                kClass = Long::class,
                false,
                listOf()
            ) ?: 0L) as Long
        }
    }

    fun InsertClause<*>.withId(): InsertClause<*> {
        stash["queryId"] = true
        return this
    }
}