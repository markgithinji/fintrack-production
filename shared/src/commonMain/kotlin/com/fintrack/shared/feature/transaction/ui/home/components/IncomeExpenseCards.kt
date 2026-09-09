package com.fintrack.shared.feature.transaction.ui.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.navigation.ui.toCurrencyString
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.ui.AnimatedShimmerBox
import com.fintrack.shared.feature.core.ui.LocalSharedTransitionScope
import com.fintrack.shared.ui.theme.GreenIncome
import com.fintrack.shared.ui.theme.PinkExpense
import com.ionspin.kotlin.bignum.decimal.BigDecimal

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun IncomeExpenseCards(
    accountResult: Result<Account>,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onCardClick: (isIncome: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var lastAccount by remember { mutableStateOf<Account?>(null) }
    if (accountResult is Result.Success) {
        lastAccount = accountResult.data
    }

    AnimatedContent(
        targetState = accountResult,
        transitionSpec = {
            fadeIn(animationSpec = tween(500)) togetherWith
                    fadeOut(animationSpec = tween(500))
        },
        label = "IncomeExpenseCardsContent",
        modifier = modifier.fillMaxWidth()
    ) { result ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (result) {
                is Result.Loading -> {
                    val currentData = lastAccount
                    if (currentData != null) {
                        SuccessCards(
                            account = currentData,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onCardClick = onCardClick
                        )
                    } else {
                        LoadingInfoCard(modifier = Modifier.weight(1f))
                        LoadingInfoCard(modifier = Modifier.weight(1f))
                    }
                }

                is Result.Error -> {
                    InfoCard(
                        title = "Total Income",
                        amount = "Error",
                        isIncomeCard = true,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = null,
                        modifier = Modifier.weight(1f)
                    )
                    InfoCard(
                        title = "Total Expense",
                        amount = "Error",
                        isIncomeCard = false,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = null,
                        modifier = Modifier.weight(1f)
                    )
                }

                is Result.Success -> {
                    SuccessCards(
                        account = result.data,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onCardClick = onCardClick
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun RowScope.SuccessCards(
    account: Account,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onCardClick: (isIncome: Boolean) -> Unit
) {
    val totalIncome = account.income ?: BigDecimal.ZERO
    val totalExpense = account.expense ?: BigDecimal.ZERO

    InfoCard(
        title = "Total Income",
        amount = totalIncome.toCurrencyString(),
        isIncomeCard = true,
        animatedVisibilityScope = animatedVisibilityScope,
        onClick = { onCardClick(true) },
        modifier = Modifier.weight(1f)
    )

    InfoCard(
        title = "Total Expense",
        amount = totalExpense.toCurrencyString(),
        isIncomeCard = false,
        animatedVisibilityScope = animatedVisibilityScope,
        onClick = { onCardClick(false) },
        modifier = Modifier.weight(1f)
    )
}

@Composable
fun LoadingInfoCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(70.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedShimmerBox(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                AnimatedShimmerBox(
                    modifier = Modifier
                        .width(60.dp)
                        .height(12.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                AnimatedShimmerBox(
                    modifier = Modifier
                        .width(80.dp)
                        .height(15.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun InfoCard(
    title: String,
    amount: String,
    isIncomeCard: Boolean,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: ((isIncome: Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current

    Card(
        modifier = modifier
            .height(70.dp)
            .then(
                if (sharedTransitionScope != null) {
                    with(sharedTransitionScope) {
                        Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = if (isIncomeCard) "income_card" else "expense_card"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(16.dp))
                        )
                    }
                } else Modifier
            )
            .clickable(enabled = onClick != null) { onClick?.invoke(isIncomeCard) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isIncomeCard) GreenIncome else PinkExpense),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncomeCard) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = if (isIncomeCard) "Income" else "Expense",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp).rotate(if (isIncomeCard) 135f else -135f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

