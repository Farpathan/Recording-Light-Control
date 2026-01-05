package com.np3.reclight

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.np3.reclight.led.LedController
import com.np3.reclight.shizuku.ShizukuHelper
import com.np3.reclight.ui.theme.RecordingLightControlTheme
import com.np3.reclight.widget.RecordingLightWidget
import kotlinx.coroutines.delay
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
            RecordingLightControlTheme {
                MainScreen(
                    showRestartPrompt = showRestartPrompt,
                    onRestartApp = {
                        finish()
                        Process.killProcess(Process.myPid()) 
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

enum class LightMode {
    STATIC, BLINK, BREATHE
}

@Composable
fun MainScreen(
    showRestartPrompt: Boolean,
    onRestartApp: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

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
    var showAboutDialog by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recording Light",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showAboutDialog = true }) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = "About",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                
                ShizukuStatusCard(
                    status = shizukuStatus,
                    onRequestPermission = { ShizukuHelper.requestPermission() }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Power Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = if (isLightOn) 1.2f else 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )
                    
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = if (isLightOn) 0.3f else 0f,
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
                            .background(Color.Red.copy(alpha = alpha))
                    )
                    
                    // Button
                    FilledIconButton(
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
                        },
                        enabled = ShizukuHelper.isReady(),
                        modifier = Modifier.size(160.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isLightOn) Color.Red else Color.DarkGray,
                            disabledContainerColor = Color.DarkGray.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            Icons.Default.FiberManualRecord,
                            contentDescription = if (isLightOn) "Turn Off" else "Turn On",
                            modifier = Modifier.size(80.dp),
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = if (isLightOn) "ON (${activeMode.name})" else "OFF",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isLightOn) Color.Red else MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
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
                
                Spacer(modifier = Modifier.weight(1f))
                
                if (!isTileAdded) {
                    InfoCard(context) { added ->
                        if (added) {
                            isTileAdded = true
                            prefs.edit().putBoolean("tile_added", true).apply()
                        }
                    }
                }
            }
            
            if (showRestartPrompt) {
                RestartPrompt(onRestartApp)
            }
            
            if (showAboutDialog) {
                AboutDialog(onDismiss = { showAboutDialog = false })
            }
        }
    }
}

@Composable
fun RestartPrompt(onRestart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
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
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Shizuku permission granted. Please restart the app to apply changes.",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Restart App")
                }
            }
        }
    }
}

@Composable
fun LightModeSelector(
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
                    containerColor = if (isActive) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                    contentColor = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                ),
                border = if (isActive) null else ButtonDefaults.outlinedButtonBorder
            ) {
                Text(
                    text = mode.name.lowercase().capitalize(),
                    fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Recording Light Control",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "v1.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Control the Recording LED on your Nothing Phone (3). Requires Root or Shizuku.",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                FilledTonalButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Farpathan/Recording-Light-Control"))
                        context.startActivity(intent)
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_github),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GitHub")
                }
            }
        }
    }
}

fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

@Composable
fun ShizukuStatusCard(
    status: ShizukuHelper.ShizukuStatus,
    onRequestPermission: () -> Unit
) {
    val (statusColor, statusText, statusDescription) = when (status) {
        ShizukuHelper.ShizukuStatus.READY -> Triple(Color(0xFF4CAF50), "Ready", "Service connected")
        ShizukuHelper.ShizukuStatus.USING_ROOT_DIRECTLY -> Triple(Color(0xFF4CAF50), "Root Ready", "KernelSU/Magisk root")
        ShizukuHelper.ShizukuStatus.PERMISSION_REQUIRED -> Triple(Color(0xFFFFA000), "Permission Required", "Tap to grant")
        ShizukuHelper.ShizukuStatus.NOT_RUNNING -> Triple(Color(0xFFF44336), "Not Running", "Start Shizuku app")
        else -> Triple(Color.Gray, "Checking...", "Verifying status")
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp),
        onClick = { if (status == ShizukuHelper.ShizukuStatus.PERMISSION_REQUIRED) onRequestPermission() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(statusColor))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Status: $statusText", fontWeight = FontWeight.SemiBold)
                Text(statusDescription, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun InfoCard(context: Context, onTileAdded: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable {
            // Request to add Quick Settings Tile (Android 13+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                try {
                    val statusBarManager = context.getSystemService(android.app.StatusBarManager::class.java)
                    val componentName = ComponentName(context, com.np3.reclight.tile.RecordingLightTileService::class.java)
                    
                    statusBarManager.requestAddTileService(
                        componentName,
                        "Rec Light",
                        android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_rec_light),
                        context.mainExecutor
                    ) { result ->
                        val message = when (result) {
                            android.app.StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> {
                                onTileAdded(true)
                                "Tile added successfully"
                            }
                            android.app.StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> {
                                onTileAdded(true)
                                "Tile already added"
                            }
                            android.app.StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED -> "Tile not added"
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
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Tap here to add Tile to Quick Settings", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("(Android 13+)", fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun BrightnessControl(
    label: String = "Brightness",
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean,
    valueRange: ClosedFloatingPointRange<Float> = 1f..255f
) {
    Column {
        Text("$label: ${value.toInt()}", style = MaterialTheme.typography.titleMedium)
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            enabled = enabled,
            colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red)
        )
    }
}
