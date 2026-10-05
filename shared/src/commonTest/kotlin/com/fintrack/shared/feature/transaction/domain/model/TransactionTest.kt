package com.fintrack.shared.feature.transaction.domain.model

import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class TransactionTest {

    @Test
    fun `expense total amount includes transaction cost`() {
        val transaction = Transaction(
            accountId = "acc_1",
            isIncome = false,
            amount = 1000.0.toBigDecimal(),
            transactionCost = 25.0.toBigDecimal(),
            categoryId = "cat_1",
            dateTime = Instant.fromEpochMilliseconds(0),
            description = "Test Expense"
        )

        assertEquals(1025.0.toBigDecimal(), transaction.totalAmount)
    }

    @Test
    fun `income total amount subtracts transaction cost`() {
        val transaction = Transaction(
            accountId = "acc_1",
            isIncome = true,
            amount = 5000.0.toBigDecimal(),
            transactionCost = 50.0.toBigDecimal(),
            categoryId = "cat_1",
            dateTime = Instant.fromEpochMilliseconds(0),
            description = "Test Income"
        )

        assertEquals(4950.0.toBigDecimal(), transaction.totalAmount)
    }
}
