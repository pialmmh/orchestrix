package com.telcobright.orchestrix.automation.devices.networking.mikrotik;

import com.telcobright.orchestrix.device.MikroTikRouter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Simple static route creator automation for MikroTik routers
 */
@Slf4j
public class StaticRouteCreatorSimple {

    private final MikroTikRouter router;

    /**
     * Static route configuration
     */
    @Data
    public static class StaticRoute {
        private String destination;     // Destination network (e.g., "10.246.7.0/24")
        private String gateway;          // Gateway IP address (e.g., "192.168.22.1")
        private String comment;          // Optional comment
        private int distance = 1;        // Administrative distance (default: 1)
        private boolean disabled = false; // Whether route is disabled

        public StaticRoute(String destination, String gateway) {
            this.destination = destination;
            this.gateway = gateway;
            this.comment = "Route to " + destination;
        }

        public StaticRoute(String destination, String gateway, String comment) {
            this.destination = destination;
            this.gateway = gateway;
            this.comment = comment;
        }
    }

    public StaticRouteCreatorSimple(MikroTikRouter router) {
        this.router = router;
    }

    /**
     * Add a single static route
     */
    public CompletableFuture<Boolean> addRoute(StaticRoute route) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Adding static route: {} via {}", route.getDestination(), route.getGateway());

                // Build the command
                StringBuilder command = new StringBuilder("/ip route add");
                command.append(" dst-address=").append(route.getDestination());
                command.append(" gateway=").append(route.getGateway());
                command.append(" distance=").append(route.getDistance());

                if (route.getComment() != null && !route.getComment().isEmpty()) {
                    command.append(" comment=\"").append(route.getComment()).append("\"");
                }

                if (route.isDisabled()) {
                    command.append(" disabled=yes");
                }

                log.debug("Executing command: {}", command);

                // Execute the command
                CompletableFuture<String> future = router.executeCustomCommand(command.toString());
                String result = future.get(10, TimeUnit.SECONDS);

                // Check for errors
                if (result != null && (result.contains("failure") || result.contains("error"))) {
                    log.error("Failed to add route: {}", result);
                    return false;
                }

                log.info("✅ Static route added successfully: {} via {}", route.getDestination(), route.getGateway());
                return true;

            } catch (Exception e) {
                log.error("Failed to add static route: {}", e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * Add multiple static routes
     */
    public CompletableFuture<Integer> addMultipleRoutes(List<StaticRoute> routes) {
        return CompletableFuture.supplyAsync(() -> {
            int successCount = 0;
            log.info("Adding {} static routes...", routes.size());

            for (StaticRoute route : routes) {
                try {
                    Boolean success = addRoute(route).get(10, TimeUnit.SECONDS);
                    if (success) {
                        successCount++;
                    }
                } catch (Exception e) {
                    log.error("Failed to add route {} via {}: {}",
                        route.getDestination(), route.getGateway(), e.getMessage());
                }
            }

            log.info("✅ Successfully added {}/{} routes", successCount, routes.size());
            return successCount;
        });
    }

    /**
     * List all static routes
     */
    public CompletableFuture<String> listRoutes() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Listing all static routes...");
                CompletableFuture<String> future = router.executeCustomCommand("/ip route print detail");
                return future.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("Failed to list routes: {}", e.getMessage());
                return "Error: " + e.getMessage();
            }
        });
    }

    /**
     * Check if a specific route exists
     */
    public CompletableFuture<Boolean> routeExists(String destination, String gateway) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String command = String.format(
                    "/ip route print where dst-address=%s gateway=%s",
                    destination, gateway
                );

                CompletableFuture<String> future = router.executeCustomCommand(command);
                String result = future.get(10, TimeUnit.SECONDS);

                return result != null && !result.trim().isEmpty();

            } catch (Exception e) {
                log.error("Failed to check route existence: {}", e.getMessage());
                return false;
            }
        });
    }

    /**
     * Remove a static route
     */
    public CompletableFuture<Boolean> removeRoute(String destination, String gateway) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Removing static route: {} via {}", destination, gateway);

                String command = String.format(
                    "/ip route remove [find dst-address=%s gateway=%s]",
                    destination, gateway
                );

                CompletableFuture<String> future = router.executeCustomCommand(command);
                String result = future.get(10, TimeUnit.SECONDS);

                // Check for errors
                if (result != null && (result.contains("failure") || result.contains("error"))) {
                    log.error("Failed to remove route: {}", result);
                    return false;
                }

                log.info("✅ Static route removed: {} via {}", destination, gateway);
                return true;

            } catch (Exception e) {
                log.error("Failed to remove route: {}", e.getMessage());
                return false;
            }
        });
    }

    /**
     * Enable a disabled route
     */
    public CompletableFuture<Boolean> enableRoute(String destination, String gateway) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Enabling route: {} via {}", destination, gateway);

                String command = String.format(
                    "/ip route enable [find dst-address=%s gateway=%s]",
                    destination, gateway
                );

                CompletableFuture<String> future = router.executeCustomCommand(command);
                future.get(10, TimeUnit.SECONDS);

                log.info("✅ Route enabled: {} via {}", destination, gateway);
                return true;

            } catch (Exception e) {
                log.error("Failed to enable route: {}", e.getMessage());
                return false;
            }
        });
    }

    /**
     * Disable a route
     */
    public CompletableFuture<Boolean> disableRoute(String destination, String gateway) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Disabling route: {} via {}", destination, gateway);

                String command = String.format(
                    "/ip route disable [find dst-address=%s gateway=%s]",
                    destination, gateway
                );

                CompletableFuture<String> future = router.executeCustomCommand(command);
                future.get(10, TimeUnit.SECONDS);

                log.info("✅ Route disabled: {} via {}", destination, gateway);
                return true;

            } catch (Exception e) {
                log.error("Failed to disable route: {}", e.getMessage());
                return false;
            }
        });
    }

    /**
     * Get routing table with specific filter
     */
    public CompletableFuture<String> getRoutingTable(String filter) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String command = "/ip route print";
                if (filter != null && !filter.isEmpty()) {
                    command += " where " + filter;
                }

                CompletableFuture<String> future = router.executeCustomCommand(command);
                return future.get(10, TimeUnit.SECONDS);

            } catch (Exception e) {
                log.error("Failed to get routing table: {}", e.getMessage());
                return "Error: " + e.getMessage();
            }
        });
    }
}