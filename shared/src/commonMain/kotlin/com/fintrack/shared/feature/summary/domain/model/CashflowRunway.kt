package com.fintrack.shared.feature.summary.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal

data class CashflowRunway(
    val currentBalance: BigDecimal,
    val dailyBurnRate: BigDecimal,
    val remainingDaysInMonth: Int,
    val projectedVariableExpense: BigDecimal,
    val upcomingBillsTotal: BigDecimal,
    val projectedEomBalance: BigDecimal,
    val isOverdraftRisk: Boolean,
    val estimatedDepletionDays: Int?
)
