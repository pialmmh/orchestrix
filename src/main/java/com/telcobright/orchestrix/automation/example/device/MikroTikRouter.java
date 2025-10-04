package com.telcobright.orchestrix.automation.example.device;

import com.telcobright.orchestrix.automation.core.device.UniversalSshDevice;
import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class MikroTikRouter extends UniversalSshDevice {

    private static final Logger log = LoggerFactory.getLogger(MikroTikRouter.class);
    
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
    
    public CompletableFuture<String> getIpAddresses() {
        try {
            return sendSshCommand("/ip address print");
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    // MikroTik RouterOS uses universal SSH implementation from UniversalSshDevice
    
    // Implementation of remaining abstract methods from NetworkingDevice
    @Override
    public CompletableFuture<String> send(String command) throws IOException {
        return sendSshCommand(command);
    }
    
    @Override
    public CompletableFuture<String> receive() throws IOException {
        return receiveSshResponse();
    }
    
    @Override
    public CompletableFuture<String> getInterfaces() {
        try {
            return sendSshCommand("/interface print");
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    public CompletableFuture<String> getRoutes() {
        try {
            return sendSshCommand("/ip route print");
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    public CompletableFuture<String> backup(String backupName) {
        try {
            String command = String.format("/system backup save name=%s", backupName);
            return sendSshCommand(command);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    public CompletableFuture<String> reboot() {
        try {
            return sendSshCommand("/system reboot");
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}