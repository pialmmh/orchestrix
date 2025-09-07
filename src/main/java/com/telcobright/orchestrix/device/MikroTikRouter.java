package com.telcobright.orchestrix.device;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class MikroTikRouter extends Router {
    
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private Session routerOSSession;
    private ChannelShell routerOSShell;
    private PrintWriter routerOSWriter;
    private BufferedReader routerOSReader;
    
    public MikroTikRouter() {
        super();
        this.vendor = DeviceVendor.MIKROTIK.getDisplayName();
    }
    
    public MikroTikRouter(String deviceId) {
        super(deviceId);
        this.vendor = DeviceVendor.MIKROTIK.getDisplayName();
    }
    
    @Override
    public CompletableFuture<String> getSystemInfo() {
        try {
            return sendAndReceive("/system identity print");
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    public CompletableFuture<String> getSystemResources() {
        try {
            return sendAndReceive("/system resource print");
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    public CompletableFuture<String> getVersion() {
        try {
            return sendAndReceive("/system package print");
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    public CompletableFuture<String> enableInterface(String interfaceName) {
        try {
            String command = String.format("/interface enable %s", interfaceName);
            return sendAndReceive(command);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    public CompletableFuture<String> disableInterface(String interfaceName) {
        try {
            String command = String.format("/interface disable %s", interfaceName);
            return sendAndReceive(command);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    public CompletableFuture<String> configureOspf(String networkAddress, String area) {
        try {
            String command = String.format("/routing ospf network add network=%s area=%s", networkAddress, area);
            return sendAndReceive(command);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    public CompletableFuture<String> enableOspfRouter(String routerId) {
        try {
            String command = String.format("/routing ospf instance set default router-id=%s", routerId);
            return sendAndReceive(command);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    public CompletableFuture<String> addNatRule(String chain, String srcAddress, String action, String toAddresses) {
        try {
            String command = String.format("/ip firewall nat add chain=%s src-address=%s action=%s to-addresses=%s", 
                                          chain, srcAddress, action, toAddresses);
            return sendAndReceive(command);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    public CompletableFuture<String> executeCustomCommand(String command) {
        try {
            log.info("Executing custom MikroTik command: {}", command);
            return sendAndReceive(command);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    public CompletableFuture<Boolean> connect(String hostname, int port, String username, String password) throws IOException {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.password = password;
        this.status = DeviceStatus.CONNECTING;
        
        log.info("Connecting to MikroTik RouterOS at {}:{}", hostname, port);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create RouterOS-specific SSH session
                JSch jsch = new JSch();
                routerOSSession = jsch.getSession(username, hostname, port);
                routerOSSession.setPassword(password);
                
                // RouterOS SSH configuration
                routerOSSession.setConfig("StrictHostKeyChecking", "no");
                routerOSSession.setConfig("UserKnownHostsFile", "/dev/null");
                routerOSSession.setConfig("CheckHostIP", "no");
                routerOSSession.setConfig("LogLevel", "ERROR");
                
                routerOSSession.connect(30000);
                
                // Open RouterOS shell
                routerOSShell = (ChannelShell) routerOSSession.openChannel("shell");
                routerOSShell.setPtyType("vt100");
                routerOSShell.connect();
                
                routerOSWriter = new PrintWriter(routerOSShell.getOutputStream(), true);
                routerOSReader = new BufferedReader(new InputStreamReader(routerOSShell.getInputStream()));
                
                // Wait for RouterOS to be ready (banner + prompt)
                if (waitForRouterOSPrompt()) {
                    this.status = DeviceStatus.CONNECTED;
                    log.info("Successfully connected to MikroTik RouterOS: {}", deviceId);
                    return true;
                } else {
                    this.status = DeviceStatus.ERROR;
                    log.error("Failed to get RouterOS prompt for: {}", deviceId);
                    return false;
                }
                
            } catch (Exception e) {
                log.error("Failed to connect to MikroTik router {}: {}", deviceId, e.getMessage());
                this.status = DeviceStatus.ERROR;
                return false;
            }
        }, executorService);
    }
    
    @Override
    public void disconnect() throws IOException {
        log.info("Disconnecting from MikroTik router {}", deviceId);
        
        try {
            if (routerOSWriter != null) routerOSWriter.close();
            if (routerOSReader != null) routerOSReader.close();
            if (routerOSShell != null && routerOSShell.isConnected()) routerOSShell.disconnect();
            if (routerOSSession != null && routerOSSession.isConnected()) routerOSSession.disconnect();
        } catch (Exception e) {
            log.error("Error disconnecting RouterOS session: {}", e.getMessage());
        }
        
        this.status = DeviceStatus.DISCONNECTED;
    }
    
    @Override
    public CompletableFuture<String> sendAndReceive(String command) throws IOException {
        return executeRouterOSCommand(command);
    }
    
    private CompletableFuture<String> executeRouterOSCommand(String command) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isRouterOSConnected()) {
                throw new RuntimeException("RouterOS not connected");
            }
            
            try {
                log.debug("Executing RouterOS command: {}", command);
                
                // Send command
                routerOSWriter.println(command);
                routerOSWriter.flush();
                
                // Give RouterOS time to process command
                Thread.sleep(2500);
                
                // Collect response with timeout-based approach
                StringBuilder response = new StringBuilder();
                String line;
                int emptyReadCount = 0;
                int maxEmptyReads = 20; // 2 seconds of empty reads = done
                boolean foundCommandOutput = false;
                
                while (emptyReadCount < maxEmptyReads) {
                    if (routerOSReader.ready()) {
                        line = routerOSReader.readLine();
                        emptyReadCount = 0; // Reset counter when we get data
                        
                        if (line == null) break;
                        
                        log.trace("RouterOS line: '{}'", line);
                        
                        // Skip command echo
                        if (line.trim().equals(command)) {
                            continue;
                        }
                        
                        // Check for prompt return - command is complete
                        if (line.contains("[admin@") && line.contains("] >")) {
                            log.debug("Found RouterOS prompt return, command complete");
                            break;
                        }
                        
                        // Skip RouterOS help/banner content  
                        if (line.contains("Gives the list of available commands") ||
                            line.contains("Gives help on the command") ||
                            line.contains("MikroTik RouterOS") ||
                            line.contains("MMM") || line.contains("KKK")) {
                            continue;
                        }
                        
                        // Collect ALL output (including empty lines for formatting)
                        response.append(line).append("\n");
                        
                        // Mark that we found some output (even if line is empty)
                        if (!line.trim().isEmpty()) {
                            foundCommandOutput = true;
                        }
                        
                    } else {
                        // No data available, wait a bit
                        Thread.sleep(100);
                        emptyReadCount++;
                    }
                }
                
                String result = response.toString().trim();
                
                if (emptyReadCount >= maxEmptyReads && !foundCommandOutput) {
                    log.warn("RouterOS command '{}' timed out without output", command);
                    return "Command timeout - no output received";
                }
                
                log.debug("RouterOS command '{}' completed: {} characters, {} empty reads", 
                         command, result.length(), emptyReadCount);
                
                return result;
                
            } catch (Exception e) {
                log.error("Error executing RouterOS command '{}': {}", command, e.getMessage());
                throw new RuntimeException("RouterOS command failed: " + e.getMessage());
            }
        }, executorService);
    }
    
    private boolean waitForRouterOSPrompt() {
        try {
            log.debug("Waiting for RouterOS prompt...");
            
            StringBuilder buffer = new StringBuilder();
            String line;
            int timeout = 0;
            int maxTimeout = 200; // 20 seconds
            
            while (timeout < maxTimeout) {
                if (routerOSReader.ready()) {
                    line = routerOSReader.readLine();
                    if (line != null) {
                        buffer.append(line).append("\n");
                        log.trace("RouterOS output: {}", line);
                        
                        // Look for the RouterOS prompt pattern: [admin@RouterName] >
                        if (line.contains("[admin@") && line.contains("] >")) {
                            log.debug("Found RouterOS prompt, ready for commands");
                            return true;
                        }
                    }
                } else {
                    Thread.sleep(100);
                    timeout++;
                }
            }
            
            log.error("Timeout waiting for RouterOS prompt. Buffer content:\n{}", buffer.toString());
            return false;
            
        } catch (Exception e) {
            log.error("Error waiting for RouterOS prompt: {}", e.getMessage());
            return false;
        }
    }
    
    private boolean isRouterOSConnected() {
        return routerOSSession != null && routerOSSession.isConnected() && 
               routerOSShell != null && routerOSShell.isConnected() &&
               status == DeviceStatus.CONNECTED;
    }
    
    @Override
    public boolean isConnected() {
        return isRouterOSConnected();
    }
}