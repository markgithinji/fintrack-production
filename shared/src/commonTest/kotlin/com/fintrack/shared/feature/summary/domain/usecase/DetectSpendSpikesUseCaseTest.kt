package com.fintrack.shared.feature.summary.domain.usecase

import com.fintrack.shared.feature.core.domain.service.NotificationService
import com.fintrack.shared.feature.core.util.DateTimeHelper
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.fintrack.shared.feature.transaction.domain.repository.FakeTransactionRepository
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DetectSpendSpikesUseCaseTest {

    private val transactionRepository = FakeTransactionRepository()
    private val fakeNotificationService = FakeTestNotificationService()
    private val useCase = DetectSpendSpikesUseCase(transactionRepository, fakeNotificationService)

    @Test
    fun `detects spend spikes successfully`() = runTest {
        val result = useCase()
        assertTrue(result is Result.Success)
    }

    @Test
    fun `dispatches notification for detected category spend spike and deduplicates within month`() = runTest {
        val now = DateTimeHelper.now()
        val currentTx = Transaction(
            id = "tx_1",
            accountId = "acc_1",
            amount = BigDecimal.fromInt(25000),
            categoryId = "cat_dining",
            category = "Dining Out",
            isIncome = false,
            dateTime = now,
            description = "Dining Out",
            merchantName = "Restaurant"
        )
        transactionRepository.setTransactions(listOf(currentTx))

        val result1 = useCase()
        assertTrue(result1 is Result.Success)

        // Check that notification was triggered for dining spend
        assertEquals(1, fakeNotificationService.notificationsSent.size)
        assertTrue(fakeNotificationService.notificationsSent.first().contains("Dining Out"))

        // Run again during the same month: notification should be deduplicated and NOT re-sent
        val result2 = useCase()
        assertTrue(result2 is Result.Success)
        assertEquals(1, fakeNotificationService.notificationsSent.size)
    }
}

private class FakeTestNotificationService : NotificationService {
    val notificationsSent = mutableListOf<String>()

    override fun showReminderNotification() {}
    override fun scheduleDailyReminder(time: LocalTime?) {}
    override fun cancelDailyReminder() {}
    override fun requestPermission(callback: (Boolean) -> Unit) { callback(true) }
    override fun showTransactionNotification(transaction: Transaction) {}
    override fun showBudgetAlertNotification(budgetName: String, threshold: Int) {}
    override fun showBillReminderNotification(billName: String, amount: BigDecimal) {}
    override fun scheduleBillReminder(billName: String, amount: BigDecimal, dueDate: LocalDate, daysBefore: Int) {}
    override fun showSummaryNotification(title: String, content: String) {
        notificationsSent.add("$title: $content")
    }
    override fun scheduleSummaryNotification(time: LocalTime) {}
    override fun cancelSummaryNotification() {}
    override fun areNotificationsEnabled(): Boolean = true
}
