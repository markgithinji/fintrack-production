package com.fintrack.shared.feature.budget.domain.usecase

import com.fintrack.shared.feature.budget.domain.repository.BudgetRepository
import com.fintrack.shared.feature.settings.domain.datasource.SettingsDataSource
import com.fintrack.shared.feature.core.domain.service.NotificationService
import com.fintrack.shared.feature.core.util.Result
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.flow.first

class CheckBudgetThresholdsUseCase(
    private val budgetRepository: BudgetRepository,
    private val settingsDataSource: SettingsDataSource,
    private val notificationService: NotificationService
) {
    suspend operator fun invoke() {
        val alertsEnabled = settingsDataSource.budgetAlertsEnabled.first()
        if (!alertsEnabled) return

        val alertBudgetId = settingsDataSource.alertBudgetId.first() ?: return
        val thresholds = settingsDataSource.budgetAlertThresholds.first()
        if (thresholds.isEmpty()) return

        val result = budgetRepository.getBudgets()
        if (result is Result.Success) {
            val budgetWithStatus = result.data.find { it.budget.id == alertBudgetId } ?: return
            
            // percentageUsed is 0.0 to 100.0 (e.g. 85.0 for 85%)
            val actualPercent = budgetWithStatus.status.percentageUsed

            val thresholdReached = thresholds
                .filter { BigDecimal.fromInt(it) <= actualPercent }
                .maxByOrNull { it }

            if (thresholdReached != null) {
                notificationService.showBudgetAlertNotification(
                    budgetName = budgetWithStatus.budget.name,
                    threshold = thresholdReached
                )
            }
        }
    }
}
