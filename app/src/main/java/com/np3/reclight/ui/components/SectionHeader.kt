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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SectionHeader(
    title: String,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(accentColor)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = MaterialTheme.typography.labelLarge.letterSpacing * 1.5
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            repeat(20) { index ->
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(
                            MaterialTheme.colorScheme.outline.copy(alpha = 1f - (index * 0.05f).coerceIn(0f, 0.8f))
                        )
                )
            }
        }
    }
}
