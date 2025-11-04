package com.telcobright.orchestrix.automation.scaffold;

import com.telcobright.orchestrix.automation.core.device.CommandExecutor;
import com.telcobright.orchestrix.automation.scaffold.entity.AlpineScaffoldConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * Java JAR Alpine Container Scaffolding
 *
 * Packages pre-built JAR files into Alpine containers, similar to how
 * we package Consul binaries. Uses existing AlpineContainerScaffold as base.
 *
 * Key Design:
 * - JAR is treated as a pre-built binary (like Consul)
 * - JRE is installed in Alpine (minimal size)
 * - Reuses AlpineContainerScaffold automation
 * - Supports multiple Java versions via config
 *
 * Workflow:
 * 1. JAR is built separately (Maven/Gradle)
 * 2. JAR placed in standalone-binaries/[app-name]/[app-name]-jar-v.[version]/
 * 3. This scaffold packages JAR + JRE into Alpine container
 *
 * Example structure:
 *   standalone-binaries/
 *     my-service/
 *       my-service-jar-v.1/
 *         my-service.jar
 *
 * Usage:
 *   JavaJarAlpineScaffold scaffold = new JavaJarAlpineScaffold(device, config);
 *   scaffold.scaffoldJarContainer();
 */
public class JavaJarAlpineScaffold extends AlpineContainerScaffold {

    private static final Logger logger = Logger.getLogger(JavaJarAlpineScaffold.class.getName());

    private final String javaVersion;
    private final String jarPath;
    private final String mainClass;
    private final String jvmOpts;
    private final boolean includePromtail;

    public JavaJarAlpineScaffold(CommandExecutor device, AlpineScaffoldConfig config) {
        super(device, config);

        // Extract Java-specific config
        this.javaVersion = config.getProperty("JAVA_VERSION", "21");
        this.jarPath = config.getBinaryPath(); // JAR is treated as binary
        this.mainClass = config.getProperty("MAIN_CLASS", "");
        this.jvmOpts = config.getProperty("JVM_OPTS", "-Xmx512m -Xms256m");
        this.includePromtail = Boolean.parseBoolean(config.getProperty("INCLUDE_PROMTAIL", "true"));
    }

    /**
     * Override to add Java-specific scaffolding
     */
    @Override
    public void scaffoldContainer() throws Exception {
        logger.info("=================================================================");
        logger.info("  Java JAR Alpine Container Scaffolding");
        logger.info("=================================================================");
        logger.info("Service: " + config.getServiceName());
        logger.info("Version: " + config.getContainerVersion());
        logger.info("JAR: " + jarPath);
        logger.info("Java Version: " + javaVersion);
        logger.info("JVM Options: " + jvmOpts);
        logger.info("Include Promtail: " + includePromtail);
        logger.info("");

        // Verify JAR exists
        if (!Files.exists(Paths.get(jarPath))) {
            throw new IllegalStateException("JAR file not found: " + jarPath +
                "\nPlease build your JAR first and place it in standalone-binaries/");
        }

        // Call parent scaffolding
        super.scaffoldContainer();

        // Add Java-specific components
        enhanceBuildScript();
        enhanceLaunchScript();
        enhanceServiceScript();

        logger.info("✓ Java JAR container scaffolding complete!");
    }

    /**
     * Enhance build.sh to install JRE and handle JAR
     */
    private void enhanceBuildScript() throws Exception {
        logger.info("Enhancing build script for Java JAR...");

        String buildScriptPath = config.getContainerPath() + "/build/build.sh";
        String content = Files.readString(Paths.get(buildScriptPath));

        // Add JRE installation after Alpine creation
        String jreInstallation = """

            # Install OpenJDK JRE
            echo "Installing OpenJDK ${JAVA_VERSION} JRE..."
            lxc exec ${BUILD_CONTAINER} -- apk add --no-cache openjdk${JAVA_VERSION}-jre

            # Create application directory
            lxc exec ${BUILD_CONTAINER} -- mkdir -p /app/lib
            lxc exec ${BUILD_CONTAINER} -- mkdir -p /app/config
            lxc exec ${BUILD_CONTAINER} -- mkdir -p /var/log/app

            # Copy JAR file (renamed from binary copy)
            echo "Copying JAR to container..."
            lxc file push ${JAR_SOURCE} ${BUILD_CONTAINER}/app/${APP_NAME}.jar
            lxc exec ${BUILD_CONTAINER} -- chmod 644 /app/${APP_NAME}.jar
            """;

        // Replace binary copy section with JAR copy
        content = content.replace(
            "# Copy binary to container",
            "# Copy JAR and install JRE" + jreInstallation
        );

        // Add Promtail installation if enabled
        if (includePromtail) {
            String promtailSetup = """

                # Install Promtail for log shipping
                echo "Installing Promtail..."
                PROMTAIL_VERSION="${PROMTAIL_VERSION:-2.9.4}"
                lxc exec ${BUILD_CONTAINER} -- wget -q -O /tmp/promtail.gz \\
                    https://github.com/grafana/loki/releases/download/v${PROMTAIL_VERSION}/promtail-linux-amd64.zip
                lxc exec ${BUILD_CONTAINER} -- unzip -q /tmp/promtail.gz -d /usr/local/bin/
                lxc exec ${BUILD_CONTAINER} -- chmod +x /usr/local/bin/promtail
                lxc exec ${BUILD_CONTAINER} -- mkdir -p /etc/promtail

                # Create Promtail config
                cat << 'PROMTAIL_EOF' | lxc exec ${BUILD_CONTAINER} -- tee /etc/promtail/config.yaml
                server:
                  http_listen_port: 9080

                positions:
                  filename: /tmp/positions.yaml

                clients:
                  - url: ${LOKI_ENDPOINT:-http://loki:3100}/loki/api/v1/push

                scrape_configs:
                  - job_name: ${APP_NAME}
                    static_configs:
                      - targets:
                          - localhost
                        labels:
                          job: ${APP_NAME}
                          container: ${CONTAINER_NAME}
                          __path__: /var/log/app/*.log
                PROMTAIL_EOF
                """;

            content = content + promtailSetup;
        }

        Files.writeString(Paths.get(buildScriptPath), content);
    }

    /**
     * Enhance launch script for JAR execution
     */
    private void enhanceLaunchScript() throws Exception {
        logger.info("Enhancing launch script for Java JAR...");

        String launchScriptPath = config.getContainerPath() +
            "/" + config.getServiceName() + "-v." + config.getContainerVersion() +
            "/generated/launch.sh";

        String content = Files.readString(Paths.get(launchScriptPath));

        // Update exec command for JAR
        String jarExec = mainClass.isEmpty()
            ? "java ${JVM_OPTS} -jar /app/${APP_NAME}.jar"
            : "java ${JVM_OPTS} -cp /app/${APP_NAME}.jar " + mainClass;

        content = content.replace(
            "/usr/local/bin/${BINARY_NAME}",
            jarExec
        );

        // Add JVM options to environment
        content = content.replace(
            "# Set environment",
            "# Set environment\nJVM_OPTS=\"" + jvmOpts + "\""
        );

        Files.writeString(Paths.get(launchScriptPath), content);
    }

    /**
     * Enhance service script for Java application
     */
    private void enhanceServiceScript() throws Exception {
        logger.info("Creating Java service configuration...");

        String configPath = config.getContainerPath() +
            "/" + config.getServiceName() + "-v." + config.getContainerVersion() +
            "/generated/sample.conf";

        // Add Java-specific configuration
        String javaConfig = """

            # ============================================
            # Java Configuration
            # ============================================
            JAVA_VERSION="%s"
            JVM_OPTS="%s"
            MAIN_CLASS="%s"

            # JVM Memory (adjust based on container limits)
            JVM_MIN_HEAP="256m"
            JVM_MAX_HEAP="512m"
            JVM_METASPACE="128m"

            # JVM GC Settings
            JVM_GC_TYPE="G1"  # G1, ZGC, Shenandoah

            # Debug Port (optional)
            DEBUG_ENABLED="false"
            DEBUG_PORT="5005"

            # Application Properties
            APP_CONFIG_FILE="/app/config/application.properties"
            LOG_LEVEL="INFO"

            """.formatted(javaVersion, jvmOpts, mainClass);

        String existingConfig = Files.readString(Paths.get(configPath));
        Files.writeString(Paths.get(configPath), existingConfig + javaConfig);

        if (includePromtail) {
            String promtailConfig = """

                # ============================================
                # Promtail Configuration
                # ============================================
                PROMTAIL_ENABLED="true"
                PROMTAIL_VERSION="2.9.4"
                LOKI_ENDPOINT="http://loki:3100"
                LOG_LABELS="app=${APP_NAME},version=${VERSION}"

                """;

            Files.writeString(Paths.get(configPath),
                Files.readString(Paths.get(configPath)) + promtailConfig);
        }
    }

    /**
     * Create OpenRC service script for Java app
     */
    @Override
    protected void createServiceScript() throws Exception {
        logger.info("Creating OpenRC service for Java application...");

        String servicePath = config.getContainerPath() + "/templates/java-service.openrc";

        String serviceContent = """
            #!/sbin/openrc-run

            name="${APP_NAME}"
            description="Java Application: ${APP_NAME}"

            # Java command
            java_cmd="/usr/bin/java"
            java_opts="${JVM_OPTS}"
            app_jar="/app/${APP_NAME}.jar"

            # Build command
            if [ -n "${MAIN_CLASS}" ]; then
                command="${java_cmd}"
                command_args="${java_opts} -cp ${app_jar} ${MAIN_CLASS}"
            else
                command="${java_cmd}"
                command_args="${java_opts} -jar ${app_jar}"
            fi

            # Process management
            command_background=true
            pidfile="/var/run/${APP_NAME}.pid"
            output_log="/var/log/app/${APP_NAME}.log"
            error_log="/var/log/app/${APP_NAME}-error.log"

            depend() {
                need net
                after firewall
            }

            start_pre() {
                # Create log directory
                mkdir -p /var/log/app

                # Check JAR exists
                if [ ! -f "${app_jar}" ]; then
                    eerror "JAR not found: ${app_jar}"
                    return 1
                fi

                # Export environment variables
                export APP_HOME="/app"
                export LOG_PATH="/var/log/app"

                # Start Promtail if enabled
                if [ "${PROMTAIL_ENABLED}" = "true" ]; then
                    /usr/local/bin/promtail -config.file=/etc/promtail/config.yaml &
                fi
            }

            stop_post() {
                # Stop Promtail
                if [ "${PROMTAIL_ENABLED}" = "true" ]; then
                    pkill -f promtail || true
                fi
            }
            """;

        Files.createDirectories(Paths.get(config.getContainerPath() + "/templates"));
        Files.writeString(Paths.get(servicePath), serviceContent);

        logger.info("✓ Java service script created");
    }
}