package com.telcobright.orchestrix.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class MikroTikService {
    
    private final DeviceManager deviceManager;
    
    public CompletableFuture<String> getSystemIdentity(String deviceId) {
        return deviceManager.sendCommand(deviceId, "/system identity print");
    }
    
    public CompletableFuture<String> getInterfaces(String deviceId) {
        return deviceManager.sendCommand(deviceId, "/interface print");
    }
    
    public CompletableFuture<String> getIpAddresses(String deviceId) {
        return deviceManager.sendCommand(deviceId, "/ip address print");
    }
    
    public CompletableFuture<String> getRoutes(String deviceId) {
        return deviceManager.sendCommand(deviceId, "/ip route print");
    }
    
    public CompletableFuture<String> getOspfNeighbors(String deviceId) {
        return deviceManager.sendCommand(deviceId, "/routing ospf neighbor print");
    }
    
    public CompletableFuture<String> getFirewallNatRules(String deviceId) {
        return deviceManager.sendCommand(deviceId, "/ip firewall nat print");
    }
    
    public CompletableFuture<String> getFirewallFilterRules(String deviceId) {
        return deviceManager.sendCommand(deviceId, "/ip firewall filter print");
    }
    
    public CompletableFuture<String> getSystemResources(String deviceId) {
        return deviceManager.sendCommand(deviceId, "/system resource print");
    }
    
    public CompletableFuture<String> setSystemIdentity(String deviceId, String identity) {
        String command = String.format("/system identity set name=%s", identity);
        return deviceManager.sendCommand(deviceId, command);
    }
    
    public CompletableFuture<String> addIpAddress(String deviceId, String address, String interfaceName) {
        String command = String.format("/ip address add address=%s interface=%s", address, interfaceName);
        return deviceManager.sendCommand(deviceId, command);
    }
    
    public CompletableFuture<String> addStaticRoute(String deviceId, String destination, String gateway) {
        String command = String.format("/ip route add dst-address=%s gateway=%s", destination, gateway);
        return deviceManager.sendCommand(deviceId, command);
    }
    
    public CompletableFuture<String> addNatRule(String deviceId, String chain, String srcAddress, String action, String toAddresses) {
        String command = String.format("/ip firewall nat add chain=%s src-address=%s action=%s to-addresses=%s", 
                                      chain, srcAddress, action, toAddresses);
        return deviceManager.sendCommand(deviceId, command);
    }
    
    public CompletableFuture<String> enableInterface(String deviceId, String interfaceName) {
        String command = String.format("/interface enable %s", interfaceName);
        return deviceManager.sendCommand(deviceId, command);
    }
    
    public CompletableFuture<String> disableInterface(String deviceId, String interfaceName) {
        String command = String.format("/interface disable %s", interfaceName);
        return deviceManager.sendCommand(deviceId, command);
    }
    
    public CompletableFuture<String> configureOspf(String deviceId, String networkAddress, String area) {
        String command = String.format("/routing ospf network add network=%s area=%s", networkAddress, area);
        return deviceManager.sendCommand(deviceId, command);
    }
    
    public CompletableFuture<String> enableOspfRouter(String deviceId, String routerId) {
        String command = String.format("/routing ospf instance set default router-id=%s", routerId);
        return deviceManager.sendCommand(deviceId, command);
    }
    
    public CompletableFuture<List<String>> getSystemInfoFromAllDevices() {
        return deviceManager.broadcastCommand("/system identity print");
    }
    
    public CompletableFuture<List<String>> getInterfacesFromAllDevices() {
        return deviceManager.broadcastCommand("/interface print");
    }
    
    public CompletableFuture<List<String>> getRoutesFromAllDevices() {
        return deviceManager.broadcastCommand("/ip route print");
    }
    
    public CompletableFuture<List<String>> executeCustomCommand(String deviceId, String command) {
        log.info("Executing custom MikroTik command on device {}: {}", deviceId, command);
        return deviceManager.sendCommand(deviceId, command)
            .thenApply(List::of);
    }
    
    public CompletableFuture<List<String>> executeCustomCommandOnMultiple(List<String> deviceIds, String command) {
        log.info("Executing custom MikroTik command on {} devices: {}", deviceIds.size(), command);
        return deviceManager.sendCommandToSelectedDevices(deviceIds, command);
    }
    
    public CompletableFuture<String> backup(String deviceId, String backupName) {
        String command = String.format("/system backup save name=%s", backupName);
        return deviceManager.sendCommand(deviceId, command);
    }
    
    public CompletableFuture<String> reboot(String deviceId) {
        String command = "/system reboot";
        return deviceManager.sendCommand(deviceId, command);
    }
    
    public CompletableFuture<String> getVersion(String deviceId) {
        return deviceManager.sendCommand(deviceId, "/system package print");
    }
}