package com.fintrack.shared.feature.planning.domain.calculator

import kotlin.math.pow
import kotlin.math.round

object CalculatorEngine {

    fun calculateMmfYield(
        initialDeposit: Double,
        monthlyContribution: Double,
        annualRatePercentage: Double,
        durationYears: Int,
        taxRatePercentage: Double = 15.0
    ): MmfCalculatorResult {
        val rate = (annualRatePercentage / 100.0).coerceAtLeast(0.0)
        val taxRate = (taxRatePercentage / 100.0).coerceAtLeast(0.0)
        val netRate = rate * (1.0 - taxRate)
        val monthlyRate = netRate / 12.0
        val totalMonths = durationYears * 12

        var balance = initialDeposit
        var totalInvested = initialDeposit

        repeat(totalMonths) {
            balance += monthlyContribution
            totalInvested += monthlyContribution
            balance *= (1.0 + monthlyRate)
        }

        val netInterest = (balance - totalInvested).coerceAtLeast(0.0)
        val grossInterest = if (taxRate < 1.0) netInterest / (1.0 - taxRate) else netInterest
        val withholdingTax = grossInterest - netInterest
        val monthlyYieldNet = balance * (netRate / 12.0)

        return MmfCalculatorResult(
            initialDeposit = initialDeposit,
            monthlyContribution = monthlyContribution,
            annualRatePercentage = annualRatePercentage,
            durationYears = durationYears,
            totalInvested = totalInvested,
            grossInterestEarned = grossInterest,
            withholdingTax = withholdingTax,
            netInterestEarned = netInterest,
            finalMmfBalance = balance,
            monthlyPassiveYieldNet = monthlyYieldNet
        )
    }

    fun calculateSavingsRateFreedom(
        monthlyIncome: Double,
        monthlyExpenses: Double,
        annualInvestmentReturnRate: Double = 8.0
    ): SavingsRateFreedomResult {
        val safeIncome = monthlyIncome.coerceAtLeast(1.0)
        val savings = (safeIncome - monthlyExpenses).coerceAtLeast(0.0)
        val ratePercentage = round((savings / safeIncome) * 100.0 * 10.0) / 10.0
        val annualExpenses = monthlyExpenses * 12.0

        val fireTarget = calculateFireTarget(
            annualExpenses = annualExpenses,
            currentInvestments = 0.0,
            monthlySavings = savings,
            annualInvestmentReturn = annualInvestmentReturnRate
        )

        return SavingsRateFreedomResult(
            monthlyIncome = safeIncome,
            monthlyExpenses = monthlyExpenses,
            monthlySavings = savings,
            savingsRatePercentage = ratePercentage,
            targetFireNumber = fireTarget.targetFireNumber,
            estimatedYearsToFreedom = fireTarget.estimatedYearsToFire
        )
    }

    fun calculateCurrencyHedge(
        initialKesAmount: Double,
        initialExchangeRateUsdKes: Double = 130.0,
        years: Int = 3,
        annualKesDevaluationRate: Double = 5.0,
        usdYieldRate: Double = 5.0,
        localKesYieldRate: Double = 11.5
    ): CurrencyHedgeResult {
        val safeYears = years.coerceAtLeast(1)
        val usdAmountInitial = if (initialExchangeRateUsdKes > 0) initialKesAmount / initialExchangeRateUsdKes else 0.0

        val localRate = localKesYieldRate / 100.0
        var localVal = initialKesAmount
        repeat(safeYears) {
            localVal *= (1.0 + localRate)
        }

        val usdRate = usdYieldRate / 100.0
        var usdVal = usdAmountInitial
        repeat(safeYears) {
            usdVal *= (1.0 + usdRate)
        }

        val devalRate = annualKesDevaluationRate / 100.0
        var futureFx = initialExchangeRateUsdKes
        repeat(safeYears) {
            futureFx *= (1.0 + devalRate)
        }

        val usdHedgedValInKes = usdVal * futureFx
        val diff = kotlin.math.abs(usdHedgedValInKes - localVal)

        return CurrencyHedgeResult(
            initialKesAmount = initialKesAmount,
            initialExchangeRateUsdKes = initialExchangeRateUsdKes,
            initialUsdAmount = usdAmountInitial,
            years = safeYears,
            localKesOutcome = localVal,
            futureExchangeRateUsdKes = futureFx,
            usdHedgedOutcomeInKes = usdHedgedValInKes,
            hedgingShieldDifferenceKes = diff,
            isUsdHedgeSuperior = usdHedgedValInKes >= localVal
        )
    }

    fun calculateCoastFire(
        currentAge: Int,
        targetRetirementAge: Int,
        currentInvestments: Double,
        annualLivingExpenses: Double,
        expectedReturnRate: Double = 8.0
    ): CoastFireResult {
        val yearsToRetire = (targetRetirementAge - currentAge).coerceAtLeast(1)
        val fullFireTarget = annualLivingExpenses * 25.0
        val rate = (expectedReturnRate / 100.0).coerceAtLeast(0.0)

        val discountFactor = (1.0 + rate).pow(yearsToRetire.toDouble())
        val coastFireTarget = if (discountFactor > 0) fullFireTarget / discountFactor else fullFireTarget

        var projected = currentInvestments
        repeat(yearsToRetire) {
            projected *= (1.0 + rate)
        }

        val isReached = currentInvestments >= coastFireTarget
        val deficit = (coastFireTarget - currentInvestments).coerceAtLeast(0.0)
        val monthlySavingsNeeded = if (!isReached && yearsToRetire > 0) deficit / (yearsToRetire * 12.0) else 0.0

        return CoastFireResult(
            currentAge = currentAge,
            targetRetirementAge = targetRetirementAge,
            currentInvestments = currentInvestments,
            annualLivingExpenses = annualLivingExpenses,
            expectedReturnRate = expectedReturnRate,
            fullFireTargetNumber = fullFireTarget,
            coastFireTargetNumberToday = coastFireTarget,
            projectedBalanceAtRetirement = projected,
            isCoastFireReached = isReached,
            monthlySavingsNeededToCoast = monthlySavingsNeeded
        )
    }

    fun calculateMortgageSaver(
        mortgagePrincipal: Double,
        annualInterestRate: Double,
        tenureYears: Int,
        extraMonthlyPrincipal: Double
    ): MortgageSaverResult {
        val safeYears = tenureYears.coerceAtLeast(1)
        val origMonths = safeYears * 12
        val monthlyRate = (annualInterestRate / 100.0) / 12.0

        val basePayment = if (monthlyRate > 0) {
            val factor = (1.0 + monthlyRate).pow(origMonths.toDouble())
            mortgagePrincipal * (monthlyRate * factor) / (factor - 1.0)
        } else {
            mortgagePrincipal / origMonths
        }

        val origInterest = (basePayment * origMonths - mortgagePrincipal).coerceAtLeast(0.0)

        val accelPayment = basePayment + extraMonthlyPrincipal
        val accelMonths = simulateDebtPayoffMonths(mortgagePrincipal, monthlyRate, accelPayment)
        val accelInterest = simulateDebtInterest(mortgagePrincipal, monthlyRate, accelPayment)

        val monthsSaved = (origMonths - accelMonths).coerceAtLeast(0)
        val interestSaved = (origInterest - accelInterest).coerceAtLeast(0.0)

        return MortgageSaverResult(
            mortgagePrincipal = mortgagePrincipal,
            annualInterestRate = annualInterestRate,
            tenureYears = safeYears,
            monthlyBasePayment = basePayment,
            extraMonthlyPrincipal = extraMonthlyPrincipal,
            originalTenureMonths = origMonths,
            acceleratedTenureMonths = accelMonths,
            monthsSaved = monthsSaved,
            totalInterestOriginal = origInterest,
            totalInterestAccelerated = accelInterest,
            totalInterestSaved = interestSaved
        )
    }

    fun calculateEducationFund(
        childCurrentAge: Int,
        collegeStartAge: Int = 18,
        currentAnnualTuition: Double = 300000.0,
        educationInflationRate: Double = 8.0,
        investmentReturnRate: Double = 11.5
    ): EducationFundResult {
        val years = (collegeStartAge - childCurrentAge).coerceAtLeast(1)
        val inflation = educationInflationRate / 100.0

        var futureAnnualTuition = currentAnnualTuition
        repeat(years) {
            futureAnnualTuition *= (1.0 + inflation)
        }

        val total4YrCost = futureAnnualTuition * 4.0
        val monthlyRate = (investmentReturnRate / 100.0) / 12.0
        val months = years * 12

        val reqMonthlyDeposit = if (monthlyRate > 0) {
            val factor = (1.0 + monthlyRate).pow(months.toDouble())
            total4YrCost * monthlyRate / (factor - 1.0)
        } else {
            total4YrCost / months
        }

        return EducationFundResult(
            childCurrentAge = childCurrentAge,
            collegeStartAge = collegeStartAge,
            currentAnnualTuition = currentAnnualTuition,
            yearsUntilCollege = years,
            total4YearFutureTuitionCost = total4YrCost,
            requiredMonthlyMmfDeposit = reqMonthlyDeposit
        )
    }

    fun calculateTipAndSplit(
        billAmount: Double,
        tipPercentage: Double,
        numberOfPeople: Int
    ): TipSplitterResult {
        val safePeople = numberOfPeople.coerceAtLeast(1)
        val tip = billAmount * (tipPercentage / 100.0)
        val total = billAmount + tip
        val share = total / safePeople

        return TipSplitterResult(
            billAmount = billAmount,
            tipPercentage = tipPercentage,
            numberOfPeople = safePeople,
            tipAmount = tip,
            totalBillWithTip = total,
            perPersonShare = share
        )
    }

    fun calculateLoanAmortization(
        loanPrincipal: Double,
        annualInterestRate: Double,
        tenureMonths: Int
    ): LoanAmortizationResult {
        val safeTenure = tenureMonths.coerceAtLeast(1)
        val monthlyRate = (annualInterestRate / 100.0) / 12.0

        val monthlyPayment = if (monthlyRate > 0) {
            val factor = (1.0 + monthlyRate).pow(safeTenure.toDouble())
            loanPrincipal * (monthlyRate * factor) / (factor - 1.0)
        } else {
            loanPrincipal / safeTenure
        }

        val totalRepaid = monthlyPayment * safeTenure
        val totalInterest = (totalRepaid - loanPrincipal).coerceAtLeast(0.0)

        return LoanAmortizationResult(
            loanPrincipal = loanPrincipal,
            annualInterestRate = annualInterestRate,
            tenureMonths = safeTenure,
            monthlyPayment = monthlyPayment,
            totalInterestPaid = totalInterest,
            totalAmountRepaid = totalRepaid
        )
    }

    fun calculateCarOwnership(
        purchasePrice: Double,
        monthlyFuelAndMaintenance: Double,
        annualInsurance: Double,
        ownershipYears: Int = 3,
        annualDepreciationRate: Double = 15.0
    ): CarOwnershipResult {
        val safeYears = ownershipYears.coerceAtLeast(1)
        val months = safeYears * 12

        var resaleValue = purchasePrice
        val depRate = (annualDepreciationRate / 100.0).coerceIn(0.0, 0.9)
        repeat(safeYears) {
            resaleValue *= (1.0 - depRate)
        }

        val totalDepreciationLoss = purchasePrice - resaleValue
        val totalFuelMaintCost = monthlyFuelAndMaintenance * months
        val totalInsuranceCost = annualInsurance * safeYears
        val totalRunningCost = totalFuelMaintCost + totalInsuranceCost
        val totalCostOfOwnership = totalDepreciationLoss + totalRunningCost
        val effectiveMonthlyCost = totalCostOfOwnership / months

        return CarOwnershipResult(
            purchasePrice = purchasePrice,
            monthlyFuelAndMaintenance = monthlyFuelAndMaintenance,
            annualInsurance = annualInsurance,
            ownershipYears = safeYears,
            totalDepreciationLoss = totalDepreciationLoss,
            estimatedResaleValue = resaleValue,
            totalRunningCost = totalRunningCost,
            totalCostOfOwnership = totalCostOfOwnership,
            effectiveMonthlyCost = effectiveMonthlyCost
        )
    }

    fun calculateDebtAccelerator(
        debtPrincipal: Double,
        annualRatePercentage: Double,
        minimumPayment: Double,
        extraMonthlyBoost: Double
    ): DebtAcceleratorResult {
        val monthlyRate = (annualRatePercentage / 100.0) / 12.0

        val monthsNoBoost = simulateDebtPayoffMonths(debtPrincipal, monthlyRate, minimumPayment)
        val interestNoBoost = simulateDebtInterest(debtPrincipal, monthlyRate, minimumPayment)

        val boostedPayment = minimumPayment + extraMonthlyBoost
        val monthsBoosted = simulateDebtPayoffMonths(debtPrincipal, monthlyRate, boostedPayment)
        val interestBoosted = simulateDebtInterest(debtPrincipal, monthlyRate, boostedPayment)

        val monthsSaved = (monthsNoBoost - monthsBoosted).coerceAtLeast(0)
        val interestSaved = (interestNoBoost - interestBoosted).coerceAtLeast(0.0)

        return DebtAcceleratorResult(
            debtPrincipal = debtPrincipal,
            annualRatePercentage = annualRatePercentage,
            minimumPayment = minimumPayment,
            extraMonthlyBoost = extraMonthlyBoost,
            monthsWithoutBoost = monthsNoBoost,
            monthsWithBoost = monthsBoosted,
            monthsSaved = monthsSaved,
            totalInterestWithoutBoost = interestNoBoost,
            totalInterestWithBoost = interestBoosted,
            totalInterestSaved = interestSaved
        )
    }

    private fun simulateDebtPayoffMonths(principal: Double, monthlyRate: Double, payment: Double): Int {
        if (payment <= 0.0) return 360
        var balance = principal
        var months = 0
        while (balance > 0.0 && months < 360) {
            val interest = balance * monthlyRate
            balance = (balance + interest - payment)
            months++
            if (balance <= 0.0) break
        }
        return months
    }

    private fun simulateDebtInterest(principal: Double, monthlyRate: Double, payment: Double): Double {
        if (payment <= 0.0) return 0.0
        var balance = principal
        var totalInterest = 0.0
        var months = 0
        while (balance > 0.0 && months < 360) {
            val interest = balance * monthlyRate
            totalInterest += interest
            balance = (balance + interest - payment)
            months++
            if (balance <= 0.0) break
        }
        return totalInterest
    }

    fun calculateBuyVsRent(
        propertyPrice: Double,
        monthlyRent: Double,
        ownershipYears: Int = 10,
        propertyAppreciationRate: Double = 5.0,
        mmfInvestmentRate: Double = 11.5
    ): BuyVsRentResult {
        val safeYears = ownershipYears.coerceAtLeast(1)

        var propertyFutureVal = propertyPrice
        val appRate = propertyAppreciationRate / 100.0
        repeat(safeYears) {
            propertyFutureVal *= (1.0 + appRate)
        }

        val totalRentPaid = monthlyRent * 12.0 * safeYears
        val mmfResult = calculateMmfYield(
            initialDeposit = propertyPrice * 0.2,
            monthlyContribution = ((propertyPrice * 0.01) - monthlyRent).coerceAtLeast(0.0),
            annualRatePercentage = mmfInvestmentRate,
            durationYears = safeYears
        )

        val buyNetWorth = propertyFutureVal
        val rentAndInvestNetWorth = mmfResult.finalMmfBalance
        val diff = kotlin.math.abs(buyNetWorth - rentAndInvestNetWorth)

        return BuyVsRentResult(
            propertyPrice = propertyPrice,
            monthlyRent = monthlyRent,
            ownershipYears = safeYears,
            propertyValueFuture = propertyFutureVal,
            totalRentPaid = totalRentPaid,
            MmfPortfolioValue = mmfResult.finalMmfBalance,
            buyNetWorthOutcome = buyNetWorth,
            rentAndInvestNetWorthOutcome = rentAndInvestNetWorth,
            netWorthDifference = diff,
            isBuyingFavorable = buyNetWorth >= rentAndInvestNetWorth
        )
    }

    fun calculateEmergencyFund(
        monthlyExpense: Double,
        cushionMonths: Int
    ): EmergencyFundResult {
        val safeMonths = cushionMonths.coerceAtLeast(1)
        val target = monthlyExpense * safeMonths

        return EmergencyFundResult(
            monthlyExpense = monthlyExpense,
            cushionMonths = safeMonths,
            targetFundRequired = target
        )
    }

    fun calculateFireTarget(
        annualExpenses: Double,
        currentInvestments: Double,
        monthlySavings: Double,
        annualInvestmentReturn: Double = 8.0
    ): FireCalculatorResult {
        val targetFireNumber = annualExpenses * 25.0
        val remainingNeeded = (targetFireNumber - currentInvestments).coerceAtLeast(0.0)

        val estimatedYears: Double? = if (remainingNeeded == 0.0) {
            0.0
        } else if (monthlySavings > 0) {
            val annualSavings = monthlySavings * 12.0
            if (annualInvestmentReturn > 0) {
                val rate = annualInvestmentReturn / 100.0
                val years = round((remainingNeeded / (annualSavings * (1 + rate))) * 10.0) / 10.0
                years.coerceAtLeast(0.1)
            } else {
                round((remainingNeeded / annualSavings) * 10.0) / 10.0
            }
        } else {
            null
        }

        return FireCalculatorResult(
            annualExpenses = annualExpenses,
            currentInvestments = currentInvestments,
            monthlySavings = monthlySavings,
            targetFireNumber = targetFireNumber,
            remainingAmountNeeded = remainingNeeded,
            estimatedYearsToFire = estimatedYears
        )
    }
}
