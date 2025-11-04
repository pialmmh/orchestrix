pipeline {
    agent { label 'orchestrix-agent' }
    
    parameters {
        string(name: 'VERSION', defaultValue: '1.0.0', description: 'Version number')
        choice(name: 'BUILD_TYPE', choices: ['DEVELOPMENT', 'SNAPSHOT', 'RELEASE'], description: 'Build type')
        booleanParam(name: 'CONFIGURE_TUNNELS', defaultValue: true, description: 'Configure default tunnels')
        booleanParam(name: 'START_TUNNELS', defaultValue: false, description: 'Start tunnels after build')
        text(name: 'TUNNEL_CONFIG', defaultValue: '', description: 'Custom tunnel configuration (optional)')
    }
    
    environment {
        BUILD_DIR = '/home/mustafa/telcobright-projects/orchestrix/images/containers/lxc/ssh-tunnel'
        CONTAINER_NAME = 'ssh-tunnel'
    }
    
    stages {
        stage('Build Info') {
            steps {
                echo "================================"
                echo "Building SSH Tunnel Container"
                echo "Version: ${params.VERSION}"
                echo "Type: ${params.BUILD_TYPE}"
                echo "Configure Tunnels: ${params.CONFIGURE_TUNNELS}"
                echo "Start Tunnels: ${params.START_TUNNELS}"
                echo "================================"
            }
        }
        
        stage('Pre-Build Check') {
            steps {
                sh '''
                    cd $BUILD_DIR
                    echo "Build directory contents:"
                    ls -la
                    
                    # Check if container already exists
                    if sudo lxc list | grep -q "${CONTAINER_NAME}"; then
                        echo "Container ${CONTAINER_NAME} exists"
                    else
                        echo "Container ${CONTAINER_NAME} does not exist"
                    fi
                '''
            }
        }
        
        stage('Build Container') {
            steps {
                sh '''
                    cd $BUILD_DIR
                    chmod +x buildSSHTunnel.sh
                    
                    echo "Building SSH tunnel container..."
                    sudo ./buildSSHTunnel.sh --overwrite
                    
                    echo "Build complete"
                '''
            }
        }
        
        stage('Configure Tunnels') {
            when {
                expression { params.CONFIGURE_TUNNELS == true }
            }
            steps {
                script {
                    if (params.TUNNEL_CONFIG?.trim()) {
                        sh """
                            echo "Applying custom tunnel configuration..."
                            sudo lxc exec ${CONTAINER_NAME} -- bash -c 'cat > /etc/ssh-tunnels/tunnels.conf << EOF
${params.TUNNEL_CONFIG}
EOF'
                        """
                    } else {
                        echo "Using default tunnel configuration"
                    }
                }
                
                sh '''
                    echo "Current tunnel configuration:"
                    sudo lxc exec ${CONTAINER_NAME} -- cat /etc/ssh-tunnels/tunnels.conf | grep -v password
                '''
            }
        }
        
        stage('Start Services') {
            when {
                expression { params.START_TUNNELS == true }
            }
            steps {
                sh '''
                    echo "Enabling and starting tunnel services..."
                    sudo lxc exec ${CONTAINER_NAME} -- systemctl enable ssh-tunnels
                    sudo lxc exec ${CONTAINER_NAME} -- systemctl enable tunnel-health.timer
                    sudo lxc exec ${CONTAINER_NAME} -- systemctl start ssh-tunnels
                    sudo lxc exec ${CONTAINER_NAME} -- systemctl start tunnel-health.timer
                    
                    sleep 5
                    
                    echo "Tunnel status:"
                    sudo lxc exec ${CONTAINER_NAME} -- /usr/local/bin/tunnel-manager.sh status
                '''
            }
        }
        
        stage('Verify Container') {
            steps {
                sh '''
                    echo "Container information:"
                    sudo lxc list ${CONTAINER_NAME}
                    
                    # Get container IP
                    CONTAINER_IP=$(sudo lxc list ${CONTAINER_NAME} --format=json | jq -r '.[0].state.network.eth0.addresses[] | select(.family=="inet").address')
                    echo "Container IP: ${CONTAINER_IP}"
                    
                    echo ""
                    echo "Available commands:"
                    echo "  Manage tunnels: ./scripts/manage-tunnels.sh"
                    echo "  Check status: sudo lxc exec ${CONTAINER_NAME} -- /usr/local/bin/tunnel-manager.sh status"
                    echo "  View logs: sudo lxc exec ${CONTAINER_NAME} -- tail -f /var/log/ssh-tunnels/*.log"
                '''
            }
        }
        
        stage('Create Backup') {
            steps {
                sh '''
                    cd $BUILD_DIR
                    BACKUP_FILE="${CONTAINER_NAME}-${VERSION}-${BUILD_TYPE}-$(date +%Y%m%d-%H%M%S).tar.gz"
                    
                    echo "Creating backup: ${BACKUP_FILE}"
                    sudo lxc stop ${CONTAINER_NAME}
                    sudo lxc export ${CONTAINER_NAME} "${BACKUP_FILE}"
                    sudo lxc start ${CONTAINER_NAME}
                    
                    echo "Backup created:"
                    ls -lh ${BACKUP_FILE}
                '''
            }
        }
    }
    
    post {
        success {
            echo """
                ✅ SSH Tunnel Container Build Successful!
                
                Container: ${CONTAINER_NAME}
                Version: ${params.VERSION}-${params.BUILD_TYPE}
                
                Next steps:
                1. Configure tunnels: sudo lxc exec ${CONTAINER_NAME} -- nano /etc/ssh-tunnels/tunnels.conf
                2. Start tunnels: sudo lxc exec ${CONTAINER_NAME} -- systemctl start ssh-tunnels
                3. Check status: ./scripts/manage-tunnels.sh status
            """
        }
        failure {
            echo "❌ Build failed. Check console output for errors."
        }
    }
}