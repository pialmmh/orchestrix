package com.telcobright.orchestrix.device;

import com.telcobright.orchestrix.automation.model.AutomationConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * LXC-specific device wrapper that provides convenient methods for LXC container operations.
 * Uses LocalDevice for executing LXC commands on the host, and can execute commands inside containers.
 */
public class LxcDevice implements TerminalDevice {

    private static final Logger log = Logger.getLogger(LxcDevice.class.getName());
    private final LocalDevice localDevice;
    private String currentContainer;
    private final String bridgeName;

    public LxcDevice() {
        this("lxdbr0");
    }

    public LxcDevice(String bridgeName) {
        AutomationConfig config = new AutomationConfig();
        config.setStreamOutput(true);
        config.setCommandTimeoutMs(300000); // 5 minutes for LXC operations
        this.localDevice = new LocalDevice(config);
        this.bridgeName = bridgeName;
    }

    // ========== TerminalDevice Interface Implementation ==========

    @Override
    public CompletableFuture<Boolean> connect(String hostname, int port, String username, String password) {
        // For LXC, "connect" means setting the target container
        this.currentContainer = hostname; // hostname is container name
        return CompletableFuture.completedFuture(containerExists(hostname));
    }

    @Override
    public CompletableFuture<String> send(String command) {
        return localDevice.send(command);
    }

    @Override
    public CompletableFuture<String> receive() {
        return localDevice.receive();
    }

    @Override
    public CompletableFuture<String> sendAndReceive(String command) {
        if (currentContainer != null) {
            // Execute inside container
            return execInContainer(currentContainer, command);
        } else {
            // Execute on host
            return localDevice.sendAndReceive(command);
        }
    }

    @Override
    public boolean isConnected() {
        return currentContainer == null || containerExists(currentContainer);
    }

    @Override
    public void disconnect() throws IOException {
        this.currentContainer = null;
    }

    @Override
    public String getDeviceId() {
        return currentContainer != null ? "lxc-" + currentContainer : "lxc-host";
    }

    @Override
    public String getHostname() {
        return currentContainer != null ? currentContainer : "lxc-host";
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.SSH; // Could add LXC type
    }

    @Override
    public DeviceStatus getStatus() {
        return isConnected() ? DeviceStatus.CONNECTED : DeviceStatus.DISCONNECTED;
    }

    // ========== LXC-Specific Operations ==========

    /**
     * Check if a container exists
     */
    public boolean containerExists(String containerName) {
        try {
            String output = localDevice.sendAndReceive(
                String.format("lxc list --format=json | jq -r '.[] | select(.name==\"%s\") | .name'", containerName)
            ).get();
            return output.trim().equals(containerName);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to check container existence: " + containerName, e);
            return false;
        }
    }

    /**
     * Check if a container is running
     */
    public boolean isContainerRunning(String containerName) {
        try {
            String output = localDevice.sendAndReceive(
                String.format("lxc list %s --format=json | jq -r '.[0].status'", containerName)
            ).get();
            return "Running".equals(output.trim());
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to check container status: " + containerName, e);
            return false;
        }
    }

    /**
     * Create and launch a new container
     */
    public CompletableFuture<Boolean> launchContainer(String containerName, String baseImage) {
        // Use a separate thread to handle the potentially long-running lxc launch
        return CompletableFuture.supplyAsync(() -> {
            try {
                // lxc launch can take time to download images, use a longer timeout
                String result = localDevice.sendAndReceive(
                    String.format("lxc launch %s %s 2>&1", baseImage, containerName)
                ).get(300, TimeUnit.SECONDS); // 5 minute timeout for image download

                log.info("Container launched: " + containerName);
                return !result.toLowerCase().contains("error");
            } catch (Exception e) {
                log.log(Level.SEVERE, "Failed to launch container: " + containerName, e);
                return false;
            }
        });
    }

    /**
     * Delete a container
     */
    public CompletableFuture<Boolean> deleteContainer(String containerName, boolean force) {
        String forceFlag = force ? "--force" : "";
        return localDevice.sendAndReceive(
            String.format("lxc delete %s %s", forceFlag, containerName)
        ).thenApply(output -> {
            log.info("Container deleted: " + containerName);
            return !output.toLowerCase().contains("error");
        }).exceptionally(e -> {
            log.log(Level.SEVERE, "Failed to delete container: " + containerName, e);
            return false;
        });
    }

    /**
     * Stop a container
     */
    public CompletableFuture<Boolean> stopContainer(String containerName) {
        return localDevice.sendAndReceive(
            String.format("lxc stop %s", containerName)
        ).thenApply(output -> {
            log.info("Container stopped: " + containerName);
            return !output.toLowerCase().contains("error");
        });
    }

    /**
     * Start a container
     */
    public CompletableFuture<Boolean> startContainer(String containerName) {
        return localDevice.sendAndReceive(
            String.format("lxc start %s", containerName)
        ).thenApply(output -> {
            log.info("Container started: " + containerName);
            return !output.toLowerCase().contains("error");
        });
    }

    /**
     * Execute a command inside a container
     */
    public CompletableFuture<String> execInContainer(String containerName, String command) {
        return localDevice.sendAndReceive(
            String.format("lxc exec %s -- bash -c '%s'", containerName, command.replace("'", "'\\''"))
        );
    }

    /**
     * Configure network for a container
     */
    public CompletableFuture<Boolean> configureNetwork(String containerName, String ipAddress, String gateway) {
        return CompletableFuture.allOf(
            // Remove existing network device
            localDevice.sendAndReceive(
                String.format("lxc config device remove %s eth0 2>/dev/null || true", containerName)
            ),
            // Add bridged network device
            localDevice.sendAndReceive(
                String.format("lxc config device add %s eth0 nic nictype=bridged parent=%s ipv4.address=%s",
                    containerName, bridgeName, ipAddress)
            )
        ).thenCompose(v -> {
            // Configure network inside container
            return execInContainer(containerName,
                String.format("ip addr add %s dev eth0 2>/dev/null || true", ipAddress)
            );
        }).thenCompose(v -> {
            // Add default route
            String gwWithoutMask = gateway.replace("/24", "");
            return execInContainer(containerName,
                String.format("ip route add default via %s 2>/dev/null || true", gwWithoutMask)
            );
        }).thenApply(output -> {
            log.info("Network configured for container " + containerName + ": IP=" + ipAddress + ", Gateway=" + gateway);
            return true;
        }).exceptionally(e -> {
            log.log(Level.SEVERE, "Failed to configure network for container: " + containerName, e);
            return false;
        });
    }

    /**
     * Push a file to container
     */
    public CompletableFuture<Boolean> pushFile(String localPath, String containerName, String containerPath) {
        return localDevice.sendAndReceive(
            String.format("lxc file push %s %s%s", localPath, containerName, containerPath)
        ).thenApply(output -> {
            log.info("File pushed to container " + containerName + ": " + localPath + " -> " + containerPath);
            return !output.toLowerCase().contains("error");
        });
    }

    /**
     * Pull a file from container
     */
    public CompletableFuture<Boolean> pullFile(String containerName, String containerPath, String localPath) {
        return localDevice.sendAndReceive(
            String.format("lxc file pull %s%s %s", containerName, containerPath, localPath)
        ).thenApply(output -> {
            log.info("File pulled from container " + containerName + ": " + containerPath + " -> " + localPath);
            return !output.toLowerCase().contains("error");
        });
    }

    /**
     * Create an image from a container
     */
    public CompletableFuture<Boolean> createImage(String containerName, String imageName, String description) {
        return localDevice.sendAndReceive(
            String.format("lxc publish %s --alias %s --description '%s'",
                containerName, imageName, description)
        ).thenApply(output -> {
            log.info("Image created: " + imageName + " from container " + containerName);
            return !output.toLowerCase().contains("error");
        });
    }

    /**
     * Install package in container using apt
     */
    public CompletableFuture<Boolean> installPackage(String containerName, String... packages) {
        String packageList = String.join(" ", packages);
        return execInContainer(containerName, "apt-get update")
            .thenCompose(v -> execInContainer(containerName,
                String.format("DEBIAN_FRONTEND=noninteractive apt-get install -y %s", packageList)))
            .thenApply(output -> {
                log.info("Packages installed in container " + containerName + ": " + packageList);
                return !output.toLowerCase().contains("error");
            });
    }

    /**
     * Write content to a file inside container
     */
    public CompletableFuture<Boolean> writeFileInContainer(String containerName, String path, String content) {
        try {
            // Write to temp file first
            Path tempFile = Files.createTempFile("lxc-", ".tmp");
            Files.write(tempFile, content.getBytes());

            // Push to container
            return pushFile(tempFile.toString(), containerName, path)
                .whenComplete((result, error) -> {
                    try {
                        Files.deleteIfExists(tempFile);
                    } catch (IOException e) {
                        log.warning("Failed to delete temp file: " + tempFile);
                    }
                });
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to write file to container: " + containerName, e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Create directory in container
     */
    public CompletableFuture<Boolean> createDirectoryInContainer(String containerName, String path) {
        return execInContainer(containerName, String.format("mkdir -p %s", path))
            .thenApply(output -> {
                log.info("Directory created in container " + containerName + ": " + path);
                return true;
            });
    }

    /**
     * Enable and start a systemd service in container
     */
    public CompletableFuture<Boolean> enableService(String containerName, String serviceName) {
        return execInContainer(containerName, String.format("systemctl enable %s", serviceName))
            .thenCompose(v -> execInContainer(containerName, String.format("systemctl start %s", serviceName)))
            .thenApply(output -> {
                log.info("Service enabled and started in container " + containerName + ": " + serviceName);
                return true;
            });
    }

    /**
     * Wait for container to be ready
     */
    public CompletableFuture<Boolean> waitForContainer(String containerName, int timeoutSeconds) {
        return CompletableFuture.supplyAsync(() -> {
            int waited = 0;
            while (waited < timeoutSeconds) {
                if (isContainerRunning(containerName)) {
                    log.info("Container " + containerName + " is ready");
                    return true;
                }
                try {
                    Thread.sleep(1000);
                    waited++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            log.log(Level.SEVERE, "Container " + containerName + " did not become ready within " + timeoutSeconds + " seconds");
            return false;
        });
    }
}