package com.telcobright.orchestrix.automation.binary.goid;

import com.telcobright.orchestrix.automation.binary.entity.BinaryBuildConfig;
import com.telcobright.orchestrix.automation.binary.entity.BinaryTestResult;
import com.telcobright.orchestrix.automation.core.device.LocalSshDevice;

import java.util.logging.Logger;

/**
 * Runner for building Go-ID binary
 *
 * Usage:
 *   mvn exec:java \
 *     -Dexec.mainClass="com.telcobright.orchestrix.automation.binary.goid.GoIdBinaryBuildRunner" \
 *     -Dexec.args="1"
 *
 * Args: version (e.g., "1", "2")
 */
public class GoIdBinaryBuildRunner {

    private static final Logger logger = Logger.getLogger(GoIdBinaryBuildRunner.class.getName());

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: GoIdBinaryBuildRunner <version>");
            System.err.println("");
            System.err.println("Example:");
            System.err.println("  mvn exec:java \\");
            System.err.println("    -Dexec.mainClass=\"com.telcobright.orchestrix.automation.binary.goid.GoIdBinaryBuildRunner\" \\");
            System.err.println("    -Dexec.args=\"1\"");
            System.exit(1);
        }

        String version = args[0];

        try {
            logger.info("=================================================================");
            logger.info("  Go-ID Binary Builder");
            logger.info("=================================================================\n");

            // Determine project root (assuming we're in orchestrix project)
            String projectRoot = System.getProperty("user.dir");
            if (!projectRoot.endsWith("orchestrix")) {
                // If running from orchestrix-lxc-work, adjust path
                projectRoot = projectRoot.replace("orchestrix-lxc-work", "orchestrix");
            }

            String standaloneBinariesPath = projectRoot + "/images/standalone-binaries/go-id";
            String versionPath = standaloneBinariesPath + "/go-id-binary-v." + version;
            String buildDir = "/tmp/go-id-build-" + System.currentTimeMillis();

            // Create configuration
            BinaryBuildConfig config = new BinaryBuildConfig("go-id", version);
            config.setDescription("Go-ID distributed unique ID generator");
            config.setSourceDirectory(versionPath);  // Will be used for future builds
            config.setBuildDirectory(buildDir);
            config.setOutputDirectory(versionPath);
            config.setOutputBinaryName("go-id");
            config.setTargetOS("linux");
            config.setTargetArch("amd64");
            config.setTestPort(7001);
            config.setRunTests(true);

            logger.info("Configuration:");
            logger.info("  Version: " + version);
            logger.info("  Build Dir: " + buildDir);
            logger.info("  Output: " + config.getOutputBinaryPath());
            logger.info("  Target: " + config.getTargetOS() + "/" + config.getTargetArch());
            logger.info("  Test Port: " + config.getTestPort());
            logger.info("");

            // Create SSH device (local)
            LocalSshDevice device = new LocalSshDevice();
            device.connect();

            try {
                // Create builder and build
                GoIdBinaryBuilder builder = new GoIdBinaryBuilder(device, config);
                BinaryTestResult result = builder.buildAndTest();

                logger.info("\n=================================================================");
                logger.info("  Binary Build Complete!");
                logger.info("=================================================================");
                logger.info("");
                logger.info("Binary Details:");
                logger.info("  Path: " + result.getBinaryPath());
                logger.info("  Size: " + result.getBinarySizeFormatted());
                logger.info("  Build Time: " + result.getBuildTimeFormatted());
                logger.info("");
                logger.info("Tests: " + result.getTestsPassed().size() + " passed, " +
                           result.getTestsFailed().size() + " failed");
                logger.info("");
                logger.info("Next Steps:");
                logger.info("  1. Binary ready for containerization");
                logger.info("  2. Use with Alpine LXC container (saves 85% space)");
                logger.info("  3. Run: " + config.getOutputBinaryPath());
                logger.info("");

            } finally {
                device.disconnect();
            }

        } catch (Exception e) {
            logger.severe("Binary build failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
