package com.fintrack.shared.feature.core.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
actual fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String,
    isDestructive: Boolean,
    isLoading: Boolean,
    isSuccess: Boolean,
    errorMessage: String?,
    successTitle: String?,
    successMessage: String?,
    autoDismiss: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = if (isLoading || isSuccess) ({}) else onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Animated content for the middle section (Icon, Title, Message)
                AnimatedContent(
                    targetState = Triple(isSuccess, errorMessage != null, title),
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(60)) + scaleIn(
                            initialScale = 0.95f,
                            animationSpec = tween(60)
                        ))
                            .togetherWith(fadeOut(animationSpec = tween(40)))
                    },
                    label = "dialogContent"
                ) { (success, error, currentTitle) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Icon Circle
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    color = when {
                                        success -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                        error -> MaterialTheme.colorScheme.errorContainer
                                        isDestructive -> MaterialTheme.colorScheme.errorContainer.copy(
                                            alpha = 0.8f
                                        )

                                        else -> MaterialTheme.colorScheme.primaryContainer
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    success -> Icons.Default.CheckCircle
                                    error -> Icons.Default.Error
                                    else -> Icons.Default.Warning
                                },
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = when {
                                    success -> Color(0xFF4CAF50)
                                    error -> MaterialTheme.colorScheme.error
                                    isDestructive -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = when {
                                success -> (successTitle ?: "Success!")
                                error -> "Operation Failed"
                                else -> currentTitle
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = when {
                                success -> (successMessage ?: "Action completed successfully.")
                                error -> errorMessage ?: "Unknown error occurred"
                                else -> message
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Bottom Buttons with their own transition
                AnimatedContent(
                    targetState = isLoading to (isSuccess || errorMessage != null),
                    transitionSpec = {
                        (fadeIn(tween(60))).togetherWith(fadeOut(tween(40)))
                    },
                    label = "dialogButtons"
                ) { (loading, finished) ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (finished) {
                                    onDismiss()
                                } else {
                                    onConfirm()
                                    if (autoDismiss && !loading) {
                                        onDismiss()
                                    }
                                }
                            },
                            enabled = !loading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    isSuccess -> Color(0xFF4CAF50)
                                    errorMessage != null || isDestructive -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                        ) {
                            if (loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    text = when {
                                        isSuccess -> "Done"
                                        errorMessage != null -> "Close"
                                        else -> confirmLabel
                                    },
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        if (!finished) {
                            TextButton(
                                onClick = onDismiss,
                                enabled = !loading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = cancelLabel,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
