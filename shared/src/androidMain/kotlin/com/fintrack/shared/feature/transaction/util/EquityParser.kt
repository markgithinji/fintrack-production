package com.fintrack.shared.feature.transaction.util

import android.util.Log
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.model.CategoryRule
import com.fintrack.shared.feature.category.domain.model.UncategorizedExpense
import com.fintrack.shared.feature.category.domain.model.UncategorizedIncome
import com.fintrack.shared.feature.category.util.CategoryMatcher
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlin.time.Clock
import kotlin.time.Instant
import java.text.SimpleDateFormat
import java.util.Locale

object EquityParser {
    private val USD_EXCHANGE_RATE = BigDecimal.parseString("129.0")

    // Non-capturing regex components
    private const val AMOUNT_VAL = """[\d,]+\.\d{1,2}"""
    private const val CURRENCY = """KES|USD|KSH|Ksh|Kshs"""
    private const val DATE_TIME_VAL = """(?:(?:\d{1,2}[-\s/.]\w{1,3}[-\s/.]\d{2,4}|\d{4}-\d{2}-\d{2}|\d{1,2}[-/.]\d{1,2}[-/.]\d{2,4})(?:\s+(?:at\s+)?(?:\d{1,2}:\d{2}(?::\d{2})?\s*[AP]M|\d{1,2}:\d{2}(?::\d{2})?))?)"""

    // 1. Confirmed Payment of KES X to MERCHANT (Expense)
    private val paymentToMerchantRegex = """Confirmed\.?\s+Payment of ($CURRENCY)\.?\s*($AMOUNT_VAL)\s+to\s+(.+?)\s+on\s+($DATE_TIME_VAL).*?\bRef[:.]?\s*(\w+)""".toRegex(RegexOption.IGNORE_CASE)

    // 2. Debited for payment to MERCHANT (Expense)
    private val debitedPaymentRegex = """(?:Dear\s+.*?,?\s*)?($CURRENCY)\.?\s*($AMOUNT_VAL)\s+has been debited from (?:your )?account.*?\s+for payment to\s+(.+?)\s+on\s+($DATE_TIME_VAL).*?\bRef[:.]?\s*(\w+)""".toRegex(RegexOption.IGNORE_CASE)

    // 3. Card Auth (Expense)
    private val cardAuthRegex = """CONFIRMED\.?\s+($CURRENCY)\.?\s*($AMOUNT_VAL),?\s+Auth for card\s+.*?\s+at\s+(.*?)\s+on\s+($DATE_TIME_VAL).*?\bRef[:.]?\s*(\w+)""".toRegex(RegexOption.IGNORE_CASE)

    // 4. Credited / Received (Income)
    private val creditedReceivedRegex = """(?:Your account.*?has been credited with|You have received)\s+($CURRENCY)\.?\s*($AMOUNT_VAL)\s+from\s+(.+?)\s+on\s+($DATE_TIME_VAL).*?\bRef[:.]?\s*(\w+)""".toRegex(RegexOption.IGNORE_CASE)

    // 5. Sent Money / You have sent KES X to RECIPIENT (Expense)
    private val sentRegex = """(?:You have sent|($AMOUNT_VAL)\s+($CURRENCY)\.?)\s*(?:KES|USD)?\.?\s*($AMOUNT_VAL)?\s+(?:has been successfully sent )?(?:from\s+[\d*.]+\s+)?to\s+(.+?)\.\s+Ref[:.]?\s*(\w+)\s+on\s+($DATE_TIME_VAL)""".toRegex(RegexOption.IGNORE_CASE)

    // 6. Drawn / ATM Withdrawal (Expense)
    private val drawnRegex = """(?:Dear (.*?),?\s*)?(?:Withdrawal of|drawn from your account).*?($CURRENCY)\.?\s*($AMOUNT_VAL).*?\bRef[:.]?\s*(\w+)\s+on\s+($DATE_TIME_VAL)""".toRegex(RegexOption.IGNORE_CASE)

    // 7. Bill Payment (Expense)
    private val billPaymentRegex = """Confirmed,?\s+Bill payment to\s+(.*?)\s+of ($CURRENCY)\.?\s*($AMOUNT_VAL).*?\bRef[:.]?\s*(\w+)\s+on\s+($DATE_TIME_VAL)""".toRegex(RegexOption.IGNORE_CASE)

    // 8. Deposited to Equity Account (Income)
    private val depositedRegex = """(?:Dear (.*?), )?Your transaction of ($CURRENCY)\.?\s*($AMOUNT_VAL)\s+has successfully been deposited to Equity Account in favor of (.*?)\s+Ref[:.]?\s*(?:Number|No\.?)?\s*(\w+)\s+on\s+($DATE_TIME_VAL)""".toRegex(RegexOption.IGNORE_CASE)

    // 9. Generic Transfer (Fallback)
    private val transferRegex = """Your transaction of ($CURRENCY)\.?\s*($AMOUNT_VAL)\s+has been (credited to|debited from)\s+.*?\.\s+Ref[:.]?\s*(EQ\w+|\d+)\.?(?:\s+MPESA Ref\.\s+\w+)?\s+on\s+($DATE_TIME_VAL)""".toRegex(RegexOption.IGNORE_CASE)

    private val formats = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH),
        SimpleDateFormat("d MMM yyyy HH:mm:ss", Locale.ENGLISH),
        SimpleDateFormat("d MMM yyyy HH:mm", Locale.ENGLISH),
        SimpleDateFormat("d-MMM-yyyy HH:mm:ss", Locale.ENGLISH),
        SimpleDateFormat("d-MMM-yyyy HH:mm", Locale.ENGLISH),
        SimpleDateFormat("d/M/yyyy HH:mm:ss", Locale.ENGLISH),
        SimpleDateFormat("d/M/yyyy HH:mm", Locale.ENGLISH),
        SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH),
        SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.ENGLISH),
        SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a", Locale.ENGLISH),
        SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.ENGLISH),
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH),
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH),
        SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.ENGLISH),
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ENGLISH)
    )

    fun parse(
        message: String,
        accountId: String = "equity",
        smsTimestamp: Instant? = null,
        rules: List<CategoryRule> = emptyList(),
        allCategories: List<Category> = emptyList()
    ): Transaction? {
        // Skip failed alerts
        if (message.contains("failed due to insufficient funds", ignoreCase = true)) return null
        if (message.contains("is due in", ignoreCase = true)) return null

        val balance = parseBalance(message)

        fun wrap(transaction: Transaction?): Transaction? {
            if (transaction == null) return null
            if (transaction.amount <= BigDecimal.ZERO) return null
            val result = transaction.copy(rawMessage = message)
            println("[SMS_SYNC] [EquityParser] Successfully parsed transaction: ref=${result.externalId}, amount=${result.amount}, category=${result.category}, accountId=${result.accountId}")
            return result
        }

        // 1. Confirmed Payment of KES X to MERCHANT
        paymentToMerchantRegex.find(message)?.let {
            val currency = it.groupValues.getOrNull(1) ?: "KES"
            val amount = resolveAmount(currency, parseAmount(it.groupValues.getOrNull(2)))
            val merchant = (it.groupValues.getOrNull(3) ?: "Merchant").trim()
            val dateStr = (it.groupValues.getOrNull(4) ?: "").trim()
            val code = it.groupValues.getOrNull(5) ?: "EQUITY"
            val dateTime = parseDateTime(dateStr, smsTimestamp)
            val category = inferCategory(merchant, isIncome = false, rules = rules, allCategories = allCategories)
            return wrap(createTransactionModel(code, amount, BigDecimal.ZERO, balance, category, dateTime, "Paid to $merchant", accountId, false, allCategories))
        }

        // 2. Debited for payment to MERCHANT
        debitedPaymentRegex.find(message)?.let {
            val currency = it.groupValues.getOrNull(1) ?: "KES"
            val amount = resolveAmount(currency, parseAmount(it.groupValues.getOrNull(2)))
            val merchant = (it.groupValues.getOrNull(3) ?: "Merchant").trim()
            val dateStr = (it.groupValues.getOrNull(4) ?: "").trim()
            val code = it.groupValues.getOrNull(5) ?: "EQUITY"
            val dateTime = parseDateTime(dateStr, smsTimestamp)
            val category = inferCategory(merchant, isIncome = false, rules = rules, allCategories = allCategories)
            return wrap(createTransactionModel(code, amount, BigDecimal.ZERO, balance, category, dateTime, "Payment to $merchant", accountId, false, allCategories))
        }

        // 3. Card Auth
        cardAuthRegex.find(message)?.let {
            val currency = it.groupValues.getOrNull(1) ?: "KES"
            val amount = resolveAmount(currency, parseAmount(it.groupValues.getOrNull(2)))
            val merchant = (it.groupValues.getOrNull(3) ?: "Merchant").trim()
            val dateStr = (it.groupValues.getOrNull(4) ?: "").trim()
            val code = it.groupValues.getOrNull(5) ?: "EQUITY"
            val dateTime = parseDateTime(dateStr, smsTimestamp)
            val category = inferCategory(merchant, isIncome = false, rules = rules, allCategories = allCategories)
            return wrap(createTransactionModel(code, amount, BigDecimal.ZERO, balance, category, dateTime, "Card payment at $merchant", accountId, false, allCategories))
        }

        // 4. Credited / Received (Income)
        creditedReceivedRegex.find(message)?.let {
            val currency = it.groupValues.getOrNull(1) ?: "KES"
            val amount = resolveAmount(currency, parseAmount(it.groupValues.getOrNull(2)))
            val sender = (it.groupValues.getOrNull(3) ?: "Sender").trim()
            val dateStr = (it.groupValues.getOrNull(4) ?: "").trim()
            val code = it.groupValues.getOrNull(5) ?: "EQUITY"
            val dateTime = parseDateTime(dateStr, smsTimestamp)
            val category = inferCategory(sender, isIncome = true, rules = rules, allCategories = allCategories)
            return wrap(createTransactionModel(code, amount, BigDecimal.ZERO, balance, category, dateTime, "Received from $sender", accountId, true, allCategories))
        }

        // 5. Sent Money (Expense)
        sentRegex.find(message)?.let {
            val rawAmt = if ((it.groupValues.getOrNull(1) ?: "").isNotBlank()) it.groupValues[1] else (it.groupValues.getOrNull(3) ?: "")
            val currency = if ((it.groupValues.getOrNull(2) ?: "").isNotBlank()) it.groupValues[2] else "KES"
            val amount = resolveAmount(currency, parseAmount(rawAmt))
            val recipient = (it.groupValues.getOrNull(4) ?: "Recipient").trim()
            val code = it.groupValues.getOrNull(5) ?: "EQUITY"
            val dateStr = (it.groupValues.getOrNull(6) ?: "").trim()
            val dateTime = parseDateTime(dateStr, smsTimestamp)
            val category = inferCategory(recipient, isIncome = false, rules = rules, allCategories = allCategories)
            return wrap(createTransactionModel(code, amount, BigDecimal.ZERO, balance, category, dateTime, "Sent to $recipient", accountId, false, allCategories))
        }

        // 6. Drawn / ATM Withdrawal
        drawnRegex.find(message)?.let {
            val currency = it.groupValues.getOrNull(2) ?: "KES"
            val amount = resolveAmount(currency, parseAmount(it.groupValues.getOrNull(3)))
            val code = it.groupValues.getOrNull(4) ?: "EQUITY"
            val dateStr = (it.groupValues.getOrNull(5) ?: "").trim()
            val dateTime = parseDateTime(dateStr, smsTimestamp)
            return wrap(createTransactionModel(code, amount, BigDecimal.ZERO, balance, "Transfer", dateTime, "ATM Withdrawal", accountId, false, allCategories))
        }

        // 7. Bill Payment
        billPaymentRegex.find(message)?.let {
            val merchant = (it.groupValues.getOrNull(1) ?: "Merchant").trim()
            val currency = it.groupValues.getOrNull(2) ?: "KES"
            val amount = resolveAmount(currency, parseAmount(it.groupValues.getOrNull(3)))
            val code = it.groupValues.getOrNull(4) ?: "EQUITY"
            val dateStr = (it.groupValues.getOrNull(5) ?: "").trim()
            val dateTime = parseDateTime(dateStr, smsTimestamp)
            val category = inferCategory(merchant, isIncome = false, rules = rules, allCategories = allCategories)
            return wrap(createTransactionModel(code, amount, BigDecimal.ZERO, balance, category, dateTime, "Bill payment to $merchant", accountId, false, allCategories))
        }

        // 8. Deposited to Equity Account
        depositedRegex.find(message)?.let {
            val currency = it.groupValues.getOrNull(2) ?: "KES"
            val amount = resolveAmount(currency, parseAmount(it.groupValues.getOrNull(3)))
            val recipient = (it.groupValues.getOrNull(4) ?: "Recipient").trim()
            val code = it.groupValues.getOrNull(5) ?: "EQUITY"
            val dateStr = (it.groupValues.getOrNull(6) ?: "").trim()
            val dateTime = parseDateTime(dateStr, smsTimestamp)
            val isIncome = true
            val category = inferCategory(recipient, isIncome = true, rules = rules, allCategories = allCategories)
            return wrap(createTransactionModel(code, amount, BigDecimal.ZERO, balance, category, dateTime, "Deposit from $recipient", accountId, isIncome, allCategories))
        }

        // 9. Generic Transfer Fallback
        transferRegex.find(message)?.let {
            val currency = it.groupValues.getOrNull(1) ?: "KES"
            val amount = resolveAmount(currency, parseAmount(it.groupValues.getOrNull(2)))
            val type = it.groupValues.getOrNull(3) ?: ""
            val isIncome = type.equals("credited to", ignoreCase = true)
            val code = it.groupValues.getOrNull(4) ?: "EQUITY"
            val dateStr = (it.groupValues.getOrNull(5) ?: "").trim()
            val dateTime = parseDateTime(dateStr, smsTimestamp)
            val category = if (isIncome) Category.UncategorizedIncome.name else Category.UncategorizedExpense.name
            val description = if (isIncome) "Bank Credit" else "Bank Debit"
            return wrap(createTransactionModel(code, amount, BigDecimal.ZERO, balance, category, dateTime, description, accountId, isIncome, allCategories))
        }

        return null
    }

    private fun resolveAmount(currency: String, rawAmount: BigDecimal): BigDecimal {
        return if (currency.equals("USD", ignoreCase = true)) {
            rawAmount * USD_EXCHANGE_RATE
        } else {
            rawAmount
        }
    }

    private fun parseAmount(value: String?): BigDecimal {
        return try {
            value?.replace(",", "")?.let { BigDecimal.parseString(it) } ?: BigDecimal.ZERO
        } catch (_: Exception) {
            BigDecimal.ZERO
        }
    }

    private fun parseDateTime(dateStr: String, smsTimestamp: Instant? = null): Instant {
        val cleanDate = dateStr.trim().replace(Regex("""\s+"""), " ")
        for (format in formats) {
            try {
                val date = format.parse(cleanDate)
                if (date != null) return Instant.fromEpochMilliseconds(date.time)
            } catch (_: Exception) {}
        }
        return smsTimestamp ?: Clock.System.now()
    }

    private fun inferCategory(description: String, isIncome: Boolean = false, rules: List<CategoryRule> = emptyList(), allCategories: List<Category> = emptyList()): String {
        val resolvedId = CategoryMatcher.resolveCategory(
            inputCategoryId = null,
            inputCategoryName = null,
            description = description,
            isIncome = isIncome,
            allCategories = allCategories,
            rules = rules,
            defaultId = "pending"
        )

        return if (resolvedId == "pending") {
            if (isIncome) Category.UncategorizedIncome.name else Category.UncategorizedExpense.name
        } else {
            allCategories.find { it.id == resolvedId }?.name
                ?: if (isIncome) Category.UncategorizedIncome.name else Category.UncategorizedExpense.name
        }
    }

    private fun createTransactionModel(
        code: String,
        amount: BigDecimal,
        cost: BigDecimal,
        balance: BigDecimal?,
        category: String,
        dateTime: Instant,
        description: String,
        accountId: String,
        isIncome: Boolean,
        allCategories: List<Category>,
        rawMessage: String? = null
    ): Transaction {
        val matched = allCategories.find { it.name.equals(category, ignoreCase = true) && it.isExpense == !isIncome }
        val categoryId = matched?.id ?: if (isIncome) Category.UncategorizedIncome.id else Category.UncategorizedExpense.id
        val categoryName = matched?.name ?: if (isIncome) Category.UncategorizedIncome.name else Category.UncategorizedExpense.name

        return Transaction(
            accountId = accountId,
            isIncome = isIncome,
            amount = amount,
            transactionCost = cost,
            category = categoryName,
            categoryId = categoryId,
            dateTime = dateTime,
            description = "$description (Ref: $code)",
            externalId = code,
            balance = balance,
            rawMessage = rawMessage
        )
    }

    fun parseBalance(message: String): BigDecimal? {
        val balanceRegex = """(?:(?:account|available|new|your)\s+)?bal(?:ance)?\s*(?:is|was|:)?\s*($CURRENCY)\.?\s*($AMOUNT_VAL)""".toRegex(RegexOption.IGNORE_CASE)
        balanceRegex.find(message)?.let {
            return resolveAmount(it.groupValues.getOrNull(1) ?: "KES", parseAmount(it.groupValues.getOrNull(2)))
        }

        val altBalanceRegex = """balance\s+for\s+[\d*.]+\s+is\s+($CURRENCY)\.?\s*($AMOUNT_VAL)""".toRegex(RegexOption.IGNORE_CASE)
        altBalanceRegex.find(message)?.let {
            return resolveAmount(it.groupValues.getOrNull(1) ?: "KES", parseAmount(it.groupValues.getOrNull(2)))
        }

        return null
    }
}
