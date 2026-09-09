package com.fintrack.shared.feature.core.service

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fintrack.shared.R
import com.fintrack.shared.feature.core.domain.service.NotificationService
import com.fintrack.shared.feature.core.util.formatToAmount
import com.fintrack.shared.feature.settings.domain.datasource.SettingsDataSource
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import java.util.Calendar

class AndroidNotificationService(
    private val context: Context,
    private val settingsDataSource: SettingsDataSource
) : NotificationService {

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = NotificationConstants.CHANNEL_NAME
            val descriptionText = NotificationConstants.CHANNEL_DESCRIPTION
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NotificationConstants.CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun showReminderNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val builder = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Fintrack Reminder")
            .setContentText("Don't forget to log your transactions today!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NotificationConstants.ID_REMINDER_NOTIFICATION, builder.build())
            }
        } catch (_: SecurityException) {
        }
    }

    override fun showTransactionNotification(transaction: Transaction) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(NotificationConstants.EXTRA_TRANSACTION_ID, transaction.id)
            putExtra(NotificationConstants.EXTRA_ACTION, NotificationConstants.ACTION_EDIT_TRANSACTION)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            transaction.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showDecimals = runBlocking { settingsDataSource.showDecimals.first() }
        val amountStr = "Ksh ${transaction.amount.formatToAmount(showDecimals = showDecimals)}"
        val emoji = if (transaction.isIncome) "💰" else "💸"
        val merchant =
            transaction.description?.split("(Ref:")?.get(0)?.trim() ?: transaction.category

        val title = "New Transaction Detected"
        val contentText = "$emoji $amountStr at $merchant"

        val iconRes =
            if (transaction.isIncome) R.drawable.ic_notification_income else R.drawable.ic_notification_expense

        val builder = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText("$emoji $amountStr detected from M-Pesa.\n\nMerchant: $merchant\nCategory: ${transaction.category}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(0, "View Details", pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(transaction.id.hashCode(), builder.build())
            }
        } catch (_: SecurityException) {
        }
    }

    override fun showBudgetAlertNotification(budgetName: String, threshold: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val title = "Budget Alert: $budgetName"
        val contentText = "You've reached $threshold% of your budget limit."

        val builder = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(budgetName.hashCode() + threshold, builder.build())
            }
        } catch (_: SecurityException) {
        }
    }

    override fun showBillReminderNotification(billName: String, amount: BigDecimal) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val showDecimals = runBlocking { settingsDataSource.showDecimals.first() }
        val amountStr = "Ksh ${amount.formatToAmount(showDecimals = showDecimals)}"
        val title = "Upcoming Bill: $billName"
        val contentText = "Your bill of $amountStr is due soon."

        val builder = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(billName.hashCode(), builder.build())
            }
        } catch (_: SecurityException) {
        }
    }

    override fun scheduleBillReminder(
        billName: String,
        amount: BigDecimal,
        dueDate: LocalDate,
        daysBefore: Int
    ) {
        val reminderDate = dueDate.minus(daysBefore, DateTimeUnit.DAY)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, reminderDate.year)
            set(Calendar.MONTH, reminderDate.month.ordinal)
            set(Calendar.DAY_OF_MONTH, reminderDate.day)
            set(Calendar.HOUR_OF_DAY, NotificationConstants.DEFAULT_BILL_REMINDER_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.before(Calendar.getInstance())) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = NotificationConstants.ACTION_SHOW_BILL_REMINDER
            putExtra(NotificationConstants.EXTRA_BILL_NAME, billName)
            putExtra(NotificationConstants.EXTRA_AMOUNT, amount.toString())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            billName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    val info = AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
                    alarmManager.setAlarmClock(info, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                val info = AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
                alarmManager.setAlarmClock(info, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    override fun showSummaryNotification(title: String, content: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val builder = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(title.hashCode(), builder.build())
            }
        } catch (_: SecurityException) {
        }
    }

    override fun scheduleSummaryNotification(time: LocalTime) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = NotificationConstants.ACTION_SHOW_SUMMARY
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NotificationConstants.ID_SUMMARY_PENDING_INTENT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, time.hour)
            set(Calendar.MINUTE, time.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    val info = AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
                    alarmManager.setAlarmClock(info, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                val info = AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
                alarmManager.setAlarmClock(info, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    override fun cancelSummaryNotification() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = NotificationConstants.ACTION_SHOW_SUMMARY
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NotificationConstants.ID_SUMMARY_PENDING_INTENT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    override fun scheduleDailyReminder(time: LocalTime?) {
        val reminderTime = time ?: LocalTime(NotificationConstants.DEFAULT_DAILY_REMINDER_HOUR, NotificationConstants.DEFAULT_DAILY_REMINDER_MINUTE)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = NotificationConstants.ACTION_SHOW_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NotificationConstants.ID_DAILY_REMINDER_PENDING_INTENT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminderTime.hour)
            set(Calendar.MINUTE, reminderTime.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    val info = AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
                    alarmManager.setAlarmClock(info, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                val info = AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
                alarmManager.setAlarmClock(info, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    override fun cancelDailyReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = NotificationConstants.ACTION_SHOW_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NotificationConstants.ID_DAILY_REMINDER_PENDING_INTENT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    override fun requestPermission(callback: (Boolean) -> Unit) {
        val areEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        callback(areEnabled && hasPermission)
    }
}
