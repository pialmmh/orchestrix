package com.telcobright.orchestrix.validator;

import com.telcobright.orchestrix.entity.NetworkDevice;
import com.telcobright.orchestrix.entity.IPAddress;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class NetworkDeviceValidator {
    
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"
    );
    
    private static final Pattern MAC_ADDRESS_PATTERN = Pattern.compile(
        "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$"
    );
    
    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    
    private static final Pattern SERIAL_NUMBER_PATTERN = Pattern.compile(
        "^[A-Za-z0-9-_]+$"
    );
    
    public List<String> validate(NetworkDevice device, boolean isUpdate) {
        List<String> errors = new ArrayList<>();
        
        // Basic required field validation
        if (!isUpdate || device.getName() != null) {
            if (!StringUtils.hasText(device.getName())) {
                errors.add("Name is required");
            } else if (device.getName().length() > 255) {
                errors.add("Name must not exceed 255 characters");
            }
        }
        
        // Device type validation
        if (!isUpdate || device.getDeviceType() != null) {
            if (!StringUtils.hasText(device.getDeviceType())) {
                errors.add("Device type is required");
            } else {
                List<String> validTypes = List.of("ROUTER", "SWITCH", "FIREWALL", "LOAD_BALANCER", 
                    "ACCESS_POINT", "GATEWAY", "MODEM", "HUB", "BRIDGE", "REPEATER", "IDS", "IPS", 
                    "VPN_CONCENTRATOR", "PROXY", "WAN_OPTIMIZER");
                if (!validTypes.contains(device.getDeviceType().toUpperCase())) {
                    errors.add("Invalid device type. Must be one of: " + String.join(", ", validTypes));
                }
            }
        }
        
        // Vendor validation
        if (device.getVendor() != null && device.getVendor().length() > 100) {
            errors.add("Vendor name must not exceed 100 characters");
        }
        
        // Model validation
        if (device.getModel() != null && device.getModel().length() > 100) {
            errors.add("Model must not exceed 100 characters");
        }
        
        // Serial number validation
        if (device.getSerialNumber() != null && !device.getSerialNumber().isEmpty()) {
            if (device.getSerialNumber().length() > 100) {
                errors.add("Serial number must not exceed 100 characters");
            } else if (!SERIAL_NUMBER_PATTERN.matcher(device.getSerialNumber()).matches()) {
                errors.add("Serial number contains invalid characters. Use only alphanumeric, dash, and underscore");
            }
        }
        
        // MAC Address validation
        if (device.getMacAddress() != null && !device.getMacAddress().isEmpty()) {
            if (!MAC_ADDRESS_PATTERN.matcher(device.getMacAddress()).matches()) {
                errors.add("Invalid MAC address format. Use format: XX:XX:XX:XX:XX:XX");
            }
        }
        
        // Port validation
        if (device.getManagementPort() != null && 
            (device.getManagementPort() < 1 || device.getManagementPort() > 65535)) {
            errors.add("Management port must be between 1 and 65535");
        }
        
        // Protocol validation
        if (device.getManagementProtocol() != null) {
            List<String> validProtocols = List.of("SSH", "TELNET", "HTTP", "HTTPS", "SNMP", "API", "CLI");
            if (!validProtocols.contains(device.getManagementProtocol().toUpperCase())) {
                errors.add("Invalid management protocol. Must be one of: " + String.join(", ", validProtocols));
            }
        }
        
        // Firmware version validation (field doesn't exist in entity, removed)
        
        // Port count validation
        if (device.getPortCount() != null && device.getPortCount() < 0) {
            errors.add("Port count cannot be negative");
        }
        
        // Performance metrics validation (these fields don't exist in entity, removed)
        
        // Physical metrics validation
        if (device.getPowerConsumptionWatts() != null && device.getPowerConsumptionWatts() < 0) {
            errors.add("Power consumption cannot be negative");
        }
        
        // Memory and storage validation
        if (device.getTotalMemoryMb() != null && device.getTotalMemoryMb() < 0) {
            errors.add("Memory cannot be negative");
        }
        
        if (device.getAvailableStorageGb() != null && device.getAvailableStorageGb().doubleValue() < 0) {
            errors.add("Storage cannot be negative");
        }
        
        if (device.getCpuUtilizationPercent() != null && 
            (device.getCpuUtilizationPercent().doubleValue() < 0 || device.getCpuUtilizationPercent().doubleValue() > 100)) {
            errors.add("CPU usage must be between 0 and 100");
        }
        
        if (device.getMemoryUtilizationPercent() != null && 
            (device.getMemoryUtilizationPercent().doubleValue() < 0 || device.getMemoryUtilizationPercent().doubleValue() > 100)) {
            errors.add("Memory usage must be between 0 and 100");
        }
        
        // High availability validation (field doesn't exist in entity, removed)
        
        // Status validation
        if (device.getStatus() != null) {
            List<String> validStatuses = List.of("ACTIVE", "INACTIVE", "MAINTENANCE", 
                "FAILED", "STANDBY", "UNKNOWN", "PROVISIONING", "DECOMMISSIONED");
            if (!validStatuses.contains(device.getStatus().toUpperCase())) {
                errors.add("Invalid status. Must be one of: " + String.join(", ", validStatuses));
            }
        }
        
        // VLAN validation
        if (device.getVlanId() != null && device.getVlanId() < 0) {
            errors.add("VLAN ID cannot be negative");
        }
        
        // Deprecated management IP validation (kept for backward compatibility)
        if (device.getManagementIp() != null && !device.getManagementIp().isEmpty()) {
            if (!IP_ADDRESS_PATTERN.matcher(device.getManagementIp()).matches()) {
                errors.add("Invalid management IP format. Use format: XXX.XXX.XXX.XXX");
            }
        }
        
        // IP Address list validation
        if (device.getIpAddresses() != null && !device.getIpAddresses().isEmpty()) {
            validateIpAddressList(device.getIpAddresses(), errors);
        }
        
        // Configuration validation (field doesn't exist in entity, removed)
        
        // Notes validation
        if (device.getNotes() != null && device.getNotes().length() > 1000) {
            errors.add("Notes must not exceed 1000 characters");
        }
        
        return errors;
    }
    
    private void validateIpAddressList(List<IPAddress> ipAddresses, List<String> errors) {
        long primaryCount = ipAddresses.stream()
            .filter(IPAddress::getIsPrimary)
            .count();
            
        if (primaryCount > 1) {
            errors.add("Only one IP address can be marked as primary");
        }
        
        // Check for duplicate IP addresses
        List<String> ips = new ArrayList<>();
        for (IPAddress ipAddr : ipAddresses) {
            if (ipAddr.getIpAddress() != null) {
                if (ips.contains(ipAddr.getIpAddress())) {
                    errors.add("Duplicate IP address found: " + ipAddr.getIpAddress());
                } else {
                    ips.add(ipAddr.getIpAddress());
                    if (!IP_ADDRESS_PATTERN.matcher(ipAddr.getIpAddress()).matches()) {
                        errors.add("Invalid IP address format: " + ipAddr.getIpAddress());
                    }
                }
            }
            
            // Validate subnet mask
            if (ipAddr.getSubnetMask() != null && 
                (ipAddr.getSubnetMask() < 0 || ipAddr.getSubnetMask() > 32)) {
                errors.add("Subnet mask must be between 0 and 32 for IP: " + ipAddr.getIpAddress());
            }
            
            // Validate gateway if present
            if (ipAddr.getGateway() != null && !ipAddr.getGateway().isEmpty()) {
                if (!IP_ADDRESS_PATTERN.matcher(ipAddr.getGateway()).matches()) {
                    errors.add("Invalid gateway format for IP: " + ipAddr.getIpAddress());
                }
            }
            
            // Validate MAC address if present
            if (ipAddr.getMacAddress() != null && !ipAddr.getMacAddress().isEmpty()) {
                if (!MAC_ADDRESS_PATTERN.matcher(ipAddr.getMacAddress()).matches()) {
                    errors.add("Invalid MAC address format for IP: " + ipAddr.getIpAddress());
                }
            }
        }
    }
}