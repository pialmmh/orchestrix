package com.telcobright.orchestrix.automation.example;

import com.telcobright.orchestrix.automation.core.device.impl.RemoteSshDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Install Docker on Debian 12 (bookworm) nodes
 */
public class InstallDockerDebian {
    private static final Logger log = LoggerFactory.getLogger(InstallDockerDebian.class);

    private static final String[][] NODES = {
        {"172.27.27.132", "Node 1 (Master)"},
        {"172.27.27.136", "Node 2 (Slave)"}
    };

    public static void main(String[] args) {
        String user = "csas";
        String password = "KsCSAS!@9";

        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║       Installing Docker on Debian 12 (bookworm) Nodes         ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");

        for (String[] node : NODES) {
            String host = node[0];
            String name = node[1];

            log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("Processing {} ({})", name, host);
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            RemoteSshDevice device = new RemoteSshDevice(host, 22, user);
            try {
                device.connect(password);

                // Check if Docker is already installed
                String dockerCheck = device.executeCommand("which docker 2>/dev/null").trim();
                if (!dockerCheck.isEmpty()) {
                    log.info("Docker already installed: {}", dockerCheck);
                    String version = device.executeCommand("docker --version").trim();
                    log.info("Version: {}", version);
                    device.disconnect();
                    continue;
                }

                log.info("Installing Docker on Debian 12...");

                // Step 1: Fix apt sources if typo exists
                log.info("  Fixing apt sources typo...");
                device.executeCommand("sudo sed -i 's/bookworm-updtaes/bookworm-updates/g' /etc/apt/sources.list 2>/dev/null || true");
                device.executeCommand("sudo sed -i 's/bookworm-updtaes/bookworm-updates/g' /etc/apt/sources.list.d/*.list 2>/dev/null || true");

                // Step 2: Remove any incorrect Docker repos
                log.info("  Removing incorrect Docker repos...");
                device.executeCommand("sudo rm -f /etc/apt/sources.list.d/docker.list");
                device.executeCommand("sudo rm -f /etc/apt/keyrings/docker.gpg");

                // Step 3: Install prerequisites
                log.info("  Installing prerequisites...");
                device.executeCommand("sudo apt-get update -qq 2>&1 || true");
                device.executeCommand("sudo apt-get install -y ca-certificates curl gnupg 2>&1");

                // Step 4: Add Docker's official GPG key for Debian
                log.info("  Adding Docker GPG key...");
                device.executeCommand("sudo install -m 0755 -d /etc/apt/keyrings");
                device.executeCommand("curl -fsSL https://download.docker.com/linux/debian/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg --yes 2>&1");
                device.executeCommand("sudo chmod a+r /etc/apt/keyrings/docker.gpg");

                // Step 5: Add Docker repository for Debian
                log.info("  Adding Docker repository for Debian...");
                String addRepo = "echo 'deb [arch=amd64 signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian bookworm stable' | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null";
                device.executeCommand(addRepo);

                // Step 6: Update and install Docker
                log.info("  Installing Docker packages...");
                device.executeCommand("sudo apt-get update -qq 2>&1");
                String installResult = device.executeCommand("sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin 2>&1");
                log.debug("Install result: {}", installResult);

                // Step 7: Start Docker service
                log.info("  Starting Docker service...");
                device.executeCommand("sudo systemctl enable docker 2>&1");
                device.executeCommand("sudo systemctl start docker 2>&1");

                // Step 8: Add user to docker group
                device.executeCommand("sudo usermod -aG docker " + user + " 2>&1");

                // Verify installation
                String version = device.executeCommand("docker --version 2>&1").trim();
                if (version.contains("Docker version")) {
                    log.info("  SUCCESS: {}", version);
                } else {
                    log.warn("  Docker may not be installed correctly: {}", version);
                }

                String composeVersion = device.executeCommand("docker compose version 2>&1").trim();
                log.info("  Docker Compose: {}", composeVersion);

                // Test Docker
                String testResult = device.executeCommand("sudo docker run --rm hello-world 2>&1 | head -5").trim();
                if (testResult.contains("Hello from Docker")) {
                    log.info("  Docker test: PASSED");
                } else {
                    log.debug("  Docker test output: {}", testResult);
                }

                device.disconnect();
                log.info("{} complete!", name);

            } catch (Exception e) {
                log.error("Error on {}: {}", name, e.getMessage(), e);
            }
        }

        log.info("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("Docker installation complete!");
        log.info("Now you can run: KsNetworkPerconaDeployment");
    }
}
