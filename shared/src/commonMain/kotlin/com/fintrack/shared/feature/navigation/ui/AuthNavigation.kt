package com.fintrack.shared.feature.navigation.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fintrack.shared.feature.auth.ui.AuthViewModel
import com.fintrack.shared.feature.auth.ui.LoginScreen
import com.fintrack.shared.feature.auth.ui.RegisterScreen
import com.fintrack.shared.feature.navigation.model.Screen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthNavigation(
    paddingValues: PaddingValues = PaddingValues(),
    viewModel: AuthViewModel = koinViewModel()
) {
    val navController = rememberNavController()
    
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()
    val loginFormState by viewModel.loginFormState.collectAsStateWithLifecycle()
    
    val registerState by viewModel.registerState.collectAsStateWithLifecycle()
    val registerFormState by viewModel.registerFormState.collectAsStateWithLifecycle()
    
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = Screen.Login,
        modifier = Modifier.fillMaxSize(),
        enterTransition = { fadeIn(animationSpec = tween(250)) },
        exitTransition = { fadeOut(animationSpec = tween(250)) }
    ) {
        composable<Screen.Login> {
            LoginScreen(
                loginState = loginState,
                formState = loginFormState,
                toastMessage = toastMessage,
                onEmailChange = viewModel::updateLoginEmail,
                onPasswordChange = viewModel::updateLoginPassword,
                onValidateEmail = viewModel::validateLoginEmail,
                onValidatePassword = viewModel::validateLoginPassword,
                onLoginClick = viewModel::login,
                onShowToast = viewModel::showToast,
                onClearToast = viewModel::clearToast,
                onLoginSuccess = {
                    // Handled globally via authStatus update in ViewModel
                },
                onSignUp = {
                    viewModel.clearAuthStates()
                    navController.navigate(Screen.Register) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                },
                onForgotPassword = {
                    // TODO: Handle forgot password
                },
                modifier = Modifier.padding(paddingValues)
            )
        }

        composable<Screen.Register> {
            RegisterScreen(
                registerState = registerState,
                formState = registerFormState,
                toastMessage = toastMessage,
                onNameChange = viewModel::updateName,
                onEmailChange = viewModel::updateEmail,
                onPasswordChange = viewModel::updatePassword,
                onConfirmPasswordChange = viewModel::updateConfirmPassword,
                onValidateName = viewModel::validateName,
                onValidateEmail = viewModel::validateEmail,
                onValidatePassword = viewModel::validatePassword,
                onValidateConfirmPassword = viewModel::validateConfirmPassword,
                onRegisterClick = viewModel::register,
                onShowToast = viewModel::showToast,
                onClearToast = viewModel::clearToast,
                onRegisterSuccess = {
                    // Handled globally via authStatus update in ViewModel
                },
                onLogin = {
                    viewModel.clearAuthStates()
                    navController.navigate(Screen.Login) {
                        popUpTo(Screen.Register) { inclusive = true }
                    }
                },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}
