package com.fintrack.shared.feature.navigation.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fintrack.shared.feature.auth.domain.model.AuthState
import com.fintrack.shared.feature.auth.ui.AuthViewModel
import com.fintrack.shared.feature.auth.ui.LockScreen
import com.fintrack.shared.feature.settings.domain.model.AppTheme
import com.fintrack.shared.feature.navigation.model.Screen
import com.fintrack.shared.feature.navigation.ui.components.AuthErrorScreen
import com.fintrack.shared.feature.navigation.ui.components.AuthLoadingScreen
import com.fintrack.shared.feature.core.ui.MaterialToast
import com.fintrack.shared.feature.core.ui.biometric.BiometricResult
import com.fintrack.shared.feature.core.ui.biometric.rememberBiometricAuthenticator
import com.fintrack.shared.ui.theme.FinanceTrackerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainScreen(
    initialTransactionId: String? = null,
    onTransactionIdConsumed: () -> Unit = {},
    mainViewModel: MainViewModel = koinInject()
) {
    val appTheme by mainViewModel.theme.collectAsStateWithLifecycle()
    
    val isDarkTheme = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    FinanceTrackerTheme(darkTheme = isDarkTheme) {
        val biometricAuthenticator = rememberBiometricAuthenticator()
        
        // Provide biometric authenticator at the root so it's available via CompositionLocal
        CompositionLocalProvider(LocalBiometricAuthenticator provides biometricAuthenticator) {
            val authViewModel: AuthViewModel = koinViewModel()
            val authStatusState by authViewModel.authStatus.collectAsStateWithLifecycle()
            val isAppLocked by authViewModel.isAppLocked.collectAsStateWithLifecycle()
            val authToast by authViewModel.toastMessage.collectAsStateWithLifecycle()
            val mainToast by mainViewModel.toastMessage.collectAsStateWithLifecycle()
            
            val scope = rememberCoroutineScope()
            
            // Observe app lifecycle for biometric lock
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_STOP -> authViewModel.onAppBackgrounded()
                        Lifecycle.Event.ON_START -> authViewModel.onAppForegrounded()
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }
            
            // Track the last error for smooth transitions on retry
            var lastError by remember { mutableStateOf<AuthState.Error?>(null) }
            var toastPadding by remember { mutableStateOf(24.dp) }

            LaunchedEffect(authStatusState) {
                val state = authStatusState
                when (state) {
                    is AuthState.Error -> lastError = state
                    is AuthState.Success -> {
                        val isAuthenticated = state.data
                        if (!isAuthenticated) {
                            // Clear errors immediately on logout for a clean transition
                            lastError = null
                        } else if (lastError != null) {
                            delay(1500)
                            lastError = null
                        }
                    }
                    else -> {}
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AnimatedContent(
                        targetState = isAppLocked to authStatusState,
                        transitionSpec = {
                            if (initialState.first && !targetState.first) {
                                // Transition from Locked to Unlocked
                                (fadeIn(animationSpec = tween(600)) + 
                                 scaleIn(initialScale = 0.92f, animationSpec = tween(600, easing = FastOutSlowInEasing)) +
                                 slideInVertically(initialOffsetY = { it / 10 }, animationSpec = tween(600, easing = FastOutSlowInEasing))
                                ).togetherWith(
                                    fadeOut(animationSpec = tween(600)) + 
                                    scaleOut(targetScale = 1.08f, animationSpec = tween(600, easing = FastOutSlowInEasing))
                                )
                            } else if (initialState.second is AuthState.Loading && targetState.second is AuthState.Success<*>) {
                                // Transition from Loading to Success (App start)
                                (fadeIn(animationSpec = tween(800)) + 
                                 scaleIn(initialScale = 0.85f, animationSpec = tween(800, easing = FastOutSlowInEasing))
                                ).togetherWith(
                                    fadeOut(animationSpec = tween(600))
                                )
                            } else {
                                fadeIn(animationSpec = tween(500)).togetherWith(fadeOut(animationSpec = tween(500)))
                            }
                        },
                        label = "MainAppTransition"
                    ) { (locked, authState) ->
                        if (locked) {
                            LockScreen(
                                onUnlock = {
                                    scope.launch {
                                        val result = biometricAuthenticator.authenticate(
                                            title = "Unlock Fintrack",
                                            subtitle = "Authenticate to access your account"
                                        )
                                        if (result is BiometricResult.Success) {
                                            authViewModel.unlockWithBiometrics()
                                        }
                                    }
                                }
                            )
                        } else {
                            when (authState) {
                                is AuthState.Loading -> {
                                    if (lastError != null) {
                                        AuthErrorScreen(
                                            error = lastError!!.exception,
                                            isLoading = true,
                                            onRetry = { authViewModel.checkAuthenticationStatus() }
                                        )
                                    } else {
                                        AuthLoadingScreen(message = authState.message)
                                    }
                                }
                                is AuthState.Error -> {
                                    AuthErrorScreen(
                                        error = authState.exception,
                                        onRetry = { authViewModel.checkAuthenticationStatus() }
                                    )
                                }
                                is AuthState.Success, is AuthState.Idle -> {
                                    if (lastError != null) {
                                        AuthErrorScreen(
                                            error = lastError!!.exception,
                                            isSuccess = true,
                                            onRetry = { authViewModel.checkAuthenticationStatus() }
                                        )
                                    } else {
                                        val isAuthenticated = (authState as? AuthState.Success)?.data ?: false
                                        
                                        if (isAuthenticated) {
                                            // Entering the Main App "Room"
                                            AppStateProvider(
                                                viewModel = mainViewModel,
                                                toastBottomPadding = toastPadding
                                            ) {
                                                val navController = LocalNavController.current
                                                
                                                // Handle deep-linking / initial navigation inside the active session
                                                LaunchedEffect(initialTransactionId) {
                                                    if (initialTransactionId != null) {
                                                        navController.navigate(Screen.AddTransaction(initialTransactionId))
                                                        onTransactionIdConsumed()
                                                    }
                                                }

                                                MainAppScaffold(
                                                    mainViewModel = mainViewModel,
                                                    onLogout = { authViewModel.logout() },
                                                    onUpdateToastPadding = { toastPadding = it }
                                                )
                                            }
                                        } else {
                                            // Entering the Auth "Room"
                                            LaunchedEffect(Unit) {
                                                toastPadding = 24.dp
                                            }
                                            AuthNavigation()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Global Toast centralized here
                authToast?.let { (message, isError) ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        MaterialToast(
                            message = message,
                            isError = isError,
                            onDismiss = { authViewModel.clearToast() },
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(bottom = toastPadding)
                        )
                    }
                }

                mainToast?.let { (message, isError) ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        MaterialToast(
                            message = message,
                            isError = isError,
                            onDismiss = { mainViewModel.clearToast() },
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(bottom = toastPadding)
                        )
                    }
                }
            }
        }
    }
}
