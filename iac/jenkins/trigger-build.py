#!/usr/bin/env python3

"""
Jenkins Build Trigger Script for AI Agent
This script allows the AI agent to trigger Jenkins builds programmatically
"""

import requests
import yaml
import json
import time
import sys
import os
from typing import Dict, Optional
from urllib.parse import urljoin

class JenkinsTrigger:
    def __init__(self, config_file: str = "jenkins-config.yml"):
        """Initialize Jenkins trigger with configuration"""
        self.config = self.load_config(config_file)
        self.jenkins_url = self.config['jenkins']['server']['url']
        self.job_name = self.config['jenkins']['server']['job_name']
        self.username = self.config['jenkins']['auth']['username']
        self.api_token = self.config['jenkins']['auth']['api_token']
        
    def load_config(self, config_file: str) -> Dict:
        """Load Jenkins configuration from YAML file"""
        config_path = os.path.join(os.path.dirname(__file__), config_file)
        with open(config_path, 'r') as f:
            return yaml.safe_load(f)
    
    def trigger_build(self, 
                     container_name: str,
                     container_type: str = "custom",
                     version: str = "latest",
                     action: str = "create",
                     upload_to_gdrive: bool = True,
                     auto_start: bool = True) -> Dict:
        """
        Trigger a Jenkins build for LXD container
        
        Args:
            container_name: Name of the container to build
            container_type: Type of container (database, cache, monitoring, web, custom)
            version: Version of software to install
            action: Action to perform (create, rebuild, backup-only)
            upload_to_gdrive: Whether to upload backup to Google Drive
            auto_start: Whether to auto-start the container
            
        Returns:
            Dictionary with build information
        """
        
        # Build parameters
        params = {
            'CONTAINER_NAME': container_name,
            'CONTAINER_TYPE': container_type,
            'VERSION': version,
            'ACTION': action,
            'UPLOAD_TO_GDRIVE': str(upload_to_gdrive).lower(),
            'AUTO_START': str(auto_start).lower()
        }
        
        # Construct build URL
        build_url = urljoin(
            self.jenkins_url,
            f'/job/{self.job_name}/buildWithParameters'
        )
        
        print(f"🚀 Triggering Jenkins build for container: {container_name}")
        print(f"   URL: {build_url}")
        print(f"   Parameters: {json.dumps(params, indent=2)}")
        
        try:
            # Trigger the build
            response = requests.post(
                build_url,
                auth=(self.username, self.api_token),
                params=params,
                timeout=10
            )
            
            if response.status_code in [200, 201]:
                # Get queue item location from headers
                queue_url = response.headers.get('Location')
                
                if queue_url:
                    # Wait a moment for the build to be scheduled
                    time.sleep(2)
                    
                    # Get build number
                    build_info = self.get_build_info(queue_url)
                    
                    return {
                        'success': True,
                        'message': 'Build triggered successfully',
                        'queue_url': queue_url,
                        'build_info': build_info,
                        'console_url': self.get_console_url(build_info)
                    }
                else:
                    return {
                        'success': True,
                        'message': 'Build triggered (no queue URL returned)',
                        'status_code': response.status_code
                    }
                    
            else:
                return {
                    'success': False,
                    'message': f'Failed to trigger build: HTTP {response.status_code}',
                    'response': response.text
                }
                
        except requests.exceptions.RequestException as e:
            return {
                'success': False,
                'message': f'Connection error: {str(e)}'
            }
    
    def get_build_info(self, queue_url: str) -> Optional[Dict]:
        """Get build information from queue URL"""
        try:
            api_url = f"{queue_url}api/json"
            response = requests.get(
                api_url,
                auth=(self.username, self.api_token),
                timeout=10
            )
            
            if response.status_code == 200:
                data = response.json()
                
                # Check if build has been assigned
                if 'executable' in data:
                    return {
                        'build_number': data['executable']['number'],
                        'build_url': data['executable']['url']
                    }
                else:
                    return {
                        'status': 'queued',
                        'why': data.get('why', 'Waiting in queue')
                    }
            
        except Exception as e:
            print(f"Warning: Could not get build info: {e}")
            
        return None
    
    def get_console_url(self, build_info: Optional[Dict]) -> Optional[str]:
        """Get console output URL for the build"""
        if build_info and 'build_url' in build_info:
            return f"{build_info['build_url']}console"
        return None
    
    def check_build_status(self, build_url: str) -> Dict:
        """Check the status of a running build"""
        try:
            api_url = f"{build_url}api/json"
            response = requests.get(
                api_url,
                auth=(self.username, self.api_token),
                timeout=10
            )
            
            if response.status_code == 200:
                data = response.json()
                return {
                    'building': data.get('building', False),
                    'result': data.get('result', 'IN_PROGRESS'),
                    'duration': data.get('duration', 0),
                    'timestamp': data.get('timestamp', 0)
                }
                
        except Exception as e:
            print(f"Error checking build status: {e}")
            
        return {'error': 'Could not check build status'}
    
    def wait_for_completion(self, build_url: str, timeout: int = 600) -> Dict:
        """Wait for build to complete with timeout"""
        start_time = time.time()
        
        print(f"⏳ Waiting for build to complete (timeout: {timeout}s)...")
        
        while time.time() - start_time < timeout:
            status = self.check_build_status(build_url)
            
            if not status.get('building', True):
                return status
            
            time.sleep(10)
            print(".", end="", flush=True)
        
        return {'error': 'Build timeout', 'timeout': timeout}

def main():
    """Main function for command-line usage"""
    if len(sys.argv) < 2:
        print("Usage: python trigger-build.py <container_name> [container_type] [version]")
        print("Example: python trigger-build.py mysql database 8.0")
        sys.exit(1)
    
    container_name = sys.argv[1]
    container_type = sys.argv[2] if len(sys.argv) > 2 else "custom"
    version = sys.argv[3] if len(sys.argv) > 3 else "latest"
    
    # Initialize trigger
    trigger = JenkinsTrigger()
    
    # Trigger the build
    result = trigger.trigger_build(
        container_name=container_name,
        container_type=container_type,
        version=version
    )
    
    if result['success']:
        print(f"✅ {result['message']}")
        
        if 'console_url' in result and result['console_url']:
            print(f"📋 Console output: {result['console_url']}")
        
        if 'build_info' in result and result['build_info']:
            if 'build_number' in result['build_info']:
                print(f"🔢 Build number: #{result['build_info']['build_number']}")
                
                # Optionally wait for completion
                if input("\nWait for build to complete? (y/n): ").lower() == 'y':
                    final_status = trigger.wait_for_completion(
                        result['build_info']['build_url']
                    )
                    print(f"\n📊 Build result: {final_status.get('result', 'UNKNOWN')}")
    else:
        print(f"❌ {result['message']}")
        if 'response' in result:
            print(f"Response: {result['response']}")
        sys.exit(1)

if __name__ == "__main__":
    main()