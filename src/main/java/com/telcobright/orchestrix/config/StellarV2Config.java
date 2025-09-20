package com.telcobright.orchestrix.config;

import com.telcobright.stellar.schema.SchemaMetaV2;
import com.telcobright.stellar.mutation.MutationExecutor;
import com.telcobright.stellar.sql.MysqlQueryBuilderV2;
import com.telcobright.stellar.result.ResultTransformerV2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class StellarV2Config {

    @Bean
    public SchemaMetaV2 schemaV2() {
        return SchemaMetaV2.builder()
            // Infrastructure entities
            .entity("partner", "partner", "p",
                List.of("id", "name", "display_name", "type", "roles", "status", "created_at", "updated_at"))
            .entity("cloud", "cloud", "cl",
                List.of("id", "partner_id", "name", "type", "provider", "region", "status", "created_at", "updated_at"))
            .entity("region", "region", "r",
                List.of("id", "cloud_id", "name", "code", "location", "status", "created_at", "updated_at"))
            .entity("availabilityzone", "availabilityzone", "az",
                List.of("id", "region_id", "name", "code", "status", "created_at", "updated_at"))
            .entity("datacenter", "datacenter", "dc",
                List.of("id", "availabilityzone_id", "name", "location", "tier", "status", "created_at", "updated_at"))
            .entity("compute", "compute", "c",
                List.of("id", "datacenter_id", "name", "type", "cpu", "memory", "storage", "status", "ip_address",
                       "os", "created_at", "updated_at"))
            .entity("networkdevice", "networkdevice", "nd",
                List.of("id", "datacenter_id", "name", "type", "vendor", "model", "management_ip", "status",
                       "created_at", "updated_at"))
            .entity("environment", "environment", "env",
                List.of("id", "partner_id", "name", "type", "description", "status", "created_at", "updated_at"))
            .entity("environmentassociation", "environmentassociation", "ea",
                List.of("id", "environment_id", "resource_type", "resource_id", "created_at"))
            .entity("resourcepool", "resourcepool", "rp",
                List.of("id", "datacenter_id", "name", "type", "capacity", "allocated", "status", "created_at", "updated_at"))
            .entity("resourcegroup", "resourcegroup", "rg",
                List.of("id", "name", "type", "description", "status", "created_at", "updated_at"))
            .entity("container", "container", "cn",
                List.of("id", "compute_id", "name", "image", "status", "ports", "created_at", "updated_at"))
            .entity("computecapability", "computecapability", "cc",
                List.of("id", "compute_id", "capability", "value", "created_at", "updated_at"))
            .entity("computeworkload", "computeworkload", "cw",
                List.of("id", "compute_id", "workload_type", "workload_name", "status", "created_at", "updated_at"))

            // Define relationships
            .relationship("partner", "cloud", "id", "partner_id")
            .relationship("partner", "environment", "id", "partner_id")
            .relationship("cloud", "region", "id", "cloud_id")
            .relationship("region", "availabilityzone", "id", "region_id")
            .relationship("availabilityzone", "datacenter", "id", "availabilityzone_id")
            .relationship("datacenter", "compute", "id", "datacenter_id")
            .relationship("datacenter", "networkdevice", "id", "datacenter_id")
            .relationship("datacenter", "resourcepool", "id", "datacenter_id")
            .relationship("compute", "container", "id", "compute_id")
            .relationship("compute", "computecapability", "id", "compute_id")
            .relationship("compute", "computeworkload", "id", "compute_id")
            .relationship("environment", "environmentassociation", "id", "environment_id")

            .build();
    }

    @Bean
    public MutationExecutor mutationExecutor(SchemaMetaV2 schemaV2) {
        return new MutationExecutor(schemaV2);
    }

    @Bean
    public MysqlQueryBuilderV2 queryBuilderV2(SchemaMetaV2 schemaV2) {
        return new MysqlQueryBuilderV2(schemaV2);
    }

    @Bean
    public ResultTransformerV2 resultTransformerV2(SchemaMetaV2 schemaV2) {
        return new ResultTransformerV2(schemaV2);
    }
}