package com.telcobright.orchestrix.automation.binary;

import com.telcobright.orchestrix.automation.binary.entity.BinaryBuildConfig;
import com.telcobright.orchestrix.automation.binary.entity.BinaryTestResult;
import com.telcobright.orchestrix.automation.core.device.LocalSshDevice;

import java.io.File;
import java.util.logging.Logger;

/**
 * Abstract base class for building standalone binaries
 *
 * Provides common workflow for:
 * 1. Building binaries locally
 * 2. Testing binaries
 * 3. Preparing for deployment
 *
 * Subclasses implement language-specific build logic (Go, Rust, C++, etc.)
 */
public abstract class BinaryBuilder {

    protected static final Logger logger = Logger.getLogger(BinaryBuilder.class.getName());
    protected final LocalSshDevice device;
    protected final BinaryBuildConfig config;

    /**
     * Create binary builder
     *
     * @param device Connected SSH device for executing commands
     * @param config Build configuration
     */
    public BinaryBuilder(LocalSshDevice device, BinaryBuildConfig config) {
        if (device == null || !device.isConnected()) {
            throw new IllegalArgumentException("LocalSshDevice must be connected");
        }
        this.device = device;
        this.config = config;
    }

    /**
     * Complete build and test workflow
     *
     * @return Test result with build info and test results
     * @throws Exception if build or tests fail
     */
    public BinaryTestResult buildAndTest() throws Exception {
        logger.info("=========================================");
        logger.info("Building Binary: " + config.getBinaryName());
        logger.info("=========================================");
        logger.info(config.toString());
        logger.info("");

        long startTime = System.currentTimeMillis();

        BinaryTestResult result = new BinaryTestResult();
        result.setBinaryPath(config.getOutputBinaryPath());

        try {
            // Step 1: Validate prerequisites
            logger.info("Checking prerequisites...");
            validatePrerequisites();

            // Step 2: Prepare build environment
            logger.info("Preparing build environment...");
            prepareBuildEnvironment();

            // Step 3: Generate source code (if needed)
            logger.info("Generating source code...");
            generateSourceCode();

            // Step 4: Install dependencies
            logger.info("Installing dependencies...");
            installDependencies();

            // Step 5: Build binary
            logger.info("Building binary...");
            buildBinary();

            // Step 6: Post-build processing
            logger.info("Post-build processing...");
            postBuild();

            long buildTime = System.currentTimeMillis() - startTime;
            result.setBuildTimeMs(buildTime);

            // Get binary size
            File binaryFile = new File(config.getOutputBinaryPath());
            if (binaryFile.exists()) {
                result.setBinarySizeBytes(binaryFile.length());
            }

            // Step 7: Run tests if enabled
            if (config.isRunTests()) {
                logger.info("Running tests...");
                runTests(result);
            }

            result.setSuccess(true);
            logger.info("");
            logger.info("=========================================");
            logger.info("✓ Build Successful!");
            logger.info("Binary: " + result.getBinaryPath());
            logger.info("Size: " + result.getBinarySizeFormatted());
            logger.info("Build Time: " + result.getBuildTimeFormatted());
            logger.info("=========================================");

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            logger.severe("Build failed: " + e.getMessage());
            throw e;
        }

        return result;
    }

    /**
     * Validate that required tools are installed (Go, Rust, gcc, etc.)
     */
    protected abstract void validatePrerequisites() throws Exception;

    /**
     * Prepare build directories and environment
     */
    protected abstract void prepareBuildEnvironment() throws Exception;

    /**
     * Generate source code (for code-generation scenarios)
     * Default: no-op (source already exists)
     */
    protected void generateSourceCode() throws Exception {
        // Default: no code generation
    }

    /**
     * Install dependencies (go get, cargo fetch, npm install, etc.)
     */
    protected abstract void installDependencies() throws Exception;

    /**
     * Build the binary
     */
    protected abstract void buildBinary() throws Exception;

    /**
     * Post-build processing (strip symbols, compress, etc.)
     * Default: no-op
     */
    protected void postBuild() throws Exception {
        // Default: no post-processing
    }

    /**
     * Run tests on the built binary
     */
    protected abstract void runTests(BinaryTestResult result) throws Exception;

    /**
     * Get binary build configuration
     */
    public BinaryBuildConfig getConfig() {
        return config;
    }

    /**
     * Helper: Execute command and return output
     */
    protected String execute(String command) throws Exception {
        return device.executeCommand(command);
    }

    /**
     * Helper: Check if command exists
     */
    protected boolean commandExists(String command) {
        try {
            String result = execute("which " + command);
            return result != null && !result.trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Helper: Get command version
     */
    protected String getCommandVersion(String command, String versionFlag) {
        try {
            return execute(command + " " + versionFlag).trim();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
