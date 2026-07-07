package com.kotlinorm.integration.fixtures

import com.kotlinorm.interfaces.KPojo

const val PAID_STATUS: Int = 1
const val OPEN_STATUS: Int = 0

data class IntegrationUserRecord(
    val id: Int?,
    val name: String?,
    val score: Int?,
    val status: Int?,
)

data class IntegrationOrderRecord(
    val id: Int?,
    val userId: Int?,
    val status: Int?,
    val amount: Int?,
)

data class IntegrationArchiveRecord(
    val id: Int?,
    val userId: Int?,
    val amount: Int?,
    val status: Int?,
)

data class IntegrationJoinRecord(
    val userId: Int?,
    val userName: String?,
    val amount: Int?,
)

data class IntegrationJoinProjection(
    var userId: Int? = null,
    var userName: String? = null,
    var amount: Int? = null,
) : KPojo

data class IntegrationTableState(
    val users: Boolean,
    val orders: Boolean,
    val archives: Boolean,
)

val integrationUsers = listOf(
    IntegrationUserRecord(id = 1, name = "Ada", score = 10, status = PAID_STATUS),
    IntegrationUserRecord(id = 2, name = "Grace", score = 20, status = PAID_STATUS),
    IntegrationUserRecord(id = 3, name = "Linus", score = 30, status = 2),
    IntegrationUserRecord(id = 4, name = "NoOrder", score = 5, status = OPEN_STATUS),
)

val integrationOrders = listOf(
    IntegrationOrderRecord(id = 1, userId = 1, status = PAID_STATUS, amount = 50),
    IntegrationOrderRecord(id = 2, userId = 1, status = OPEN_STATUS, amount = 20),
    IntegrationOrderRecord(id = 3, userId = 2, status = PAID_STATUS, amount = 40),
)
