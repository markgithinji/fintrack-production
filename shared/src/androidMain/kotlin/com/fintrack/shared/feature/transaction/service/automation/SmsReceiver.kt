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
        // Cache to deduplicate real-time messages that arrive in pairs (e.g. Sent + Drawn)
        private val recentTransactions = mutableListOf<Triple<BigDecimal, Long, String>>()

        @Synchronized
        private fun isDuplicate(amount: BigDecimal, timestamp: Long, sender: String?): Boolean {
            val now = System.currentTimeMillis() / 1000
            // Keep only last 5 minutes of signatures
            recentTransactions.removeAll { it.second < (now - 300) }
            
            val senderPrefix = sender?.take(5) ?: ""
            // Check for similar amount and sender within 2 minutes
            val exists = recentTransactions.any { (a, t, s) ->
                a == amount && abs(t - timestamp) < 120 && s == senderPrefix
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
        
        val isEquity = sender?.contains("EquitBank", ignoreCase = true) == true || 
                      sender?.contains("EquityBank", ignoreCase = true) == true ||
                      sender?.equals("EQUITY", ignoreCase = true) == true

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
                    
                    val accountId = if (isMpesa) {
                        accounts.find { it.linkedSources.contains("mpesa") }?.id
                    } else {
                        accounts.find { it.linkedSources.contains("equity") }?.id
                    }

                    if (accountId == null) return@launch
                    
                    var transaction: Transaction? = if (isMpesa) {
                        MpesaParser.parse(fullMessage, accountId)
                    } else {
                        EquityParser.parse(fullMessage, accountId)
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
