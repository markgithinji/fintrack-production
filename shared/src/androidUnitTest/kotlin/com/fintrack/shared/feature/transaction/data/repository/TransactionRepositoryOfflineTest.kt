package com.fintrack.shared.feature.transaction.data.repository

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
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransactionRepositoryOfflineTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: FintrackDatabase
    private lateinit var repository: TransactionRepositoryOfflineImpl

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
        repository = TransactionRepositoryOfflineImpl(
            database = database,
            logger = KMPLogger()
        )

        // Seed required account and category
        val now = Clock.System.now()
        database.fintrackDatabaseQueries.insertAccount(
            id = "acc_1",
            userId = userId,
            name = "M-Pesa",
            isDefault = 1L,
            type = "MPESA",
            balance = "1000.0",
            linkedSources = emptyList(),
            createdAt = now,
            lastSyncedAt = null
        )
        database.fintrackDatabaseQueries.insertCategory(
            id = "cat_1",
            userId = userId,
            name = "Food",
            isExpense = 1L,
            iconName = "food",
            isDefault = 1L,
            createdAt = now
        )
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `insert and retrieve transactions successfully`() = runTest {
        val now = Clock.System.now()
        val transaction = Transaction(
            id = "tx_1",
            accountId = "acc_1",
            isIncome = false,
            amount = 250.0.toBigDecimal(),
            transactionCost = 10.0.toBigDecimal(),
            category = "Food",
            categoryId = "cat_1",
            dateTime = now,
            description = "Lunch",
            externalId = null,
            balance = 740.0.toBigDecimal()
        )

        // Save transaction
        val saveResult = repository.addTransaction(transaction)
        assertTrue(saveResult is Result.Success, "Expected success when inserting transaction")

        // Fetch transactions
        val getResult = repository.getTransactions(
            limit = 10,
            sortBy = "date",
            order = "DESC"
        )
        assertTrue(getResult is Result.Success, "Expected success when fetching transactions")

        val (transactions, _) = getResult.data
        assertEquals(1, transactions.size)
        assertEquals("tx_1", transactions[0].id)
        assertEquals("Lunch", transactions[0].description)
        assertEquals(250.0.toBigDecimal(), transactions[0].amount)
        assertEquals(10.0.toBigDecimal(), transactions[0].transactionCost)
    }
}
