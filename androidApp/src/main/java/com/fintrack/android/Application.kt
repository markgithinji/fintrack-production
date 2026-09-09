package com.fintrack.android

import android.app.Application
import com.fintrack.shared.feature.core.data.remote.Environment
import com.fintrack.shared.feature.core.di.Koin
import org.koin.android.ext.koin.androidContext

class FintrackApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val environment = if (BuildConfig.DEBUG) {
            Environment.DEVELOPMENT
        } else {
            Environment.PRODUCTION
        }

        Koin.init(
            environment = environment,
            enableNetworkLogs = BuildConfig.DEBUG,
            appDeclaration = {
                androidContext(this@FintrackApp)
            }
        )
    }
}
