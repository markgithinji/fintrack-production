package com.fintrack.shared.feature.core.service

object NotificationConstants {
    const val CHANNEL_ID = "transaction_reminders"
    const val CHANNEL_NAME = "Transaction Reminders"
    const val CHANNEL_DESCRIPTION = "Reminders to log your transactions"

    const val ACTION_SHOW_REMINDER = "com.fintrack.shared.ACTION_SHOW_REMINDER"
    const val ACTION_SHOW_BILL_REMINDER = "com.fintrack.shared.ACTION_SHOW_BILL_REMINDER"
    const val ACTION_SHOW_SUMMARY = "com.fintrack.shared.ACTION_SHOW_SUMMARY"

    const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
    const val ACTION_HTC_QUICKBOOT_POWERON = "com.htc.intent.action.QUICKBOOT_POWERON"

    const val EXTRA_TRANSACTION_ID = "transactionId"
    const val EXTRA_ACTION = "action"
    const val EXTRA_BILL_NAME = "billName"
    const val EXTRA_AMOUNT = "amount"

    const val ACTION_EDIT_TRANSACTION = "edit_transaction"

    const val ID_REMINDER_NOTIFICATION = 1
    const val ID_DAILY_REMINDER_PENDING_INTENT = 0
    const val ID_SUMMARY_PENDING_INTENT = 2

    const val DEFAULT_BILL_REMINDER_HOUR = 9
    const val DEFAULT_DAILY_REMINDER_HOUR = 20
    const val DEFAULT_DAILY_REMINDER_MINUTE = 0
}
