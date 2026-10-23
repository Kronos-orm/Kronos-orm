package com.kotlinorm.beans.task
import com.kotlinorm.interfaces.KronosTransactionScope
import com.kotlinorm.interfaces.KronosSavepointToken
actual typealias TransactionSavepoint = KronosSavepointToken
actual class TransactionScope(private val delegate: KronosTransactionScope) {
    actual fun savepoint(name: String): TransactionSavepoint = delegate.savepoint(name)
    actual fun rollbackToSavepoint(savepoint: TransactionSavepoint) = delegate.rollbackToSavepoint(savepoint)
    actual fun releaseSavepoint(savepoint: TransactionSavepoint) = delegate.releaseSavepoint(savepoint)
}

internal actual fun executionTransactionScope(scope: KronosTransactionScope): TransactionScope = TransactionScope(scope)
