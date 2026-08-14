package com.fintrack.shared.feature.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrack.shared.feature.core.data.model.getUserFriendlyMessage

@Composable
fun CommonErrorState(
    modifier: Modifier = Modifier,
    title: String = "Something went wrong",
    error: Throwable? = null,
    errorMessage: String? = null,
    isLoading: Boolean = false,
    isSuccess: Boolean = false,
    useOnPrimaryColors: Boolean = false,
    onRetry: (() -> Unit)? = null
) {
    val displayMessage = remember(error, errorMessage, isSuccess) {
        if (isSuccess) errorMessage ?: "Action completed successfully!"
        else errorMessage ?: error?.getUserFriendlyMessage() ?: "An unexpected error occurred"
    }

    val baseColor = if (useOnPrimaryColors) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        if (isSuccess) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    }

    val stateIcon = if (isSuccess) Icons.Default.CheckCircle else Icons.Outlined.Error

    Box(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = baseColor.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = stateIcon,
                    contentDescription = if (isSuccess) "Success" else "Error",
                    tint = baseColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = title,
                color = baseColor,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = displayMessage,
                color = if (useOnPrimaryColors) baseColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(24.dp))
                
                if (useOnPrimaryColors) {
                    Button(
                        onClick = onRetry,
                        enabled = !isLoading && !isSuccess,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = baseColor.copy(alpha = 0.2f),
                            contentColor = baseColor,
                            disabledContainerColor = baseColor.copy(alpha = 0.1f),
                            disabledContentColor = baseColor.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, baseColor.copy(alpha = 0.3f)),
                        modifier = Modifier.height(40.dp).widthIn(min = 120.dp)
                    ) {
                        RetryButtonContent(isLoading, isSuccess)
                    }
                } else {
                    Button(
                        onClick = onRetry,
                        enabled = !isLoading && !isSuccess,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        ),
                        modifier = Modifier.height(40.dp).widthIn(min = 120.dp)
                    ) {
                        RetryButtonContent(isLoading, isSuccess)
                    }
                }
            }
        }
    }
}

@Composable
private fun RetryButtonContent(isLoading: Boolean, isSuccess: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current
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
                    Text("Success", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            else -> {
                Text("Retry", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
