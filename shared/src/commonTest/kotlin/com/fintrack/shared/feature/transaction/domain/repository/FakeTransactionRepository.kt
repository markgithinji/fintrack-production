package com.fintrack.shared.feature.transaction.domain.repository

import app.cash.paging.PagingData
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.transaction.domain.model.RecurringBill
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeTransactionRepository : TransactionRepository {

    private val transactions = mutableListOf<Transaction>()
    var shouldReturnError = false

    fun setTransactions(list: List<Transaction>) {
        transactions.clear()
        transactions.addAll(list)
    }

    override suspend fun getTransactions(
        limit: Int,
        sortBy: String,
        order: String,
        afterDateTime: String?,
        afterId: String?,
        accountId: String?,
        isIncome: Boolean?,
        categoryId: String?,
        startDate: String?,
        endDate: String?,
        hasTransactionCost: Boolean?
    ): Result<Pair<List<Transaction>, String?>> {
        if (shouldReturnError) {
            return Result.Error(Exception("Mock error"))
        }
        var filtered = transactions.toList()
        if (isIncome != null) {
            filtered = filtered.filter { it.isIncome == isIncome }
        }
        return Result.Success(Pair(filtered, null))
    }

    override suspend fun addTransaction(transaction: Transaction): Result<Transaction> {
        transactions.add(transaction)
        return Result.Success(transaction)
    }

    override suspend fun addTransactions(transactions: List<Transaction>): Result<Unit> {
        this.transactions.addAll(transactions)
        return Result.Success(Unit)
    }

    override suspend fun importMpesaTransactions(transactions: List<Transaction>): Result<Unit> {
        this.transactions.addAll(transactions)
        return Result.Success(Unit)
    }

    override suspend fun importEquityTransactions(transactions: List<Transaction>): Result<Unit> {
        this.transactions.addAll(transactions)
        return Result.Success(Unit)
    }

    override suspend fun getTransaction(id: String): Result<Transaction> {
        val tx = transactions.find { it.id == id }
        return if (tx != null) Result.Success(tx) else Result.Error(Exception("Not found"))
    }

    override suspend fun getAllTransactions(
        startDate: String?,
        endDate: String?,
        accountId: String?
    ): Result<List<Transaction>> {
        return Result.Success(transactions.toList())
    }

    override suspend fun updateTransaction(id: String, transaction: Transaction): Result<Transaction> {
        val index = transactions.indexOfFirst { it.id == id }
        if (index != -1) {
            transactions[index] = transaction
            return Result.Success(transaction)
        }
        return Result.Error(Exception("Not found"))
    }

    override suspend fun deleteTransaction(id: String): Result<Unit> {
        transactions.removeAll { it.id == id }
        return Result.Success(Unit)
    }

    override suspend fun deleteAllTransactions(accountIds: List<String>?): Result<Unit> {
        transactions.clear()
        return Result.Success(Unit)
    }

    override suspend fun getRecurringBills(): Result<List<RecurringBill>> {
        return Result.Success(emptyList())
    }

    override fun getTransactionsPagingFlow(
        accountId: String?,
        isIncome: Boolean?,
        categoryId: String?,
        startDate: String?,
        endDate: String?,
        hasTransactionCost: Boolean?,
        sortBy: String,
        order: String
    ): Flow<PagingData<Transaction>> {
        return flowOf(PagingData.empty())
    }
}
