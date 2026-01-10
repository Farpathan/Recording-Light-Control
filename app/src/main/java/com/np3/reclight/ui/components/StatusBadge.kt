package com.np3.reclight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.np3.reclight.ui.theme.*

enum class StatusType {
    SUCCESS, WARNING, ERROR, NEUTRAL
}

@Composable
fun StatusBadge(
    text: String,
    type: StatusType,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (type) {
        StatusType.SUCCESS -> StatusSuccess.copy(alpha = 0.15f) to StatusSuccess
        StatusType.WARNING -> StatusWarning.copy(alpha = 0.15f) to StatusWarning
        StatusType.ERROR -> StatusError.copy(alpha = 0.15f) to StatusError
        StatusType.NEUTRAL -> DarkCardVariant to TextSecondary
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}

@Composable
fun StatusIndicator(
    isActive: Boolean,
    activeText: String = "Active",
    inactiveText: String = "Inactive",
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isActive) StatusSuccess else StatusError)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isActive) activeText else inactiveText,
            style = MaterialTheme.typography.bodySmall,
            color = if (isActive) StatusSuccess else StatusError
        )
    }
}
