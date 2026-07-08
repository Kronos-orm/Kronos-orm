package com.kotlinorm.integration.profiles

import com.kotlinorm.integration.fixtures.IntegrationArchiveRecord
import com.kotlinorm.integration.fixtures.IntegrationJoinRecord
import com.kotlinorm.integration.fixtures.IntegrationOrderRecord
import com.kotlinorm.integration.fixtures.IntegrationTableState
import com.kotlinorm.integration.fixtures.IntegrationUserRecord
import com.kotlinorm.integration.fixtures.integrationOrders
import com.kotlinorm.integration.fixtures.integrationUsers

interface IntegrationScenarioProfile {
    fun tableState(): IntegrationTableState
    fun createTables()
    fun syncTables()
    fun truncateTables(restartIdentity: Boolean)
    fun dropTables()
    fun countUsers(): Int
    fun countOrders(): Int
    fun countArchives(): Int
    fun insertUser(record: IntegrationUserRecord): Int
    fun insertOrder(record: IntegrationOrderRecord): Int
    fun selectAllUsers(): List<IntegrationUserRecord>
    fun selectUserById(id: Int): IntegrationUserRecord
    fun selectUsersByExampleStatus(status: Int): List<IntegrationUserRecord>
    fun selectUsersWithLambdaWhereOverride(): List<IntegrationUserRecord>
    fun selectUsersWithChainedWhere(): List<IntegrationUserRecord>
    fun updateUserScore(id: Int, score: Int): Int
    fun deleteUserById(id: Int): Int
    fun selectUsersByStatusOrderedByScore(status: Int, limit: Int): List<IntegrationUserRecord>
    fun countUsersByStatus(status: Int): Int
    fun selectUsersScoreHigherThanUser(id: Int): List<IntegrationUserRecord>
    fun selectUsersWithPaidOrdersByInSubquery(): List<IntegrationUserRecord>
    fun selectUsersWithLargePaidOrdersByExistsSubquery(): List<IntegrationUserRecord>
    fun selectPaidOrderJoinRecords(): List<IntegrationJoinRecord>
    fun updateScoresFromPaidOrders(): Int
    fun deleteUsersWithoutOrders(): Int
    fun insertPaidOrdersToArchive(): Int
    fun selectArchives(): List<IntegrationArchiveRecord>
    fun upsertUser(record: IntegrationUserRecord)

    fun seedUsersAndOrders() {
        integrationUsers.forEach { insertUser(it) }
        integrationOrders.forEach { insertOrder(it) }
    }
}
