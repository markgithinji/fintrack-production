package com.fintrack.shared.feature.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.ui.util.toColor
import com.fintrack.shared.feature.category.ui.util.toIcon

@Composable
fun FinanceCategorySelection(
    label: String,
    categories: List<Category>,
    selectedCategories: Set<Category>,
    onCategorySelectionChange: (Set<Category>) -> Unit,
    isExpense: Boolean,
    modifier: Modifier = Modifier,
    multiSelect: Boolean = false
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth().height(180.dp)
        ) {
            LazyHorizontalStaggeredGrid(
                rows = StaggeredGridCells.Adaptive(48.dp),
                horizontalItemSpacing = 8.dp,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.fillMaxSize().padding(vertical = 8.dp)
            ) {
                val filteredCategories = categories.filter { it.isExpense == isExpense }

                if (multiSelect) {
                    item {
                        val allSelected = selectedCategories.size == filteredCategories.size
                        FinanceCategoryChip(
                            text = "All",
                            icon = Icons.Default.SelectAll,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            selected = allSelected,
                            isIncome = !isExpense,
                            onClick = {
                                val newSelection = if (allSelected) {
                                    emptySet()
                                } else {
                                    filteredCategories.toSet()
                                }
                                onCategorySelectionChange(newSelection)
                            }
                        )
                    }
                }

                items(filteredCategories.size) { index ->
                    val cat = filteredCategories[index]
                    val selected = selectedCategories.any { it.id == cat.id }
                    FinanceCategoryChip(
                        text = cat.name,
                        icon = cat.toIcon(),
                        color = cat.toColor(),
                        selected = selected,
                        isIncome = !cat.isExpense,
                        onClick = {
                            if (multiSelect) {
                                val newSelection = if (selected) {
                                    selectedCategories.filter { it.id != cat.id }.toSet()
                                } else {
                                    selectedCategories + cat
                                }
                                onCategorySelectionChange(newSelection)
                            } else {
                                onCategorySelectionChange(if (selected) emptySet() else setOf(cat))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FinanceCategoryChip(
    text: String,
    icon: ImageVector,
    color: Color,
    selected: Boolean,
    isIncome: Boolean,
    onClick: () -> Unit
) {
    val animatedBgColor by animateColorAsState(
        targetValue = if (selected) color else MaterialTheme.colorScheme.surface,
        animationSpec = tween(300)
    )
    
    val selectedContentColor = if (isIncome) MaterialTheme.colorScheme.onTertiary else Color.White
    
    val animatedContentColor by animateColorAsState(
        targetValue = if (selected) selectedContentColor else color,
        animationSpec = tween(300)
    )
    val animatedTextColor by animateColorAsState(
        targetValue = if (selected) selectedContentColor else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(300)
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = animatedBgColor,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = Modifier.graphicsLayer {
            scaleX = if (selected) 1.05f else 1f
            scaleY = if (selected) 1.05f else 1f
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, tint = animatedContentColor, modifier = Modifier.size(18.dp))
            Text(text, color = animatedTextColor, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
