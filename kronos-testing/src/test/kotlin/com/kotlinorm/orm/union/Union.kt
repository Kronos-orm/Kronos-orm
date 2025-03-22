package com.kotlinorm.orm.union

import com.kotlinorm.Kronos
import com.kotlinorm.KronosBasicWrapper
import com.kotlinorm.orm.beans.sample.Customer
import com.kotlinorm.orm.beans.sample.User
import com.kotlinorm.orm.database.table
import com.kotlinorm.orm.insert.InsertClause.Companion.execute
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import org.apache.commons.dbcp2.BasicDataSource
import kotlin.test.Test

class Union {

    private val ds = BasicDataSource().apply {
        driverClassName = "com.mysql.cj.jdbc.Driver"
        url = "jdbc:mysql://localhost:3306/test"
        username = "root"
        password = "********"
        maxIdle = 10
    }

    private val wrapper by lazy {
        Kronos.dataSource()
    }

    init {
        Kronos.init {
            fieldNamingStrategy = lineHumpNamingStrategy
            tableNamingStrategy = lineHumpNamingStrategy
            dataSource = { KronosBasicWrapper(ds) }
        }
    }

    @Test
    fun prepareTestData() {
        Kronos.dataSource.table.dropTable(User())
        Kronos.dataSource.table.dropTable(Customer())
        Kronos.dataSource.table.createTable(User())
        Kronos.dataSource.table.createTable(Customer())

        val userList = listOf(
            User(1, "user1", 1),
            User(2, "user2", 1),
            User(3, "user3", 1),
            User(4, "user4", 1),
        )

        val customerList = listOf(
            Customer(1, "customer1", "1111111111", "aaaaa"),
            Customer(2, "customer2", "2222222222", "bbbbb"),
            Customer(3, "customer3", "3333333333", "ccccc"),
            Customer(4, "customer4", "4444444444", "ddddd"),
        )

        userList.insert().execute()
        customerList.insert().execute()

    }

    @Test
    fun testUnion() {
        val rst = union(
            User().select().where { it.id == 1 || it.id == 2 },
            User().select().where { it.id == 3 || it.id == 4 },
            Customer().select().limit(1),
        ).query()
        println(rst)
    }

}