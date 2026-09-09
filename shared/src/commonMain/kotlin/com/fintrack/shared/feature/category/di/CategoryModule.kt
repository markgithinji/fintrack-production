package com.fintrack.shared.feature.category.di

import com.fintrack.shared.feature.category.data.CategoryApi
import com.fintrack.shared.feature.category.data.CategoryRepositoryImpl
import com.fintrack.shared.feature.category.data.LocalCategoryDataSource
import com.fintrack.shared.feature.category.domain.repository.CategoryRepository
import com.fintrack.shared.feature.category.domain.usecase.AddCategoryUseCase
import com.fintrack.shared.feature.category.domain.usecase.DeleteCategoryUseCase
import com.fintrack.shared.feature.category.domain.usecase.SyncCategoriesUseCase
import com.fintrack.shared.feature.category.ui.CategoryManagementViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val categoryModule = module {
    singleOf(::CategoryApi)
    singleOf(::LocalCategoryDataSource)
    singleOf(::CategoryRepositoryImpl) { bind<CategoryRepository>() }
    
    singleOf(::SyncCategoriesUseCase)
    singleOf(::AddCategoryUseCase)
    singleOf(::DeleteCategoryUseCase)

    viewModelOf(::CategoryManagementViewModel)
}
