package com.fintrack.shared.feature.summary.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PillarScore(
    val name: String = "",
    val score: Int = 0,
    val maxScore: Int = 20,
    val detail: String = ""
)

@Serializable
data class FinancialHealthIndex(
    val overallScore: Int = 0,
    val grade: String = "",
    val liquidityPillar: PillarScore = PillarScore("Liquidity"),
    val solvencyPillar: PillarScore = PillarScore("Solvency"),
    val savingsRatePillar: PillarScore = PillarScore("Savings Rate"),
    val emergencyShieldPillar: PillarScore = PillarScore("Emergency Shield"),
    val budgetAlignmentPillar: PillarScore = PillarScore("Budget Alignment"),
    val actionableTips: List<String> = emptyList()
)
