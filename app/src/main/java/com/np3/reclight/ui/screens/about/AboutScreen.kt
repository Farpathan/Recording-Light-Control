package com.np3.reclight.ui.screens.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.np3.reclight.R
import com.np3.reclight.ui.components.*

@Composable
fun AboutScreen(
    materialYouEnabled: Boolean,
    onMaterialYouToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        
        // Hero section with animated gradient
        GradientBox(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "Recording Light Control",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                StatusBadge(
                    text = "v1.1.0",
                    type = StatusType.NEUTRAL
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Control the Recording LED on your Nothing Phone (3). " +
                           "Supports static, blinking, and breathing modes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Settings section
        SectionHeader(title = "Settings", accentColor = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        
        SettingsGroup {
            SettingsItem(
                title = "Material You",
                subtitle = "Use system dynamic colors",
                icon = Icons.Default.Palette,
                trailing = {
                    NothingSwitch(
                        checked = materialYouEnabled,
                        onCheckedChange = onMaterialYouToggle
                    )
                }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Links section
        SectionHeader(title = "Links", accentColor = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        
        SettingsGroup {
            SettingsItem(
                title = "GitHub Repository",
                subtitle = "View source code",
                icon = null,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Farpathan/Recording-Light-Control"))
                    context.startActivity(intent)
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_github),
                        contentDescription = "GitHub",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            )
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}
