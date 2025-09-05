export interface Cloud {
  id: number;
  name: string;
  description: string;
  clientName: string;
  deploymentRegion: string;
  status: string;
  
  // Enhanced fields for cloud inventory tracking
  cloudProvider: 'AWS' | 'AZURE' | 'GCP' | 'PRIVATE' | 'HYBRID' | 'OTHER';
  cloudAccountId?: string;
  subscriptionId?: string;
  projectId?: string;
  environment: 'PRODUCTION' | 'STAGING' | 'DEVELOPMENT' | 'TEST' | 'DR';
  costCenter?: string;
  businessUnit?: string;
  technicalContact?: string;
  businessContact?: string;
  complianceLevel: 'PUBLIC' | 'INTERNAL' | 'CONFIDENTIAL' | 'RESTRICTED';
  backupStrategy?: string;
  disasterRecoveryRto?: number; // Recovery Time Objective in hours
  disasterRecoveryRpo?: number; // Recovery Point Objective in hours
  monitoringEnabled: boolean;
  loggingEnabled: boolean;
  autoScalingEnabled: boolean;
  maintenanceWindow?: string;
  tags: { [key: string]: string };
  
  datacenters: Datacenter[];
  computes: Compute[];
  storages: Storage[];
  networks: Networking[];
  
  // Provisioning information
  provisionedBy: string;
  provisionedAt: string;
  lastAuditDate?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Datacenter {
  id: number;
  name: string;
  country: Country;
  state: State;
  city: City;
  locationOther?: string;
  type: string;
  status: string;
  provider: string;
  partner: Partner;
  latitude?: number;
  longitude?: number;
  servers: number;
  storageTb: number;
  utilization: number;
  isDrSite: boolean;
  
  // Enhanced datacenter inventory fields
  facilityType: 'COLOCATION' | 'OWNED' | 'LEASED' | 'EDGE' | 'PRIVATE' | 'PUBLIC';
  tier: 'TIER_1' | 'TIER_2' | 'TIER_3' | 'TIER_4';
  complianceCertifications: string[]; // SOC2, ISO27001, PCI-DSS, etc.
  powerCapacityKw: number;
  powerUsageKw: number;
  coolingType: 'AIR' | 'LIQUID' | 'HYBRID';
  networkCarriers: string[];
  internetBandwidthGbps: number;
  redundancyLevel: 'N' | 'N_PLUS_1' | 'N_PLUS_N' | '2N';
  
  // Physical specifications
  floorSpaceSqFt?: number;
  rackCount: number;
  rackUsed: number;
  supportContact?: string;
  emergencyContact?: string;
  accessControlSystem?: string;
  
  // Operational data
  maintenanceContract?: string;
  maintenanceProvider?: string;
  nextMaintenanceDate?: string;
  slaUptime: number; // percentage
  mtbf?: number; // Mean Time Between Failures in hours
  mttr?: number; // Mean Time To Recovery in hours
  
  // Environmental
  temperatureRangeMin: number;
  temperatureRangeMax: number;
  humidityRangeMin: number;
  humidityRangeMax: number;
  
  cloud: Cloud;
  
  // Provisioning information
  provisionedBy: string;
  provisionedAt: string;
  lastInspectionDate?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Compute {
  id: number;
  name: string;
  description: string;
  computeType: ComputeType;
  hostname: string;
  ipAddress: string;
  operatingSystem: string;
  cpuCores: number;
  memoryGb: number;
  diskGb: number;
  status: string;
  
  // Enhanced compute inventory fields
  osVersion: string;
  osDistribution?: string; // Ubuntu, CentOS, RHEL, Windows Server
  architecture: 'x86_64' | 'ARM64' | 'x86' | 'ARM';
  virtualizationPlatform?: 'VMWARE' | 'HYPER_V' | 'KVM' | 'XEN' | 'PROXMOX' | 'BARE_METAL';
  hypervisorVersion?: string;
  
  // Hardware specifications  
  manufacturer?: string; // Dell, HP, Supermicro, etc.
  model?: string;
  serialNumber?: string;
  biosVersion?: string;
  cpuModel: string;
  cpuSpeed: number; // GHz
  cpuSockets: number;
  memoryType?: 'DDR3' | 'DDR4' | 'DDR5';
  memorySpeed?: number; // MHz
  diskType: 'HDD' | 'SSD' | 'NVME' | 'HYBRID';
  diskInterface?: 'SATA' | 'SAS' | 'NVME' | 'IDE';
  raidConfiguration?: string;
  
  // Network configuration
  macAddress: string;
  networkInterfaces: {
    name: string;
    type: 'ETHERNET' | 'WIFI' | 'FIBER' | 'INFINIBAND';
    speed: string; // 1Gbps, 10Gbps, etc.
    ipAddress: string;
    vlanId?: number;
  }[];
  
  // Management
  managementIp?: string;
  iloIp?: string; // iLO, iDRAC, IPMI
  snmpCommunity?: string;
  monitoringAgent?: string;
  backupAgent?: string;
  antivirusAgent?: string;
  
  // Performance and capacity
  cpuUtilization?: number; // percentage
  memoryUtilization?: number; // percentage
  diskUtilization?: number; // percentage
  networkUtilization?: number; // percentage
  uptime?: number; // hours
  loadAverage?: number[];
  
  // Security and compliance
  securityGroups?: string[];
  firewallRules?: string[];
  encryptionEnabled: boolean;
  complianceProfile?: string;
  vulnerabilityScanDate?: string;
  lastPatchDate?: string;
  
  // Licensing
  windowsLicense?: string;
  vmwareLicense?: string;
  otherLicenses?: { [key: string]: string };
  
  cloud: Cloud;
  datacenter: Datacenter;
  containers: Container[];
  
  // Provisioning information
  provisionedBy: string;
  provisionedAt: string;
  imageTemplate?: string;
  deploymentMethod: 'MANUAL' | 'AUTOMATION' | 'TERRAFORM' | 'ANSIBLE' | 'PUPPET' | 'CHEF';
  configurationManagement?: string;
  lastHealthCheck?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Storage {
  id: number;
  name: string;
  description: string;
  storageType: StorageType;
  capacityGb: number;
  usedGb: number;
  availableGb: number;
  protocol: string;
  mountPath: string;
  status: string;
  
  // Enhanced storage inventory fields
  vendor: string; // NetApp, EMC, Pure Storage, etc.
  model: string;
  serialNumber?: string;
  firmwareVersion?: string;
  raidLevel?: 'RAID0' | 'RAID1' | 'RAID5' | 'RAID6' | 'RAID10' | 'RAID50' | 'RAID60';
  
  // Performance characteristics
  iopsRead?: number;
  iopsWrite?: number;
  throughputMbps?: number;
  latencyMs?: number;
  queueDepth?: number;
  
  // Physical specifications
  driveCount?: number;
  driveType: 'HDD' | 'SSD' | 'NVME' | 'HYBRID';
  driveSize?: number; // GB per drive
  driveSpeed?: number; // RPM for HDD
  
  // Networking and connectivity
  networkProtocol: 'ISCSI' | 'FC' | 'NFS' | 'CIFS' | 'S3' | 'FCP' | 'NVME_OVER_FABRIC';
  ipAddresses?: string[];
  fcWwns?: string[]; // Fibre Channel World Wide Names
  
  // Features and capabilities
  deduplicationEnabled?: boolean;
  compressionEnabled?: boolean;
  encryptionEnabled: boolean;
  snapshotEnabled?: boolean;
  replicationEnabled?: boolean;
  tiering?: 'HOT' | 'WARM' | 'COLD' | 'ARCHIVE';
  
  // Backup and DR
  backupSchedule?: string;
  backupRetention?: number; // days
  replicationTarget?: string;
  lastBackupDate?: string;
  lastSnapshotDate?: string;
  
  // Monitoring and alerts
  healthStatus: 'HEALTHY' | 'WARNING' | 'CRITICAL' | 'FAILED';
  temperatureC?: number;
  powerConsumptionW?: number;
  utilizationPercentage: number;
  
  // Maintenance
  warrantyExpiry?: string;
  maintenanceContract?: string;
  lastMaintenanceDate?: string;
  nextMaintenanceDate?: string;
  
  cloud: Cloud;
  datacenter: Datacenter;
  
  // Provisioning information
  provisionedBy: string;
  provisionedAt: string;
  configurationProfile?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Networking {
  id: number;
  name: string;
  description: string;
  networkType: NetworkType;
  networkCidr: string;
  vlanId?: number;
  gateway: string;
  dnsServers: string;
  dhcpEnabled: boolean;
  status: string;
  cloud: Cloud;
  datacenter: Datacenter;
  createdAt: string;
  updatedAt: string;
}

export interface Container {
  id: number;
  name: string;
  description: string;
  containerType: ContainerType;
  containerId: string;
  image: string;
  imageVersion: string;
  status: string;
  ipAddress: string;
  exposedPorts: string;
  environmentVars: string;
  mountPoints: string;
  cpuLimit: string;
  memoryLimit: string;
  autoStart: boolean;
  
  // Enhanced container inventory fields
  imageRegistry?: string;
  imageDigest?: string;
  imageSize?: number; // MB
  imagePullPolicy: 'ALWAYS' | 'IF_NOT_PRESENT' | 'NEVER';
  
  // Runtime configuration
  restartPolicy: 'NO' | 'ON_FAILURE' | 'ALWAYS' | 'UNLESS_STOPPED';
  privileged: boolean;
  readOnly: boolean;
  user?: string;
  workingDirectory?: string;
  command?: string[];
  args?: string[];
  
  // Resource limits and requests
  cpuRequest?: string;
  memoryRequest?: string;
  diskLimit?: string;
  networkLimit?: string;
  
  // Health and monitoring
  healthCheckCommand?: string;
  healthCheckInterval?: number; // seconds
  healthCheckTimeout?: number; // seconds
  healthCheckRetries?: number;
  livenessProbe?: string;
  readinessProbe?: string;
  
  // Logging and monitoring
  logDriver: 'JSON_FILE' | 'SYSLOG' | 'JOURNALD' | 'GELF' | 'FLUENTD' | 'AWSLOGS' | 'SPLUNK';
  logOptions?: { [key: string]: string };
  monitoringEnabled: boolean;
  metricsPort?: number;
  
  // Networking
  networkMode: 'BRIDGE' | 'HOST' | 'NONE' | 'CONTAINER' | 'CUSTOM';
  networks?: string[];
  dnsServers?: string[];
  extraHosts?: string[];
  publishAllPorts?: boolean;
  
  // Security
  capabilities?: string[];
  selinuxOptions?: string;
  appArmorProfile?: string;
  seccompProfile?: string;
  noNewPrivileges?: boolean;
  
  // Labels and annotations
  labels: { [key: string]: string };
  annotations?: { [key: string]: string };
  
  // Orchestration (for K8s containers)
  namespace?: string;
  podName?: string;
  deployment?: string;
  replicaSet?: string;
  service?: string;
  
  // Performance metrics
  cpuUsage?: number; // percentage
  memoryUsage?: number; // MB
  networkRxBytes?: number;
  networkTxBytes?: number;
  diskReadBytes?: number;
  diskWriteBytes?: number;
  
  compute: Compute;
  
  // Provisioning information
  provisionedBy: string;
  provisionedAt: string;
  deploymentTool?: 'DOCKER' | 'KUBERNETES' | 'DOCKER_COMPOSE' | 'PORTAINER' | 'RANCHER' | 'OPENSHIFT';
  configurationFiles?: string[];
  lastRestartedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Country {
  id: number;
  name: string;
  code: string;
}

export interface State {
  id: number;
  name: string;
  country: Country;
}

export interface City {
  id: number;
  name: string;
  state: State;
}

export interface Partner {
  id: number;
  name: string;
  type: string;
  status: string;
}

export enum ComputeType {
  DEDICATED = 'DEDICATED',
  VM = 'VM'
}

export enum StorageType {
  SAN = 'SAN',
  NAS = 'NAS',
  LOCAL = 'LOCAL',
  OBJECT = 'OBJECT'
}

export enum NetworkType {
  VLAN = 'VLAN',
  VXLAN = 'VXLAN',
  BRIDGE = 'BRIDGE',
  OVERLAY = 'OVERLAY'
}

export enum ContainerType {
  LXC = 'LXC',
  LXD = 'LXD',
  DOCKER = 'DOCKER',
  PODMAN = 'PODMAN',
  CONTAINERD = 'CONTAINERD',
  KUBERNETES = 'KUBERNETES'
}

export interface TreeNode {
  id: string;
  name: string;
  type: 'category' | 'cloud' | 'datacenter' | 'compute' | 'storage' | 'networking' | 'container' | 'container-group';
  entityId?: number | string;
  icon?: React.ReactNode;
  children?: TreeNode[];
  expanded?: boolean;
  data?: any;
}