package com.fintrack.shared.feature.user.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fintrack.shared.feature.core.ui.AnimatedShimmerBox
import com.fintrack.shared.feature.core.ui.ConfirmationDialog
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.navigation.ui.toCurrencyString
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: ProfileViewModel = koinViewModel(),
    onNavigateToAccounts: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val metricsResult by viewModel.metricsState.collectAsStateWithLifecycle()
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshProfile()
    }

    if (showLogoutConfirmation) {
        ConfirmationDialog(
            title = "Logout",
            message = "Are you sure you want to logout? You will need to sign in again to access your data.",
            confirmLabel = "Logout",
            isDestructive = true,
            onConfirm = onLogout,
            onDismiss = { showLogoutConfirmation = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val topPadding = paddingValues.calculateTopPadding()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                top = topPadding + 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            )
        ) {
            item {
                // Profile Header Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Surface(
                                modifier = Modifier
                                    .size(80.dp)
                                    .padding(4.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(42.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Edit Icon Overlay
                            Surface(
                                modifier = Modifier
                                    .size(30.dp)
                                    .offset(x = (-2).dp, y = (-2).dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                onClick = onNavigateToEditProfile,
                                shadowElevation = 3.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Profile",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = metricsResult,
                                transitionSpec = {
                                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) togetherWith
                                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
                                },
                                label = "ProfileDetails"
                            ) { state ->
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    when (state) {
                                        is Result.Loading -> {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                AnimatedShimmerBox(
                                                    modifier = Modifier
                                                        .width(140.dp)
                                                        .height(20.dp)
                                                        .clip(RoundedCornerShape(8.dp)),
                                                    shimmerColors = listOf(
                                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.05f),
                                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                                                    )
                                                )
                                                AnimatedShimmerBox(
                                                    modifier = Modifier
                                                        .width(180.dp)
                                                        .height(12.dp)
                                                        .clip(RoundedCornerShape(8.dp)),
                                                    shimmerColors = listOf(
                                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.04f),
                                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                                                    )
                                                )
                                            }
                                        }

                                        is Result.Error -> {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = "Connection Error",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                                TextButton(
                                                    onClick = { viewModel.refreshProfile() },
                                                    modifier = Modifier.height(28.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Refresh,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Try Again", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }

                                        is Result.Success -> {
                                            val profile = state.data
                                            Text(
                                                text = profile.name,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center
                                            )

                                            Text(
                                                text = profile.email,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Insights Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isLoading = metricsResult is Result.Loading
                            val metrics = (metricsResult as? Result.Success)?.data
                            val netWorth = metrics?.netWorth ?: BigDecimal.ZERO
                            val savingsRate = metrics?.savingsRate
                            val essentialRatio = metrics?.essentialSpendRatio

                            ProfileMetricItem(
                                label = "Net Worth",
                                value = netWorth.toCurrencyString(),
                                icon = Icons.Default.AccountBalanceWallet,
                                isLoading = isLoading,
                                modifier = Modifier.weight(1f)
                            )

                            VerticalDivider(
                                modifier = Modifier.height(32.dp).padding(horizontal = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            ProfileMetricItem(
                                label = "Savings",
                                value = if (savingsRate != null) "${savingsRate.intValue(false)}%" else "--",
                                icon = Icons.AutoMirrored.Filled.TrendingUp,
                                isLoading = isLoading,
                                modifier = Modifier.weight(1f)
                            )

                            VerticalDivider(
                                modifier = Modifier.height(32.dp).padding(horizontal = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            ProfileMetricItem(
                                label = "Needs",
                                value = if (essentialRatio != null) "${essentialRatio.intValue(false)}%" else "--",
                                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                isLoading = isLoading,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Financial Management Section
            item { SectionHeader("Account Management") }
            item {
                ProfileOptionItem(
                    title = "Accounts",
                    icon = Icons.Default.AccountBalance,
                    description = "View and manage your bank accounts",
                    onClick = onNavigateToAccounts
                )
            }
            item {
                ProfileOptionItem(
                    title = "Categories",
                    icon = Icons.Default.Category,
                    description = "Organize your transaction types",
                    onClick = onNavigateToCategories
                )
            }

            // App Section
            item { SectionHeader("Application") }
            item {
                ProfileOptionItem(
                    title = "Settings",
                    icon = Icons.Default.Settings,
                    description = "Preferences, security, and app data",
                    onClick = onNavigateToSettings
                )
            }

            item {
                ProfileOptionItem(
                    title = "Log Out",
                    icon = Icons.AutoMirrored.Filled.Logout,
                    description = "Securely sign out of your account",
                    onClick = { showLogoutConfirmation = true },
                    isDanger = true
                )
            }
        }
    }
}

@Composable
private fun ProfileMetricItem(
    label: String,
    value: String,
    icon: ImageVector,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.height(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        if (isLoading) {
            AnimatedShimmerBox(
                modifier = Modifier
                    .width(48.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp)),
                shimmerColors = listOf(
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.04f),
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                )
            )
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileOptionItem(
    title: String,
    icon: ImageVector,
    description: String,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    val contentColor = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val containerColor = if (isDanger) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = contentColor.copy(alpha = 0.08f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = contentColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDanger) MaterialTheme.colorScheme.error.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isDanger) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}
