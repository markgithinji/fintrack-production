package com.fintrack.shared.feature.auth.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrack.shared.feature.auth.domain.model.AuthState
import com.fintrack.shared.feature.auth.domain.model.LoginFormState
import com.fintrack.shared.feature.auth.ui.common.FinanceTextField
import com.fintrack.shared.feature.auth.ui.common.SocialLoginButton
import com.fintrack.shared.feature.core.data.model.ApiException
import com.fintrack.shared.feature.core.data.model.getUserFriendlyMessage
import com.fintrack.shared.feature.core.domain.ValidationTrigger
import com.fintrack.shared.feature.core.ui.MaterialToast
import com.fintrack.shared.ui.theme.AuthGold
import com.fintrack.shared.ui.theme.AuthLinkText
import fintrack.shared.generated.resources.Res
import fintrack.shared.generated.resources.apple_signIn_icon
import fintrack.shared.generated.resources.fintrack_app_icon
import fintrack.shared.generated.resources.google_signIn_icon
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

@Composable
fun LoginScreen(
    loginState: AuthState<*>,
    formState: LoginFormState,
    toastMessage: Pair<String, Boolean>?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onValidateEmail: (ValidationTrigger) -> Unit,
    onValidatePassword: (ValidationTrigger) -> Unit,
    onLoginClick: () -> Unit,
    onShowToast: (String, Boolean) -> Unit,
    onClearToast: () -> Unit,
    onLoginSuccess: () -> Unit,
    onSignUp: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val emailFocusRequester = remember { FocusRequester() }

    // Inline error visibility
    var passwordVisible by remember { mutableStateOf(value = false) }

    val colorScheme = MaterialTheme.colorScheme

    // Automatically request focus on the email field when the screen opens
    LaunchedEffect(Unit) {
        delay(300) // Small delay to ensure UI is ready
        try {
            emailFocusRequester.requestFocus()
        } catch (_: Exception) {
            // Ignore focus request failures
        }
    }

    LaunchedEffect(loginState) {
        when (loginState) {
            is AuthState.Success -> {
                onLoginSuccess()
            }

            is AuthState.Error -> {
                val exception = loginState.exception
                onShowToast(
                    (exception as? ApiException)?.getUserFriendlyMessage() ?: exception.message
                    ?: "Login failed. Please try again.", true
                )
            }

            else -> Unit
        }
    }

    val validationErrorMessage = formState.activeError

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .verticalScroll(scrollState)
                .imePadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // 1. Logo and Title Area
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .shadow(4.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.fintrack_app_icon),
                        contentDescription = "FinTrack App",
                        modifier = Modifier
                            .size(64.dp)
                            .padding(4.dp),
                        contentScale = ContentScale.Fit,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Securely access your financial insights",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            // 2. Input Fields
            FinanceTextField(
                value = formState.email,
                onValueChange = onEmailChange,
                label = "Email Address",
                leadingIcon = Icons.Default.Email,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                colorScheme = colorScheme,
                isError = formState.emailError != null,
                errorMessage = null, // Consolidated in the error box
                onFocusChanged = { isFocused ->
                    if (!isFocused) onValidateEmail(ValidationTrigger.FocusLoss)
                },
                contentType = ContentType.EmailAddress,
                modifier = Modifier.focusRequester(emailFocusRequester)
            )

            Spacer(modifier = Modifier.height(20.dp))

            FinanceTextField(
                value = formState.password,
                onValueChange = onPasswordChange,
                label = "Password",
                leadingIcon = Icons.Default.Lock,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onLoginClick()
                    }
                ),
                isPassword = true,
                passwordVisible = passwordVisible,
                onPasswordToggle = { passwordVisible = !passwordVisible },
                colorScheme = colorScheme,
                isError = formState.passwordError != null,
                errorMessage = null, // Consolidated in the error box
                onFocusChanged = { isFocused ->
                    if (!isFocused) onValidatePassword(ValidationTrigger.FocusLoss)
                },
                contentType = ContentType.Password
            )

            // 3. Inline Validation Error
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Column {
                    AnimatedVisibility(
                        visible = validationErrorMessage != null,
                        enter = fadeIn() + slideInVertically { it / 2 },
                        exit = fadeOut(),
                    ) {
                        Text(
                            text = validationErrorMessage ?: "",
                            color = colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Forgot Password
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    text = "Forgot Password?",
                    color = AuthLinkText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onForgotPassword() },
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 5. Login Button
            val isLoggingIn = loginState is AuthState.Loading
            val isSuccess = loginState is AuthState.Success<*>

            Button(
                onClick = {
                    focusManager.clearFocus()
                    onLoginClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = if (formState.isFormValid) 2.dp else 0.dp,
                    pressedElevation = 8.dp,
                    disabledElevation = 0.dp,
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                    disabledContainerColor = if (isSuccess) AuthGold else colorScheme.primary.copy(
                        alpha = 0.5f
                    ),
                    disabledContentColor = if (isSuccess) colorScheme.onSecondary else colorScheme.onPrimary.copy(
                        alpha = 0.7f
                    ),
                ),
                enabled = !isLoggingIn && !isSuccess && (formState.email.isNotBlank() && formState.password.isNotBlank()),
            ) {
                when (loginState) {
                    is AuthState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp,
                            color = colorScheme.onPrimary.copy(alpha = 0.7f),
                        )
                    }

                    is AuthState.Success -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                "Success",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                            )
                        }
                    }

                    else -> {
                        Text(
                            "Sign In",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 6. 'or' Separator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = colorScheme.outlineVariant,
                )
                Text(
                    text = "Secure Connect",
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = colorScheme.outlineVariant,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 7. Social Login Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SocialLoginButton(
                    text = "Google",
                    iconResource = Res.drawable.google_signIn_icon,
                    onClick = { /* Google Login */ },
                    colorScheme = colorScheme,
                    modifier = Modifier.weight(1f),
                )
                SocialLoginButton(
                    text = "Apple",
                    iconResource = Res.drawable.apple_signIn_icon,
                    onClick = { /* Apple Login */ },
                    colorScheme = colorScheme,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // 8. Sign Up Link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "New to FinTrack?",
                    color = colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Create Account",
                    color = AuthLinkText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.clickable { onSignUp() },
                )
            }
        }

        // Screen-local Toast
        toastMessage?.let { (message, isError) ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                MaterialToast(
                    message = message,
                    isError = isError,
                    onDismiss = onClearToast
                )
            }
        }
    }
}
