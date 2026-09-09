package com.fintrack.shared.feature.core.di

import com.fintrack.shared.feature.account.di.accountModule
import com.fintrack.shared.feature.auth.di.authModule
import com.fintrack.shared.feature.budget.di.budgetModule
import com.fintrack.shared.feature.core.data.remote.ApiConfig
import com.fintrack.shared.feature.core.data.remote.Environment
import com.fintrack.shared.feature.category.di.categoryModule
import com.fintrack.shared.feature.navigation.di.navigationModule
import com.fintrack.shared.feature.summary.di.summaryModule
import com.fintrack.shared.feature.settings.di.settingsModule
import com.fintrack.shared.feature.transaction.di.transactionModule
import com.fintrack.shared.feature.user.di.userModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin

object Koin {
    private var _koinApplication: KoinApplication? = null
    
    val koin: KoinApplication
        get() = _koinApplication ?: throw IllegalStateException("Koin not initialized")

    fun init(
        environment: Environment = Environment.STAGING,
        enableNetworkLogs: Boolean = false,
        appDeclaration: KoinApplication.() -> Unit = {}
    ) {
        ApiConfig.initialize(environment)

        _koinApplication = startKoin {
            appDeclaration()
            
            properties(
                mapOf(
                    "baseUrl" to ApiConfig.BASE_URL,
                    "enableNetworkLogs" to enableNetworkLogs.toString()
                )
            )

            modules(
                platformModule,
                coreModule,
                authModule,
                accountModule,
                categoryModule,
                transactionModule,
                budgetModule,
                summaryModule,
                settingsModule,
                userModule,
                navigationModule
            )
        }
    }

    inline fun <reified T> get(): T = koin.koin.get()
    inline fun <reified T> inject(): Lazy<T> = koin.koin.inject()
}
