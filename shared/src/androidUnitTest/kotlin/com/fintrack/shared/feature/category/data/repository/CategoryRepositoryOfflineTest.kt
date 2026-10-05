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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class CategoryRepositoryOfflineTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: FintrackDatabase
    private lateinit var repository: CategoryRepositoryOfflineImpl

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
        repository = CategoryRepositoryOfflineImpl(
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
}
