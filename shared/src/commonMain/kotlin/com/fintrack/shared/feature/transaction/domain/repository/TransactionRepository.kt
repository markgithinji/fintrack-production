package com.fintrack.shared.feature.transaction.domain.repository

import androidx.paging.PagingData
import androidx.paging.PagingSource
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.transaction.domain.model.RecurringBill
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun getTransactions(
        limit: Int,
        sortBy: String,
        order: String,
        afterDateTime: String? = null,
        afterId: String? = null,
        accountId: String? = null,
        isIncome: Boolean? = null,
        categoryId: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        hasTransactionCost: Boolean? = null
    ): Result<Pair<List<Transaction>, String?>>

    suspend fun addTransaction(transaction: Transaction): Result<Transaction>

    suspend fun addTransactions(transactions: List<Transaction>): Result<Unit>

    suspend fun importMpesaTransactions(transactions: List<Transaction>): Result<Unit>

    suspend fun importEquityTransactions(transactions: List<Transaction>): Result<Unit>

    suspend fun getTransaction(id: String): Result<Transaction>

    suspend fun getAllTransactions(
        startDate: String? = null,
        endDate: String? = null,
        accountId: String? = null
    ): Result<List<Transaction>>

    suspend fun updateTransaction(id: String, transaction: Transaction): Result<Transaction>

    suspend fun deleteTransaction(id: String): Result<Unit>

    suspend fun deleteAllTransactions(accountIds: List<String>? = null): Result<Unit>

    suspend fun getRecurringBills(): Result<List<RecurringBill>>

    fun getTransactionsPagingFlow(
        accountId: String?,
        isIncome: Boolean? = null,
        categoryId: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        hasTransactionCost: Boolean? = null
    ): Flow<PagingData<Transaction>>
}