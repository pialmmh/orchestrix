package com.orchestrix.builder;

import expectj.ExpectJ;
import expectj.Spawn;
import expectj.TimeoutException;
import org.apache.commons.exec.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Abstract base class for all LXC container builders
 * Provides core LXC operations and utilities
 */
public abstract class LXCBuilder {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final BuildConfig config;
    protected final String containerBase;
    protected final int version;
    protected boolean verbose = true;
    
    // Color codes for output
    protected static final String GREEN = "\u001B[32m";
    protected static final String YELLOW = "\u001B[33m";
    protected static final String RED = "\u001B[31m";
    protected static final String RESET = "\u001B[0m";
    
    public LXCBuilder(BuildConfig config) {
        this.config = config;
        this.containerBase = config.getContainerName();
        this.version = config.getVersion();
    }
    
    /**
     * Execute command with verbose output (no verification)
     * Used for most commands where we just need to see output
     */
    protected int execute(String command) {
        return execute(command, true);
    }
    
    protected int execute(String command, boolean showOutput) {
        if (verbose) {
            System.out.println(YELLOW + "→ " + command + RESET);
        }
        
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            if (showOutput) {
                pb.inheritIO();
            }
            Process process = pb.start();
            boolean finished = process.waitFor(config.getTimeout(), TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                throw new BuildException("Command timed out: " + command);
            }
            
            return process.exitValue();
        } catch (Exception e) {
            log.error("Command failed: " + command, e);
            throw new BuildException("Execution failed: " + e.getMessage());
        }
    }
    
    /**
     * Execute command with expected output verification
     * Used for critical steps only
     */
    protected void executeWithVerification(String command, String expectedPattern, int timeoutSeconds) {
        System.out.println(YELLOW + "→ " + command + " [verifying]" + RESET);
        
        try {
            ExpectJ expect = new ExpectJ(timeoutSeconds);
            Spawn shell = expect.spawn("/bin/bash");
            
            shell.send(command + "\n");
            shell.expect(expectedPattern);
            
            System.out.println(GREEN + "  ✓ Verified: " + expectedPattern + RESET);
            shell.send("exit\n");
            shell.expectClose();
        } catch (Exception e) {
            throw new BuildException("Verification failed for: " + command + " - " + e.getMessage());
        }
    }
    
    /**
     * Safely delete LXC image if it exists
     */
    protected void safeDeleteImage(String imageName) {
        String checkCmd = String.format("lxc image list --format=csv | grep -q '^%s,'", imageName.replace(":", ","));
        
        if (execute(checkCmd, false) == 0) {
            System.out.println("Warning: Image " + imageName + " exists, deleting...");
            execute("lxc image delete '" + imageName + "' 2>/dev/null || true", true);
        }
    }
    
    /**
     * Safely delete LXC container if it exists
     */
    protected void safeDeleteContainer(String containerName) {
        execute("lxc delete " + containerName + " --force 2>/dev/null || true", false);
    }
    
    
    /**
     * Install packages with verbose output
     */
    protected void installPackages(String container, String description, String... packages) {
        System.out.println("  - Installing " + description);
        
        String packageList = String.join(" ", packages);
        String cmd = String.format(
            "lxc exec %s -- bash -c 'export DEBIAN_FRONTEND=noninteractive; " +
            "apt-get install -y --no-install-recommends %s'",
            container, packageList
        );
        
        execute(cmd);
    }
    
    /**
     * Wait for container network to be ready
     */
    protected void waitForNetwork(String container) {
        System.out.println("Waiting for network connectivity...");
        String cmd = String.format(
            "lxc exec %s -- bash -c 'until ping -c1 google.com &>/dev/null; do sleep 1; done'",
            container
        );
        execute(cmd, false);
    }
    
    /**
     * Create LXC image from container
     */
    protected void createImage(String container, String imageName) {
        System.out.println("Creating image: " + imageName);
        
        execute("lxc stop " + container);
        
        if (execute("lxc publish " + container + " --alias '" + imageName + "' --force", true) != 0) {
            System.out.println("Warning: Could not create image alias");
        }
        
        execute("lxc delete " + container + " --force");
    }
    
    /**
     * Generate runtime artifacts
     */
    protected void generateArtifacts() {
        String versionDir = "../" + containerBase + "-v." + version;
        String generatedDir = versionDir + "/generated";
        
        try {
            Files.createDirectories(Paths.get(generatedDir));
            
            // Generate launch.sh
            generateLaunchScript(generatedDir);
            
            // Generate sample.conf
            generateSampleConfig(generatedDir);
            
            // Generate README
            generateReadme(generatedDir);
            
            // Create artifact tarball
            createArtifactArchive(generatedDir);
            
            System.out.println(GREEN + "✅ Build complete: " + containerBase + ":" + version + RESET);
            System.out.println("   Generated files in: " + generatedDir);
            
        } catch (IOException e) {
            throw new BuildException("Failed to generate artifacts: " + e.getMessage());
        }
    }
    
    /**
     * Main build process - implements Template Method pattern
     */
    public void build() {
        String containerName = containerBase + ":" + version;
        String buildContainer = containerBase + "-v" + version + "-build";
        
        // Pre-flight checks
        performPreFlightChecks(containerName, buildContainer);
        
        // Create and setup container
        createBuildContainer(buildContainer);
        
        // Core setup that all containers need
        performCoreSetup(buildContainer);
        
        // Container-specific setup (implemented by subclasses)
        performCustomSetup(buildContainer);
        
        // Verify installation (implemented by subclasses)
        performVerification(buildContainer);
        
        // Create image and cleanup
        finalizeImage(buildContainer, containerName);
        
        // Generate artifacts
        generateArtifacts();
    }
    
    /**
     * Pre-flight checks before build
     */
    protected void performPreFlightChecks(String containerName, String buildContainer) {
        System.out.println("\nPre-flight checks...");
        
        // Check for existing image
        if (imageExists(containerName)) {
            System.out.println("⚠️  Image " + containerName + " already exists");
            if (!config.isForceMode()) {
                Scanner scanner = new Scanner(System.in);
                System.out.print("Delete existing image and rebuild? (y/N): ");
                String response = scanner.nextLine();
                if (!response.equalsIgnoreCase("y")) {
                    throw new BuildException("Build aborted - image already exists");
                }
            }
            System.out.println("  Deleting existing image...");
            execute("lxc image delete '" + containerName + "'");
        }
        
        // Check for running containers with same base name
        List<String> runningContainers = findRunningContainers(containerBase);
        if (!runningContainers.isEmpty()) {
            System.out.println("⚠️  Found running containers:");
            runningContainers.forEach(c -> System.out.println("    - " + c));
            
            if (!config.isForceMode()) {
                Scanner scanner = new Scanner(System.in);
                System.out.print("Stop and delete these containers? (y/N): ");
                String response = scanner.nextLine();
                if (!response.equalsIgnoreCase("y")) {
                    System.out.println("Tip: Stop them manually with: lxc stop <container-name>");
                    throw new BuildException("Build aborted - containers still running");
                }
            }
            
            System.out.println("  Stopping containers...");
            for (String container : runningContainers) {
                execute("lxc stop " + container + " --force 2>/dev/null || true", false);
                execute("lxc delete " + container + " --force 2>/dev/null || true", false);
            }
        }
        
        // Clean up any leftover build container
        safeDeleteContainer(buildContainer);
    }
    
    /**
     * Check if image exists
     */
    protected boolean imageExists(String imageName) {
        String checkCmd = String.format("lxc image list --format=csv | grep -q '^%s,'", 
            imageName.replace(":", ","));
        return execute(checkCmd, false) == 0;
    }
    
    /**
     * Find running containers with base name
     */
    protected List<String> findRunningContainers(String baseName) {
        List<String> containers = new ArrayList<>();
        String cmd = "lxc list --format=csv -c ns | grep '^" + baseName + ".*,RUNNING'";
        
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"bash", "-c", cmd});
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            
            while ((line = reader.readLine()) != null) {
                String containerName = line.split(",")[0];
                containers.add(containerName);
            }
        } catch (Exception e) {
            // Ignore errors, return empty list
        }
        
        return containers;
    }
    
    /**
     * Create the build container
     */
    protected void createBuildContainer(String buildContainer) {
        System.out.println("\nCreating build container...");
        execute("lxc launch images:" + config.getBaseImage() + " " + buildContainer);
        
        // Wait for container to fully initialize
        System.out.println("Waiting for container to initialize...");
        waitForContainerReady(buildContainer);
        
        waitForNetwork(buildContainer);
    }
    
    /**
     * Wait for container to be ready for commands
     * This fixes the common "System has not been booted with systemd" error
     */
    protected void waitForContainerReady(String container) {
        int maxAttempts = 30; // 30 seconds max
        int attempt = 0;
        
        while (attempt < maxAttempts) {
            try {
                // Try a simple command to check if container is ready
                Process p = Runtime.getRuntime().exec(new String[]{
                    "bash", "-c", 
                    String.format("lxc exec %s -- systemctl --version 2>&1", container)
                });
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = reader.readLine();
                
                if (line != null && line.contains("systemd")) {
                    System.out.println("  Container is ready");
                    return;
                }
            } catch (Exception e) {
                // Ignore and retry
            }
            
            attempt++;
            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(1000); // Wait 1 second before retry
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        // If we get here, container might not be fully ready but continue anyway
        System.out.println("  Container initialization timeout - continuing anyway");
    }
    
    /**
     * Core setup that all containers need
     */
    protected void performCoreSetup(String buildContainer) {
        System.out.println("\nInstalling packages...");
        
        // Update package lists
        execute(String.format(
            "lxc exec %s -- bash -c 'export DEBIAN_FRONTEND=noninteractive; apt-get update'",
            buildContainer
        ));
        
        // Install core packages if defined
        if (config.getCorePackages() != null && !config.getCorePackages().isEmpty()) {
            installPackages(buildContainer, "core utilities",
                config.getCorePackages().toArray(new String[0]));
        }
        
        // Install build packages if defined
        if (config.getBuildPackages() != null && !config.getBuildPackages().isEmpty()) {
            installPackages(buildContainer, "build tools",
                config.getBuildPackages().toArray(new String[0]));
        }
        
        // Install additional packages if defined
        if (config.getAdditionalPackages() != null && !config.getAdditionalPackages().isEmpty()) {
            installPackages(buildContainer, "additional packages",
                config.getAdditionalPackages().toArray(new String[0]));
        }
    }
    
    /**
     * Finalize the image
     */
    protected void finalizeImage(String buildContainer, String containerName) {
        System.out.println("\nFinalizing image...");
        createImage(buildContainer, containerName);
    }
    
    /**
     * Container-specific setup - subclasses implement this
     */
    protected abstract void performCustomSetup(String buildContainer);
    
    /**
     * Container-specific verification - subclasses implement this
     */
    protected abstract void performVerification(String buildContainer);
    
    /**
     * Generate standard launch script with common functionality
     * Subclasses can override to customize, or use the helper methods
     */
    protected String getLaunchScriptContent() {
        return generateLaunchScript(
            null,  // No pre-launch setup
            null,  // No post-launch setup
            null   // No runtime config
        );
    }
    
    /**
     * Helper to generate launch script with custom sections
     * @param preLaunchSetup Optional commands before launching container
     * @param postLaunchSetup Optional commands after launching container
     * @param runtimeConfig Optional runtime configuration to push to container
     */
    protected String generateLaunchScript(String preLaunchSetup, String postLaunchSetup, String runtimeConfig) {
        StringBuilder script = new StringBuilder();
        
        // Standard header
        script.append("""
            #!/bin/bash
            set -e
            
            # Load configuration
            CONFIG_FILE="${1:-$(dirname "$0")/sample.conf}"
            if [ ! -f "$CONFIG_FILE" ]; then
                echo "Error: Config file not found: $CONFIG_FILE"
                echo "Usage: $0 [config-file]"
                exit 1
            fi
            source "$CONFIG_FILE"
            
            """);
        
        // Add pre-launch setup if provided
        if (preLaunchSetup != null && !preLaunchSetup.trim().isEmpty()) {
            script.append("# Pre-launch setup\n");
            script.append(preLaunchSetup);
            script.append("\n\n");
        }
        
        // Standard image resolution and launch
        script.append("""
            # Resolve image name from directory structure
            SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
            VERSION_DIR=$(cd "$SCRIPT_DIR/.." && pwd)
            VERSION=$(basename "$VERSION_DIR" | cut -d. -f2)
            BASE_DIR=$(cd "$VERSION_DIR/.." && pwd)
            BASE_NAME=$(basename "$BASE_DIR")
            IMAGE_NAME="${BASE_NAME}:${VERSION}"
            
            # Check if container already exists
            if lxc list --format=csv -c n | grep -q "^${CONTAINER_NAME}$"; then
                echo "⚠️  Container ${CONTAINER_NAME} already exists"
                read -p "Delete existing container? (y/N): " DELETE_EXISTING
                if [[ "$DELETE_EXISTING" =~ ^[Yy]$ ]]; then
                    echo "Deleting existing container..."
                    lxc delete ${CONTAINER_NAME} --force
                else
                    echo "Aborted."
                    exit 1
                fi
            fi
            
            echo "Launching container: ${CONTAINER_NAME}"
            echo "From image: ${IMAGE_NAME}"
            
            # Launch container (use local: prefix to avoid remote interpretation)
            lxc launch local:${IMAGE_NAME} ${CONTAINER_NAME}
            
            # Wait for container to be ready
            echo "Waiting for container initialization..."
            for i in {1..30}; do
                if lxc exec ${CONTAINER_NAME} -- systemctl --version &>/dev/null 2>&1; then
                    echo "  Container ready"
                    break
                fi
                sleep 1
            done
            
            # Apply bind mounts if configured
            if [ -n "${BIND_MOUNTS+x}" ]; then
                for mount in "${BIND_MOUNTS[@]}"; do
                    if [ -n "$mount" ]; then
                        HOST_PATH="${mount%%:*}"
                        CONTAINER_PATH="${mount#*:}"
                        DEVICE_NAME=$(basename "$HOST_PATH" | tr '/' '_')
                        echo "  Mounting: $HOST_PATH -> $CONTAINER_PATH"
                        lxc config device add ${CONTAINER_NAME} ${DEVICE_NAME} disk source="${HOST_PATH}" path="${CONTAINER_PATH}"
                    fi
                done
            fi
            
            """);
        
        // Add runtime configuration if provided
        if (runtimeConfig != null && !runtimeConfig.trim().isEmpty()) {
            script.append("# Push runtime configuration\n");
            script.append(runtimeConfig);
            script.append("\n\n");
        }
        
        // Add post-launch setup if provided
        if (postLaunchSetup != null && !postLaunchSetup.trim().isEmpty()) {
            script.append("# Post-launch setup\n");
            script.append(postLaunchSetup);
            script.append("\n\n");
        }
        
        // Standard footer with status display
        script.append("""
            # Display container status
            echo ""
            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            echo "✅ Container ${CONTAINER_NAME} is running"
            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            echo ""
            echo "Container Info:"
            lxc list ${CONTAINER_NAME} --format=table --columns=ns4tS
            echo ""
            echo "Container Details:"
            lxc info ${CONTAINER_NAME} | head -15
            echo ""
            echo "Quick Commands:"
            echo "  Access:  lxc exec ${CONTAINER_NAME} -- bash"
            echo "  Stop:    lxc stop ${CONTAINER_NAME}"
            echo "  Delete:  lxc delete ${CONTAINER_NAME} --force"
            echo "  Status:  lxc list ${CONTAINER_NAME}"
            """);
        
        return script.toString();
    }
    
    /**
     * Template method - subclasses define sample config content
     */
    protected abstract String getSampleConfigContent();
    
    /**
     * Generate standard sample config
     * Subclasses can override or use this as base
     */
    protected String generateSampleConfigContent(String additionalConfig) {
        StringBuilder config = new StringBuilder();
        
        config.append(String.format("""
            # Runtime Configuration for %s v.%d
            # Generated by Orchestrix Builder
            
            # Container instance name
            CONTAINER_NAME=%s-instance-01
            
            # Bind Mounts (host:container)
            # Uncomment and modify as needed
            BIND_MOUNTS=(
                # "/host/path:/container/path"
                # "/home/user/data:/data"
            )
            
            """, containerBase, version, containerBase));
        
        if (additionalConfig != null && !additionalConfig.trim().isEmpty()) {
            config.append("# Container-specific configuration\n");
            config.append(additionalConfig);
            config.append("\n");
        }
        
        return config.toString();
    }
    
    /**
     * Template method - subclasses can override README content
     */
    protected String getReadmeContent() {
        return String.format("""
            # %s Version %d
            
            ## Quick Start
            ```bash
            cd %s-v.%d/generated
            sudo ./launch.sh
            ```
            
            ## Image
            - Name: %s:%d
            - Built: %s
            - Base: %s
            """,
            containerBase, version,
            containerBase, version,
            containerBase, version,
            new Date(),
            config.getBaseImage()
        );
    }
    
    private void generateLaunchScript(String dir) throws IOException {
        Path launchScript = Paths.get(dir, "launch.sh");
        Files.writeString(launchScript, getLaunchScriptContent());
        launchScript.toFile().setExecutable(true);
    }
    
    private void generateSampleConfig(String dir) throws IOException {
        Files.writeString(Paths.get(dir, "sample.conf"), getSampleConfigContent());
    }
    
    private void generateReadme(String dir) throws IOException {
        String filename = String.format("README-v.%d.md", version);
        Files.writeString(Paths.get(dir, filename), getReadmeContent());
    }
    
    private void createArtifactArchive(String dir) throws IOException {
        String archiveName = String.format("%s-v.%d.tar.gz", containerBase, version);
        System.out.println("Creating artifact: " + archiveName);
        
        execute(String.format(
            "cd %s && tar czf %s launch.sh sample.conf README-v.%d.md",
            dir, archiveName, version
        ));
    }
}

class BuildException extends RuntimeException {
    public BuildException(String message) {
        super(message);
    }
}