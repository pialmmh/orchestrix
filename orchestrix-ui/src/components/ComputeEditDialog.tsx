import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  Tabs,
  Tab,
  Box,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  FormControlLabel,
  Checkbox,
} from '@mui/material';

interface ComputeEditDialogProps {
  open: boolean;
  onClose: () => void;
  formData: any;
  setFormData: (data: any) => void;
  onSave: (updatedData: any) => void;
  editMode: boolean;
}

const ComputeEditDialog: React.FC<ComputeEditDialogProps> = ({
  open,
  onClose,
  formData,
  setFormData,
  onSave,
  editMode,
}) => {
  const [activeTab, setActiveTab] = useState(0);

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setActiveTab(newValue);
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>{editMode ? 'Edit' : 'Add'} Compute Node</DialogTitle>
      <DialogContent>
        <Box sx={{ borderBottom: 1, borderColor: 'divider', maxWidth: 800, mx: 'auto' }}>
          <Tabs value={activeTab} onChange={handleTabChange} variant="scrollable">
            <Tab label="Basic Info" />
            <Tab label="OS & Software" />
            <Tab label="Hardware" />
            <Tab label="Network" />
            <Tab label="Storage" />
            <Tab label="Performance" />
            <Tab label="Compliance & Security" />
            <Tab label="Maintenance" />
            <Tab label="Monitoring" />
            <Tab label="Roles & Metadata" />
          </Tabs>
        </Box>
        
        <Box sx={{ px: 4, py: 2, minHeight: 350, maxHeight: '60vh', overflowY: 'auto' }}>
          {/* Basic Info Tab */}
          {activeTab === 0 && (
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2, mt: 2, px: 3, maxWidth: 800, mx: 'auto' }}>
              <TextField
                label="Name"
                value={formData.name || ''}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                fullWidth
                required
                size="small"
              />
              <TextField
                label="Hostname"
                value={formData.hostname || ''}
                onChange={(e) => setFormData({ ...formData, hostname: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="IP Address"
                value={formData.ipAddress || ''}
                onChange={(e) => setFormData({ ...formData, ipAddress: e.target.value })}
                fullWidth
                size="small"
              />
              <FormControl fullWidth size="small">
                <InputLabel>Node Type</InputLabel>
                <Select
                  value={formData.nodeType || ''}
                  onChange={(e) => setFormData({ ...formData, nodeType: e.target.value })}
                  label="Node Type"
                >
                  <MenuItem value="dedicated_server">Dedicated Server</MenuItem>
                  <MenuItem value="vm">Virtual Machine</MenuItem>
                </Select>
              </FormControl>
              <FormControl fullWidth size="small">
                <InputLabel>Status</InputLabel>
                <Select
                  value={formData.status || ''}
                  onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                  label="Status"
                >
                  <MenuItem value="ACTIVE">Active</MenuItem>
                  <MenuItem value="INACTIVE">Inactive</MenuItem>
                  <MenuItem value="MAINTENANCE">Maintenance</MenuItem>
                </Select>
              </FormControl>
              <TextField
                label="CPU Cores"
                type="number"
                value={formData.cpuCores || ''}
                onChange={(e) => setFormData({ ...formData, cpuCores: parseInt(e.target.value) || null })}
                fullWidth
                size="small"
              />
              <TextField
                label="Memory (GB)"
                type="number"
                value={formData.memoryGb || ''}
                onChange={(e) => setFormData({ ...formData, memoryGb: parseInt(e.target.value) || null })}
                fullWidth
                size="small"
              />
              <TextField
                label="Disk (GB)"
                type="number"
                value={formData.diskGb || ''}
                onChange={(e) => setFormData({ ...formData, diskGb: parseInt(e.target.value) || null })}
                fullWidth
                size="small"
              />
              <Box sx={{ gridColumn: 'span 2' }}>
                <TextField
                  label="Description"
                  value={formData.description || ''}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  fullWidth
                  multiline
                  rows={3}
                  size="small"
                />
              </Box>
            </Box>
          )}
          
          {/* OS & Software Tab */}
          {activeTab === 1 && (
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2, mt: 2, px: 3, maxWidth: 800, mx: 'auto' }}>
              <FormControl fullWidth size="small">
                <InputLabel>OS Type</InputLabel>
                <Select
                  value={formData.osType || ''}
                  onChange={(e) => setFormData({ ...formData, osType: e.target.value })}
                  label="OS Type"
                >
                  <MenuItem value="LINUX">Linux</MenuItem>
                  <MenuItem value="WINDOWS">Windows</MenuItem>
                  <MenuItem value="UNIX">Unix</MenuItem>
                  <MenuItem value="MACOS">macOS</MenuItem>
                </Select>
              </FormControl>
              <TextField
                label="OS Distribution"
                value={formData.osDistribution || ''}
                onChange={(e) => setFormData({ ...formData, osDistribution: e.target.value })}
                fullWidth
                size="small"
                placeholder="Ubuntu, RHEL, CentOS, Windows Server, etc."
              />
              <TextField
                label="OS Version"
                value={formData.osVersionString || ''}
                onChange={(e) => setFormData({ ...formData, osVersionString: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="Kernel Version"
                value={formData.kernelVersion || ''}
                onChange={(e) => setFormData({ ...formData, kernelVersion: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="Firmware Version"
                value={formData.firmwareVersion || ''}
                onChange={(e) => setFormData({ ...formData, firmwareVersion: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="BIOS Version"
                value={formData.biosVersion || ''}
                onChange={(e) => setFormData({ ...formData, biosVersion: e.target.value })}
                fullWidth
                size="small"
              />
              <FormControl fullWidth size="small">
                <InputLabel>Hypervisor</InputLabel>
                <Select
                  value={formData.hypervisor || ''}
                  onChange={(e) => setFormData({ ...formData, hypervisor: e.target.value })}
                  label="Hypervisor"
                >
                  <MenuItem value="VMWARE">VMware</MenuItem>
                  <MenuItem value="KVM">KVM</MenuItem>
                  <MenuItem value="HYPERV">Hyper-V</MenuItem>
                  <MenuItem value="XEN">Xen</MenuItem>
                  <MenuItem value="">None</MenuItem>
                </Select>
              </FormControl>
              <FormControl fullWidth size="small">
                <InputLabel>Virtualization Type</InputLabel>
                <Select
                  value={formData.virtualizationType || ''}
                  onChange={(e) => setFormData({ ...formData, virtualizationType: e.target.value })}
                  label="Virtualization Type"
                >
                  <MenuItem value="KVM">KVM</MenuItem>
                  <MenuItem value="VMWARE">VMware</MenuItem>
                  <MenuItem value="HYPERV">Hyper-V</MenuItem>
                  <MenuItem value="XEN">Xen</MenuItem>
                  <MenuItem value="DOCKER">Docker</MenuItem>
                  <MenuItem value="LXC">LXC</MenuItem>
                  <MenuItem value="LXD">LXD</MenuItem>
                  <MenuItem value="NONE">None</MenuItem>
                </Select>
              </FormControl>
            </Box>
          )}

          {/* Hardware Tab */}
          {activeTab === 2 && (
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2, mt: 2, px: 3, maxWidth: 800, mx: 'auto' }}>
              <TextField
                label="Brand"
                value={formData.brand || ''}
                onChange={(e) => setFormData({ ...formData, brand: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="Model"
                value={formData.model || ''}
                onChange={(e) => setFormData({ ...formData, model: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="Serial Number"
                value={formData.serialNumber || ''}
                onChange={(e) => setFormData({ ...formData, serialNumber: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="Asset Tag"
                value={formData.assetTag || ''}
                onChange={(e) => setFormData({ ...formData, assetTag: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="Rack Location"
                value={formData.rackLocation || ''}
                onChange={(e) => setFormData({ ...formData, rackLocation: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="Rack Unit"
                type="number"
                value={formData.rackUnit || ''}
                onChange={(e) => setFormData({ ...formData, rackUnit: parseInt(e.target.value) || null })}
                fullWidth
                size="small"
              />
              <TextField
                label="CPU Model"
                value={formData.cpuModel || ''}
                onChange={(e) => setFormData({ ...formData, cpuModel: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="CPU Cores"
                type="number"
                value={formData.cpuCores || ''}
                onChange={(e) => setFormData({ ...formData, cpuCores: parseInt(e.target.value) || null })}
                fullWidth
                size="small"
              />
              <TextField
                label="CPU Speed (GHz)"
                type="number"
                value={formData.cpuSpeedGhz || ''}
                onChange={(e) => setFormData({ ...formData, cpuSpeedGhz: parseFloat(e.target.value) || null })}
                fullWidth
                size="small"
                inputProps={{ step: 0.1 }}
              />
              <TextField
                label="RAM (GB)"
                type="number"
                value={formData.ramGb || ''}
                onChange={(e) => setFormData({ ...formData, ramGb: parseInt(e.target.value) || null })}
                fullWidth
                size="small"
              />
              <TextField
                label="Storage (GB)"
                type="number"
                value={formData.storageGb || ''}
                onChange={(e) => setFormData({ ...formData, storageGb: parseInt(e.target.value) || null })}
                fullWidth
                size="small"
              />
              <FormControl fullWidth size="small">
                <InputLabel>Storage Type</InputLabel>
                <Select
                  value={formData.storageType || ''}
                  onChange={(e) => setFormData({ ...formData, storageType: e.target.value })}
                  label="Storage Type"
                >
                  <MenuItem value="SSD">SSD</MenuItem>
                  <MenuItem value="HDD">HDD</MenuItem>
                  <MenuItem value="NVME">NVMe</MenuItem>
                  <MenuItem value="SAN">SAN</MenuItem>
                  <MenuItem value="NAS">NAS</MenuItem>
                </Select>
              </FormControl>
              <TextField
                label="Power Consumption (Watts)"
                type="number"
                value={formData.powerConsumptionWatts || ''}
                onChange={(e) => setFormData({ ...formData, powerConsumptionWatts: parseInt(e.target.value) || null })}
                fullWidth
                size="small"
              />
              <TextField
                label="Thermal Output (BTU)"
                type="number"
                value={formData.thermalOutputBtu || ''}
                onChange={(e) => setFormData({ ...formData, thermalOutputBtu: parseInt(e.target.value) || null })}
                fullWidth
                size="small"
              />
              <FormControlLabel
                control={
                  <Checkbox
                    checked={formData.isPhysical || false}
                    onChange={(e) => setFormData({ ...formData, isPhysical: e.target.checked })}
                  />
                }
                label="Is Physical Server"
                sx={{ gridColumn: 'span 2' }}
              />
            </Box>
          )}

          {/* Network Tab */}
          {activeTab === 3 && (
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2, mt: 2, px: 3, maxWidth: 800, mx: 'auto' }}>
              <TextField
                label="Primary MAC Address"
                value={formData.primaryMacAddress || ''}
                onChange={(e) => setFormData({ ...formData, primaryMacAddress: e.target.value })}
                fullWidth
                size="small"
                placeholder="aa:bb:cc:dd:ee:ff"
              />
              <TextField
                label="Management IP"
                value={formData.managementIp || ''}
                onChange={(e) => setFormData({ ...formData, managementIp: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="Public IP"
                value={formData.publicIp || ''}
                onChange={(e) => setFormData({ ...formData, publicIp: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="Private IP"
                value={formData.privateIp || ''}
                onChange={(e) => setFormData({ ...formData, privateIp: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="IPMI IP"
                value={formData.ipmiIp || ''}
                onChange={(e) => setFormData({ ...formData, ipmiIp: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="VLAN IDs"
                value={formData.vlanIds || ''}
                onChange={(e) => setFormData({ ...formData, vlanIds: e.target.value })}
                fullWidth
                size="small"
                placeholder="JSON array: [100, 200, 300]"
              />
              <TextField
                label="Network Interfaces Count"
                type="number"
                value={formData.networkInterfacesCount || ''}
                onChange={(e) => setFormData({ ...formData, networkInterfacesCount: parseInt(e.target.value) || 1 })}
                fullWidth
                size="small"
              />
              <TextField
                label="Network Speed (Gbps)"
                type="number"
                value={formData.networkSpeedGbps || ''}
                onChange={(e) => setFormData({ ...formData, networkSpeedGbps: parseInt(e.target.value) || null })}
                fullWidth
                size="small"
              />
            </Box>
          )}

          {/* Storage Tab */}
          {activeTab === 4 && (
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2, mt: 2, px: 3, maxWidth: 800, mx: 'auto' }}>
              <FormControl fullWidth size="small">
                <InputLabel>Storage Type</InputLabel>
                <Select
                  value={formData.storageType || ''}
                  onChange={(e) => setFormData({ ...formData, storageType: e.target.value })}
                  label="Storage Type"
                >
                  <MenuItem value="SSD">SSD</MenuItem>
                  <MenuItem value="HDD">HDD</MenuItem>
                  <MenuItem value="NVME">NVMe</MenuItem>
                  <MenuItem value="HYBRID">Hybrid</MenuItem>
                  <MenuItem value="SAN">SAN</MenuItem>
                  <MenuItem value="NAS">NAS</MenuItem>
                  <MenuItem value="DAS">DAS</MenuItem>
                </Select>
              </FormControl>
              <TextField
                label="Storage RAID Level"
                value={formData.storageRaidLevel || ''}
                onChange={(e) => setFormData({ ...formData, storageRaidLevel: e.target.value })}
                fullWidth
                size="small"
                placeholder="RAID0, RAID1, RAID5, RAID10, etc."
              />
              <TextField
                label="Total Storage (GB)"
                type="number"
                value={formData.totalStorageGb || ''}
                onChange={(e) => setFormData({ ...formData, totalStorageGb: parseInt(e.target.value) || null })}
                fullWidth
                size="small"
              />
              <TextField
                label="Used Storage (GB)"
                type="number"
                value={formData.usedStorageGb || ''}
                onChange={(e) => setFormData({ ...formData, usedStorageGb: parseInt(e.target.value) || 0 })}
                fullWidth
                size="small"
              />
              <TextField
                label="Storage IOPS"
                type="number"
                value={formData.storageIops || ''}
                onChange={(e) => setFormData({ ...formData, storageIops: parseInt(e.target.value) || null })}
                fullWidth
                size="small"
              />
            </Box>
          )}

          {/* Performance Tab */}
          {activeTab === 5 && (
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2, mt: 2 }}>
              <TextField
                label="CPU Benchmark Score"
                type="number"
                value={formData.cpuBenchmarkScore || ''}
                onChange={(e) => setFormData({ ...formData, cpuBenchmarkScore: parseInt(e.target.value) || null })}
                fullWidth
                size="small"
              />
              <TextField
                label="Memory Bandwidth (Gbps)"
                type="number"
                inputProps={{ step: "0.01" }}
                value={formData.memoryBandwidthGbps || ''}
                onChange={(e) => setFormData({ ...formData, memoryBandwidthGbps: parseFloat(e.target.value) || null })}
                fullWidth
                size="small"
              />
              <TextField
                label="Network Latency (ms)"
                type="number"
                inputProps={{ step: "0.01" }}
                value={formData.networkLatencyMs || ''}
                onChange={(e) => setFormData({ ...formData, networkLatencyMs: parseFloat(e.target.value) || null })}
                fullWidth
                size="small"
              />
              <TextField
                label="Uptime (days)"
                type="number"
                value={formData.uptimeDays || ''}
                onChange={(e) => setFormData({ ...formData, uptimeDays: parseInt(e.target.value) || 0 })}
                fullWidth
                size="small"
              />
              <TextField
                label="Last Reboot Date"
                type="datetime-local"
                value={formData.lastRebootDate || ''}
                onChange={(e) => setFormData({ ...formData, lastRebootDate: e.target.value })}
                fullWidth
                size="small"
                InputLabelProps={{ shrink: true }}
              />
            </Box>
          )}

          {/* Compliance & Security Tab */}
          {activeTab === 6 && (
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2, mt: 2 }}>
              <FormControl fullWidth size="small">
                <InputLabel>Compliance Status</InputLabel>
                <Select
                  value={formData.complianceStatus || ''}
                  onChange={(e) => setFormData({ ...formData, complianceStatus: e.target.value })}
                  label="Compliance Status"
                >
                  <MenuItem value="COMPLIANT">Compliant</MenuItem>
                  <MenuItem value="NON_COMPLIANT">Non-Compliant</MenuItem>
                  <MenuItem value="PENDING_REVIEW">Pending Review</MenuItem>
                  <MenuItem value="EXEMPT">Exempt</MenuItem>
                </Select>
              </FormControl>
              <FormControl fullWidth size="small">
                <InputLabel>Security Zone</InputLabel>
                <Select
                  value={formData.securityZone || ''}
                  onChange={(e) => setFormData({ ...formData, securityZone: e.target.value })}
                  label="Security Zone"
                >
                  <MenuItem value="DMZ">DMZ</MenuItem>
                  <MenuItem value="INTERNAL">Internal</MenuItem>
                  <MenuItem value="EXTERNAL">External</MenuItem>
                  <MenuItem value="RESTRICTED">Restricted</MenuItem>
                  <MenuItem value="PUBLIC">Public</MenuItem>
                  <MenuItem value="PRIVATE">Private</MenuItem>
                </Select>
              </FormControl>
              <FormControlLabel
                control={
                  <Checkbox
                    checked={formData.encryptionEnabled || false}
                    onChange={(e) => setFormData({ ...formData, encryptionEnabled: e.target.checked })}
                  />
                }
                label="Encryption Enabled"
              />
              <TextField
                label="Last Security Scan Date"
                type="date"
                value={formData.lastSecurityScanDate || ''}
                onChange={(e) => setFormData({ ...formData, lastSecurityScanDate: e.target.value })}
                fullWidth
                size="small"
                InputLabelProps={{ shrink: true }}
              />
              <TextField
                label="Patch Level"
                value={formData.patchLevel || ''}
                onChange={(e) => setFormData({ ...formData, patchLevel: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="Antivirus Status"
                value={formData.antivirusStatus || ''}
                onChange={(e) => setFormData({ ...formData, antivirusStatus: e.target.value })}
                fullWidth
                size="small"
              />
            </Box>
          )}

          {/* Maintenance Tab */}
          {activeTab === 7 && (
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2, mt: 2 }}>
              <TextField
                label="Warranty Expiry Date"
                type="date"
                value={formData.warrantyExpiryDate || ''}
                onChange={(e) => setFormData({ ...formData, warrantyExpiryDate: e.target.value })}
                fullWidth
                size="small"
                InputLabelProps={{ shrink: true }}
              />
              <TextField
                label="Support Contract ID"
                value={formData.supportContractId || ''}
                onChange={(e) => setFormData({ ...formData, supportContractId: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="Maintenance Window"
                value={formData.maintenanceWindow || ''}
                onChange={(e) => setFormData({ ...formData, maintenanceWindow: e.target.value })}
                fullWidth
                size="small"
                placeholder="e.g., Sunday 02:00-04:00"
              />
              <TextField
                label="Last Maintenance Date"
                type="date"
                value={formData.lastMaintenanceDate || ''}
                onChange={(e) => setFormData({ ...formData, lastMaintenanceDate: e.target.value })}
                fullWidth
                size="small"
                InputLabelProps={{ shrink: true }}
              />
              <TextField
                label="Next Maintenance Date"
                type="date"
                value={formData.nextMaintenanceDate || ''}
                onChange={(e) => setFormData({ ...formData, nextMaintenanceDate: e.target.value })}
                fullWidth
                size="small"
                InputLabelProps={{ shrink: true }}
              />
            </Box>
          )}

          {/* Monitoring Tab */}
          {activeTab === 8 && (
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2, mt: 2 }}>
              <FormControlLabel
                control={
                  <Checkbox
                    checked={formData.monitoringEnabled !== false}
                    onChange={(e) => setFormData({ ...formData, monitoringEnabled: e.target.checked })}
                  />
                }
                label="Monitoring Enabled"
              />
              <TextField
                label="Monitoring Agent"
                value={formData.monitoringAgent || ''}
                onChange={(e) => setFormData({ ...formData, monitoringAgent: e.target.value })}
                fullWidth
                size="small"
                placeholder="Prometheus, Nagios, Zabbix, etc."
              />
              <TextField
                label="Management Tool"
                value={formData.managementTool || ''}
                onChange={(e) => setFormData({ ...formData, managementTool: e.target.value })}
                fullWidth
                size="small"
                placeholder="Ansible, Puppet, Chef, etc."
              />
              <FormControlLabel
                control={
                  <Checkbox
                    checked={formData.backupEnabled || false}
                    onChange={(e) => setFormData({ ...formData, backupEnabled: e.target.checked })}
                  />
                }
                label="Backup Enabled"
              />
              <TextField
                label="Backup Schedule"
                value={formData.backupSchedule || ''}
                onChange={(e) => setFormData({ ...formData, backupSchedule: e.target.value })}
                fullWidth
                size="small"
                placeholder="0 2 * * * (daily at 2 AM)"
              />
              <TextField
                label="Last Backup Date"
                type="datetime-local"
                value={formData.lastBackupDate || ''}
                onChange={(e) => setFormData({ ...formData, lastBackupDate: e.target.value })}
                fullWidth
                size="small"
                InputLabelProps={{ shrink: true }}
              />
            </Box>
          )}

          {/* Roles & Metadata Tab */}
          {activeTab === 9 && (
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2, mt: 2 }}>
              <FormControl fullWidth size="small">
                <InputLabel>Compute Role</InputLabel>
                <Select
                  value={formData.computeRole || ''}
                  onChange={(e) => setFormData({ ...formData, computeRole: e.target.value })}
                  label="Compute Role"
                >
                  <MenuItem value="DATABASE">Database</MenuItem>
                  <MenuItem value="APPLICATION">Application</MenuItem>
                  <MenuItem value="WEB">Web Server</MenuItem>
                  <MenuItem value="CACHE">Cache</MenuItem>
                  <MenuItem value="QUEUE">Queue/Message Broker</MenuItem>
                  <MenuItem value="LOAD_BALANCER">Load Balancer</MenuItem>
                  <MenuItem value="PROXY">Proxy</MenuItem>
                  <MenuItem value="STORAGE">Storage</MenuItem>
                  <MenuItem value="COMPUTE">Compute</MenuItem>
                  <MenuItem value="MONITORING">Monitoring</MenuItem>
                  <MenuItem value="BACKUP">Backup</MenuItem>
                  <MenuItem value="MANAGEMENT">Management</MenuItem>
                  <MenuItem value="OTHER">Other</MenuItem>
                </Select>
              </FormControl>
              <FormControl fullWidth size="small">
                <InputLabel>Environment Type</InputLabel>
                <Select
                  value={formData.environmentType || ''}
                  onChange={(e) => setFormData({ ...formData, environmentType: e.target.value })}
                  label="Environment Type"
                >
                  <MenuItem value="PRODUCTION">Production</MenuItem>
                  <MenuItem value="STAGING">Staging</MenuItem>
                  <MenuItem value="DEVELOPMENT">Development</MenuItem>
                  <MenuItem value="TEST">Test</MenuItem>
                  <MenuItem value="QA">QA</MenuItem>
                  <MenuItem value="UAT">UAT</MenuItem>
                  <MenuItem value="DR">Disaster Recovery</MenuItem>
                </Select>
              </FormControl>
              <FormControl fullWidth size="small">
                <InputLabel>Service Tier</InputLabel>
                <Select
                  value={formData.serviceTier || ''}
                  onChange={(e) => setFormData({ ...formData, serviceTier: e.target.value })}
                  label="Service Tier"
                >
                  <MenuItem value="CRITICAL">Critical</MenuItem>
                  <MenuItem value="HIGH">High</MenuItem>
                  <MenuItem value="MEDIUM">Medium</MenuItem>
                  <MenuItem value="LOW">Low</MenuItem>
                </Select>
              </FormControl>
              <TextField
                label="Business Unit"
                value={formData.businessUnit || ''}
                onChange={(e) => setFormData({ ...formData, businessUnit: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="Cost Center"
                value={formData.costCenter || ''}
                onChange={(e) => setFormData({ ...formData, costCenter: e.target.value })}
                fullWidth
                size="small"
              />
              <FormControlLabel
                control={
                  <Checkbox
                    checked={formData.supportsContainers || false}
                    onChange={(e) => setFormData({ ...formData, supportsContainers: e.target.checked })}
                  />
                }
                label="Supports Containers"
              />
              <FormControl fullWidth size="small">
                <InputLabel>Container Runtime</InputLabel>
                <Select
                  value={formData.containerRuntime || ''}
                  onChange={(e) => setFormData({ ...formData, containerRuntime: e.target.value })}
                  label="Container Runtime"
                >
                  <MenuItem value="DOCKER">Docker</MenuItem>
                  <MenuItem value="CONTAINERD">containerd</MenuItem>
                  <MenuItem value="CRIO">CRI-O</MenuItem>
                  <MenuItem value="PODMAN">Podman</MenuItem>
                  <MenuItem value="LXC">LXC</MenuItem>
                  <MenuItem value="NONE">None</MenuItem>
                </Select>
              </FormControl>
              <FormControl fullWidth size="small">
                <InputLabel>Orchestration Platform</InputLabel>
                <Select
                  value={formData.orchestrationPlatform || ''}
                  onChange={(e) => setFormData({ ...formData, orchestrationPlatform: e.target.value })}
                  label="Orchestration Platform"
                >
                  <MenuItem value="KUBERNETES">Kubernetes</MenuItem>
                  <MenuItem value="OPENSHIFT">OpenShift</MenuItem>
                  <MenuItem value="SWARM">Docker Swarm</MenuItem>
                  <MenuItem value="NOMAD">Nomad</MenuItem>
                  <MenuItem value="RANCHER">Rancher</MenuItem>
                  <MenuItem value="NONE">None</MenuItem>
                </Select>
              </FormControl>
              <Box sx={{ gridColumn: 'span 2' }}>
                <TextField
                  label="Purpose"
                  value={formData.purpose || ''}
                  onChange={(e) => setFormData({ ...formData, purpose: e.target.value })}
                  fullWidth
                  multiline
                  rows={2}
                  size="small"
                  placeholder="Describe the purpose and role of this compute node..."
                />
              </Box>
              <Box sx={{ gridColumn: 'span 2' }}>
                <TextField
                  label="Tags"
                  value={formData.tags || ''}
                  onChange={(e) => setFormData({ ...formData, tags: e.target.value })}
                  fullWidth
                  size="small"
                  placeholder="JSON array: ['web-server', 'production', 'critical']"
                />
              </Box>
              <Box sx={{ gridColumn: 'span 2' }}>
                <TextField
                  label="Notes"
                  value={formData.notes || ''}
                  onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                  fullWidth
                  multiline
                  rows={3}
                  size="small"
                  placeholder="Additional notes and comments..."
                />
              </Box>
              <Box sx={{ gridColumn: 'span 2' }}>
                <TextField
                  label="Custom Attributes"
                  value={formData.customAttributes || ''}
                  onChange={(e) => setFormData({ ...formData, customAttributes: e.target.value })}
                  fullWidth
                  multiline
                  rows={2}
                  size="small"
                  placeholder="JSON object: {'department': 'IT', 'project': 'WebApp'}"
                />
              </Box>
            </Box>
          )}
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button onClick={() => onSave(formData)} variant="contained">
          {editMode ? 'Update' : 'Create'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default ComputeEditDialog;