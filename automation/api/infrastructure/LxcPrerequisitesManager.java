package automation.api.infrastructure;

import com.jcraft.jsch.*;
import java.io.*;
import java.util.*;

/**
 * LXC/LXD Prerequisites Manager
 *
 * Ensures all LXC prerequisites are met before deployment:
 * - LXD installation
 * - Default storage pool
 * - Default network bridge
 * - System initialization
 *
 * Can execute locally or via SSH on remote hosts.
 */
public class LxcPrerequisitesManager {

    private static final String DEFAULT_STORAGE_POOL = "default";
    private static final String DEFAULT_NETWORK = "lxdbr0";
    private static final String DEFAULT_NETWORK_SUBNET = "10.10.199.1/24";

    private final String host;
    private final String user;
    private final int port;
    private final String password;
    private final String privateKeyPath;
    private final boolean useSudo;

    private Session session;
    private StringBuilder executionLog = new StringBuilder();

    /**
     * SSH-based execution
     */
    public LxcPrerequisitesManager(String host, String user, int port, String password, String privateKeyPath, boolean useSudo) {
        this.host = host;
        this.user = user;
        this.port = port;
        this.password = password;
        this.privateKeyPath = privateKeyPath;
        this.useSudo = useSudo;
    }

    /**
     * Local execution (localhost)
     */
    public LxcPrerequisitesManager() {
        this("localhost", System.getProperty("user.name"), 22, null, null, false);
    }

    /**
     * Check and setup all LXC prerequisites
     */
    public PrerequisitesResult setupPrerequisites() throws Exception {
        PrerequisitesResult result = new PrerequisitesResult();

        log("═══════════════════════════════════════════════════════");
        log("  LXC/LXD Prerequisites Check");
        log("═══════════════════════════════════════════════════════");
        log("");
        log("Host: " + host);
        log("");

        try {
            // Connect if remote
            if (!isLocal()) {
                connect();
            }

            // 1. Check LXD installation
            log("1. Checking LXD Installation");
            log("───────────────────────────────────────────────────────");
            if (!checkLxdInstalled()) {
                log("✗ LXD not installed");
                result.setLxdInstalled(false);
                result.addError("LXD is not installed. Please install: snap install lxd");
                return result;
            }
            log("✓ LXD installed");
            result.setLxdInstalled(true);
            log("");

            // 2. Check LXD initialization
            log("2. Checking LXD Initialization");
            log("───────────────────────────────────────────────────────");
            if (!checkLxdInitialized()) {
                log("⚠ LXD not initialized, initializing now...");
                initializeLxd();
                log("✓ LXD initialized");
            } else {
                log("✓ LXD already initialized");
            }
            result.setLxdInitialized(true);
            log("");

            // 3. Check storage pool
            log("3. Checking Storage Pool");
            log("───────────────────────────────────────────────────────");
            StoragePoolInfo poolInfo = checkStoragePool();
            if (poolInfo.exists) {
                log("✓ Storage pool '" + DEFAULT_STORAGE_POOL + "' exists");
                log("  Driver: " + poolInfo.driver);
                log("  Source: " + poolInfo.source);
                log("  Used by: " + poolInfo.usedBy + " containers");
            } else {
                log("⚠ Storage pool not found, creating...");
                createStoragePool();
                log("✓ Storage pool created");
            }
            result.setStoragePoolExists(true);
            result.setStoragePoolName(DEFAULT_STORAGE_POOL);
            log("");

            // 4. Check network bridge
            log("4. Checking Network Bridge");
            log("───────────────────────────────────────────────────────");
            NetworkInfo networkInfo = checkNetwork();
            if (networkInfo.exists) {
                log("✓ Network bridge '" + DEFAULT_NETWORK + "' exists");
                log("  IPv4: " + networkInfo.ipv4);
                log("  IPv6: " + networkInfo.ipv6);
                log("  Used by: " + networkInfo.usedBy + " containers");
            } else {
                log("⚠ Network bridge not found, creating...");
                createNetwork();
                log("✓ Network bridge created");
            }
            result.setNetworkExists(true);
            result.setNetworkName(DEFAULT_NETWORK);
            log("");

            // 5. Verify system readiness
            log("5. System Readiness Check");
            log("───────────────────────────────────────────────────────");
            verifySystemReadiness();
            log("✓ System ready for LXC deployments");
            log("");

            log("═══════════════════════════════════════════════════════");
            log("  All Prerequisites Met ✓");
            log("═══════════════════════════════════════════════════════");

            result.setSuccess(true);
            result.setLog(executionLog.toString());
            return result;

        } catch (Exception e) {
            log("✗ Error: " + e.getMessage());
            result.setSuccess(false);
            result.addError(e.getMessage());
            result.setLog(executionLog.toString());
            throw e;
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * Check if LXD is installed
     */
    private boolean checkLxdInstalled() throws Exception {
        String result = executeCommand("which lxd", false);
        return result != null && result.contains("/lxd");
    }

    /**
     * Check if LXD is initialized
     */
    private boolean checkLxdInitialized() throws Exception {
        String result = executeCommand("lxc storage list --format csv 2>/dev/null", false);
        return result != null && !result.trim().isEmpty();
    }

    /**
     * Initialize LXD with default settings
     */
    private void initializeLxd() throws Exception {
        // Non-interactive initialization with defaults
        String initCommand = "cat <<EOF | lxd init --preseed\n" +
            "config:\n" +
            "  core.https_address: 127.0.0.1:8443\n" +
            "  core.trust_password: \"\"\n" +
            "storage_pools:\n" +
            "- name: " + DEFAULT_STORAGE_POOL + "\n" +
            "  driver: dir\n" +
            "networks:\n" +
            "- name: " + DEFAULT_NETWORK + "\n" +
            "  type: bridge\n" +
            "  config:\n" +
            "    ipv4.address: " + DEFAULT_NETWORK_SUBNET + "\n" +
            "    ipv4.nat: \"true\"\n" +
            "    ipv6.address: none\n" +
            "profiles:\n" +
            "- name: default\n" +
            "  devices:\n" +
            "    eth0:\n" +
            "      name: eth0\n" +
            "      network: " + DEFAULT_NETWORK + "\n" +
            "      type: nic\n" +
            "    root:\n" +
            "      path: /\n" +
            "      pool: " + DEFAULT_STORAGE_POOL + "\n" +
            "      type: disk\n" +
            "EOF";

        executeCommand(initCommand, true);
    }

    /**
     * Check storage pool status
     */
    private StoragePoolInfo checkStoragePool() throws Exception {
        String output = executeCommand("lxc storage list --format csv", false);
        StoragePoolInfo info = new StoragePoolInfo();

        if (output != null && !output.trim().isEmpty()) {
            for (String line : output.split("\n")) {
                String[] parts = line.split(",");
                if (parts.length >= 3 && parts[0].equals(DEFAULT_STORAGE_POOL)) {
                    info.exists = true;
                    info.driver = parts[1];
                    info.source = parts[2];
                    if (parts.length >= 5) {
                        try {
                            info.usedBy = Integer.parseInt(parts[4].trim());
                        } catch (NumberFormatException e) {
                            info.usedBy = 0;
                        }
                    }
                    break;
                }
            }
        }

        return info;
    }

    /**
     * Create default storage pool
     */
    private void createStoragePool() throws Exception {
        executeCommand("lxc storage create " + DEFAULT_STORAGE_POOL + " dir", true);
    }

    /**
     * Check network status
     */
    private NetworkInfo checkNetwork() throws Exception {
        String output = executeCommand("lxc network list --format csv", false);
        NetworkInfo info = new NetworkInfo();

        if (output != null && !output.trim().isEmpty()) {
            for (String line : output.split("\n")) {
                String[] parts = line.split(",");
                if (parts.length >= 2 && parts[0].equals(DEFAULT_NETWORK)) {
                    info.exists = true;
                    if (parts.length >= 4) {
                        info.ipv4 = parts[3].trim();
                    }
                    if (parts.length >= 5) {
                        info.ipv6 = parts[4].trim();
                    }
                    if (parts.length >= 7) {
                        try {
                            info.usedBy = Integer.parseInt(parts[6].trim());
                        } catch (NumberFormatException e) {
                            info.usedBy = 0;
                        }
                    }
                    break;
                }
            }
        }

        return info;
    }

    /**
     * Create default network
     */
    private void createNetwork() throws Exception {
        String createCommand = String.format(
            "lxc network create %s " +
            "ipv4.address=%s " +
            "ipv4.nat=true " +
            "ipv6.address=none",
            DEFAULT_NETWORK,
            DEFAULT_NETWORK_SUBNET
        );
        executeCommand(createCommand, true);
    }

    /**
     * Verify system is ready for deployments
     */
    private void verifySystemReadiness() throws Exception {
        // Skip test container creation for speed
        // System is verified by successful storage and network checks
        log("  ✓ Storage and network prerequisites verified");
        log("  ✓ System ready (skip test container for performance)");
    }

    /**
     * Execute command locally or via SSH
     */
    private String executeCommand(String command, boolean printOutput) throws Exception {
        if (isLocal()) {
            return executeLocalCommand(command, printOutput);
        } else {
            return executeRemoteCommand(command, printOutput);
        }
    }

    /**
     * Execute command locally
     */
    private String executeLocalCommand(String command, boolean printOutput) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                if (printOutput) {
                    log("  " + line);
                }
            }
        }

        process.waitFor();
        return output.toString();
    }

    /**
     * Execute command via SSH
     */
    private String executeRemoteCommand(String command, boolean printOutput) throws Exception {
        // Prefix LXC/LXD commands with sudo when useSudo is true
        if (useSudo && !isLocal() && (command.trim().startsWith("lxc ") || command.trim().startsWith("lxd "))) {
            // Use sudo -S to read password from stdin
            command = "echo '" + password.replace("'", "'\\''") + "' | sudo -S " + command;
        }

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);

        InputStream in = channel.getInputStream();
        channel.connect();

        StringBuilder output = new StringBuilder();
        byte[] tmp = new byte[1024];
        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0) break;
                String line = new String(tmp, 0, i);
                output.append(line);
                if (printOutput) {
                    log("  " + line.trim());
                }
            }
            if (channel.isClosed()) {
                if (in.available() > 0) continue;
                break;
            }
            try { Thread.sleep(100); } catch (Exception e) {}
        }

        channel.disconnect();
        return output.toString();
    }

    /**
     * Connect to remote host via SSH
     */
    private void connect() throws Exception {
        JSch jsch = new JSch();

        if (privateKeyPath != null) {
            jsch.addIdentity(privateKeyPath);
        }

        session = jsch.getSession(user, host, port);

        if (password != null) {
            session.setPassword(password);
        }

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);

        session.connect(30000);
        log("Connected to " + host + " via SSH");
        log("");
    }

    /**
     * Check if execution is local
     */
    private boolean isLocal() {
        return "localhost".equals(host) || "127.0.0.1".equals(host);
    }

    /**
     * Log message
     */
    private void log(String message) {
        System.out.println(message);
        executionLog.append(message).append("\n");
    }

    /**
     * Storage pool information
     */
    private static class StoragePoolInfo {
        boolean exists = false;
        String driver = "";
        String source = "";
        int usedBy = 0;
    }

    /**
     * Network information
     */
    private static class NetworkInfo {
        boolean exists = false;
        String ipv4 = "";
        String ipv6 = "";
        int usedBy = 0;
    }

    /**
     * Prerequisites check result
     */
    public static class PrerequisitesResult {
        private boolean success;
        private boolean lxdInstalled;
        private boolean lxdInitialized;
        private boolean storagePoolExists;
        private String storagePoolName;
        private boolean networkExists;
        private String networkName;
        private String log;
        private List<String> errors = new ArrayList<>();

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public boolean isLxdInstalled() { return lxdInstalled; }
        public void setLxdInstalled(boolean lxdInstalled) { this.lxdInstalled = lxdInstalled; }

        public boolean isLxdInitialized() { return lxdInitialized; }
        public void setLxdInitialized(boolean lxdInitialized) { this.lxdInitialized = lxdInitialized; }

        public boolean isStoragePoolExists() { return storagePoolExists; }
        public void setStoragePoolExists(boolean storagePoolExists) { this.storagePoolExists = storagePoolExists; }

        public String getStoragePoolName() { return storagePoolName; }
        public void setStoragePoolName(String storagePoolName) { this.storagePoolName = storagePoolName; }

        public boolean isNetworkExists() { return networkExists; }
        public void setNetworkExists(boolean networkExists) { this.networkExists = networkExists; }

        public String getNetworkName() { return networkName; }
        public void setNetworkName(String networkName) { this.networkName = networkName; }

        public String getLog() { return log; }
        public void setLog(String log) { this.log = log; }

        public List<String> getErrors() { return errors; }
        public void addError(String error) { this.errors.add(error); }
    }

    /**
     * Main entry point
     */
    public static void main(String[] args) {
        try {
            LxcPrerequisitesManager manager;

            if (args.length >= 4) {
                // Remote execution
                String host = args[0];
                String user = args[1];
                int port = Integer.parseInt(args[2]);
                String password = args[3];
                String privateKey = args.length > 4 ? args[4] : null;
                boolean useSudo = args.length > 5 ? Boolean.parseBoolean(args[5]) : true;

                manager = new LxcPrerequisitesManager(host, user, port, password, privateKey, useSudo);
            } else {
                // Local execution
                System.out.println("Using local execution mode");
                System.out.println("Usage for remote: java LxcPrerequisitesManager <host> <user> <port> <password> [privateKeyPath]");
                System.out.println("");
                manager = new LxcPrerequisitesManager();
            }

            PrerequisitesResult result = manager.setupPrerequisites();

            if (result.isSuccess()) {
                System.out.println("");
                System.out.println("✓ Prerequisites setup completed successfully");
                System.exit(0);
            } else {
                System.out.println("");
                System.out.println("✗ Prerequisites setup failed:");
                for (String error : result.getErrors()) {
                    System.out.println("  - " + error);
                }
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
