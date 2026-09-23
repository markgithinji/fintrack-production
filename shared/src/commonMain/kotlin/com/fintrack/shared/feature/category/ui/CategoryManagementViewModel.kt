package com.fintrack.shared.feature.category.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.shared.feature.category.data.LocalCategoryDataSource
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.model.allCategories
import com.fintrack.shared.feature.category.domain.repository.CategoryRepository
import com.fintrack.shared.feature.category.domain.usecase.AddCategoryUseCase
import com.fintrack.shared.feature.category.domain.usecase.DeleteCategoryUseCase
import com.fintrack.shared.feature.category.domain.usecase.SyncCategoriesUseCase
import com.fintrack.shared.feature.core.data.model.ApiException
import com.fintrack.shared.feature.core.data.model.getUserFriendlyMessage
import com.fintrack.shared.feature.core.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CategoryManagementViewModel(
    private val localCategoryDataSource: LocalCategoryDataSource,
    private val syncCategoriesUseCase: SyncCategoriesUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CategoryManagementState(categories = Category.allCategories))
    val state: StateFlow<CategoryManagementState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            localCategoryDataSource.categories.collect { categories ->
                _state.update { it.copy(categories = categories) }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val syncResult = syncCategoriesUseCase()
            if (syncResult is Result.Error) {
                val exception = syncResult.exception
                _state.update { it.copy(
                    error = (exception as? ApiException)?.getUserFriendlyMessage()
                        ?: exception.message ?: "Failed to refresh categories"
                ) }
            }
            
            val rulesResult = categoryRepository.getCategoryRules()
            if (rulesResult is Result.Success) {
                _state.update { it.copy(rules = rulesResult.data) }
            }
            
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun addRule(keyword: String, categoryId: String, isExpense: Boolean) {
        if (keyword.isBlank()) {
            _state.update { it.copy(error = "Keyword cannot be empty") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = categoryRepository.addCategoryRule(keyword.trim(), categoryId, isExpense)) {
                is Result.Error -> {
                    _state.update { it.copy(error = result.exception.message ?: "Failed to add rule") }
                }
                is Result.Success -> {
                    refresh()
                }
                else -> {}
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun deleteRule(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = categoryRepository.deleteCategoryRule(id)) {
                is Result.Error -> {
                    _state.update { it.copy(error = result.exception.message ?: "Failed to delete rule") }
                }
                is Result.Success -> {
                    refresh()
                }
                else -> {}
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun addCategory(name: String, isExpense: Boolean, iconName: String? = null) {
        if (name.isBlank()) {
            _state.update { it.copy(error = "Category name cannot be empty") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = addCategoryUseCase(name, isExpense, iconName)) {
                is Result.Error -> {
                    val exception = result.exception
                    _state.update { it.copy(
                        error = (exception as? ApiException)?.getUserFriendlyMessage()
                            ?: exception.message ?: "Failed to add category"
                    ) }
                }
                is Result.Success -> { /* List will auto-update via repository flow */ }
                else -> {}
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = deleteCategoryUseCase(id)) {
                is Result.Error -> {
                    val exception = result.exception
                    _state.update { it.copy(
                        error = (exception as? ApiException)?.getUserFriendlyMessage()
                            ?: exception.message ?: "Failed to delete category"
                    ) }
                }
                is Result.Success -> { /* List will auto-update via repository flow */ }
                else -> {}
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
