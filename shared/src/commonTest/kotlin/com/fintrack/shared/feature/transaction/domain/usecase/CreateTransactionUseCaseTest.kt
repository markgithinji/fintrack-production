package com.fintrack.shared.feature.transaction.domain.usecase

import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.category.domain.model.Category
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import kotlin.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CreateTransactionUseCaseTest {

    private val useCase = CreateTransactionUseCase()

    private val category = Category(id = "c1", name = "Food", isExpense = true)
    private val account = Account(id = "a1", name = "M-Pesa")

    @Test
    fun `creates transaction successfully with valid inputs`() {
        val now = Clock.System.now()
        val tx = useCase(
            amount = "500",
            transactionCost = "10",
            isIncome = false,
            category = category,
            description = "Lunch",
            selectedAccount = account,
            dateTime = now
        )

        assertNotNull(tx)
        assertEquals("a1", tx.accountId)
        assertEquals(500.0.toBigDecimal(), tx.amount)
        assertEquals(10.0.toBigDecimal(), tx.transactionCost)
        assertEquals(false, tx.isIncome)
        assertEquals("Food", tx.category)
        assertEquals("c1", tx.categoryId)
        assertEquals("Lunch", tx.description)
        assertEquals(now, tx.dateTime)
    }

    @Test
    fun `returns null when amount is invalid`() {
        val now = Clock.System.now()
        val tx = useCase(
            amount = "abc",
            isIncome = false,
            category = category,
            description = "Lunch",
            selectedAccount = account,
            dateTime = now
        )
        assertNull(tx)
    }

    @Test
    fun `returns null when account is missing`() {
        val now = Clock.System.now()
        val tx = useCase(
            amount = "500",
            isIncome = false,
            category = category,
            description = "Lunch",
            selectedAccount = null,
            dateTime = now
        )
        assertNull(tx)
    }
}
