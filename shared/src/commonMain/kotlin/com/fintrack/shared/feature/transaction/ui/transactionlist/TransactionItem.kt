package com.fintrack.shared.feature.transaction.ui.transactionlist

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.model.fromId
import com.fintrack.shared.feature.category.ui.util.toColor
import com.fintrack.shared.feature.category.ui.util.toIcon
import com.fintrack.shared.feature.core.ui.LocalSharedTransitionScope
import com.fintrack.shared.feature.navigation.ui.LocalTimeFormat
import com.fintrack.shared.feature.navigation.ui.toCurrencyString
import com.fintrack.shared.feature.settings.domain.util.format
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import com.fintrack.shared.ui.theme.GreenIncome
import com.fintrack.shared.ui.theme.PinkExpense
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TransactionItem(
    transaction: Transaction,
    animatedVisibilityScope: AnimatedVisibilityScope,
    includeFees: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val category = remember(transaction.categoryId, transaction.category, transaction.isIncome) {
        Category.fromId(
            transaction.categoryId,
            name = transaction.category,
            isExpense = !transaction.isIncome
        )
    }
    val amountColor = if (transaction.isIncome) GreenIncome else PinkExpense
    val timeFormat = LocalTimeFormat.current
    val localDateTime = transaction.dateTime.toLocalDateTime(TimeZone.currentSystemDefault())
    val formattedTime = localDateTime.time.format(timeFormat)
    val sharedTransitionScope = LocalSharedTransitionScope.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (sharedTransitionScope != null) {
                    with(sharedTransitionScope) {
                        Modifier.sharedBounds(
                            rememberSharedContentState(key = "transaction_header_${transaction.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ ->
                                spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            },
                            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(20.dp))
                        )
                    }
                } else Modifier
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(category.toColor().copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = category.toIcon(),
                        contentDescription = category.name,
                        tint = category.toColor(),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    val detailText = remember(transaction.description, formattedTime) {
                        if (!transaction.description.isNullOrBlank()) {
                            transaction.description
                        } else {
                            formattedTime
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = detailText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                val displayAmount = remember(transaction, includeFees) {
                    if (includeFees) transaction.totalAmount else transaction.amount
                }
                
                Text(
                    text = "${if (transaction.isIncome) "+" else "-"}${displayAmount.toCurrencyString()}",
                    color = amountColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (transaction.transactionCost > BigDecimal.ZERO && includeFees) {
                    Text(
                        text = "Incl. ${transaction.transactionCost.toCurrencyString()} fee",
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                }
                
                if (transaction.balance != null) {
                    Text(
                        text = "Bal: ${transaction.balance.toCurrencyString()}",
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                
                if (!transaction.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}