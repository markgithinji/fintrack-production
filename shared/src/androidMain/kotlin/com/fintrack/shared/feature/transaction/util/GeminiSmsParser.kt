package com.fintrack.shared.feature.transaction.util

import android.content.Context
import android.util.Log
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.model.CategoryRule
import com.fintrack.shared.feature.category.util.CategoryMatcher
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.google.mlkit.genai.prompt.Generation
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.Instant
import kotlin.time.Clock

class GeminiSmsParser(private val context: Context) {

    companion object {
        private const val TAG = "SMS_SYNC"
    }

    private val generativeModel = Generation.getClient()

    suspend fun parseSmsWithGemini(
        rawSms: String,
        accountId: String,
        smsTimestamp: Instant?,
        rules: List<CategoryRule> = emptyList(),
        allCategories: List<Category> = emptyList()
    ): Transaction? {
        return try {
            Log.d(TAG, "[GeminiSmsParser] Fallback triggered! Passing non-standard SMS to Gemini Nano...")

            val prompt = """
                Extract transaction details from this financial SMS text:
                "$rawSms"
                
                Respond in JSON with keys:
                - referenceCode (string, e.g. QFA12345)
                - amount (number/string, e.g. 350.00)
                - merchant (string, e.g. Java House)
                - isIncome (boolean)
            """.trimIndent()

            val response = generativeModel.generateContent(prompt)
            val text = response.candidates.firstOrNull()?.text ?: return null

            val parsedAmount = extractAmountFromText(text)
            if (parsedAmount <= BigDecimal.ZERO) return null

            val merchantName = extractMerchantFromText(text)
            val isIncome = text.contains("\"isIncome\": true", ignoreCase = true) || text.contains("\"isIncome\":true", ignoreCase = true)
            val description = if (isIncome) "Received from $merchantName" else "Paid to $merchantName"
            val refCode = extractRefCodeFromText(text)

            val resolvedCategoryId = CategoryMatcher.resolveCategory(
                inputCategoryId = null,
                inputCategoryName = null,
                description = "$merchantName $rawSms",
                isIncome = isIncome,
                allCategories = allCategories,
                rules = rules,
                defaultId = "pending"
            )

            val categoryName = allCategories.find { it.id == resolvedCategoryId }?.name
                ?: if (isIncome) "Other Income" else "Transfer"

            val transaction = Transaction(
                accountId = accountId,
                isIncome = isIncome,
                amount = parsedAmount,
                transactionCost = BigDecimal.ZERO,
                category = categoryName,
                categoryId = resolvedCategoryId,
                dateTime = smsTimestamp ?: Clock.System.now(),
                description = "$description (Ref: $refCode)",
                externalId = refCode,
                balance = null,
                rawMessage = rawSms
            )

            Log.d(TAG, "[GeminiSmsParser] Gemini Nano successfully parsed transaction: ref=$refCode, amount=$parsedAmount, merchant=$merchantName, category=$categoryName")
            transaction
        } catch (e: Exception) {
            Log.d(TAG, "[GeminiSmsParser] Gemini Nano on-device parsing unavailable or exception: ${e.message}")
            null
        }
    }

    private fun extractAmountFromText(text: String): BigDecimal {
        return try {
            val amountMatch = """"amount"\s*:\s*"?([\d.,]+)"?""".toRegex().find(text)
            val str = amountMatch?.groupValues?.get(1)?.replace(",", "") ?: "0"
            BigDecimal.parseString(str)
        } catch (_: Exception) {
            BigDecimal.ZERO
        }
    }

    private fun extractMerchantFromText(text: String): String {
        val merchantMatch = """"merchant"\s*:\s*"([^"]+)"""".toRegex().find(text)
        return merchantMatch?.groupValues?.get(1) ?: "Unknown Merchant"
    }

    private fun extractRefCodeFromText(text: String): String {
        val refMatch = """"referenceCode"\s*:\s*"([^"]+)"""".toRegex().find(text)
        return refMatch?.groupValues?.get(1) ?: "GEMINI_${Clock.System.now().toEpochMilliseconds()}"
    }

    fun close() {
        try {
            generativeModel.close()
        } catch (_: Exception) {}
    }
}
