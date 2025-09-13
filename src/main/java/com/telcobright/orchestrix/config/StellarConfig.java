package com.telcobright.orchestrix.config;

import com.telcobright.stellar.schema.SchemaMetaV2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class StellarConfig {
    
    @Bean
    public SchemaMetaV2 stellarSchema() {
        return SchemaMetaV2.builder()
            // Define entities
            .entity("partner", "partner", "p", 
                List.of("id", "name", "display_name", "phone", "email", "address", "type", "roles", "status", "created_at", "updated_at"))
            .entity("cloud", "cloud", "c", 
                List.of("id", "name", "partner_id", "type", "region", "availability_zones", "endpoint_url", "api_version", "status", "created_at", "updated_at"))
            .entity("datacenter", "datacenter", "dc", 
                List.of("id", "name", "cloud_id", "location", "capacity", "power_capacity", "cooling_type", "tier_level", "status", "created_at", "updated_at"))
            .entity("availabilityzone", "availability_zone", "az",
                List.of("id", "name", "datacenter_id", "zone_code", "subnet_range", "status", "created_at", "updated_at"))
            .entity("rack", "rack", "r", 
                List.of("id", "name", "datacenter_id", "row_number", "position", "height_units", "power_capacity", "status", "created_at", "updated_at"))
            .entity("compute", "compute", "comp", 
                List.of("id", "name", "datacenter_id", "rack_id", "host_type", "hostname", "domain", 
                    "ip_address", "mac_address", "os_type", "os_version", "kernel_version",
                    "cpu_model", "cpu_cores", "cpu_threads", "ram_gb", "disk_gb", "disk_type",
                    "network_interfaces", "gpu_model", "gpu_count", "bios_version", "firmware_version",
                    "serial_number", "asset_tag", "location", "status", "created_at", "updated_at"))
            .entity("networkdevice", "network_device", "nd",
                List.of("id", "name", "datacenter_id", "rack_id", "device_type", "manufacturer", "model",
                    "serial_number", "firmware_version", "management_ip", "snmp_community", "snmp_version",
                    "port_count", "port_speed", "vlan_support", "routing_protocols", "status", "created_at", "updated_at"))
            .entity("container", "container", "cnt",
                List.of("id", "name", "compute_id", "container_id", "image", "command", "ports", 
                    "volumes", "environment", "labels", "network_mode", "restart_policy", "cpu_limit",
                    "memory_limit", "status", "created_at", "updated_at"))
            .entity("virtualnetwork", "virtual_network", "vn",
                List.of("id", "name", "cloud_id", "datacenter_id", "network_type", "cidr", "gateway",
                    "dns_servers", "dhcp_enabled", "vlan_id", "status", "created_at", "updated_at"))
            .entity("storage", "storage", "st",
                List.of("id", "name", "datacenter_id", "storage_type", "protocol", "capacity_gb",
                    "used_gb", "iops", "throughput_mbps", "replication_type", "status", "created_at", "updated_at"))
            .entity("ipaddress", "ip_address", "ip",
                List.of("id", "address", "subnet_mask", "gateway", "dns_primary", "dns_secondary",
                    "datacenter_id", "compute_id", "network_device_id", "allocation_type", "status", 
                    "created_at", "updated_at"))
            
            // Define relationships
            .relationship("partner", "cloud", "id", "partner_id")
            .relationship("cloud", "datacenter", "id", "cloud_id")
            .relationship("datacenter", "availabilityzone", "id", "datacenter_id")
            .relationship("datacenter", "rack", "id", "datacenter_id")
            .relationship("datacenter", "compute", "id", "datacenter_id")
            .relationship("datacenter", "networkdevice", "id", "datacenter_id")
            .relationship("rack", "compute", "id", "rack_id")
            .relationship("rack", "networkdevice", "id", "rack_id")
            .relationship("compute", "container", "id", "compute_id")
            .relationship("cloud", "virtualnetwork", "id", "cloud_id")
            .relationship("datacenter", "virtualnetwork", "id", "datacenter_id")
            .relationship("datacenter", "storage", "id", "datacenter_id")
            .relationship("datacenter", "ipaddress", "id", "datacenter_id")
            .relationship("compute", "ipaddress", "id", "compute_id")
            .relationship("networkdevice", "ipaddress", "id", "network_device_id")
            .build();
    }
}