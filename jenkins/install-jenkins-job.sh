#!/bin/bash

# Install Jenkins Job for Clarity LXC Build
# This script creates the Jenkins pipeline job via API

set -e

# Load configuration from jenkins-config.yml
CONFIG_FILE="$(dirname "$0")/jenkins-config.yml"

if [ -f "$CONFIG_FILE" ]; then
    JENKINS_URL=$(grep "url:" "$CONFIG_FILE" | head -1 | awk '{print $2}' | tr -d '"')
    JENKINS_USER=$(grep "username:" "$CONFIG_FILE" | awk '{print $2}' | tr -d '"')
    JENKINS_TOKEN=$(grep "api_token:" "$CONFIG_FILE" | awk '{print $2}' | tr -d '"')
else
    # Fallback to environment variables or defaults
    JENKINS_URL="${JENKINS_URL:-http://your-jenkins-server:8080}"
    JENKINS_USER="${JENKINS_USER:-admin}"
    JENKINS_TOKEN="${JENKINS_TOKEN:-your-api-token}"
fi

JOB_NAME="clarity-lxc-manual-build"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}=== Installing Jenkins Job: ${JOB_NAME} ===${NC}"

# Function to check Jenkins connectivity
check_jenkins() {
    echo -e "${YELLOW}Checking Jenkins connectivity...${NC}"
    if curl -s -o /dev/null -w "%{http_code}" ${JENKINS_URL}/api/json | grep -q "200"; then
        echo -e "${GREEN}✓ Jenkins is reachable${NC}"
        return 0
    else
        echo -e "${RED}✗ Cannot reach Jenkins at ${JENKINS_URL}${NC}"
        return 1
    fi
}

# Create job XML configuration
create_job_config() {
    cat > job-config.xml << 'EOF'
<?xml version='1.1' encoding='UTF-8'?>
<flow-definition plugin="workflow-job@1308.v58d48a_763b_31">
  <actions>
    <org.jenkinsci.plugins.pipeline.modeldefinition.actions.DeclarativeJobAction plugin="pipeline-model-definition@2.2144.v077a_d1928a_40"/>
    <org.jenkinsci.plugins.pipeline.modeldefinition.actions.DeclarativeJobPropertyTrackerAction plugin="pipeline-model-definition@2.2144.v077a_d1928a_40">
      <jobProperties/>
      <triggers/>
      <parameters/>
      <options/>
    </org.jenkinsci.plugins.pipeline.modeldefinition.actions.DeclarativeJobPropertyTrackerAction>
  </actions>
  <description>Manual versioned build and backup of Clarity LXC container with Prometheus and Grafana</description>
  <keepDependencies>false</keepDependencies>
  <properties>
    <hudson.plugins.jira.JiraProjectProperty plugin="jira@3.10"/>
    <jenkins.model.BuildDiscarderProperty>
      <strategy class="hudson.tasks.LogRotator">
        <daysToKeep>30</daysToKeep>
        <numToKeep>10</numToKeep>
        <artifactDaysToKeep>-1</artifactDaysToKeep>
        <artifactNumToKeep>-1</artifactNumToKeep>
      </strategy>
    </jenkins.model.BuildDiscarderProperty>
    <hudson.model.ParametersDefinitionProperty>
      <parameterDefinitions>
        <hudson.model.StringParameterDefinition>
          <name>VERSION</name>
          <description>Version number for this build (e.g., 1.0.0, 2.1.3)</description>
          <defaultValue>1.0.0</defaultValue>
        </hudson.model.StringParameterDefinition>
        <hudson.model.ChoiceParameterDefinition>
          <name>BUILD_TYPE</name>
          <description>Type of build</description>
          <choices class="java.util.Arrays$ArrayList">
            <a class="string-array">
              <string>RELEASE</string>
              <string>SNAPSHOT</string>
              <string>DEVELOPMENT</string>
            </a>
          </choices>
        </hudson.model.ChoiceParameterDefinition>
        <hudson.model.BooleanParameterDefinition>
          <name>UPLOAD_TO_GDRIVE</name>
          <description>Upload backup to Google Drive after build</description>
          <defaultValue>true</defaultValue>
        </hudson.model.BooleanParameterDefinition>
      </parameterDefinitions>
    </hudson.model.ParametersDefinitionProperty>
  </properties>
  <definition class="org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition" plugin="workflow-cps@3653.v07ea_433c90b_4">
    <scm class="hudson.plugins.git.GitSCM" plugin="git@5.2.0">
      <configVersion>2</configVersion>
      <userRemoteConfigs>
        <hudson.plugins.git.UserRemoteConfig>
          <url>file:///home/mustafa/telcobright-projects/orchestrix</url>
        </hudson.plugins.git.UserRemoteConfig>
      </userRemoteConfigs>
      <branches>
        <hudson.plugins.git.BranchSpec>
          <name>*/main</name>
        </hudson.plugins.git.BranchSpec>
      </branches>
      <doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations>
      <submoduleCfg class="empty-list"/>
      <extensions/>
    </scm>
    <scriptPath>jenkins/Jenkinsfile.clarity</scriptPath>
    <lightweight>true</lightweight>
  </definition>
  <triggers/>
  <disabled>false</disabled>
</flow-definition>
EOF
}

# Alternative: Create job with inline Jenkinsfile
create_job_config_inline() {
    cat > job-config-inline.xml << 'EOF'
<?xml version='1.1' encoding='UTF-8'?>
<flow-definition plugin="workflow-job@1308.v58d48a_763b_31">
  <description>Manual versioned build and backup of Clarity LXC container with Prometheus and Grafana</description>
  <keepDependencies>false</keepDependencies>
  <properties>
    <jenkins.model.BuildDiscarderProperty>
      <strategy class="hudson.tasks.LogRotator">
        <daysToKeep>30</daysToKeep>
        <numToKeep>10</numToKeep>
      </strategy>
    </jenkins.model.BuildDiscarderProperty>
    <hudson.model.ParametersDefinitionProperty>
      <parameterDefinitions>
        <hudson.model.StringParameterDefinition>
          <name>VERSION</name>
          <description>Version number for this build (e.g., 1.0.0, 2.1.3)</description>
          <defaultValue>1.0.0</defaultValue>
        </hudson.model.StringParameterDefinition>
        <hudson.model.ChoiceParameterDefinition>
          <name>BUILD_TYPE</name>
          <description>Type of build</description>
          <choices class="java.util.Arrays$ArrayList">
            <a class="string-array">
              <string>RELEASE</string>
              <string>SNAPSHOT</string>
              <string>DEVELOPMENT</string>
            </a>
          </choices>
        </hudson.model.ChoiceParameterDefinition>
        <hudson.model.BooleanParameterDefinition>
          <name>UPLOAD_TO_GDRIVE</name>
          <description>Upload backup to Google Drive after build</description>
          <defaultValue>true</defaultValue>
        </hudson.model.BooleanParameterDefinition>
      </parameterDefinitions>
    </hudson.model.ParametersDefinitionProperty>
  </properties>
  <definition class="org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition" plugin="workflow-cps@3653.v07ea_433c90b_4">
    <script><![CDATA[
pipeline {
    agent { label 'orchestrix-agent' }
    
    environment {
        BUILD_DIR = '/home/mustafa/telcobright-projects/orchestrix/images/lxc/clarity'
        CONTAINER_NAME = 'clarity'
    }
    
    stages {
        stage('Build LXC Container') {
            steps {
                sh '''
                    cd ${BUILD_DIR}
                    sudo ./buildClarity.sh --overwrite
                '''
            }
        }
        
        stage('Verify Container') {
            steps {
                sh '''
                    sudo lxc list ${CONTAINER_NAME}
                    CONTAINER_IP=$(sudo lxc list ${CONTAINER_NAME} --format=json | jq -r '.[0].state.network.eth0.addresses[] | select(.family=="inet").address')
                    curl -f http://${CONTAINER_IP}:9090/-/healthy
                    curl -f http://${CONTAINER_IP}:3300/api/health
                '''
            }
        }
        
        stage('Upload to Google Drive') {
            steps {
                sh '''
                    cd ${BUILD_DIR}
                    LATEST_BACKUP=$(ls -t clarity-*.tar.gz | head -1)
                    rclone copy ${LATEST_BACKUP} pialmmhtb:orchestrix/lxc-images/clarity/
                '''
            }
        }
    }
}
    ]]></script>
    <sandbox>true</sandbox>
  </definition>
  <triggers/>
  <disabled>false</disabled>
</flow-definition>
EOF
}

# Function to create job via API
create_jenkins_job() {
    echo -e "${YELLOW}Creating Jenkins job...${NC}"
    
    # Check if job already exists
    if curl -s -u ${JENKINS_USER}:${JENKINS_TOKEN} \
        ${JENKINS_URL}/job/${JOB_NAME}/api/json &>/dev/null; then
        echo -e "${YELLOW}Job already exists. Updating...${NC}"
        # Update existing job
        curl -X POST \
            -u ${JENKINS_USER}:${JENKINS_TOKEN} \
            -H "Content-Type: text/xml" \
            --data-binary @job-config-inline.xml \
            ${JENKINS_URL}/job/${JOB_NAME}/config.xml
    else
        echo -e "${YELLOW}Creating new job...${NC}"
        # Create new job
        curl -X POST \
            -u ${JENKINS_USER}:${JENKINS_TOKEN} \
            -H "Content-Type: text/xml" \
            --data-binary @job-config-inline.xml \
            ${JENKINS_URL}/createItem?name=${JOB_NAME}
    fi
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Job created/updated successfully${NC}"
        echo -e "${GREEN}Job URL: ${JENKINS_URL}/job/${JOB_NAME}${NC}"
    else
        echo -e "${RED}✗ Failed to create/update job${NC}"
        return 1
    fi
}

# Function to trigger first build
trigger_build() {
    echo -e "${YELLOW}Would you like to trigger the first build now? (y/n)${NC}"
    read -r response
    
    if [[ "$response" =~ ^[Yy]$ ]]; then
        echo -e "${YELLOW}Triggering build...${NC}"
        curl -X POST \
            -u ${JENKINS_USER}:${JENKINS_TOKEN} \
            ${JENKINS_URL}/job/${JOB_NAME}/build
        
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✓ Build triggered${NC}"
            echo -e "${GREEN}Monitor at: ${JENKINS_URL}/job/${JOB_NAME}${NC}"
        else
            echo -e "${RED}✗ Failed to trigger build${NC}"
        fi
    fi
}

# Main execution
main() {
    echo -e "${YELLOW}Current configuration:${NC}"
    echo "  JENKINS_URL: ${JENKINS_URL}"
    echo "  JENKINS_USER: ${JENKINS_USER}"
    echo "  JOB_NAME: ${JOB_NAME}"
    echo ""
    
    # Check if credentials are set
    if [[ "${JENKINS_URL}" == "http://your-jenkins-server:8080" ]]; then
        echo -e "${RED}Please update the Jenkins configuration at the top of this script${NC}"
        echo "Required variables:"
        echo "  JENKINS_URL - Your Jenkins server URL"
        echo "  JENKINS_USER - Your Jenkins username"
        echo "  JENKINS_TOKEN - Your Jenkins API token"
        echo ""
        echo "To get API token:"
        echo "1. Log into Jenkins"
        echo "2. Click your username (top right)"
        echo "3. Click 'Configure'"
        echo "4. Add new API Token"
        exit 1
    fi
    
    # Check Jenkins connectivity
    if ! check_jenkins; then
        exit 1
    fi
    
    # Create job configuration
    echo -e "${YELLOW}Creating job configuration...${NC}"
    create_job_config_inline
    
    # Create Jenkins job
    create_jenkins_job
    
    # Optionally trigger build
    trigger_build
    
    # Cleanup
    rm -f job-config.xml job-config-inline.xml
    
    echo -e "${GREEN}=== Installation Complete ===${NC}"
}

# Run main function
main "$@"