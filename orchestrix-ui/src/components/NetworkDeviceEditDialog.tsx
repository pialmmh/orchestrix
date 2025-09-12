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
import RemoteAccessTab from './RemoteAccessTab';
import IPAddressManager from './IPAddressManager';

interface NetworkDeviceEditDialogProps {
  open: boolean;
  onClose: () => void;
  formData: any;
  setFormData: (data: any) => void;
  onSave: (updatedData: any) => void;
  editMode: boolean;
}

const NetworkDeviceEditDialog: React.FC<NetworkDeviceEditDialogProps> = ({
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
      <DialogTitle>{editMode ? 'Edit' : 'Add'} Network Device</DialogTitle>
      <DialogContent>
        <Box sx={{ borderBottom: 1, borderColor: 'divider', maxWidth: 800, mx: 'auto' }}>
          <Tabs value={activeTab} onChange={handleTabChange} variant="scrollable">
            <Tab label="Basic Info" />
            <Tab label="Network Config" />
            <Tab label="Physical Location" />
            <Tab label="Performance" />
            <Tab label="Management" />
            <Tab label="Business" />
            <Tab label="Monitoring" />
            <Tab label="Metadata" />
            <Tab label="Remote Access" />
          </Tabs>
        </Box>
        
        <Box sx={{ px: 4, py: 2, minHeight: 350, maxHeight: '60vh', overflowY: 'auto' }}>
          {/* Basic Info Tab */}
          {activeTab === 0 && (
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2, mt: 2 }}>
              <TextField
                label="Name"
                value={formData.name || ''}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                fullWidth
                required
                size="small"
              />
              <TextField
                label="Display Name"
                value={formData.displayName || ''}
                onChange={(e) => setFormData({ ...formData, displayName: e.target.value })}
                fullWidth
                size="small"
              />
              <FormControl fullWidth size="small">
                <InputLabel>Device Type</InputLabel>
                <Select
                  value={formData.deviceType || ''}
                  onChange={(e) => setFormData({ ...formData, deviceType: e.target.value })}
                  label="Device Type"
                  required
                >
                  <MenuItem value="Router">Router</MenuItem>
                  <MenuItem value="Switch">Switch</MenuItem>
                  <MenuItem value="Firewall">Firewall</MenuItem>
                  <MenuItem value="Load Balancer">Load Balancer</MenuItem>
                  <MenuItem value="VPN Gateway">VPN Gateway</MenuItem>
                  <MenuItem value="WiFi Access Point">WiFi Access Point</MenuItem>
                  <MenuItem value="Network Storage">Network Storage</MenuItem>
                  <MenuItem value="Network Security Appliance">Network Security Appliance</MenuItem>
                </Select>
              </FormControl>
              <TextField
                label="Vendor"
                value={formData.vendor || ''}
                onChange={(e) => setFormData({ ...formData, vendor: e.target.value })}
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
                label="Version"
                value={formData.version || ''}
                onChange={(e) => setFormData({ ...formData, version: e.target.value })}
                fullWidth
                size="small"
              />
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
                  <MenuItem value="ERROR">Error</MenuItem>
                </Select>
              </FormControl>
              <FormControl fullWidth size="small">
                <InputLabel>Operational Status</InputLabel>
                <Select
                  value={formData.operationalStatus || ''}
                  onChange={(e) => setFormData({ ...formData, operationalStatus: e.target.value })}
                  label="Operational Status"
                >
                  <MenuItem value="UP">Up</MenuItem>
                  <MenuItem value="DOWN">Down</MenuItem>
                  <MenuItem value="DEGRADED">Degraded</MenuItem>
                  <MenuItem value="UNKNOWN">Unknown</MenuItem>
                </Select>
              </FormControl>
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
          
          {/* Network Config Tab */}
          {activeTab === 1 && (
            <Box sx={{ mt: 2 }}>
              <IPAddressManager
                deviceType="NETWORK_DEVICE"
                deviceId={formData.id || 0}
                deviceName={formData.name || 'New Network Device'}
                readOnly={!editMode}
              />
              
              {/* Legacy IP field - hidden but kept for backward compatibility */}
              <Box sx={{ display: 'none' }}>
                <TextField
                  label="Management IP (Legacy)"
                  value={formData.managementIp || ''}
                  onChange={(e) => setFormData({ ...formData, managementIp: e.target.value })}
                  fullWidth
                  size="small"
                />
              </Box>
              
              {/* Additional Network Configuration */}
              <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2, mt: 3 }}>
              <TextField
                label="MAC Address"
                value={formData.macAddress || ''}
                onChange={(e) => setFormData({ ...formData, macAddress: e.target.value })}
                fullWidth
                size="small"
                placeholder="aa:bb:cc:dd:ee:ff"
              />
              <TextField
                label="Subnet"
                value={formData.subnet || ''}
                onChange={(e) => setFormData({ ...formData, subnet: e.target.value })}
                fullWidth
                size="small"
                placeholder="192.168.1.0/24"
              />
              <TextField
                label="Gateway"
                value={formData.gateway || ''}
                onChange={(e) => setFormData({ ...formData, gateway: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="VLAN ID"
                type="number"
                value={formData.vlanId || ''}
                onChange={(e) => setFormData({ ...formData, vlanId: parseInt(e.target.value) || null })}
                fullWidth
                size="small"
              />
              <TextField
                label="Management Port"
                type="number"
                value={formData.managementPort || ''}
                onChange={(e) => setFormData({ ...formData, managementPort: parseInt(e.target.value) || null })}
                fullWidth
                size="small"
              />
              <FormControl fullWidth size="small">
                <InputLabel>Management Protocol</InputLabel>
                <Select
                  value={formData.managementProtocol || ''}
                  onChange={(e) => setFormData({ ...formData, managementProtocol: e.target.value })}
                  label="Management Protocol"
                >
                  <MenuItem value="SSH">SSH</MenuItem>
                  <MenuItem value="TELNET">Telnet</MenuItem>
                  <MenuItem value="HTTPS">HTTPS</MenuItem>
                  <MenuItem value="HTTP">HTTP</MenuItem>
                  <MenuItem value="SNMP">SNMP</MenuItem>
                </Select>
              </FormControl>
              <TextField
                label="Port Count"
                type="number"
                value={formData.portCount || ''}
                onChange={(e) => setFormData({ ...formData, portCount: parseInt(e.target.value) || null })}
                fullWidth
                size="small"
              />
              <TextField
                label="DNS Servers"
                value={formData.dnsServers || ''}
                onChange={(e) => setFormData({ ...formData, dnsServers: e.target.value })}
                fullWidth
                size="small"
                placeholder="8.8.8.8,8.8.4.4"
              />
              <TextField
                label="SNMP Community"
                value={formData.snmpCommunity || ''}
                onChange={(e) => setFormData({ ...formData, snmpCommunity: e.target.value })}
                fullWidth
                size="small"
                type="password"
              />
              </Box>
            </Box>
          )}

          {/* Physical Location Tab */}
          {activeTab === 2 && (
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2, mt: 2 }}>
              <TextField
                label="Serial Number"
                value={formData.serialNumber || ''}
                onChange={(e) => setFormData({ ...formData, serialNumber: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="Rack Position"
                value={formData.rackPosition || ''}
                onChange={(e) => setFormData({ ...formData, rackPosition: e.target.value })}
                fullWidth
                size="small"
                placeholder="R1-U10"
              />
              <FormControl fullWidth size="small">
                <InputLabel>Environment</InputLabel>
                <Select
                  value={formData.environment || ''}
                  onChange={(e) => setFormData({ ...formData, environment: e.target.value })}
                  label="Environment"
                >
                  <MenuItem value="production">Production</MenuItem>
                  <MenuItem value="staging">Staging</MenuItem>
                  <MenuItem value="development">Development</MenuItem>
                  <MenuItem value="test">Test</MenuItem>
                  <MenuItem value="qa">QA</MenuItem>
                </Select>
              </FormControl>
              <FormControl fullWidth size="small">
                <InputLabel>Criticality</InputLabel>
                <Select
                  value={formData.criticality || ''}
                  onChange={(e) => setFormData({ ...formData, criticality: e.target.value })}
                  label="Criticality"
                >
                  <MenuItem value="critical">Critical</MenuItem>
                  <MenuItem value="high">High</MenuItem>
                  <MenuItem value="medium">Medium</MenuItem>
                  <MenuItem value="low">Low</MenuItem>
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
                label="Firmware Version"
                value={formData.firmwareVersion || ''}
                onChange={(e) => setFormData({ ...formData, firmwareVersion: e.target.value })}
                fullWidth
                size="small"
              />
            </Box>
          )}

          {/* Performance Tab */}
          {activeTab === 3 && (
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2, mt: 2 }}>
              <TextField
                label="CPU Utilization (%)"
                type="number"
                inputProps={{ step: "0.01", min: "0", max: "100" }}
                value={formData.cpuUtilizationPercent || ''}
                onChange={(e) => setFormData({ ...formData, cpuUtilizationPercent: parseFloat(e.target.value) || null })}
                fullWidth
                size="small"
              />
              <TextField
                label="Memory Utilization (%)"
                type="number"
                inputProps={{ step: "0.01", min: "0", max: "100" }}
                value={formData.memoryUtilizationPercent || ''}
                onChange={(e) => setFormData({ ...formData, memoryUtilizationPercent: parseFloat(e.target.value) || null })}
                fullWidth
                size="small"
              />
              <TextField
                label="Total Memory (MB)"
                type="number"
                value={formData.totalMemoryMb || ''}
                onChange={(e) => setFormData({ ...formData, totalMemoryMb: parseInt(e.target.value) || null })}
                fullWidth
                size="small"
              />
              <TextField
                label="Available Storage (GB)"
                type="number"
                value={formData.availableStorageGb || ''}
                onChange={(e) => setFormData({ ...formData, availableStorageGb: parseInt(e.target.value) || null })}
                fullWidth
                size="small"
              />
              <TextField
                label="Bandwidth Utilization (Mbps)"
                type="number"
                inputProps={{ step: "0.01" }}
                value={formData.bandwidthUtilizationMbps || ''}
                onChange={(e) => setFormData({ ...formData, bandwidthUtilizationMbps: parseFloat(e.target.value) || null })}
                fullWidth
                size="small"
              />
              <TextField
                label="Uptime (hours)"
                type="number"
                value={formData.uptimeHours || ''}
                onChange={(e) => setFormData({ ...formData, uptimeHours: parseInt(e.target.value) || null })}
                fullWidth
                size="small"
              />
              <TextField
                label="Last Seen"
                type="datetime-local"
                value={formData.lastSeen || ''}
                onChange={(e) => setFormData({ ...formData, lastSeen: e.target.value })}
                fullWidth
                size="small"
                InputLabelProps={{ shrink: true }}
              />
            </Box>
          )}

          {/* Management Tab */}
          {activeTab === 4 && (
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2, mt: 2 }}>
              <TextField
                label="SSH Username"
                value={formData.sshUsername || ''}
                onChange={(e) => setFormData({ ...formData, sshUsername: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="SSH Key Path"
                value={formData.sshKeyPath || ''}
                onChange={(e) => setFormData({ ...formData, sshKeyPath: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="Configuration Backup Path"
                value={formData.configurationBackupPath || ''}
                onChange={(e) => setFormData({ ...formData, configurationBackupPath: e.target.value })}
                fullWidth
                size="small"
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
              <TextField
                label="Support Contract"
                value={formData.supportContract || ''}
                onChange={(e) => setFormData({ ...formData, supportContract: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="Owner Contact"
                value={formData.ownerContact || ''}
                onChange={(e) => setFormData({ ...formData, ownerContact: e.target.value })}
                fullWidth
                size="small"
                placeholder="admin@company.com"
              />
            </Box>
          )}

          {/* Business Tab */}
          {activeTab === 5 && (
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2, mt: 2 }}>
              <TextField
                label="Cost Center"
                value={formData.costCenter || ''}
                onChange={(e) => setFormData({ ...formData, costCenter: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="Compliance Zone"
                value={formData.complianceZone || ''}
                onChange={(e) => setFormData({ ...formData, complianceZone: e.target.value })}
                fullWidth
                size="small"
                placeholder="GDPR, HIPAA, PCI-DSS, SOX"
              />
              <TextField
                label="Alert Email"
                value={formData.alertEmail || ''}
                onChange={(e) => setFormData({ ...formData, alertEmail: e.target.value })}
                fullWidth
                size="small"
                type="email"
                placeholder="alerts@company.com"
              />
              <TextField
                label="Alert Threshold CPU (%)"
                type="number"
                inputProps={{ step: "0.01", min: "0", max: "100" }}
                value={formData.alertThresholdCpu || ''}
                onChange={(e) => setFormData({ ...formData, alertThresholdCpu: parseFloat(e.target.value) || null })}
                fullWidth
                size="small"
              />
              <TextField
                label="Alert Threshold Memory (%)"
                type="number"
                inputProps={{ step: "0.01", min: "0", max: "100" }}
                value={formData.alertThresholdMemory || ''}
                onChange={(e) => setFormData({ ...formData, alertThresholdMemory: parseFloat(e.target.value) || null })}
                fullWidth
                size="small"
              />
              <TextField
                label="Alert Threshold Bandwidth (%)"
                type="number"
                inputProps={{ step: "0.01", min: "0", max: "100" }}
                value={formData.alertThresholdBandwidth || ''}
                onChange={(e) => setFormData({ ...formData, alertThresholdBandwidth: parseFloat(e.target.value) || null })}
                fullWidth
                size="small"
              />
            </Box>
          )}

          {/* Monitoring Tab */}
          {activeTab === 6 && (
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
            </Box>
          )}

          {/* Metadata Tab */}
          {activeTab === 7 && (
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2, mt: 2, px: 3, maxWidth: 800, mx: 'auto' }}>
              <TextField
                label="Created By"
                value={formData.createdBy || ''}
                onChange={(e) => setFormData({ ...formData, createdBy: e.target.value })}
                fullWidth
                size="small"
                disabled
              />
              <TextField
                label="Updated By"
                value={formData.updatedBy || ''}
                onChange={(e) => setFormData({ ...formData, updatedBy: e.target.value })}
                fullWidth
                size="small"
                disabled
              />
              <TextField
                label="Created At"
                type="datetime-local"
                value={formData.createdAt || ''}
                onChange={(e) => setFormData({ ...formData, createdAt: e.target.value })}
                fullWidth
                size="small"
                InputLabelProps={{ shrink: true }}
                disabled
              />
              <TextField
                label="Updated At"
                type="datetime-local"
                value={formData.updatedAt || ''}
                onChange={(e) => setFormData({ ...formData, updatedAt: e.target.value })}
                fullWidth
                size="small"
                InputLabelProps={{ shrink: true }}
                disabled
              />
              <Box sx={{ gridColumn: 'span 2' }}>
                <TextField
                  label="Tags"
                  value={formData.tags || ''}
                  onChange={(e) => setFormData({ ...formData, tags: e.target.value })}
                  fullWidth
                  size="small"
                  placeholder="core,router,cisco,production"
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
            </Box>
          )}
          
          {/* Remote Access Tab */}
          {activeTab === 8 && (
            <Box sx={{ mt: 2 }}>
              <RemoteAccessTab
                deviceType="NETWORK_DEVICE"
                deviceId={formData.id || 0}
                deviceName={formData.name || 'New Network Device'}
              />
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

export default NetworkDeviceEditDialog;