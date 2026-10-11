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
        val transactions = mutableListOf<Transaction>()
        var latestBalance: BigDecimal? = null

        if (!isPortfolioSeed) {
            val permissionStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)

            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Permission READ_SMS is not granted!")
                throw Exception("Permission denied: READ_SMS is required for M-Pesa sync")
            }

            val cursor = try {
                context.contentResolver.query(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    arrayOf(Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.DATE),
                    "${Telephony.Sms.Inbox.ADDRESS} LIKE ? OR ${Telephony.Sms.Inbox.ADDRESS} LIKE ?",
                    arrayOf("%MPESA%", "%M-PESA%"),
                    "${Telephony.Sms.Inbox.DATE} DESC"
                )
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException querying SMS inbox", e)
                throw Exception("Permission denied: READ_SMS is required for M-Pesa sync", e)
            }

            val totalMessages = cursor?.count ?: 0
            Log.d(TAG, "M-Pesa SMS query returned $totalMessages messages.")

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

                    val geminiParser = GeminiSmsParser(context)
                    val parsed = MpesaParser.parse(body, accountId, smsInstant, rules, categories)
                        ?: geminiParser.parseSmsWithGemini(body, accountId, smsInstant, rules, categories)
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

                        transactions.add(parsed.copy(categoryId = finalCategoryId))
                    }

                    loggedCount++
                    if (loggedCount % 500 == 0 && totalMessages > 0) onProgress(0.05f + (loggedCount.toFloat() / totalMessages) * 0.25f)
                }
            }
            Log.d(TAG, "M-Pesa SMS parsing complete: ${transactions.size} valid transactions parsed from $totalMessages SMS messages.")
        } else {
            val dummyTransactions = portfolioSeeder.generateDummyTransactions(accountId, categories)
            transactions.addAll(dummyTransactions)
        }

        if (transactions.isNotEmpty()) {
            onProgress(0.3f)
            Log.d(TAG, "Inserting ${transactions.size} parsed M-Pesa transactions into local database for account $accountId...")
            val chunks = transactions.chunked(250)
            chunks.forEachIndexed { index, chunk ->
                currentCoroutineContext().job.ensureActive()
                val result = transactionRepository.importMpesaTransactions(chunk)
                if (result is Result.Error) {
                    Log.e(TAG, "Error saving M-Pesa chunk $index to database", result.exception)
                    throw result.exception
                }
                onProgress(0.3f + ((index + 1).toFloat() / chunks.size) * 0.6f)
            }
            Log.d(TAG, "M-Pesa database insertion finished successfully!")
        }

        val accountResult = accountRepository.getAccountById(accountId)
        if (accountResult is Result.Success) {
            val account = accountResult.data
            val newBalance = when {
                isPortfolioSeed -> BigDecimal.fromInt(72450)
                transactions.isNotEmpty() -> latestBalance ?: account.balance
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
}
