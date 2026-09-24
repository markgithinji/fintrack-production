package com.fintrack.shared.feature.summary.ui.custom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.shared.feature.account.domain.repository.AccountRepository
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.model.fromId
import com.fintrack.shared.feature.category.domain.repository.CategoryRepository
import com.fintrack.shared.feature.core.util.DateTimeHelper
import com.fintrack.shared.feature.core.util.FileSaver
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.fintrack.shared.feature.transaction.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

class CustomAnalysisViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val fileSaver: FileSaver
) : ViewModel() {

    private val _state = MutableStateFlow(CustomAnalysisState())
    val state: StateFlow<CustomAnalysisState> = _state.asStateFlow()

    init {
        loadMetadata()
        applyDatePreset(DatePreset.THIS_MONTH)
    }

    private fun loadMetadata() {
        viewModelScope.launch {
            val accountsResult = accountRepository.getAccounts()
            val categoriesResult = categoryRepository.getCategories()

            _state.update { current ->
                current.copy(
                    allAccounts = (accountsResult as? Result.Success)?.data ?: emptyList(),
                    allCategories = (categoriesResult as? Result.Success)?.data ?: emptyList()
                )
            }
            runAnalysis()
        }
    }

    fun applyDatePreset(preset: DatePreset) {
        val today = DateTimeHelper.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        val (start, end) = when (preset) {
            DatePreset.THIS_MONTH -> {
                val startOfMonth = LocalDate(today.year, today.month, 1)
                startOfMonth.toString() to today.toString()
            }
            DatePreset.LAST_MONTH -> {
                val firstOfThisMonth = LocalDate(today.year, today.month, 1)
                val lastMonthEnd = firstOfThisMonth.minus(DatePeriod(days = 1))
                val lastMonthStart = LocalDate(lastMonthEnd.year, lastMonthEnd.month, 1)
                lastMonthStart.toString() to lastMonthEnd.toString()
            }
            DatePreset.LAST_90_DAYS -> {
                val ninetyDaysAgo = today.minus(DatePeriod(days = 90))
                ninetyDaysAgo.toString() to today.toString()
            }
            DatePreset.THIS_YEAR -> {
                val startOfYear = LocalDate(today.year, 1, 1)
                startOfYear.toString() to today.toString()
            }
            DatePreset.ALL_TIME -> null to null
            DatePreset.CUSTOM -> _state.value.startDate to _state.value.endDate
        }

        _state.update { it.copy(datePreset = preset, startDate = start, endDate = end) }
        runAnalysis()
    }

    fun setCustomDateRange(startDate: String?, endDate: String?) {
        _state.update {
            it.copy(
                datePreset = DatePreset.CUSTOM,
                startDate = startDate,
                endDate = endDate
            )
        }
        runAnalysis()
    }

    fun toggleAccountSelection(accountId: String) {
        _state.update { current ->
            val updated = current.selectedAccountIds.toMutableSet()
            if (updated.contains(accountId)) {
                updated.remove(accountId)
            } else {
                updated.add(accountId)
            }
            current.copy(selectedAccountIds = updated)
        }
        runAnalysis()
    }

    fun toggleCategorySelection(categoryId: String) {
        _state.update { current ->
            val updated = current.selectedCategoryIds.toMutableSet()
            if (updated.contains(categoryId)) {
                updated.remove(categoryId)
            } else {
                updated.add(categoryId)
            }
            current.copy(selectedCategoryIds = updated)
        }
        runAnalysis()
    }

    fun setTransactionTypeFilter(type: TransactionTypeFilter) {
        _state.update { it.copy(typeFilter = type) }
        runAnalysis()
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
        runAnalysis()
    }

    fun setAmountRange(min: Double?, max: Double?) {
        _state.update { it.copy(minAmount = min, maxAmount = max) }
        runAnalysis()
    }

    fun clearAllFilters() {
        val today = DateTimeHelper.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val startOfMonth = LocalDate(today.year, today.month, 1).toString()

        _state.update {
            it.copy(
                searchQuery = "",
                datePreset = DatePreset.THIS_MONTH,
                startDate = startOfMonth,
                endDate = today.toString(),
                selectedAccountIds = emptySet(),
                selectedCategoryIds = emptySet(),
                typeFilter = TransactionTypeFilter.ALL,
                minAmount = null,
                maxAmount = null
            )
        }
        runAnalysis()
    }

    private fun runAnalysis() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val currentState = _state.value
            // Fetch transactions across selected accounts or all accounts
            val rawTransactionsResult = transactionRepository.getAllTransactions(
                startDate = currentState.startDate,
                endDate = currentState.endDate,
                accountId = if (currentState.selectedAccountIds.size == 1) currentState.selectedAccountIds.first() else null
            )

            if (rawTransactionsResult is Result.Error) {
                _state.update { it.copy(isLoading = false, error = rawTransactionsResult.exception.message ?: "Error loading transactions") }
                return@launch
            }

            val allFetched = (rawTransactionsResult as? Result.Success)?.data ?: emptyList()

            // Filter transactions in memory
            val filtered = allFetched.filter { tx ->
                val txAmount = tx.amount.doubleValue(false)
                val isExpense = !tx.isIncome
                val txDateStr = try {
                    tx.dateTime.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
                } catch (_: Exception) {
                    ""
                }

                // Date filter
                val matchDate = (currentState.startDate == null || txDateStr >= currentState.startDate) &&
                        (currentState.endDate == null || txDateStr <= currentState.endDate)

                // Account filter
                val matchAccount = currentState.selectedAccountIds.isEmpty() || currentState.selectedAccountIds.contains(tx.accountId)
                
                // Category filter
                val matchCategory = currentState.selectedCategoryIds.isEmpty() || currentState.selectedCategoryIds.contains(tx.categoryId)

                // Type filter
                val matchType = when (currentState.typeFilter) {
                    TransactionTypeFilter.ALL -> true
                    TransactionTypeFilter.EXPENSE_ONLY -> isExpense
                    TransactionTypeFilter.INCOME_ONLY -> tx.isIncome
                }

                // Amount filter
                val matchMin = currentState.minAmount == null || txAmount >= currentState.minAmount
                val matchMax = currentState.maxAmount == null || txAmount <= currentState.maxAmount

                // Text search query
                val query = currentState.searchQuery.trim().lowercase()
                val matchQuery = query.isEmpty() ||
                        (tx.description?.lowercase()?.contains(query) == true) ||
                        (tx.category?.lowercase()?.contains(query) == true) ||
                        (tx.externalId?.lowercase()?.contains(query) == true) ||
                        txAmount.toString().contains(query)

                matchDate && matchAccount && matchCategory && matchType && matchMin && matchMax && matchQuery
            }.map { tx ->
                val resolvedCatName = Category.fromId(
                    id = tx.categoryId,
                    name = tx.category.takeIf { !it.isNullOrBlank() && it != "Uncategorized" },
                    knownCategories = currentState.allCategories
                ).name
                tx.copy(category = resolvedCatName)
            }

            // Calculations
            val income = filtered.filter { it.isIncome }.sumOf { it.amount.doubleValue(false) }
            val expense = filtered.filter { !it.isIncome }.sumOf { it.amount.doubleValue(false) }
            val net = income - expense
            val count = filtered.size
            val avg = if (count > 0) (income + expense) / count else 0.0
            val highest = filtered.maxByOrNull { it.amount.doubleValue(false) }

            // Category breakdown
            val categoryGrouped = filtered.groupBy { it.categoryId }
            val totalVol = income + expense
            val breakdown = categoryGrouped.map { (catId, list) ->
                val catName = currentState.allCategories.find { it.id == catId }?.name ?: list.firstOrNull()?.category ?: "Other"
                val sum = list.sumOf { it.amount.doubleValue(false) }
                val pct = if (totalVol > 0) (sum / totalVol) * 100 else 0.0
                CategoryShare(
                    categoryId = catId,
                    categoryName = catName,
                    totalAmount = sum,
                    percentage = pct,
                    isExpense = list.firstOrNull()?.let { !it.isIncome } ?: true
                )
            }.sortedByDescending { it.totalAmount }

            _state.update {
                it.copy(
                    totalIncome = income,
                    totalExpense = expense,
                    netFlow = net,
                    averageTransaction = avg,
                    highestTransaction = highest,
                    transactionCount = count,
                    categoryBreakdown = breakdown,
                    matchingTransactions = filtered,
                    isLoading = false
                )
            }
        }
    }

    fun exportReportCsv() {
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true) }
            val transactions = _state.value.matchingTransactions
            if (transactions.isEmpty()) {
                _state.update { it.copy(isExporting = false, error = "No transactions to export") }
                return@launch
            }

            val csvHeader = "ID,Date,Description,Amount,Type,Category,Account\n"
            val csvRows = transactions.joinToString("\n") { tx ->
                val desc = (tx.description ?: "").replace("\"", "\"\"")
                val categoryName = tx.category ?: "Uncategorized"
                val type = if (!tx.isIncome) "EXPENSE" else "INCOME"
                "\"${tx.id ?: ""}\",\"${tx.dateTime}\",\"$desc\",${tx.amount.doubleValue(false)},\"$type\",\"$categoryName\",\"${tx.accountId}\""
            }
            val content = csvHeader + csvRows
            val filename = "custom_analysis_report_${DateTimeHelper.today()}.csv"

            val savedPath = fileSaver.saveFile(filename, content)
            _state.update {
                it.copy(
                    isExporting = false,
                    exportSuccessMessage = if (savedPath != null) "Exported report to $savedPath successfully!" else "Export failed or cancelled."
                )
            }
        }
    }

    fun clearExportMessage() {
        _state.update { it.copy(exportSuccessMessage = null) }
    }
}
