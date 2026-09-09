package com.fintrack.shared.feature.core.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.fintrack.shared.feature.core.ui.util.ThousandsSeparatorOffsetMapping
import com.fintrack.shared.feature.navigation.ui.LocalCurrency

@Composable
fun FinanceAmountHeader(
    amount: String,
    selectionStart: Int,
    selectionEnd: Int,
    onSelectionChange: (Int, Int) -> Unit,
    label: String,
    isIncome: Boolean,
    themeColor: Color,
    onToggleNumpad: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    isActive: Boolean = false
) {
    val amountFontSize by animateDpAsState(
        targetValue = when {
            amount.length >= 10 -> 32.dp
            amount.length >= 8 -> 38.dp
            amount.length >= 6 -> 44.dp
            else -> 48.dp
        }.value.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    val contentColor = if (isIncome) MaterialTheme.colorScheme.onTertiary else Color.White

    Surface(
        color = themeColor,
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp + paddingValues.calculateTopPadding())
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onToggleNumpad(true) }
            ),
        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = label,
                transitionSpec = {
                    (slideInVertically { height -> height } + fadeIn()).togetherWith(
                        slideOutVertically { height -> -height } + fadeOut())
                }
            ) { targetLabel ->
                Text(
                    text = targetLabel,
                    color = contentColor.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.animateContentSize()
                ) {
                    Text(
                        text = LocalCurrency.current.symbol,
                        color = contentColor.copy(alpha = 0.7f),
                        fontSize = (amountFontSize.value * 0.5f).sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = (amountFontSize.value * 0.2f).dp, end = 8.dp)
                    )

                    Box(contentAlignment = Alignment.CenterStart) {
                        var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                        
                        val infiniteTransition = rememberInfiniteTransition(label = "cursor")
                        val cursorAlpha by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 500),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "cursorAlpha"
                        )

                        val transformedAmount = remember(amount) {
                            if (amount.isEmpty()) return@remember "0"
                            val parts = amount.split(".")
                            val integerPart = parts[0].reversed().chunked(3).joinToString(",").reversed()
                            val decimalPart = if (parts.size > 1) "." + parts[1] else ""
                            (if (integerPart.isEmpty() && decimalPart.isNotEmpty()) "0" else integerPart) + decimalPart
                        }

                        // We need the mapping to handle taps correctly
                        val offsetMapping = remember(amount) {
                            ThousandsSeparatorOffsetMapping(amount)
                        }

                        Text(
                            text = transformedAmount,
                            style = TextStyle(
                                color = if (amount.isEmpty()) contentColor.copy(alpha = 0.4f) else contentColor,
                                fontSize = amountFontSize.value.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Start,
                                letterSpacing = 0.sp
                            ),
                            onTextLayout = { textLayoutResult = it },
                            modifier = Modifier
                                .pointerInput(amount) {
                                    detectTapGestures { offset ->
                                        textLayoutResult?.let { layout ->
                                            val transformedIndex = layout.getOffsetForPosition(offset)
                                            val originalIndex = offsetMapping.transformedToOriginal(transformedIndex)
                                            onSelectionChange(originalIndex, originalIndex)
                                            onToggleNumpad(true)
                                        }
                                    }
                                }
                        )

                        // Custom Cursor
                        if (isActive) {
                            Canvas(modifier = Modifier.matchParentSize()) {
                                textLayoutResult?.let { layout ->
                                    val transformedIndex = offsetMapping.originalToTransformed(selectionStart)
                                    val cursorRect = layout.getCursorRect(transformedIndex.coerceIn(0, transformedAmount.length))
                                    
                                    drawLine(
                                        color = contentColor.copy(alpha = cursorAlpha),
                                        start = Offset(cursorRect.left, cursorRect.top + 4.dp.toPx()),
                                        end = Offset(cursorRect.left, cursorRect.bottom - 4.dp.toPx()),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
