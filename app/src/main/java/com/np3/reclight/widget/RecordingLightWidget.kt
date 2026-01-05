package com.np3.reclight.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.cornerRadius
import com.np3.reclight.led.LedController
import com.np3.reclight.shizuku.ShizukuHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Home screen widget for quick recording light control
 */
class RecordingLightWidget : GlanceAppWidget() {
    
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Get current state
        val isOn = try {
            if (ShizukuHelper.isReady()) {
                withContext(Dispatchers.IO) {
                    LedController.getCurrentBrightness() > 0
                }
            } else false
        } catch (e: Exception) {
            false
        }
        
        provideContent {
            RecordingLightWidgetContent(isOn = isOn)
        }
    }
}

@Composable
private fun RecordingLightWidgetContent(isOn: Boolean) {
    val backgroundColor = if (isOn) {
        ColorProvider(Color(0xFFB71C1C))
    } else {
        ColorProvider(Color(0xFF1E1E1E))
    }
    
    val textColor = ColorProvider(Color.White)
    
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(backgroundColor)
            .clickable(
                onClick = actionRunCallback<ToggleLightAction>(
                    actionParametersOf(ToggleLightAction.isOnKey to isOn)
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Record icon circle - outer
            Box(
                modifier = GlanceModifier
                    .size(48.dp)
                    .cornerRadius(24.dp)
                    .background(
                        if (isOn) ColorProvider(Color.Red)
                        else ColorProvider(Color(0xFF424242))
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Inner dot
                Box(
                    modifier = GlanceModifier
                        .size(20.dp)
                        .cornerRadius(10.dp)
                        .background(
                            if (isOn) ColorProvider(Color.White)
                            else ColorProvider(Color(0xFF757575))
                        )
                ) {}
            }
            
            Spacer(modifier = GlanceModifier.height(8.dp))
            
            Text(
                text = if (isOn) "REC ON" else "REC OFF",
                style = TextStyle(
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

/**
 * Action callback for toggling the light from the widget
 */
class ToggleLightAction : ActionCallback {
    
    companion object {
        val isOnKey = ActionParameters.Key<Boolean>("is_on")
    }
    
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val currentlyOn = parameters[isOnKey] ?: false
        
        if (ShizukuHelper.isReady()) {
            val success = if (currentlyOn) {
                LedController.turnOff()
            } else {
                LedController.turnOn()
            }
            
            if (success) {
                // Update the widget
                RecordingLightWidget().update(context, glanceId)
            }
        }
    }
}

/**
 * Widget receiver
 */
class RecordingLightWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RecordingLightWidget()
}
