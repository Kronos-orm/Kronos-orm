package com.kotlinorm.integration.fixtures

import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType.INT
import com.kotlinorm.enums.KColumnType.VARCHAR
import com.kotlinorm.interfaces.KPojo

const val COMPLEX_OPEN_STATUS: Int = 0
const val COMPLEX_PAID_STATUS: Int = 1
const val COMPLEX_CANCELLED_STATUS: Int = 2

@Table("kt_complex_customer")
data class ComplexCustomer(
    @PrimaryKey
    @ColumnType(INT)
    var id: Int? = null,

    @ColumnType(VARCHAR, 80)
    var name: String? = null,

    @ColumnType(VARCHAR, 20)
    var tier: String? = null,

    @ColumnType(INT)
    var age: Int? = null,

    @ColumnType(INT)
    var score: Int? = null,

    @ColumnType(VARCHAR, 20)
    var region: String? = null,

    @ColumnType(VARCHAR, 80)
    var referrer: String? = null,

    @ColumnType(INT)
    var archived: Int? = null,
) : KPojo

@Table("kt_complex_invoice")
data class ComplexInvoice(
    @PrimaryKey
    @ColumnType(INT)
    var id: Int? = null,

    @ColumnType(INT)
    var customerId: Int? = null,

    @ColumnType(INT)
    var status: Int? = null,

    @ColumnType(INT)
    var amount: Int? = null,

    @ColumnType(INT)
    var discount: Int? = null,

    @ColumnType(VARCHAR, 20)
    var channel: String? = null,

    @ColumnType(INT)
    var createdDay: Int? = null,

    @ColumnType(VARCHAR, 80)
    var note: String? = null,
) : KPojo

data class ComplexCustomerRow(
    var id: Int? = null,
    var name: String? = null,
    var tier: String? = null,
    var age: Int? = null,
    var score: Int? = null,
    var region: String? = null,
) : KPojo

data class ComplexCustomerCard(
    var customerId: Int? = null,
    var displayName: String? = null,
    var tier: String? = null,
    var age: Int? = null,
    var score: Int? = null,
    var region: String? = null,
    var referrer: String? = null,
    var lastPaidAmount: Int? = null,
) : KPojo

data class ComplexCustomerSearchRow(
    var customerId: Int? = null,
    var displayName: String? = null,
    var tier: String? = null,
    var region: String? = null,
    var score: Int? = null,
) : KPojo

data class ComplexJoinSubqueryRow(
    var customerId: Int? = null,
    var customerName: String? = null,
    var openAmount: Int? = null,
    var paidAmount: Int? = null,
    var channel: String? = null,
    var status: Int? = null,
) : KPojo
