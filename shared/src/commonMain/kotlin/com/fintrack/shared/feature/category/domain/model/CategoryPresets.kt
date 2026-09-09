package com.fintrack.shared.feature.category.domain.model

import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
internal object CategoryPresets {
    // Expense categories (Base: 00000000-0000-4000-a000-)
    val Food = Category("00000000-0000-4000-a000-000000000001", "Food", true, "Fastfood", true)
    val Transport = Category("00000000-0000-4000-a000-000000000002", "Transport", true, "DirectionsCar", true)
    val Shopping = Category("00000000-0000-4000-a000-000000000003", "Shopping", true, "ShoppingCart", true)
    val Health = Category("00000000-0000-4000-a000-000000000004", "Health", true, "LocalHospital", true)
    val Bills = Category("00000000-0000-4000-a000-000000000005", "Bills", true, "Receipt", true)
    val Entertainment = Category("00000000-0000-4000-a000-000000000006", "Entertainment", true, "Movie", true)
    val Education = Category("00000000-0000-4000-a000-000000000007", "Education", true, "School", true)
    val GiftsExpense = Category("00000000-0000-4000-a000-000000000008", "Gifts", true, "CardGiftcard", true)
    val Travel = Category("00000000-0000-4000-a000-000000000009", "Travel", true, "Flight", true)
    val PersonalCare = Category("00000000-0000-4000-a000-000000000010", "Personal Care", true, "ContentCut", true)
    val Subscriptions = Category("00000000-0000-4000-a000-000000000011", "Subscriptions", true, "Subscriptions", true)
    val Rent = Category("00000000-0000-4000-a000-000000000012", "Rent", true, "Home", true)
    val Groceries = Category("00000000-0000-4000-a000-000000000013", "Groceries", true, "ShoppingBag", true)
    val Insurance = Category("00000000-0000-4000-a000-000000000014", "Insurance", true, "Shield", true)
    val DiningOut = Category("00000000-0000-4000-a000-000000000015", "Dining Out", true, "Restaurant", true)
    val Utilities = Category("00000000-0000-4000-a000-000000000016", "Utilities", true, "Lightbulb", true)
    val Internet = Category("00000000-0000-4000-a000-000000000017", "Internet", true, "Wifi", true)
    val Airtime = Category("00000000-0000-4000-a000-000000000018", "Airtime", true, "Smartphone", true)
    val Bank = Category("00000000-0000-4000-a000-000000000019", "Bank", true, "AccountBalance", true)
    val Loans = Category("00000000-0000-4000-a000-000000000020", "Loans", true, "AccountBalanceWallet", true)
    val Charity = Category("00000000-0000-4000-a000-000000000021", "Charity", true, "VolunteerActivism", true)
    val Government = Category("00000000-0000-4000-a000-000000000022", "Government", true, "AccountBalance", true)
    val Savings = Category("00000000-0000-4000-a000-000000000023", "Savings", true, "Savings", true)
    val Transfer = Category("00000000-0000-4000-a000-000000000024", "Transfer", true, "Sync", true)
    val Pets = Category("00000000-0000-4000-a000-000000000025", "Pets", true, "Pets", true)
    val Fitness = Category("00000000-0000-4000-a000-000000000026", "Fitness", true, "FitnessCenter", true)
    val Maintenance = Category("00000000-0000-4000-a000-000000000027", "Maintenance", true, "Build", true)
    val TransactionCost = Category("00000000-0000-4000-a000-000000000028", "Transaction Fees", true, "Receipt", true)
    val UncategorizedExpense = Category("pending", "Uncategorized", true, "HelpOutline", true)
    val MiscExpense = Category("99999999-9999-4999-a999-999999999999", "Misc", true, "HelpOutline", true)

    // Income categories (Base: aaaaaaaa-aaaa-4aaa-baaa-)
    val Salary = Category("aaaaaaaa-aaaa-4aaa-baaa-000000000001", "Salary", false, "AttachMoney", true)
    val Freelance = Category("aaaaaaaa-aaaa-4aaa-baaa-000000000002", "Freelance", false, "Work", true)
    val Investments = Category("aaaaaaaa-aaaa-4aaa-baaa-000000000003", "Investments", false, "TrendingUp", true)
    val GiftsIncome = Category("aaaaaaaa-aaaa-4aaa-baaa-000000000004", "Gifts", false, "CardGiftcard", true)
    val Bonus = Category("aaaaaaaa-aaaa-4aaa-baaa-000000000005", "Bonus", false, "Paid", true)
    val Rental = Category("aaaaaaaa-aaaa-4aaa-baaa-000000000006", "Rental", false, "RealEstateAgent", true)
    val Dividends = Category("aaaaaaaa-aaaa-4aaa-baaa-000000000007", "Dividends", false, "Analytics", true)
    val Interest = Category("aaaaaaaa-aaaa-4aaa-baaa-000000000008", "Interest", false, "Percent", true)
    val LoansIncome = Category("aaaaaaaa-aaaa-4aaa-baaa-000000000009", "Loans", false, "AccountBalanceWallet", true)
    val TransferIncome = Category("aaaaaaaa-aaaa-4aaa-baaa-000000000010", "Transfer", false, "Sync", true)
    val SavingsIncome = Category("aaaaaaaa-aaaa-4aaa-baaa-000000000011", "Savings", false, "Savings", true)
    val OtherIncome = Category("bbbbbbbb-bbbb-4bbb-bbbb-bbbbbbbbbbbb", "Other Income", false, "AttachMoney", true)
    val UncategorizedIncome = Category("pending_income", "Uncategorized", false, "HelpOutline", true)

    val allCategories = listOf(
        Food, Transport, Shopping, Health, Bills,
        Entertainment, Education, GiftsExpense, Travel,
        PersonalCare, Subscriptions, Rent, Groceries,
        Insurance, DiningOut, Utilities, Internet, Airtime, Bank, Loans, Charity, Government, Savings, Transfer,
        Pets, Fitness, Maintenance, TransactionCost, UncategorizedExpense, MiscExpense,
        Salary, Freelance, Investments, GiftsIncome, Bonus, Rental, Dividends, Interest,
        LoansIncome, TransferIncome, SavingsIncome, OtherIncome, UncategorizedIncome
    )
}
