package com.np3.reclight.ui.screens.home

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.np3.reclight.R
import com.np3.reclight.led.LedController
import com.np3.reclight.shizuku.ShizukuHelper
import com.np3.reclight.tile.RecordingLightTileService
import com.np3.reclight.ui.components.*
import com.np3.reclight.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class LightMode {
    STATIC, BLINK, BREATHE
}

@Composable
fun HomeScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val scrollState = rememberScrollState()

    var isLightOn by remember { mutableStateOf(false) }
    var activeMode by remember { 
        mutableStateOf(
            try {
                LightMode.valueOf(prefs.getString("active_mode", "STATIC") ?: "STATIC")
            } catch (e: Exception) {
                LightMode.STATIC
            }
        ) 
    }
    
    // Preferences & State
    var staticBrightness by remember { mutableFloatStateOf(prefs.getFloat("static_brightness", 100f)) }
    var blinkBrightness by remember { mutableFloatStateOf(prefs.getFloat("blink_brightness", 255f)) }
    var blinkSpeed by remember { mutableFloatStateOf(prefs.getFloat("blink_speed", 50f)) }
    var breatheBrightness by remember { mutableFloatStateOf(prefs.getFloat("breathe_brightness", 255f)) }
    
    var shizukuStatus by remember { mutableStateOf(ShizukuHelper.ShizukuStatus.CHECKING) }
    var isTileAdded by remember { mutableStateOf(prefs.getBoolean("tile_added", false)) }
    
    fun getBlinkDelay(speed: Float): Long {
        val normalized = (speed.coerceIn(1f, 100f) - 1f) / 99f
        return (1000 - (normalized * 950)).toLong()
    }

    // Status polling
    LaunchedEffect(Unit) {
        while (true) {
            shizukuStatus = ShizukuHelper.checkShizukuStatus()
            if (ShizukuHelper.isReady()) {
                // Poll hardware only if in Static mode to avoid UI flicker
                if (activeMode == LightMode.STATIC) {
                    val current = LedController.getCurrentBrightness()
                    isLightOn = current > 0
                    if (current > 0) staticBrightness = current.toFloat()
                }
            }
            delay(2000)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        
        // Hero section
        HeroSection()
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Status section with BOTH Root and Shizuku
        SectionHeader(title = "Status")
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Root status card - uses dedicated root check
            val rootGranted = ShizukuHelper.isUsingDirectRoot()
            StatusCard(
                title = "Root",
                status = if (rootGranted) "Granted" else "Not Granted",
                icon = Icons.Default.Security,
                isActive = rootGranted,
                modifier = Modifier.weight(1f)
            )
            
            // Shizuku status card - uses dedicated Shizuku check only
            val shizukuGranted = ShizukuHelper.isShizukuAvailable()
            StatusCard(
                title = "Shizuku",
                status = if (shizukuGranted) "Granted" else "Not Granted",
                icon = Icons.Default.Shield,
                isActive = shizukuGranted,
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = shizukuStatus == ShizukuHelper.ShizukuStatus.PERMISSION_REQUIRED) {
                        ShizukuHelper.requestPermission()
                    }
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // LED Control section
        SectionHeader(title = "Control")
        Spacer(modifier = Modifier.height(24.dp))
        
        // Power Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            PowerButton(
                isOn = isLightOn,
                enabled = ShizukuHelper.isReady(),
                onClick = {
                    if (ShizukuHelper.isReady()) {
                        scope.launch {
                            val newState = !isLightOn
                            val result = if (newState) {
                                when (activeMode) {
                                    LightMode.STATIC -> LedController.turnOn(staticBrightness.toInt())
                                    LightMode.BLINK -> {
                                        val d = getBlinkDelay(blinkSpeed)
                                        LedController.startBlinking(d, d, blinkBrightness.toInt())
                                    }
                                    LightMode.BREATHE -> LedController.startBreathing(breatheBrightness.toInt())
                                }
                            } else {
                                LedController.turnOff()
                            }
                            
                            if (result) {
                                isLightOn = newState
                            } else {
                                isLightOn = !newState 
                            }
                        }
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (isLightOn) "ON (${activeMode.name})" else "OFF",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isLightOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Mode selector
        LightModeSelector(
            activeMode = activeMode,
            enabled = ShizukuHelper.isReady(),
            onModeSelected = { mode ->
                activeMode = mode
                prefs.edit().putString("active_mode", mode.name).apply()
                if (isLightOn && ShizukuHelper.isReady()) {
                    scope.launch {
                        when (mode) {
                            LightMode.STATIC -> {
                                LedController.stopBlinking()
                                LedController.turnOn(staticBrightness.toInt())
                            }
                            LightMode.BLINK -> {
                                val d = getBlinkDelay(blinkSpeed)
                                LedController.startBlinking(d, d, blinkBrightness.toInt())
                            }
                            LightMode.BREATHE -> LedController.startBreathing(breatheBrightness.toInt())
                        }
                    }
                }
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Brightness controls
        AnimatedVisibility(visible = true) {
            Column {
                when (activeMode) {
                    LightMode.STATIC -> {
                        BrightnessControl(
                            label = "Brightness",
                            value = staticBrightness / 2.55f,
                            valueRange = 1f..100f,
                            onValueChange = { newVal ->
                                staticBrightness = newVal * 2.55f
                                if (isLightOn) {
                                    scope.launch { LedController.turnOn(staticBrightness.toInt()) }
                                }
                            },
                            onValueChangeFinished = {
                                prefs.edit().putFloat("static_brightness", staticBrightness).apply()
                            },
                            enabled = ShizukuHelper.isReady()
                        )
                    }
                    LightMode.BLINK -> {
                        BrightnessControl(
                            label = "Brightness",
                            value = blinkBrightness / 2.55f,
                            valueRange = 1f..100f,
                            onValueChange = { newVal ->
                                blinkBrightness = newVal * 2.55f
                                if (isLightOn) {
                                    scope.launch { 
                                        val d = getBlinkDelay(blinkSpeed)
                                        LedController.startBlinking(d, d, blinkBrightness.toInt()) 
                                    }
                                }
                            },
                            onValueChangeFinished = {
                                prefs.edit().putFloat("blink_brightness", blinkBrightness).apply()
                            },
                            enabled = ShizukuHelper.isReady()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BrightnessControl(
                            label = "Blink Speed",
                            value = blinkSpeed,
                            valueRange = 1f..100f,
                            onValueChange = { newVal ->
                                blinkSpeed = newVal
                                if (isLightOn) {
                                    scope.launch {
                                        val d = getBlinkDelay(newVal)
                                        LedController.startBlinking(d, d, blinkBrightness.toInt())
                                    }
                                }
                            },
                            onValueChangeFinished = {
                                prefs.edit().putFloat("blink_speed", blinkSpeed).apply()
                            },
                            enabled = ShizukuHelper.isReady()
                        )
                    }
                    LightMode.BREATHE -> {
                        BrightnessControl(
                            label = "Max Brightness",
                            value = breatheBrightness / 2.55f,
                            valueRange = 1f..100f,
                            onValueChange = { newVal ->
                                breatheBrightness = newVal * 2.55f
                                if (isLightOn) {
                                    scope.launch { LedController.startBreathing(breatheBrightness.toInt()) }
                                }
                            },
                            onValueChangeFinished = {
                                prefs.edit().putFloat("breathe_brightness", breatheBrightness).apply()
                            },
                            enabled = ShizukuHelper.isReady()
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Quick Tile Info
        if (!isTileAdded) {
            InfoCard(context) { added ->
                if (added) {
                    isTileAdded = true
                    prefs.edit().putBoolean("tile_added", true).apply()
                }
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun HeroSection() {
    GradientBox(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Dot pattern decoration
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(5) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f + it * 0.1f))
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Recording Light",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Control the LED on your Nothing Phone (3)",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PowerButton(
    isOn: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(200.dp)
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (isOn) 1.2f else 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = if (isOn) 0.3f else 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        
        // Ripple
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(primaryColor.copy(alpha = alpha))
        )
        
        // Button
        FilledIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(160.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isOn) primaryColor else surfaceVariant,
                disabledContainerColor = surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Icon(
                Icons.Default.FiberManualRecord,
                contentDescription = if (isOn) "Turn Off" else "Turn On",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun LightModeSelector(
    activeMode: LightMode,
    enabled: Boolean,
    onModeSelected: (LightMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LightMode.entries.forEach { mode ->
            val isActive = activeMode == mode
            
            OutlinedButton(
                onClick = { onModeSelected(mode) },
                enabled = enabled,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                    contentColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = if (isActive) null else ButtonDefaults.outlinedButtonBorder(enabled)
            ) {
                Text(
                    text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                    fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun BrightnessControl(
    label: String = "Brightness",
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean,
    valueRange: ClosedFloatingPointRange<Float> = 1f..255f
) {
    Column {
        Text(
            text = "$label: ${value.toInt()}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun InfoCard(context: Context, onTileAdded: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    try {
                        val statusBarManager = context.getSystemService(StatusBarManager::class.java)
                        val componentName = ComponentName(context, RecordingLightTileService::class.java)
                        
                        statusBarManager.requestAddTileService(
                            componentName,
                            "Rec Light",
                            Icon.createWithResource(context, R.drawable.ic_rec_light),
                            context.mainExecutor
                        ) { result ->
                            val message = when (result) {
                                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> {
                                    onTileAdded(true)
                                    "Tile added successfully"
                                }
                                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> {
                                    onTileAdded(true)
                                    "Tile already added"
                                }
                                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED -> "Tile not added"
                                else -> "Unknown result"
                            }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error requesting tile: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "QS Tile request requires Android 13+", Toast.LENGTH_SHORT).show()
                }
            }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "Tap here to add Tile to Quick Settings",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "(Android 13+)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
