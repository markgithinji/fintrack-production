package com.fintrack.shared.feature.navigation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.fintrack.shared.feature.core.ui.biometric.BiometricAuthenticator
import com.fintrack.shared.feature.core.util.formatToCurrency
import com.fintrack.shared.feature.settings.domain.model.AppTheme
import com.fintrack.shared.feature.settings.domain.model.Currency
import com.fintrack.shared.feature.settings.domain.model.TimeFormat
import com.fintrack.shared.feature.user.domain.model.User
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.koin.compose.koinInject

val LocalCurrency = compositionLocalOf { Currency.KES }
val LocalPrivacyMode = compositionLocalOf { false }
val LocalShowDecimals = compositionLocalOf { true }
val LocalTimeFormat = compositionLocalOf { TimeFormat.TWENTY_FOUR_HOUR }
val LocalAppTheme = compositionLocalOf { AppTheme.SYSTEM }
val LocalUser = compositionLocalOf<User?> { null }

/**
 * Provides the extra bottom padding for toasts to avoid overlapping with FABs or bottom bars.
 */
val LocalToastBottomPadding = compositionLocalOf { 0.dp }

val LocalBiometricAuthenticator = staticCompositionLocalOf<BiometricAuthenticator> {
    error("No BiometricAuthenticator provided")
}

val LocalNavController = staticCompositionLocalOf<NavHostController> {
    error("No NavController provided")
}

@Composable
fun Double.toCurrencyString(): String {
    val currency = LocalCurrency.current
    val isPrivacyMode = LocalPrivacyMode.current
    val showDecimals = LocalShowDecimals.current

    if (isPrivacyMode) {
        return "${currency.symbol} ****"
    }
    return this.formatToCurrency(currency.symbol, showDecimals = showDecimals)
}

@Composable
fun BigDecimal.toCurrencyString(): String {
    val currency = LocalCurrency.current
    val isPrivacyMode = LocalPrivacyMode.current
    val showDecimals = LocalShowDecimals.current

    if (isPrivacyMode) {
        return "${currency.symbol} ****"
    }
    return this.formatToCurrency(currency.symbol, showDecimals = showDecimals)
}

@Composable
fun AppStateProvider(
    viewModel: MainViewModel = koinInject(),
    toastBottomPadding: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    val navController = rememberNavController()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val isBalanceHidden by viewModel.isBalanceHidden.collectAsStateWithLifecycle()
    val showDecimals by viewModel.showDecimals.collectAsStateWithLifecycle()
    val timeFormat by viewModel.timeFormat.collectAsStateWithLifecycle()
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    CompositionLocalProvider(
        LocalCurrency provides currency,
        LocalPrivacyMode provides isBalanceHidden,
        LocalShowDecimals provides showDecimals,
        LocalTimeFormat provides timeFormat,
        LocalAppTheme provides theme,
        LocalUser provides userProfile,
        LocalToastBottomPadding provides toastBottomPadding,
        LocalNavController provides navController
    ) {
        content()
    }
}
