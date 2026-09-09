package com.fintrack.shared.ui.theme
import androidx.compose.ui.graphics.Color

// Finance App Colors - Light Theme
val primaryLight = Color(0xFF1B1B21) // Black
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFFEFEFEF)
val onPrimaryContainerLight = Color(0xFF1B1B21)
val secondaryLight = Color(0xFFFFD600) // Bright Yellow
val onSecondaryLight = Color(0xFF1B1B21)
val secondaryContainerLight = Color(0xFFFFF9C4)
val onSecondaryContainerLight = Color(0xFF5B4300)
val tertiaryLight = Color(0xFF1FC287) // Green Income
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFFD1FADF)
val onTertiaryContainerLight = Color(0xFF027A48)
val errorLight = Color(0xFFBA1A1A)
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFFDAD6)
val onErrorContainerLight = Color(0xFF93000A)
val backgroundLight = Color(0xFFF5F5F5) // Very light gray for screen background
val onBackgroundLight = Color(0xFF1B1B21)
val surfaceLight = Color(0xFFFFFFFF) // Pure white for cards/containers
val onSurfaceLight = Color(0xFF1B1B21)
val surfaceVariantLight = Color(0xFFEFEFEF) // Subtle gray for secondary containers
val onSurfaceVariantLight = Color(0xFF46464F)
val outlineLight = Color(0xFF767680)
val inverseSurfaceLight = Color(0xFF303036)
val inverseOnSurfaceLight = Color(0xFFF2EFF7)

// Finance App Colors - Dark Theme
val primaryDark = Color(0xFFFFFFFF)
val onPrimaryDark = Color(0xFF1B1B21)
val primaryContainerDark = Color(0xFF2D2D2D)
val onPrimaryContainerDark = Color(0xFFFFFFFF)
val secondaryDark = Color(0xFFFFB300) // Darker Yellow/Gold for better contrast in dark mode
val onSecondaryDark = Color(0xFF1B1B21)
val secondaryContainerDark = Color(0xFF4F3A00)
val onSecondaryContainerDark = Color(0xFFFFF9C4)
val tertiaryDark = Color(0xFF1FC287)
val onTertiaryDark = Color(0xFF003921)
val tertiaryContainerDark = Color(0xFF005232)
val onTertiaryContainerDark = Color(0xFFD1FADF)
val errorDark = Color(0xFFFFB4AB)
val onErrorDark = Color(0xFF690005)
val errorContainerDark = Color(0xFF93000A)
val onErrorContainerDark = Color(0xFFFFDAD6)
val backgroundDark = Color(0xFF121212)
val onBackgroundDark = Color(0xFFE4E1E9)
val surfaceDark = Color(0xFF1E1E1E)
val onSurfaceDark = Color(0xFFE4E1E9)
val surfaceVariantDark = Color(0xFF2D2D2D)
val onSurfaceVariantDark = Color(0xFFC7C5D0)
val outlineDark = Color(0xFF90909A)
val inverseSurfaceDark = Color(0xFFE4E1E9)
val inverseOnSurfaceDark = Color(0xFF303036)

// Finance-specific semantic colors
val GreenIncome = Color(0xFF1FC287) // green for income
val PinkExpense = Color(0xFFE27C94)  // pinkish-red for expense
val backgroundGray = Color(0xFFEFEFEF)
val YellowWarning = Color(0xFFFFD600)  // Bright Yellow
val PurpleBudget = Color(0xFF8B5CF6)   // Purple for budgets/categories
val AuthGold = Color(0xFFFFB300)      // Balanced Gold for buttons/success (original yellow-gold)
val AuthLinkText = Color(0xFFC59000)   // Darker gold specifically for text links to avoid being "washed out"

// Chart segments
val SegmentColor1 = Color(0xFFE63946) // Strong red
val SegmentColor2 = Color(0xFF228B22) // Forest Green
val SegmentColor3 = Color(0xFF457B9D) // Vibrant blue
val SegmentColor4 = Color(0xFFF4A261) // Warm orange
val SegmentColor5 = Color(0xFF2A9D8F) // Teal / turquoise

// Period Selector Colors
val periodSelectedBg = Color(0xFF2D2D2D)
val periodUnselectedBg = Color(0xFFE0E0E0)
val periodSelectedText = Color.White
val periodUnselectedText = Color.Black

// Category List Colors
val categoryCardBg = Color(0xFFF4F4F4)
val categoryNameText = Color.DarkGray
val categoryAmountText = Color.Black
val categoryPercentageText = Color.DarkGray.copy(alpha = 0.8f)

// Error State Colors
val errorIconColor = Color.Gray
val errorHeaderText = Color.Black
val errorMessageText = Color.Gray
val errorRetryButton = GreenIncome
val errorRetryButtonText = Color.White


val cardBackground = Color(0xFFF5F5F5) // For cards/containers
val currencyTextColor = GreenIncome // For Ksh text
val accountChipSelectedBg = Color(0xFFE3F2FD) // Selected account chip
val accountChipBorder = Color(0xFF2196F3) // Selected account chip border
val incomeButtonColor = Color(0xFF2E7D32) // Income button color
val transactionBackground = Color(0xFFF5F5F5)