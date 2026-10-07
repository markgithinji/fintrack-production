package com.fintrack.shared.feature.settings.domain.usecase

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.fintrack.shared.db.AccountEntity
import com.fintrack.shared.db.BudgetEntity
import com.fintrack.shared.db.CategoryEntity
import com.fintrack.shared.db.FintrackDatabase
import com.fintrack.shared.db.TransactionEntity
import com.fintrack.shared.db.instantAdapter
import com.fintrack.shared.db.stringListAdapter
import com.fintrack.shared.feature.core.logger.KMPLogger
import com.fintrack.shared.feature.core.util.FakeFileSaver
import com.fintrack.shared.feature.core.util.Result
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class BackupRestoreUseCaseTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: FintrackDatabase
    private lateinit var fakeFileSaver: FakeFileSaver
    private lateinit var backupRestoreUseCase: BackupRestoreUseCase

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
        fakeFileSaver = FakeFileSaver()
        backupRestoreUseCase = BackupRestoreUseCase(
            database = database,
            fileSaver = fakeFileSaver,
            logger = KMPLogger()
        )
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `create backup json successfully`() = runTest {
        val result = backupRestoreUseCase.createBackupJson()
        assertTrue(result is Result.Success)
        val jsonString = result.data
        assertTrue(jsonString.contains("accounts") || jsonString.contains("transactions"))
    }
}
