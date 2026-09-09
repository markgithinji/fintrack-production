package com.fintrack.shared.feature.transaction.service.importer

import android.content.Context
import com.fintrack.shared.feature.account.domain.repository.AccountRepository
import com.fintrack.shared.feature.category.domain.repository.CategoryRepository
import com.fintrack.shared.feature.transaction.domain.repository.TransactionRepository
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.settings.domain.datasource.SettingsDataSource
import com.fintrack.shared.feature.transaction.domain.service.TransactionImporter
import kotlinx.coroutines.flow.first

class AndroidTransactionImporter(
    private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsDataSource: SettingsDataSource
) : TransactionImporter {
    override suspend fun importHistory(
        targetAccountId: String?,
        isPortfolioSeed: Boolean,
        onProgress: (Float) -> Unit
    ) {
        val accountsResult = accountRepository.getAccounts()
        if (accountsResult is Result.Error && !isPortfolioSeed) {
            throw accountsResult.exception
        }

        val accounts = (accountsResult as? Result.Success)?.data ?: emptyList()
        
        val mpesaImporter = MpesaImporter(context, transactionRepository, accountRepository, categoryRepository)
        val equityImporter = EquityImporter(context, transactionRepository, accountRepository, categoryRepository)

        val mpesaLinkedAccountIds = settingsDataSource.mpesaLinkedAccountIds.first()
        val equityLinkedAccountIds = settingsDataSource.equityLinkedAccountIds.first()

        if (targetAccountId != null) {
            val hasMpesa = mpesaLinkedAccountIds.contains(targetAccountId)
            val hasEquity = equityLinkedAccountIds.contains(targetAccountId)
            
            when {
                isPortfolioSeed -> {
                    if (hasMpesa && hasEquity) {
                        mpesaImporter.importHistory(targetAccountId, true) { onProgress(it * 0.5f) }
                        equityImporter.importHistory(targetAccountId, true) { onProgress(0.5f + (it * 0.5f)) }
                    } else if (hasEquity) {
                        equityImporter.importHistory(targetAccountId, true, onProgress)
                    } else {
                        // Default to M-Pesa for seeding
                        mpesaImporter.importHistory(targetAccountId, true, onProgress)
                    }
                }
                hasMpesa && hasEquity -> {
                    mpesaImporter.importHistory(targetAccountId, isPortfolioSeed) { onProgress(it * 0.5f) }
                    equityImporter.importHistory(targetAccountId, isPortfolioSeed) { onProgress(0.5f + (it * 0.5f)) }
                }
                hasMpesa -> {
                    mpesaImporter.importHistory(targetAccountId, isPortfolioSeed, onProgress)
                }
                hasEquity -> {
                    equityImporter.importHistory(targetAccountId, isPortfolioSeed, onProgress)
                }
                else -> {
                    onProgress(1.0f)
                }
            }
            return
        }

        // Global sync
        val mpesaAccounts = accounts.filter { mpesaLinkedAccountIds.contains(it.id) }
        val equityAccounts = accounts.filter { equityLinkedAccountIds.contains(it.id) }

        if (mpesaAccounts.isEmpty() && equityAccounts.isEmpty()) {
            if (isPortfolioSeed) {
                val fallback = accounts.firstOrNull()
                mpesaImporter.importHistory(fallback?.id, true, onProgress)
            } else {
                onProgress(1.0f)
            }
            return
        }

        // Perform sync for each account
        val totalSteps = mpesaAccounts.size + equityAccounts.size
        var currentStep = 0

        mpesaAccounts.forEach { account ->
            mpesaImporter.importHistory(account.id, isPortfolioSeed) { 
                onProgress((currentStep + it) / totalSteps)
            }
            currentStep++
        }

        equityAccounts.forEach { account ->
            equityImporter.importHistory(account.id, isPortfolioSeed) {
                onProgress((currentStep + it) / totalSteps)
            }
            currentStep++
        }
    }
}
