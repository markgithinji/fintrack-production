package com.fintrack.shared

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.fintrack.shared.feature.core.di.Koin
import com.fintrack.shared.feature.navigation.ui.MainScreen

fun MainViewController() = ComposeUIViewController {
    remember {
        Koin.init()
    }
    MainScreen()
}
