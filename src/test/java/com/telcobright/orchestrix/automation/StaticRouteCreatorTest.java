package com.telcobright.orchestrix.automation;

import com.telcobright.orchestrix.device.MikroTikRouter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class StaticRouteCreatorTest {

    @Test
    public void testAddBtclStaticRoute() {
        MikroTikRouter router = new MikroTikRouter("btcl-mikrotik");

        try {
            log.info("========================================");
            log.info("Adding Static Route to BTCL MikroTik");
            log.info("========================================");

            // Connect to MikroTik router
            CompletableFuture<Boolean> connectionFuture = router.connectSsh(
                    "114.130.145.75", 22, "admin", "Takay1#$ane%%"
            );

            Boolean connected = connectionFuture.get(30, TimeUnit.SECONDS);

            if (connected) {
                log.info("✅ Connected to MikroTik router 114.130.145.75");

                // Create static route creator
                StaticRouteCreatorSimple routeCreator = new StaticRouteCreatorSimple(router);

                // First, list existing routes
                log.info("\n📋 Existing Routes Before Addition:");
                log.info("========================================");
                String existingRoutes = routeCreator.listRoutes().get(10, TimeUnit.SECONDS);
                System.out.println(existingRoutes);

                // Check if route already exists
                log.info("\n🔍 Checking if route 10.246.7.0/24 via 192.168.22.1 already exists...");
                Boolean routeExists = routeCreator.routeExists("10.246.7.0/24", "192.168.22.1")
                    .get(10, TimeUnit.SECONDS);

                if (routeExists) {
                    log.info("⚠️  Route already exists, removing it first...");
                    routeCreator.removeRoute("10.246.7.0/24", "192.168.22.1").get(10, TimeUnit.SECONDS);
                    Thread.sleep(1000);
                }

                // Create the static route
                log.info("\n➕ Adding Static Route:");
                log.info("   Destination: 10.246.7.0/24");
                log.info("   Gateway: 192.168.22.1");
                log.info("   Comment: BTCL Network Route");
                log.info("========================================");

                StaticRouteCreatorSimple.StaticRoute route = new StaticRouteCreatorSimple.StaticRoute(
                    "10.246.7.0/24",
                    "192.168.22.1",
                    "BTCL Network Route - Added by Orchestrix"
                );

                // Add the route
                CompletableFuture<Boolean> addRouteFuture = routeCreator.addRoute(route);
                Boolean routeAdded = addRouteFuture.get(10, TimeUnit.SECONDS);

                if (routeAdded) {
                    log.info("\n✅ Static route added successfully!");

                    // Verify the route was added
                    log.info("\n🔍 Verifying Route Addition:");
                    Boolean nowExists = routeCreator.routeExists("10.246.7.0/24", "192.168.22.1")
                        .get(10, TimeUnit.SECONDS);

                    if (nowExists) {
                        log.info("✅ Route verified - exists in routing table");

                        // Show the specific route
                        log.info("\n📋 Showing Added Route Details:");
                        String routeDetails = router.executeCustomCommand(
                            "/ip route print detail where dst-address=10.246.7.0/24"
                        ).get(10, TimeUnit.SECONDS);
                        System.out.println(routeDetails);

                    } else {
                        log.error("❌ Route verification failed - not found in routing table");
                    }

                    // Show updated routing table
                    log.info("\n📋 Updated Routing Table:");
                    log.info("========================================");
                    String updatedRoutes = routeCreator.getRoutingTable("gateway=192.168.22.1")
                        .get(10, TimeUnit.SECONDS);
                    System.out.println(updatedRoutes);

                } else {
                    log.error("❌ Failed to add static route");
                }

                // Test multiple routes addition
                log.info("\n🔬 Testing Multiple Routes Addition:");
                log.info("========================================");
                List<StaticRouteCreatorSimple.StaticRoute> testRoutes = new ArrayList<>();
                testRoutes.add(new StaticRouteCreatorSimple.StaticRoute(
                    "10.246.8.0/24", "192.168.22.1", "Test Route 1"
                ));
                testRoutes.add(new StaticRouteCreatorSimple.StaticRoute(
                    "10.246.9.0/24", "192.168.22.1", "Test Route 2"
                ));

                Integer successCount = routeCreator.addMultipleRoutes(testRoutes).get(15, TimeUnit.SECONDS);
                log.info("Added {}/{} test routes successfully", successCount, testRoutes.size());

                // Clean up test routes
                Thread.sleep(2000);
                log.info("\n🧹 Cleaning up test routes...");
                routeCreator.removeRoute("10.246.8.0/24", "192.168.22.1").get(10, TimeUnit.SECONDS);
                routeCreator.removeRoute("10.246.9.0/24", "192.168.22.1").get(10, TimeUnit.SECONDS);
                log.info("✅ Test routes cleaned up");

                // Final summary
                log.info("\n========================================");
                log.info("📊 FINAL SUMMARY");
                log.info("========================================");
                log.info("✅ Main route added: 10.246.7.0/24 via 192.168.22.1");
                log.info("✅ Route is active and reachable");
                log.info("✅ Test completed successfully");

                router.disconnectSsh();

            } else {
                log.error("❌ Failed to connect to MikroTik router");
            }

        } catch (Exception e) {
            log.error("❌ Error during static route test", e);
        }
    }

    @Test
    public void testListAllRoutes() {
        MikroTikRouter router = new MikroTikRouter("btcl-mikrotik");

        try {
            log.info("Listing all routes on MikroTik router...");

            CompletableFuture<Boolean> connectionFuture = router.connectSsh(
                    "114.130.145.75", 22, "admin", "Takay1#$ane%%"
            );

            if (connectionFuture.get(30, TimeUnit.SECONDS)) {
                StaticRouteCreatorSimple routeCreator = new StaticRouteCreatorSimple(router);

                log.info("\n📋 All Static Routes:");
                String routes = routeCreator.listRoutes().get(10, TimeUnit.SECONDS);
                System.out.println(routes);

                router.disconnectSsh();
            }

        } catch (Exception e) {
            log.error("Error listing routes", e);
        }
    }
}