package com.telcobright.orchestrix.automation.scaffold.goid;

import com.telcobright.orchestrix.automation.scaffold.AlpineContainerScaffold;
import com.telcobright.orchestrix.automation.scaffold.entity.AlpineScaffoldConfig;
import com.telcobright.orchestrix.automation.core.device.LocalSshDevice;

import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * Runner for scaffolding Go-ID Alpine container
 *
 * Creates go-id-v.2 with Alpine Linux and standalone binary
 * Follows LXC container conventions with versioning
 *
 * Usage:
 *   mvn exec:java \
 *     -Dexec.mainClass="com.telcobright.orchestrix.automation.scaffold.goid.GoIdAlpineScaffoldRunner"
 *
 * Or pass version directly:
 *   mvn exec:java \
 *     -Dexec.mainClass="com.telcobright.orchestrix.automation.scaffold.goid.GoIdAlpineScaffoldRunner" \
 *     -Dexec.args="1"
 */
public class GoIdAlpineScaffoldRunner {

    private static final Logger logger = Logger.getLogger(GoIdAlpineScaffoldRunner.class.getName());

    public static void main(String[] args) {
        try {
            logger.info("=================================================================");
            logger.info("  Go-ID Alpine Container Scaffolding");
            logger.info("=================================================================");
            logger.info("");

            // Connect to local device
            LocalSshDevice device = new LocalSshDevice();
            device.connect();

            try {
                // Create configuration
                AlpineScaffoldConfig config = new AlpineScaffoldConfig("Go-ID", "go-id", "1");
                config.setServiceDescription("Distributed unique ID generation service");
                config.setServicePort(7001);
                config.setBinaryName("go-id");
                config.setBinaryBuilderClass("com.telcobright.orchestrix.automation.binary.goid.GoIdBinaryBuildRunner");

                // Additional API endpoints for documentation
                config.setAdditionalEndpoints("""
                    - `GET /api/next-id/:entityName` - Generate single ID
                    - `GET /api/next-batch/:entityName?batchSize=N` - Generate batch
                    - `GET /api/status/:entityName` - Entity status
                    - `DELETE /api/entity/:entityName` - Delete/reset entity
                    - `GET /api/list` - List all entities
                    - `GET /api/types` - Available ID types""");

                // Create scaffold
                AlpineContainerScaffold scaffold = new AlpineContainerScaffold(device, config);

                // Check for existing binaries
                List<String> versions = scaffold.checkExistingBinaries();

                String selectedVersion = null;

                if (versions.isEmpty()) {
                    logger.warning("");
                    logger.warning("No Go-ID binaries found!");
                    logger.warning("");
                    logger.warning("Please build a binary first:");
                    logger.warning("  cd " + config.getStandaloneBinaryPath());
                    logger.warning("  mvn exec:java \\");
                    logger.warning("    -Dexec.mainClass=\"com.telcobright.orchestrix.automation.binary.goid.GoIdBinaryBuildRunner\" \\");
                    logger.warning("    -Dexec.args=\"1\"");
                    logger.warning("");
                    device.disconnect();
                    System.exit(1);

                } else if (args.length > 0) {
                    // Version passed as argument
                    selectedVersion = args[0];
                    if (!versions.contains(selectedVersion)) {
                        logger.warning("Version " + selectedVersion + " not found!");
                        logger.warning("Available versions: " + String.join(", ", versions));
                        device.disconnect();
                        System.exit(1);
                    }

                } else if (versions.size() == 1) {
                    // Only one version available
                    selectedVersion = versions.get(0);
                    logger.info("Using version: " + selectedVersion);

                } else {
                    // Multiple versions, let user select
                    logger.info("");
                    logger.info("Multiple binary versions available:");
                    for (int i = 0; i < versions.size(); i++) {
                        logger.info("  " + (i + 1) + ". Version " + versions.get(i));
                    }

                    Scanner scanner = new Scanner(System.in);
                    System.out.print("\nSelect version (1-" + versions.size() + "): ");
                    int choice = scanner.nextInt();

                    if (choice < 1 || choice > versions.size()) {
                        logger.warning("Invalid choice!");
                        device.disconnect();
                        System.exit(1);
                    }

                    selectedVersion = versions.get(choice - 1);
                }

                // Set binary version and path
                config.setBinaryVersion(selectedVersion);
                String binaryPath = config.getStandaloneBinaryPath() +
                                   "/go-id-binary-v." + selectedVersion + "/go-id";
                config.setBinaryPath(binaryPath);

                logger.info("");
                logger.info("Selected binary: " + binaryPath);
                logger.info("");

                // Confirm with user
                logger.info("This will create:");
                logger.info("  Directory: " + config.getContainerPath() + "/" + config.getVersionDirName());
                logger.info("  Container: Alpine-based, ~25 MB (85% smaller than Debian)");
                logger.info("  Binary: Pre-built standalone from version " + selectedVersion);
                logger.info("");

                // If version was passed as argument, skip confirmation
                if (args.length == 0) {
                    Scanner scanner = new Scanner(System.in);
                    System.out.print("Continue? (y/n): ");
                    String answer = scanner.nextLine().toLowerCase();

                    if (!answer.equals("y") && !answer.equals("yes")) {
                        logger.info("Cancelled.");
                        device.disconnect();
                        System.exit(0);
                    }
                } else {
                    logger.info("Auto-continuing (version specified via argument)...");
                }

                logger.info("");

                // Scaffold the container
                scaffold.scaffoldContainer();

            } finally {
                device.disconnect();
            }

        } catch (Exception e) {
            logger.severe("Scaffolding failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}