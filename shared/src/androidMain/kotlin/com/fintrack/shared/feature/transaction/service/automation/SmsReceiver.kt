package com.fintrack.shared.feature.transaction.service.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.fintrack.shared.feature.account.domain.repository.AccountRepository
import com.fintrack.shared.feature.category.domain.repository.CategoryRepository
import com.fintrack.shared.feature.core.domain.service.NotificationService
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.settings.domain.datasource.SettingsDataSource
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.fintrack.shared.feature.transaction.util.EquityParser
import com.fintrack.shared.feature.transaction.util.MpesaParser
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.abs

class SmsReceiver : BroadcastReceiver(), KoinComponent {
    private val accountRepository: AccountRepository by inject()
    private val categoryRepository: CategoryRepository by inject()
    private val settingsDataSource: SettingsDataSource by inject()
    private val notificationService: NotificationService by inject()

    companion object {
        // REAL-TIME DUPLICATE SMS FILTER (Timer #1):
        // Prevents double-counting when Safaricom/Equity sends dual SMS broadcasts in rapid succession
        // (e.g. payment receipt + M-Shwari/Fuliza balance update within seconds of each other).
        // Caches (Amount, Timestamp, Sender) for 60 seconds (1 minute) to ignore duplicate broadcasts.
        private val recentTransactions = mutableListOf<Triple<BigDecimal, Long, String>>()

        @Synchronized
        private fun isDuplicate(amount: BigDecimal, timestamp: Long, sender: String?): Boolean {
            val now = System.currentTimeMillis() / 1000
            // Keep signatures from the last 60 seconds
            recentTransactions.removeAll { it.second < (now - 60) }
            
            val senderPrefix = sender?.take(5) ?: ""
            // Ignore duplicate SMS with matching amount and provider sender prefix within 60 seconds
            val exists = recentTransactions.any { (a, t, s) ->
                a == amount && abs(t - timestamp) < 60 && s == senderPrefix
            }
            
            if (!exists) {
                recentTransactions.add(Triple(amount, timestamp, senderPrefix))
            }
            return exists
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION || context == null) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val fullMessage = messages.joinToString("") { it.displayMessageBody }
        val sender = messages.firstOrNull()?.displayOriginatingAddress

        val isMpesa = sender?.contains("MPESA", ignoreCase = true) == true || 
                     sender?.contains("M-PESA", ignoreCase = true) == true
        
        // STRICT SENDER FILTER: Match ONLY official 'EquityBank' sender ID.
        // Ignore M-Pesa confirmatory texts that mention Equity Bank to avoid misassigning M-Pesa transactions.
        val isEquity = sender?.contains("Equity", ignoreCase = true) == true || 
                      sender?.contains("Equit", ignoreCase = true) == true

        if (isMpesa || isEquity) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val isEnabled = if (isMpesa) {
                        settingsDataSource.isMpesaListenerEnabled.first()
                    } else {
                        settingsDataSource.isEquityListenerEnabled.first()
                    }

                    if (!isEnabled) return@launch

                    val accountsResult = accountRepository.getAccounts()
                    val accounts = (accountsResult as? Result.Success)?.data ?: emptyList()
                    
                    val matchedAccountId = if (isMpesa) {
                        accounts.find { it.linkedSources.contains("mpesa") }?.id
                    } else {
                        accounts.find { it.linkedSources.contains("equity") }?.id
                    }

                    val fallbackAccount = accounts.find { it.isDefault } ?: accounts.firstOrNull()
                    val accountId = matchedAccountId ?: fallbackAccount?.id ?: return@launch
                    
                    val categoriesResult = categoryRepository.getCategories()
                    val allCategories = (categoriesResult as? Result.Success)?.data ?: emptyList()

                    val rulesResult = categoryRepository.getCategoryRules()
                    val rules = (rulesResult as? Result.Success)?.data ?: emptyList()

                    var transaction: Transaction? = if (isMpesa) {
                        MpesaParser.parse(fullMessage, accountId, null, rules, allCategories)
                    } else {
                        EquityParser.parse(fullMessage, accountId, null, rules, allCategories)
                    }

                    if (transaction != null) {
                        // Fuzzy deduplication for real-time messages
                        if (isDuplicate(transaction.amount, transaction.dateTime.epochSeconds, sender)) {
                            return@launch
                        }

                        // Map category name to ID (Only if parser didn't resolve to a fixed UUID)
                        if (transaction.categoryId == "pending") {
                            val categoriesResult = categoryRepository.getCategories()
                            val categories = (categoriesResult as? Result.Success)?.data ?: emptyList()
                            val categoryName = transaction.category
                            val isExpense = !transaction.isIncome
                            
                            val categoryId = categories.find { 
                                it.name.equals(categoryName, ignoreCase = true) && it.isExpense == isExpense 
                            }?.id ?: categories.find { 
                                it.name.equals("Transfer", ignoreCase = true) && it.isExpense == isExpense 
                            }?.id ?: categories.find {
                                it.name.contains("Other", ignoreCase = true) && it.isExpense == isExpense
                            }?.id ?: categories.find {
                                it.name.contains("Misc", ignoreCase = true) && it.isExpense == isExpense
                            }?.id ?: categories.firstOrNull { it.isExpense == isExpense }?.id
                            ?: "pending"
                            
                            transaction = transaction.copy(categoryId = categoryId)
                        }

                        // Show notification immediately so the user knows we caught it
                        notificationService.showTransactionNotification(transaction)

                        // Enqueue reliable background sync via WorkManager
                        TransactionSyncWorker.enqueue(context, transaction)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
