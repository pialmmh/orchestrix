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
            shell.close();
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
     * Check for running containers with same base name
     */
    protected boolean checkRunningContainers() {
        String cmd = "lxc list --format=csv -c n | grep '^" + containerBase + "'";
        
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"bash", "-c", cmd});
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            List<String> running = new ArrayList<>();
            String line;
            
            while ((line = reader.readLine()) != null) {
                running.add(line);
            }
            
            if (!running.isEmpty()) {
                System.out.println("Warning: Found running containers:");
                running.forEach(c -> System.out.println("  - " + c));
                
                if (!config.isForceMode()) {
                    Scanner scanner = new Scanner(System.in);
                    System.out.print("Continue anyway? (y/N): ");
                    String response = scanner.nextLine();
                    return response.equalsIgnoreCase("y");
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("Could not check running containers", e);
            return true;
        }
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
        if (!checkRunningContainers()) {
            throw new BuildException("Build aborted by user");
        }
        safeDeleteImage(containerName);
        safeDeleteContainer(buildContainer);
    }
    
    /**
     * Create the build container
     */
    protected void createBuildContainer(String buildContainer) {
        System.out.println("\nCreating build container...");
        execute("lxc launch images:" + config.getBaseImage() + " " + buildContainer);
        waitForNetwork(buildContainer);
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
     * Template method - subclasses define launch script content
     */
    protected abstract String getLaunchScriptContent();
    
    /**
     * Template method - subclasses define sample config content
     */
    protected abstract String getSampleConfigContent();
    
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