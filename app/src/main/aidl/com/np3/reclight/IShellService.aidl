// IShellService.aidl
package com.np3.reclight;

// AIDL interface for Shizuku UserService shell execution
interface IShellService {
    // Execute a shell command and return the result
    String exec(String command);
    
    // Execute a shell command and return exit code
    int execGetExitCode(String command);
    
    // Destroy the service
    void destroy();
}
