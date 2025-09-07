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
    <Dialog open={open} onClose={onClose} maxWidth="lg" fullWidth>
      <DialogTitle>{editMode ? 'Edit' : 'Add'} Compute Node</DialogTitle>
      <DialogContent>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={activeTab} onChange={handleTabChange} variant="scrollable">
            <Tab label="Basic Info" />
            <Tab label="Hardware" />
            <Tab label="OS & Software" />
            <Tab label="Network" />
            <Tab label="Storage" />
            <Tab label="Performance" />
            <Tab label="Compliance & Security" />
            <Tab label="Maintenance" />
            <Tab label="Monitoring" />
            <Tab label="Additional" />
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
                onChange={(e) => setFormData({ ...formData, cpuCores: parseInt(e.target.value) || 0 })}
                fullWidth
                size="small"
              />
              <TextField
                label="Memory (GB)"
                type="number"
                value={formData.memoryGb || ''}
                onChange={(e) => setFormData({ ...formData, memoryGb: parseInt(e.target.value) || 0 })}
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
          
          {/* Hardware Tab */}
          {activeTab === 1 && (
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2, mt: 2 }}>
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
              <FormControlLabel
                control={
                  <Checkbox
                    checked={formData.isPhysical || false}
                    onChange={(e) => setFormData({ ...formData, isPhysical: e.target.checked })}
                  />
                }
                label="Is Physical Server"
              />
            </Box>
          )}

          {/* OS & Software Tab */}
          {activeTab === 2 && (
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2, mt: 2 }}>
              <TextField
                label="Operating System"
                value={formData.operatingSystem || ''}
                onChange={(e) => setFormData({ ...formData, operatingSystem: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="OS Version"
                value={formData.osVersion || ''}
                onChange={(e) => setFormData({ ...formData, osVersion: e.target.value })}
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
                label="Hypervisor"
                value={formData.hypervisor || ''}
                onChange={(e) => setFormData({ ...formData, hypervisor: e.target.value })}
                fullWidth
                size="small"
              />
              <Box sx={{ gridColumn: 'span 2' }}>
                <TextField
                  label="Installed Software"
                  value={formData.installedSoftware || ''}
                  onChange={(e) => setFormData({ ...formData, installedSoftware: e.target.value })}
                  fullWidth
                  multiline
                  rows={2}
                  size="small"
                />
              </Box>
            </Box>
          )}

          {/* Network Tab */}
          {activeTab === 3 && (
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2, mt: 2 }}>
              <TextField
                label="MAC Address"
                value={formData.macAddress || ''}
                onChange={(e) => setFormData({ ...formData, macAddress: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="Gateway"
                value={formData.gateway || ''}
                onChange={(e) => setFormData({ ...formData, gateway: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="DNS Server"
                value={formData.dnsServer || ''}
                onChange={(e) => setFormData({ ...formData, dnsServer: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="Domain"
                value={formData.domain || ''}
                onChange={(e) => setFormData({ ...formData, domain: e.target.value })}
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
                label="Network Speed"
                value={formData.networkSpeed || ''}
                onChange={(e) => setFormData({ ...formData, networkSpeed: e.target.value })}
                fullWidth
                size="small"
              />
            </Box>
          )}

          {/* Storage Tab */}
          {activeTab === 4 && (
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2, mt: 2 }}>
              <TextField
                label="Storage (GB)"
                type="number"
                value={formData.storageGb || ''}
                onChange={(e) => setFormData({ ...formData, storageGb: parseInt(e.target.value) || 0 })}
                fullWidth
                size="small"
              />
              <TextField
                label="Storage Type"
                value={formData.storageType || ''}
                onChange={(e) => setFormData({ ...formData, storageType: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="Available Storage (GB)"
                type="number"
                value={formData.availableStorageGb || ''}
                onChange={(e) => setFormData({ ...formData, availableStorageGb: parseInt(e.target.value) || 0 })}
                fullWidth
                size="small"
              />
              <TextField
                label="Disk Count"
                type="number"
                value={formData.diskCount || ''}
                onChange={(e) => setFormData({ ...formData, diskCount: parseInt(e.target.value) || 0 })}
                fullWidth
                size="small"
              />
              <FormControlLabel
                control={
                  <Checkbox
                    checked={formData.raidConfig || false}
                    onChange={(e) => setFormData({ ...formData, raidConfig: e.target.checked })}
                  />
                }
                label="RAID Configured"
              />
            </Box>
          )}

          {/* Performance Tab */}
          {activeTab === 5 && (
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2, mt: 2 }}>
              <TextField
                label="CPU Usage (%)"
                type="number"
                value={formData.cpuUsagePercent || ''}
                onChange={(e) => setFormData({ ...formData, cpuUsagePercent: parseFloat(e.target.value) || 0 })}
                fullWidth
                size="small"
              />
              <TextField
                label="Memory Usage (%)"
                type="number"
                value={formData.memoryUsagePercent || ''}
                onChange={(e) => setFormData({ ...formData, memoryUsagePercent: parseFloat(e.target.value) || 0 })}
                fullWidth
                size="small"
              />
              <TextField
                label="Disk Usage (%)"
                type="number"
                value={formData.diskUsagePercent || ''}
                onChange={(e) => setFormData({ ...formData, diskUsagePercent: parseFloat(e.target.value) || 0 })}
                fullWidth
                size="small"
              />
              <TextField
                label="Load Average"
                type="number"
                inputProps={{ step: "0.01" }}
                value={formData.loadAverage || ''}
                onChange={(e) => setFormData({ ...formData, loadAverage: parseFloat(e.target.value) || 0 })}
                fullWidth
                size="small"
              />
              <TextField
                label="Uptime (hours)"
                type="number"
                value={formData.uptimeHours || ''}
                onChange={(e) => setFormData({ ...formData, uptimeHours: parseInt(e.target.value) || 0 })}
                fullWidth
                size="small"
              />
              <TextField
                label="Network Latency (ms)"
                type="number"
                value={formData.networkLatency || ''}
                onChange={(e) => setFormData({ ...formData, networkLatency: parseInt(e.target.value) || 0 })}
                fullWidth
                size="small"
              />
            </Box>
          )}

          {/* Compliance & Security Tab */}
          {activeTab === 6 && (
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2, mt: 2 }}>
              <TextField
                label="Security Patch Level"
                value={formData.securityPatchLevel || ''}
                onChange={(e) => setFormData({ ...formData, securityPatchLevel: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="Compliance Status"
                value={formData.complianceStatus || ''}
                onChange={(e) => setFormData({ ...formData, complianceStatus: e.target.value })}
                fullWidth
                size="small"
              />
              <FormControlLabel
                control={
                  <Checkbox
                    checked={formData.firewallEnabled || false}
                    onChange={(e) => setFormData({ ...formData, firewallEnabled: e.target.checked })}
                  />
                }
                label="Firewall Enabled"
              />
              <FormControlLabel
                control={
                  <Checkbox
                    checked={formData.antivirusInstalled || false}
                    onChange={(e) => setFormData({ ...formData, antivirusInstalled: e.target.checked })}
                  />
                }
                label="Antivirus Installed"
              />
              <TextField
                label="Backup Status"
                value={formData.backupStatus || ''}
                onChange={(e) => setFormData({ ...formData, backupStatus: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="Encryption Level"
                value={formData.encryptionLevel || ''}
                onChange={(e) => setFormData({ ...formData, encryptionLevel: e.target.value })}
                fullWidth
                size="small"
              />
            </Box>
          )}

          {/* Maintenance Tab */}
          {activeTab === 7 && (
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2, mt: 2 }}>
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
                label="Support Contract"
                value={formData.supportContract || ''}
                onChange={(e) => setFormData({ ...formData, supportContract: e.target.value })}
                fullWidth
                size="small"
              />
              <Box sx={{ gridColumn: 'span 2' }}>
                <TextField
                  label="Maintenance Notes"
                  value={formData.maintenanceNotes || ''}
                  onChange={(e) => setFormData({ ...formData, maintenanceNotes: e.target.value })}
                  fullWidth
                  multiline
                  rows={3}
                  size="small"
                />
              </Box>
            </Box>
          )}

          {/* Monitoring Tab */}
          {activeTab === 8 && (
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2, mt: 2 }}>
              <TextField
                label="Monitoring Agent"
                value={formData.monitoringAgent || ''}
                onChange={(e) => setFormData({ ...formData, monitoringAgent: e.target.value })}
                fullWidth
                size="small"
              />
              <TextField
                label="Alert Threshold"
                value={formData.alertThreshold || ''}
                onChange={(e) => setFormData({ ...formData, alertThreshold: e.target.value })}
                fullWidth
                size="small"
              />
              <FormControlLabel
                control={
                  <Checkbox
                    checked={formData.loggingEnabled || false}
                    onChange={(e) => setFormData({ ...formData, loggingEnabled: e.target.checked })}
                  />
                }
                label="Logging Enabled"
              />
              <FormControlLabel
                control={
                  <Checkbox
                    checked={formData.metricsCollection || false}
                    onChange={(e) => setFormData({ ...formData, metricsCollection: e.target.checked })}
                  />
                }
                label="Metrics Collection"
              />
              <TextField
                label="Log Retention (days)"
                type="number"
                value={formData.logRetentionDays || ''}
                onChange={(e) => setFormData({ ...formData, logRetentionDays: parseInt(e.target.value) || 0 })}
                fullWidth
                size="small"
              />
              <TextField
                label="Health Check Interval"
                value={formData.healthCheckInterval || ''}
                onChange={(e) => setFormData({ ...formData, healthCheckInterval: e.target.value })}
                fullWidth
                size="small"
              />
            </Box>
          )}

          {/* Additional Tab */}
          {activeTab === 9 && (
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2, mt: 2 }}>
              <TextField
                label="Owner"
                value={formData.owner || ''}
                onChange={(e) => setFormData({ ...formData, owner: e.target.value })}
                fullWidth
                size="small"
              />
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
              <TextField
                label="Purchase Date"
                type="date"
                value={formData.purchaseDate || ''}
                onChange={(e) => setFormData({ ...formData, purchaseDate: e.target.value })}
                fullWidth
                size="small"
                InputLabelProps={{ shrink: true }}
              />
              <TextField
                label="Purchase Price"
                type="number"
                value={formData.purchasePrice || ''}
                onChange={(e) => setFormData({ ...formData, purchasePrice: parseFloat(e.target.value) || 0 })}
                fullWidth
                size="small"
              />
              <TextField
                label="Location"
                value={formData.location || ''}
                onChange={(e) => setFormData({ ...formData, location: e.target.value })}
                fullWidth
                size="small"
              />
              <Box sx={{ gridColumn: 'span 2' }}>
                <TextField
                  label="Tags"
                  value={formData.tags || ''}
                  onChange={(e) => setFormData({ ...formData, tags: e.target.value })}
                  fullWidth
                  size="small"
                  placeholder="Comma-separated tags"
                />
              </Box>
              <Box sx={{ gridColumn: 'span 2' }}>
                <TextField
                  label="Additional Notes"
                  value={formData.additionalNotes || ''}
                  onChange={(e) => setFormData({ ...formData, additionalNotes: e.target.value })}
                  fullWidth
                  multiline
                  rows={3}
                  size="small"
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