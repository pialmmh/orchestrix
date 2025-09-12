package com.orchestrix.stellar.schema;

import com.orchestrix.stellar.model.Kind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrchestrixSchema {
    
    private static final Map<Kind, TableMeta> tableMetaMap = new HashMap<>();
    private static final List<JoinEdge> joinEdges = new ArrayList<>();
    
    static {
        // Define table metadata for each entity
        
        // Partners/Organizations
        tableMetaMap.put(Kind.PARTNER, new TableMeta(
            "partners", "p",
            Arrays.asList("id", "name", "display_name", "type", "roles", "status", 
                         "contact_email", "contact_phone", "website", "api_key", "api_secret",
                         "billing_account_id", "created_at", "updated_at"),
            "id"
        ));
        
        // Clouds
        tableMetaMap.put(Kind.CLOUD, new TableMeta(
            "clouds", "c",
            Arrays.asList("id", "name", "description", "client_name", "deployment_region", 
                         "status", "partner_id", "created_at", "updated_at"),
            "id"
        ));
        
        // Regions
        tableMetaMap.put(Kind.REGION, new TableMeta(
            "regions", "r",
            Arrays.asList("id", "name", "cloud_id", "code", "location", "status", "created_at", "updated_at"),
            "id"
        ));
        
        // Availability Zones
        tableMetaMap.put(Kind.AVAILABILITYZONE, new TableMeta(
            "availability_zones", "az",
            Arrays.asList("id", "name", "region_id", "zone_code", "status", "created_at", "updated_at"),
            "id"
        ));
        
        // Datacenters
        tableMetaMap.put(Kind.DATACENTER, new TableMeta(
            "datacenters", "dc",
            Arrays.asList("id", "name", "country_id", "state_id", "city_id", "location_other",
                        "type", "status", "provider", "latitude", "longitude", "servers", 
                        "storage_tb", "utilization", "partner_id", "is_dr_site", "cloud_id", 
                        "tier", "availability_zone_id", "dr_paired_with", "environment_id",
                        "created_at", "updated_at"),
            "id"
        ));
        
        // Resource Pools
        tableMetaMap.put(Kind.RESOURCEPOOL, new TableMeta(
            "resource_pools", "rp",
            Arrays.asList("id", "name", "datacenter_id", "type", "capacity", "used", "status", "created_at", "updated_at"),
            "id"
        ));
        
        // Computes
        tableMetaMap.put(Kind.COMPUTE, new TableMeta(
            "computes", "comp",
            Arrays.asList("id", "name", "hostname", "ip_address", "datacenter_id", "cloud_id",
                        "resource_pool_id", "os_version_id", "cpu_cores", "memory_gb", "disk_gb", 
                        "status", "hypervisor", "is_physical", "node_type", "description",
                        "created_at", "updated_at"),
            "id"
        ));
        
        // Containers
        tableMetaMap.put(Kind.CONTAINER, new TableMeta(
            "containers", "cont",
            Arrays.asList("id", "name", "compute_id", "image", "status", "cpu_limit", "memory_limit_mb",
                        "port_mappings", "environment_vars", "created_at", "updated_at"),
            "id"
        ));
        
        // Network Devices
        tableMetaMap.put(Kind.NETWORKDEVICE, new TableMeta(
            "network_devices", "nd",
            Arrays.asList("id", "name", "datacenter_id", "resource_pool_id", "device_type", 
                        "vendor", "model", "management_ip", "mac_address", "port_count", 
                        "status", "description", "created_at", "updated_at"),
            "id"
        ));
        
        // OS Versions
        tableMetaMap.put(Kind.OSVERSION, new TableMeta(
            "os_versions", "os",
            Arrays.asList("id", "name", "version", "architecture", "type", "created_at", "updated_at"),
            "id"
        ));
        
        // IP Addresses
        tableMetaMap.put(Kind.IPADDRESS, new TableMeta(
            "ip_addresses", "ip",
            Arrays.asList("id", "address", "compute_id", "network_device_id", "type", "is_primary",
                        "subnet", "gateway", "dns_servers", "created_at", "updated_at"),
            "id"
        ));
        
        // Define relationships (join edges)
        
        // Cloud relationships
        joinEdges.add(new JoinEdge(Kind.PARTNER, Kind.CLOUD, "id", "partner_id"));
        joinEdges.add(new JoinEdge(Kind.CLOUD, Kind.REGION, "id", "cloud_id"));
        joinEdges.add(new JoinEdge(Kind.CLOUD, Kind.DATACENTER, "id", "cloud_id"));
        
        // Region relationships
        joinEdges.add(new JoinEdge(Kind.REGION, Kind.AVAILABILITYZONE, "id", "region_id"));
        
        // Availability Zone relationships  
        joinEdges.add(new JoinEdge(Kind.AVAILABILITYZONE, Kind.DATACENTER, "id", "availability_zone_id"));
        
        // Datacenter relationships
        joinEdges.add(new JoinEdge(Kind.DATACENTER, Kind.RESOURCEPOOL, "id", "datacenter_id"));
        joinEdges.add(new JoinEdge(Kind.DATACENTER, Kind.COMPUTE, "id", "datacenter_id"));
        joinEdges.add(new JoinEdge(Kind.DATACENTER, Kind.NETWORKDEVICE, "id", "datacenter_id"));
        
        // Resource Pool relationships
        joinEdges.add(new JoinEdge(Kind.RESOURCEPOOL, Kind.COMPUTE, "id", "resource_pool_id"));
        
        // Compute relationships
        joinEdges.add(new JoinEdge(Kind.COMPUTE, Kind.CONTAINER, "id", "compute_id"));
        joinEdges.add(new JoinEdge(Kind.COMPUTE, Kind.IPADDRESS, "id", "compute_id"));
        joinEdges.add(new JoinEdge(Kind.OSVERSION, Kind.COMPUTE, "id", "os_version_id"));
        
        // Network Device relationships
        joinEdges.add(new JoinEdge(Kind.NETWORKDEVICE, Kind.IPADDRESS, "id", "network_device_id"));
    }
    
    public static SchemaMeta getSchema() {
        return new SchemaMeta(tableMetaMap, joinEdges);
    }
    
    /**
     * Get hierarchy for infrastructure tree
     * partner -> cloud -> region -> az -> datacenter -> (resourcepool | compute | networkdevice)
     */
    public static List<Kind> getInfrastructureHierarchy() {
        List<Kind> hierarchy = new ArrayList<>();
        hierarchy.add(Kind.PARTNER);
        hierarchy.add(Kind.CLOUD);
        hierarchy.add(Kind.DATACENTER);
        hierarchy.add(Kind.COMPUTE);
        return hierarchy;
    }
    
    /**
     * Check if a kind supports children
     */
    public static boolean supportsChildren(String kindName) {
        switch (kindName) {
            case "partner":
            case "cloud":
            case "region":
            case "availabilityzone":
            case "datacenter":
            case "resourcepool":
            case "compute":
                return true;
            default:
                return false;
        }
    }
}