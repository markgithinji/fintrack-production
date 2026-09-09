package com.fintrack.shared.feature.transaction.util

import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.random.Random

/**
 * PortfolioSeeder provides dummy transaction data for app exploration and testing.
 * This can be easily toggled in the Importers to show a populated UI without using real data.
 */
class PortfolioSeeder {
    
    /**
     * Generates a list of dummy transactions for app exploration and testing.
     */
    fun generateDummyTransactions(
        accountId: String,
        categories: List<Category>,
        daysCount: Int = 180
    ): List<Transaction> {
        val dummyTransactions = mutableListOf<Transaction>()
        val now = Clock.System.now()
        val random = Random(42)

        val expenseCategories = categories.filter { it.isExpense }
        val incomeCategories = categories.filter { !it.isExpense }

        if (expenseCategories.isEmpty() || incomeCategories.isEmpty()) return emptyList()

        val merchants = listOf("KFC", "Uber", "Netflix", "Safaricom", "Zuku", "Naivas", "Carrefour", "Shell", "Java House", "Jumia", "Spotify", "Bolt", "Rubis", "Quickmart")
        val incomeSources = listOf("Salary", "Consultancy Pay", "Dividends", "Gift", "Refund", "Rental Income")

        // Iterate through each day in the range
        for (dayOffset in 0 until daysCount) {
            // Generate 2 to 5 transactions per day for a more "active" look
            val txCountForDay = random.nextInt(2, 6)
            
            for (i in 0 until txCountForDay) {
                val timestamp = now.minus(dayOffset.days)
                    .minus(random.nextInt(0, 24).hours)
                    .minus(random.nextInt(0, 60).minutes)
                
                val isIncome = random.nextFloat() > 0.85f // ~15% income
                val category = if (isIncome) incomeCategories.random(random) else expenseCategories.random(random)
                
                val amount = if (isIncome) {
                    BigDecimal.fromInt(random.nextInt(15000, 80000))
                } else {
                    BigDecimal.fromInt(random.nextInt(200, 4500))
                }
                
                val description = if (isIncome) incomeSources.random(random) else merchants.random(random)
                
                // Generate a realistic-looking transaction code (e.g., RGH1234567)
                val charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
                val randomCode = (1..10).map { charset.random(random) }.joinToString("")
                val code = "DEMO_$randomCode"

                dummyTransactions.add(
                    Transaction(
                        accountId = accountId,
                        isIncome = isIncome,
                        amount = amount,
                        transactionCost = if (isIncome) BigDecimal.ZERO else BigDecimal.fromInt(random.nextInt(10, 55)),
                        category = category.name,
                        categoryId = category.id,
                        dateTime = timestamp,
                        description = description,
                        externalId = code,
                        balance = null
                    )
                )
            }
        }
        return dummyTransactions
    }
}
