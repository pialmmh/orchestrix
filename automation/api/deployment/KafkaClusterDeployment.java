package automation.api.deployment;

import automation.api.deployment.KafkaZookeeperDeployment;
import automation.api.deployment.KafkaZookeeperDeployment.DeploymentResult;

/**
 * 3-Server Kafka & Zookeeper Cluster Deployment
 *
 * Deploys to:
 * - Server 1: 123.200.0.50:8210
 * - Server 2: 123.200.0.117:8210
 * - Server 3: 123.200.0.51:8210
 */
public class KafkaClusterDeployment {

    public static void main(String[] args) {
        String user = "tbsms";
        String password = "TB@l38800";
        int port = 8210;
        boolean useSudo = true;

        String[] hosts = {
            "123.200.0.50",
            "123.200.0.117",
            "123.200.0.51"
        };

        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("  Kafka & Zookeeper 3-Node Cluster Deployment");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println();

        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < hosts.length; i++) {
            String host = hosts[i];
            int serverNum = i + 1;

            System.out.println("───────────────────────────────────────────────────────");
            System.out.println("  Server " + serverNum + ": " + host);
            System.out.println("───────────────────────────────────────────────────────");
            System.out.println();

            try {
                // Step 1: Clean up old Docker Kafka on first server
                if (serverNum == 1) {
                    System.out.println("Cleaning up old Docker Kafka container on " + host + "...");
                    cleanupDockerKafka(host, user, port, password, useSudo);
                    System.out.println("✓ Docker Kafka cleanup complete");
                    System.out.println();
                }

                // Step 2: Deploy Kafka + Zookeeper
                KafkaZookeeperDeployment deployment = new KafkaZookeeperDeployment(
                    host, user, port, password, useSudo
                );

                DeploymentResult result = deployment.deploy();

                if (result.isSuccess()) {
                    System.out.println();
                    System.out.println("✓ Server " + serverNum + " deployment successful!");
                    System.out.println("  Zookeeper: " + result.getZookeeperIp() + ":" + result.getZookeeperPort());
                    System.out.println("  Kafka:     " + result.getKafkaIp() + ":" + result.getKafkaPort());
                    System.out.println();
                    successCount++;
                } else {
                    System.err.println("✗ Server " + serverNum + " deployment failed!");
                    System.err.println("  Error: " + result.getError());
                    System.err.println();
                    failCount++;
                }

            } catch (Exception e) {
                System.err.println("✗ Server " + serverNum + " deployment failed with exception!");
                System.err.println("  Exception: " + e.getMessage());
                e.printStackTrace();
                System.err.println();
                failCount++;
            }

            // Small delay between servers
            if (i < hosts.length - 1) {
                try { Thread.sleep(2000); } catch (InterruptedException e) {}
            }
        }

        // Final summary
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("  Deployment Complete");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println();
        System.out.println("Successful: " + successCount + "/" + hosts.length);
        System.out.println("Failed:     " + failCount + "/" + hosts.length);
        System.out.println();

        if (successCount == hosts.length) {
            System.out.println("✓ All servers deployed successfully!");
            System.exit(0);
        } else {
            System.err.println("✗ Some deployments failed. Check logs above.");
            System.exit(1);
        }
    }

    /**
     * Clean up Docker Kafka container on remote server
     */
    private static void cleanupDockerKafka(String host, String user, int port, String password, boolean useSudo) {
        try {
            com.jcraft.jsch.JSch jsch = new com.jcraft.jsch.JSch();
            com.jcraft.jsch.Session session = jsch.getSession(user, host, port);
            session.setPassword(password);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect(30000);

            String[] commands = {
                "docker ps -a | grep kafka | awk '{print $1}' | xargs -r docker stop",
                "docker ps -a | grep kafka | awk '{print $1}' | xargs -r docker rm",
                "docker volume ls | grep kafka | awk '{print $2}' | xargs -r docker volume rm",
                "rm -rf ~/kafka-data ~/kafka-logs"
            };

            for (String cmd : commands) {
                com.jcraft.jsch.ChannelExec channel = (com.jcraft.jsch.ChannelExec) session.openChannel("exec");
                if (useSudo) {
                    channel.setCommand("echo '" + password + "' | sudo -S " + cmd);
                } else {
                    channel.setCommand(cmd);
                }
                channel.connect();
                Thread.sleep(1000);
                channel.disconnect();
            }

            session.disconnect();

        } catch (Exception e) {
            System.err.println("Warning: Docker cleanup encountered errors (may be normal if no Docker containers exist)");
            System.err.println("  " + e.getMessage());
        }
    }
}
