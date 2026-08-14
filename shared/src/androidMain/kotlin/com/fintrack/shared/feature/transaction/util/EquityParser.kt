package com.fintrack.shared.feature.transaction.util

import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.model.CategoryRule
import com.fintrack.shared.feature.category.domain.model.fromId
import com.fintrack.shared.feature.category.domain.model.fromName
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlin.time.Clock
import kotlin.time.Instant
import java.text.SimpleDateFormat
import java.util.Locale

object EquityParser {
    private val USD_EXCHANGE_RATE = BigDecimal.parseString("129.0")

    // Regex components
    private const val AMOUNT_VAL = """([\d,]+\.\d{1,2})"""
    private const val CURRENCY = """(KES|USD|KSH|Ksh|Kshs)"""
    private const val DATE_DASH = """(\d{1,2}[-/.]\d{1,2}[-/.]\d{4})"""
    private const val DATE_TEXT = """(\d{1,2}[-\s]\w{3}[-\s]\d{4}|\d{1,2}\s+\w{3}\s+\d{4})"""
    private const val TIME = """(\d{1,2}:\d{2}(?::\d{2})?)"""
    private const val TIME_AMPM = """(\d{1,2}:\d{2}(?::\d{2})?\s*[AP]M)"""

    // 1. Card Auth (Expense) - Explicitly Bank related
    private val cardAuthRegex = """CONFIRMED\.?\s+$CURRENCY\.?\s*$AMOUNT_VAL,?\s+Auth for card\s+.*?\s+at\s+(.*?)\s+on\s+(\d{4}-\d{2}-\d{2}\s+$TIME)\s+Ref:\s*(\w+)""".toRegex(RegexOption.IGNORE_CASE)
    
    // 2. Sent Money (Expense) - Explicitly from an account number
    private val sentRegex = """$AMOUNT_VAL\s+$CURRENCY\.?\s+has been successfully sent from\s+(\d+)[*.]+(\d+)?\s+to\s+(.+?)\.\s+Ref\.\s*(\w+)\s+on\s+$DATE_TEXT\s+at\s+$TIME(?:\s+EAT)?""".toRegex(RegexOption.IGNORE_CASE)
    
    // 3. Drawn / Withdrawal (Expense) - Mentions account
    private val drawnRegex = """Dear (.*?)\s+$CURRENCY\.?\s*$AMOUNT_VAL\s+has been drawn from your account\s+(\d+)[*.]+(\d+)?\s+Ref:\s*(\w+)\s+on\s+$DATE_TEXT\s+$TIME_AMPM""".toRegex(RegexOption.IGNORE_CASE)
    
    // 4. Bill Payment (Expense) - If it looks like a bank bill payment
    private val billPaymentRegex = """Confirmed,?\s+Bill payment to\s+(.*?)\s+of $CURRENCY\.?\s*$AMOUNT_VAL\s+for account\s+(\d+)[*.]+(\d+)?\s+and Ref\.\s*(\w+)\s+on\s+$DATE_DASH\s+at\s+$TIME""".toRegex(RegexOption.IGNORE_CASE)
    
    // 5. Loan Approved (Income) - Credited to bank account
    private val loanApprovedRegex = """Confirmed\.?\s+Your application is approved and credited to your account\s+(\d+)[*.]+(\d+)?\.\s+Repayment amount of $CURRENCY\.?\s*$AMOUNT_VAL\s+will be due on\s+.*?\.\s+Reference\s+(\w+)""".toRegex(RegexOption.IGNORE_CASE)

    // 6. Deposited to Equity Account (Income)
    private val depositedRegex = """(?:Dear (.*?), )?Your transaction of $CURRENCY\.?\s*$AMOUNT_VAL\s+has successfully been deposited to Equity Account in favor of (.*?)\s+Ref\.\s*(?:Number|No\.?)?\s*(\w+)\s+on\s+$DATE_DASH\s+at\s+$TIME""".toRegex(RegexOption.IGNORE_CASE)

    // 6b. Received to Equity Account (Income)
    private val receivedEquityRegex = """You have received $CURRENCY\.?\s*$AMOUNT_VAL\s+from\s+(.*?)\s+to your Equity account\s+(\d+)[*.]+(\d+)?\.\s+Ref\.\s*(\w+)\s+on\s+$DATE_TEXT\s+at\s+$TIME(?:\s+EAT)?""".toRegex(RegexOption.IGNORE_CASE)

    // 7. Generic Transfer (Expense/Income) - Only if it has an Equity Ref (usually starts with EQ or is long)
    private val transferRegex = """Your transaction of $CURRENCY\.?\s*$AMOUNT_VAL\s+has been (credited to|debited from)\s+.*?\.\s+Ref\.\s*(EQ\w+|\d+)\.?(?:\s+MPESA Ref\.\s+\w+)?\s+on\s+(\d{1,2}[-/.]\d{1,2}[-/.]\d{4})\s+at\s+$TIME""".toRegex(RegexOption.IGNORE_CASE)

    // 8. Successfully Sent (Expense)
    private val successfullySentRegex = """$AMOUNT_VAL\s+$CURRENCY\.?\s+has been successfully sent to\s+(.*?)\.\s+Ref\.\s*(\w+)\s+on\s+$DATE_TEXT\s+at\s+$TIME(?:\s+EAT)?""".toRegex(RegexOption.IGNORE_CASE)

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
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ENGLISH),
    )

    fun parse(
        message: String, 
        accountId: String = "equity", 
        smsTimestamp: Instant? = null,
        rules: List<CategoryRule> = emptyList()
    ): Transaction? {
        // Skip messages that are clearly just M-Pesa relay notifications
        if (message.contains("has sent", ignoreCase = true) && 
            message.contains("to your MPESA", ignoreCase = true)) return null
            
        if (message.contains("Confirmed. Payment of", ignoreCase = true) && 
            !message.contains("Equity", ignoreCase = true)) return null

        // Generic Intra-account skip: If the message shows a transfer between two masked numbers
        // e.g., "sent from 1********9426 to 7********4608" -> Skip to avoid double counting internal moves
        val maskedAcc = """\d+[*.]+\d+"""
        if (message.contains(Regex("sent from $maskedAcc to $maskedAcc", RegexOption.IGNORE_CASE)) ||
            message.contains(Regex("received .* from $maskedAcc to .* $maskedAcc", RegexOption.IGNORE_CASE))) {
            return null
        }

        if (message.contains("failed due to insufficient funds", ignoreCase = true)) return null
        if (message.contains("is due in", ignoreCase = true)) return null

        val balance = parseBalance(message)

        // Helper to validate and create model
        fun wrap(transaction: Transaction?): Transaction? {
            if (transaction == null) return null
            if (transaction.amount <= BigDecimal.ZERO) {
                return null
            }
            return transaction
        }

        // 1. Card Auth
        cardAuthRegex.find(message)?.let {
            val currency = it.groupValues[1]
            val amount = resolveAmount(currency, parseAmount(it.groupValues[2]))
            val merchant = it.groupValues[3].trim()
            val dateTime = parseDateTime(it.groupValues[4], smsTimestamp)
            val code = it.groupValues[6]
            return wrap(createTransactionModel(code, amount, BigDecimal.ZERO, balance, inferCategory(merchant, isIncome = false, rules = rules), dateTime, "Card payment at $merchant", accountId, isIncome = false))
        }

        // 2. Sent Money (Expense or Income)
        sentRegex.find(message)?.let {
            val amountRaw = parseAmount(it.groupValues[1])
            val currency = it.groupValues[2]
            val amount = resolveAmount(currency, amountRaw)
            val recipient = it.groupValues[5].trim()
            val code = it.groupValues[6]
            val dateStr = "${it.groupValues[7]} ${it.groupValues[8]}"
            val dateTime = parseDateTime(dateStr, smsTimestamp)
            
            val isIncome = message.contains("to your account", ignoreCase = true)
            
            val description = if (isIncome) "Received Transfer" else "Sent to $recipient"
            return wrap(createTransactionModel(code, amount, BigDecimal.ZERO, balance, if (isIncome) "Other Income" else "Transfer", dateTime, description, accountId, isIncome))
        }

        // 3. Drawn / Withdrawal (Expense)
        drawnRegex.find(message)?.let {
            val currency = it.groupValues[2]
            val amount = resolveAmount(currency, parseAmount(it.groupValues[3]))
            val code = it.groupValues[6]
            val dateStr = "${it.groupValues[7]} ${it.groupValues[8]}"
            val dateTime = parseDateTime(dateStr, smsTimestamp)
            return wrap(createTransactionModel(code, amount, BigDecimal.ZERO, balance, "Transfer", dateTime, "Withdrawal / Drawn", accountId, false))
        }

        // 4. Bill Payment
        billPaymentRegex.find(message)?.let {
            val merchant = it.groupValues[1].trim()
            val currency = it.groupValues[2]
            val amount = resolveAmount(currency, parseAmount(it.groupValues[3]))
            val code = it.groupValues[6]
            val dateStr = "${it.groupValues[7]} ${it.groupValues[8]}"
            val dateTime = parseDateTime(dateStr, smsTimestamp)
            return wrap(createTransactionModel(code, amount, BigDecimal.ZERO, balance, inferCategory(merchant, isIncome = false, rules = rules), dateTime, "Bill payment to $merchant", accountId, false))
        }

        // 5. Loan Approved
        loanApprovedRegex.find(message)?.let {
            val currency = it.groupValues[3]
            val amount = resolveAmount(currency, parseAmount(it.groupValues[4]))
            val code = it.groupValues[5]
            return wrap(createTransactionModel(code, amount, BigDecimal.ZERO, balance, "Loans", smsTimestamp ?: Clock.System.now(), "Loan Approved", accountId, true))
        }

        // 6. Deposited
        depositedRegex.find(message)?.let {
            val currency = it.groupValues[2]
            val amount = resolveAmount(currency, parseAmount(it.groupValues[3]))
            val recipient = it.groupValues[4].trim()
            val code = it.groupValues[5]
            val dateStr = "${it.groupValues[6]} ${it.groupValues[7]}"
            val dateTime = parseDateTime(dateStr, smsTimestamp)
            
            val isMySelf = recipient.contains("MARK", ignoreCase = true) || recipient.contains("NGOTHI", ignoreCase = true)
            val description = if (isMySelf) "Cash Deposit" else "Paid to $recipient"
            
            return wrap(createTransactionModel(code, amount, BigDecimal.ZERO, balance, if (isMySelf) "Other Income" else inferCategory(recipient, isIncome = false, rules = rules), dateTime, description, accountId, isMySelf))
        }

        // 6b. Received to Equity Account
        receivedEquityRegex.find(message)?.let {
            val currency = it.groupValues[1]
            val amount = resolveAmount(currency, parseAmount(it.groupValues[2]))
            val sender = it.groupValues[3].trim()
            val code = it.groupValues[6]
            val dateStr = "${it.groupValues[7]} ${it.groupValues[8]}"
            val dateTime = parseDateTime(dateStr, smsTimestamp)
            return wrap(createTransactionModel(code, amount, BigDecimal.ZERO, balance, "Other Income", dateTime, "Received from $sender", accountId, true))
        }

        // 7. Generic Equity Transfer (including credited to phone number)
        transferRegex.find(message)?.let {
            val currency = it.groupValues[1]
            val amount = resolveAmount(currency, parseAmount(it.groupValues[2]))
            val code = it.groupValues[4]
            val dateStr = "${it.groupValues[5]} ${it.groupValues[6]}"
            val dateTime = parseDateTime(dateStr, smsTimestamp)
            
            val isIncome = false 
            
            val description = if (message.contains("2547", ignoreCase = true) || message.contains("MPESA", ignoreCase = true)) {
                "Withdrawal to M-Pesa"
            } else {
                "Bank Transaction"
            }
            
            return wrap(createTransactionModel(code, amount, BigDecimal.ZERO, balance, "Transfer", dateTime, description, accountId, isIncome))
        }

        // 8. Successfully Sent
        successfullySentRegex.find(message)?.let {
            val amountRaw = parseAmount(it.groupValues[1])
            val currency = it.groupValues[2]
            val amount = resolveAmount(currency, amountRaw)
            val recipient = it.groupValues[3].trim()
            val code = it.groupValues[4]
            val dateStr = "${it.groupValues[5]} ${it.groupValues[6]}"
            val dateTime = parseDateTime(dateStr, smsTimestamp)
            return wrap(createTransactionModel(code, amount, BigDecimal.ZERO, balance, inferCategory(recipient, isIncome = false, rules = rules), dateTime, "Sent to $recipient", accountId, false))
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
        for (format in formats) {
            try {
                val date = format.parse(dateStr)
                if (date != null) return Instant.fromEpochMilliseconds(date.time)
            } catch (_: Exception) {}
        }
        return smsTimestamp ?: Clock.System.now()
    }

    private fun inferCategory(description: String, isIncome: Boolean = false, rules: List<CategoryRule> = emptyList()): String {
        val d = description.lowercase()
        
        // 1. Try dynamic rules from backend first
        rules.forEach { rule ->
            if (d.contains(rule.keyword.lowercase())) {
                return Category.fromId(rule.categoryId).name
            }
        }

        // 2. Fallback to hardcoded defaults
        return when {
            d.contains("netflix") || d.contains("google") || d.contains("youtube") || d.contains("spotify") || d.contains("openai") || d.contains("chatgpt") || d.contains("prime") -> "Subscriptions"
            d.contains("pharmacy") || d.contains("chemist") || d.contains("hospital") || d.contains("clinic") -> "Health"
            d.contains("kplc") || d.contains("token") || d.contains("power") || d.contains("water") -> "Utilities"
            d.contains("supermarket") || d.contains("groceries") || d.contains("naivas") || d.contains("carrefour") || d.contains("quickmart") || d.contains("chandarana") -> "Groceries"
            d.contains("restaurant") || d.contains("cafe") || d.contains("dining") || d.contains("bar") || d.contains("inn") || d.contains("dishes") || d.contains("pizza") || d.contains("kfc") || d.contains("java") -> "Dining Out"
            d.contains("zimele") || d.contains("etica") || d.contains("m-shwari") || d.contains("kcb m-pesa") || d.contains("money market") || d.contains("m-pesa saving") -> "Savings"
            d.contains("loan") || d.contains("kcb loan") || d.contains("m-shwari loan") -> "Loans"
            d.contains("uber") || d.contains("bolt") || d.contains("fuel") || d.contains("shell") || d.contains("rubis") || d.contains("total") -> "Transport"
            else -> if (isIncome) "Other Income" else "Transfer"
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
        isIncome: Boolean
    ): Transaction {
        val resolvedCategory = Category.fromName(category, !isIncome)
        
        return Transaction(
            accountId = accountId,
            isIncome = isIncome,
            amount = amount,
            transactionCost = cost,
            category = resolvedCategory.name,
            categoryId = resolvedCategory.id,
            dateTime = dateTime,
            description = "$description (Ref: $code)",
            externalId = code,
            balance = balance
        )
    }

    fun parseBalance(message: String): BigDecimal? {
        // Look for common bank balance patterns
        val balanceRegex = """(?:(?:account|available|new|your)\s+)?bal(?:ance)?\s*(?:is|was|:)?\s*$CURRENCY\.?\s*$AMOUNT_VAL""".toRegex(RegexOption.IGNORE_CASE)
        balanceRegex.find(message)?.let {
            return resolveAmount(it.groupValues[1], parseAmount(it.groupValues[2]))
        }
        
        // Alternative pattern: "Your account balance for 123***456 is KES 1,234.56"
        val altBalanceRegex = """balance\s+for\s+[\d*.]+\s+is\s+$CURRENCY\.?\s*$AMOUNT_VAL""".toRegex(RegexOption.IGNORE_CASE)
        altBalanceRegex.find(message)?.let {
            return resolveAmount(it.groupValues[1], parseAmount(it.groupValues[2]))
        }

        return null
    }
}
