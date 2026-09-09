package com.fintrack.shared.feature.auth.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.border
import androidx.compose.material3.TextField
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun FinanceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    keyboardType: KeyboardType,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordToggle: () -> Unit = {},
    colorScheme: ColorScheme,
    isError: Boolean = false,
    errorMessage: String? = null,
    onFocusChanged: (Boolean) -> Unit = {},
    contentType: ContentType? = null,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    
    // Track if the field has been touched to avoid premature validation logs
    var isTouched by remember { mutableStateOf(false) }

    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = 1.dp,
                color = if (isError) colorScheme.error else colorScheme.outline.copy(alpha = 0.2f),
                shape = shape
            )
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    isTouched = true
                }
                // Only propagate focus loss if the field was actually touched/focused before
                if (!focusState.isFocused && !isTouched) {
                    return@onFocusChanged
                }
                onFocusChanged(focusState.isFocused)
            }
            .then(
                if (contentType != null) {
                    Modifier.semantics { this.contentType = contentType }
                } else Modifier
            ),
        label = {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = {
            Icon(
                leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onPasswordToggle) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
            autoCorrectEnabled = !isPassword && keyboardType != KeyboardType.Email,
            capitalization = when (keyboardType) {
                KeyboardType.Text -> KeyboardCapitalization.Words
                else -> KeyboardCapitalization.None
            }
        ),
        keyboardActions = keyboardActions,
        singleLine = true,
        shape = shape,
        colors = TextFieldDefaults.colors(
            focusedTextColor = colorScheme.onSurface,
            unfocusedTextColor = colorScheme.onSurface,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedLabelColor = colorScheme.primary,
            unfocusedLabelColor = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            focusedLeadingIconColor = colorScheme.primary,
            unfocusedLeadingIconColor = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            focusedTrailingIconColor = colorScheme.onSurfaceVariant,
            unfocusedTrailingIconColor = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            cursorColor = colorScheme.primary,
            focusedIndicatorColor = Color.Transparent, // Hide default bottom line
            unfocusedIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            errorLabelColor = colorScheme.error,
            errorLeadingIconColor = colorScheme.error,
            errorTrailingIconColor = colorScheme.error,
            errorContainerColor = Color.Transparent
        ),
        isError = isError,
        supportingText = if (isError && errorMessage != null) {
            {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.error
                )
            }
        } else null
    )
}
