package com.fintrack.shared.feature.account.domain.usecase

import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.account.domain.repository.FakeAccountRepository
import com.fintrack.shared.feature.core.util.Result
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetAccountsUseCaseTest {

    private val fakeRepo = FakeAccountRepository()
    private val useCase = GetAccountsUseCase(fakeRepo)

    @Test
    fun `sorts default account first then by creation time`() = runTest {
        val t1 = Instant.fromEpochMilliseconds(100)
        val t2 = Instant.fromEpochMilliseconds(200)

        val acc1 = Account(id = "1", name = "Equity", isDefault = false, createdAt = t1)
        val acc2 = Account(id = "2", name = "M-Pesa", isDefault = true, createdAt = t2)
        val acc3 = Account(id = "3", name = "Cash", isDefault = false, createdAt = t2)

        fakeRepo.setAccounts(listOf(acc1, acc2, acc3))

        val result = useCase()
        assertTrue(result is Result.Success)
        val sorted = result.data

        // Default account (acc2) should be first
        assertEquals("2", sorted[0].id)
        // Between acc1 and acc3 (both not default), acc1 has earlier createdAt (t1 vs t2)
        assertEquals("1", sorted[1].id)
        assertEquals("3", sorted[2].id)
    }
}
