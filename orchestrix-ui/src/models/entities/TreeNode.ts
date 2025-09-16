export interface TreeNode {
  id: string;
  name: string;
  type: 'partner' | 'organization' | 'environment' | 'cloud' | 'region' | 'az' | 'datacenter' |
        'pool' | 'resourcepool' | 'compute' | 'container' | 'resource-group' | 'service' | 'network-device';
  data?: any;
  children?: TreeNode[];
  metadata?: {
    tier?: number;
    drPaired?: boolean;
    compliance?: string[];
    capabilities?: string[];
    utilization?: number;
    category?: string;
    icon?: string;
    color?: string;
    resourceGroup?: string;
    hostname?: string;
    ipAddress?: string;
    environment?: string;
    environmentId?: number;
  };
}

export interface Partner {
  id: number;
  name: string;
  displayName: string;
  type: string;
  roles: string[];
  status: string;
  contactEmail?: string;
  contactPhone?: string;
  website?: string;
  apiKey?: string;
  apiSecret?: string;
  billingAccountId?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface Cloud {
  id: number;
  name: string;
  description?: string;
  clientName?: string;
  deploymentRegion?: string;
  status: string;
  partnerId: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface Datacenter {
  id: number;
  name: string;
  countryId?: number;
  stateId?: number;
  cityId?: number;
  locationOther?: string;
  type?: string;
  status?: string;
  provider?: string;
  latitude?: number;
  longitude?: number;
  servers?: number;
  storageTb?: number;
  utilization?: number;
  partnerId?: number;
  isDrSite?: boolean;
  cloudId?: number;
  tier?: number;
  availabilityZoneId?: number;
  drPairedWith?: number;
  environmentId?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface Compute {
  id: number;
  name: string;
  hostname?: string;
  ipAddress?: string;
  nodeType?: string;
  cpuCores?: number;
  memoryGb?: number;
  diskGb?: number;
  status?: string;
  hypervisor?: string;
  isPhysical?: boolean;
  datacenterId?: number;
  cloudId?: number;
  resourcePoolId?: number;
  osVersionId?: number;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface NetworkDevice {
  id: number;
  name: string;
  deviceType: string;
  vendor?: string;
  model?: string;
  managementIp?: string;
  macAddress?: string;
  portCount?: number;
  status?: string;
  description?: string;
  datacenterId?: number;
  resourcePoolId?: number;
  createdAt?: string;
  updatedAt?: string;
}