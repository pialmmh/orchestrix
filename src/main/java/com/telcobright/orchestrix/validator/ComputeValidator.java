package com.telcobright.orchestrix.validator;

import com.telcobright.orchestrix.entity.Compute;
import com.telcobright.orchestrix.entity.IPAddress;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class ComputeValidator {
    
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"
    );
    
    private static final Pattern MAC_ADDRESS_PATTERN = Pattern.compile(
        "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$"
    );
    
    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    
    public List<String> validate(Compute compute, boolean isUpdate) {
        List<String> errors = new ArrayList<>();
        
        // Basic required field validation
        if (!isUpdate || compute.getName() != null) {
            if (!StringUtils.hasText(compute.getName())) {
                errors.add("Name is required");
            } else if (compute.getName().length() > 255) {
                errors.add("Name must not exceed 255 characters");
            }
        }
        
        // Hostname validation
        if (!isUpdate || compute.getHostname() != null) {
            if (!StringUtils.hasText(compute.getHostname())) {
                errors.add("Hostname is required");
            } else if (!HOSTNAME_PATTERN.matcher(compute.getHostname()).matches()) {
                errors.add("Invalid hostname format. Must be a valid FQDN or hostname");
            } else if (compute.getHostname().length() > 255) {
                errors.add("Hostname must not exceed 255 characters");
            }
        }
        
        // Status validation
        if (compute.getStatus() != null) {
            List<String> validStatuses = List.of("ACTIVE", "INACTIVE", "MAINTENANCE", "PROVISIONING", 
                "DECOMMISSIONED", "FAILED", "SUSPENDED", "UNKNOWN");
            if (!validStatuses.contains(compute.getStatus())) {
                errors.add("Invalid status. Must be one of: " + String.join(", ", validStatuses));
            }
        }
        
        // Type validation (using isPhysical field instead of type)
        
        // Hardware validation
        if (compute.getCpuCores() != null && compute.getCpuCores() <= 0) {
            errors.add("CPU cores must be greater than 0");
        }
        
        if (compute.getMemoryGb() != null && compute.getMemoryGb() <= 0) {
            errors.add("Memory must be greater than 0 GB");
        }
        
        if (compute.getDiskGb() != null && compute.getDiskGb() <= 0) {
            errors.add("Disk space must be greater than 0 GB");
        }
        
        // MAC Address validation
        if (compute.getPrimaryMacAddress() != null && !compute.getPrimaryMacAddress().isEmpty()) {
            if (!MAC_ADDRESS_PATTERN.matcher(compute.getPrimaryMacAddress()).matches()) {
                errors.add("Invalid MAC address format. Use format: XX:XX:XX:XX:XX:XX");
            }
        }
        
        // IP Address validations (deprecated fields, but still validate if present)
        validateIpAddress(compute.getManagementIp(), "Management IP", errors);
        validateIpAddress(compute.getPublicIp(), "Public IP", errors);
        validateIpAddress(compute.getPrivateIp(), "Private IP", errors);
        validateIpAddress(compute.getIpmiIp(), "IPMI IP", errors);
        
        // Network interface validation
        if (compute.getNetworkInterfacesCount() != null && compute.getNetworkInterfacesCount() < 0) {
            errors.add("Network interfaces count cannot be negative");
        }
        
        if (compute.getNetworkSpeedGbps() != null && compute.getNetworkSpeedGbps() < 0) {
            errors.add("Network speed cannot be negative");
        }
        
        // Storage validation
        if (compute.getTotalStorageGb() != null && compute.getTotalStorageGb() < 0) {
            errors.add("Total storage cannot be negative");
        }
        
        if (compute.getUsedStorageGb() != null) {
            if (compute.getUsedStorageGb() < 0) {
                errors.add("Used storage cannot be negative");
            }
            if (compute.getTotalStorageGb() != null && 
                compute.getUsedStorageGb() > compute.getTotalStorageGb()) {
                errors.add("Used storage cannot exceed total storage");
            }
        }
        
        // Physical location validation
        if (compute.getRackUnit() != null && compute.getRackUnit() <= 0) {
            errors.add("Rack unit must be greater than 0");
        }
        
        if (compute.getPowerConsumptionWatts() != null && compute.getPowerConsumptionWatts() < 0) {
            errors.add("Power consumption cannot be negative");
        }
        
        if (compute.getThermalOutputBtu() != null && compute.getThermalOutputBtu() < 0) {
            errors.add("Thermal output cannot be negative");
        }
        
        // Container support validation
        if (compute.getMaxContainers() != null && compute.getMaxContainers() < 0) {
            errors.add("Maximum containers cannot be negative");
        }
        
        if (compute.getCurrentContainers() != null) {
            if (compute.getCurrentContainers() < 0) {
                errors.add("Current containers cannot be negative");
            }
            if (compute.getMaxContainers() != null && 
                compute.getCurrentContainers() > compute.getMaxContainers()) {
                errors.add("Current containers cannot exceed maximum containers");
            }
        }
        
        // Monitoring thresholds validation (fields don't exist in entity, removed)
        
        // Port validation (fields don't exist in entity, removed)
        
        // IP Address list validation
        if (compute.getIpAddresses() != null && !compute.getIpAddresses().isEmpty()) {
            validateIpAddressList(compute.getIpAddresses(), errors);
        }
        
        return errors;
    }
    
    private void validateIpAddress(String ip, String fieldName, List<String> errors) {
        if (ip != null && !ip.isEmpty()) {
            if (!IP_ADDRESS_PATTERN.matcher(ip).matches()) {
                errors.add(fieldName + " has invalid format. Use format: XXX.XXX.XXX.XXX");
            }
        }
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