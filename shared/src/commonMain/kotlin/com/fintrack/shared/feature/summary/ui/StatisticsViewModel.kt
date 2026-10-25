package com.fintrack.shared.feature.summary.ui

import Period
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.summary.domain.model.CashflowRunway
import com.fintrack.shared.feature.summary.domain.model.CategoryComparisonSummary
import com.fintrack.shared.feature.summary.domain.model.DistributionSummary
import com.fintrack.shared.feature.summary.domain.model.Highlights
import com.fintrack.shared.feature.summary.domain.model.EmergencyFundScore
import com.fintrack.shared.feature.summary.domain.model.MerchantSummary
import com.fintrack.shared.feature.summary.domain.model.NetWorthSummary
import com.fintrack.shared.feature.summary.domain.model.OverviewPeriod
import com.fintrack.shared.feature.summary.domain.model.OverviewSummary
import com.fintrack.shared.feature.summary.domain.model.SpendSpikeAnomaly
import com.fintrack.shared.feature.summary.domain.usecase.DetectSpendSpikesUseCase
import com.fintrack.shared.feature.summary.domain.usecase.GetEmergencyFundScoreUseCase
import com.fintrack.shared.feature.summary.domain.usecase.GetNetWorthSummaryUseCase
import com.fintrack.shared.feature.summary.domain.model.StatisticsSummary
import com.fintrack.shared.feature.summary.domain.model.TabType
import com.fintrack.shared.feature.summary.domain.model.TransactionCountSummary
import com.fintrack.shared.feature.summary.domain.model.TransactionType
import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.account.domain.repository.AccountRepository
import com.fintrack.shared.feature.summary.domain.repository.SummaryRepository
import com.fintrack.shared.feature.summary.domain.usecase.GetCashflowRunwayUseCase
import com.fintrack.shared.feature.summary.domain.usecase.GetMerchantAnalyticsUseCase
import com.fintrack.shared.feature.summary.domain.model.BudgetOptimizerSummary
import com.fintrack.shared.feature.summary.domain.usecase.GetBudgetOptimizerUseCase
import kotlinx.coroutines.flow.asStateFlow
import com.fintrack.shared.feature.core.util.DateTimeHelper
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.fintrack.shared.feature.summary.domain.model.FinancialHealthIndex
import com.fintrack.shared.feature.summary.domain.model.PassiveIncomeCoverage
import com.fintrack.shared.feature.summary.domain.usecase.GetFinancialHealthIndexUseCase
import com.fintrack.shared.feature.summary.domain.usecase.GetPassiveIncomeCoverageUseCase

@OptIn(FlowPreview::class)
class StatisticsViewModel(
    private val summaryRepository: SummaryRepository,
    private val accountRepository: AccountRepository,
    private val getCashflowRunwayUseCase: GetCashflowRunwayUseCase,
    private val getNetWorthSummaryUseCase: GetNetWorthSummaryUseCase? = null,
    private val detectSpendSpikesUseCase: DetectSpendSpikesUseCase? = null,
    private val getEmergencyFundScoreUseCase: GetEmergencyFundScoreUseCase? = null,
    private val getMerchantAnalyticsUseCase: GetMerchantAnalyticsUseCase? = null,
    private val getBudgetOptimizerUseCase: GetBudgetOptimizerUseCase? = null,
    private val getPassiveIncomeCoverageUseCase: GetPassiveIncomeCoverageUseCase? = null,
    private val getFinancialHealthIndexUseCase: GetFinancialHealthIndexUseCase? = null
) : ViewModel() {

    private val _spendSpikes = MutableStateFlow<Result<List<SpendSpikeAnomaly>>>(Result.Loading)
    val spendSpikes: StateFlow<Result<List<SpendSpikeAnomaly>>> = _spendSpikes.asStateFlow()

    private val _budgetOptimizer = MutableStateFlow<Result<BudgetOptimizerSummary>>(Result.Loading)
    val budgetOptimizer: StateFlow<Result<BudgetOptimizerSummary>> = _budgetOptimizer.asStateFlow()

    private val _selectedBudgetOptimizerDays = MutableStateFlow(30)
    val selectedBudgetOptimizerDays: StateFlow<Int> = _selectedBudgetOptimizerDays.asStateFlow()

    private var budgetOptimizerJob: Job? = null

    fun setBudgetOptimizerDays(days: Int, accountId: String? = null) {
        println("[BUDGET_OPTIMIZER_LOG] Duration chip tapped: days=$days, accountId=$accountId")
        _selectedBudgetOptimizerDays.value = days
        loadBudgetOptimizer(accountId, force = true)
    }

    private var lastOptimizerAccountId: String? = null

    fun loadBudgetOptimizer(accountId: String? = null, force: Boolean = false) {
        if (getBudgetOptimizerUseCase == null) return
        val accountChanged = lastOptimizerAccountId != accountId
        if (!force && !accountChanged && _budgetOptimizer.value is Result.Success) return

        val days = _selectedBudgetOptimizerDays.value
        val dateRange = _selectedPeriod.value?.getDateRange()
        val startDate = dateRange?.first
        val endDate = dateRange?.second

        budgetOptimizerJob?.cancel()
        if (accountChanged || force) {
            _budgetOptimizer.value = Result.Loading
        }

        budgetOptimizerJob = viewModelScope.launch {
            lastOptimizerAccountId = accountId
            println("[BUDGET_OPTIMIZER_LOG] Calling getBudgetOptimizerUseCase with accountId=$accountId, days=$days, startDate=$startDate, endDate=$endDate")
            _budgetOptimizer.value = getBudgetOptimizerUseCase(accountId, days, startDate, endDate)
        }
    }

    private val _emergencyFundScore = MutableStateFlow<Result<EmergencyFundScore>>(Result.Loading)
    val emergencyFundScore: StateFlow<Result<EmergencyFundScore>> = _emergencyFundScore.asStateFlow()

    private var lastSpendSpikesAccountId: String? = null

    fun loadSpendSpikes(accountId: String? = null, force: Boolean = false) {
        if (detectSpendSpikesUseCase == null) return
        val accountChanged = lastSpendSpikesAccountId != accountId
        if (!force && !accountChanged && _spendSpikes.value is Result.Success) return

        if (accountChanged || force) {
            _spendSpikes.value = Result.Loading
        }

        viewModelScope.launch {
            lastSpendSpikesAccountId = accountId
            _spendSpikes.value = detectSpendSpikesUseCase(accountId)
        }
    }

    private var lastEmergencyScoreAccountId: String? = null

    fun loadEmergencyFundScore(accountId: String? = null, force: Boolean = false) {
        if (getEmergencyFundScoreUseCase == null) return
        val accountChanged = lastEmergencyScoreAccountId != accountId
        if (!force && !accountChanged && _emergencyFundScore.value is Result.Success) return

        if (accountChanged || force) {
            _emergencyFundScore.value = Result.Loading
        }

        viewModelScope.launch {
            lastEmergencyScoreAccountId = accountId
            _emergencyFundScore.value = getEmergencyFundScoreUseCase(accountId)
        }
    }

    private val _passiveIncomeCoverage = MutableStateFlow<Result<PassiveIncomeCoverage>>(Result.Loading)
    val passiveIncomeCoverage: StateFlow<Result<PassiveIncomeCoverage>> = _passiveIncomeCoverage.asStateFlow()

    private var lastPassiveCoverageAccountId: String? = null

    fun loadPassiveIncomeCoverage(accountId: String? = null, force: Boolean = false) {
        if (getPassiveIncomeCoverageUseCase == null) return
        val accountChanged = lastPassiveCoverageAccountId != accountId
        if (!force && !accountChanged && _passiveIncomeCoverage.value is Result.Success) return

        if (accountChanged || force) {
            _passiveIncomeCoverage.value = Result.Loading
        }

        viewModelScope.launch {
            lastPassiveCoverageAccountId = accountId
            _passiveIncomeCoverage.value = getPassiveIncomeCoverageUseCase(accountId)
        }
    }

    private val _financialHealthIndex = MutableStateFlow<Result<FinancialHealthIndex>>(Result.Loading)
    val financialHealthIndex: StateFlow<Result<FinancialHealthIndex>> = _financialHealthIndex.asStateFlow()

    private var lastHealthIndexAccountId: String? = null

    fun loadFinancialHealthIndex(accountId: String? = null, force: Boolean = false) {
        if (getFinancialHealthIndexUseCase == null) return
        val accountChanged = lastHealthIndexAccountId != accountId
        if (!force && !accountChanged && _financialHealthIndex.value is Result.Success) return

        if (accountChanged || force) {
            _financialHealthIndex.value = Result.Loading
        }

        viewModelScope.launch {
            lastHealthIndexAccountId = accountId
            _financialHealthIndex.value = getFinancialHealthIndexUseCase(accountId)
        }
    }

    private val _netWorthSummary = MutableStateFlow<Result<NetWorthSummary>>(Result.Loading)
    val netWorthSummary: StateFlow<Result<NetWorthSummary>> = _netWorthSummary.asStateFlow()

    private var lastNetWorthAccountId: String? = null
    private var netWorthJob: Job? = null

    fun loadNetWorthSummary(accountId: String? = null, force: Boolean = false) {
        if (getNetWorthSummaryUseCase == null) return
        val accountChanged = lastNetWorthAccountId != accountId
        if (!force && !accountChanged && _netWorthSummary.value is Result.Success) return

        netWorthJob?.cancel()
        if (accountChanged || force) {
            _netWorthSummary.value = Result.Loading
        }

        netWorthJob = viewModelScope.launch {
            lastNetWorthAccountId = accountId
            _netWorthSummary.value = getNetWorthSummaryUseCase(accountId)
        }
    }

    private val _selectedOverviewPeriod = MutableStateFlow<Period?>(null)
    val selectedOverviewPeriod: StateFlow<Period?> = _selectedOverviewPeriod.asStateFlow()

    fun onOverviewPeriodChanged(period: Period, accountId: String? = null) {
        _selectedOverviewPeriod.value = period
        val yearCode = when (period) {
            is Period.Week -> period.code.substringBefore("-")
            is Period.Month -> period.code.substringBefore("-")
            is Period.Year -> period.code
        }
        loadHighlights(accountId = accountId, period = yearCode, force = true)
    }

    private val _selectedMerchantDays = MutableStateFlow(60)
    val selectedMerchantDays: StateFlow<Int> = _selectedMerchantDays.asStateFlow()

    private val _topMerchants = MutableStateFlow<Result<List<MerchantSummary>>>(Result.Loading)
    val topMerchants: StateFlow<Result<List<MerchantSummary>>> = _topMerchants.asStateFlow()

    fun setMerchantDays(days: Int, accountId: String? = null) {
        _selectedMerchantDays.value = days
        loadTopMerchants(accountId, days)
    }

    fun loadTopMerchants(accountId: String? = null, days: Int? = null) {
        if (getMerchantAnalyticsUseCase == null) return
        val targetDays = days ?: _selectedMerchantDays.value
        val isIncomeMode = _selectedTab.value is TabType.Income
        viewModelScope.launch {
            _topMerchants.value = Result.Loading
            _topMerchants.value = getMerchantAnalyticsUseCase(accountId, targetDays, isIncome = isIncomeMode)
        }
    }

    private val _availableAccounts = MutableStateFlow<List<Account>>(emptyList())
    val availableAccounts: StateFlow<List<Account>> = _availableAccounts.asStateFlow()

    private val _selectedAccountIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedAccountIds: StateFlow<Set<String>> = _selectedAccountIds.asStateFlow()

    val selectedAccountFilter: StateFlow<String?> = _selectedAccountIds
        .combine(_selectedAccountIds) { _, _ -> getEffectiveAccountId() }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _runway = MutableStateFlow<Result<CashflowRunway>>(Result.Loading)
    val runway: StateFlow<Result<CashflowRunway>> = _runway.asStateFlow()

    fun loadAvailableAccounts() {
        viewModelScope.launch {
            when (val res = accountRepository.getAccounts()) {
                is Result.Success -> {
                    val accounts = res.data

                    // Default to M-Pesa account on initial load if no filter selected yet
                    if (_selectedAccountIds.value.isEmpty()) {
                        val mpesaAccount = accounts.find { acc ->
                            acc.linkedSources.contains("mpesa") ||
                                    acc.name.contains("mpesa", ignoreCase = true) ||
                                    acc.type == com.fintrack.shared.feature.account.domain.model.AccountType.MPESA
                        }
                        if (mpesaAccount != null) {
                            _selectedAccountIds.value = setOf(mpesaAccount.id)
                        }
                    }

                    _availableAccounts.value = accounts
                }
                else -> {}
            }
        }
    }

    fun selectAccountFilter(accountId: String?) {
        toggleAccountFilter(accountId)
    }

    fun toggleAccountFilter(accountId: String?) {
        if (accountId == null) {
            _selectedAccountIds.value = emptySet()
        } else {
            val current = _selectedAccountIds.value.toMutableSet()
            if (current.contains(accountId)) {
                current.remove(accountId)
            } else {
                current.add(accountId)
            }
            _selectedAccountIds.value = current
        }

        val effectiveAccountId = getEffectiveAccountId()
        resetStats()
        loadAvailablePeriods(effectiveAccountId, force = true)
        loadOverview(effectiveAccountId, force = true)
        loadCashflowRunway(effectiveAccountId, force = true)
        loadNetWorthSummary(effectiveAccountId, force = true)
        loadSpendSpikes(effectiveAccountId, force = true)
        loadEmergencyFundScore(effectiveAccountId, force = true)
        loadBudgetOptimizer(effectiveAccountId, force = true)
        val yearCode = _selectedPeriod.value?.code?.split("-")?.firstOrNull() ?: _selectedPeriod.value?.code
        loadHighlights(effectiveAccountId, yearCode, force = true)
        reloadDistributionForCurrentSelection(effectiveAccountId, force = true)
    }

    fun getEffectiveAccountId(): String? {
        val set = _selectedAccountIds.value
        return when {
            set.isEmpty() -> null
            set.size == 1 -> set.first()
            else -> set.joinToString(",")
        }
    }

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

    private val _metadataState = MutableStateFlow<Result<Unit>>(Result.Loading)
    val metadataState: StateFlow<Result<Unit>> = _metadataState

    private val _isSyncing = MutableStateFlow(false)
    fun setSyncing(syncing: Boolean) {
        if (_isSyncing.value != syncing) {
            _isSyncing.value = syncing
            if (!syncing && _selectedPeriod.value == null) {
                // If sync finished and we still have no period, we might need to re-fetch metadata
                // or confirm it's empty
                loadAvailablePeriods(lastAvailablePeriodsAccountId, force = true)
            }
        }
    }

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

    val hasNextOverviewPeriod: StateFlow<Boolean> = combine(
        _selectedOverviewPeriod,
        _availableYears
    ) { period, years ->
        if (period == null || years.isEmpty()) return@combine false
        val currentYear = period.code.substringBefore("-")
        val index = years.indexOf(currentYear)
        index > 0
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val hasPreviousOverviewPeriod: StateFlow<Boolean> = combine(
        _selectedOverviewPeriod,
        _availableYears
    ) { period, years ->
        if (period == null || years.isEmpty()) return@combine false
        val currentYear = period.code.substringBefore("-")
        val index = years.indexOf(currentYear)
        if (index == -1) {
            years.size > 1
        } else {
            index < years.size - 1
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun navigateToNextOverviewPeriod(accountId: String? = null) {
        val current = _selectedOverviewPeriod.value ?: return
        val years = _availableYears.value
        if (years.isEmpty()) return

        val currentYear = current.code.substringBefore("-")
        val index = years.indexOf(currentYear)
        val targetIndex = if (index > 0) index - 1 else 0
        val targetYear = years[targetIndex]
        onOverviewPeriodChanged(Period.Year(targetYear), accountId)
    }

    fun navigateToPreviousOverviewPeriod(accountId: String? = null) {
        val current = _selectedOverviewPeriod.value ?: return
        val years = _availableYears.value
        if (years.isEmpty()) return

        val currentYear = current.code.substringBefore("-")
        val index = years.indexOf(currentYear)
        val targetIndex = if (index != -1 && index < years.size - 1) index + 1 else if (years.size > 1) 1 else 0
        val targetYear = years[targetIndex]
        onOverviewPeriodChanged(Period.Year(targetYear), accountId)
    }

    private var lastHighlightsAccountId: String? = null
    private var lastHighlightsPeriod: String? = null
    private var lastRunwayAccountId: String? = null
    private var highlightsJob: Job? = null
    private var availablePeriodsJob: Job? = null
    private var overviewJob: Job? = null
    private var categoryComparisonsJob: Job? = null
    private var transactionCountsJob: Job? = null
    private var runwayJob: Job? = null

    fun loadCashflowRunway(accountId: String? = null, force: Boolean = false) {
        val accountChanged = lastRunwayAccountId != accountId
        if (!force && !accountChanged && _runway.value is Result.Success) return

        runwayJob?.cancel()
        if (accountChanged || force) {
            _runway.value = Result.Loading
        }

        runwayJob = viewModelScope.launch {
            lastRunwayAccountId = accountId
            _runway.value = getCashflowRunwayUseCase(accountId)
        }
    }

    fun resetStats() {
        lastHighlightsAccountId = null
        lastHighlightsPeriod = null
        lastOverviewAccountId = null
        lastCategoryComparisonAccountId = null
        lastCategoryComparisonPeriod = null
        lastTransactionCountsAccountId = null
        lastTransactionCountsIsIncome = null
        lastTransactionCountsCategoryId = null
        lastTransactionCountsStart = null
        lastTransactionCountsEnd = null
        lastTransactionCountsHasCost = null
        lastIncomeDistributionParams = null
        lastExpenseDistributionParams = null
        lastAvailablePeriodsAccountId = null

        _highlights.value = Result.Loading
        _overview.value = Result.Loading
        _categoryComparisons.value = Result.Loading
        _transactionCounts.value = Result.Loading
        _incomeDistribution.value = Result.Loading
        _expenseDistribution.value = Result.Loading
        _runway.value = Result.Loading
        _metadataState.value = Result.Loading
    }

    fun loadHighlights(accountId: String? = null, period: String? = null, force: Boolean = false) {
        val currentPeriod = period ?: DateTimeHelper.currentMonthCode()

        val paramsChanged = lastHighlightsAccountId != accountId || lastHighlightsPeriod != currentPeriod
        val current = _highlights.value
        
        if (!force && !paramsChanged) {
            if (current is Result.Success) return
            if (highlightsJob?.isActive == true) return
        }

        highlightsJob?.cancel()
        
        // Immediately set to loading state if parameters changed to avoid stale data
        if (paramsChanged || force) {
            _highlights.value = Result.Loading
        }

        highlightsJob = viewModelScope.launch {
            lastHighlightsAccountId = accountId
            lastHighlightsPeriod = currentPeriod
            _highlights.value = summaryRepository.getHighlightsSummary(accountId, currentPeriod)
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
        
        if (!force && !paramsChanged) {
            if (current is Result.Success) return
            
            val currentJob = when (type) {
                TransactionType.Income -> incomeDistributionJob
                TransactionType.Expense -> expenseDistributionJob
            }
            if (currentJob?.isActive == true) return
        }

        // Immediately set to loading state if parameters changed
        if (paramsChanged || force) {
            targetFlow.value = Result.Loading
        }

        val job = viewModelScope.launch {
            when (type) {
                TransactionType.Income -> lastIncomeDistributionParams = paramKey
                TransactionType.Expense -> lastExpenseDistributionParams = paramKey
            }
            
            targetFlow.value = summaryRepository.getDistributionSummary(
                weekOrMonthCode = weekOrMonthCode,
                type = type.apiName,
                start = start,
                end = end,
                accountId = accountId
            )
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
        
        // If not forced and account hasn't changed, check if we already have data
        if (!force && !accountChanged) {
            if (_availableWeeks.value.isNotEmpty() || _availableMonths.value.isNotEmpty() || _availableYears.value.isNotEmpty()) {
                reloadDistributionForCurrentSelection(accountId, force = false)
                return
            }
            if (availablePeriodsJob?.isActive == true) return
        }

        availablePeriodsJob?.cancel()
        lastAvailablePeriodsAccountId = accountId
        
        // Reset metadata state if account changed or if forced (e.g. pull-to-refresh)
        if (accountChanged || force) {
            _metadataState.value = Result.Loading
        }

        availablePeriodsJob = viewModelScope.launch {
            try {
                val weeksResult = summaryRepository.getAvailableWeeks(accountId)
                val monthsResult = summaryRepository.getAvailableMonths(accountId)
                val yearsResult = summaryRepository.getAvailableYears(accountId)

                if (weeksResult is Result.Error || monthsResult is Result.Error || yearsResult is Result.Error) {
                    val error = (weeksResult as? Result.Error ?: monthsResult as? Result.Error ?: yearsResult as? Result.Error)!!.exception
                    _metadataState.value = Result.Error(error)
                    return@launch
                }

                val weeks = (weeksResult as Result.Success).data.weeks
                val months = (monthsResult as Result.Success).data.months
                val years = (yearsResult as Result.Success).data.years

                _availableWeeks.value = weeks
                _availableMonths.value = months
                _availableYears.value = years
                
                _metadataState.value = if (weeks.isEmpty() && months.isEmpty() && years.isEmpty() && _isSyncing.value) {
                    Result.Loading
                } else {
                    Result.Success(Unit)
                }

                // Initialize selection if nothing is selected or account changed
                if (accountChanged || _selectedPeriod.value == null) {
                    _selectedPeriod.value = when {
                        weeks.isNotEmpty() -> Period.Week(weeks.first())
                        months.isNotEmpty() -> Period.Month(months.first())
                        years.isNotEmpty() -> Period.Year(years.first())
                        else -> null
                    }
                }

                if (accountChanged || _selectedOverviewPeriod.value == null) {
                    _selectedOverviewPeriod.value = when {
                        weeks.isNotEmpty() -> Period.Week(weeks.first())
                        months.isNotEmpty() -> Period.Month(months.first())
                        years.isNotEmpty() -> Period.Year(years.first())
                        else -> null
                    }
                }

                reloadDistributionForCurrentSelection(accountId, force = force)
            } catch (e: Exception) {
                if (e !is CancellationException) _metadataState.value = Result.Error(e)
            }
        }
    }

    private var lastOverviewAccountId: String? = null
    fun loadOverview(accountId: String? = null, force: Boolean = false) {
        val paramsChanged = lastOverviewAccountId != accountId
        val current = _overview.value
        
        if (!force && !paramsChanged) {
            if (current is Result.Success) return
            if (overviewJob?.isActive == true) return
        }
        
        overviewJob?.cancel()
        
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
        
        println("[DEBUG_ANR] StatisticsViewModel.loadCategoryComparisons(accountId=$accountId, period=$period, force=$force)")

        if (!force && !paramsChanged) {
            if (current is Result.Success) {
                println("[DEBUG_ANR] Skipping loadCategoryComparisons - already success")
                return
            }
            if (categoryComparisonsJob?.isActive == true) {
                println("[DEBUG_ANR] Skipping loadCategoryComparisons - job active")
                return
            }
        }

        categoryComparisonsJob?.cancel()
        
        // Immediately enter loading state
        if (paramsChanged || force) {
            _categoryComparisons.value = Result.Loading
        }

        categoryComparisonsJob = viewModelScope.launch {
            val start = DateTimeHelper.now()
            println("[DEBUG_ANR] Start fetching category comparisons")
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
            val end = DateTimeHelper.now()
            println("[DEBUG_ANR] Finished fetching category comparisons in ${end.toEpochMilliseconds() - start.toEpochMilliseconds()}ms")
        }
    }

    fun onTabChanged(tab: TabType, accountId: String? = null) {
        _selectedTab.value = tab
        loadTopMerchants(accountId)
    }

    fun onPeriodChanged(period: Period, accountId: String? = null) {
        _selectedPeriod.value = period
        // Load BOTH income and expense data for the new period
        reloadDistributionForCurrentSelection(accountId, force = true)
        loadBudgetOptimizer(accountId, force = true)
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
            val periodCode = currentPeriod.code

            // Load Distribution for the specific period (granular)
            loadDistribution(periodCode, TransactionType.Income, accountId = accountId, force = force)
            loadDistribution(periodCode, TransactionType.Expense, accountId = accountId, force = force)

            val yearCode = when (currentPeriod) {
                is Period.Week -> currentPeriod.code.substringBefore("-")
                is Period.Month -> currentPeriod.code.substringBefore("-")
                is Period.Year -> currentPeriod.code
            }
            
            // Only reload Highlights if the year actually changed, or if it's a Yearly selection, or if explicitly forced
            val yearChanged = yearCode != lastHighlightsPeriod
            val shouldForceHighlights = force && (currentPeriod is Period.Year || yearChanged)
            
            loadHighlights(accountId = accountId, period = yearCode, force = shouldForceHighlights)
            
            // Also load category comparisons if the period is a month
            if (currentPeriod is Period.Month) {
                loadCategoryComparisons(accountId = accountId, period = periodCode, force = force)
            }
        } else {
            if (_isSyncing.value) {
                return
            }

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
        val paramsChanged = lastTransactionCountsAccountId != accountId ||
            lastTransactionCountsIsIncome != isIncome ||
            lastTransactionCountsCategoryId != categoryId ||
            lastTransactionCountsStart != start ||
            lastTransactionCountsEnd != end ||
            lastTransactionCountsHasCost != hasCost

        if (!force && !paramsChanged) {
            if (_transactionCounts.value is Result.Success) return
            if (transactionCountsJob?.isActive == true) return
        }

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
