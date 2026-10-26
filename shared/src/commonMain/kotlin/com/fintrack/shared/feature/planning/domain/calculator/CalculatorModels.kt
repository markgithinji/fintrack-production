package com.fintrack.shared.feature.planning.domain.calculator

enum class FinancialCalculatorType {
    MMF_COMPOUND,
    SAVINGS_RATE_FREEDOM,
    DEBT_ACCELERATOR,
    BUY_VS_RENT,
    COAST_FIRE,
    MORTGAGE_SAVER,
    EDUCATION_FUND,
    CAR_OWNERSHIP,
    TIP_SPLITTER,
    LOAN_AMORTIZATION,
    EMERGENCY_FUND,
    FIRE_RETIREMENT
}

enum class DebtStrategyType {
    SNOWBALL_BALANCE,
    AVALANCHE_INTEREST
}

data class MmfCalculatorResult(
    val initialDeposit: Double,
    val monthlyContribution: Double,
    val annualRatePercentage: Double,
    val durationYears: Int,
    val totalInvested: Double,
    val grossInterestEarned: Double,
    val withholdingTax: Double,
    val netInterestEarned: Double,
    val finalMmfBalance: Double,
    val monthlyPassiveYieldNet: Double
)

data class SavingsRateFreedomResult(
    val monthlyIncome: Double,
    val monthlyExpenses: Double,
    val monthlySavings: Double,
    val savingsRatePercentage: Double,
    val targetFireNumber: Double,
    val estimatedYearsToFreedom: Double?
)

data class CoastFireResult(
    val currentAge: Int,
    val targetRetirementAge: Int,
    val currentInvestments: Double,
    val annualLivingExpenses: Double,
    val expectedReturnRate: Double,
    val fullFireTargetNumber: Double,
    val coastFireTargetNumberToday: Double,
    val projectedBalanceAtRetirement: Double,
    val isCoastFireReached: Boolean,
    val monthlySavingsNeededToCoast: Double
)

data class MortgageSaverResult(
    val mortgagePrincipal: Double,
    val annualInterestRate: Double,
    val tenureYears: Int,
    val monthlyBasePayment: Double,
    val extraMonthlyPrincipal: Double,
    val originalTenureMonths: Int,
    val acceleratedTenureMonths: Int,
    val monthsSaved: Int,
    val totalInterestOriginal: Double,
    val totalInterestAccelerated: Double,
    val totalInterestSaved: Double
)

data class EducationFundResult(
    val childCurrentAge: Int,
    val collegeStartAge: Int,
    val currentAnnualTuition: Double,
    val yearsUntilCollege: Int,
    val total4YearFutureTuitionCost: Double,
    val requiredMonthlyMmfDeposit: Double
)

data class TipSplitterResult(
    val billAmount: Double,
    val tipPercentage: Double,
    val numberOfPeople: Int,
    val tipAmount: Double,
    val totalBillWithTip: Double,
    val perPersonShare: Double
)

data class LoanAmortizationResult(
    val loanPrincipal: Double,
    val annualInterestRate: Double,
    val tenureMonths: Int,
    val monthlyPayment: Double,
    val totalInterestPaid: Double,
    val totalAmountRepaid: Double
)

data class CarOwnershipResult(
    val purchasePrice: Double,
    val monthlyFuelAndMaintenance: Double,
    val annualInsurance: Double,
    val ownershipYears: Int,
    val totalDepreciationLoss: Double,
    val estimatedResaleValue: Double,
    val totalRunningCost: Double,
    val totalCostOfOwnership: Double,
    val effectiveMonthlyCost: Double
)

data class DebtAcceleratorResult(
    val debtPrincipal: Double,
    val annualRatePercentage: Double,
    val minimumPayment: Double,
    val extraMonthlyBoost: Double,
    val strategy: DebtStrategyType = DebtStrategyType.AVALANCHE_INTEREST,
    val monthsWithoutBoost: Int,
    val monthsWithBoost: Int,
    val monthsSaved: Int,
    val totalInterestWithoutBoost: Double,
    val totalInterestWithBoost: Double,
    val totalInterestSaved: Double,
    val avalancheSavingsVsSnowball: Double = 0.0
)

data class BuyVsRentResult(
    val propertyPrice: Double,
    val monthlyRent: Double,
    val ownershipYears: Int,
    val propertyValueFuture: Double,
    val totalRentPaid: Double,
    val MmfPortfolioValue: Double,
    val buyNetWorthOutcome: Double,
    val rentAndInvestNetWorthOutcome: Double,
    val netWorthDifference: Double,
    val isBuyingFavorable: Boolean
)

data class EmergencyFundResult(
    val monthlyExpense: Double,
    val cushionMonths: Int,
    val targetFundRequired: Double
)

data class FireCalculatorResult(
    val annualExpenses: Double,
    val currentInvestments: Double,
    val monthlySavings: Double,
    val targetFireNumber: Double,
    val remainingAmountNeeded: Double,
    val estimatedYearsToFire: Double?
)
