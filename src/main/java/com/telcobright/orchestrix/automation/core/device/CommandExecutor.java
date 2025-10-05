package com.telcobright.orchestrix.automation.core.device;

/**
 * Simple interface for command execution
 * Allows both SshDevice and LocalSshDevice to be used interchangeably
 */
public interface CommandExecutor {
    /**
     * Execute command and return output
     */
    String executeCommand(String command) throws Exception;

    /**
     * Execute command with optional PTY allocation
     * @param command Command to execute
     * @param usePty Whether to allocate a pseudo-terminal
     */
    String executeCommand(String command, boolean usePty) throws Exception;
}
