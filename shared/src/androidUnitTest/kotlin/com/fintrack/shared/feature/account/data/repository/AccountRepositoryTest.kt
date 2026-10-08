package com.fintrack.shared.feature.account.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.fintrack.shared.db.AccountEntity
import com.fintrack.shared.db.BudgetEntity
import com.fintrack.shared.db.CategoryEntity
import com.fintrack.shared.db.FintrackDatabase
import com.fintrack.shared.db.TransactionEntity
import com.fintrack.shared.db.instantAdapter
import com.fintrack.shared.db.stringListAdapter
import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.account.domain.model.AccountType
import com.fintrack.shared.feature.core.logger.KMPLogger
import com.fintrack.shared.feature.core.util.Result
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccountRepositoryTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: FintrackDatabase
    private lateinit var repository: AccountRepositoryImpl

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
        repository = AccountRepositoryImpl(
            database = database,
            logger = KMPLogger()
        )
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `add and retrieve accounts successfully`() = runTest {
        val account = Account(
            id = "acc_1",
            name = "Equity Bank",
            type = AccountType.BANK,
            balance = 5000.0.toBigDecimal(),
            isDefault = false
        )

        val saveResult = repository.addOrUpdateAccount(account)
        assertTrue(saveResult is Result.Success)

        val getResult = repository.getAccounts()
        assertTrue(getResult is Result.Success)

        val accounts = getResult.data
        assertEquals(1, accounts.size)
        assertEquals("acc_1", accounts[0].id)
        assertEquals("Equity Bank", accounts[0].name)
        assertEquals(5000.0.toBigDecimal(), accounts[0].balance)
    }
}
