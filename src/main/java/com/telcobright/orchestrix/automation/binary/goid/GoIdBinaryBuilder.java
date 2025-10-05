package com.telcobright.orchestrix.automation.binary.goid;

import com.telcobright.orchestrix.automation.api.container.lxc.app.goid.scripts.GoIdServiceCode;
import com.telcobright.orchestrix.automation.binary.BinaryBuilder;
import com.telcobright.orchestrix.automation.binary.entity.BinaryBuildConfig;
import com.telcobright.orchestrix.automation.binary.entity.BinaryTestResult;
import com.telcobright.orchestrix.automation.core.device.LocalSshDevice;

import java.io.File;
import java.io.FileWriter;

/**
 * Build Go-ID standalone binary locally
 *
 * Process:
 * 1. Generate Go source code
 * 2. Build static binary for Linux AMD64
 * 3. Test binary (startup, health check, API endpoints)
 * 4. Store in images/standalone-binaries/go-id/go-id-binary-v.X/
 *
 * Binary can then be bundled into Alpine containers (25 MB vs 169 MB)
 */
public class GoIdBinaryBuilder extends BinaryBuilder {

    private int servicePort;

    /**
     * Create Go-ID binary builder
     *
     * @param device Connected SSH device
     * @param config Build configuration
     */
    public GoIdBinaryBuilder(LocalSshDevice device, BinaryBuildConfig config) {
        super(device, config);
        this.servicePort = config.getTestPort();
    }

    @Override
    protected void validatePrerequisites() throws Exception {
        // Check Go installation
        if (!commandExists("go")) {
            throw new Exception("Go not installed. Install: https://go.dev/dl/");
        }

        String goVersion = getCommandVersion("go", "version");
        logger.info("✓ Go found: " + goVersion);

        // Check minimum Go version (1.21+)
        if (!goVersion.contains("go1.21") && !goVersion.contains("go1.22") && !goVersion.contains("go1.23")) {
            logger.warning("⚠ Go 1.21+ recommended. Found: " + goVersion);
        }
    }

    @Override
    protected void prepareBuildEnvironment() throws Exception {
        // Create build directory
        File buildDir = new File(config.getBuildDirectory());
        if (!buildDir.exists()) {
            buildDir.mkdirs();
            logger.info("✓ Created build directory: " + config.getBuildDirectory());
        }

        // Create output directory
        File outputDir = new File(config.getOutputDirectory());
        if (!outputDir.exists()) {
            outputDir.mkdirs();
            logger.info("✓ Created output directory: " + config.getOutputDirectory());
        }

        // Clean old build files
        File mainGo = new File(config.getBuildDirectory() + "/main.go");
        if (mainGo.exists()) {
            mainGo.delete();
        }
    }

    @Override
    protected void generateSourceCode() throws Exception {
        logger.info("Generating Go source code for Go-ID service...");

        // Generate Go code using existing GoIdServiceCode
        GoIdServiceCode codeGenerator = new GoIdServiceCode(servicePort);
        String goCode = codeGenerator.getServiceCode();

        // Write to build directory
        String mainGoPath = config.getBuildDirectory() + "/main.go";
        try (FileWriter writer = new FileWriter(mainGoPath)) {
            writer.write(goCode);
        }

        logger.info("✓ Generated main.go (" + goCode.length() + " bytes)");
    }

    @Override
    protected void installDependencies() throws Exception {
        logger.info("Installing Go dependencies...");

        String buildDir = config.getBuildDirectory();

        // Initialize Go module
        execute("cd " + buildDir + " && go mod init go-id-service 2>/dev/null || true");

        // Install dependencies
        logger.info("  - Installing github.com/sony/sonyflake...");
        execute("cd " + buildDir + " && go get github.com/sony/sonyflake@latest");

        logger.info("  - Installing github.com/gorilla/mux...");
        execute("cd " + buildDir + " && go get github.com/gorilla/mux@latest");

        logger.info("  - Installing github.com/hashicorp/consul/api...");
        execute("cd " + buildDir + " && go get github.com/hashicorp/consul/api@latest");

        // Tidy modules
        execute("cd " + buildDir + " && go mod tidy");

        logger.info("✓ Dependencies installed");
    }

    @Override
    protected void buildBinary() throws Exception {
        logger.info("Building static binary for Linux AMD64...");

        String buildDir = config.getBuildDirectory();
        String outputBinary = config.getOutputBinaryPath();

        // Build command with static linking
        String buildCmd = String.format(
            "cd %s && CGO_ENABLED=0 GOOS=%s GOARCH=%s go build -ldflags='-w -s' -o %s main.go",
            buildDir,
            config.getTargetOS(),
            config.getTargetArch(),
            outputBinary
        );

        execute(buildCmd);

        // Verify binary was created
        File binaryFile = new File(outputBinary);
        if (!binaryFile.exists()) {
            throw new Exception("Binary not created: " + outputBinary);
        }

        // Make executable
        execute("chmod +x " + outputBinary);

        logger.info("✓ Binary built: " + outputBinary);
        logger.info("  Size: " + String.format("%.2f MB", binaryFile.length() / (1024.0 * 1024.0)));
    }

    @Override
    protected void runTests(BinaryTestResult result) throws Exception {
        String binaryPath = config.getOutputBinaryPath();
        int testPort = config.getTestPort();

        logger.info("Running binary tests on port " + testPort + "...");

        // Test 1: Binary runs without errors
        logger.info("  Test 1: Binary execution...");
        Process process = null;
        try {
            // Start binary in background
            ProcessBuilder pb = new ProcessBuilder(binaryPath);
            pb.environment().put("SERVICE_PORT", String.valueOf(testPort));
            pb.environment().put("SHARD_ID", "1");
            pb.environment().put("TOTAL_SHARDS", "1");
            pb.redirectErrorStream(true);
            process = pb.start();

            // Wait for startup
            Thread.sleep(2000);

            if (!process.isAlive()) {
                throw new Exception("Binary exited immediately");
            }
            result.addTestPassed("Binary execution");
            logger.info("    ✓ Binary started successfully");

            // Test 2: Health endpoint
            logger.info("  Test 2: Health endpoint...");
            String healthResponse = execute("curl -s http://localhost:" + testPort + "/health");
            if (!healthResponse.contains("\"status\":\"ok\"")) {
                throw new Exception("Health check failed: " + healthResponse);
            }
            result.addTestPassed("Health endpoint");
            logger.info("    ✓ Health check passed");

            // Test 3: Shard info endpoint
            logger.info("  Test 3: Shard info endpoint...");
            String shardInfo = execute("curl -s http://localhost:" + testPort + "/shard-info");
            if (!shardInfo.contains("\"shard_id\":1")) {
                throw new Exception("Shard info failed: " + shardInfo);
            }
            result.addTestPassed("Shard info endpoint");
            logger.info("    ✓ Shard info correct");

            // Test 4: ID generation
            logger.info("  Test 4: ID generation...");
            String idResponse = execute("curl -s http://localhost:" + testPort + "/api/next-id/test-entity");
            if (!idResponse.contains("\"id\":")) {
                throw new Exception("ID generation failed: " + idResponse);
            }
            result.addTestPassed("ID generation");
            logger.info("    ✓ ID generation works");

            // Test 5: Batch generation
            logger.info("  Test 5: Batch ID generation...");
            String batchResponse = execute("curl -s 'http://localhost:" + testPort + "/api/next-batch/test-entity?batchSize=5'");
            if (!batchResponse.contains("\"ids\":")) {
                throw new Exception("Batch generation failed: " + batchResponse);
            }
            result.addTestPassed("Batch ID generation");
            logger.info("    ✓ Batch generation works");

            logger.info("");
            logger.info("✓ All tests passed (" + result.getTestsPassed().size() + "/5)");

        } catch (Exception e) {
            result.addTestFailed(e.getMessage());
            throw e;
        } finally {
            // Clean up: stop binary
            if (process != null && process.isAlive()) {
                process.destroy();
                process.waitFor();
            }
        }
    }
}
