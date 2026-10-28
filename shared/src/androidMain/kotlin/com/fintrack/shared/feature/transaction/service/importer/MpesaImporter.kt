package com.fintrack.shared.feature.transaction.service.importer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import com.fintrack.shared.feature.account.domain.repository.AccountRepository
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.model.CategoryRule
import com.fintrack.shared.feature.category.domain.model.UncategorizedExpense
import com.fintrack.shared.feature.category.domain.model.UncategorizedIncome
import com.fintrack.shared.feature.category.domain.repository.CategoryRepository
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.fintrack.shared.feature.transaction.util.GeminiSmsParser
import com.fintrack.shared.feature.transaction.domain.repository.TransactionRepository
import com.fintrack.shared.feature.transaction.domain.service.TransactionImporter
import com.fintrack.shared.feature.transaction.util.MpesaParser
import java.io.File
import com.fintrack.shared.feature.transaction.util.PortfolioSeeder
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Instant

class MpesaImporter(
    private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : TransactionImporter {
    private val portfolioSeeder = PortfolioSeeder()

    companion object {
        private const val TAG = "SMS_SYNC"
    }

    override suspend fun importHistory(
        targetAccountId: String?,
        isPortfolioSeed: Boolean,
        onProgress: (Float) -> Unit
    ): Unit = withContext(Dispatchers.IO) {
        onProgress(0.05f)
        
        val categoriesResult = categoryRepository.getCategories()
        val categories = (categoriesResult as? Result.Success)?.data ?: emptyList()
        
        val rulesResult = categoryRepository.getCategoryRules()
        val rules = (rulesResult as? Result.Success)?.data ?: emptyList()
        
        val accountsResult = accountRepository.getAccounts()
        val accounts = (accountsResult as? Result.Success)?.data ?: emptyList()
        
        val accountId = targetAccountId ?: accounts.find { it.linkedSources.contains("mpesa") }?.id ?: accounts.firstOrNull()?.id

        if (accountId == null) {
            if (isPortfolioSeed) {
                 val fallbackId = accounts.firstOrNull()?.id ?: "mpesa"
                 processImport(fallbackId, categories, rules, true, onProgress)
            } else {
                Log.e(TAG, "No account is available for M-Pesa sync.")
                throw Exception("No account is configured for M-Pesa sync.")
            }
            return@withContext
        }

        processImport(accountId, categories, rules, isPortfolioSeed, onProgress)
    }

    private suspend fun processImport(
        accountId: String,
        categories: List<Category>,
        rules: List<CategoryRule>,
        isPortfolioSeed: Boolean,
        onProgress: (Float) -> Unit
    ) {
        val batch = mutableListOf<Transaction>()
        var totalParsedCount = 0
        var latestBalance: BigDecimal? = null

        if (!isPortfolioSeed) {
            val permissionStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)

            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Permission READ_SMS is not granted!")
                throw Exception("Permission denied: READ_SMS is required for M-Pesa sync")
            }

            val dateOneYearAgo = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000L)
            val cursor = try {
                context.contentResolver.query(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    arrayOf(Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.DATE),
                    "(${Telephony.Sms.Inbox.ADDRESS} LIKE ? OR ${Telephony.Sms.Inbox.ADDRESS} LIKE ?) AND ${Telephony.Sms.Inbox.DATE} >= ?",
                    arrayOf("%MPESA%", "%M-PESA%", dateOneYearAgo.toString()),
                    "${Telephony.Sms.Inbox.DATE} DESC"
                )
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException querying SMS inbox", e)
                throw Exception("Permission denied: READ_SMS is required for M-Pesa sync", e)
            }

            val totalMessages = cursor?.count ?: 0
            Log.d(TAG, "M-Pesa SMS query returned $totalMessages messages.")

            try {
                cursor?.use { smsCursor ->
                    val bodyIndex = smsCursor.getColumnIndex(Telephony.Sms.Inbox.BODY)
                    val dateIndex = smsCursor.getColumnIndex(Telephony.Sms.Inbox.DATE)
                    var loggedCount = 0

                    while (smsCursor.moveToNext()) {
                        currentCoroutineContext().job.ensureActive()
                        val body = smsCursor.getString(bodyIndex) ?: continue
                        val timestamp = smsCursor.getLong(dateIndex)
                        val smsInstant = Instant.fromEpochMilliseconds(timestamp)

                        if (latestBalance == null) {
                            latestBalance = MpesaParser.parseBalance(body)
                        }

                        val parsed = MpesaParser.parse(body, accountId, smsInstant, rules, categories)

                        if (parsed != null) {
                            val categoryName = parsed.category
                            val isExpense = !parsed.isIncome
                            val finalCategoryId = categories.find { cat ->
                                cat.name.equals(categoryName, ignoreCase = true) && cat.isExpense == isExpense
                            }?.id
                                ?: categories.find { cat ->
                                    cat.name.contains("Other", ignoreCase = true) && cat.isExpense == isExpense
                                }?.id
                                ?: categories.find { cat -> cat.isExpense == isExpense }?.id
                                ?: categories.firstOrNull()?.id
                                ?: if (isExpense) Category.UncategorizedExpense.id else Category.UncategorizedIncome.id

                            batch.add(parsed.copy(categoryId = finalCategoryId))

                            if (batch.size >= 250) {
                                val result = transactionRepository.importMpesaTransactions(batch)
                                if (result is Result.Error) throw result.exception
                                totalParsedCount += batch.size
                                batch.clear()
                            }
                        }

                        loggedCount++
                        if (loggedCount % 200 == 0 && totalMessages > 0) {
                            onProgress(0.05f + (loggedCount.toFloat() / totalMessages) * 0.85f)
                        }
                    }

                    if (batch.isNotEmpty()) {
                        val result = transactionRepository.importMpesaTransactions(batch)
                        if (result is Result.Error) throw result.exception
                        totalParsedCount += batch.size
                        batch.clear()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during SMS parsing and importing", e)
                throw e
            }
            Log.d(TAG, "M-Pesa SMS parsing complete: $totalParsedCount valid transactions imported from $totalMessages SMS messages.")
        } else {
            val dummyTransactions = portfolioSeeder.generateDummyTransactions(accountId, categories)
            transactionRepository.importMpesaTransactions(dummyTransactions)
            totalParsedCount = dummyTransactions.size
        }

        val accountResult = accountRepository.getAccountById(accountId)
        if (accountResult is Result.Success) {
            val account = accountResult.data
            val newBalance = when {
                isPortfolioSeed -> BigDecimal.fromInt(72450)
                totalParsedCount > 0 -> latestBalance ?: account.balance
                else -> account.balance
            }

            val updateResult = accountRepository.addOrUpdateAccount(
                account.copy(
                    balance = newBalance, 
                    lastSyncedAt = Clock.System.now()
                )
            )
            if (updateResult is Result.Error) throw updateResult.exception
        }
        onProgress(1.0f)
    }

    private fun isFinancialCandidate(text: String): Boolean {
        val lower = text.lowercase()
        val hasCurrency = lower.contains("ksh") || lower.contains("kes") || lower.contains("usd") || lower.contains("amount")
        val hasAction = lower.contains("sent") || lower.contains("paid") || lower.contains("received") || 
                        lower.contains("transferred") || lower.contains("bought") || lower.contains("withdraw") || 
                        lower.contains("deposited") || lower.contains("confirmed") || lower.contains("ref")
        return hasCurrency && hasAction
    }

    private fun logMpesaSmsComparison(rawSms: String, parsedTx: Transaction?) {
        try {
            val logEntry = buildString {
                appendLine("==================================================")
                appendLine("[RAW_MPESA_SMS]: $rawSms")
                if (parsedTx != null) {
                    appendLine("[PARSED_RESULT]: KEPT (SUCCESS)")
                    appendLine(" - Ref: ${parsedTx.externalId}")
                    appendLine(" - Amount: ${parsedTx.amount}")
                    appendLine(" - Cost: ${parsedTx.transactionCost}")
                    appendLine(" - Category: ${parsedTx.category}")
                    appendLine(" - Description: ${parsedTx.description}")
                    appendLine(" - IsIncome: ${parsedTx.isIncome}")
                    appendLine(" - Balance: ${parsedTx.balance}")
                } else {
                    appendLine("[PARSED_RESULT]: IGNORED (Regex Returned NULL)")
                }
                appendLine("==================================================")
                appendLine()
            }

            val cacheFile = File(context.cacheDir, "mpesa_sms_parsing_log.txt")
            cacheFile.appendText(logEntry)

            val externalDir = context.getExternalFilesDir(null)
            if (externalDir != null) {
                val externalFile = File(externalDir, "mpesa_sms_parsing_log.txt")
                externalFile.appendText(logEntry)
            }
        } catch (e: Exception) {
            Log.e("MPESA_LOG", "Error writing M-Pesa SMS comparison log", e)
        }
    }
}
