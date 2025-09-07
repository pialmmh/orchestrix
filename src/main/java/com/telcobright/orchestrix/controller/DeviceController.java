package com.telcobright.orchestrix.controller;

import com.telcobright.orchestrix.service.DeviceManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DeviceController {
    
    private final DeviceManager deviceManager;
    
    @PostMapping("/connect")
    public CompletableFuture<ResponseEntity<String>> connectDevice(@RequestBody ConnectDeviceRequest request) {
        log.info("Received connection request for device: {}", request.getDeviceId());
        
        return deviceManager.connectToDevice(
            request.getDeviceId(),
            request.getHostname(),
            request.getPort(),
            request.getUsername(),
            request.getPassword()
        ).thenApply(result -> ResponseEntity.ok(result));
    }
    
    @PostMapping("/connect-multiple")
    public CompletableFuture<ResponseEntity<List<String>>> connectMultipleDevices(@RequestBody List<ConnectDeviceRequest> requests) {
        log.info("Received connection request for {} devices", requests.size());
        
        List<DeviceManager.DeviceConnectionInfo> deviceInfos = requests.stream()
            .map(req -> new DeviceManager.DeviceConnectionInfo(
                req.getDeviceId(),
                req.getHostname(),
                req.getPort(),
                req.getUsername(),
                req.getPassword()
            ))
            .toList();
        
        return deviceManager.connectToMultipleDevices(deviceInfos)
            .thenApply(ResponseEntity::ok);
    }
    
    @PostMapping("/command")
    public CompletableFuture<ResponseEntity<String>> sendCommand(@RequestBody SendCommandRequest request) {
        log.info("Sending command to device {}: {}", request.getDeviceId(), request.getCommand());
        
        return deviceManager.sendCommand(request.getDeviceId(), request.getCommand())
            .thenApply(ResponseEntity::ok);
    }
    
    @PostMapping("/broadcast")
    public CompletableFuture<ResponseEntity<List<String>>> broadcastCommand(@RequestBody Map<String, String> request) {
        String command = request.get("command");
        log.info("Broadcasting command to all devices: {}", command);
        
        return deviceManager.broadcastCommand(command)
            .thenApply(ResponseEntity::ok);
    }
    
    @PostMapping("/command-selected")
    public CompletableFuture<ResponseEntity<List<String>>> sendCommandToSelected(@RequestBody SendCommandToSelectedRequest request) {
        log.info("Sending command to {} selected devices: {}", request.getDeviceIds().size(), request.getCommand());
        
        return deviceManager.sendCommandToSelectedDevices(request.getDeviceIds(), request.getCommand())
            .thenApply(ResponseEntity::ok);
    }
    
    @GetMapping("/status")
    public ResponseEntity<List<DeviceManager.DeviceStatus>> getDeviceStatuses() {
        return ResponseEntity.ok(deviceManager.getDeviceStatuses());
    }
    
    @GetMapping("/connected-count")
    public ResponseEntity<Map<String, Integer>> getConnectedDeviceCount() {
        return ResponseEntity.ok(Map.of("connectedDevices", deviceManager.getConnectedDeviceCount()));
    }
    
    @PostMapping("/disconnect/{deviceId}")
    public ResponseEntity<String> disconnectDevice(@PathVariable String deviceId) {
        log.info("Disconnecting device: {}", deviceId);
        deviceManager.disconnectDevice(deviceId);
        return ResponseEntity.ok("Device disconnected: " + deviceId);
    }
    
    @PostMapping("/disconnect-all")
    public ResponseEntity<String> disconnectAllDevices() {
        log.info("Disconnecting all devices");
        deviceManager.disconnectAllDevices();
        return ResponseEntity.ok("All devices disconnected");
    }
    
    @GetMapping("/connected/{deviceId}")
    public ResponseEntity<Map<String, Boolean>> isDeviceConnected(@PathVariable String deviceId) {
        boolean connected = deviceManager.isDeviceConnected(deviceId);
        return ResponseEntity.ok(Map.of("connected", connected));
    }
    
    public static class ConnectDeviceRequest {
        private String deviceId;
        private String hostname;
        private int port = 22;
        private String username;
        private String password;
        
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        
        public String getHostname() { return hostname; }
        public void setHostname(String hostname) { this.hostname = hostname; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    public static class SendCommandRequest {
        private String deviceId;
        private String command;
        
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
    }
    
    public static class SendCommandToSelectedRequest {
        private List<String> deviceIds;
        private String command;
        
        public List<String> getDeviceIds() { return deviceIds; }
        public void setDeviceIds(List<String> deviceIds) { this.deviceIds = deviceIds; }
        
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
    }
}