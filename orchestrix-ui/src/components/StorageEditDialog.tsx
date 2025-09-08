import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Box,
  Typography,
  Tabs,
  Tab,
  Grid,
  FormControlLabel,
  Switch,
  Divider,
  InputAdornment
} from '@mui/material';
import RemoteAccessTab from './RemoteAccessTab';

interface StorageEditDialogProps {
  open: boolean;
  onClose: () => void;
  storage?: any;
  datacenters?: any[];
  clouds?: any[];
  onSave: (storage: any) => void;
}

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`storage-tabpanel-${index}`}
      aria-labelledby={`storage-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

const StorageEditDialog: React.FC<StorageEditDialogProps> = ({
  open,
  onClose,
  storage,
  datacenters = [],
  clouds = [],
  onSave
}) => {
  const [activeTab, setActiveTab] = useState(0);
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    storageType: 'SAN',
    capacityGb: 1000,
    usedGb: 0,
    availableGb: 1000,
    protocol: 'NFS',
    mountPath: '',
    status: 'ACTIVE',
    cloudId: '',
    datacenterId: '',
    vendor: '',
    model: '',
    serialNumber: '',
    performanceTier: 'STANDARD',
    iops: 0,
    throughputMbps: 0,
    encryption: false,
    replication: false,
    snapshotEnabled: false
  });

  useEffect(() => {
    if (storage) {
      setFormData({
        ...formData,
        ...storage
      });
    }
  }, [storage]);

  const handleChange = (field: string, value: any) => {
    setFormData(prev => {
      const updated = { ...prev, [field]: value };
      
      // Auto-calculate available storage
      if (field === 'capacityGb' || field === 'usedGb') {
        const capacity = field === 'capacityGb' ? value : prev.capacityGb;
        const used = field === 'usedGb' ? value : prev.usedGb;
        updated.availableGb = Math.max(0, capacity - used);
      }
      
      return updated;
    });
  };

  const handleSubmit = () => {
    onSave(formData);
    onClose();
  };

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setActiveTab(newValue);
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="lg" fullWidth>
      <DialogTitle>
        <Typography variant="h6">
          {storage ? 'Edit Storage' : 'Add Storage'} {formData.name && `- ${formData.name}`}
        </Typography>
      </DialogTitle>
      
      <DialogContent>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={activeTab} onChange={handleTabChange} variant="scrollable">
            <Tab label="Basic Info" />
            <Tab label="Configuration" />
            <Tab label="Performance" />
            <Tab label="Location" />
            <Tab label="Features" />
            {storage && <Tab label="Remote Access" />}
          </Tabs>
        </Box>

        {/* Basic Info Tab */}
        <TabPanel value={activeTab} index={0}>
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <TextField
                label="Storage Name"
                value={formData.name}
                onChange={(e) => handleChange('name', e.target.value)}
                fullWidth
                required
              />
            </Grid>
            
            <Grid item xs={12}>
              <TextField
                label="Description"
                value={formData.description}
                onChange={(e) => handleChange('description', e.target.value)}
                fullWidth
                multiline
                rows={2}
              />
            </Grid>
            
            <Grid item xs={6}>
              <FormControl fullWidth>
                <InputLabel>Storage Type</InputLabel>
                <Select
                  value={formData.storageType}
                  onChange={(e) => handleChange('storageType', e.target.value)}
                  label="Storage Type"
                >
                  <MenuItem value="SAN">SAN</MenuItem>
                  <MenuItem value="NAS">NAS</MenuItem>
                  <MenuItem value="LOCAL">Local Storage</MenuItem>
                  <MenuItem value="OBJECT">Object Storage</MenuItem>
                  <MenuItem value="BLOCK">Block Storage</MenuItem>
                  <MenuItem value="FILE">File Storage</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            
            <Grid item xs={6}>
              <FormControl fullWidth>
                <InputLabel>Status</InputLabel>
                <Select
                  value={formData.status}
                  onChange={(e) => handleChange('status', e.target.value)}
                  label="Status"
                >
                  <MenuItem value="ACTIVE">Active</MenuItem>
                  <MenuItem value="INACTIVE">Inactive</MenuItem>
                  <MenuItem value="MAINTENANCE">Maintenance</MenuItem>
                  <MenuItem value="DECOMMISSIONED">Decommissioned</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            
            <Grid item xs={6}>
              <TextField
                label="Vendor"
                value={formData.vendor}
                onChange={(e) => handleChange('vendor', e.target.value)}
                fullWidth
                placeholder="e.g., NetApp, EMC, HPE"
              />
            </Grid>
            
            <Grid item xs={6}>
              <TextField
                label="Model"
                value={formData.model}
                onChange={(e) => handleChange('model', e.target.value)}
                fullWidth
              />
            </Grid>
            
            <Grid item xs={12}>
              <TextField
                label="Serial Number"
                value={formData.serialNumber}
                onChange={(e) => handleChange('serialNumber', e.target.value)}
                fullWidth
              />
            </Grid>
          </Grid>
        </TabPanel>

        {/* Configuration Tab */}
        <TabPanel value={activeTab} index={1}>
          <Grid container spacing={2}>
            <Grid item xs={6}>
              <FormControl fullWidth>
                <InputLabel>Protocol</InputLabel>
                <Select
                  value={formData.protocol}
                  onChange={(e) => handleChange('protocol', e.target.value)}
                  label="Protocol"
                >
                  <MenuItem value="NFS">NFS</MenuItem>
                  <MenuItem value="iSCSI">iSCSI</MenuItem>
                  <MenuItem value="FC">Fibre Channel</MenuItem>
                  <MenuItem value="SMB">SMB/CIFS</MenuItem>
                  <MenuItem value="S3">S3</MenuItem>
                  <MenuItem value="AZURE_BLOB">Azure Blob</MenuItem>
                  <MenuItem value="GCS">Google Cloud Storage</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            
            <Grid item xs={6}>
              <TextField
                label="Mount Path"
                value={formData.mountPath}
                onChange={(e) => handleChange('mountPath', e.target.value)}
                fullWidth
                placeholder="/mnt/storage"
              />
            </Grid>
            
            <Grid item xs={4}>
              <TextField
                label="Total Capacity"
                type="number"
                value={formData.capacityGb}
                onChange={(e) => handleChange('capacityGb', parseInt(e.target.value) || 0)}
                fullWidth
                InputProps={{
                  endAdornment: <InputAdornment position="end">GB</InputAdornment>
                }}
              />
            </Grid>
            
            <Grid item xs={4}>
              <TextField
                label="Used Space"
                type="number"
                value={formData.usedGb}
                onChange={(e) => handleChange('usedGb', parseInt(e.target.value) || 0)}
                fullWidth
                InputProps={{
                  endAdornment: <InputAdornment position="end">GB</InputAdornment>
                }}
              />
            </Grid>
            
            <Grid item xs={4}>
              <TextField
                label="Available Space"
                type="number"
                value={formData.availableGb}
                fullWidth
                disabled
                InputProps={{
                  endAdornment: <InputAdornment position="end">GB</InputAdornment>
                }}
              />
            </Grid>
          </Grid>
        </TabPanel>

        {/* Performance Tab */}
        <TabPanel value={activeTab} index={2}>
          <Grid container spacing={2}>
            <Grid item xs={6}>
              <FormControl fullWidth>
                <InputLabel>Performance Tier</InputLabel>
                <Select
                  value={formData.performanceTier}
                  onChange={(e) => handleChange('performanceTier', e.target.value)}
                  label="Performance Tier"
                >
                  <MenuItem value="ULTRA">Ultra</MenuItem>
                  <MenuItem value="PREMIUM">Premium</MenuItem>
                  <MenuItem value="STANDARD">Standard</MenuItem>
                  <MenuItem value="ARCHIVE">Archive</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            
            <Grid item xs={6}>
              <TextField
                label="IOPS"
                type="number"
                value={formData.iops}
                onChange={(e) => handleChange('iops', parseInt(e.target.value) || 0)}
                fullWidth
                helperText="Input/Output Operations Per Second"
              />
            </Grid>
            
            <Grid item xs={6}>
              <TextField
                label="Throughput"
                type="number"
                value={formData.throughputMbps}
                onChange={(e) => handleChange('throughputMbps', parseInt(e.target.value) || 0)}
                fullWidth
                InputProps={{
                  endAdornment: <InputAdornment position="end">MB/s</InputAdornment>
                }}
              />
            </Grid>
          </Grid>
        </TabPanel>

        {/* Location Tab */}
        <TabPanel value={activeTab} index={3}>
          <Grid container spacing={2}>
            <Grid item xs={6}>
              <FormControl fullWidth>
                <InputLabel>Cloud</InputLabel>
                <Select
                  value={formData.cloudId}
                  onChange={(e) => handleChange('cloudId', e.target.value)}
                  label="Cloud"
                >
                  <MenuItem value="">None</MenuItem>
                  {clouds.map((cloud: any) => (
                    <MenuItem key={cloud.id} value={cloud.id}>
                      {cloud.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            
            <Grid item xs={6}>
              <FormControl fullWidth>
                <InputLabel>Datacenter</InputLabel>
                <Select
                  value={formData.datacenterId}
                  onChange={(e) => handleChange('datacenterId', e.target.value)}
                  label="Datacenter"
                >
                  <MenuItem value="">None</MenuItem>
                  {datacenters.map((dc: any) => (
                    <MenuItem key={dc.id} value={dc.id}>
                      {dc.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
          </Grid>
        </TabPanel>

        {/* Features Tab */}
        <TabPanel value={activeTab} index={4}>
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.encryption}
                    onChange={(e) => handleChange('encryption', e.target.checked)}
                  />
                }
                label="Encryption Enabled"
              />
            </Grid>
            
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.replication}
                    onChange={(e) => handleChange('replication', e.target.checked)}
                  />
                }
                label="Replication Enabled"
              />
            </Grid>
            
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.snapshotEnabled}
                    onChange={(e) => handleChange('snapshotEnabled', e.target.checked)}
                  />
                }
                label="Snapshots Enabled"
              />
            </Grid>
          </Grid>
        </TabPanel>

        {/* Remote Access Tab */}
        {storage && (
          <TabPanel value={activeTab} index={5}>
            <RemoteAccessTab
              deviceType="STORAGE"
              deviceId={storage.id}
              deviceName={storage.name}
            />
          </TabPanel>
        )}
      </DialogContent>
      
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button onClick={handleSubmit} variant="contained" color="primary">
          {storage ? 'Update' : 'Create'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default StorageEditDialog;