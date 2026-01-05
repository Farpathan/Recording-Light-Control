package com.np3.reclight.led

import android.util.Log
import com.np3.reclight.shizuku.ShellExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Controller for the red recording LED on Nothing Phone (3)
 * Controls via sysfs: /sys/class/leds/red/brightness
 */
object LedController {
    
    private const val TAG = "LedController"
    private const val LED_BRIGHTNESS_PATH = "/sys/class/leds/red/brightness"
    
    private var animationJob: kotlinx.coroutines.Job? = null
    
    suspend fun turnOn(brightness: Int = 255): Boolean = withContext(Dispatchers.IO) {
        stopAnimations()
        setBrightnessInternal(brightness)
    }
    
    suspend fun turnOff(): Boolean = withContext(Dispatchers.IO) {
        stopAnimations()
        setBrightnessInternal(0)
    }
    
    private suspend fun setBrightnessInternal(brightness: Int): Boolean {
        val clamped = brightness.coerceIn(0, 255)
        return ShellExecutor.execute("echo $clamped > $LED_BRIGHTNESS_PATH").isSuccess
    }
    
    suspend fun setBrightness(brightness: Int): Boolean = withContext(Dispatchers.IO) {
        if (animationJob?.isActive == true) {
            stopAnimations()
        }
        setBrightnessInternal(brightness)
    }
    
    suspend fun getCurrentBrightness(): Int = withContext(Dispatchers.IO) {
        val result = ShellExecutor.execute("cat $LED_BRIGHTNESS_PATH")
        if (result.isSuccess) {
            try {
                result.output.trim().toInt()
            } catch (e: Exception) { -1 }
        } else { -1 }
    }
    
    private suspend fun stopAnimations() {
        animationJob?.cancel()
        animationJob = null
    }

    suspend fun stopBlinking() = turnOff()
    
    suspend fun startBlinking(
        delayOnMs: Long = 500L,
        delayOffMs: Long = 500L,
        brightness: Int = 255
    ): Boolean = withContext(Dispatchers.IO) {
        stopAnimations()
        Log.d(TAG, "Starting blink animation")
        
        val scope = CoroutineScope(Dispatchers.IO)
        
        animationJob = scope.launch {
            while (isActive) {
                setBrightnessInternal(brightness)
                delay(delayOnMs)
                setBrightnessInternal(0)
                delay(delayOffMs)
            }
        }
        
        true
    }
    
    suspend fun startBreathing(maxBrightness: Int = 255): Boolean = withContext(Dispatchers.IO) {
        stopAnimations()
        Log.d(TAG, "Starting breathing animation")
        
        val scope = CoroutineScope(Dispatchers.IO)
        
        animationJob = scope.launch {
            while (isActive) {
                // Breathe in
                for (i in 0..10) {
                    val b = (maxBrightness * (i / 10f)).toInt()
                    setBrightnessInternal(b)
                    delay(50) 
                }
                delay(200)
                // Breathe out
                for (i in 10 downTo 0) {
                    val b = (maxBrightness * (i / 10f)).toInt()
                    setBrightnessInternal(b)
                    delay(50)
                }
                delay(500)
            }
        }
        
        true
    }
}
