package com.fintrack.shared.feature.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fintrack.shared.feature.core.data.model.getUserFriendlyMessage

@Composable
fun CommonErrorState(
    modifier: Modifier = Modifier,
    title: String = "Something went wrong",
    error: Throwable? = null,
    errorMessage: String? = null,
    isLoading: Boolean = false,
    isSuccess: Boolean = false,
    onRetry: (() -> Unit)? = null
) {
    val displayMessage = remember(error, errorMessage, isSuccess) {
        if (isSuccess) errorMessage ?: "Action completed successfully!"
        else errorMessage ?: error?.getUserFriendlyMessage() ?: "An unexpected error occurred"
    }

    val stateColor = if (isSuccess) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    val stateIcon = if (isSuccess) Icons.Default.CheckCircle else Icons.Outlined.Error

    Box(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = stateIcon,
                contentDescription = if (isSuccess) "Success" else "Error",
                tint = stateColor,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                color = stateColor,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = displayMessage,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
            
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRetry,
                    enabled = !isLoading && !isSuccess,
                    modifier = Modifier.height(44.dp).widthIn(min = 120.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = if (isSuccess) stateColor else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        disabledContentColor = if (isSuccess) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        when {
                            isLoading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            isSuccess -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("Success", fontWeight = FontWeight.Bold)
                                }
                            }
                            else -> {
                                Text("Retry", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
