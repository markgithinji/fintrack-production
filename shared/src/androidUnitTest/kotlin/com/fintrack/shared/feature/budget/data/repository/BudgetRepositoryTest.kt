package com.fintrack.shared.feature.budget.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.fintrack.shared.db.AccountEntity
import com.fintrack.shared.db.BudgetEntity
import com.fintrack.shared.db.CategoryEntity
import com.fintrack.shared.db.FintrackDatabase
import com.fintrack.shared.db.TransactionEntity
import com.fintrack.shared.db.instantAdapter
import com.fintrack.shared.db.stringListAdapter
import com.fintrack.shared.feature.budget.domain.model.Budget
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.core.logger.KMPLogger
import com.fintrack.shared.feature.core.util.Result
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BudgetRepositoryTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: FintrackDatabase
    private lateinit var repository: BudgetRepositoryImpl

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).apply {
            FintrackDatabase.Schema.create(this)
        }
        database = FintrackDatabase(
            driver = driver,
            AccountEntityAdapter = AccountEntity.Adapter(
                linkedSourcesAdapter = stringListAdapter,
                createdAtAdapter = instantAdapter,
                lastSyncedAtAdapter = instantAdapter
            ),
            CategoryEntityAdapter = CategoryEntity.Adapter(
                createdAtAdapter = instantAdapter
            ),
            TransactionEntityAdapter = TransactionEntity.Adapter(
                dateTimeAdapter = instantAdapter,
                createdAtAdapter = instantAdapter,
                updatedAtAdapter = instantAdapter
            ),
            BudgetEntityAdapter = BudgetEntity.Adapter(
                createdAtAdapter = instantAdapter
            )
        )
        repository = BudgetRepositoryImpl(
            database = database,
            logger = KMPLogger()
        )
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `add and retrieve budget successfully`() = runTest {
        val budget = Budget(
            id = "b_1",
            accountIds = listOf("acc_1"),
            name = "Food",
            categories = listOf(Category(id = "cat_1", name = "Food", isExpense = true)),
            limit = 5000.0.toBigDecimal(),
            isExpense = true,
            startDate = LocalDate(2026, 1, 1),
            endDate = LocalDate(2026, 1, 31)
        )

        val saveResult = repository.addOrUpdateBudget(budget)
        assertTrue(saveResult is Result.Success)

        val getResult = repository.getBudgets()
        assertTrue(getResult is Result.Success)

        val budgets = getResult.data
        assertEquals(1, budgets.size)
        assertEquals("b_1", budgets[0].budget.id)
        assertEquals("Food", budgets[0].budget.name)
        assertEquals(5000.0.toBigDecimal(), budgets[0].budget.limit)
    }
}
