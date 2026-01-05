package com.np3.reclight.tile

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.np3.reclight.R
import com.np3.reclight.led.LedController
import com.np3.reclight.shizuku.ShizukuHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Quick Settings Tile for toggling the recording light
 */
class RecordingLightTileService : TileService() {
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isOn = false
    
    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }
    
    override fun onStopListening() {
        super.onStopListening()
    }
    
    override fun onClick() {
        super.onClick()
        
        if (!ShizukuHelper.isReady()) {
            // Show unavailable state
            qsTile?.apply {
                state = Tile.STATE_UNAVAILABLE
                subtitle = "Shizuku not ready"
                updateTile()
            }
            return
        }
        
        scope.launch {
            val success = if (isOn) {
                LedController.turnOff()
            } else {
                LedController.turnOn()
            }
            
            if (success) {
                isOn = !isOn
                updateTileState()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
    
    private fun updateTileState() {
        if (!ShizukuHelper.isReady()) {
            qsTile?.apply {
                state = Tile.STATE_UNAVAILABLE
                label = "Rec Light"
                subtitle = "Shizuku required"
                icon = Icon.createWithResource(this@RecordingLightTileService, R.drawable.ic_rec_light)
                updateTile()
            }
            return
        }
        
        scope.launch {
            val brightness = LedController.getCurrentBrightness()
            isOn = brightness > 0
            
            qsTile?.apply {
                state = if (isOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                label = "Rec Light"
                subtitle = if (isOn) "On" else "Off"
                icon = Icon.createWithResource(this@RecordingLightTileService, R.drawable.ic_rec_light)
                updateTile()
            }
        }
    }
}
