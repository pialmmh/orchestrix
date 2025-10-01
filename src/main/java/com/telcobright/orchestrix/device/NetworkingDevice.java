package com.telcobright.orchestrix.device;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
@Setter
public abstract class NetworkingDevice {

    private static final Logger log = LoggerFactory.getLogger(NetworkingDevice.class);
    
    protected String deviceId;
    protected String hostname;
    protected int port;
    protected String username;
    protected String password;
    protected DeviceStatus status = DeviceStatus.DISCONNECTED;
    protected DeviceType deviceType;
    protected String vendor;
    protected String model;
    protected String osVersion;
    
    public NetworkingDevice() {
        this.deviceId = UUID.randomUUID().toString();
    }
    
    public NetworkingDevice(String deviceId) {
        this.deviceId = deviceId != null ? deviceId : UUID.randomUUID().toString();
    }
    
    public abstract CompletableFuture<Boolean> connect(String hostname, int port, String username, String password) throws IOException;
    
    public abstract CompletableFuture<String> send(String command) throws IOException;
    
    public abstract CompletableFuture<String> receive() throws IOException;
    
    public abstract CompletableFuture<String> sendAndReceive(String command) throws IOException;
    
    public abstract boolean isConnected();
    
    public abstract void disconnect() throws IOException;
    
    public abstract CompletableFuture<String> getSystemInfo();
    
    public abstract CompletableFuture<String> getInterfaces();
    
    public abstract CompletableFuture<String> getRoutes();
    
    public abstract CompletableFuture<String> backup(String backupName);
    
    public abstract CompletableFuture<String> reboot();
    
    public enum DeviceType {
        SSH, TELNET, SERIAL
    }
    
    public enum DeviceStatus {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR, TIMEOUT
    }
    
    public enum DeviceVendor {
        MIKROTIK("MikroTik"),
        CISCO("Cisco"),
        JUNIPER("Juniper"),
        HUAWEI("Huawei"),
        UBIQUITI("Ubiquiti"),
        GENERIC("Generic");
        
        private final String displayName;
        
        DeviceVendor(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}