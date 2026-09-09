package com.fintrack.shared.feature.category.domain.model

data class Category(
    val id: String,
    val name: String,
    val isExpense: Boolean,
    val iconName: String? = null,
    val isDefault: Boolean = false
) {
    companion object
}

// --- Companion Extensions (Presets) ---

val Category.Companion.Food get() = CategoryPresets.Food
val Category.Companion.Transport get() = CategoryPresets.Transport
val Category.Companion.Shopping get() = CategoryPresets.Shopping
val Category.Companion.Health get() = CategoryPresets.Health
val Category.Companion.Bills get() = CategoryPresets.Bills
val Category.Companion.Entertainment get() = CategoryPresets.Entertainment
val Category.Companion.Education get() = CategoryPresets.Education
val Category.Companion.GiftsExpense get() = CategoryPresets.GiftsExpense
val Category.Companion.Travel get() = CategoryPresets.Travel
val Category.Companion.PersonalCare get() = CategoryPresets.PersonalCare
val Category.Companion.Subscriptions get() = CategoryPresets.Subscriptions
val Category.Companion.Rent get() = CategoryPresets.Rent
val Category.Companion.Groceries get() = CategoryPresets.Groceries
val Category.Companion.Insurance get() = CategoryPresets.Insurance
val Category.Companion.DiningOut get() = CategoryPresets.DiningOut
val Category.Companion.Utilities get() = CategoryPresets.Utilities
val Category.Companion.Internet get() = CategoryPresets.Internet
val Category.Companion.Airtime get() = CategoryPresets.Airtime
val Category.Companion.Bank get() = CategoryPresets.Bank
val Category.Companion.Loans get() = CategoryPresets.Loans
val Category.Companion.Charity get() = CategoryPresets.Charity
val Category.Companion.Government get() = CategoryPresets.Government
val Category.Companion.Savings get() = CategoryPresets.Savings
val Category.Companion.Transfer get() = CategoryPresets.Transfer
val Category.Companion.Pets get() = CategoryPresets.Pets
val Category.Companion.Fitness get() = CategoryPresets.Fitness
val Category.Companion.Maintenance get() = CategoryPresets.Maintenance
val Category.Companion.TransactionCost get() = CategoryPresets.TransactionCost
val Category.Companion.UncategorizedExpense get() = CategoryPresets.UncategorizedExpense
val Category.Companion.MiscExpense get() = CategoryPresets.MiscExpense

val Category.Companion.Salary get() = CategoryPresets.Salary
val Category.Companion.Freelance get() = CategoryPresets.Freelance
val Category.Companion.Investments get() = CategoryPresets.Investments
val Category.Companion.GiftsIncome get() = CategoryPresets.GiftsIncome
val Category.Companion.Bonus get() = CategoryPresets.Bonus
val Category.Companion.Rental get() = CategoryPresets.Rental
val Category.Companion.Dividends get() = CategoryPresets.Dividends
val Category.Companion.Interest get() = CategoryPresets.Interest
val Category.Companion.LoansIncome get() = CategoryPresets.LoansIncome
val Category.Companion.TransferIncome get() = CategoryPresets.TransferIncome
val Category.Companion.SavingsIncome get() = CategoryPresets.SavingsIncome
val Category.Companion.OtherIncome get() = CategoryPresets.OtherIncome
val Category.Companion.UncategorizedIncome get() = CategoryPresets.UncategorizedIncome

val Category.Companion.allCategories get() = CategoryPresets.allCategories
val Category.Companion.expenseCategories get() = allCategories.filter { it.isExpense }
val Category.Companion.incomeCategories get() = allCategories.filter { !it.isExpense }

fun Category.Companion.fromName(name: String, isExpense: Boolean): Category =
    allCategories.find { it.name == name && it.isExpense == isExpense }
        ?: Category(id = "custom_$name", name = name, isExpense = isExpense)

fun Category.Companion.fromId(
    id: String,
    name: String? = null,
    isExpense: Boolean = true,
    knownCategories: List<Category> = emptyList()
): Category =
    allCategories.find { it.id == id }
        ?: knownCategories.find { it.id == id }
        ?: Category(id = id, name = name ?: "Unknown", isExpense = isExpense)
