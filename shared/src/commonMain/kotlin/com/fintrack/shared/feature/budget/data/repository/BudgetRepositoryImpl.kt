package com.fintrack.shared.feature.budget.data.repository

import com.fintrack.shared.db.FintrackDatabase
import com.fintrack.shared.feature.budget.domain.model.Budget
import com.fintrack.shared.feature.budget.domain.model.BudgetStatus
import com.fintrack.shared.feature.budget.domain.model.BudgetWithStatus
import com.fintrack.shared.feature.budget.domain.repository.BudgetRepository
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.model.allCategories
import com.fintrack.shared.feature.core.logger.KMPLogger
import com.fintrack.shared.feature.core.util.DateTimeHelper
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.util.randomUUID
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.DecimalMode
import com.ionspin.kotlin.bignum.decimal.RoundingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant

class BudgetRepositoryImpl(
    private val database: FintrackDatabase,
    private val logger: KMPLogger
) : BudgetRepository {

    private val queries = database.fintrackDatabaseQueries
    private val offlineUserId = "offline_user"
    private val TAG = "BudgetRepo"
    private val ratioMode = DecimalMode(decimalPrecision = 20, roundingMode = RoundingMode.ROUND_HALF_AWAY_FROM_ZERO)

    override suspend fun getBudgets(
        accountId: String?
    ): Result<List<BudgetWithStatus>> = withContext(Dispatchers.IO) {
        try {
            val budgets = queries.selectAllBudgets(offlineUserId).executeAsList().map { row ->
                val categoryIds = row.categoryIds.split(",").filter { it.isNotEmpty() }
                val budget = Budget(
                    id = row.id,
                    name = row.name,
                    limit = BigDecimal.parseString(row.limitAmount),
                    isExpense = row.isExpense != 0L,
                    startDate = LocalDate.parse(row.startDate),
                    endDate = LocalDate.parse(row.endDate),
                    categories = categoryIds.mapNotNull { id -> Category.allCategories.find { it.id == id } },
                    accountIds = row.accountIds.split(",").filter { it.isNotEmpty() }
                )

                calculateBudgetStatus(budget)
            }
            Result.Success(budgets)
        } catch (e: Exception) {
            logger.error(TAG, "Error fetching budgets", e)
            Result.Error(e)
        }
    }

    override suspend fun getBudgetById(id: String): Result<BudgetWithStatus> = withContext(Dispatchers.IO) {
        try {
            val row = queries.selectBudgetById(id).executeAsOneOrNull()
            if (row != null) {
                val categoryIds = row.categoryIds.split(",").filter { it.isNotEmpty() }
                val budget = Budget(
                    id = row.id,
                    name = row.name,
                    limit = BigDecimal.parseString(row.limitAmount),
                    isExpense = row.isExpense != 0L,
                    startDate = LocalDate.parse(row.startDate),
                    endDate = LocalDate.parse(row.endDate),
                    categories = categoryIds.mapNotNull { catId -> Category.allCategories.find { it.id == catId } },
                    accountIds = row.accountIds.split(",").filter { it.isNotEmpty() }
                )
                Result.Success(calculateBudgetStatus(budget))
            } else {
                Result.Error(Exception("Budget not found"))
            }
        } catch (e: Exception) {
            logger.error(TAG, "Error fetching budget $id", e)
            Result.Error(e)
        }
    }

    private fun calculateBudgetStatus(budget: Budget): BudgetWithStatus {
        val start = budget.startDate.atStartOfDayIn(TimeZone.currentSystemDefault())
        val end = budget.endDate.atTime(23, 59, 59).toInstant(TimeZone.currentSystemDefault())

        val transactions = queries.selectAllTransactions(offlineUserId).executeAsList().filter { tx ->
            val inRange = tx.dateTime >= start && tx.dateTime <= end
            val inCategories = budget.categories.isEmpty() || budget.categories.any { it.id == tx.categoryId }
            val inAccounts = budget.accountIds.isEmpty() || budget.accountIds.contains(tx.accountId)
            val correctType = if (budget.isExpense) tx.isIncome == 0L else tx.isIncome != 0L
            
            inRange && inCategories && inAccounts && correctType
        }

        val spent = transactions.fold(BigDecimal.ZERO) { acc, tx ->
            val amount = BigDecimal.parseString(tx.amount)
            val cost = BigDecimal.parseString(tx.transactionCost)
            // Backend Parity: Use Net Impact (Amount + Cost for expenses, Amount - Cost for income)
            val netImpact = if (tx.isIncome != 0L) (amount - cost) else (amount + cost)
            acc + netImpact
        }

        val percentage = if (budget.limit > BigDecimal.ZERO) {
            spent.divide(budget.limit, ratioMode).multiply(BigDecimal.fromInt(100))
        } else BigDecimal.ZERO

        return BudgetWithStatus(
            budget = budget,
            status = BudgetStatus(
                spent = spent,
                remaining = budget.limit - spent,
                percentageUsed = percentage,
                isExceeded = spent > budget.limit
            )
        )
    }

    override suspend fun addOrUpdateBudget(budget: Budget): Result<Budget> = withContext(Dispatchers.IO) {
        try {
            val id = budget.id ?: randomUUID()
            queries.insertBudget(
                id = id,
                userId = offlineUserId,
                name = budget.name,
                limitAmount = budget.limit.toPlainString(),
                isExpense = if (budget.isExpense) 1L else 0L,
                startDate = budget.startDate.toString(),
                endDate = budget.endDate.toString(),
                categoryIds = budget.categories.joinToString(",") { it.id },
                accountIds = budget.accountIds.joinToString(","),
                createdAt = DateTimeHelper.now()
            )
            Result.Success(budget.copy(id = id))
        } catch (e: Exception) {
            logger.error(TAG, "Error saving budget ${budget.name}", e)
            Result.Error(e)
        }
    }

    override suspend fun deleteBudget(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.deleteBudget(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.error(TAG, "Error deleting budget $id", e)
            Result.Error(e)
        }
    }

    override suspend fun deleteAllBudgets(accountIds: List<String>?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.deleteAllBudgets(offlineUserId)
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.error(TAG, "Error deleting all budgets", e)
            Result.Error(e)
        }
    }
}
