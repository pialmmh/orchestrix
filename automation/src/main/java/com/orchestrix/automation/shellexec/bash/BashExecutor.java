package com.orchestrix.automation.shellexec.bash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Local bash command executor
 * Executes commands on the local machine (not via SSH)
 */
public class BashExecutor {

    private static final Logger logger = LoggerFactory.getLogger(BashExecutor.class);

    private boolean dryRun = false;mustafa@mustafa-pc:~/telcobright-projects/orchestrix/images/containers/lxc/auto-increment-service$ cd /home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/unique-id-generator/build
    mustafa@mustafa-pc:~/telcobright-projects/orchestrix/images/containers/lxc/unique-id-generator/build$ sudo ./build.sh
[sudo] password for mustafa:
            ==========================================
    Unique ID Generator Container Builder
    Version: 1.0.0
            ==========================================


            ▶ Checking Prerequisites
─────────────────────────────────────
        ✓ LXC found
✓ Java found: openjdk version "21.0.8" 2025-07-15
            ✗ Java compiler (javac) is not installed
    mustafa@mustafa-pc:~/telcobright-projects/orchestrix/images/containers/lxc/unique-id-generator/build$
    private boolean streamOutput = false;
    private File workingDirectory;
    private final List<String> executionHistory = new ArrayList<>();

    public BashExecutor() {
        this.workingDirectory = new File(System.getProperty("user.dir"));
    }

    /**
     * Execute a single bash command
     */
    public CommandResult execute(String command) {
        return execute(command, 120000); // Default 2 minute timeout
    }

    /**
     * Execute command with custom timeout
     */
    public CommandResult execute(String command, long timeoutMs) {
        logger.info("Executing: {}", command);
        executionHistory.add(command);

        if (dryRun) {
            logger.info("[DRY RUN] Would execute: {}", command);
            return new CommandResult(0, "", "", 0);
        }

        long startTime = System.currentTimeMillis();

        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.directory(workingDirectory);
            pb.redirectErrorStream(false);

            Process process = pb.start();

            // Stream output if requested
            StreamGobbler outputGobbler = new StreamGobbler(
                process.getInputStream(), streamOutput ? logger::info : null);
            StreamGobbler errorGobbler = new StreamGobbler(
                process.getErrorStream(), streamOutput ? logger::error : null);

            outputGobbler.start();
            errorGobbler.start();

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new CommandTimeoutException("Command timed out after " + timeoutMs + "ms");
            }

            outputGobbler.join(1000);
            errorGobbler.join(1000);

            int exitCode = process.exitValue();
            long executionTime = System.currentTimeMillis() - startTime;

            CommandResult result = new CommandResult(
                exitCode,
                outputGobbler.getOutput(),
                errorGobbler.getOutput(),
                executionTime
            );

            if (exitCode != 0) {
                logger.warn("Command failed with exit code {}: {}", exitCode, command);
                if (!errorGobbler.getOutput().isEmpty()) {
                    logger.error("Error output: {}", errorGobbler.getOutput());
                }
            }

            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Failed to execute command: {}", command, e);
            return new CommandResult(-1, "", e.getMessage(), executionTime);
        }
    }

    /**
     * Execute a multi-line script
     */
    public CommandResult executeScript(String script) {
        return executeScript(script, 300000); // Default 5 minute timeout
    }

    /**
     * Execute script with custom timeout
     */
    public CommandResult executeScript(String script, long timeoutMs) {
        try {
            // Write script to temporary file
            Path tempScript = Files.createTempFile("bash-script-", ".sh");
            Files.write(tempScript, script.getBytes(StandardCharsets.UTF_8));
            tempScript.toFile().setExecutable(true);

            logger.debug("Created temporary script: {}", tempScript);

            try {
                // Execute the script
                return execute(tempScript.toString(), timeoutMs);
            } finally {
                // Clean up temp file
                Files.deleteIfExists(tempScript);
            }

        } catch (IOException e) {
            logger.error("Failed to create temporary script", e);
            return new CommandResult(-1, "", e.getMessage(), 0);
        }
    }

    /**
     * Execute command with environment variables
     */
    public CommandResult execute(String command, Map<String, String> env) {
        // Build environment string
        StringBuilder envCmd = new StringBuilder();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            envCmd.append("export ").append(entry.getKey())
                  .append("='").append(entry.getValue()).append("'; ");
        }
        envCmd.append(command);

        return execute(envCmd.toString());
    }

    /**
     * Execute command with retries
     */
    public CommandResult executeWithRetry(String command, int maxRetries, long retryDelayMs) {
        CommandResult result = null;

        for (int i = 0; i <= maxRetries; i++) {
            if (i > 0) {
                logger.info("Retry attempt {} of {}", i, maxRetries);
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            result = execute(command);

            if (result.isSuccess()) {
                return result;
            }
        }

        logger.error("Command failed after {} retries", maxRetries);
        return result;
    }

    /**
     * Check if a command exists
     */
    public boolean commandExists(String command) {
        CommandResult result = execute("which " + command);
        return result.isSuccess() && !result.getStdout().isEmpty();
    }

    /**
     * Check if running with sudo privileges
     */
    public boolean hasSudoPrivileges() {
        CommandResult result = execute("sudo -n true 2>/dev/null");
        return result.isSuccess();
    }

    /**
     * Set working directory
     */
    public void setWorkingDirectory(String path) {
        this.workingDirectory = new File(path);
        if (!workingDirectory.exists() || !workingDirectory.isDirectory()) {
            throw new IllegalArgumentException("Invalid working directory: " + path);
        }
    }

    /**
     * Enable/disable dry run mode
     */
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    /**
     * Enable/disable streaming output
     */
    public void setStreamOutput(boolean streamOutput) {
        this.streamOutput = streamOutput;
    }

    /**
     * Get execution history
     */
    public List<String> getExecutionHistory() {
        return new ArrayList<>(executionHistory);
    }

    /**
     * Clear execution history
     */
    public void clearHistory() {
        executionHistory.clear();
    }

    /**
     * Command execution result
     */
    public static class CommandResult {
        private final int exitCode;
        private final String stdout;
        private final String stderr;
        private final long executionTimeMs;

        public CommandResult(int exitCode, String stdout, String stderr, long executionTimeMs) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
            this.executionTimeMs = executionTimeMs;
        }

        public int getExitCode() { return exitCode; }
        public String getStdout() { return stdout; }
        public String getStderr() { return stderr; }
        public long getExecutionTimeMs() { return executionTimeMs; }
        public boolean isSuccess() { return exitCode == 0; }

        @Override
        public String toString() {
            return String.format("CommandResult[exitCode=%d, executionTime=%dms, stdout=%d chars, stderr=%d chars]",
                exitCode, executionTimeMs,
                stdout != null ? stdout.length() : 0,
                stderr != null ? stderr.length() : 0);
        }
    }

    /**
     * Stream gobbler to capture process output
     */
    private static class StreamGobbler extends Thread {
        private final InputStream inputStream;
        private final StringBuilder output = new StringBuilder();
        private final OutputHandler handler;

        interface OutputHandler {
            void handle(String line);
        }

        StreamGobbler(InputStream inputStream, OutputHandler handler) {
            this.inputStream = inputStream;
            this.handler = handler;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    if (handler != null) {
                        handler.handle(line);
                    }
                }
            } catch (IOException e) {
                logger.error("Error reading stream", e);
            }
        }

        String getOutput() {
            return output.toString();
        }
    }

    /**
     * Command timeout exception
     */
    public static class CommandTimeoutException extends RuntimeException {
        public CommandTimeoutException(String message) {
            super(message);
        }
    }
}