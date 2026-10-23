package com.kotlinorm.beans.task
expect interface TransactionSavepoint
expect class TransactionScope {
    fun savepoint(name: String): TransactionSavepoint
    fun rollbackToSavepoint(savepoint: TransactionSavepoint)
    fun releaseSavepoint(savepoint: TransactionSavepoint)
}

internal expect fun executionTransactionScope(scope: com.kotlinorm.interfaces.KronosTransactionScope): TransactionScope
