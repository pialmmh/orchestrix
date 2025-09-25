package com.telcobright.orchestrix.device;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class Router extends NetworkingDevice implements SshCapable {
    
    private final SshDeviceImpl sshImplementation;
    
    public Router() {
        super();
        this.sshImplementation = new SshDeviceImpl();
        this.deviceType = DeviceType.SSH;
        this.vendor = DeviceVendor.GENERIC.getDisplayName();
    }
    
    public Router(String deviceId) {
        super(deviceId);
        this.sshImplementation = new SshDeviceImpl();
        this.deviceType = DeviceType.SSH;
        this.vendor = DeviceVendor.GENERIC.getDisplayName();
    }
    
    @Override
    public CompletableFuture<Boolean> connect(String hostname, int port, String username, String password) throws IOException {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.password = password;
        this.status = DeviceStatus.CONNECTING;
        
        return connectSsh(hostname, port, username, password)
            .thenApply(connected -> {
                if (connected) {
                    this.status = DeviceStatus.CONNECTED;
                    log.info("Router {} connected successfully", deviceId);
                } else {
                    this.status = DeviceStatus.ERROR;
                    log.error("Router {} failed to connect", deviceId);
                }
                return connected;
            });
    }
    
    @Override
    public CompletableFuture<String> send(String command) throws IOException {
        return sendSshCommand(command);
    }
    
    @Override
    public CompletableFuture<String> receive() throws IOException {
        return receiveSshResponse();
    }
    
    @Override
    public CompletableFuture<String> sendAndReceive(String command) throws IOException {
        return executeSshCommand(command);
    }
    
    @Override
    public boolean isConnected() {
        return isSshConnected() && status == DeviceStatus.CONNECTED;
    }
    
    @Override
    public void disconnect() throws IOException {
        disconnectSsh();
        this.status = DeviceStatus.DISCONNECTED;
        log.info("Router {} disconnected", deviceId);
    }
    
    @Override
    public CompletableFuture<String> getSystemInfo() {
        try {
            return sendAndReceive("/system identity print");
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    public CompletableFuture<String> getInterfaces() {
        try {
            return sendAndReceive("/interface print");
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    public CompletableFuture<String> getRoutes() {
        try {
            return sendAndReceive("/ip route print");
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    public CompletableFuture<String> backup(String backupName) {
        try {
            String command = String.format("/system backup save name=%s", backupName);
            return sendAndReceive(command);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    public CompletableFuture<String> reboot() {
        try {
            return sendAndReceive("/system reboot");
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    public CompletableFuture<Boolean> connectSsh(String hostname, int port, String username, String password) throws IOException {
        return sshImplementation.connectSsh(hostname, port, username, password);
    }
    
    @Override
    public CompletableFuture<String> sendSshCommand(String command) throws IOException {
        return sshImplementation.sendSshCommand(command);
    }
    
    @Override
    public CompletableFuture<String> receiveSshResponse() throws IOException {
        return sshImplementation.receiveSshResponse();
    }
    
    @Override
    public boolean isSshConnected() {
        return sshImplementation.isSshConnected();
    }
    
    @Override
    public void disconnectSsh() throws IOException {
        sshImplementation.disconnectSsh();
    }
    
    public CompletableFuture<String> getIpAddresses() {
        try {
            return sendAndReceive("/ip address print");
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    public CompletableFuture<String> getOspfNeighbors() {
        try {
            return sendAndReceive("/routing ospf neighbor print");
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    public CompletableFuture<String> getFirewallNatRules() {
        try {
            return sendAndReceive("/ip firewall nat print");
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    public CompletableFuture<String> getFirewallFilterRules() {
        try {
            return sendAndReceive("/ip firewall filter print");
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    public CompletableFuture<String> setSystemIdentity(String identity) {
        try {
            String command = String.format("/system identity set name=%s", identity);
            return sendAndReceive(command);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    public CompletableFuture<String> addIpAddress(String address, String interfaceName) {
        try {
            String command = String.format("/ip address add address=%s interface=%s", address, interfaceName);
            return sendAndReceive(command);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    public CompletableFuture<String> addStaticRoute(String destination, String gateway) {
        try {
            String command = String.format("/ip route add dst-address=%s gateway=%s", destination, gateway);
            return sendAndReceive(command);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}