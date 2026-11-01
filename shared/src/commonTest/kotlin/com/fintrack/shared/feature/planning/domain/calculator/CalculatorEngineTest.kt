package com.fintrack.shared.feature.planning.domain.calculator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalculatorEngineTest {

    @Test
    fun `calculateMmfYield computes gross net interest and withholding tax correctly`() {
        val result = CalculatorEngine.calculateMmfYield(
            initialDeposit = 100000.0,
            monthlyContribution = 10000.0,
            annualRatePercentage = 11.5,
            durationYears = 3,
            taxRatePercentage = 15.0
        )

        assertEquals(100000.0, result.initialDeposit)
        assertEquals(10000.0, result.monthlyContribution)
        assertEquals(3, result.durationYears)
        assertEquals(460000.0, result.totalInvested)
        assertTrue(result.finalMmfBalance > result.totalInvested)
        assertTrue(result.netInterestEarned > 0)
        assertTrue(result.withholdingTax >= 0)
        assertTrue(result.monthlyPassiveYieldNet > 0)
    }

    @Test
    fun `calculateMmfYield handles zero initial deposit gracefully`() {
        val result = CalculatorEngine.calculateMmfYield(
            initialDeposit = 0.0,
            monthlyContribution = 5000.0,
            annualRatePercentage = 10.0,
            durationYears = 1
        )

        assertEquals(60000.0, result.totalInvested)
        assertTrue(result.finalMmfBalance > 60000.0)
    }

    @Test
    fun `calculateSavingsRateFreedom calculates FIRE target and savings rate correctly`() {
        val result = CalculatorEngine.calculateSavingsRateFreedom(
            monthlyIncome = 200000.0,
            monthlyExpenses = 80000.0,
            annualInvestmentReturnRate = 10.0
        )

        assertEquals(200000.0, result.monthlyIncome)
        assertEquals(80000.0, result.monthlyExpenses)
        assertEquals(120000.0, result.monthlySavings)
        assertEquals(60.0, result.savingsRatePercentage)
        assertEquals(24000000.0, result.targetFireNumber) // 960000 * 25
        assertTrue(result.estimatedYearsToFreedom != null && result.estimatedYearsToFreedom!! > 0)
    }

    @Test
    fun `calculateCoastFire handles reached status correctly`() {
        val result = CalculatorEngine.calculateCoastFire(
            currentAge = 30,
            targetRetirementAge = 60,
            currentInvestments = 5000000.0,
            annualLivingExpenses = 1200000.0,
            expectedReturnRate = 8.0
        )

        assertEquals(30, result.currentAge)
        assertEquals(60, result.targetRetirementAge)
        assertEquals(30000000.0, result.fullFireTargetNumber) // 1.2M * 25
        assertTrue(result.projectedBalanceAtRetirement > result.currentInvestments)
        assertTrue(result.isCoastFireReached)
        assertEquals(0.0, result.monthlySavingsNeededToCoast)
    }

    @Test
    fun `calculateMortgageSaver calculates acceleration savings correctly`() {
        val result = CalculatorEngine.calculateMortgageSaver(
            mortgagePrincipal = 5000000.0,
            annualInterestRate = 13.0,
            tenureYears = 15,
            extraMonthlyPrincipal = 20000.0
        )

        assertEquals(5000000.0, result.mortgagePrincipal)
        assertEquals(15, result.tenureYears)
        assertTrue(result.monthlyBasePayment > 0)
        assertTrue(result.acceleratedTenureMonths < result.originalTenureMonths)
        assertTrue(result.monthsSaved > 0)
        assertTrue(result.totalInterestSaved > 0)
    }

    @Test
    fun `calculateEducationFund calculates inflation and required deposit correctly`() {
        val result = CalculatorEngine.calculateEducationFund(
            childCurrentAge = 3,
            collegeStartAge = 18,
            currentAnnualTuition = 300000.0,
            educationInflationRate = 8.0,
            investmentReturnRate = 11.5
        )

        assertEquals(15, result.yearsUntilCollege)
        assertTrue(result.total4YearFutureTuitionCost > 1200000.0) // 300k * 4 * inflation
        assertTrue(result.requiredMonthlyMmfDeposit > 0)
    }

    @Test
    fun `calculateTipAndSplit calculates per person share and tip amount correctly`() {
        val result = CalculatorEngine.calculateTipAndSplit(
            billAmount = 10000.0,
            tipPercentage = 10.0,
            numberOfPeople = 4
        )

        assertEquals(10000.0, result.billAmount)
        assertEquals(1000.0, result.tipAmount)
        assertEquals(11000.0, result.totalBillWithTip)
        assertEquals(2750.0, result.perPersonShare)
    }

    @Test
    fun `calculateTipAndSplit handles 1 person edge case`() {
        val result = CalculatorEngine.calculateTipAndSplit(
            billAmount = 5000.0,
            tipPercentage = 15.0,
            numberOfPeople = 0 // edge case 0 people
        )

        assertEquals(1, result.numberOfPeople)
        assertEquals(5750.0, result.perPersonShare)
    }

    @Test
    fun `calculateCarOwnership calculates total cost and resale value correctly`() {
        val result = CalculatorEngine.calculateCarOwnership(
            purchasePrice = 2500000.0,
            monthlyFuelAndMaintenance = 25000.0,
            annualInsurance = 100000.0,
            ownershipYears = 3,
            annualDepreciationRate = 15.0
        )

        assertEquals(3, result.ownershipYears)
        assertTrue(result.estimatedResaleValue < 2500000.0)
        assertTrue(result.totalDepreciationLoss > 0)
        assertTrue(result.totalRunningCost > 0)
        assertTrue(result.totalCostOfOwnership > result.totalDepreciationLoss)
        assertTrue(result.effectiveMonthlyCost > 0)
    }

    @Test
    fun `calculateDebtAccelerator calculates interest and months saved correctly`() {
        val result = CalculatorEngine.calculateDebtAccelerator(
            debtPrincipal = 500000.0,
            annualRatePercentage = 18.0,
            minimumPayment = 15000.0,
            extraMonthlyBoost = 10000.0
        )

        assertEquals(500000.0, result.debtPrincipal)
        assertTrue(result.monthsWithBoost < result.monthsWithoutBoost)
        assertTrue(result.monthsSaved > 0)
        assertTrue(result.totalInterestSaved > 0)
    }

    @Test
    fun `calculateBuyVsRent determines buy vs rent outcome correctly`() {
        val result = CalculatorEngine.calculateBuyVsRent(
            propertyPrice = 10000000.0,
            monthlyRent = 50000.0,
            ownershipYears = 10,
            propertyAppreciationRate = 5.0,
            mmfInvestmentRate = 11.5
        )

        assertEquals(10, result.ownershipYears)
        assertTrue(result.propertyValueFuture > 10000000.0)
        assertEquals(6000000.0, result.totalRentPaid)
        assertTrue(result.netWorthDifference >= 0)
    }

    @Test
    fun `calculateFireTarget handles already reached FIRE number`() {
        val result = CalculatorEngine.calculateFireTarget(
            annualExpenses = 1000000.0,
            currentInvestments = 30000000.0, // 30M > 25M target
            monthlySavings = 50000.0
        )

        assertEquals(25000000.0, result.targetFireNumber)
        assertEquals(0.0, result.remainingAmountNeeded)
        assertEquals(0.0, result.estimatedYearsToFire)
    }
}
