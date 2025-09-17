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
            // Define all entities with singular table names
            .entity("partner", "partner", "p",
                List.of("id", "name", "display_name", "contact_phone", "contact_email", "website", "type", "roles", "status", "api_key", "api_secret", "billing_account_id", "created_at", "updated_at"))

            .entity("environment", "environment", "env",
                List.of("id", "name", "code", "description", "type", "status", "partner_id", "created_at", "updated_at"))

            .entity("cloud", "cloud", "c",
                List.of("id", "name", "partner_id", "client_name", "deployment_region", "description", "status", "created_at", "updated_at"))

            .entity("region", "region", "r",
                List.of("id", "name", "code", "description", "geographic_area", "compliance_zones", "status", "cloud_id", "created_at", "updated_at"))

            .entity("availabilityzone", "availability_zone", "az",
                List.of("id", "name", "code", "zone_type", "capabilities", "is_default", "status", "region_id", "created_at", "updated_at"))

            .entity("datacenter", "datacenter", "dc",
                List.of("id", "name", "cloud_id", "availability_zone_id", "environment_id", "country_id", "state_id", "city_id", "location_other", "type", "status", "provider", "latitude", "longitude", "servers", "storage_tb", "utilization", "partner_id", "is_dr_site", "tier", "dr_paired_with", "created_at", "updated_at"))

            .entity("resourcepool", "resource_pool", "rp",
                List.of("id", "name", "description", "type", "hypervisor", "orchestrator", "total_cpu_cores", "total_memory_gb", "total_storage_tb", "used_cpu_cores", "used_memory_gb", "used_storage_tb", "status", "datacenter_id", "created_at", "updated_at"))

            .entity("compute", "compute", "comp",
                List.of("id", "name", "datacenter_id", "cloud_id", "resource_pool_id", "os_version_id", "hostname", "ip_address", "node_type", "cpu_cores", "memory_gb", "disk_gb", "hypervisor", "is_physical", "description", "status", "created_at", "updated_at"))

            .entity("container", "container", "cont",
                List.of("id", "name", "container_id", "container_type", "image", "image_version", "status", "ip_address", "cpu_limit", "memory_limit", "memory_limit_mb", "exposed_ports", "mount_points", "environment_vars", "auto_start", "description", "compute_id", "created_at", "updated_at"))

            .entity("networkdevice", "network_device", "nd",
                List.of("id", "name", "device_type", "vendor", "model", "serial_number", "management_ip", "status", "datacenter_id", "created_at", "updated_at"))

            .entity("resourcegroup", "resource_group", "rg",
                List.of("id", "name", "display_name", "category", "description", "icon", "color", "sort_order", "is_active", "created_at", "updated_at"))

            .entity("environmentassociation", "environment_association", "ea",
                List.of("id", "environment_id", "resource_type", "resource_id", "is_primary", "allocated_percentage", "metadata", "created_at", "updated_at"))

            .entity("computeworkload", "compute_workload", "cw",
                List.of("id", "compute_id", "environment_id", "workload_type", "workload_name", "workload_id",
                       "cpu_cores", "memory_mb", "disk_gb", "ip_address", "ports",
                       "isolation_method", "isolation_details", "status", "created_at", "updated_at"))

            .entity("computecapability", "compute_capability", "cc",
                List.of("compute_id", "capability_type", "capability_version", "configuration", "enabled", "created_at", "updated_at"))

            // Define all relationships for complete hierarchy
            .relationship("partner", "environment", "id", "partner_id")
            .relationship("partner", "cloud", "id", "partner_id")
            .relationship("cloud", "region", "id", "cloud_id")
            .relationship("region", "availabilityzone", "id", "region_id")
            .relationship("availabilityzone", "datacenter", "id", "availability_zone_id")
            .relationship("cloud", "datacenter", "id", "cloud_id")
            .relationship("environment", "datacenter", "id", "environment_id")
            .relationship("datacenter", "resourcepool", "id", "datacenter_id")
            .relationship("resourcepool", "compute", "id", "resource_pool_id")
            .relationship("datacenter", "compute", "id", "datacenter_id")
            .relationship("cloud", "compute", "id", "cloud_id")
            .relationship("compute", "container", "id", "compute_id")
            .relationship("datacenter", "networkdevice", "id", "datacenter_id")

            // New flexible environment associations
            .relationship("environment", "environmentassociation", "id", "environment_id")
            .relationship("compute", "environmentassociation", "id", "resource_id")

            // Workload relationships
            .relationship("compute", "computeworkload", "id", "compute_id")
            .relationship("environment", "computeworkload", "id", "environment_id")

            // Capability relationships
            .relationship("compute", "computecapability", "id", "compute_id")

            .build();
    }
}