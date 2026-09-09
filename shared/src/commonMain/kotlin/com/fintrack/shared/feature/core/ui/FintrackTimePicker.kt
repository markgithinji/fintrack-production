package com.fintrack.shared.feature.core.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fintrack.shared.feature.settings.domain.model.TimeFormat
import kotlinx.datetime.LocalTime

@Composable
fun FintrackTimePickerDialog(
    initialTime: LocalTime?,
    timeFormat: TimeFormat = TimeFormat.TWENTY_FOUR_HOUR,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val baseTime = initialTime ?: LocalTime(12, 0)
    val is24Hour = timeFormat == TimeFormat.TWENTY_FOUR_HOUR
    
    // Convert 24h to 12h format if needed
    var hour by remember { 
        val h = baseTime.hour
        if (is24Hour) {
            mutableStateOf(h)
        } else {
            mutableStateOf(if (h == 0 || h == 12) 12 else h % 12) 
        }
    }
    var isAm by remember { mutableStateOf(baseTime.hour < 12) }
    var minute by remember { mutableStateOf(baseTime.minute) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Time",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimeUnitPicker(
                        label = "Hour",
                        value = hour,
                        range = if (is24Hour) 0..23 else 1..12,
                        onValueChange = { hour = it }
                    )
                    
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 24.dp)
                    )

                    TimeUnitPicker(
                        label = "Minute",
                        value = minute,
                        range = 0..59,
                        onValueChange = { minute = it }
                    )

                    if (!is24Hour) {
                        Spacer(modifier = Modifier.width(16.dp))

                        // AM/PM Selector
                        Column(
                            modifier = Modifier.padding(top = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AmPmButton(
                                text = "AM",
                                isSelected = isAm,
                                onClick = { isAm = true }
                            )
                            AmPmButton(
                                text = "PM",
                                isSelected = !isAm,
                                onClick = { isAm = false }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalHour = if (is24Hour) {
                                hour
                            } else {
                                // Convert back to 24h format
                                when {
                                    isAm && hour == 12 -> 0
                                    !isAm && hour == 12 -> 12
                                    !isAm -> hour + 12
                                    else -> hour
                                }
                            }
                            onTimeSelected(LocalTime(finalHour, minute))
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

@Composable
private fun AmPmButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 50.dp, height = 36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary 
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun TimeUnitPicker(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        IconButton(
            onClick = {
                val next = if (value < range.last) value + 1 else range.first
                onValueChange(next)
            },
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Increase",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Box(
            modifier = Modifier
                .width(64.dp)
                .height(56.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value.toString().padStart(2, '0'),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        }

        IconButton(
            onClick = {
                val prev = if (value > range.first) value - 1 else range.last
                onValueChange(prev)
            },
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
        ) {
            Icon(
                Icons.Default.Remove,
                contentDescription = "Decrease",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
