package com.telcobright.orchestrix.automation.javaenv;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Java Environment Automation for checking and installing Java-related dependencies
 * Handles JDK, Maven, Gradle, and environment variables setup
 */
public class JavaEnvironmentAutomation {

    private static final Logger LOGGER = Logger.getLogger(JavaEnvironmentAutomation.class.getName());

    // Default versions (can be overridden by global config)
    private String jdkVersion = "21";
    private String mavenVersion = "3.9.6";
    private boolean autoInstall = true;
    private boolean interactive = true;

    // Environment check results
    private Map<String, CheckResult> checkResults = new HashMap<>();

    /**
     * Constructor - loads global configuration
     */
    public JavaEnvironmentAutomation() {
        loadGlobalConfig();
    }

    /**
     * Load configuration from global Orchestrix config
     */
    private void loadGlobalConfig() {
        try {
            // Try to find Orchestrix config directory
            String orchestrixHome = System.getenv("ORCHESTRIX_HOME");
            if (orchestrixHome == null) {
                // Try to detect from current path
                Path currentPath = Paths.get(System.getProperty("user.dir"));
                while (currentPath != null) {
                    Path configPath = currentPath.resolve("config/java.conf");
                    if (Files.exists(configPath)) {
                        orchestrixHome = currentPath.toString();
                        break;
                    }
                    currentPath = currentPath.getParent();
                }
            }

            if (orchestrixHome != null) {
                Path javaConfig = Paths.get(orchestrixHome, "config", "java.conf");
                if (Files.exists(javaConfig)) {
                    LOGGER.info("Loading global config from: " + javaConfig);
                    loadConfigFile(javaConfig);
                }
            }

            // Check environment variables (override config file)
            String envJavaVersion = System.getenv("ORCHESTRIX_JAVA_VERSION");
            if (envJavaVersion != null) {
                jdkVersion = envJavaVersion;
                LOGGER.info("Using Java version from environment: " + jdkVersion);
            }

            String envInteractive = System.getenv("ORCHESTRIX_INTERACTIVE");
            if (envInteractive != null) {
                interactive = Boolean.parseBoolean(envInteractive);
            }

        } catch (Exception e) {
            LOGGER.warning("Could not load global config, using defaults: " + e.getMessage());
        }
    }

    /**
     * Load configuration from file
     */
    private void loadConfigFile(Path configFile) throws Exception {
        List<String> lines = Files.readAllLines(configFile);
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("#") || line.isEmpty()) continue;

            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();

                switch (key) {
                    case "ORCHESTRIX_JAVA_VERSION":
                        jdkVersion = value;
                        break;
                    case "ORCHESTRIX_MAVEN_VERSION":
                        mavenVersion = value;
                        break;
                    case "ORCHESTRIX_JAVA_AUTO_INSTALL":
                        autoInstall = Boolean.parseBoolean(value);
                        break;
                    case "ORCHESTRIX_JAVA_INTERACTIVE":
                        interactive = Boolean.parseBoolean(value);
                        break;
                }
            }
        }
        LOGGER.info("Loaded config: Java=" + jdkVersion + ", Maven=" + mavenVersion +
                   ", AutoInstall=" + autoInstall + ", Interactive=" + interactive);
    }

    public static class CheckResult {
        public boolean isInstalled;
        public String version;
        public String path;
        public String errorMessage;

        public CheckResult(boolean isInstalled, String version, String path) {
            this.isInstalled = isInstalled;
            this.version = version;
            this.path = path;
        }

        public CheckResult(boolean isInstalled, String errorMessage) {
            this.isInstalled = isInstalled;
            this.errorMessage = errorMessage;
        }
    }

    /**
     * Check all Java environment dependencies
     */
    public Map<String, CheckResult> checkEnvironment() {
        LOGGER.info("Checking Java environment...");

        checkResults.put("java", checkJava());
        checkResults.put("javac", checkJavac());
        checkResults.put("maven", checkMaven());
        checkResults.put("gradle", checkGradle());
        checkResults.put("JAVA_HOME", checkJavaHome());

        return checkResults;
    }

    /**
     * Check if Java is installed
     */
    private CheckResult checkJava() {
        try {
            ProcessBuilder pb = new ProcessBuilder("java", "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String firstLine = reader.readLine();

            if (process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0) {
                String version = extractVersion(firstLine);
                String javaPath = getCommandPath("java");
                return new CheckResult(true, version, javaPath);
            }
        } catch (Exception e) {
            return new CheckResult(false, "Java not found: " + e.getMessage());
        }
        return new CheckResult(false, "Java not found");
    }

    /**
     * Check if javac is installed
     */
    private CheckResult checkJavac() {
        try {
            ProcessBuilder pb = new ProcessBuilder("javac", "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String firstLine = reader.readLine();

            if (process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0) {
                String version = extractVersion(firstLine);
                String javacPath = getCommandPath("javac");
                return new CheckResult(true, version, javacPath);
            }
        } catch (Exception e) {
            return new CheckResult(false, "javac not found: " + e.getMessage());
        }
        return new CheckResult(false, "javac not found");
    }

    /**
     * Check if Maven is installed
     */
    private CheckResult checkMaven() {
        try {
            ProcessBuilder pb = new ProcessBuilder("mvn", "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String firstLine = reader.readLine();

            if (process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0) {
                String version = extractMavenVersion(firstLine);
                String mvnPath = getCommandPath("mvn");
                return new CheckResult(true, version, mvnPath);
            }
        } catch (Exception e) {
            return new CheckResult(false, "Maven not found");
        }
        return new CheckResult(false, "Maven not found");
    }

    /**
     * Check if Gradle is installed
     */
    private CheckResult checkGradle() {
        try {
            ProcessBuilder pb = new ProcessBuilder("gradle", "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String output = "";
            String line;
            while ((line = reader.readLine()) != null) {
                output += line + "\n";
                if (line.startsWith("Gradle")) {
                    break;
                }
            }

            if (process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0) {
                String version = extractGradleVersion(output);
                String gradlePath = getCommandPath("gradle");
                return new CheckResult(true, version, gradlePath);
            }
        } catch (Exception e) {
            return new CheckResult(false, "Gradle not found");
        }
        return new CheckResult(false, "Gradle not found");
    }

    /**
     * Check JAVA_HOME environment variable
     */
    private CheckResult checkJavaHome() {
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isEmpty()) {
            File javaHomeDir = new File(javaHome);
            if (javaHomeDir.exists() && javaHomeDir.isDirectory()) {
                File javaBin = new File(javaHomeDir, "bin/java");
                if (javaBin.exists()) {
                    return new CheckResult(true, "Set", javaHome);
                }
            }
            return new CheckResult(false, "JAVA_HOME set but invalid: " + javaHome);
        }
        return new CheckResult(false, "JAVA_HOME not set");
    }

    /**
     * Auto-install missing Java components
     */
    public boolean autoInstall() {
        LOGGER.info("Auto-installing missing Java components...");

        if (!autoInstall) {
            LOGGER.info("Auto-installation is disabled in configuration.");
            printManualInstructions();
            return false;
        }

        boolean allSuccess = true;

        // Check what needs to be installed
        boolean needsJava = !checkResults.get("java").isInstalled || !checkResults.get("javac").isInstalled;
        boolean needsMaven = !checkResults.get("maven").isInstalled;
        boolean needsJavaHome = !checkResults.get("JAVA_HOME").isInstalled;

        if (!needsJava && !needsMaven && !needsJavaHome) {
            LOGGER.info("All required components are already installed.");
            return true;
        }

        // Interactive prompt if enabled
        if (interactive) {
            if (!promptForInstallation(needsJava, needsMaven, needsJavaHome)) {
                LOGGER.info("Installation cancelled by user.");
                printManualInstructions();
                return false;
            }
        }

        // Check for sudo/root access
        boolean isRoot = "root".equals(System.getProperty("user.name"));
        if (!isRoot && !hasSudoAccess()) {
            LOGGER.warning("No sudo access available. Manual installation required.");
            printManualInstructions();
            return false;
        }

        // Install JDK if missing
        if (needsJava) {
            allSuccess &= installJDK();
        }

        // Install Maven if missing
        if (needsMaven) {
            allSuccess &= installMaven();
        }

        // Set JAVA_HOME if not set
        if (needsJavaHome) {
            allSuccess &= setupJavaHome();
        }

        return allSuccess;
    }

    /**
     * Prompt user for installation confirmation
     */
    private boolean promptForInstallation(boolean needsJava, boolean needsMaven, boolean needsJavaHome) {
        System.out.println("\n========================================");
        System.out.println("Java Environment Installation Required");
        System.out.println("========================================");

        System.out.println("\nThe following components need to be installed:");
        if (needsJava) {
            System.out.println("  • OpenJDK " + jdkVersion + " (Java Development Kit)");
        }
        if (needsMaven) {
            System.out.println("  • Maven " + mavenVersion + " (Build tool)");
        }
        if (needsJavaHome) {
            System.out.println("  • JAVA_HOME environment variable");
        }

        // Check if running as sudo/root
        String currentUser = System.getProperty("user.name");
        if ("root".equals(currentUser)) {
            System.out.println("\nNote: Running as root user.");
        }

        System.out.println("\nWould you like to install these components now?");
        System.out.print("Type 'yes' to continue or 'no' to abort: ");

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String response = reader.readLine();
            return response != null && response.toLowerCase().startsWith("yes");
        } catch (Exception e) {
            LOGGER.warning("Could not read user input: " + e.getMessage());
            return false;
        }
    }

    /**
     * Install JDK
     */
    private boolean installJDK() {
        LOGGER.info("Installing OpenJDK " + jdkVersion + "...");

        try {
            // Check /etc/os-release first for better OS detection
            String osName = detectOSFromFile();
            if (osName == null) {
                osName = System.getProperty("os.name").toLowerCase();
            }

            ProcessBuilder pb;

            if (osName.contains("ubuntu") || osName.contains("debian")) {
                // Ubuntu/Debian
                pb = new ProcessBuilder("sudo", "apt-get", "update");
                executeProcess(pb);

                pb = new ProcessBuilder("sudo", "apt-get", "install", "-y",
                    "openjdk-" + jdkVersion + "-jdk");
            } else if (osName.contains("fedora") || osName.contains("centos") || osName.contains("rhel")) {
                // Fedora/CentOS/RHEL
                pb = new ProcessBuilder("sudo", "dnf", "install", "-y",
                    "java-" + jdkVersion + "-openjdk-devel");
            } else if (osName.contains("mac")) {
                // macOS
                pb = new ProcessBuilder("brew", "install", "openjdk@" + jdkVersion);
            } else {
                LOGGER.warning("Unsupported OS for auto-installation: " + osName);
                return false;
            }

            return executeProcess(pb);

        } catch (Exception e) {
            LOGGER.severe("Failed to install JDK: " + e.getMessage());
            return false;
        }
    }

    /**
     * Install Maven
     */
    private boolean installMaven() {
        LOGGER.info("Installing Maven...");

        try {
            // Check /etc/os-release first for better OS detection
            String osName = detectOSFromFile();
            if (osName == null) {
                osName = System.getProperty("os.name").toLowerCase();
            }

            ProcessBuilder pb;

            if (osName.contains("ubuntu") || osName.contains("debian")) {
                // Ubuntu/Debian
                pb = new ProcessBuilder("sudo", "apt-get", "install", "-y", "maven");
            } else if (osName.contains("fedora") || osName.contains("centos") || osName.contains("rhel")) {
                // Fedora/CentOS/RHEL
                pb = new ProcessBuilder("sudo", "dnf", "install", "-y", "maven");
            } else if (osName.contains("mac")) {
                // macOS
                pb = new ProcessBuilder("brew", "install", "maven");
            } else {
                LOGGER.warning("Unsupported OS for auto-installation: " + osName);
                return false;
            }

            return executeProcess(pb);

        } catch (Exception e) {
            LOGGER.severe("Failed to install Maven: " + e.getMessage());
            return false;
        }
    }

    /**
     * Setup JAVA_HOME environment variable
     */
    private boolean setupJavaHome() {
        LOGGER.info("Setting up JAVA_HOME...");

        try {
            // Find Java installation
            String javaPath = getCommandPath("java");
            if (javaPath == null) {
                return false;
            }

            // Resolve to JDK home
            Path javaBin = Paths.get(javaPath);
            Path jdkHome = javaBin.getParent().getParent();

            // Create script to set JAVA_HOME
            String profileScript = "\n# Java Environment Setup\n" +
                "export JAVA_HOME=" + jdkHome.toString() + "\n" +
                "export PATH=$JAVA_HOME/bin:$PATH\n";

            // Write to profile
            String homeDir = System.getProperty("user.home");
            Path bashProfile = Paths.get(homeDir, ".bashrc");

            if (Files.exists(bashProfile)) {
                String content = Files.readString(bashProfile);
                if (!content.contains("JAVA_HOME")) {
                    Files.writeString(bashProfile, content + profileScript);
                    LOGGER.info("Added JAVA_HOME to " + bashProfile);
                }
            }

            // Also create a script for immediate use
            Path setupScript = Paths.get("/tmp/java_env_setup.sh");
            Files.writeString(setupScript, profileScript);
            LOGGER.info("Created setup script at: " + setupScript);

            return true;

        } catch (Exception e) {
            LOGGER.severe("Failed to setup JAVA_HOME: " + e.getMessage());
            return false;
        }
    }

    /**
     * Detect OS from /etc/os-release file
     */
    private String detectOSFromFile() {
        try {
            Path osRelease = Paths.get("/etc/os-release");
            if (Files.exists(osRelease)) {
                List<String> lines = Files.readAllLines(osRelease);
                for (String line : lines) {
                    if (line.startsWith("ID=")) {
                        String osId = line.substring(3).replace("\"", "").toLowerCase();
                        LOGGER.info("Detected OS from /etc/os-release: " + osId);
                        return osId;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Could not read /etc/os-release: " + e.getMessage());
        }
        return null;
    }

    /**
     * Check if we have sudo access
     */
    private boolean hasSudoAccess() {
        try {
            ProcessBuilder pb = new ProcessBuilder("sudo", "-n", "true");
            Process process = pb.start();
            process.waitFor(2, TimeUnit.SECONDS);
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Execute a process
     */
    private boolean executeProcess(ProcessBuilder pb) {
        try {
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                LOGGER.info("  " + line);
            }

            boolean completed = process.waitFor(5, TimeUnit.MINUTES);
            return completed && process.exitValue() == 0;

        } catch (Exception e) {
            LOGGER.severe("Process execution failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get path of a command
     */
    private String getCommandPath(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", command);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String path = reader.readLine();

            if (process.waitFor(2, TimeUnit.SECONDS) && process.exitValue() == 0) {
                return path;
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    /**
     * Extract version from Java output
     */
    private String extractVersion(String versionLine) {
        if (versionLine == null) return "unknown";

        // Handle different formats:
        // openjdk version "21.0.8" 2025-07-15
        // java version "1.8.0_301"
        // javac 21.0.8

        if (versionLine.contains("\"")) {
            int start = versionLine.indexOf("\"") + 1;
            int end = versionLine.lastIndexOf("\"");
            if (end > start) {
                return versionLine.substring(start, end);
            }
        } else {
            String[] parts = versionLine.split("\\s+");
            if (parts.length > 1) {
                return parts[1];
            }
        }
        return versionLine;
    }

    /**
     * Extract Maven version
     */
    private String extractMavenVersion(String versionLine) {
        if (versionLine == null) return "unknown";
        // Apache Maven 3.9.6 (bc0240f3c744dd6b6ec2920b3cd08dcc295161ae)
        if (versionLine.contains("Apache Maven")) {
            String[] parts = versionLine.split("\\s+");
            if (parts.length > 2) {
                return parts[2];
            }
        }
        return versionLine;
    }

    /**
     * Extract Gradle version
     */
    private String extractGradleVersion(String versionOutput) {
        if (versionOutput == null) return "unknown";
        // Gradle 8.5
        String[] lines = versionOutput.split("\n");
        for (String line : lines) {
            if (line.startsWith("Gradle")) {
                String[] parts = line.split("\\s+");
                if (parts.length > 1) {
                    return parts[1];
                }
            }
        }
        return "unknown";
    }

    /**
     * Print manual installation instructions
     */
    private void printManualInstructions() {
        System.out.println("\n========================================");
        System.out.println("Manual Installation Instructions");
        System.out.println("========================================");
        System.out.println("\nFor Ubuntu/Debian:");
        System.out.println("  sudo apt-get update");
        System.out.println("  sudo apt-get install -y openjdk-" + jdkVersion + "-jdk maven");
        System.out.println("\nFor Fedora/CentOS/RHEL:");
        System.out.println("  sudo dnf install -y java-" + jdkVersion + "-openjdk-devel maven");
        System.out.println("\nFor macOS:");
        System.out.println("  brew install openjdk@" + jdkVersion + " maven");
        System.out.println("\nThen set JAVA_HOME:");
        System.out.println("  export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))");
        System.out.println("  export PATH=$JAVA_HOME/bin:$PATH");
        System.out.println("\nAdd these lines to your ~/.bashrc or ~/.zshrc file");
        System.out.println("========================================\n");
    }

    /**
     * Print environment check summary
     */
    public void printSummary() {
        System.out.println("\n========================================");
        System.out.println("Java Environment Check Summary");
        System.out.println("========================================");

        for (Map.Entry<String, CheckResult> entry : checkResults.entrySet()) {
            String component = entry.getKey();
            CheckResult result = entry.getValue();

            String status = result.isInstalled ? "✓" : "✗";
            System.out.print(status + " " + component);

            if (result.isInstalled) {
                if (result.version != null && !result.version.equals("Set")) {
                    System.out.print(" (version: " + result.version + ")");
                }
                if (result.path != null) {
                    System.out.print(" [" + result.path + "]");
                }
            } else {
                if (result.errorMessage != null) {
                    System.out.print(" - " + result.errorMessage);
                }
            }
            System.out.println();
        }
        System.out.println("========================================\n");
    }

    /**
     * Main method for testing
     */
    public static void main(String[] args) {
        JavaEnvironmentAutomation automation = new JavaEnvironmentAutomation();

        // Check environment
        automation.checkEnvironment();
        automation.printSummary();

        // Auto-install if needed
        boolean needsInstall = automation.checkResults.values().stream()
            .anyMatch(r -> !r.isInstalled);

        if (needsInstall) {
            System.out.println("Some components are missing. Attempting auto-installation...");
            if (automation.autoInstall()) {
                System.out.println("Auto-installation completed successfully!");

                // Re-check environment
                automation.checkEnvironment();
                automation.printSummary();
            } else {
                System.out.println("Auto-installation failed. Please install manually.");
            }
        } else {
            System.out.println("All Java environment components are installed!");
        }
    }
}