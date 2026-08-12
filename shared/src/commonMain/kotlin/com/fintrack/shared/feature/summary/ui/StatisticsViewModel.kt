package com.fintrack.shared.feature.summary.ui

import Period
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.summary.domain.model.CategoryComparisonSummary
import com.fintrack.shared.feature.summary.domain.model.DistributionSummary
import com.fintrack.shared.feature.summary.domain.model.Highlights
import com.fintrack.shared.feature.summary.domain.model.OverviewSummary
import com.fintrack.shared.feature.summary.domain.model.StatisticsSummary
import com.fintrack.shared.feature.summary.domain.model.TabType
import com.fintrack.shared.feature.summary.domain.model.TransactionCountSummary
import com.fintrack.shared.feature.summary.domain.model.TransactionType
import com.fintrack.shared.feature.summary.domain.repository.SummaryRepository
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class StatisticsViewModel(
    private val summaryRepository: SummaryRepository
) : ViewModel() {

    private val _highlights = MutableStateFlow<Result<StatisticsSummary>>(Result.Loading)
    val highlights: StateFlow<Result<StatisticsSummary>> = _highlights

    private val _incomeDistribution = MutableStateFlow<Result<DistributionSummary>>(Result.Loading)
    private val _expenseDistribution = MutableStateFlow<Result<DistributionSummary>>(Result.Loading)

    private val _availableWeeks = MutableStateFlow<List<String>>(emptyList())
    val availableWeeks: StateFlow<List<String>> = _availableWeeks

    private val _availableMonths = MutableStateFlow<List<String>>(emptyList())
    val availableMonths: StateFlow<List<String>> = _availableMonths

    private val _availableYears = MutableStateFlow<List<String>>(emptyList())
    val availableYears: StateFlow<List<String>> = _availableYears

    private val _overview = MutableStateFlow<Result<OverviewSummary>>(Result.Loading)
    val overview: StateFlow<Result<OverviewSummary>> = _overview

    private val _selectedTab = MutableStateFlow<TabType>(TabType.Expense)
    val selectedTab: StateFlow<TabType> = _selectedTab

    private val _selectedPeriod = MutableStateFlow<Period?>(null)
    val selectedPeriod: StateFlow<Period?> = _selectedPeriod

    private val _categoryComparisons =
        MutableStateFlow<Result<CategoryComparisonSummary>>(Result.Loading)
    val categoryComparisons: StateFlow<Result<CategoryComparisonSummary>> = _categoryComparisons

    private val _transactionCounts =
        MutableStateFlow<Result<TransactionCountSummary>>(Result.Loading)
    val transactionCounts: StateFlow<Result<TransactionCountSummary>> = _transactionCounts

    val distribution: StateFlow<Result<DistributionSummary>> =
        combine(
            selectedTab,
            _incomeDistribution,
            _expenseDistribution
        ) { tab, income, expense ->
            when (tab) {
                is TabType.Income -> income
                is TabType.Expense -> expense
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Result.Loading
        )

    val hasNextPeriod: StateFlow<Boolean> = combine(
        _selectedPeriod,
        _availableWeeks,
        _availableMonths,
        _availableYears
    ) { period, weeks, months, years ->
        if (period == null) return@combine false
        val list = when (period) {
            is Period.Week -> weeks
            is Period.Month -> months
            is Period.Year -> years
        }
        val index = list.indexOf(period.code)
        index > 0 // Newest first, so "Next" (forward in time) is index - 1
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val hasPreviousPeriod: StateFlow<Boolean> = combine(
        _selectedPeriod,
        _availableWeeks,
        _availableMonths,
        _availableYears
    ) { period, weeks, months, years ->
        if (period == null) return@combine false
        val list = when (period) {
            is Period.Week -> weeks
            is Period.Month -> months
            is Period.Year -> years
        }
        val index = list.indexOf(period.code)
        index != -1 && index < list.size - 1
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private var lastHighlightsAccountId: String? = null
    private var lastHighlightsPeriod: String? = null
    private var highlightsJob: Job? = null
    private var availablePeriodsJob: Job? = null
    private var overviewJob: Job? = null
    private var categoryComparisonsJob: Job? = null
    private var transactionCountsJob: Job? = null

    fun loadHighlights(accountId: String? = null, period: String? = null, force: Boolean = false) {
        if (period == null) {
            _highlights.value = Result.Success(createEmptyStatisticsSummary())
            return
        }

        val paramsChanged = lastHighlightsAccountId != accountId || lastHighlightsPeriod != period
        val current = _highlights.value
        
        if (!force && current is Result.Success && !paramsChanged) {
            return
        }

        highlightsJob?.cancel()
        
        // Immediately set to loading state if parameters changed to avoid stale data
        if (paramsChanged || force) {
            _highlights.value = Result.Loading
        }

        highlightsJob = viewModelScope.launch {
            lastHighlightsAccountId = accountId
            lastHighlightsPeriod = period
            _highlights.value = summaryRepository.getHighlightsSummary(accountId, period)
        }
    }

    private var lastIncomeDistributionParams: String? = null
    private var lastExpenseDistributionParams: String? = null
    private var incomeDistributionJob: Job? = null
    private var expenseDistributionJob: Job? = null

    private fun loadDistribution(
        weekOrMonthCode: String,
        type: TransactionType,
        start: String? = null,
        end: String? = null,
        accountId: String? = null,
        force: Boolean = false
    ) {
        val paramKey = "$weekOrMonthCode|$start|$end|$accountId"
        val targetFlow = when (type) {
            TransactionType.Income -> _incomeDistribution
            TransactionType.Expense -> _expenseDistribution
        }
        val lastParams = when (type) {
            TransactionType.Income -> lastIncomeDistributionParams
            TransactionType.Expense -> lastExpenseDistributionParams
        }

        val paramsChanged = lastParams != paramKey
        val current = targetFlow.value
        
        if (!force && current is Result.Success && !paramsChanged) return

        // Immediately set to loading state if parameters changed
        if (paramsChanged || force) {
            targetFlow.value = Result.Loading
        }

        val job = viewModelScope.launch {
            when (type) {
                TransactionType.Income -> lastIncomeDistributionParams = paramKey
                TransactionType.Expense -> lastExpenseDistributionParams = paramKey
            }
            
            val distribution = summaryRepository.getDistributionSummary(
                weekOrMonthCode = weekOrMonthCode,
                type = type.apiName,
                start = start,
                end = end,
                accountId = accountId
            )
            
            targetFlow.value = distribution
        }

        when (type) {
            TransactionType.Income -> {
                incomeDistributionJob?.cancel()
                incomeDistributionJob = job
            }
            TransactionType.Expense -> {
                expenseDistributionJob?.cancel()
                expenseDistributionJob = job
            }
        }
    }

    private var lastAvailablePeriodsAccountId: String? = null
    fun loadAvailablePeriods(accountId: String? = null, force: Boolean = false) {
        val accountChanged = lastAvailablePeriodsAccountId != accountId
        if (!force && !accountChanged && (_availableWeeks.value.isNotEmpty() || _availableMonths.value.isNotEmpty() || _availableYears.value.isNotEmpty())) {
            reloadDistributionForCurrentSelection(accountId, force = false)
            return
        }

        availablePeriodsJob?.cancel()
        
        // Immediately clear available periods if account changed to trigger loading states in UI
        if (accountChanged || force) {
            _availableWeeks.value = emptyList()
            _availableMonths.value = emptyList()
            _availableYears.value = emptyList()
            _selectedPeriod.value = null
            
            // Also set distributions to loading
            _incomeDistribution.value = Result.Loading
            _expenseDistribution.value = Result.Loading
            _highlights.value = Result.Loading
            _categoryComparisons.value = Result.Loading
        }

        availablePeriodsJob = viewModelScope.launch {
            try {
                lastAvailablePeriodsAccountId = accountId
                val weeksDeferred = viewModelScope.async {
                    val result = summaryRepository.getAvailableWeeks(accountId)
                    if (result is Result.Success) result.data.weeks else emptyList()
                }

                val monthsDeferred = viewModelScope.async {
                    val result = summaryRepository.getAvailableMonths(accountId)
                    if (result is Result.Success) result.data.months else emptyList()
                }

                val yearsDeferred = viewModelScope.async {
                    val result = summaryRepository.getAvailableYears(accountId)
                    if (result is Result.Success) result.data.years else emptyList()
                }

                // Wait for all results
                val weeks = weeksDeferred.await()
                val months = monthsDeferred.await()
                val years = yearsDeferred.await()

                _availableWeeks.value = weeks
                _availableMonths.value = months
                _availableYears.value = years

                // Reset selection if account changed or if nothing is selected
                if (accountChanged || _selectedPeriod.value == null) {
                    _selectedPeriod.value = when {
                        weeks.isNotEmpty() -> Period.Week(weeks.first())
                        months.isNotEmpty() -> Period.Month(months.first())
                        years.isNotEmpty() -> Period.Year(years.first())
                        else -> null
                    }
                }

                // Load BOTH income and expense data for the selection
                reloadDistributionForCurrentSelection(accountId, force = force)
            } catch (e: Exception) {
                _availableWeeks.value = emptyList()
                _availableMonths.value = emptyList()
                _availableYears.value = emptyList()
                _selectedPeriod.value = null
                reloadDistributionForCurrentSelection(accountId, force = force)
            }
        }
    }

    private var lastOverviewAccountId: String? = null
    fun loadOverview(accountId: String? = null, force: Boolean = false) {
        val paramsChanged = lastOverviewAccountId != accountId
        val current = _overview.value
        
        if (!force && current is Result.Success && !paramsChanged) return
        
        overviewJob?.cancel()
        
        // Immediately enter loading state if account changed or if forced
        if (paramsChanged || force) {
            _overview.value = Result.Loading
        }

        overviewJob = viewModelScope.launch {
            lastOverviewAccountId = accountId
            _overview.value = summaryRepository.getOverviewSummary(accountId)
        }
    }

    private var lastCategoryComparisonAccountId: String? = null
    private var lastCategoryComparisonPeriod: String? = null
    fun loadCategoryComparisons(
        accountId: String? = null,
        period: String? = null,
        force: Boolean = false
    ) {
        val paramsChanged = lastCategoryComparisonAccountId != accountId || lastCategoryComparisonPeriod != period
        val current = _categoryComparisons.value
        
        if (!force && current is Result.Success && !paramsChanged) return

        categoryComparisonsJob?.cancel()
        
        // Immediately enter loading state
        if (paramsChanged || force) {
            _categoryComparisons.value = Result.Loading
        }

        categoryComparisonsJob = viewModelScope.launch {
            lastCategoryComparisonAccountId = accountId
            lastCategoryComparisonPeriod = period

            // If no period provided, try to find the last active month
            val targetPeriod = period ?: if (_availableMonths.value.isNotEmpty()) {
                _availableMonths.value.first()
            } else {
                // Fetch available months if they aren't loaded yet
                val result = summaryRepository.getAvailableMonths(accountId)
                if (result is Result.Success && result.data.months.isNotEmpty()) {
                    val months = result.data.months
                    _availableMonths.value = months
                    months.first()
                } else {
                    null
                }
            }

            _categoryComparisons.value = summaryRepository.getCategoryComparisons(accountId, targetPeriod)
        }
    }

    fun onTabChanged(tab: TabType, accountId: String? = null) {
        _selectedTab.value = tab
    }

    fun onPeriodChanged(period: Period, accountId: String? = null) {
        _selectedPeriod.value = period
        // Load BOTH income and expense data for the new period
        reloadDistributionForCurrentSelection(accountId, force = true)
    }

    fun navigateToNextPeriod(accountId: String? = null) {
        val current = _selectedPeriod.value ?: return
        val list = when (current) {
            is Period.Week -> _availableWeeks.value
            is Period.Month -> _availableMonths.value
            is Period.Year -> _availableYears.value
        }
        val currentIndex = list.indexOf(current.code)
        if (currentIndex > 0) { // List is sorted descending (newest first)
            val nextPeriod = when (current) {
                is Period.Week -> Period.Week(list[currentIndex - 1])
                is Period.Month -> Period.Month(list[currentIndex - 1])
                is Period.Year -> Period.Year(list[currentIndex - 1])
            }
            onPeriodChanged(nextPeriod, accountId)
        }
    }

    fun navigateToPreviousPeriod(accountId: String? = null) {
        val current = _selectedPeriod.value ?: return
        val list = when (current) {
            is Period.Week -> _availableWeeks.value
            is Period.Month -> _availableMonths.value
            is Period.Year -> _availableYears.value
        }
        val currentIndex = list.indexOf(current.code)
        if (currentIndex != -1 && currentIndex < list.size - 1) {
            val prevPeriod = when (current) {
                is Period.Week -> Period.Week(list[currentIndex + 1])
                is Period.Month -> Period.Month(list[currentIndex + 1])
                is Period.Year -> Period.Year(list[currentIndex + 1])
            }
            onPeriodChanged(prevPeriod, accountId)
        }
    }

    fun reloadDistributionForCurrentSelection(accountId: String? = null, force: Boolean = false) {
        val currentPeriod = _selectedPeriod.value
        if (currentPeriod != null) {
            val periodCode = when (currentPeriod) {
                is Period.Week -> currentPeriod.code
                is Period.Month -> currentPeriod.code
                is Period.Year -> currentPeriod.code
            }

            // Load BOTH income and expense data for this period
            loadDistribution(periodCode, TransactionType.Income, accountId = accountId, force = force)
            loadDistribution(periodCode, TransactionType.Expense, accountId = accountId, force = force)

            val yearCode = when (currentPeriod) {
                is Period.Week -> currentPeriod.code.substringBefore("-")
                is Period.Month -> currentPeriod.code.substringBefore("-")
                is Period.Year -> currentPeriod.code
            }
            loadHighlights(accountId = accountId, period = yearCode, force = force)
            
            // Also load category comparisons if the period is a month
            if (currentPeriod is Period.Month) {
                loadCategoryComparisons(accountId = accountId, period = periodCode, force = force)
            }
        } else {
            // Clear all summary states if no period is selected (e.g., empty account)
            lastIncomeDistributionParams = null
            lastExpenseDistributionParams = null
            lastHighlightsPeriod = null
            lastCategoryComparisonPeriod = null
            
            _incomeDistribution.value = Result.Success(
                DistributionSummary(
                    period = "",
                    totalTransactionCost = BigDecimal.ZERO,
                    incomeCategories = emptyList(),
                    expenseCategories = emptyList()
                )
            )
            _expenseDistribution.value = Result.Success(
                DistributionSummary(
                    period = "",
                    totalTransactionCost = BigDecimal.ZERO,
                    incomeCategories = emptyList(),
                    expenseCategories = emptyList()
                )
            )
            _highlights.value = Result.Success(createEmptyStatisticsSummary())
            _categoryComparisons.value = Result.Success(
                CategoryComparisonSummary(
                    period = "",
                    isCurrent = false,
                    data = emptyList()
                )
            )
        }
    }

    private fun createEmptyStatisticsSummary() = StatisticsSummary(
        period = "",
        incomeHighlights = createEmptyHighlights(),
        expenseHighlights = createEmptyHighlights()
    )

    private fun createEmptyHighlights() = Highlights(
        highestMonth = null,
        highestCategory = null,
        highestDay = null,
        averagePerDay = BigDecimal.ZERO
    )

    private var lastTransactionCountsAccountId: String? = null
    private var lastTransactionCountsIsIncome: Boolean? = null
    private var lastTransactionCountsCategoryId: String? = null
    private var lastTransactionCountsStart: String? = null
    private var lastTransactionCountsEnd: String? = null
    private var lastTransactionCountsHasCost: Boolean? = null

    fun loadTransactionCounts(
        accountId: String,
        isIncome: Boolean? = null,
        categoryId: String? = null,
        start: String? = null,
        end: String? = null,
        hasCost: Boolean? = null,
        force: Boolean = false
    ) {
        if (!force && _transactionCounts.value is Result.Success &&
            lastTransactionCountsAccountId == accountId &&
            lastTransactionCountsIsIncome == isIncome &&
            lastTransactionCountsCategoryId == categoryId &&
            lastTransactionCountsStart == start &&
            lastTransactionCountsEnd == end &&
            lastTransactionCountsHasCost == hasCost
        ) return

        transactionCountsJob?.cancel()
        transactionCountsJob = viewModelScope.launch {
            _transactionCounts.value = Result.Loading
            lastTransactionCountsAccountId = accountId
            lastTransactionCountsIsIncome = isIncome
            lastTransactionCountsCategoryId = categoryId
            lastTransactionCountsStart = start
            lastTransactionCountsEnd = end
            lastTransactionCountsHasCost = hasCost
            val result = summaryRepository.getTransactionCounts(accountId, isIncome, categoryId, start, end, hasCost)
            _transactionCounts.value = result
        }
    }
}