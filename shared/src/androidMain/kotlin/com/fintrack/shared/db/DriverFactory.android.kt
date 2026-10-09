package com.fintrack.shared.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = FintrackDatabase.Schema,
            context = context,
            name = "fintrack.db",
            callback = object : AndroidSqliteDriver.Callback(FintrackDatabase.Schema) {
                override fun onConfigure(db: SupportSQLiteDatabase) {
                    super.onConfigure(db)
                    db.setForeignKeyConstraintsEnabled(true)
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    try {
                        super.onUpgrade(db, oldVersion, newVersion)
                    } catch (e: Exception) {
                        // Catch duplicate column exception if column was previously added on device
                        if (e.message?.contains("duplicate column", ignoreCase = true) == true) {
                            // Column already exists, safe to ignore
                        } else {
                            throw e
                        }
                    }
                }
            }
        )
    }
}
