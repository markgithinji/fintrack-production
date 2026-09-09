package com.fintrack.shared.feature.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrack.shared.feature.core.domain.SaveState

@Composable
fun FinanceSaveButton(
    saveState: SaveState<*>,
    isFormValid: Boolean,
    themeColor: Color,
    contentColor: Color,
    onSaveClick: () -> Unit,
    label: String,
    successLabel: String = "Saved",
    modifier: Modifier = Modifier
) {
    val isInProgress = saveState is SaveState.Loading
    val isSuccess = saveState is SaveState.Success<*>

    Button(
        onClick = onSaveClick,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isFormValid || isSuccess) themeColor else themeColor.copy(alpha = 0.5f),
            contentColor = if (isFormValid || isSuccess) contentColor else contentColor.copy(alpha = 0.5f),
            disabledContainerColor = if (isSuccess) themeColor else themeColor.copy(alpha = 0.5f),
            disabledContentColor = if (isSuccess) contentColor else contentColor.copy(alpha = 0.5f)
        ),
        enabled = !isInProgress && !isSuccess
    ) {
        if (isInProgress) {
            CircularProgressIndicator(
                color = contentColor,
                modifier = Modifier.size(22.dp)
            )
        } else if (isSuccess) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = successLabel,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        } else {
            Text(
                text = label,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
    }
}
