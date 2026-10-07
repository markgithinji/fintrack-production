package com.fintrack.shared.feature.settings.domain.usecase

import com.fintrack.shared.feature.budget.domain.repository.FakeBudgetRepository
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.transaction.domain.repository.FakeTransactionRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ClearAllUserDataUseCaseTest {

    @Test
    fun `clears all user data successfully`() = runTest {
        val fakeTxRepo = FakeTransactionRepository()
        val fakeBudgetRepo = FakeBudgetRepository()
        val useCase = ClearAllUserDataUseCase(fakeTxRepo, fakeBudgetRepo)

        val result = useCase(listOf("acc_1"))
        assertTrue(result is Result.Success)
    }
}
