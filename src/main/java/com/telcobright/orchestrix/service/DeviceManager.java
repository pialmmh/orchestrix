package com.telcobright.orchestrix.service;

import com.telcobright.orchestrix.device.SshDevice;
import com.telcobright.orchestrix.device.TerminalDevice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeviceManager {
    
    private final Map<String, TerminalDevice> devices = new ConcurrentHashMap<>();
    
    public CompletableFuture<String> connectToDevice(String deviceId, String hostname, int port, String username, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Attempting to connect to device: {} at {}:{}", deviceId, hostname, port);
                
                SshDevice device = new SshDevice(deviceId);
                
                CompletableFuture<Boolean> connectionResult = device.connect(hostname, port, username, password);
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
    
    public CompletableFuture<List<String>> connectToMultipleDevices(List<DeviceConnectionInfo> deviceInfos) {
        List<CompletableFuture<String>> connectionTasks = deviceInfos.stream()
            .map(info -> connectToDevice(info.getDeviceId(), info.getHostname(), info.getPort(), info.getUsername(), info.getPassword()))
            .collect(Collectors.toList());
        
        return CompletableFuture.allOf(connectionTasks.toArray(new CompletableFuture[0]))
            .thenApply(v -> connectionTasks.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }
    
    public CompletableFuture<String> sendCommand(String deviceId, String command) {
        TerminalDevice device = devices.get(deviceId);
        if (device == null) {
            return CompletableFuture.completedFuture("Device not found: " + deviceId);
        }
        
        if (!device.isConnected()) {
            return CompletableFuture.completedFuture("Device not connected: " + deviceId);
        }
        
        return device.sendAndReceive(command)
            .handle((response, throwable) -> {
                if (throwable != null) {
                    log.error("Error executing command '{}' on device {}: {}", command, deviceId, throwable.getMessage());
                    return "Error executing command: " + throwable.getMessage();
                }
                return response;
            });
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
                .status(entry.getValue().getStatus().name())
                .connected(entry.getValue().isConnected())
                .build())
            .collect(Collectors.toList());
    }
    
    public void disconnectDevice(String deviceId) {
        TerminalDevice device = devices.get(deviceId);
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
        TerminalDevice device = devices.get(deviceId);
        return device != null && device.isConnected();
    }
    
    public int getConnectedDeviceCount() {
        return (int) devices.values().stream()
            .filter(TerminalDevice::isConnected)
            .count();
    }
    
    public static class DeviceConnectionInfo {
        private String deviceId;
        private String hostname;
        private int port;
        private String username;
        private String password;
        
        public DeviceConnectionInfo(String deviceId, String hostname, int port, String username, String password) {
            this.deviceId = deviceId;
            this.hostname = hostname;
            this.port = port;
            this.username = username;
            this.password = password;
        }
        
        public String getDeviceId() { return deviceId; }
        public String getHostname() { return hostname; }
        public int getPort() { return port; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
    }
    
    public static class DeviceStatus {
        private String deviceId;
        private String hostname;
        private String deviceType;
        private String status;
        private boolean connected;
        
        public static DeviceStatusBuilder builder() {
            return new DeviceStatusBuilder();
        }
        
        public static class DeviceStatusBuilder {
            private String deviceId;
            private String hostname;
            private String deviceType;
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
                deviceStatus.status = this.status;
                deviceStatus.connected = this.connected;
                return deviceStatus;
            }
        }
        
        public String getDeviceId() { return deviceId; }
        public String getHostname() { return hostname; }
        public String getDeviceType() { return deviceType; }
        public String getStatus() { return status; }
        public boolean isConnected() { return connected; }
    }
}