package com.np3.reclight.shizuku

import android.content.pm.PackageManager
import android.util.Log
import com.np3.reclight.MainActivity
import rikka.shizuku.Shizuku

/**
 * Helper class for managing Shizuku and root access
 */
object ShizukuHelper {
    
    private const val TAG = "ShizukuHelper"
    
    enum class ShizukuStatus {
        CHECKING,
        READY,                    // Ready to execute (either Shizuku or direct root)
        PERMISSION_REQUIRED,      // Shizuku permission needed
        NOT_RUNNING,              // Shizuku not running
        NOT_INSTALLED,            // Neither Shizuku nor root available
        USING_ROOT_DIRECTLY       // Using direct su (KernelSU/Magisk)
    }
    
    private var directRootAvailable = false
    private var shizukuReady = false
    
    /**
     * Check the current root/Shizuku status
     */
    fun checkShizukuStatus(): ShizukuStatus {
        // Check for direct root first (KernelSU/Magisk)
        val hasDirectRoot = checkDirectRoot()
        if (hasDirectRoot) {
            directRootAvailable = true
            shizukuReady = false
            Log.d(TAG, "Direct root available (KernelSU/Magisk)")
            return ShizukuStatus.USING_ROOT_DIRECTLY
        }
        
        directRootAvailable = false
        
        // Check Shizuku status
        return try {
            if (!Shizuku.pingBinder()) {
                shizukuReady = false
                Log.d(TAG, "Shizuku not running")
                ShizukuStatus.NOT_RUNNING
            } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                shizukuReady = true
                Log.d(TAG, "Shizuku ready, binding UserService")
                // Bind the UserService when Shizuku permission is granted
                ShellExecutor.bindShizukuService()
                ShizukuStatus.READY
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                shizukuReady = false
                ShizukuStatus.PERMISSION_REQUIRED
            } else {
                shizukuReady = false
                ShizukuStatus.PERMISSION_REQUIRED
            }
        } catch (e: Exception) {
            Log.w(TAG, "Status check failed: ${e.message}")
            shizukuReady = false
            ShizukuStatus.NOT_INSTALLED
        }
    }
    
    /**
     * Check if direct root access is available (KernelSU, Magisk)
     */
    private fun checkDirectRoot(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val reader = process.inputStream.bufferedReader()
            val output = reader.readText()
            val exitCode = process.waitFor()
            reader.close()
            process.destroy()
            
            val success = exitCode == 0 && output.contains("uid=0")
            Log.d(TAG, "Direct root check: exitCode=$exitCode, hasRoot=$success")
            success
        } catch (e: Exception) {
            Log.d(TAG, "Direct root not available: ${e.message}")
            false
        }
    }
    
    /**
     * Request Shizuku permission
     */
    fun requestPermission() {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(MainActivity.SHIZUKU_PERMISSION_REQUEST_CODE)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request Shizuku permission: ${e.message}")
        }
    }
    
    /**
     * Check if root commands can be executed (either via Shizuku or direct root)
     */
    fun isReady(): Boolean {
        return directRootAvailable || shizukuReady
    }
    
    /**
     * Returns true if using direct root (not Shizuku)
     */
    fun isUsingDirectRoot(): Boolean = directRootAvailable
    
    /**
     * Check if Shizuku is available and ready
     */
    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder() && 
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get Shizuku UID to check privilege level
     * Returns 0 for root, 2000 for ADB/shell
     */
    fun getShizukuUid(): Int {
        return try {
            if (Shizuku.pingBinder()) {
                Shizuku.getUid()
            } else {
                -1
            }
        } catch (e: Exception) {
            -1
        }
    }
}
