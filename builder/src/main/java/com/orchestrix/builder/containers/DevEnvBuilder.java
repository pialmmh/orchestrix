package com.orchestrix.builder.containers;

import com.orchestrix.builder.*;
import java.nio.file.*;
import java.util.Date;

/**
 * Builder for dev-env container
 * Implements specific setup and verification for development environments
 */
public class DevEnvBuilder extends LXCBuilder {
    
    private static final String SDKMAN_DIR = "/usr/local/sdkman";
    private static final String DEFAULT_JAVA = "21.0.5-tem";
    
    public DevEnvBuilder(BuildConfig config) {
        super(config);
    }
    
    @Override
    protected void performCustomSetup(String buildContainer) {
        System.out.println("\nPerforming dev-env specific setup...");
        
        // Install SDKMAN and Java
        installSDKMAN(buildContainer);
        installJava(buildContainer);
        
        // Configure SSH for development
        configureSshAutoAccept(buildContainer);
        
        // Setup Jenkins agent
        createJenkinsAgentScript(buildContainer);
        
        // Create required directories
        System.out.println("  - Creating required directories");
        execute("lxc exec " + buildContainer + " -- mkdir -p /var/jenkins /var/run/ssh-tunnels");
        
        // Enable Docker service
        System.out.println("  - Enabling Docker service");
        execute("lxc exec " + buildContainer + " -- systemctl enable docker");
    }
    
    @Override
    protected void performVerification(String buildContainer) {
        if (!config.getCustomBoolean("verify", true)) {
            System.out.println("Skipping verification (disabled in config)");
            return;
        }
        
        System.out.println("\nVerifying critical components...");
        
        // Verify Docker is installed
        executeWithVerification(
            "lxc exec " + buildContainer + " -- docker --version",
            "Docker version",
            10
        );
        
        // Verify Java is installed
        executeWithVerification(
            String.format("lxc exec %s -- bash -c 'source /etc/profile && java -version 2>&1'", buildContainer),
            "openjdk version \"21",
            10
        );
        
        System.out.println(GREEN + "  ✓ All verifications passed" + RESET);
    }
    
    private void installSDKMAN(String container) {
        System.out.println("  - Installing SDKMAN (Java version manager)");
        
        String installCmd = String.format("""
            lxc exec %s -- bash -c "
                export SDKMAN_DIR='%s'
                curl -s 'https://get.sdkman.io?rcupdate=false' | bash
                
                source \\"\\$SDKMAN_DIR/bin/sdkman-init.sh\\"
                
                echo 'sdkman_auto_answer=true' > \\$SDKMAN_DIR/etc/config
                echo 'sdkman_selfupdate_feature=false' >> \\$SDKMAN_DIR/etc/config
                echo 'sdkman_auto_env=false' >> \\$SDKMAN_DIR/etc/config
            "
            """, container, SDKMAN_DIR);
        
        execute(installCmd);
    }
    
    private void installJava(String container) {
        System.out.println("  - Installing Java 21 (Eclipse Temurin)");
        
        String javaVersion = config.getCustomString("java_version", DEFAULT_JAVA);
        
        String installCmd = String.format("""
            lxc exec %s -- bash -c "
                export SDKMAN_DIR='%s'
                source \\"\\$SDKMAN_DIR/bin/sdkman-init.sh\\"
                sdk install java %s < /dev/null || echo 'Warning: Java installation had issues'
                sdk default java %s < /dev/null || echo 'Warning: Could not set default Java'
                
                echo 'export SDKMAN_DIR=\\"%s\\"' >> /etc/profile
                echo '[[ -s \\"\\$SDKMAN_DIR/bin/sdkman-init.sh\\" ]] && source \\"\\$SDKMAN_DIR/bin/sdkman-init.sh\\"' >> /etc/profile
            "
            """, container, SDKMAN_DIR, javaVersion, javaVersion, SDKMAN_DIR);
        
        execute(installCmd);
    }
    
    private void configureSshAutoAccept(String container) {
        System.out.println("  - Configuring SSH auto-accept for dev mode");
        
        execute(String.format("""
            lxc exec %s -- bash -c 'cat > /etc/ssh/ssh_config.d/99-dev.conf << "EOF"
Host *
    StrictHostKeyChecking no
    UserKnownHostsFile /dev/null
    CheckHostIP no
    LogLevel ERROR
EOF'
            """, container));
    }
    
    private void createJenkinsAgentScript(String container) {
        System.out.println("  - Creating Jenkins agent launcher");
        
        String script = """
            #!/bin/bash
            [ -f /etc/container.conf ] && source /etc/container.conf
            
            if [ -n "$AGENT_SECRET" ] && [ -n "$JENKINS_URL" ]; then
                docker pull jenkins/inbound-agent:latest-jdk17 2>/dev/null
                docker stop jenkins-agent 2>/dev/null || true
                docker rm jenkins-agent 2>/dev/null || true
                
                docker run -d --name jenkins-agent --restart unless-stopped \\
                    -e JENKINS_URL="$JENKINS_URL" \\
                    -e JENKINS_AGENT_NAME="$JENKINS_AGENT_NAME" \\
                    -e JENKINS_SECRET="$AGENT_SECRET" \\
                    -e JENKINS_AGENT_WORKDIR="/var/jenkins" \\
                    -v /var/jenkins:/var/jenkins \\
                    jenkins/inbound-agent:latest-jdk17 > /dev/null 2>&1
                echo "Jenkins agent started"
            fi
            """;
        
        execute(String.format(
            "lxc exec %s -- bash -c 'cat > /usr/local/bin/start-jenkins-agent.sh << \"EOF\"\n%s\nEOF\nchmod +x /usr/local/bin/start-jenkins-agent.sh'",
            container, script
        ));
    }
    
    @Override
    protected String getLaunchScriptContent() {
        return """
            #!/bin/bash
            set -e
            CONFIG_FILE="${1:-$(dirname "$0")/sample.conf}"
            source "$CONFIG_FILE"
            
            # Fetch Jenkins params from instance config
            if [ -n "$JENKINS_INSTANCE" ]; then
                JENKINS_CONFIG="/home/mustafa/telcobright-projects/orchestrix/jenkins/instances/$JENKINS_INSTANCE/config.yml"
                if [ -f "$JENKINS_CONFIG" ]; then
                    JENKINS_URL=$(grep "jenkins_url:" "$JENKINS_CONFIG" | awk '{print $2}')
                    
                    if [ -n "$JENKINS_AGENT_NAME" ]; then
                        AGENT_SECRET=$(awk "/^agents:/,/^[^ ]/ {
                            if (\\$0 ~ /^  $JENKINS_AGENT_NAME:/) {found=1} 
                            else if (found && \\$0 ~ /secret:/) {gsub(/^[ \\t]+secret:[ \\t]*/, \\"\\"); print; exit}
                        }" "$JENKINS_CONFIG")
                    fi
                fi
            fi
            
            # Launch from versioned image
            SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
            VERSION_DIR=$(cd "$SCRIPT_DIR/.." && pwd)
            VERSION=$(basename "$VERSION_DIR" | cut -d. -f2)
            BASE_DIR=$(cd "$VERSION_DIR/.." && pwd)
            BASE_NAME=$(basename "$BASE_DIR")
            IMAGE_NAME="${BASE_NAME}:${VERSION}"
            
            echo "Launching container from image: ${IMAGE_NAME}"
            
            lxc launch ${IMAGE_NAME} ${CONTAINER_NAME}
            sleep 3
            
            # Apply bind mounts
            for mount in "${BIND_MOUNTS[@]}"; do
                lxc config device add ${CONTAINER_NAME} $(basename ${mount%%:*}) disk source=${mount%%:*} path=${mount#*:}
            done
            
            # Push runtime config
            cat <<EOF | lxc file push - ${CONTAINER_NAME}/etc/container.conf
            export JENKINS_URL="$JENKINS_URL"
            export JENKINS_AGENT_NAME="$JENKINS_AGENT_NAME"
            export AGENT_SECRET="$AGENT_SECRET"
            EOF
            
            # Start agent if configured
            if [ -n "$AGENT_SECRET" ]; then
                lxc exec ${CONTAINER_NAME} -- /usr/local/bin/start-jenkins-agent.sh
            fi
            
            echo "Container ${CONTAINER_NAME} launched from ${IMAGE_NAME}"
            """;
    }
    
    @Override
    protected String getSampleConfigContent() {
        return String.format("""
            # Runtime Configuration for %s v.%d
            CONTAINER_NAME=%s-instance-01
            
            # Jenkins Integration - auto-fetches from instance config
            JENKINS_INSTANCE=massivegrid01
            JENKINS_AGENT_NAME=orchestrix-agent
            
            # Bind Mounts (host:container)
            BIND_MOUNTS=(
                "/home/mustafa/telcobright-projects:/workspace"
            )
            """, containerBase, version, containerBase);
    }
    
    @Override
    protected String getReadmeContent() {
        return String.format("""
            # %s Version %d
            
            Development environment with Docker-based Jenkins agent and SDKMAN Java management.
            
            ## Quick Start
            ```bash
            cd %s-v.%d/generated
            sudo ./launch.sh
            ```
            
            ## Features
            - Jenkins Agent: Docker container (no Java in LXC)
            - Java: Version 21 via SDKMAN
            - Docker: Full support
            - Auto-config: Fetches Jenkins params from instance config
            
            ## Image
            - Name: %s:%d
            - Built: %s
            - Base: %s
            
            ## Configuration
            Edit `sample.conf`:
            - `JENKINS_INSTANCE`: Which Jenkins to connect to
            - `JENKINS_AGENT_NAME`: Agent name from Jenkins config
            - `BIND_MOUNTS`: Project directories to mount
            """,
            containerBase, version,
            containerBase, version,
            containerBase, version,
            new Date(),
            config.getBaseImage()
        );
    }
}