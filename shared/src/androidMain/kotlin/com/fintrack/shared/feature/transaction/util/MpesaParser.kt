package com.fintrack.shared.feature.transaction.util

import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.model.CategoryRule
import com.fintrack.shared.feature.category.domain.model.fromId
import com.fintrack.shared.feature.category.domain.model.fromName
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.Instant
import java.text.SimpleDateFormat
import java.util.Locale

object MpesaParser {
    // Basic regex components
    private const val CODE = """([A-Z0-9]{10})"""
    private const val AMOUNT_VAL = """([\d,]+\.\d{1,2})"""
    private const val DATE = """(\d{1,2}/\d{1,2}/\d{2})"""
    private const val TIME = """(\d{1,2}:\d{2} [AP]M)"""
    
    // M-Pesa amount format: Optional space/dot after Ksh/KES, e.g., "Ksh1,000.00" or "KES. 1,000.00"
    private const val AMOUNT = """(?:[Kk][Ss][Hh]|[Kk][Ee][Ss])[.\s]*$AMOUNT_VAL"""

    private const val DATE_TIME = """[\s,]+(?:on\s+)?$DATE[\s,]+(?:at\s+)?$TIME"""
    private const val FOOTER = """(?:\.|\s+)(?:New M-PESA|Transaction cost|Amount you can transact|Your new M-PESA|Separate personal|Start Investing|on Lipa Na M-PESA)"""
    private const val PARTY_END = """(?:$DATE_TIME|$FOOTER|$)"""

    // 1. Agent Transactions with Date first
    private val agentDateFirstRegex = """(?:Congratulations!\s+)?$CODE\s+(?:Confirmed|confirmed)[.\s,]*on $DATE at $TIME\s?(Withdraw|Withdrawn|Receive|Received) $AMOUNT (?:from|to) (.+?)$PARTY_END""".toRegex(RegexOption.IGNORE_CASE)
    
    private val giveCashRegex = """(?:Congratulations!\s+)?$CODE\s+(?:Confirmed|confirmed)[.\s,]*on $DATE at $TIME\s?Give $AMOUNT cash to (.+?)$PARTY_END""".toRegex(RegexOption.IGNORE_CASE)

    private val sentRegex = """(?:Congratulations!\s+)?$CODE\s+(?:Confirmed|confirmed)[.\s,]+$AMOUNT sent to (.+?)$PARTY_END""".toRegex(RegexOption.IGNORE_CASE)
    
    private val receivedRegex = """(?:Congratulations!\s+)?$CODE\s+(?:Confirmed|confirmed)[.\s,]+(?:You have received |Received |)$AMOUNT (?:from |received from )(.+?)$PARTY_END""".toRegex(RegexOption.IGNORE_CASE)
    
    private val paidRegex = """(?:Congratulations!\s+)?$CODE\s+(?:Confirmed|confirmed)[.\s,]+$AMOUNT paid to (.+?)$PARTY_END""".toRegex(RegexOption.IGNORE_CASE)
    
    private val depositRegex = """(?:Congratulations!\s+)?$CODE\s+(?:Confirmed|confirmed)[.\s,]+$AMOUNT deposited to your M-PESA account by (.+?)$PARTY_END""".toRegex(RegexOption.IGNORE_CASE)
    private val sentToYouRegex = """(?:Congratulations!\s+)?$CODE\s+(?:Confirmed|confirmed)[.\s,]+$AMOUNT was sent to you by (.+?)$PARTY_END""".toRegex(RegexOption.IGNORE_CASE)
    private val withdrawRegex = """(?:Congratulations!\s+)?$CODE\s+(?:Confirmed|confirmed)[.\s,]+$AMOUNT withdrawn from (.+?)$PARTY_END""".toRegex(RegexOption.IGNORE_CASE)
    
    private val transferFromRegex = """(?:Congratulations!\s+)?$CODE\s+(?:Confirmed|confirmed)[.\s,]+(?:You have transfered |)$AMOUNT (?:transferred |)from (?:your )?(.+?)(?: account)?(?:\s+on\s+$DATE\s+at\s+$TIME)?$PARTY_END""".toRegex(RegexOption.IGNORE_CASE)
    
    private val transferToRegex = """(?:Congratulations!\s+)?$CODE\s+(?:Confirmed|confirmed)[.\s,]+$AMOUNT transferred to (.+?)(?: account)?(?:\s+on\s+$DATE\s+at\s+$TIME)?$PARTY_END""".toRegex(RegexOption.IGNORE_CASE)
    
    private val loanRepayRegex = """(?:Congratulations!\s+)?$CODE\s+(?:Confirmed|confirmed)[.\s,]+(?:Loan of |Your loan repayment of )$AMOUNT (?:repaid (?:from|to)|from your M-PESA account to) (?:your\s+)?(.+?)(?:\s+account)?.*?\s+on\s+$DATE\s+at\s+$TIME(?: is successful)?""".toRegex(RegexOption.IGNORE_CASE)
    private val loanApprovedRegex = """(?:Congratulations!\s+)?$CODE\s+(?:Confirmed|confirmed)[.\s,]+Your M-Shwari loan has been approved (?:on $DATE[\s,]+(?:at\s+)?$TIME\s+)?.*?and $AMOUNT (?:less .*? )?deposited to your M-PESA account""".toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    
    private val fulizaRepayRegex = """(?:Congratulations!\s+)?$CODE\s+(?:Confirmed|confirmed)[.\s,]+$AMOUNT (?:from|transferred from) your M-PESA has been used to (?:fully|partially) pay your outstanding Fuliza M-PESA(?:.*?$DATE[\s,]+(?:at\s+)?$TIME)?""".toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    
    private val airtimeRegex = """(?:Congratulations!\s+)?$CODE\s+(?:Confirmed|confirmed)[.\s,]+(?:You (?:bought|have bought|have received) |)$AMOUNT (?:of |)airtime(?: for [+\d]+)?\s+on\s+$DATE\s+at\s+$TIME""".toRegex(RegexOption.IGNORE_CASE)

    private val reversalRegex = """$CODE\s+(?:Confirmed|confirmed)[.\s,]+(?:Your transaction|Your original transaction|Reversal of transaction) (.+?) (?:in favour of .+? |)has been (?:successfully reversed|reversed successfully)[\s,]+(?:\s+on\s+$DATE\s+at\s+$TIME)?.*?$AMOUNT\s+(?:is|has been) (debited|credited) (?:from|to) your M-PESA account""".toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

    // 8. Received at Till (Income for Merchant)
    private val receivedAtTillRegex = """Confirmed\.?\s+Payment of\s+(?:KES|Ksh|KSH)\.?\s*$AMOUNT_VAL\s+to\s+(.*?)\s+Till No\.\s+\d+\s+has been received\.\s+Ref\.\s*$CODE\s+on\s+$DATE\s+at\s+$TIME""".toRegex(RegexOption.IGNORE_CASE)

    // 9. Sent from Bank to M-Pesa (Income)
    private val bankToMpesaRegex = """(.*?)\s+has sent\s+(?:KShs|KES|Ksh|KSH)\.?\s*$AMOUNT_VAL\s+to your MPESA\.\s+The MPESA receipt number is\s+$CODE\s+and transaction reference is\s+(\w+)""".toRegex(RegexOption.IGNORE_CASE)

    // Auxiliary data regexes
    private val costRegex = """Transaction cost[\s,]+[Kk][Ss][Hh][.\s]*$AMOUNT_VAL""".toRegex(RegexOption.IGNORE_CASE)

    private val dateFormat = SimpleDateFormat("d/M/yy h:mm a", Locale.ENGLISH)

    fun parse(
        message: String, 
        accountId: String = "mpesa", 
        smsTimestamp: Instant? = null,
        rules: List<CategoryRule> = emptyList()
    ): Transaction? {
        if (!message.contains("Confirmed", ignoreCase = true)) return null

        // Ignore messages that are just balance notifications or meta-info
        if (message.contains("Fuliza M-Pesa amount is", ignoreCase = true)) return null
        if (message.contains("loan request will be processed shortly", ignoreCase = true)) return null
        if (message.contains("Your loan limit is", ignoreCase = true)) return null
        if (message.contains("Deposit Account balance is", ignoreCase = true)) return null
        if (message.contains("KCB M-PESA balance is", ignoreCase = true)) return null
        if (message.contains("account balance was", ignoreCase = true) && !message.contains("reversed", ignoreCase = true)) return null

        val cost = parseAmountFromMatch(costRegex.find(message))
        val balance = parseBalance(message)

        // Helper to validate and create model
        fun wrap(transaction: Transaction?): Transaction? {
            if (transaction == null) return null
            if (transaction.amount <= BigDecimal.ZERO) {
                return null
            }
            return transaction
        }

        // 1. Fuliza (Check this FIRST because it often uses "transferred from M-PESA")
        fulizaRepayRegex.find(message)?.let {
            val code = it.groupValues[1]
            val amount = parseAmount(it.groupValues[2])
            val date = it.groupValues.getOrNull(3) ?: ""
            val time = it.groupValues.getOrNull(4) ?: ""
            return wrap(createTransactionModel(code, amount, cost, balance, "Loans", parseDateTime(date, time, smsTimestamp), "Fuliza M-PESA Repayment", accountId, isIncome = false))
        }

        // 2. Loans (Repayments should be checked before general transfers)
        loanRepayRegex.find(message)?.let { 
            val code = it.groupValues[1]
            val amount = parseAmount(it.groupValues[2])
            val party = cleanPartyName(it.groupValues[3])
            val date = it.groupValues.getOrNull(4) ?: ""
            val time = it.groupValues.getOrNull(5) ?: ""
            return wrap(createTransactionModel(code, amount, cost, balance, "Loans", parseDateTime(date, time, smsTimestamp), "Loan Repayment to $party", accountId, isIncome = false))
        }

        loanApprovedRegex.find(message)?.let { 
            val code = it.groupValues[1]
            val date = it.groupValues.getOrNull(2) ?: ""
            val time = it.groupValues.getOrNull(3) ?: ""
            val amount = parseAmount(it.groupValues[4])
            return wrap(createTransactionModel(code, amount, cost, balance, "Loans", parseDateTime(date, time, smsTimestamp), "Loan Approved", accountId, isIncome = true))
        }

        // 3. Try Agent Transactions (Date First)
        agentDateFirstRegex.find(message)?.let {
            val code = it.groupValues[1]
            val date = it.groupValues[2]
            val time = it.groupValues[3]
            val action = it.groupValues[4].lowercase()
            val amount = parseAmount(it.groupValues[5])
            val party = cleanPartyName(it.groupValues[6])
            
            val isIncome = action.contains("receive")
            val prefix = if (isIncome) "Deposit from" else "Withdrawn from"
            val category = if (isIncome) "Other Income" else "Transport"
            
            return wrap(createTransactionModel(code, amount, cost, balance, category, parseDateTime(date, time, smsTimestamp), "$prefix $party", accountId, isIncome))
        }

        // Try "Give cash to" (Older withdrawal style)
        giveCashRegex.find(message)?.let {
            val code = it.groupValues[1]
            val date = it.groupValues[2]
            val time = it.groupValues[3]
            val amount = parseAmount(it.groupValues[4])
            val party = cleanPartyName(it.groupValues[5])
            return wrap(createTransactionModel(code, amount, cost, balance, "Transport", parseDateTime(date, time, smsTimestamp), "Withdrawn from $party", accountId, isIncome = false))
        }

        // M-Shwari / Bank / KCB Transfers
        transferFromRegex.find(message)?.let { 
            val code = it.groupValues[1]
            val amount = parseAmount(it.groupValues[2])
            val party = cleanPartyName(it.groupValues[3])
            val date = it.groupValues.getOrNull(4) ?: ""
            val time = it.groupValues.getOrNull(5) ?: ""
            
            return wrap(createTransactionModel(
                code, 
                amount, 
                cost, 
                balance, 
                inferCategory(party, isIncome = true, rules = rules),
                parseDateTime(date, time, smsTimestamp), 
                "Transferred from $party", 
                accountId,
                isIncome = true
            ))
        }
        transferToRegex.find(message)?.let { 
            val code = it.groupValues[1]
            val amount = parseAmount(it.groupValues[2])
            val party = cleanPartyName(it.groupValues[3])
            val date = it.groupValues.getOrNull(4) ?: ""
            val time = it.groupValues.getOrNull(5) ?: ""
            
            return wrap(createTransactionModel(
                code, 
                amount, 
                cost, 
                balance, 
                inferCategory(party, isIncome = false, rules = rules),
                parseDateTime(date, time, smsTimestamp), 
                "Transferred to $party", 
                accountId,
                isIncome = false
            ))
        }
        
        // Airtime
        airtimeRegex.find(message)?.let {
            val code = it.groupValues[1]
            val amount = parseAmount(it.groupValues[2])
            val date = it.groupValues[3]
            val time = it.groupValues[4]
            return wrap(createTransactionModel(code, amount, cost, balance, "Airtime", parseDateTime(date, time, smsTimestamp), "Bought airtime", accountId, isIncome = false))
        }

        // Reversals
        reversalRegex.find(message)?.let {
            val code = it.groupValues[1]
            val originalCode = it.groupValues[2]
            val date = it.groupValues[3]
            val time = it.groupValues[4]
            val amount = parseAmount(it.groupValues[5])
            val type = it.groupValues[6].lowercase()
            val isIncome = type == "credited"
            
            return wrap(createTransactionModel(code, amount, cost, balance, "Transfer", parseDateTime(date, time, smsTimestamp), "Reversal of $originalCode ($type)", accountId, isIncome))
        }

        // Standard transactions
        sentRegex.find(message)?.let { return wrap(createFromMatch(it, isIncome = false, "Sent to", null, cost, balance, accountId, smsTimestamp, rules)) }
        
        receivedRegex.find(message)?.let { return wrap(createFromMatch(it, isIncome = true, "Received from", null, cost, balance, accountId, smsTimestamp, rules)) }
        sentToYouRegex.find(message)?.let { return wrap(createFromMatch(it, isIncome = true, "Sent by", null, cost, balance, accountId, smsTimestamp, rules)) }
        
        paidRegex.find(message)?.let { return wrap(createFromMatch(it, isIncome = false, "Paid to", null, cost, balance, accountId, smsTimestamp, rules)) }

        depositRegex.find(message)?.let { return wrap(createFromMatch(it, isIncome = true, "Deposit from", null, cost, balance, accountId, smsTimestamp, rules)) }
        withdrawRegex.find(message)?.let { return wrap(createFromMatch(it, isIncome = false, "Withdrawn from", null, cost, balance, accountId, smsTimestamp, rules)) }

        // Received at Till (Ref at end)
        receivedAtTillRegex.find(message)?.let {
            val amount = parseAmount(it.groupValues[1])
            val recipient = it.groupValues[2].trim()
            val code = it.groupValues[3]
            val date = it.groupValues[4]
            val time = it.groupValues[5]
            return wrap(createTransactionModel(code, amount, cost, balance, "Other Income", parseDateTime(date, time, smsTimestamp), "Payment received at Till ($recipient)", accountId, isIncome = true))
        }

        // Bank to M-Pesa
        bankToMpesaRegex.find(message)?.let {
            val sender = it.groupValues[1].trim()
            val amount = parseAmount(it.groupValues[2])
            val code = it.groupValues[3]
            val bankRef = it.groupValues[4]
            return wrap(createTransactionModel(code, amount, cost, balance, "Other Income", smsTimestamp ?: Clock.System.now(), "Received from $sender (Bank Ref: $bankRef)", accountId, isIncome = true))
        }

        return null
    }

    private fun createFromMatch(
        match: MatchResult, 
        isIncome: Boolean, 
        prefix: String, 
        fixedCategory: String?,
        cost: BigDecimal,
        balance: BigDecimal?,
        accountId: String,
        smsTimestamp: Instant? = null,
        rules: List<CategoryRule> = emptyList()
    ): Transaction {
        val code = match.groupValues[1]
        val amount = parseAmount(match.groupValues[2])
        val rawParty = if (match.groupValues.size > 3) match.groupValues[3] else "M-PESA"
        val party = cleanPartyName(rawParty)
        val date = if (match.groupValues.size > 4) match.groupValues[4] else ""
        val time = if (match.groupValues.size > 5) match.groupValues[5] else ""
        
        return createTransactionModel(code, amount, cost, balance, fixedCategory ?: inferCategory(party, rules = rules), parseDateTime(date, time, smsTimestamp), "$prefix $party", accountId, isIncome)
    }

    private fun cleanPartyName(name: String): String {
        return name.split(Regex("(?i)New M-PESA|Transaction cost|Amount you can transact|for account|on Lipa Na M-PESA|VIA\\s+[A-Z]+|Express"))[0]
            .trim()
            .removeSuffix(".")
            .trim()
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
        // 1. Look for explicit M-PESA balance (highest priority)
        // Format examples: 
        // "M-PESA balance is Ksh1,234.56"
        // "New M-PESA balance is Ksh1,234.56"
        // "Your M-PESA balance was Ksh1,234.56 on..."
        val mpesaRegex = """(?:New\s+)?M-?PESA\s+(?:account\s+)?balance\s+(?:is|was)\s+(?:Ksh\.?\s*|KSH\s*|KES\s*)?([\d,]+\.\d{1,2})""".toRegex(RegexOption.IGNORE_CASE)
        mpesaRegex.find(message)?.let {
            return parseAmount(it.groupValues[1])
        }

        // 1b. Summary format: M-PESA Account : Ksh72,253.08
        val summaryRegex = """M-?PESA\s+Account\s*:\s*(?:Ksh\.?\s*|KSH\s*|KES\s*)?([\d,]+\.\d{1,2})""".toRegex(RegexOption.IGNORE_CASE)
        summaryRegex.find(message)?.let {
            return parseAmount(it.groupValues[1])
        }

        // 2. Look for "balance is/was" but ONLY if it's not an M-Shwari balance
        val genericRegex = """balance\s+(?:is|was)\s+(?:Ksh\.?\s*|KSH\s*|KES\s*)?([\d,]+\.\d{1,2})""".toRegex(RegexOption.IGNORE_CASE)
        val allMatches = genericRegex.findAll(message)
        
        for (match in allMatches) {
            val startIdx = match.range.first
            // Look back to see if this balance is qualified by M-Shwari
            val lookbackStart = maxOf(0, startIdx - 50)
            val context = message.substring(lookbackStart, startIdx).lowercase()
            
            // If the balance is explicitly labeled as M-Shwari, skip it.
            if (context.contains("m-shwari") || context.contains("mshwari")) {
                continue
            }
            
            // If we found a generic balance and it's not M-Shwari, it's likely M-PESA
            return parseAmount(match.groupValues[1])
        }

        return null
    }

    private fun parseAmountFromMatch(match: MatchResult?): BigDecimal {
        return match?.groupValues?.get(1)?.let { parseAmount(it) } ?: BigDecimal.ZERO
    }

    private fun parseAmount(value: String?): BigDecimal {
        return try {
            value?.replace(",", "")?.let { BigDecimal.parseString(it) } ?: BigDecimal.ZERO
        } catch (_: Exception) {
            BigDecimal.ZERO
        }
    }

    private fun parseDateTime(date: String, time: String, smsTimestamp: Instant? = null): Instant {
        if (date.isBlank() || time.isBlank()) return smsTimestamp ?: Clock.System.now()
        return try {
            val dateStr = "$date $time"
            val parsedDate = dateFormat.parse(dateStr)
            if (parsedDate != null) {
                val parsedInstant = Instant.fromEpochMilliseconds(parsedDate.time)
                
                // Use arrival time seconds/millis if within the same minute as the text
                if (smsTimestamp != null) {
                    val diff = abs(parsedInstant.toEpochMilliseconds() - smsTimestamp.toEpochMilliseconds())
                    if (diff < 60000) smsTimestamp else parsedInstant
                } else {
                    parsedInstant
                }
            } else {
                smsTimestamp ?: Clock.System.now()
            }
        } catch (_: Exception) {
            smsTimestamp ?: Clock.System.now()
        }
    }

    private fun inferCategory(recipient: String, isIncome: Boolean = false, rules: List<CategoryRule> = emptyList()): String {
        val r = recipient.lowercase(Locale.ENGLISH)

        // 1. Try dynamic rules from backend first
        rules.forEach { rule ->
            if (r.contains(rule.keyword.lowercase())) {
                // Find matching category name from our global list to ensure correct display name
                return Category.fromId(rule.categoryId).name
            }
        }

        // 2. Fallback to hardcoded defaults (as safety net)
        return when {
            r.contains("kplc") || r.contains("tokens") || r.contains("power") || r.contains("jajemelo") ||
            r.contains("water") || r.contains("sewerage") || r.contains("ncwsc") || r.contains("kiwasco") ||
            r.contains("mawasco") || r.contains("nyewasco") || r.contains("eldowas") || r.contains("mowasco") ||
            r.contains("gas") || r.contains("m-gas") || r.contains("mgas") || r.contains("afrigas") ||
            r.contains("garbage") || r.contains("waste") || r.contains("trash") ||
            r.contains("m-kopa") || r.contains("mkopa") || r.contains("d.light") || r.contains("sunking") || r.contains("bboxx") -> "Utilities"
            r.contains("zuku") || r.contains("safaricom home") || r.contains("poa internet") || r.contains("vilcom") || 
            r.contains("faiba") || r.contains("jtl") || r.contains("jtlk") || r.contains("wananchi") ||
            r.contains("mawingu") || r.contains("starlink") || r.contains("konnect") || r.contains("fibre connect") || 
            r.contains("fiber connect") || r.contains("airtel fibre") || r.contains("telkom home") ||
            r.contains("liquid home") || r.contains("liquid telecom") ||
            r.contains("data bundles") || r.contains("data bundle") || r.contains("offers") || r.contains("tunukiwa") ||
            r.contains("internet") || r.contains("bundles") -> "Internet"
            r.contains("airtime") || r.contains("tingg") || r.contains("top up") -> "Airtime"
            r.contains("supermarket") || r.contains("naivas") || r.contains("carrefour") || r.contains("quickmart") || r.contains("butchery") || r.contains("quick mart") || r.contains("friendly 5") || r.contains("slice city") || r.contains("memento butchery") -> "Groceries"
            r.contains("restaurant") || r.contains("cafe") || r.contains("kfc") || r.contains("java") || Regex("""\bbar\b""").containsMatchIn(r) || r.contains("lounge") || r.contains("chicken inn") || r.contains("pizza inn") || r.contains("creamy inn") || r.contains("choma place") || r.contains("nas n001") || r.contains("caterers") || r.contains("dishes") -> "Dining Out"
            r.contains("equity") || r.contains("co-operative") || r.contains("bank") || r.contains("i&m") || r.contains("ncba") || r.contains("boa") || r.contains("family bank") || r.contains("stanbic") || r.contains("loop") || r.contains("sidian") -> "Bank"
            r.contains("loan repayment") || r.contains("loan") || r.contains("fuliza") || r.contains("tala") || r.contains("branch") -> "Loans"
            r.contains("m-shwari saving") || r.contains("mshwari saving") || r.contains("m-shwari") || r.contains("mshwari") || r.contains("kcb") || r.contains("sacco") || r.contains("chama") || r.contains("orokise") ||
            r.contains("zimele") || r.contains("etica") || r.contains("gulfcap") || r.contains("cytonn") || r.contains("arvocap") || 
            r.contains("lofty") || r.contains("kuza") || r.contains("mali") || r.contains("ziidi") || r.contains("kasha") || 
            r.contains("genghis") || r.contains("hela imara") || r.contains("nabo capital") || 
            r.contains("stima sacco") || r.contains("police sacco") || r.contains("unaitas") || r.contains("mwalimu") ||
            r.contains("harambee") || r.contains("kimisitu") || r.contains("hazina sacco") || r.contains("imarisha") ||
            r.contains("tower sacco") || r.contains("waumini") ||
            r.contains("dry associates") || r.contains("m-pesa saving") || r.contains("money market") || r.contains("fund") || r.contains("asset") || r.contains("mmf") -> "Savings"
            r.contains("tithe") || r.contains("offering") || r.contains("citam") || r.contains("church") || r.contains("charity") || r.contains("mosque") || r.contains("prayer mountain") -> "Charity"
            r.contains("parking") || r.contains("kaps") || r.contains("bolt") || r.contains("uber") || r.contains("taxi") || r.contains("rubis") || r.contains("totalenergies") || r.contains("shell") -> "Transport"
            r.contains("chemist") || r.contains("pharmacy") || r.contains("hospital") || r.contains("health") || r.contains("clinic") || r.contains("meds") || r.contains("dental") || r.contains("hopemed") || r.contains("medical") -> "Health"
            r.contains("netflix") || r.contains("spotify") || r.contains("showmax") || r.contains("youtube") -> "Subscriptions"
            r.contains("jumia") || r.contains("leather") || r.contains("watches") || r.contains("perfume") || r.contains("clothes") || r.contains("fashion") || r.contains("mrp") || r.contains("miniso") || r.contains("woolworths") || r.contains("tushop") || r.contains("m-pesa card") || r.contains("canva") || r.contains("pdfaid") -> "Shopping"
            r.contains("salon") || r.contains("barber") || r.contains("beauty") || r.contains("nail bar") -> "Personal Care"
            r.contains("hardware") || r.contains("timber") || r.contains("maintenance") || r.contains("repair") -> "Maintenance"
            r.contains("e-citizen") || r.contains("kra") || r.contains("county") -> "Government"
            r.contains("britam") || r.contains("nhif") || r.contains("shif") || r.contains("insurance") || r.contains("apa") || 
            r.contains("jubilee") || r.contains("sanlam") || r.contains("cic") || r.contains("old mutual") || 
            r.contains("icea lion") || r.contains("madison") || r.contains("apollo") || r.contains("ga insurance") ||
            r.contains("heritage") || r.contains("geminia") || r.contains("pioneer") || r.contains("kenindia") || r.contains("uap") -> "Insurance"
            r.contains("salary") -> "Salary"
            r.contains("bonus") -> "Bonus"
            r.contains("interest") -> "Interest"
            r.contains("commission") || r.contains("income") -> "Other Income"
            else -> if (isIncome) "Other Income" else "Transfer"
        }
    }
}
