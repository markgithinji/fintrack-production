package com.fintrack.shared.feature.summary.domain.usecase

import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.transaction.domain.repository.FakeTransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DetectSpendSpikesUseCaseTest {

    private val transactionRepository = FakeTransactionRepository()
    private val useCase = DetectSpendSpikesUseCase(transactionRepository)

    @Test
    fun `detects spend spikes successfully`() = runTest {
        val result = useCase()
        assertTrue(result is Result.Success)
    }
}
