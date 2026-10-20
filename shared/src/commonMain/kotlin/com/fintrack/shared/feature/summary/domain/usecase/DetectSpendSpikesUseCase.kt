package com.fintrack.shared.feature.summary.domain.usecase

import com.fintrack.shared.feature.core.util.DateTimeHelper
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.summary.domain.model.SpendSpikeAnomaly
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.fintrack.shared.feature.transaction.domain.repository.TransactionRepository
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.DecimalMode
import com.ionspin.kotlin.bignum.decimal.RoundingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

class DetectSpendSpikesUseCase(
    private val transactionRepository: TransactionRepository
) {
    private val decimalMode = DecimalMode(decimalPrecision = 20, roundingMode = RoundingMode.ROUND_HALF_CEILING)

    suspend operator fun invoke(
        accountId: String? = null,
        thresholdPercentage: Double = 15.0
    ): Result<List<SpendSpikeAnomaly>> = withContext(Dispatchers.IO) {
        try {
            val nowInstant = DateTimeHelper.now()
            val today = nowInstant.toLocalDateTime(TimeZone.currentSystemDefault()).date
            
            val thisMonthStart = "${today.year}-${today.monthNumber.toString().padStart(2, '0')}-01"
            val todayStr = today.toString()
            val ninetyDaysAgoStr = today.minus(DatePeriod(days = 90)).toString()

            val singleAccountId = if (!accountId.isNullOrBlank() && !accountId.contains(",")) accountId else null
            val txResult = transactionRepository.getAllTransactions(startDate = ninetyDaysAgoStr, endDate = todayStr, accountId = singleAccountId)
            val allTx = (txResult as? Result.Success)?.data ?: emptyList()

            val targetAccountIds = accountId?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
            val filteredTx = if (targetAccountIds.size > 1) {
                allTx.filter { targetAccountIds.contains(it.accountId) }
            } else allTx

            // 1. Current Month Expenses by Category
            val currentMonthExpenses = filteredTx.filter { tx ->
                if (tx.isIncome) return@filter false
                val txDate = try { tx.dateTime.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString() } catch (_: Exception) { "" }
                txDate >= thisMonthStart && txDate <= todayStr
            }

            val currentMonthTotals = currentMonthExpenses.groupBy { it.categoryId to (it.category ?: "Uncategorized") }
                .mapValues { entry -> entry.value.fold(BigDecimal.ZERO) { sum, tx -> sum + tx.amount } }

            // 2. Past 3 Months Baseline Expenses by Category
            val pastExpenses = filteredTx.filter { tx ->
                if (tx.isIncome) return@filter false
                val txDate = try { tx.dateTime.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString() } catch (_: Exception) { "" }
                txDate < thisMonthStart
            }

            val pastCategoryTotals = pastExpenses.groupBy { it.categoryId to (it.category ?: "Uncategorized") }
                .mapValues { entry ->
                    val total = entry.value.fold(BigDecimal.ZERO) { sum, tx -> sum + tx.amount }
                    total.divide(BigDecimal.fromInt(3), decimalMode)
                }

            // 3. Detect Anomalies
            val anomalies = mutableListOf<SpendSpikeAnomaly>()

            currentMonthTotals.forEach { (catPair, currentAmt) ->
                val (catId, catName) = catPair
                val baseline = pastCategoryTotals[catPair] ?: BigDecimal.ZERO

                if (baseline > BigDecimal.ZERO && currentAmt > baseline) {
                    val diff = currentAmt - baseline
                    val spikePct = diff.divide(baseline, decimalMode).multiply(BigDecimal.fromInt(100))

                    if (spikePct.doubleValue(false) >= thresholdPercentage) {
                        val currentFormatted = "KES ${currentAmt.longValue(false)}"
                        val baselineFormatted = "KES ${baseline.longValue(false)}"
                        val alertMsg = "⚡ $catName is +${spikePct.intValue(false)}% higher than your 3-month average ($currentFormatted vs $baselineFormatted baseline)."

                        anomalies.add(
                            SpendSpikeAnomaly(
                                categoryName = catName,
                                categoryId = catId,
                                currentMonthAmount = currentAmt,
                                historical3MonthAvgAmount = baseline,
                                spikePercentage = spikePct,
                                alertMessage = alertMsg
                            )
                        )
                    }
                } else if (baseline <= BigDecimal.ZERO && currentAmt >= BigDecimal.fromInt(3000)) {
                    val currentFormatted = "KES ${currentAmt.longValue(false)}"
                    val alertMsg = "⚡ $catName: High new category spend of $currentFormatted detected this month."

                    anomalies.add(
                        SpendSpikeAnomaly(
                            categoryName = catName,
                            categoryId = catId,
                            currentMonthAmount = currentAmt,
                            historical3MonthAvgAmount = BigDecimal.ZERO,
                            spikePercentage = BigDecimal.fromInt(100),
                            alertMessage = alertMsg
                        )
                    )
                }
            }

            Result.Success(anomalies.sortedByDescending { it.spikePercentage.doubleValue(false) })
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
