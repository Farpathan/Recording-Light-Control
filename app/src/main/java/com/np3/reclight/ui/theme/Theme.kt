package com.np3.reclight.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NothingRed,
    onPrimary = TextPrimary,
    primaryContainer = NothingRedDark,
    onPrimaryContainer = TextPrimary,
    
    secondary = DarkCardVariant,
    onSecondary = TextPrimary,
    secondaryContainer = DarkCard,
    onSecondaryContainer = TextPrimary,
    
    tertiary = TextTertiary,
    onTertiary = TextPrimary,
    
    background = Black,
    onBackground = TextPrimary,
    
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    
    error = StatusError,
    onError = TextPrimary
)

@Composable
fun RecordingLightControlTheme(
    dynamicColor: Boolean = false, // Material You support
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            // Get dynamic colors but override background to stay AMOLED black
            dynamicDarkColorScheme(context).copy(
                background = Black,
                surface = DarkSurface,
                surfaceVariant = DarkCard
            )
        }
        else -> DarkColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Black.toArgb()
            window.navigationBarColor = Black.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
