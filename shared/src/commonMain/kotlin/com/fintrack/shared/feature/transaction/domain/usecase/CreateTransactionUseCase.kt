package com.fintrack.shared.feature.transaction.domain.usecase

import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.Instant

class CreateTransactionUseCase {
    operator fun invoke(
        amount: String,
        transactionCost: String = "0",
        isIncome: Boolean,
        category: Category?,
        description: String,
        selectedAccount: Account?,
        dateTime: Instant
    ): Transaction? {
        val parsedAmount = try { BigDecimal.parseString(amount) } catch (e: Exception) { null } ?: return null
        val parsedCost = try { BigDecimal.parseString(transactionCost) } catch (e: Exception) { BigDecimal.ZERO }

        return Transaction(
            id = null,
            accountId = selectedAccount?.id ?: return null,
            amount = parsedAmount,
            transactionCost = parsedCost,
            isIncome = isIncome,
            category = category?.name,
            categoryId = category?.id ?: return null,
            description = description.takeIf { it.isNotBlank() },
            dateTime = dateTime
        )
    }
}
