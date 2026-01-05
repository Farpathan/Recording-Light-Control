package com.np3.reclight.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.np3.reclight.BuildConfig
import com.np3.reclight.IShellService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

/**
 * Executes shell commands with elevated privileges
 * Supports both direct root (KernelSU/Magisk) and Shizuku UserService
 */
object ShellExecutor {
    
    private const val TAG = "ShellExecutor"
    
    private var shellService: IShellService? = null
    private var serviceConnected = CompletableDeferred<Boolean>()
    
    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, ShellService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("shell")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Shizuku UserService connected")
            shellService = IShellService.Stub.asInterface(service)
            if (!serviceConnected.isCompleted) {
                serviceConnected.complete(true)
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Shizuku UserService disconnected")
            shellService = null
            serviceConnected = CompletableDeferred()
        }
    }
    
    data class ShellResult(
        val isSuccess: Boolean,
        val output: String,
        val error: String,
        val exitCode: Int
    )
    
    /**
     * Bind to Shizuku UserService
     * Call this when Shizuku permission is granted
     */
    fun bindShizukuService() {
        if (ShizukuHelper.isShizukuAvailable()) {
            try {
                Log.d(TAG, "Binding to Shizuku UserService")
                Shizuku.bindUserService(userServiceArgs, serviceConnection)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind Shizuku service: ${e.message}", e)
            }
        }
    }
    
    /**
     * Unbind from Shizuku UserService
     */
    fun unbindShizukuService() {
        try {
            Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unbind Shizuku service: ${e.message}", e)
        }
    }
    
    /**
     * Execute a shell command with elevated privileges
     * Uses Shizuku UserService if available, otherwise falls back to direct su
     * @param command The command to execute
     * @return ShellResult containing success status, output, error, and exit code
     */
    fun execute(command: String): ShellResult {
        Log.d(TAG, "Executing command: $command")
        
        // Try Shizuku UserService first if available
        if (ShizukuHelper.isShizukuAvailable() && !ShizukuHelper.isUsingDirectRoot()) {
            val shizukuResult = executeViaShizuku(command)
            if (shizukuResult != null) {
                return shizukuResult
            }
        }
        
        // Fall back to direct su
        return executeViaSu(command)
    }
    
    /**
     * Execute command via Shizuku UserService
     */
    private fun executeViaShizuku(command: String): ShellResult? {
        // Ensure service is bound
        if (shellService == null) {
            Log.d(TAG, "Shizuku service not bound, attempting to bind...")
            bindShizukuService()
            
            // Wait for connection with timeout
            val connected = runBlocking {
                withTimeoutOrNull(3000) {
                    serviceConnected.await()
                }
            }
            
            if (connected != true || shellService == null) {
                Log.w(TAG, "Failed to connect to Shizuku UserService")
                return null
            }
        }
        
        return try {
            Log.d(TAG, "Executing via Shizuku UserService")
            val service = shellService ?: return null
            
            val output = service.exec(command)
            val exitCode = if (output.startsWith("Error:")) -1 else 0
            
            ShellResult(
                isSuccess = exitCode == 0,
                output = output,
                error = if (exitCode != 0) output else "",
                exitCode = exitCode
            )
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku execution failed: ${e.message}", e)
            null
        }
    }
    
    /**
     * Execute command via direct su (KernelSU/Magisk)
     */
    private fun executeViaSu(command: String): ShellResult {
        return try {
            Log.d(TAG, "Executing via su")
            
            // Use su to execute commands with root privileges
            val process = Runtime.getRuntime().exec("su")
            
            // Write command to su shell
            val outputStream = DataOutputStream(process.outputStream)
            outputStream.writeBytes("$command\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            
            val outputReader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            val output = outputReader.readText()
            val error = errorReader.readText()
            
            val exitCode = process.waitFor()
            
            outputReader.close()
            errorReader.close()
            outputStream.close()
            process.destroy()
            
            val isSuccess = exitCode == 0
            
            if (!isSuccess) {
                Log.e(TAG, "su command failed with exit code $exitCode: $error")
            } else {
                Log.d(TAG, "su command succeeded: $output")
            }
            
            ShellResult(
                isSuccess = isSuccess,
                output = output,
                error = error,
                exitCode = exitCode
            )
        } catch (e: Exception) {
            Log.e(TAG, "su execution failed: ${e.message}", e)
            ShellResult(
                isSuccess = false,
                output = "",
                error = e.message ?: "Unknown error",
                exitCode = -1
            )
        }
    }
    
    /**
     * Execute a command asynchronously and return result via callback
     */
    fun executeAsync(command: String, callback: (ShellResult) -> Unit) {
        Thread {
            val result = execute(command)
            callback(result)
        }.start()
    }
    
    /**
     * Check if root access is available
     */
    fun isRootAvailable(): Boolean {
        return try {
            val result = execute("id")
            result.isSuccess && result.output.contains("uid=0")
        } catch (e: Exception) {
            false
        }
    }
}
