package com.fintrack.shared.feature.category.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.fintrack.shared.db.AccountEntity
import com.fintrack.shared.db.BudgetEntity
import com.fintrack.shared.db.CategoryEntity
import com.fintrack.shared.db.FintrackDatabase
import com.fintrack.shared.db.TransactionEntity
import com.fintrack.shared.db.instantAdapter
import com.fintrack.shared.db.stringListAdapter
import com.fintrack.shared.feature.core.logger.KMPLogger
import com.fintrack.shared.feature.core.util.Result
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CategoryRepositoryTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: FintrackDatabase
    private lateinit var repository: CategoryRepositoryImpl

    private val userId = "offline_user"

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
        repository = CategoryRepositoryImpl(
            database = database,
            logger = KMPLogger()
        )
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `add and retrieve category successfully`() = runTest {
        val saveResult = repository.addCategory(name = "Investment", isExpense = false, iconName = "trending_up")
        assertTrue(saveResult is Result.Success)

        val getResult = repository.getCategories()
        assertTrue(getResult is Result.Success)

        val categories = getResult.data
        assertTrue(categories.any { it.name == "Investment" })
    }

    @Test
    fun `addCategoryRule with applyToExisting updates matching existing transactions`() = runTest {
        val now = Clock.System.now()
        val queries = database.fintrackDatabaseQueries

        // Seed account and 2 categories
        queries.insertAccount("acc_1", userId, "M-Pesa", 1L, "MPESA", "1000.0", emptyList(), now, null)
        queries.insertCategory("cat_old", userId, "Misc", 1L, "misc", 1L, now)
        queries.insertCategory("cat_new", userId, "Coffee", 1L, "coffee", 0L, now)

        // Seed transaction with old category and "Java House" description and rawMessage
        queries.insertTransaction(
            id = "tx_1",
            userId = userId,
            accountId = "acc_1",
            categoryId = "cat_old",
            isIncome = 0L,
            amount = "350.0",
            transactionCost = "0.0",
            dateTime = now,
            description = "Paid to Merchant (Ref: QZE12345)",
            externalId = null,
            balance = null,
            rawMessage = "QZE12345 Confirmed. Ksh350.00 paid to Java House Westlands on 15/1/25.",
            createdAt = now,
            updatedAt = now
        )

        // Add rule with keyword "Java" and applyToExisting = true
        val ruleResult = repository.addCategoryRule(
            keyword = "Java",
            categoryId = "cat_new",
            isExpense = true,
            applyToExisting = true
        )
        assertTrue(ruleResult is Result.Success)

        // Verify transaction was updated to new category ID
        val tx = queries.selectTransactionById("tx_1").executeAsOne()
        assertEquals("cat_new", tx.categoryId)
    }
}
