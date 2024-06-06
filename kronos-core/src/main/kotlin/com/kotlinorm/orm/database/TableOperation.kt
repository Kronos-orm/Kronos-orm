package com.kotlinorm.orm.database

import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.delete.DeleteClause

class TableOperation(val wrapper: KronosDataSourceWrapper) {

    // val ds = Kronos.dataSource
    //
    //data class User(): KPojo
    //
    //// 1.  return if the table exists
    //ds.table.exist<User>() // return Kotlin.Boolean
    //// 2. create a table
    //ds.table.create<User>()
    //// 3. confirm to delete a table
    //ds.table.delete<User>().confirm()
    //// 4. confirm to sync the structure from kotlin code to database
    //ds.table.structureSync<User>().confirm()

    inline fun <reified T: KPojo> createTable(): Table {
        TODO()
    }

}