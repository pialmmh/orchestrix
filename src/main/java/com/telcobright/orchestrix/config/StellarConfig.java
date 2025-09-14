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
            // Define entities (with correct plural table names)
            .entity("partner", "partners", "p",
                List.of("id", "name", "display_name", "contact_phone", "contact_email", "website", "type", "roles", "status", "api_key", "api_secret", "billing_account_id", "created_at", "updated_at"))
            .entity("environment", "environments", "env",
                List.of("id", "name", "code", "description", "type", "status", "partner_id", "created_at", "updated_at"))
            .entity("cloud", "clouds", "c",
                List.of("id", "name", "partner_id", "client_name", "deployment_region", "description", "status", "created_at", "updated_at"))
            .entity("datacenter", "datacenters", "dc",
                List.of("id", "name", "cloud_id", "country_id", "state_id", "city_id", "location_other", "type", "status", "provider", "latitude", "longitude", "servers", "storage_tb", "utilization", "partner_id", "is_dr_site", "tier", "availability_zone_id", "dr_paired_with", "environment_id", "created_at", "updated_at"))
            .entity("compute", "computes", "comp",
                List.of("id", "name", "datacenter_id", "cloud_id", "resource_pool_id", "os_version_id", "hostname",
                    "ip_address", "node_type", "cpu_cores", "memory_gb", "disk_gb",
                    "hypervisor", "is_physical", "description", "status", "created_at", "updated_at"))
            
            // Define relationships
            .relationship("partner", "environment", "id", "partner_id")
            .relationship("partner", "cloud", "id", "partner_id")
            .relationship("cloud", "datacenter", "id", "cloud_id")
            .relationship("datacenter", "compute", "id", "datacenter_id")
            .relationship("cloud", "compute", "id", "cloud_id")
            .build();
    }
}