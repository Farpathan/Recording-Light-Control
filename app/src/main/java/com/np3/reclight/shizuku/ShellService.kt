package com.np3.reclight.shizuku

import android.util.Log
import com.np3.reclight.IShellService
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess

/**
 * Shizuku UserService implementation for executing shell commands
 * This service runs in a separate process with elevated privileges (root or shell)
 */
class ShellService : IShellService.Stub() {
    
    companion object {
        private const val TAG = "ShellService"
    }
    
    /**
     * Execute a shell command and return the output
     */
    override fun exec(command: String): String {
        Log.d(TAG, "Executing: $command")
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            val output = reader.readText()
            val error = errorReader.readText()
            
            process.waitFor()
            
            reader.close()
            errorReader.close()
            process.destroy()
            
            if (output.isNotEmpty()) output else error
        } catch (e: Exception) {
            Log.e(TAG, "Error executing command: ${e.message}", e)
            "Error: ${e.message}"
        }
    }
    
    /**
     * Execute a shell command and return the exit code
     */
    override fun execGetExitCode(command: String): Int {
        Log.d(TAG, "Executing (get exit code): $command")
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val exitCode = process.waitFor()
            process.destroy()
            exitCode
        } catch (e: Exception) {
            Log.e(TAG, "Error executing command: ${e.message}", e)
            -1
        }
    }
    
    /**
     * Destroy the service - required by Shizuku
     * Transaction code: 16777115 (or 16777114 in AIDL)
     */
    override fun destroy() {
        Log.d(TAG, "Service destroy called")
        exitProcess(0)
    }
}
