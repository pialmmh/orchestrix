package com.telcobright.orchestrix.automation.core;

import com.telcobright.orchestrix.automation.api.model.AutomationConfig;
import com.telcobright.orchestrix.automation.api.model.CommandResult;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Local command executor - executes commands on local machine
 */
public class LocalCommandExecutor implements BaseAutomation.CommandExecutor {

    private static final Logger logger = Logger.getLogger(LocalCommandExecutor.class.getName());
    private final AutomationConfig config;
    private final File workingDirectory;

    public LocalCommandExecutor(AutomationConfig config) {
        this.config = config;
        this.workingDirectory = config.getWorkingDirectory() != null ?
            new File(config.getWorkingDirectory()) :
            new File(System.getProperty("user.dir"));
    }

    @Override
    public CommandResult execute(String command) {
        long startTime = System.currentTimeMillis();

        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.directory(workingDirectory);

            // Add environment variables
            if (config.getVariables() != null) {
                pb.environment().putAll(config.getVariables());
            }

            Process process = pb.start();

            // Capture output
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            Thread outputThread = new Thread(() ->
                captureStream(process.getInputStream(), stdout, config.isStreamOutput()));
            Thread errorThread = new Thread(() ->
                captureStream(process.getErrorStream(), stderr, config.isStreamOutput()));

            outputThread.start();
            errorThread.start();

            boolean finished = process.waitFor(config.getCommandTimeoutMs(), TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Command timed out after " + config.getCommandTimeoutMs() + "ms");
            }

            outputThread.join(1000);
            errorThread.join(1000);

            int exitCode = process.exitValue();
            long executionTime = System.currentTimeMillis() - startTime;

            return new CommandResult(
                command,
                exitCode,
                stdout.toString(),
                stderr.toString(),
                executionTime,
                "localhost"
            );

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to execute command: " + command, e);
            long executionTime = System.currentTimeMillis() - startTime;
            return new CommandResult(command, -1, "", e.getMessage(), executionTime, "localhost");
        }
    }

    @Override
    public CommandResult executeScript(String script) {
        try {
            // Write script to temp file
            Path tempScript = Files.createTempFile("automation-script-", ".sh");
            Files.write(tempScript, script.getBytes(StandardCharsets.UTF_8));
            tempScript.toFile().setExecutable(true);

            try {
                return execute(tempScript.toString());
            } finally {
                Files.deleteIfExists(tempScript);
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create script file", e);
            return new CommandResult("script", -1, "", e.getMessage(), 0, "localhost");
        }
    }

    private void captureStream(InputStream stream, StringBuilder output, boolean streamOutput) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                if (streamOutput) {
                    logger.info("[OUTPUT] " + line);
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading stream", e);
        }
    }
}