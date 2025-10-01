package com.telcobright.orchestrix.service;

import com.telcobright.orchestrix.entity.*;
import com.telcobright.orchestrix.entity.IPAddress.IPAddressType;
import com.telcobright.orchestrix.entity.IPAddress.AssignmentMethod;
import com.telcobright.orchestrix.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class IPAddressService {

    private static final Logger log = LoggerFactory.getLogger(IPAddressService.class);
    
    @Autowired
    private IPAddressRepository ipAddressRepository;
    
    @Autowired
    private ComputeRepository computeRepository;
    
    @Autowired
    private NetworkDeviceRepository networkDeviceRepository;
    
    @Autowired
    private ContainerRepository containerRepository;
    
    // Create or update IP address
    public IPAddress saveIpAddress(IPAddress ipAddress) {
        // Validate IP address format
        if (!isValidIpAddress(ipAddress.getIpAddress())) {
            throw new IllegalArgumentException("Invalid IP address format: " + ipAddress.getIpAddress());
        }
        
        // Check for duplicates if it's a new IP
        if (ipAddress.getId() == null) {
            Optional<IPAddress> existing = ipAddressRepository.findByIpAddress(ipAddress.getIpAddress());
            if (existing.isPresent()) {
                throw new IllegalArgumentException("IP address already exists: " + ipAddress.getIpAddress());
            }
        }
        
        // Set default values
        if (ipAddress.getAssignmentMethod() == null) {
            ipAddress.setAssignmentMethod(AssignmentMethod.STATIC);
        }
        
        if (ipAddress.getIsActive() == null) {
            ipAddress.setIsActive(true);
        }
        
        if (ipAddress.getIsPrimary() == null) {
            ipAddress.setIsPrimary(false);
        }
        
        return ipAddressRepository.save(ipAddress);
    }
    
    // Get all IP addresses
    public List<IPAddress> getAllIpAddresses() {
        return ipAddressRepository.findAll();
    }
    
    // Get IP address by ID
    public Optional<IPAddress> getIpAddressById(Long id) {
        return ipAddressRepository.findById(id);
    }
    
    // Get IP address by IP string
    public Optional<IPAddress> getIpAddressByIp(String ipAddress) {
        return ipAddressRepository.findByIpAddress(ipAddress);
    }
    
    // Delete IP address
    public void deleteIpAddress(Long id) {
        ipAddressRepository.deleteById(id);
    }
    
    // Assign IP address to a device
    public IPAddress assignIpToDevice(Long ipId, String deviceType, Long deviceId) {
        IPAddress ipAddress = ipAddressRepository.findById(ipId)
            .orElseThrow(() -> new IllegalArgumentException("IP address not found: " + ipId));
        
        // Clear existing assignments
        ipAddress.setCompute(null);
        ipAddress.setNetworkDevice(null);
        ipAddress.setContainer(null);
        
        // Assign to new device
        switch (deviceType.toUpperCase()) {
            case "COMPUTE":
                Compute compute = computeRepository.findById(deviceId)
                    .orElseThrow(() -> new IllegalArgumentException("Compute not found: " + deviceId));
                ipAddress.setCompute(compute);
                break;
            case "NETWORK_DEVICE":
                NetworkDevice networkDevice = networkDeviceRepository.findById(deviceId)
                    .orElseThrow(() -> new IllegalArgumentException("Network device not found: " + deviceId));
                ipAddress.setNetworkDevice(networkDevice);
                break;
            case "CONTAINER":
                Container container = containerRepository.findById(deviceId)
                    .orElseThrow(() -> new IllegalArgumentException("Container not found: " + deviceId));
                ipAddress.setContainer(container);
                break;
            default:
                throw new IllegalArgumentException("Invalid device type: " + deviceType);
        }
        
        ipAddress.setAssignedAt(LocalDateTime.now());
        return ipAddressRepository.save(ipAddress);
    }
    
    // Unassign IP address from device
    public IPAddress unassignIpFromDevice(Long ipId) {
        IPAddress ipAddress = ipAddressRepository.findById(ipId)
            .orElseThrow(() -> new IllegalArgumentException("IP address not found: " + ipId));
        
        ipAddress.setCompute(null);
        ipAddress.setNetworkDevice(null);
        ipAddress.setContainer(null);
        ipAddress.setAssignedAt(null);
        
        return ipAddressRepository.save(ipAddress);
    }
    
    // Get IP addresses for a device
    public List<IPAddress> getIpAddressesForDevice(String deviceType, Long deviceId) {
        switch (deviceType.toUpperCase()) {
            case "COMPUTE":
                return ipAddressRepository.findByComputeId(deviceId);
            case "NETWORK_DEVICE":
                return ipAddressRepository.findByNetworkDeviceId(deviceId);
            case "CONTAINER":
                return ipAddressRepository.findByContainerId(deviceId);
            default:
                throw new IllegalArgumentException("Invalid device type: " + deviceType);
        }
    }
    
    // Set primary IP for a device
    public IPAddress setPrimaryIpForDevice(Long ipId, String deviceType, Long deviceId) {
        // First, unset any existing primary IP for the device
        List<IPAddress> deviceIps = getIpAddressesForDevice(deviceType, deviceId);
        for (IPAddress ip : deviceIps) {
            if (ip.getIsPrimary()) {
                ip.setIsPrimary(false);
                ipAddressRepository.save(ip);
            }
        }
        
        // Set the new primary IP
        IPAddress ipAddress = ipAddressRepository.findById(ipId)
            .orElseThrow(() -> new IllegalArgumentException("IP address not found: " + ipId));
        
        // Verify IP belongs to the device
        boolean belongsToDevice = false;
        switch (deviceType.toUpperCase()) {
            case "COMPUTE":
                belongsToDevice = ipAddress.getCompute() != null && 
                                 ipAddress.getCompute().getId().equals(deviceId);
                break;
            case "NETWORK_DEVICE":
                belongsToDevice = ipAddress.getNetworkDevice() != null && 
                                 ipAddress.getNetworkDevice().getId().equals(deviceId);
                break;
            case "CONTAINER":
                belongsToDevice = ipAddress.getContainer() != null && 
                                 ipAddress.getContainer().getId().equals(deviceId);
                break;
        }
        
        if (!belongsToDevice) {
            throw new IllegalArgumentException("IP address does not belong to the specified device");
        }
        
        ipAddress.setIsPrimary(true);
        return ipAddressRepository.save(ipAddress);
    }
    
    // Add IP type to an IP address
    public IPAddress addIpType(Long ipId, IPAddressType type) {
        IPAddress ipAddress = ipAddressRepository.findById(ipId)
            .orElseThrow(() -> new IllegalArgumentException("IP address not found: " + ipId));
        
        if (ipAddress.getTypes() == null) {
            ipAddress.setTypes(new HashSet<>());
        }
        ipAddress.getTypes().add(type);
        
        return ipAddressRepository.save(ipAddress);
    }
    
    // Remove IP type from an IP address
    public IPAddress removeIpType(Long ipId, IPAddressType type) {
        IPAddress ipAddress = ipAddressRepository.findById(ipId)
            .orElseThrow(() -> new IllegalArgumentException("IP address not found: " + ipId));
        
        if (ipAddress.getTypes() != null) {
            ipAddress.getTypes().remove(type);
        }
        
        return ipAddressRepository.save(ipAddress);
    }
    
    // Set IP types (replace all existing types)
    public IPAddress setIpTypes(Long ipId, Set<IPAddressType> types) {
        IPAddress ipAddress = ipAddressRepository.findById(ipId)
            .orElseThrow(() -> new IllegalArgumentException("IP address not found: " + ipId));
        
        ipAddress.setTypes(types != null ? types : new HashSet<>());
        
        return ipAddressRepository.save(ipAddress);
    }
    
    // Get unassigned IP addresses
    public List<IPAddress> getUnassignedIpAddresses() {
        return ipAddressRepository.findUnassigned();
    }
    
    // Get IP addresses by type
    public List<IPAddress> getIpAddressesByType(IPAddressType type) {
        return ipAddressRepository.findByType(type);
    }
    
    // Get IP addresses by multiple types
    public List<IPAddress> getIpAddressesByTypes(Set<IPAddressType> types) {
        return ipAddressRepository.findByTypes(types);
    }
    
    // Search IP addresses
    public List<IPAddress> searchIpAddresses(String search) {
        return ipAddressRepository.searchIpAddresses(search);
    }
    
    // Check for duplicate IP addresses
    public List<String> findDuplicateIpAddresses() {
        List<Object[]> duplicates = ipAddressRepository.findDuplicateIpAddresses();
        return duplicates.stream()
            .map(obj -> (String) obj[0])
            .collect(Collectors.toList());
    }
    
    // Validate IP address format
    private boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        
        try {
            for (String part : parts) {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    // Batch create IP addresses for a range
    public List<IPAddress> createIpRange(String startIp, String endIp, Integer subnetMask, 
                                         String gateway, Set<IPAddressType> types) {
        List<IPAddress> createdIps = new ArrayList<>();
        
        long start = ipToLong(startIp);
        long end = ipToLong(endIp);
        
        if (start > end) {
            throw new IllegalArgumentException("Start IP must be less than or equal to end IP");
        }
        
        for (long i = start; i <= end; i++) {
            String ip = longToIp(i);
            
            // Skip if IP already exists
            if (ipAddressRepository.existsByIpAddress(ip)) {
                log.warn("IP address already exists, skipping: {}", ip);
                continue;
            }
            
            IPAddress ipAddress = new IPAddress();
            ipAddress.setIpAddress(ip);
            ipAddress.setSubnetMask(subnetMask != null ? subnetMask : 24);
            ipAddress.setGateway(gateway);
            ipAddress.setTypes(types != null ? types : new HashSet<>());
            ipAddress.setIsActive(true);
            ipAddress.setIsPrimary(false);
            ipAddress.setAssignmentMethod(AssignmentMethod.STATIC);
            
            createdIps.add(ipAddressRepository.save(ipAddress));
        }
        
        return createdIps;
    }
    
    // Convert IP address string to long
    private long ipToLong(String ipAddress) {
        String[] parts = ipAddress.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = result << 8;
            result |= Integer.parseInt(parts[i]);
        }
        return result;
    }
    
    // Convert long to IP address string
    private String longToIp(long ip) {
        return ((ip >> 24) & 0xFF) + "." +
               ((ip >> 16) & 0xFF) + "." +
               ((ip >> 8) & 0xFF) + "." +
               (ip & 0xFF);
    }
    
    // Get IP address statistics
    public Map<String, Object> getIpAddressStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("total", ipAddressRepository.count());
        stats.put("assigned", ipAddressRepository.findAssigned().size());
        stats.put("unassigned", ipAddressRepository.findUnassigned().size());
        stats.put("active", ipAddressRepository.findByIsActiveTrue().size());
        stats.put("primary", ipAddressRepository.findByIsPrimaryTrue().size());
        
        // Count by type
        Map<String, Long> typeCount = new HashMap<>();
        for (IPAddressType type : IPAddressType.values()) {
            List<IPAddress> ips = ipAddressRepository.findByType(type);
            if (!ips.isEmpty()) {
                typeCount.put(type.name(), (long) ips.size());
            }
        }
        stats.put("byType", typeCount);
        
        // Count by assignment method
        Map<String, Long> methodCount = new HashMap<>();
        for (AssignmentMethod method : AssignmentMethod.values()) {
            List<IPAddress> ips = ipAddressRepository.findByAssignmentMethod(method);
            if (!ips.isEmpty()) {
                methodCount.put(method.name(), (long) ips.size());
            }
        }
        stats.put("byAssignmentMethod", methodCount);
        
        return stats;
    }
}