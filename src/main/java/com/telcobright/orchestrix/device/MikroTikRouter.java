package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class MikroTikRouter extends Router {
    
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
        log.info("Connecting to MikroTik router at {}:{}", hostname, port);
        return super.connect(hostname, port, username, password);
    }
    
    @Override
    public void disconnect() throws IOException {
        log.info("Disconnecting from MikroTik router {}", deviceId);
        super.disconnect();
    }
}