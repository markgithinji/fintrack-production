package com.fintrack.shared.feature.transaction.service.automation

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fintrack.shared.R
import com.fintrack.shared.feature.account.domain.repository.AccountRepository
import com.fintrack.shared.feature.core.util.Result as AppResult
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.fintrack.shared.feature.transaction.domain.repository.TransactionRepository
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock

class TransactionSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val transactionRepository: TransactionRepository by inject()
    private val accountRepository: AccountRepository by inject()

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, "transaction_reminders")
            .setSmallIcon(R.drawable.ic_notification_sync)
            .setContentTitle("Syncing Transaction")
            .setContentText("Updating your records...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    override suspend fun doWork(): ListenableWorker.Result {
        val transactionJson = inputData.getString(KEY_TRANSACTION_JSON) ?: return ListenableWorker.Result.failure()
        
        return try {
            val transaction = Json.decodeFromString<Transaction>(transactionJson)
            val result = transactionRepository.addTransaction(transaction)
            
            if (result is AppResult.Success) {
                // Also update account balance if we have it parsed from the SMS
                transaction.balance?.let { newBalance ->
                    val accResult = accountRepository.getAccountById(transaction.accountId)
                    if (accResult is AppResult.Success) {
                        val account = accResult.data
                        accountRepository.addOrUpdateAccount(
                            account.copy(
                                balance = newBalance,
                                lastSyncedAt = Clock.System.now()
                            )
                        )
                    }
                }
                ListenableWorker.Result.success()
            } else {
                // If it's a network error, retry. If it's a 4xx error, maybe fail.
                ListenableWorker.Result.retry()
            }
        } catch (_: Exception) {
            ListenableWorker.Result.failure()
        }
    }

    companion object {
        const val KEY_TRANSACTION_JSON = "transaction_json"
        private const val NOTIFICATION_ID = 999

        fun enqueue(context: Context, transaction: Transaction) {
            val data = Data.Builder()
                .putString(KEY_TRANSACTION_JSON, Json.encodeToString(transaction))
                .build()
            
            val workRequest = OneTimeWorkRequestBuilder<TransactionSyncWorker>()
                .setInputData(data)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag("transaction_sync")
                .build()
            
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
