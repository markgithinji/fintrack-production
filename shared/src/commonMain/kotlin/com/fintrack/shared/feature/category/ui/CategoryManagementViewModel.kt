package com.fintrack.shared.feature.category.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.shared.feature.category.data.LocalCategoryDataSource
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.model.allCategories
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
    private val deleteCategoryUseCase: DeleteCategoryUseCase
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
            when (val result = syncCategoriesUseCase()) {
                is Result.Error -> {
                    val exception = result.exception
                    _state.update { it.copy(
                        error = (exception as? ApiException)?.getUserFriendlyMessage()
                            ?: exception.message ?: "Failed to refresh categories"
                    ) }
                }
                else -> { /* Success or Loading handled elsewhere */ }
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
