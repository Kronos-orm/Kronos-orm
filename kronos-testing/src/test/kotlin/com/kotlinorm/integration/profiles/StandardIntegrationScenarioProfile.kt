package com.kotlinorm.integration.profiles

import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.count
import com.kotlinorm.integration.fixtures.IntegrationArchive
import com.kotlinorm.integration.fixtures.IntegrationArchiveRecord
import com.kotlinorm.integration.fixtures.IntegrationJoinProjection
import com.kotlinorm.integration.fixtures.IntegrationJoinRecord
import com.kotlinorm.integration.fixtures.IntegrationOrder
import com.kotlinorm.integration.fixtures.IntegrationOrderRecord
import com.kotlinorm.integration.fixtures.IntegrationTableState
import com.kotlinorm.integration.fixtures.IntegrationUser
import com.kotlinorm.integration.fixtures.IntegrationUserRecord
import com.kotlinorm.integration.fixtures.PAID_STATUS
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.update.update
import com.kotlinorm.orm.upsert.upsert

object StandardIntegrationScenarioProfile : BaseIntegrationScenarioProfile() {
    override fun tableState(): IntegrationTableState =
        IntegrationTableState(
            users = activeDataSource.table.exists(IntegrationUser()),
            orders = activeDataSource.table.exists(IntegrationOrder()),
            archives = activeDataSource.table.exists(IntegrationArchive()),
        )

    override fun countUsers(): Int =
        IntegrationUser().countRows()

    override fun countOrders(): Int =
        IntegrationOrder().countRows()

    override fun countArchives(): Int =
        IntegrationArchive().countRows()

    override fun createTables() {
        activeDataSource.table.createTable(IntegrationUser())
        activeDataSource.table.createTable(IntegrationOrder())
        activeDataSource.table.createTable(IntegrationArchive())
    }

    override fun syncTables() {
        activeDataSource.table.syncTable(IntegrationUser())
        activeDataSource.table.syncTable(IntegrationOrder())
        activeDataSource.table.syncTable(IntegrationArchive())
    }

    override fun truncateTables(restartIdentity: Boolean) {
        activeDataSource.table.truncateTable(IntegrationArchive(), restartIdentity = restartIdentity)
        activeDataSource.table.truncateTable(IntegrationOrder(), restartIdentity = restartIdentity)
        activeDataSource.table.truncateTable(IntegrationUser(), restartIdentity = restartIdentity)
    }

    override fun dropTables() {
        activeDataSource.table.dropTable(IntegrationArchive())
        activeDataSource.table.dropTable(IntegrationOrder())
        activeDataSource.table.dropTable(IntegrationUser())
    }

    override fun insertUser(record: IntegrationUserRecord): Int =
        record.toIntegrationUser().insert().execute().affectedRows

    override fun insertOrder(record: IntegrationOrderRecord): Int =
        record.toIntegrationOrder().insert().execute().affectedRows

    override fun selectAllUsers(): List<IntegrationUserRecord> =
        IntegrationUser()
            .select()
            .orderBy { it.id.asc() }
            .toList<IntegrationUser>()
            .map { it.toRecord() }

    override fun selectUserById(id: Int): IntegrationUserRecord =
        IntegrationUser()
            .select()
            .where { it.id == id }
            .toList<IntegrationUser>()
            .single()
            .toRecord()

    override fun selectUsersByExampleStatus(status: Int): List<IntegrationUserRecord> =
        IntegrationUser(status = status)
            .select()
            .where()
            .orderBy { it.id.asc() }
            .toList<IntegrationUser>()
            .map { it.toRecord() }

    override fun selectUsersWithLambdaWhereOverride(): List<IntegrationUserRecord> =
        IntegrationUser(id = 1)
            .select()
            .where { it.status == 2 }
            .orderBy { it.id.asc() }
            .toList<IntegrationUser>()
            .map { it.toRecord() }

    override fun selectUsersWithChainedWhere(): List<IntegrationUserRecord> =
        IntegrationUser(status = PAID_STATUS)
            .select()
            .where()
            .where { it.score > 15 }
            .orderBy { it.id.asc() }
            .toList<IntegrationUser>()
            .map { it.toRecord() }

    override fun updateUserScore(id: Int, score: Int): Int =
        IntegrationUser(score = score)
            .update()
            .set { it.score = score }
            .where { it.id == id }
            .execute()
            .affectedRows

    override fun deleteUserById(id: Int): Int =
        IntegrationUser(id = id)
            .delete()
            .logic(false)
            .by { it.id }
            .execute()
            .affectedRows

    override fun selectUsersByStatusOrderedByScore(status: Int, limit: Int): List<IntegrationUserRecord> =
        IntegrationUser()
            .select()
            .where { it.status == status }
            .orderBy { it.score.desc() }
            .limit(limit)
            .toList<IntegrationUser>()
            .map { it.toRecord() }

    override fun countUsersByStatus(status: Int): Int =
        IntegrationUser()
            .select { f.count(it.id).alias("total") }
            .where { it.status == status }
            .first<Int>()

    override fun selectUsersScoreHigherThanUser(id: Int): List<IntegrationUserRecord> =
        IntegrationUser()
            .select()
            .where {
                it.score > IntegrationUser()
                    .select { user -> user.score }
                    .where { user -> user.id == id }
                    .limit(1)
            }
            .orderBy { it.id.asc() }
            .toList<IntegrationUser>()
            .map { it.toRecord() }

    override fun selectUsersWithPaidOrdersByInSubquery(): List<IntegrationUserRecord> =
        IntegrationUser()
            .select()
            .where {
                it.id in IntegrationOrder()
                    .select { order -> order.userId }
                    .where { order -> order.status == PAID_STATUS }
            }
            .orderBy { it.id.asc() }
            .toList<IntegrationUser>()
            .map { it.toRecord() }

    override fun selectUsersWithLargePaidOrdersByExistsSubquery(): List<IntegrationUserRecord> =
        IntegrationUser()
            .select()
            .where {
                exists(
                    IntegrationOrder()
                        .select()
                        .where { order -> order.userId == it.id && order.status == PAID_STATUS && order.amount == 50 }
                )
            }
            .orderBy { it.id.asc() }
            .toList<IntegrationUser>()
            .map { it.toRecord() }

    override fun selectPaidOrderJoinRecords(): List<IntegrationJoinRecord> =
        IntegrationUser()
            .join(IntegrationOrder()) { user, order ->
                innerJoin(order) { user.id == order.userId }
                select {
                    [
                        user.id.alias("userId"),
                        user.name.alias("userName"),
                        order.amount
                    ]
                }
                where { order.status == PAID_STATUS }
                orderBy { user.id.asc() }
            }
            .toList<IntegrationJoinProjection>()
            .map { it.toRecord() }

    override fun updateScoresFromPaidOrders(): Int =
        IntegrationUser()
            .update()
            .set {
                it.score = (IntegrationOrder()
                    .select { order -> order.amount }
                    .where { order -> order.userId == it.id && order.status == PAID_STATUS }
                    .limit(1) as Int?)
            }
            .where {
                exists(
                    IntegrationOrder()
                        .select()
                        .where { order -> order.userId == it.id && order.status == PAID_STATUS }
                )
            }
            .execute()
            .affectedRows

    override fun deleteUsersWithoutOrders(): Int =
        IntegrationUser()
            .delete()
            .logic(false)
            .where {
                !exists(
                    IntegrationOrder()
                        .select()
                        .where { order -> order.userId == it.id }
                )
            }
            .execute()
            .affectedRows

    override fun insertPaidOrdersToArchive(): Int =
        IntegrationOrder()
            .select { [it.id, it.userId, it.amount, it.status] }
            .where { it.status == PAID_STATUS }
            .insert<IntegrationArchive>()
            .execute()
            .affectedRows

    override fun selectArchives(): List<IntegrationArchiveRecord> =
        IntegrationArchive()
            .select()
            .orderBy { it.id.asc() }
            .toList<IntegrationArchive>()
            .map { it.toRecord() }

    override fun upsertUser(record: IntegrationUserRecord) {
        record.toIntegrationUser()
            .upsert { [it.name, it.score, it.status] }
            .on { it.id }
            .onConflict()
            .execute()
    }

    private fun IntegrationUserRecord.toIntegrationUser(): IntegrationUser =
        IntegrationUser(id = id, name = name, score = score, status = status)

    private fun IntegrationOrderRecord.toIntegrationOrder(): IntegrationOrder =
        IntegrationOrder(id = id, userId = userId, status = status, amount = amount)

    private fun IntegrationUser.toRecord(): IntegrationUserRecord =
        IntegrationUserRecord(id = id, name = name, score = score, status = status)

    private fun IntegrationArchive.toRecord(): IntegrationArchiveRecord =
        IntegrationArchiveRecord(id = id, userId = userId, amount = amount, status = status)

    private fun IntegrationJoinProjection.toRecord(): IntegrationJoinRecord =
        IntegrationJoinRecord(userId = userId, userName = userName, amount = amount)

    private fun IntegrationUser.countRows(): Int =
        select { f.count(1).alias("total") }
            .first<Long>()
            .toInt()

    private fun IntegrationOrder.countRows(): Int =
        select { f.count(1).alias("total") }
            .first<Long>()
            .toInt()

    private fun IntegrationArchive.countRows(): Int =
        select { f.count(1).alias("total") }
            .first<Long>()
            .toInt()
}
