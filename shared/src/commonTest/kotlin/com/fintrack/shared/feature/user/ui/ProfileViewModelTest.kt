package com.fintrack.shared.feature.user.ui

import com.fintrack.shared.feature.core.domain.SaveState
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.summary.domain.model.ProfileMetrics
import com.fintrack.shared.feature.user.domain.model.User
import com.fintrack.shared.feature.user.domain.repository.UserRepository
import com.fintrack.shared.feature.user.domain.usecase.ProfileValidationUseCase
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeUserRepository = FakeTestUserRepository()
    private val validationUseCase = ProfileValidationUseCase()

    private lateinit var viewModel: ProfileViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val fakeSummaryRepo = object : com.fintrack.shared.feature.summary.domain.repository.SummaryRepository {
            override suspend fun getHighlightsSummary(accountId: String?, period: String?): Result<com.fintrack.shared.feature.summary.domain.model.StatisticsSummary> = Result.Success(com.fintrack.shared.feature.summary.domain.model.StatisticsSummary("", false))
            override suspend fun getDistributionSummary(weekOrMonthCode: String, type: String?, start: String?, end: String?, accountId: String?): Result<com.fintrack.shared.feature.summary.domain.model.DistributionSummary> = Result.Success(com.fintrack.shared.feature.summary.domain.model.DistributionSummary("", BigDecimal.ZERO, emptyList(), emptyList()))
            override suspend fun getAvailableWeeks(accountId: String?): Result<com.fintrack.shared.feature.summary.domain.model.AvailableWeeks> = Result.Success(com.fintrack.shared.feature.summary.domain.model.AvailableWeeks(emptyList()))
            override suspend fun getAvailableMonths(accountId: String?): Result<com.fintrack.shared.feature.summary.domain.model.AvailableMonths> = Result.Success(com.fintrack.shared.feature.summary.domain.model.AvailableMonths(emptyList()))
            override suspend fun getAvailableYears(accountId: String?): Result<com.fintrack.shared.feature.summary.domain.model.AvailableYears> = Result.Success(com.fintrack.shared.feature.summary.domain.model.AvailableYears(emptyList()))
            override suspend fun getOverviewSummary(accountId: String?, startDate: String?, endDate: String?): Result<com.fintrack.shared.feature.summary.domain.model.OverviewSummary> = Result.Success(com.fintrack.shared.feature.summary.domain.model.OverviewSummary("", false, emptyList(), emptyList()))
            override suspend fun getCategoryComparisons(accountId: String?, period: String?): Result<com.fintrack.shared.feature.summary.domain.model.CategoryComparisonSummary> = Result.Success(com.fintrack.shared.feature.summary.domain.model.CategoryComparisonSummary("", false, emptyList()))
            override suspend fun getTransactionCounts(accountId: String, isIncome: Boolean?, categoryId: String?, start: String?, end: String?, hasTransactionCost: Boolean?): Result<com.fintrack.shared.feature.summary.domain.model.TransactionCountSummary> = Result.Success(com.fintrack.shared.feature.summary.domain.model.TransactionCountSummary(0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO))
            override suspend fun getProfileMetrics(): Result<ProfileMetrics> = Result.Success(ProfileMetrics("Jane Doe", "jane@example.com", BigDecimal.fromInt(150000), BigDecimal.fromInt(25), BigDecimal.fromInt(40)))
            override suspend fun getTopMerchants(accountId: String?, days: Int, limit: Long, isIncome: Boolean): Result<List<com.fintrack.shared.feature.summary.domain.model.MerchantSummary>> = Result.Success(emptyList())
        }

        viewModel = ProfileViewModel(
            userRepository = fakeUserRepository,
            validationUseCase = validationUseCase,
            summaryRepository = fakeSummaryRepo
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `form setters update name and email state correctly`() = runTest(testDispatcher) {
        viewModel.onNameChange("Alex Mercer")
        viewModel.onEmailChange("alex@example.com")

        val formState = viewModel.formState.value
        assertEquals("Alex Mercer", formState.name)
        assertEquals("alex@example.com", formState.email)
        assertNull(formState.nameError)
        assertNull(formState.emailError)
    }

    @Test
    fun `updateProfile validates invalid email and sets error message`() = runTest(testDispatcher) {
        viewModel.onNameChange("John Doe")
        viewModel.onEmailChange("invalid-email-string")

        viewModel.updateProfile()

        val formState = viewModel.formState.value
        assertTrue(formState.emailError != null)
    }

    @Test
    fun `updateProfile saves valid name and email successfully`() = runTest(testDispatcher) {
        viewModel.onNameChange("John Doe")
        viewModel.onEmailChange("john.doe@example.com")

        viewModel.updateProfile()

        assertTrue(viewModel.editState.value is SaveState.Success)
        assertEquals("John Doe", fakeUserRepository.getUserProfile().value?.name)
        assertEquals("john.doe@example.com", fakeUserRepository.getUserProfile().value?.email)
    }

    @Test
    fun `refreshProfile fetches fresh profile metrics and updates form state`() = runTest(testDispatcher) {
        viewModel.refreshProfile(force = true)

        assertTrue(viewModel.metricsState.value is Result.Success)
        val metrics = (viewModel.metricsState.value as Result.Success).data
        assertEquals("Jane Doe", metrics.name)
        assertEquals("jane@example.com", metrics.email)
    }
}

private class FakeTestUserRepository : UserRepository {
    private val _userProfile = MutableStateFlow<User?>(User(name = "Original Name", email = "original@example.com"))
    override fun getUserProfile(): StateFlow<User?> = _userProfile.asStateFlow()
    override suspend fun refreshProfile(): Result<User> = Result.Success(_userProfile.value!!)
    override suspend fun updateProfile(name: String, email: String): Result<User> {
        val user = User(name = name, email = email)
        _userProfile.value = user
        return Result.Success(user)
    }
    override suspend fun updateTrackedCategories(categories: List<String>): Result<User> = Result.Success(_userProfile.value!!)
    override suspend fun deleteAccount(): Result<Unit> = Result.Success(Unit)
    override fun clearProfile() { _userProfile.value = null }
}
