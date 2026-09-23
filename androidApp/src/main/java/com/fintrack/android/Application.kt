package com.fintrack.android

import android.app.Application
import com.fintrack.shared.feature.core.di.Koin
import org.koin.android.ext.koin.androidContext

class FintrackApp : Application() {
    override fun onCreate() {
        super.onCreate()

        Koin.init(
            appDeclaration = {
                androidContext(this@FintrackApp)
            }
        )
    }
}
