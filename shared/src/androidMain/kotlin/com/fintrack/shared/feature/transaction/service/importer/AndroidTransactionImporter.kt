package com.fintrack.shared.feature.transaction.service.importer

import android.content.Context
import android.util.Log
import com.fintrack.shared.feature.account.domain.model.AccountType
import com.fintrack.shared.feature.account.domain.repository.AccountRepository
import com.fintrack.shared.feature.category.domain.repository.CategoryRepository
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.settings.domain.datasource.SettingsDataSource
import com.fintrack.shared.feature.transaction.domain.repository.TransactionRepository
import com.fintrack.shared.feature.transaction.domain.service.TransactionImporter

class AndroidTransactionImporter(
    private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsDataSource: SettingsDataSource
) : TransactionImporter {

    companion object {
        private const val TAG = "SMS_SYNC"
    }

    override suspend fun importHistory(
        targetAccountId: String?,
        isPortfolioSeed: Boolean,
        onProgress: (Float) -> Unit
    ) {
        Log.d(TAG, "Starting importHistory: targetAccountId=$targetAccountId, isPortfolioSeed=$isPortfolioSeed")

        val accountsResult = accountRepository.getAccounts()
        if (accountsResult is Result.Error && !isPortfolioSeed) {
            Log.e(TAG, "Error fetching accounts for SMS sync", accountsResult.exception)
            throw accountsResult.exception
        }

        val accounts = (accountsResult as? Result.Success)?.data ?: emptyList()
        Log.d(TAG, "Found ${accounts.size} total accounts in database.")

        val mpesaImporter = MpesaImporter(context, transactionRepository, accountRepository, categoryRepository)
        val equityImporter = EquityImporter(context, transactionRepository, accountRepository, categoryRepository)

        if (targetAccountId != null) {
            val account = accounts.find { it.id == targetAccountId }
            Log.d(TAG, "Importing for specific target account: ${account?.name} (id=$targetAccountId)")

            val isExplicitBoth = account?.linkedSources?.contains("mpesa") == true && account?.linkedSources?.contains("equity") == true

            val isEquityOnly = !isExplicitBoth && (
                account?.linkedSources?.contains("equity") == true ||
                account?.name?.contains("equity", ignoreCase = true) == true
            )

            when {
                isPortfolioSeed -> {
                    if (isExplicitBoth) {
                        mpesaImporter.importHistory(targetAccountId, true) { onProgress(it * 0.5f) }
                        equityImporter.importHistory(targetAccountId, true) { onProgress(0.5f + (it * 0.5f)) }
                    } else if (isEquityOnly) {
                        equityImporter.importHistory(targetAccountId, true, onProgress)
                    } else {
                        mpesaImporter.importHistory(targetAccountId, true, onProgress)
                    }
                }
                isExplicitBoth -> {
                    mpesaImporter.importHistory(targetAccountId, isPortfolioSeed) { onProgress(it * 0.5f) }
                    equityImporter.importHistory(targetAccountId, isPortfolioSeed) { onProgress(0.5f + (it * 0.5f)) }
                }
                isEquityOnly -> {
                    equityImporter.importHistory(targetAccountId, isPortfolioSeed, onProgress)
                }
                else -> {
                    // Sync M-Pesa for M-Pesa or default accounts
                    mpesaImporter.importHistory(targetAccountId, isPortfolioSeed, onProgress)
                }
            }
            return
        }

        // Global Sync
        val mpesaAccounts = accounts.filter {
            it.linkedSources.contains("mpesa") ||
                    it.type == AccountType.MPESA ||
                    it.name.contains("mpesa", ignoreCase = true) ||
                    it.name.contains("m-pesa", ignoreCase = true)
        }

        val equityAccounts = accounts.filter {
            it.linkedSources.contains("equity") ||
                    it.type == AccountType.BANK ||
                    it.name.contains("equity", ignoreCase = true) ||
                    it.name.contains("bank", ignoreCase = true)
        }

        Log.d(TAG, "Global sync: found ${mpesaAccounts.size} M-Pesa candidate accounts and ${equityAccounts.size} Equity candidate accounts.")

        val effectiveMpesaAccounts = if (mpesaAccounts.isEmpty() && equityAccounts.isEmpty() && accounts.isNotEmpty()) {
            val fallback = accounts.find { it.isDefault } ?: accounts.first()
            Log.d(TAG, "No specific linked accounts found. Falling back to account '${fallback.name}' (${fallback.id}) for M-Pesa SMS sync.")
            listOf(fallback)
        } else {
            mpesaAccounts
        }

        val totalSteps = (effectiveMpesaAccounts.size + equityAccounts.size).coerceAtLeast(1)
        var currentStep = 0

        effectiveMpesaAccounts.forEach { account ->
            Log.d(TAG, "Syncing M-Pesa SMS for account: ${account.name} (${account.id})")
            mpesaImporter.importHistory(account.id, isPortfolioSeed) {
                onProgress((currentStep + it) / totalSteps)
            }
            currentStep++
        }

        equityAccounts.forEach { account ->
            Log.d(TAG, "Syncing Equity SMS for account: ${account.name} (${account.id})")
            equityImporter.importHistory(account.id, isPortfolioSeed) {
                onProgress((currentStep + it) / totalSteps)
            }
            currentStep++
        }

        onProgress(1.0f)
        Log.d(TAG, "SMS sync finished successfully.")
    }
}
