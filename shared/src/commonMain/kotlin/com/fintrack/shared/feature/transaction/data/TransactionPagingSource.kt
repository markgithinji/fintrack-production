package com.fintrack.shared.feature.transaction.data

import app.cash.paging.PagingSource
import app.cash.paging.PagingState
import app.cash.paging.PagingSourceLoadParams
import app.cash.paging.PagingSourceLoadResult
import app.cash.paging.PagingSourceLoadResultPage
import app.cash.paging.PagingSourceLoadResultError
import com.fintrack.shared.feature.transaction.data.model.toDomain
import com.fintrack.shared.feature.transaction.domain.model.Transaction

class TransactionPagingSource(
    private val transactionApi: TransactionApi,
    private val accountId: String?,
    private val isIncome: Boolean? = null,
    private val categoryId: String? = null,
    private val startDate: String? = null,
    private val endDate: String? = null,
    private val hasTransactionCost: Boolean? = null
) : PagingSource<String, Transaction>() {

    override suspend fun load(params: PagingSourceLoadParams<String>): PagingSourceLoadResult<String, Transaction> {
        try {
            val cursor = params.key
            val afterDateTime = cursor?.split("|")?.getOrNull(0)
            val afterId = cursor?.split("|")?.getOrNull(1)

            val result = transactionApi.getTransactions(
                limit = params.loadSize,
                sortBy = "date",
                order = "DESC",
                afterDateTime = afterDateTime,
                afterId = afterId,
                accountId = accountId,
                isIncome = isIncome,
                categoryId = categoryId,
                startDate = startDate,
                endDate = endDate,
                hasTransactionCost = hasTransactionCost
            )

            val transactions = result.data.map { it.toDomain() }
            val nextCursor = result.nextCursor

            val validNextKey = if (transactions.isEmpty() || nextCursor == cursor) null else nextCursor

            return PagingSourceLoadResultPage<String, Transaction>(
                data = transactions,
                prevKey = null,
                nextKey = validNextKey
            )
        } catch (e: Exception) {
            return PagingSourceLoadResultError<String, Transaction>(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, Transaction>): String? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }
}
