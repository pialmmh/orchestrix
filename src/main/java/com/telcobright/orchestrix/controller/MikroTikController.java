package com.telcobright.orchestrix.controller;

import com.telcobright.orchestrix.service.MikroTikService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/mikrotik")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MikroTikController {
    
    private final MikroTikService mikrotikService;
    
    @GetMapping("/{deviceId}/identity")
    public CompletableFuture<ResponseEntity<String>> getSystemIdentity(@PathVariable String deviceId) {
        return mikrotikService.getSystemIdentity(deviceId)
            .thenApply(ResponseEntity::ok);
    }
    
    @GetMapping("/{deviceId}/interfaces")
    public CompletableFuture<ResponseEntity<String>> getInterfaces(@PathVariable String deviceId) {
        return mikrotikService.getInterfaces(deviceId)
            .thenApply(ResponseEntity::ok);
    }
    
    @GetMapping("/{deviceId}/ip/addresses")
    public CompletableFuture<ResponseEntity<String>> getIpAddresses(@PathVariable String deviceId) {
        return mikrotikService.getIpAddresses(deviceId)
            .thenApply(ResponseEntity::ok);
    }
    
    @GetMapping("/{deviceId}/ip/routes")
    public CompletableFuture<ResponseEntity<String>> getRoutes(@PathVariable String deviceId) {
        return mikrotikService.getRoutes(deviceId)
            .thenApply(ResponseEntity::ok);
    }
    
    @GetMapping("/{deviceId}/ospf/neighbors")
    public CompletableFuture<ResponseEntity<String>> getOspfNeighbors(@PathVariable String deviceId) {
        return mikrotikService.getOspfNeighbors(deviceId)
            .thenApply(ResponseEntity::ok);
    }
    
    @GetMapping("/{deviceId}/firewall/nat")
    public CompletableFuture<ResponseEntity<String>> getFirewallNatRules(@PathVariable String deviceId) {
        return mikrotikService.getFirewallNatRules(deviceId)
            .thenApply(ResponseEntity::ok);
    }
    
    @GetMapping("/{deviceId}/firewall/filter")
    public CompletableFuture<ResponseEntity<String>> getFirewallFilterRules(@PathVariable String deviceId) {
        return mikrotikService.getFirewallFilterRules(deviceId)
            .thenApply(ResponseEntity::ok);
    }
    
    @GetMapping("/{deviceId}/system/resources")
    public CompletableFuture<ResponseEntity<String>> getSystemResources(@PathVariable String deviceId) {
        return mikrotikService.getSystemResources(deviceId)
            .thenApply(ResponseEntity::ok);
    }
    
    @PostMapping("/{deviceId}/identity")
    public CompletableFuture<ResponseEntity<String>> setSystemIdentity(
            @PathVariable String deviceId, 
            @RequestBody Map<String, String> request) {
        String identity = request.get("identity");
        return mikrotikService.setSystemIdentity(deviceId, identity)
            .thenApply(ResponseEntity::ok);
    }
    
    @PostMapping("/{deviceId}/ip/address")
    public CompletableFuture<ResponseEntity<String>> addIpAddress(
            @PathVariable String deviceId, 
            @RequestBody AddIpAddressRequest request) {
        return mikrotikService.addIpAddress(deviceId, request.getAddress(), request.getInterfaceName())
            .thenApply(ResponseEntity::ok);
    }
    
    @PostMapping("/{deviceId}/ip/route")
    public CompletableFuture<ResponseEntity<String>> addStaticRoute(
            @PathVariable String deviceId, 
            @RequestBody AddRouteRequest request) {
        return mikrotikService.addStaticRoute(deviceId, request.getDestination(), request.getGateway())
            .thenApply(ResponseEntity::ok);
    }
    
    @PostMapping("/{deviceId}/firewall/nat")
    public CompletableFuture<ResponseEntity<String>> addNatRule(
            @PathVariable String deviceId, 
            @RequestBody AddNatRuleRequest request) {
        return mikrotikService.addNatRule(deviceId, request.getChain(), request.getSrcAddress(), 
                                        request.getAction(), request.getToAddresses())
            .thenApply(ResponseEntity::ok);
    }
    
    @PostMapping("/{deviceId}/interface/{interfaceName}/enable")
    public CompletableFuture<ResponseEntity<String>> enableInterface(
            @PathVariable String deviceId, 
            @PathVariable String interfaceName) {
        return mikrotikService.enableInterface(deviceId, interfaceName)
            .thenApply(ResponseEntity::ok);
    }
    
    @PostMapping("/{deviceId}/interface/{interfaceName}/disable")
    public CompletableFuture<ResponseEntity<String>> disableInterface(
            @PathVariable String deviceId, 
            @PathVariable String interfaceName) {
        return mikrotikService.disableInterface(deviceId, interfaceName)
            .thenApply(ResponseEntity::ok);
    }
    
    @PostMapping("/{deviceId}/ospf/network")
    public CompletableFuture<ResponseEntity<String>> configureOspf(
            @PathVariable String deviceId, 
            @RequestBody ConfigureOspfRequest request) {
        return mikrotikService.configureOspf(deviceId, request.getNetworkAddress(), request.getArea())
            .thenApply(ResponseEntity::ok);
    }
    
    @PostMapping("/{deviceId}/ospf/router")
    public CompletableFuture<ResponseEntity<String>> enableOspfRouter(
            @PathVariable String deviceId, 
            @RequestBody Map<String, String> request) {
        String routerId = request.get("routerId");
        return mikrotikService.enableOspfRouter(deviceId, routerId)
            .thenApply(ResponseEntity::ok);
    }
    
    @GetMapping("/all/identity")
    public CompletableFuture<ResponseEntity<List<String>>> getSystemInfoFromAllDevices() {
        return mikrotikService.getSystemInfoFromAllDevices()
            .thenApply(ResponseEntity::ok);
    }
    
    @GetMapping("/all/interfaces")
    public CompletableFuture<ResponseEntity<List<String>>> getInterfacesFromAllDevices() {
        return mikrotikService.getInterfacesFromAllDevices()
            .thenApply(ResponseEntity::ok);
    }
    
    @GetMapping("/all/routes")
    public CompletableFuture<ResponseEntity<List<String>>> getRoutesFromAllDevices() {
        return mikrotikService.getRoutesFromAllDevices()
            .thenApply(ResponseEntity::ok);
    }
    
    @PostMapping("/{deviceId}/command")
    public CompletableFuture<ResponseEntity<List<String>>> executeCustomCommand(
            @PathVariable String deviceId, 
            @RequestBody Map<String, String> request) {
        String command = request.get("command");
        return mikrotikService.executeCustomCommand(deviceId, command)
            .thenApply(ResponseEntity::ok);
    }
    
    @PostMapping("/command/multiple")
    public CompletableFuture<ResponseEntity<List<String>>> executeCustomCommandOnMultiple(
            @RequestBody ExecuteCommandOnMultipleRequest request) {
        return mikrotikService.executeCustomCommandOnMultiple(request.getDeviceIds(), request.getCommand())
            .thenApply(ResponseEntity::ok);
    }
    
    @PostMapping("/{deviceId}/backup")
    public CompletableFuture<ResponseEntity<String>> backup(
            @PathVariable String deviceId, 
            @RequestBody Map<String, String> request) {
        String backupName = request.get("backupName");
        return mikrotikService.backup(deviceId, backupName)
            .thenApply(ResponseEntity::ok);
    }
    
    @PostMapping("/{deviceId}/reboot")
    public CompletableFuture<ResponseEntity<String>> reboot(@PathVariable String deviceId) {
        return mikrotikService.reboot(deviceId)
            .thenApply(ResponseEntity::ok);
    }
    
    @GetMapping("/{deviceId}/version")
    public CompletableFuture<ResponseEntity<String>> getVersion(@PathVariable String deviceId) {
        return mikrotikService.getVersion(deviceId)
            .thenApply(ResponseEntity::ok);
    }
    
    public static class AddIpAddressRequest {
        private String address;
        private String interfaceName;
        
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        
        public String getInterfaceName() { return interfaceName; }
        public void setInterfaceName(String interfaceName) { this.interfaceName = interfaceName; }
    }
    
    public static class AddRouteRequest {
        private String destination;
        private String gateway;
        
        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
        
        public String getGateway() { return gateway; }
        public void setGateway(String gateway) { this.gateway = gateway; }
    }
    
    public static class AddNatRuleRequest {
        private String chain;
        private String srcAddress;
        private String action;
        private String toAddresses;
        
        public String getChain() { return chain; }
        public void setChain(String chain) { this.chain = chain; }
        
        public String getSrcAddress() { return srcAddress; }
        public void setSrcAddress(String srcAddress) { this.srcAddress = srcAddress; }
        
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public String getToAddresses() { return toAddresses; }
        public void setToAddresses(String toAddresses) { this.toAddresses = toAddresses; }
    }
    
    public static class ConfigureOspfRequest {
        private String networkAddress;
        private String area;
        
        public String getNetworkAddress() { return networkAddress; }
        public void setNetworkAddress(String networkAddress) { this.networkAddress = networkAddress; }
        
        public String getArea() { return area; }
        public void setArea(String area) { this.area = area; }
    }
    
    public static class ExecuteCommandOnMultipleRequest {
        private List<String> deviceIds;
        private String command;
        
        public List<String> getDeviceIds() { return deviceIds; }
        public void setDeviceIds(List<String> deviceIds) { this.deviceIds = deviceIds; }
        
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
    }
}