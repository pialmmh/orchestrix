package com.telcobright.orchestrix.service;

import com.telcobright.orchestrix.automation.example.device.MikroTikRouter;
import com.telcobright.orchestrix.automation.api.device.NetworkingDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class DeviceManager {

    private static final Logger log = LoggerFactory.getLogger(DeviceManager.class);
    
    private final Map<String, NetworkingDevice> devices = new ConcurrentHashMap<>();
    
    public CompletableFuture<String> connectToDevice(String deviceId, String hostname, int port, String username, String password) {
        return connectToDevice(deviceId, hostname, port, username, password, "MIKROTIK");
    }
    
    public CompletableFuture<String> connectToDevice(String deviceId, String hostname, int port, String username, String password, String deviceType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Attempting to connect to {} device: {} at {}:{}", deviceType, deviceId, hostname, port);
                
                NetworkingDevice device = createDevice(deviceId, deviceType);
                
                CompletableFuture<Boolean> connectionResult;
                try {
                    connectionResult = device.connect(hostname, port, username, password);
                } catch (IOException e) {
                    log.error("IOException during connection to device {}: {}", deviceId, e.getMessage());
                    return "IOException during connection to device " + deviceId + ": " + e.getMessage();
                }
                Boolean connected = connectionResult.get(30, TimeUnit.SECONDS);
                
                if (connected) {
                    devices.put(deviceId, device);
                    log.info("Successfully connected and registered device: {}", deviceId);
                    return "Connected to device: " + deviceId;
                } else {
                    log.error("Failed to connect to device: {}", deviceId);
                    return "Failed to connect to device: " + deviceId;
                }
                
            } catch (Exception e) {
                log.error("Error connecting to device {}: {}", deviceId, e.getMessage(), e);
                return "Error connecting to device " + deviceId + ": " + e.getMessage();
            }
        });
    }
    
    private NetworkingDevice createDevice(String deviceId, String deviceType) {
        return switch (deviceType.toUpperCase()) {
            case "MIKROTIK" -> new MikroTikRouter(deviceId);
            default -> new MikroTikRouter(deviceId); // Default to MikroTik for now
        };
    }
    
    public CompletableFuture<List<String>> connectToMultipleDevices(List<DeviceConnectionInfo> deviceInfos) {
        List<CompletableFuture<String>> connectionTasks = deviceInfos.stream()
            .map(info -> connectToDevice(info.getDeviceId(), info.getHostname(), info.getPort(), 
                                       info.getUsername(), info.getPassword(), info.getDeviceType()))
            .collect(Collectors.toList());
        
        return CompletableFuture.allOf(connectionTasks.toArray(new CompletableFuture[0]))
            .thenApply(v -> connectionTasks.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }
    
    public CompletableFuture<String> sendCommand(String deviceId, String command) {
        NetworkingDevice device = devices.get(deviceId);
        if (device == null) {
            return CompletableFuture.completedFuture("Device not found: " + deviceId);
        }
        
        if (!device.isConnected()) {
            return CompletableFuture.completedFuture("Device not connected: " + deviceId);
        }
        
        try {
            return device.sendAndReceive(command)
                .handle((response, throwable) -> {
                    if (throwable != null) {
                        log.error("Error executing command '{}' on device {}: {}", command, deviceId, throwable.getMessage());
                        return "Error executing command: " + throwable.getMessage();
                    }
                    return response;
                });
        } catch (Exception e) {
            return CompletableFuture.completedFuture("Exception: " + e.getMessage());
        }
    }
    
    public CompletableFuture<List<String>> broadcastCommand(String command) {
        List<CompletableFuture<String>> commandTasks = devices.entrySet().stream()
            .filter(entry -> entry.getValue().isConnected())
            .map(entry -> {
                String deviceId = entry.getKey();
                return sendCommand(deviceId, command)
                    .thenApply(response -> deviceId + ": " + response);
            })
            .collect(Collectors.toList());
        
        if (commandTasks.isEmpty()) {
            return CompletableFuture.completedFuture(List.of("No connected devices available"));
        }
        
        return CompletableFuture.allOf(commandTasks.toArray(new CompletableFuture[0]))
            .thenApply(v -> commandTasks.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }
    
    public CompletableFuture<List<String>> sendCommandToSelectedDevices(List<String> deviceIds, String command) {
        List<CompletableFuture<String>> commandTasks = deviceIds.stream()
            .filter(devices::containsKey)
            .filter(deviceId -> devices.get(deviceId).isConnected())
            .map(deviceId -> sendCommand(deviceId, command)
                .thenApply(response -> deviceId + ": " + response))
            .collect(Collectors.toList());
        
        if (commandTasks.isEmpty()) {
            return CompletableFuture.completedFuture(List.of("No connected devices found from selection"));
        }
        
        return CompletableFuture.allOf(commandTasks.toArray(new CompletableFuture[0]))
            .thenApply(v -> commandTasks.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }
    
    public List<DeviceStatus> getDeviceStatuses() {
        return devices.entrySet().stream()
            .map(entry -> DeviceStatus.builder()
                .deviceId(entry.getKey())
                .hostname(entry.getValue().getHostname())
                .deviceType(entry.getValue().getDeviceType().name())
                .vendor(entry.getValue().getVendor())
                .status(entry.getValue().getStatus().name())
                .connected(entry.getValue().isConnected())
                .build())
            .collect(Collectors.toList());
    }
    
    public void disconnectDevice(String deviceId) {
        NetworkingDevice device = devices.get(deviceId);
        if (device != null) {
            try {
                device.disconnect();
                devices.remove(deviceId);
                log.info("Disconnected and removed device: {}", deviceId);
            } catch (IOException e) {
                log.error("Error disconnecting device {}: {}", deviceId, e.getMessage());
            }
        }
    }
    
    public void disconnectAllDevices() {
        List<String> deviceIds = new ArrayList<>(devices.keySet());
        deviceIds.forEach(this::disconnectDevice);
        log.info("Disconnected all devices");
    }
    
    public boolean isDeviceConnected(String deviceId) {
        NetworkingDevice device = devices.get(deviceId);
        return device != null && device.isConnected();
    }
    
    public int getConnectedDeviceCount() {
        return (int) devices.values().stream()
            .filter(NetworkingDevice::isConnected)
            .count();
    }
    
    public NetworkingDevice getDevice(String deviceId) {
        return devices.get(deviceId);
    }
    
    public static class DeviceConnectionInfo {
        private String deviceId;
        private String hostname;
        private int port;
        private String username;
        private String password;
        private String deviceType = "MIKROTIK";
        
        public DeviceConnectionInfo(String deviceId, String hostname, int port, String username, String password) {
            this.deviceId = deviceId;
            this.hostname = hostname;
            this.port = port;
            this.username = username;
            this.password = password;
        }
        
        public DeviceConnectionInfo(String deviceId, String hostname, int port, String username, String password, String deviceType) {
            this.deviceId = deviceId;
            this.hostname = hostname;
            this.port = port;
            this.username = username;
            this.password = password;
            this.deviceType = deviceType;
        }
        
        public String getDeviceId() { return deviceId; }
        public String getHostname() { return hostname; }
        public int getPort() { return port; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getDeviceType() { return deviceType; }
    }
    
    public static class DeviceStatus {
        private String deviceId;
        private String hostname;
        private String deviceType;
        private String vendor;
        private String status;
        private boolean connected;
        
        public static DeviceStatusBuilder builder() {
            return new DeviceStatusBuilder();
        }
        
        public static class DeviceStatusBuilder {
            private String deviceId;
            private String hostname;
            private String deviceType;
            private String vendor;
            private String status;
            private boolean connected;
            
            public DeviceStatusBuilder deviceId(String deviceId) {
                this.deviceId = deviceId;
                return this;
            }
            
            public DeviceStatusBuilder hostname(String hostname) {
                this.hostname = hostname;
                return this;
            }
            
            public DeviceStatusBuilder deviceType(String deviceType) {
                this.deviceType = deviceType;
                return this;
            }
            
            public DeviceStatusBuilder vendor(String vendor) {
                this.vendor = vendor;
                return this;
            }
            
            public DeviceStatusBuilder status(String status) {
                this.status = status;
                return this;
            }
            
            public DeviceStatusBuilder connected(boolean connected) {
                this.connected = connected;
                return this;
            }
            
            public DeviceStatus build() {
                DeviceStatus deviceStatus = new DeviceStatus();
                deviceStatus.deviceId = this.deviceId;
                deviceStatus.hostname = this.hostname;
                deviceStatus.deviceType = this.deviceType;
                deviceStatus.vendor = this.vendor;
                deviceStatus.status = this.status;
                deviceStatus.connected = this.connected;
                return deviceStatus;
            }
        }
        
        public String getDeviceId() { return deviceId; }
        public String getHostname() { return hostname; }
        public String getDeviceType() { return deviceType; }
        public String getVendor() { return vendor; }
        public String getStatus() { return status; }
        public boolean isConnected() { return connected; }
    }
}