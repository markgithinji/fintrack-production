package com.fintrack.shared.feature.transaction.service.importer

import android.content.Context
import android.provider.Telephony
import com.fintrack.shared.feature.account.domain.repository.AccountRepository
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.model.CategoryRule
import com.fintrack.shared.feature.category.domain.repository.CategoryRepository
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.transaction.domain.repository.TransactionRepository
import com.fintrack.shared.feature.transaction.domain.service.TransactionImporter
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.fintrack.shared.feature.transaction.util.EquityParser
import com.fintrack.shared.feature.transaction.util.PortfolioSeeder
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.currentCoroutineContext

class EquityImporter(
    private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : TransactionImporter {
    private val portfolioSeeder = PortfolioSeeder()

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
        val accountId = targetAccountId ?: accounts.find { it.linkedSources.contains("equity") }?.id

        if (accountId == null) {
            if (isPortfolioSeed) {
                 val fallbackId = accounts.firstOrNull()?.id ?: "equity"
                 processImport(fallbackId, categories, rules, true, onProgress)
            } else {
                throw Exception("No account is configured for Equity sync.")
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
                throw Exception("Permission denied: READ_SMS is required for Equity sync")
            }

            val cursor = try {
                context.contentResolver.query(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    arrayOf(Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.DATE),
                    "${Telephony.Sms.Inbox.ADDRESS} IN (?, ?, ?, ?)",
                    arrayOf("EquitBank", "EquityBank", "EQUITYBANK", "EQUITY"),
                    "${Telephony.Sms.Inbox.DATE} DESC"
                )
            } catch (e: SecurityException) {
                throw Exception("Permission denied: READ_SMS is required for Equity sync", e)
            }

            if (cursor != null) {
                cursor.use { smsCursor ->
                    val bodyIndex = smsCursor.getColumnIndex(Telephony.Sms.Inbox.BODY)
                    val dateIndex = smsCursor.getColumnIndex(Telephony.Sms.Inbox.DATE)
                    var loggedCount = 0
                    val totalMessages = smsCursor.count

                    while (smsCursor.moveToNext()) {
                        currentCoroutineContext().job.ensureActive()
                        val body = smsCursor.getString(bodyIndex)
                        val timestamp = smsCursor.getLong(dateIndex)
                        val smsInstant = Instant.fromEpochMilliseconds(timestamp)

                        if (latestBalance == null) {
                            latestBalance = EquityParser.parseBalance(body)
                        }

                        val parsed = EquityParser.parse(body, accountId, smsInstant, rules)
                        if (parsed != null) {
                            val categoryName = parsed.category
                            val isExpense = !parsed.isIncome
                            val finalCategoryId = categories.find { cat ->
                                cat.name.equals(categoryName, ignoreCase = true) && cat.isExpense == isExpense
                            }?.id
                                ?: categories.find { cat ->
                                    cat.name.contains("Other", ignoreCase = true) && cat.isExpense == isExpense
                                }?.id
                                ?: "pending"

                            transactions.add(parsed.copy(categoryId = finalCategoryId))
                        }

                        loggedCount++
                        if (loggedCount % 100 == 0) onProgress(0.05f + (loggedCount.toFloat() / totalMessages) * 0.25f)
                    }
                }
            }
        } else {
            val dummyTransactions = portfolioSeeder.generateDummyTransactions(accountId, categories)
            transactions.addAll(dummyTransactions)
        }

        if (transactions.isNotEmpty()) {
            onProgress(0.3f)
            val chunks = transactions.chunked(250)
            chunks.forEachIndexed { index, chunk ->
                currentCoroutineContext().job.ensureActive()
                val result = transactionRepository.importEquityTransactions(chunk)
                if (result is Result.Error) throw result.exception
                onProgress(0.3f + (((index + 1).toFloat() / chunks.size) * 0.6f))
            }
        }

        // Update lastSyncedAt even if no transactions were found, 
        // as long as the process reached this point without error.
        val accountResult = accountRepository.getAccountById(accountId)
        if (accountResult is Result.Success) {
            val account = accountResult.data
            val isPureEquityAccount = account.linkedSources.contains("equity") && account.linkedSources.size == 1
            
            // Only update balance if we actually found transactions or are seeding
            val newBalance = when {
                isPortfolioSeed -> BigDecimal.fromInt(145800)
                transactions.isNotEmpty() && isPureEquityAccount -> latestBalance ?: account.balance
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
