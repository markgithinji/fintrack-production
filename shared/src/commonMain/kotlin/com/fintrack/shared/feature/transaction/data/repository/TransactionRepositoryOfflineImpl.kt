package com.fintrack.shared.feature.transaction.data.repository

import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import app.cash.paging.map
import app.cash.sqldelight.paging3.QueryPagingSource
import com.fintrack.shared.db.FintrackDatabase
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.transaction.domain.model.RecurringBill
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.fintrack.shared.feature.transaction.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.fintrack.shared.feature.core.util.DateTimeHelper
import com.fintrack.shared.feature.core.logger.KMPLogger
import com.fintrack.shared.feature.core.util.randomUUID
import kotlin.math.abs

class TransactionRepositoryOfflineImpl(
    private val database: FintrackDatabase,
    private val logger: KMPLogger
) : TransactionRepository {

    private val queries = database.fintrackDatabaseQueries
    private val offlineUserId = "offline_user"
    private val TAG = "TransactionRepo"

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
    ): Result<Pair<List<Transaction>, String?>> = withContext(Dispatchers.IO) {
        try {
            val transactions = if (accountId != null) {
                queries.selectRecentTransactionsByAccount(accountId, limit.toLong()).executeAsList().map { row ->
                    Transaction(
                        id = row.id,
                        accountId = row.accountId,
                        isIncome = row.isIncome != 0L,
                        amount = BigDecimal.parseString(row.amount),
                        transactionCost = BigDecimal.parseString(row.transactionCost),
                        category = "Uncategorized",
                        categoryId = row.categoryId,
                        dateTime = row.dateTime,
                        description = row.description,
                        externalId = row.externalId,
                        balance = row.balance?.let { BigDecimal.parseString(it) }
                    )
                }
            } else {
                queries.selectAllTransactions(offlineUserId).executeAsList().take(limit).map { row ->
                    Transaction(
                        id = row.id,
                        accountId = row.accountId,
                        isIncome = row.isIncome != 0L,
                        amount = BigDecimal.parseString(row.amount),
                        transactionCost = BigDecimal.parseString(row.transactionCost),
                        category = "Uncategorized",
                        categoryId = row.categoryId,
                        dateTime = row.dateTime,
                        description = row.description,
                        externalId = row.externalId,
                        balance = row.balance?.let { BigDecimal.parseString(it) }
                    )
                }
            }
            Result.Success(transactions to null)
        } catch (e: Exception) {
            logger.error(TAG, "Error fetching transactions", e)
            Result.Error(e)
        }
    }

    override suspend fun addTransaction(transaction: Transaction): Result<Transaction> = withContext(Dispatchers.IO) {
        try {
            queries.insertTransaction(
                id = transaction.id ?: randomUUID(),
                userId = offlineUserId,
                accountId = transaction.accountId,
                categoryId = transaction.categoryId,
                isIncome = if (transaction.isIncome) 1L else 0L,
                amount = transaction.amount.toPlainString(),
                transactionCost = transaction.transactionCost.toPlainString(),
                dateTime = transaction.dateTime,
                description = transaction.description,
                externalId = transaction.externalId,
                balance = transaction.balance?.toPlainString(),
                createdAt = DateTimeHelper.now(),
                updatedAt = DateTimeHelper.now()
            )
            Result.Success(transaction)
        } catch (e: Exception) {
            logger.error(TAG, "Error adding transaction", e)
            Result.Error(e)
        }
    }

    override suspend fun addTransactions(transactions: List<Transaction>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.transaction {
                transactions.forEach { transaction ->
                    queries.insertTransaction(
                        id = transaction.id ?: randomUUID(),
                        userId = offlineUserId,
                        accountId = transaction.accountId,
                        categoryId = transaction.categoryId,
                        isIncome = if (transaction.isIncome) 1L else 0L,
                        amount = transaction.amount.toPlainString(),
                        transactionCost = transaction.transactionCost.toPlainString(),
                        dateTime = transaction.dateTime,
                        description = transaction.description,
                        externalId = transaction.externalId,
                        balance = transaction.balance?.toPlainString(),
                        createdAt = DateTimeHelper.now(),
                        updatedAt = DateTimeHelper.now()
                    )
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.error(TAG, "Error adding multiple transactions", e)
            Result.Error(e)
        }
    }

    override suspend fun importMpesaTransactions(transactions: List<Transaction>): Result<Unit> = addTransactions(transactions)

    override suspend fun importEquityTransactions(transactions: List<Transaction>): Result<Unit> = addTransactions(transactions)

    override suspend fun getTransaction(id: String): Result<Transaction> = withContext(Dispatchers.IO) {
        try {
            val row = queries.selectTransactionById(id).executeAsOneOrNull()
            if (row != null) {
                Result.Success(
                    Transaction(
                        id = row.id,
                        accountId = row.accountId,
                        isIncome = row.isIncome != 0L,
                        amount = BigDecimal.parseString(row.amount),
                        transactionCost = BigDecimal.parseString(row.transactionCost),
                        category = "Uncategorized",
                        categoryId = row.categoryId,
                        dateTime = row.dateTime,
                        description = row.description,
                        externalId = row.externalId,
                        balance = row.balance?.let { BigDecimal.parseString(it) }
                    )
                )
            } else {
                Result.Error(Exception("Transaction not found"))
            }
        } catch (e: Exception) {
            logger.error(TAG, "Error fetching transaction $id", e)
            Result.Error(e)
        }
    }

    override suspend fun getAllTransactions(
        startDate: String?,
        endDate: String?,
        accountId: String?
    ): Result<List<Transaction>> = withContext(Dispatchers.IO) {
        try {
            val transactions = if (accountId != null) {
                queries.selectTransactionsByAccount(accountId).executeAsList().map { row ->
                    Transaction(
                        id = row.id,
                        accountId = row.accountId,
                        isIncome = row.isIncome != 0L,
                        amount = BigDecimal.parseString(row.amount),
                        transactionCost = BigDecimal.parseString(row.transactionCost),
                        category = "Uncategorized",
                        categoryId = row.categoryId,
                        dateTime = row.dateTime,
                        description = row.description,
                        externalId = row.externalId,
                        balance = row.balance?.let { BigDecimal.parseString(it) }
                    )
                }
            } else {
                queries.selectAllTransactions(offlineUserId).executeAsList().map { row ->
                    Transaction(
                        id = row.id,
                        accountId = row.accountId,
                        isIncome = row.isIncome != 0L,
                        amount = BigDecimal.parseString(row.amount),
                        transactionCost = BigDecimal.parseString(row.transactionCost),
                        category = "Uncategorized",
                        categoryId = row.categoryId,
                        dateTime = row.dateTime,
                        description = row.description,
                        externalId = row.externalId,
                        balance = row.balance?.let { BigDecimal.parseString(it) }
                    )
                }
            }
            Result.Success(transactions)
        } catch (e: Exception) {
            logger.error(TAG, "Error fetching all transactions", e)
            Result.Error(e)
        }
    }

    override suspend fun updateTransaction(id: String, transaction: Transaction): Result<Transaction> = addTransaction(transaction)

    override suspend fun deleteTransaction(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.deleteTransaction(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.error(TAG, "Error deleting transaction $id", e)
            Result.Error(e)
        }
    }

    override suspend fun deleteAllTransactions(accountIds: List<String>?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.transaction {
                val txs = queries.selectAllTransactions(offlineUserId).executeAsList()
                txs.forEach { queries.deleteTransaction(it.id) }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.error(TAG, "Error deleting transactions for accounts $accountIds", e)
            Result.Error(e)
        }
    }

    override suspend fun getRecurringBills(): Result<List<RecurringBill>> = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch last 90 days of transactions (Backend Parity)
            val now = DateTimeHelper.now()
            val analysisStart = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() - 90 * 24 * 60 * 60 * 1000L)
            
            val transactions = queries.selectTransactionsByRange(
                userId = offlineUserId,
                start = analysisStart,
                end = now
            ).executeAsList()

            // 2. Group by normalized description and category (Ignoring SMS specific Ref IDs)
            val groups = transactions
                .filter { it.isIncome == 0L } // Only expenses
                .groupBy { row ->
                    val normalizedDesc = row.description?.split("(Ref:")?.get(0)?.trim()?.lowercase() ?: ""
                    "${row.categoryId}|$normalizedDesc"
                }

            val recurringBills = mutableListOf<RecurringBill>()

            // 3. Detect regular intervals (~30 days)
            groups.forEach { (_, txns) ->
                if (txns.size >= 3) {
                    val sortedTxns = txns.sortedBy { it.dateTime.toEpochMilliseconds() }
                    
                    val intervals = mutableListOf<Long>()
                    for (i in 0 until (sortedTxns.size - 1)) {
                        val diffMs = sortedTxns[i + 1].dateTime.toEpochMilliseconds() - sortedTxns[i].dateTime.toEpochMilliseconds()
                        intervals.add(diffMs / (24 * 60 * 60 * 1000L))
                    }

                    val avgInterval = if (intervals.isEmpty()) 0.0 else intervals.average()
                    val isRegular = if (intervals.isEmpty()) false 
                        else intervals.all { abs(it - avgInterval) <= 3 } && avgInterval in 25.0..35.0

                    if (isRegular) {
                        val lastTxn = sortedTxns.last()
                        val avgAmountRaw = sortedTxns.sumOf { BigDecimal.parseString(it.amount).toDouble(false) } / sortedTxns.size
                        
                        val name = lastTxn.description?.split("(Ref:")?.get(0)?.trim() ?: "Recurring Bill"
                        val nextDueDate = lastTxn.dateTime.toLocalDateTime(TimeZone.currentSystemDefault()).date.plus(DatePeriod(days = 30))

                        recurringBills.add(
                            RecurringBill(
                                id = randomUUID(),
                                name = name,
                                amount = BigDecimal.fromDouble(avgAmountRaw),
                                category = lastTxn.categoryName ?: "General",
                                categoryId = lastTxn.categoryId,
                                frequency = "Monthly",
                                nextDueDate = nextDueDate.toString(),
                                isActive = true
                            )
                        )
                    }
                }
            }

            Result.Success(recurringBills)
        } catch (e: Exception) {
            logger.error(TAG, "Error detecting recurring bills", e)
            Result.Error(e)
        }
    }

    override fun getTransactionsPagingFlow(
        accountId: String?,
        isIncome: Boolean?,
        categoryId: String?,
        startDate: String?,
        endDate: String?,
        hasTransactionCost: Boolean?
    ): Flow<PagingData<Transaction>> {
        val timeZone = TimeZone.currentSystemDefault()
        val start = startDate?.let { 
            try { LocalDate.parse(it).atStartOfDayIn(timeZone) } catch (e: Exception) { null } 
        }
        val end = endDate?.let { 
            try { LocalDate.parse(it).atTime(23, 59, 59).toInstant(timeZone) } catch (e: Exception) { null } 
        }
        val isIncomeLong = isIncome?.let { if (it) 1L else 0L }

        val categoryIds = categoryId?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
        val useCategoryFilter = if (categoryIds.isNotEmpty()) 1L else 0L

        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                QueryPagingSource(
                    countQuery = queries.countTransactions(
                        userId = offlineUserId,
                        accountId = accountId,
                        isIncome = isIncomeLong,
                        useCategoryFilter = useCategoryFilter,
                        categoryIds = categoryIds,
                        start = start,
                        end = end,
                        hasTransactionCost = hasTransactionCost
                    ),
                    transacter = queries,
                    context = Dispatchers.IO,
                    queryProvider = { limit, offset ->
                        queries.selectTransactionsPaged(
                            userId = offlineUserId,
                            accountId = accountId,
                            isIncome = isIncomeLong,
                            useCategoryFilter = useCategoryFilter,
                            categoryIds = categoryIds,
                            start = start,
                            end = end,
                            hasTransactionCost = hasTransactionCost,
                            limit = limit,
                            offset = offset
                        )
                    }
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { row ->
                Transaction(
                    id = row.id,
                    accountId = row.accountId,
                    isIncome = row.isIncome != 0L,
                    amount = BigDecimal.parseString(row.amount),
                    transactionCost = BigDecimal.parseString(row.transactionCost),
                    category = row.categoryName ?: "Uncategorized",
                    categoryId = row.categoryId,
                    dateTime = row.dateTime,
                    description = row.description,
                    externalId = row.externalId,
                    balance = row.balance?.let { BigDecimal.parseString(it) }
                )
            }
        }
    }
}
