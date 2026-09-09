package com.fintrack.shared.feature.transaction.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.util.safeApiCall
import com.fintrack.shared.feature.transaction.data.model.toCreateRequest
import com.fintrack.shared.feature.transaction.data.model.toDomain
import com.fintrack.shared.feature.transaction.domain.model.RecurringBill
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.fintrack.shared.feature.transaction.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class TransactionRepositoryImpl(
    private val transactionApi: TransactionApi,
) : TransactionRepository {

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
    ): Result<Pair<List<Transaction>, String?>> =
        safeApiCall {
            val paginated = transactionApi.getTransactions(
                limit = limit,
                sortBy = sortBy,
                order = order,
                afterDateTime = afterDateTime,
                afterId = afterId,
                accountId = accountId,
                isIncome = isIncome,
                categoryId = categoryId,
                startDate = startDate,
                endDate = endDate,
                hasTransactionCost = hasTransactionCost
            )
            val transactions = paginated.data.map { it.toDomain() }
            transactions to paginated.nextCursor
        }

    override suspend fun addTransaction(transaction: Transaction): Result<Transaction> {
        return safeApiCall {
            val createRequest = transaction.toCreateRequest()
            val dto = transactionApi.addTransaction(createRequest)
            dto.toDomain()
        }
    }

    override suspend fun addTransactions(transactions: List<Transaction>): Result<Unit> {
        return safeApiCall {
            val requests = transactions.map { it.toCreateRequest() }
            transactionApi.addTransactions(requests)
        }
    }

    override suspend fun importMpesaTransactions(transactions: List<Transaction>): Result<Unit> {
        return safeApiCall {
            val requests = transactions.map { it.toCreateRequest() }
            transactionApi.importMpesaTransactions(requests)
        }
    }

    override suspend fun importEquityTransactions(transactions: List<Transaction>): Result<Unit> {
        return safeApiCall {
            val requests = transactions.map { it.toCreateRequest() }
            transactionApi.importEquityTransactions(requests)
        }
    }

    override suspend fun getTransaction(id: String): Result<Transaction> =
        safeApiCall {
            transactionApi.getTransaction(id).toDomain()
        }

    override suspend fun getAllTransactions(
        startDate: String?,
        endDate: String?,
        accountId: String?
    ): Result<List<Transaction>> =
        safeApiCall {
            // Fetching with a large limit for export. 
            val paginated = transactionApi.getTransactions(
                limit = 2000,
                startDate = startDate,
                endDate = endDate,
                accountId = accountId
            )
            paginated.data.map { it.toDomain() }
        }

    override suspend fun updateTransaction(id: String, transaction: Transaction): Result<Transaction> {
        return safeApiCall {
            val result = transactionApi.updateTransaction(id, transaction.toCreateRequest()).toDomain()
            result
        }
    }

    override suspend fun deleteTransaction(id: String): Result<Unit> {
        return safeApiCall {
            transactionApi.deleteTransaction(id)
        }
    }

    override suspend fun deleteAllTransactions(accountIds: List<String>?): Result<Unit> {
        return safeApiCall {
            transactionApi.deleteAllTransactions(accountIds)
        }
    }

    override suspend fun getRecurringBills(): Result<List<RecurringBill>> = safeApiCall {
        transactionApi.getRecurringBills()
    }

    override fun getTransactionsPagingFlow(
        accountId: String?,
        isIncome: Boolean?,
        categoryId: String?,
        startDate: String?,
        endDate: String?,
        hasTransactionCost: Boolean?
    ): Flow<PagingData<Transaction>> {
        return createPager {
            TransactionPagingSource(
                transactionApi = transactionApi,
                accountId = accountId,
                isIncome = isIncome,
                categoryId = categoryId,
                startDate = startDate,
                endDate = endDate,
                hasTransactionCost = hasTransactionCost
            )
        }
    }

    private fun createPager(
        pagingSourceFactory: () -> PagingSource<String, Transaction>
    ): Flow<PagingData<Transaction>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                initialLoadSize = INITIAL_LOAD_SIZE,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = pagingSourceFactory
        ).flow
            .distinctUntilChanged()
    }

    companion object {
        private const val PAGE_SIZE = 20
        private const val INITIAL_LOAD_SIZE = 20
        private const val PREFETCH_DISTANCE = 10
    }
}
