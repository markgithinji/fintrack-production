package com.fintrack.shared.feature.planning.domain.calculator

data class FinancialCalculatorsState(
    val activeCalculator: FinancialCalculatorType = FinancialCalculatorType.MMF_COMPOUND,

    // MMF
    val mmfInitialDeposit: String = "100000",
    val mmfMonthlyContribution: String = "10000",
    val mmfAnnualRate: String = "11.5",
    val mmfYears: String = "3",

    // Savings Rate & Freedom
    val savingsRateIncome: String = "150000",
    val savingsRateExpense: String = "90000",

    // Debt Accelerator
    val debtPrincipal: String = "400000",
    val debtRate: String = "16.0",
    val debtMinPayment: String = "18000",
    val debtExtraBoost: String = "7000",
    val debtStrategy: DebtStrategyType = DebtStrategyType.AVALANCHE_INTEREST,

    // Buy vs Rent
    val buyVsRentPrice: String = "12000000",
    val buyVsRentRent: String = "60000",
    val buyVsRentYears: String = "10",

    // Coast FIRE
    val coastFireAge: String = "30",
    val coastFireRetireAge: String = "60",
    val coastFireCurrentInvestments: String = "2500000",
    val coastFireAnnualExpenses: String = "720000",

    // Mortgage Saver
    val mortgagePrincipal: String = "10000000",
    val mortgageRate: String = "14.0",
    val mortgageTenureYears: String = "20",
    val mortgageExtraPrincipal: String = "10000",

    // Education Fund
    val educationChildAge: String = "2",
    val educationAnnualTuition: String = "300000",

    // Car Ownership
    val carPrice: String = "2500000",
    val carRunningCost: String = "30000",
    val carInsurance: String = "100000",
    val carYears: String = "3",

    // Tip Splitter
    val tipBillAmount: String = "5000",
    val tipPercentage: String = "10",
    val tipPeopleSplit: String = "4",

    // FIRE Freedom
    val fireAnnualExpenses: String = "720000",
    val fireCurrentInvestments: String = "500000",
    val fireMonthlySavings: String = "30000"
)
