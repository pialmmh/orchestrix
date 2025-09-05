#!/bin/bash
# Test Jenkins agent connection with a secret

JENKINS_URL="http://172.82.66.90:8080"
AGENT_NAME="${1:-dev-env-01}"
SECRET="$2"

if [ -z "$SECRET" ]; then
    echo "Usage: $0 <agent-name> <secret>"
    echo ""
    echo "Steps to get secret:"
    echo "1. Go to Jenkins: $JENKINS_URL"
    echo "2. Navigate to: Manage Jenkins → Manage Nodes and Clouds"
    echo "3. Click 'New Node'"
    echo "4. Name: $AGENT_NAME"
    echo "5. Type: Permanent Agent"
    echo "6. Remote root: /var/jenkins"
    echo "7. Launch method: 'Launch agent by connecting it to the controller'"
    echo "8. Save and get the secret from agent page"
    echo ""
    echo "Then run: $0 $AGENT_NAME <secret-from-jenkins>"
    exit 1
fi

echo "Testing connection for agent: $AGENT_NAME"
echo "Jenkins URL: $JENKINS_URL"
echo "Secret: ${SECRET:0:10}..."

# Test with curl if Jenkins is reachable
curl -s -o /dev/null -w "Jenkins reachable: %{http_code}\n" $JENKINS_URL

echo ""
echo "To add this to config.yml:"
echo "  $AGENT_NAME:"
echo "    secret: $SECRET"
echo "    workdir: /var/jenkins"