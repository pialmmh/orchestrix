-- Rename main tables from plural to singular
USE orchestrix;

-- Core infrastructure tables
RENAME TABLE partners TO partner;
RENAME TABLE environments TO environment;
RENAME TABLE clouds TO cloud;
RENAME TABLE regions TO region;
RENAME TABLE availability_zones TO availability_zone;
RENAME TABLE datacenters TO datacenter;
RENAME TABLE computes TO compute;
RENAME TABLE containers TO container;
RENAME TABLE network_devices TO network_device;
RENAME TABLE resource_pools TO resource_pool;
RENAME TABLE resource_groups TO resource_group;

-- Association and capability tables
RENAME TABLE environment_associations TO environment_association;
RENAME TABLE compute_workloads TO compute_workload;
RENAME TABLE compute_capabilities TO compute_capability;
RENAME TABLE compute_resources TO compute_resource;
RENAME TABLE datacenter_resource_groups TO datacenter_resource_group;

-- Location tables
RENAME TABLE countries TO country;
RENAME TABLE states TO state;
RENAME TABLE cities TO city;

-- Network tables
RENAME TABLE bridges TO bridge;
RENAME TABLE bridge_attachments TO bridge_attachment;
RENAME TABLE ip_addresses TO ip_address;
RENAME TABLE ip_address_types TO ip_address_type;

-- Container tables
RENAME TABLE lxc_containers TO lxc_container;
RENAME TABLE lxc_images TO lxc_image;

-- Other tables
RENAME TABLE local_secrets TO local_secret;
RENAME TABLE remote_access_credentials TO remote_access_credential;
RENAME TABLE secret_providers TO secret_provider;
RENAME TABLE servers TO server;
RENAME TABLE os_versions TO os_version;