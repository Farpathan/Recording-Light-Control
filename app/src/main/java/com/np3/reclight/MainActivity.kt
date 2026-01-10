package com.np3.reclight

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.np3.reclight.ui.navigation.*
import com.np3.reclight.ui.screens.about.AboutScreen
import com.np3.reclight.ui.screens.home.HomeScreen
import com.np3.reclight.ui.theme.*
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity(), Shizuku.OnRequestPermissionResultListener {

    private var showRestartPrompt by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        try {
            Shizuku.addRequestPermissionResultListener(this)
        } catch (e: Exception) {
            // Shizuku not installed or error
        }
        
        setContent {
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            var materialYouEnabled by remember { 
                mutableStateOf(prefs.getBoolean("material_you_enabled", false)) 
            }
            
            RecordingLightControlTheme(
                dynamicColor = materialYouEnabled
            ) {
                MainApp(
                    showRestartPrompt = showRestartPrompt,
                    onRestartApp = {
                        finish()
                        Process.killProcess(Process.myPid()) 
                    },
                    materialYouEnabled = materialYouEnabled,
                    onMaterialYouToggle = { enabled ->
                        materialYouEnabled = enabled
                        prefs.edit().putBoolean("material_you_enabled", enabled).apply()
                    }
                )
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            Shizuku.removeRequestPermissionResultListener(this)
        } catch (e: Exception) {}
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                showRestartPrompt = true
            }
        }
    }

    companion object {
        const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MainApp(
    showRestartPrompt: Boolean,
    onRestartApp: () -> Unit,
    materialYouEnabled: Boolean,
    onMaterialYouToggle: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { bottomNavItems.size })
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    
    // First launch popup state
    var showFirstLaunchDialog by remember { 
        mutableStateOf(!prefs.getBoolean("first_launch_shown", false))
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main content with swipe navigation
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> HomeScreen()
                    1 -> AboutScreen(
                        materialYouEnabled = materialYouEnabled,
                        onMaterialYouToggle = onMaterialYouToggle
                    )
                }
            }
            
            // Bottom navigation
            BottomNavBar(
                items = bottomNavItems,
                currentRoute = bottomNavItems.getOrNull(pagerState.currentPage)?.route,
                onItemClick = { screen ->
                    val targetPage = bottomNavItems.indexOf(screen)
                    if (targetPage >= 0) {
                        scope.launch {
                            pagerState.animateScrollToPage(targetPage)
                        }
                    }
                }
            )
        }
        
        // Restart prompt overlay
        if (showRestartPrompt) {
            RestartPrompt(onRestartApp)
        }
        
        // First launch dialog
        if (showFirstLaunchDialog) {
            FirstLaunchDialog(
                onDismiss = {
                    showFirstLaunchDialog = false
                    prefs.edit().putBoolean("first_launch_shown", true).apply()
                }
            )
        }
    }
}

@Composable
fun FirstLaunchDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "This app supports multiple access methods:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Method cards
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MethodCard(
                        title = "KernelSU",
                        subtitle = "Root access",
                        modifier = Modifier.weight(1f)
                    )
                    MethodCard(
                        title = "Shizuku",
                        subtitle = "ADB-level",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "Grant permission using either method to control the LED.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Got it!", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun MethodCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RestartPrompt(onRestart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.85f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Outlined.RestartAlt, 
                    contentDescription = null, 
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Restart Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Shizuku permission granted. Please restart the app to apply changes.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Restart App", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}
