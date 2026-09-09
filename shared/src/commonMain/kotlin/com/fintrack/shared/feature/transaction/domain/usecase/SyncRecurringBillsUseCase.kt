package com.fintrack.shared.feature.transaction.domain.usecase

import com.fintrack.shared.feature.transaction.domain.repository.TransactionRepository
import com.fintrack.shared.feature.settings.domain.datasource.SettingsDataSource
import com.fintrack.shared.feature.core.domain.service.NotificationService
import com.fintrack.shared.feature.core.util.Result
import kotlinx.coroutines.flow.first

class SyncRecurringBillsUseCase(
    private val transactionRepository: TransactionRepository,
    private val settingsDataSource: SettingsDataSource,
    private val notificationService: NotificationService
) {
    suspend operator fun invoke() {
        val isEnabled = settingsDataSource.isBillReminderEnabled.first()
        if (!isEnabled) return

        val daysBefore = settingsDataSource.billReminderDaysBefore.first()

        val result = transactionRepository.getRecurringBills()
        if (result is Result.Success) {
            val bills = result.data
            bills.forEach { bill ->
                if (bill.isActive) {
                    notificationService.scheduleBillReminder(
                        billName = bill.name,
                        amount = bill.amount,
                        dueDate = bill.nextDueDate,
                        daysBefore = daysBefore
                    )
                }
            }
        }
    }
}
