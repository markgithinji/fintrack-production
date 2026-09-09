package com.fintrack.shared.feature.transaction.ui.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Savings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.fintrack.shared.feature.account.domain.model.AccountType

sealed class AccountIcon(val icon: ImageVector, val color: Color) {
    object Bank : AccountIcon(Icons.Default.AccountBalance, Color(0xFF1976D2))
    object Wallet : AccountIcon(Icons.Default.AccountCircle, Color(0xFF00897B))
    object Cash : AccountIcon(Icons.Default.Money, Color(0xFFF57C00))
    object Savings : AccountIcon(Icons.Default.Savings, Color(0xFF0097A7))
    object Mpesa : AccountIcon(Icons.Default.Money, Color(0xFF2E7D32))
    object Equity : AccountIcon(Icons.Default.AccountBalance, Color(0xFF8B0000)) // Brownish red for Equity
    object Default : AccountIcon(Icons.Default.AccountBalance, Color(0xFF616161))

    companion object {
        /** Map account types to icons */
        fun fromAccountType(type: AccountType, name: String): AccountIcon {
            return when (type) {
                AccountType.MPESA -> Mpesa
                AccountType.BANK -> Bank
                AccountType.CASH -> Cash
                AccountType.WALLET -> Wallet
                AccountType.SAVINGS -> Savings
                AccountType.OTHER -> fromAccountName(name)
            }
        }

        fun fromAccountName(name: String): AccountIcon {
            val lower = name.lowercase()
            return when {
                lower.contains("mpesa") -> Mpesa
                lower.contains("equity") -> Equity
                lower.contains("bank") -> Bank
                lower.contains("wallet") -> Wallet
                lower.contains("cash") -> Cash
                lower.contains("savings") -> Savings
                else -> Default
            }
        }
    }
}
