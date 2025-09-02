pipeline {
    agent { label 'orchestrix-agent' }
    
    parameters {
        string(name: 'VERSION', defaultValue: '1.0.0', description: 'Version number')
        choice(name: 'BUILD_TYPE', choices: ['DEVELOPMENT', 'SNAPSHOT', 'RELEASE'], description: 'Build type')
        booleanParam(name: 'UPLOAD_TO_GDRIVE', defaultValue: false, description: 'Upload to Google Drive')
    }
    
    environment {
        BUILD_DIR = '/home/mustafa/telcobright-projects/orchestrix/images/lxc/clarity'
    }
    
    stages {
        stage('Build Container') {
            steps {
                echo "Building Clarity ${params.VERSION} - ${params.BUILD_TYPE}"
                sh """
                    cd ${BUILD_DIR}
                    echo "Current directory:"
                    pwd
                    echo "Files:"
                    ls -la
                    echo "Running build script..."
                    sudo ./buildClarity.sh --overwrite
                """
            }
        }
        
        stage('Verify') {
            steps {
                sh """
                    echo "Checking container..."
                    sudo lxc list clarity
                    cd ${BUILD_DIR}
                    echo "Backup files:"
                    ls -la clarity*.tar.gz | head -5
                """
            }
        }
        
        stage('Upload') {
            when {
                expression { params.UPLOAD_TO_GDRIVE == true }
            }
            steps {
                sh """
                    cd ${BUILD_DIR}
                    LATEST=\$(ls -t clarity*.tar.gz | head -1)
                    echo "Would upload: \${LATEST}"
                    rclone copy "\${LATEST}" "pialmmhtb:orchestrix/lxc-images/clarity/${params.VERSION}/"
                """
            }
        }
    }
    
    post {
        success {
            echo "Build successful!"
        }
        failure {
            echo "Build failed!"
        }
    }
}