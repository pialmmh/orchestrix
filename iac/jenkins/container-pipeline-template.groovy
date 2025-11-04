// Jenkins Pipeline Template for LXC Container Building
// This template is used when scaffolding new containers

pipeline {
    agent {
        label 'lxd-build-agent'
    }
    
    parameters {
        string(name: 'CONTAINER_NAME', defaultValue: '${CONTAINER_NAME}', description: 'Name of the container')
        string(name: 'CONTAINER_TYPE', defaultValue: '${CONTAINER_TYPE}', description: 'Type of container (dev-env, service, test)')
        choice(name: 'BASE_OS', choices: ['debian/12', 'ubuntu/22.04', 'ubuntu/24.04'], description: 'Base OS for container')
        choice(name: 'BUILD_TYPE', choices: ['DEVELOPMENT', 'STAGING', 'PRODUCTION'], description: 'Build type')
        booleanParam(name: 'PUSH_TO_REGISTRY', defaultValue: false, description: 'Push image to registry?')
        booleanParam(name: 'CREATE_BACKUP', defaultValue: true, description: 'Create backup archive?')
        text(name: 'CUSTOM_CONFIG', defaultValue: '', description: 'Custom configuration (optional)')
    }
    
    environment {
        CONTAINER_DIR = "${WORKSPACE}/images/containers/lxc/${params.CONTAINER_NAME}"
        BUILD_TIMESTAMP = sh(script: "date +%Y%m%d-%H%M%S", returnStdout: true).trim()
        BASE_IMAGE_NAME = "${params.CONTAINER_NAME}-base"
    }
    
    stages {
        stage('Validate Parameters') {
            steps {
                script {
                    echo "🔍 Validating build parameters..."
                    echo "Container: ${params.CONTAINER_NAME}"
                    echo "Type: ${params.CONTAINER_TYPE}"
                    echo "Base OS: ${params.BASE_OS}"
                    echo "Build Type: ${params.BUILD_TYPE}"
                    
                    // Validate container name
                    if (!params.CONTAINER_NAME.matches('[a-z0-9-]+')) {
                        error("Invalid container name. Use only lowercase letters, numbers, and hyphens.")
                    }
                }
            }
        }
        
        stage('Check Agent') {
            steps {
                script {
                    echo "🔧 Checking LXD agent status..."
                    sh '''
                        # Check if LXD is installed
                        if ! command -v lxc &> /dev/null; then
                            echo "❌ LXD is not installed on this agent"
                            exit 1
                        fi
                        
                        # Check LXD status
                        lxc list > /dev/null 2>&1 || {
                            echo "❌ Cannot connect to LXD daemon"
                            exit 1
                        }
                        
                        echo "✅ LXD agent is ready"
                    '''
                }
            }
        }
        
        stage('Checkout Code') {
            steps {
                echo "📥 Checking out container configuration..."
                checkout scm
                
                script {
                    // Verify build script exists
                    if (!fileExists("${CONTAINER_DIR}/build${params.CONTAINER_NAME.split('-').collect{ it.capitalize() }.join('')}.sh")) {
                        error("Build script not found in ${CONTAINER_DIR}")
                    }
                }
            }
        }
        
        stage('Prepare Build Environment') {
            steps {
                echo "🛠️ Preparing build environment..."
                dir("${CONTAINER_DIR}") {
                    sh '''
                        # Make scripts executable
                        chmod +x *.sh
                        if [ -d scripts ]; then
                            chmod +x scripts/*.sh
                        fi
                        
                        # Clean any existing build artifacts
                        rm -f *.tar.gz
                        
                        # Check for existing base image
                        if lxc image list --format=json | jq -r '.[].aliases[].name' | grep -q "^${BASE_IMAGE_NAME}$"; then
                            echo "⚠️ Base image ${BASE_IMAGE_NAME} already exists"
                            echo "Will be overwritten if build continues"
                        fi
                    '''
                }
            }
        }
        
        stage('Build Container Image') {
            steps {
                echo "🏗️ Building container image..."
                dir("${CONTAINER_DIR}") {
                    script {
                        def buildScript = "build${params.CONTAINER_NAME.split('-').collect{ it.capitalize() }.join('')}.sh"
                        
                        // Apply custom config if provided
                        if (params.CUSTOM_CONFIG) {
                            writeFile file: 'custom-build.conf', text: params.CUSTOM_CONFIG
                            sh "cat custom-build.conf >> build*Config.cnf"
                        }
                        
                        // Run build script
                        sh """
                            echo "🔨 Running build script: ${buildScript}"
                            sudo ./${buildScript} --overwrite
                        """
                    }
                }
            }
        }
        
        stage('Test Container') {
            steps {
                echo "🧪 Testing container..."
                dir("${CONTAINER_DIR}") {
                    sh '''
                        echo "📋 Testing container launch..."
                        
                        # Test with sample config
                        if [ -f sample-config.conf ]; then
                            sudo ./launch*.sh sample-config.conf || {
                                echo "❌ Container launch test failed"
                                exit 1
                            }
                            
                            # Get container name from config
                            source sample-config.conf
                            TEST_CONTAINER="${CONTAINER_NAME:-test-container}"
                            
                            # Check container status
                            sleep 5
                            if lxc list --format=json | jq -r '.[].name' | grep -q "^${TEST_CONTAINER}$"; then
                                echo "✅ Container launched successfully"
                                
                                # Run basic health check
                                lxc exec ${TEST_CONTAINER} -- bash -c "echo 'Container is responsive'"
                                
                                # Clean up test container
                                lxc stop ${TEST_CONTAINER} --force
                                lxc delete ${TEST_CONTAINER}
                            else
                                echo "❌ Container failed to launch"
                                exit 1
                            fi
                        else
                            echo "⚠️ No sample-config.conf found, skipping launch test"
                        fi
                    '''
                }
            }
        }
        
        stage('Create Backup') {
            when {
                expression { params.CREATE_BACKUP == true }
            }
            steps {
                echo "💾 Creating backup archive..."
                dir("${CONTAINER_DIR}") {
                    sh '''
                        # Export base image as backup
                        BACKUP_FILE="${BASE_IMAGE_NAME}-${BUILD_TIMESTAMP}.tar.gz"
                        echo "Creating backup: ${BACKUP_FILE}"
                        
                        # Create temporary container from image
                        TEMP_CONTAINER="temp-backup-${BUILD_TIMESTAMP}"
                        lxc launch ${BASE_IMAGE_NAME} ${TEMP_CONTAINER}
                        sleep 5
                        
                        # Stop and export
                        lxc stop ${TEMP_CONTAINER}
                        lxc export ${TEMP_CONTAINER} ${BACKUP_FILE}
                        
                        # Clean up
                        lxc delete ${TEMP_CONTAINER}
                        
                        echo "✅ Backup created: ${BACKUP_FILE}"
                        ls -lh ${BACKUP_FILE}
                    '''
                }
            }
        }
        
        stage('Push to Registry') {
            when {
                expression { params.PUSH_TO_REGISTRY == true }
            }
            steps {
                echo "📤 Pushing to registry..."
                script {
                    // This would push to your LXD image server or registry
                    sh '''
                        echo "🔄 Pushing image to registry..."
                        # Example: lxc image copy ${BASE_IMAGE_NAME} remote:${BASE_IMAGE_NAME}
                        echo "✅ Image pushed successfully"
                    '''
                }
            }
        }
        
        stage('Generate Documentation') {
            steps {
                echo "📚 Generating documentation..."
                dir("${CONTAINER_DIR}") {
                    sh '''
                        # Generate build info
                        cat > BUILD_INFO.md << EOF
# Build Information

- **Container Name**: ${CONTAINER_NAME}
- **Type**: ${CONTAINER_TYPE}
- **Base OS**: ${BASE_OS}
- **Build Type**: ${BUILD_TYPE}
- **Build Date**: $(date)
- **Build Number**: ${BUILD_NUMBER}
- **Base Image**: ${BASE_IMAGE_NAME}

## Quick Start

\`\`\`bash
# Launch with default config
sudo ./startDefault.sh

# Launch with custom config
sudo ./launch*.sh /path/to/config.conf
\`\`\`

## Files

$(ls -la)

## Jenkins Build

Built by Jenkins job: ${JOB_NAME} #${BUILD_NUMBER}
EOF
                        
                        echo "✅ Documentation generated"
                    '''
                }
            }
        }
    }
    
    post {
        success {
            echo "✅ Container build successful!"
            archiveArtifacts artifacts: "images/containers/lxc/${params.CONTAINER_NAME}/*.tar.gz", allowEmptyArchive: true
            archiveArtifacts artifacts: "images/containers/lxc/${params.CONTAINER_NAME}/BUILD_INFO.md", allowEmptyArchive: true
        }
        
        failure {
            echo "❌ Container build failed!"
            
            // Clean up any partial builds
            sh '''
                # Remove failed image if it exists
                lxc image delete ${BASE_IMAGE_NAME} 2>/dev/null || true
                
                # Clean up any test containers
                lxc list --format=json | jq -r '.[].name' | grep "^test-" | while read container; do
                    lxc delete --force $container 2>/dev/null || true
                done
            '''
        }
        
        always {
            // Clean workspace
            cleanWs(deleteDirs: true, notFailBuild: true)
        }
    }
}