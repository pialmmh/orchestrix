package com.orchestrix.automation.ssh.example;

import com.orchestrix.automation.ssh.api.SSHClient;
import com.orchestrix.automation.ssh.core.BaseSSHClient;

/**
 * Example SSH client implementation
 */
public class SSHExample {
    
    /**
     * Concrete SSH client implementation
     */
    static class OrchestriXSSHClient extends BaseSSHClient {
        
        @Override
        protected void beforeConnect() throws SSHException {
            System.out.println("Connecting to SSH server...");
        }
        
        @Override
        protected void afterConnect() throws SSHException {
            System.out.println("Successfully connected!");
        }
        
        @Override
        protected void configureJSch() {
            super.configureJSch();
            // Add custom configuration if needed
        }
    }
    
    public static void main(String[] args) {
        // Example 1: Basic connection and command execution
        basicExample();
        
        // Example 2: File transfer
        fileTransferExample();
        
        // Example 3: Script execution
        scriptExecutionExample();
        
        // Example 4: Tunnel creation
        tunnelExample();
    }
    
    private static void basicExample() {
        System.out.println("\n=== Basic SSH Example ===");
        
        try (SSHClient ssh = new OrchestriXSSHClient()) {
            // Connect to server
            ssh.connect("127.0.0.1", 22, "mustafa", "/home/mustafa/.ssh/id_rsa");
            
            // Execute simple command
            SSHClient.CommandResult result = ssh.executeCommand("uname -a");
            System.out.println("Command output: " + result.getStdout());
            System.out.println("Exit code: " + result.getExitCode());
            
            // Execute command with timeout
            result = ssh.executeCommand("sleep 2 && echo 'Done'", 5000);
            System.out.println("Timed command: " + result.getStdout());
            
            // Check disk usage
            result = ssh.executeCommand("df -h");
            System.out.println("Disk usage:\n" + result.getStdout());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void fileTransferExample() {
        System.out.println("\n=== File Transfer Example ===");
        
        try (SSHClient ssh = new OrchestriXSSHClient()) {
            ssh.connect("127.0.0.1", 22, "mustafa", "/home/mustafa/.ssh/id_rsa");
            
            // Upload a file
            String localFile = "/tmp/test.txt";
            String remoteFile = "/tmp/uploaded.txt";
            
            // Create test file
            ssh.executeCommand("echo 'Test content' > " + localFile);
            
            // Upload
            ssh.uploadFile(localFile, remoteFile);
            System.out.println("File uploaded successfully");
            
            // Verify
            SSHClient.CommandResult result = ssh.executeCommand("cat " + remoteFile);
            System.out.println("Uploaded content: " + result.getStdout());
            
            // Download
            String downloadPath = "/tmp/downloaded.txt";
            ssh.downloadFile(remoteFile, downloadPath);
            System.out.println("File downloaded to: " + downloadPath);
            
            // Cleanup
            ssh.executeCommand("rm -f " + localFile + " " + remoteFile);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void scriptExecutionExample() {
        System.out.println("\n=== Script Execution Example ===");
        
        try (SSHClient ssh = new OrchestriXSSHClient()) {
            ssh.connect("127.0.0.1", 22, "mustafa", "/home/mustafa/.ssh/id_rsa");
            
            // Create a script
            String script = """
                #!/bin/bash
                echo "Script started"
                echo "Current directory: $(pwd)"
                echo "User: $(whoami)"
                echo "Date: $(date)"
                
                # Check system info
                echo "CPU count: $(nproc)"
                echo "Memory: $(free -h | grep Mem | awk '{print $2}')"
                
                echo "Script completed"
                """;
            
            // Execute script
            SSHClient.CommandResult result = ssh.executeScript(script);
            System.out.println("Script output:\n" + result.getStdout());
            System.out.println("Script exit code: " + result.getExitCode());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void tunnelExample() {
        System.out.println("\n=== SSH Tunnel Example ===");
        
        try (SSHClient ssh = new OrchestriXSSHClient()) {
            ssh.connect("127.0.0.1", 22, "mustafa", "/home/mustafa/.ssh/id_rsa");
            
            // Create tunnel to MySQL
            int localPort = ssh.createTunnel(13306, "127.0.0.1", 3306);
            System.out.println("Tunnel created on local port: " + localPort);
            System.out.println("You can now connect to MySQL via localhost:" + localPort);
            
            // Keep tunnel open for a bit
            Thread.sleep(5000);
            
            System.out.println("Closing tunnel...");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}