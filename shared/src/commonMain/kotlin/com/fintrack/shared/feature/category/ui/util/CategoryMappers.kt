package com.fintrack.shared.feature.category.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.RealEstateAgent
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.fintrack.shared.feature.category.domain.model.Category


fun Category.toIcon(): ImageVector = when (this.iconName ?: this.name) {
    "Fastfood", "Food" -> Icons.Default.Fastfood
    "DirectionsCar", "Transport" -> Icons.Default.DirectionsCar
    "ShoppingCart", "Shopping" -> Icons.Default.ShoppingCart
    "LocalHospital", "Health" -> Icons.Default.LocalHospital
    "Receipt", "Bills" -> Icons.Default.Receipt
    "Movie", "Entertainment" -> Icons.Default.Movie
    "School", "Education" -> Icons.Default.School
    "CardGiftcard", "Gifts" -> Icons.Default.CardGiftcard
    "Flight", "Travel" -> Icons.Default.Flight
    "ContentCut", "Personal Care" -> Icons.Default.ContentCut
    "Subscriptions" -> Icons.Default.Subscriptions
    "Home", "Rent" -> Icons.Default.Home
    "ShoppingBag", "Groceries" -> Icons.Default.ShoppingBag
    "Shield", "Insurance" -> Icons.Default.Shield
    "Restaurant", "Dining Out" -> Icons.Default.Restaurant
    "VolunteerActivism", "Charity" -> Icons.Default.VolunteerActivism
    "AccountBalanceWallet", "Loans" -> Icons.Default.AccountBalanceWallet
    "AccountBalance", "Bank", "Government" -> Icons.Default.AccountBalance
    "Savings" -> Icons.Default.Savings
    "Wifi", "Internet" -> Icons.Default.Wifi
    "Smartphone", "Airtime" -> Icons.Default.Smartphone
    "Lightbulb", "Utilities" -> Icons.Default.Lightbulb
    "Pets" -> Icons.Default.Pets
    "FitnessCenter", "Fitness" -> Icons.Default.FitnessCenter
    "Build", "Maintenance" -> Icons.Default.Build
    "Transaction Fees" -> Icons.Default.Receipt
    "Paid", "Bonus" -> Icons.Default.Paid
    "RealEstateAgent", "Rental" -> Icons.Default.RealEstateAgent
    "Analytics", "Dividends" -> Icons.Default.Analytics
    "Percent", "Interest" -> Icons.Default.Percent
    "HelpOutline", "Misc", "Other" -> Icons.Default.HelpOutline
    "AttachMoney", "Salary", "Income" -> Icons.Default.AttachMoney
    "Work", "Freelance" -> Icons.Default.Work
    "TrendingUp", "Investments" -> Icons.Default.TrendingUp
    "Sync", "Transfer" -> Icons.Default.Sync
    else -> Icons.Default.HelpOutline
}

fun Category.toColor(): Color = when (this.iconName ?: this.name) {
    "Fastfood", "Food" -> Color(0xFFFFA726)
    "DirectionsCar", "Transport" -> Color(0xFF29B6F6)
    "ShoppingCart", "Shopping" -> Color(0xFFAB47BC)
    "LocalHospital", "Health" -> Color(0xFFEF5350)
    "Receipt", "Bills" -> Color(0xFF8D6E63)
    "Movie", "Entertainment" -> Color(0xFFFF7043)
    "School", "Education" -> Color(0xFF42A5F5)
    "CardGiftcard", "Gifts" -> Color(0xFFEC407A)
    "Flight", "Travel" -> Color(0xFF26C6DA)
    "ContentCut", "Personal Care" -> Color(0xFFFFCA28)
    "Subscriptions" -> Color(0xFF66BB6A)
    "Home", "Rent" -> Color(0xFF7E57C2)
    "ShoppingBag", "Groceries" -> Color(0xFF8BC34A)
    "Shield", "Insurance" -> Color(0xFF78909C)
    "Restaurant", "Dining Out" -> Color(0xFFF06292)
    "VolunteerActivism", "Charity" -> Color(0xFFE91E63)
    "AccountBalanceWallet", "Loans" -> Color(0xFF795548)
    "AccountBalance", "Bank", "Government" -> Color(0xFF1976D2)
    "Savings" -> Color(0xFF0097A7)
    "Wifi", "Internet" -> Color(0xFF4FC3F7)
    "Smartphone", "Airtime" -> Color(0xFF81C784)
    "Lightbulb", "Utilities" -> Color(0xFFFFD54F)
    "Pets" -> Color(0xFF8D6E63)
    "FitnessCenter", "Fitness" -> Color(0xFF4DB6AC)
    "Build", "Maintenance" -> Color(0xFF90A4AE)
    "Transaction Fees" -> Color(0xFF607D8B)
    "Paid", "Bonus" -> Color(0xFF66BB6A)
    "RealEstateAgent", "Rental" -> Color(0xFF5C6BC0)
    "Analytics", "Dividends" -> Color(0xFF26A69A)
    "Percent", "Interest" -> Color(0xFF4FC3F7)
    "HelpOutline", "Misc", "Other" -> Color.Gray
    "AttachMoney", "Salary", "Income" -> Color(0xFF2E7D32)
    "Work", "Freelance" -> Color(0xFF0097A7)
    "TrendingUp", "Investments" -> Color(0xFFFFB300)
    "Sync", "Transfer" -> Color(0xFF00796B)
    else -> Color.Gray
}
