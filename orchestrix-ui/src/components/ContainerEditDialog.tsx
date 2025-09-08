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
  Chip,
  IconButton,
  InputAdornment
} from '@mui/material';
import { Add, Delete } from '@mui/icons-material';
import RemoteAccessTab from './RemoteAccessTab';

interface ContainerEditDialogProps {
  open: boolean;
  onClose: () => void;
  container?: any;
  computes?: any[];
  clouds?: any[];
  onSave: (container: any) => void;
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
      id={`container-tabpanel-${index}`}
      aria-labelledby={`container-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

const ContainerEditDialog: React.FC<ContainerEditDialogProps> = ({
  open,
  onClose,
  container,
  computes = [],
  clouds = [],
  onSave
}) => {
  const [activeTab, setActiveTab] = useState(0);
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    containerType: 'DOCKER',
    containerId: '',
    image: '',
    imageVersion: 'latest',
    status: 'RUNNING',
    ipAddress: '',
    exposedPorts: [],
    environmentVars: [],
    mountPoints: [],
    cpuLimit: '',
    memoryLimit: '',
    diskLimit: '',
    restartPolicy: 'unless-stopped',
    networkMode: 'bridge',
    privileged: false,
    autoRemove: false,
    hostId: '',
    cloudId: '',
    labels: [],
    command: '',
    entrypoint: ''
  });

  const [newPort, setNewPort] = useState('');
  const [newEnvVar, setNewEnvVar] = useState({ key: '', value: '' });
  const [newMount, setNewMount] = useState({ source: '', target: '', type: 'bind' });
  const [newLabel, setNewLabel] = useState({ key: '', value: '' });

  useEffect(() => {
    if (container) {
      setFormData({
        ...formData,
        ...container,
        exposedPorts: container.exposedPorts ? JSON.parse(container.exposedPorts) : [],
        environmentVars: container.environmentVars ? JSON.parse(container.environmentVars) : [],
        mountPoints: container.mountPoints ? JSON.parse(container.mountPoints) : [],
        labels: container.labels ? JSON.parse(container.labels) : []
      });
    }
  }, [container]);

  const handleChange = (field: string, value: any) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const handleAddPort = () => {
    if (newPort) {
      handleChange('exposedPorts', [...formData.exposedPorts, newPort]);
      setNewPort('');
    }
  };

  const handleRemovePort = (index: number) => {
    const updated = formData.exposedPorts.filter((_: any, i: number) => i !== index);
    handleChange('exposedPorts', updated);
  };

  const handleAddEnvVar = () => {
    if (newEnvVar.key && newEnvVar.value) {
      handleChange('environmentVars', [...formData.environmentVars, newEnvVar]);
      setNewEnvVar({ key: '', value: '' });
    }
  };

  const handleRemoveEnvVar = (index: number) => {
    const updated = formData.environmentVars.filter((_: any, i: number) => i !== index);
    handleChange('environmentVars', updated);
  };

  const handleAddMount = () => {
    if (newMount.source && newMount.target) {
      handleChange('mountPoints', [...formData.mountPoints, newMount]);
      setNewMount({ source: '', target: '', type: 'bind' });
    }
  };

  const handleRemoveMount = (index: number) => {
    const updated = formData.mountPoints.filter((_: any, i: number) => i !== index);
    handleChange('mountPoints', updated);
  };

  const handleSubmit = () => {
    const dataToSave = {
      ...formData,
      exposedPorts: JSON.stringify(formData.exposedPorts),
      environmentVars: JSON.stringify(formData.environmentVars),
      mountPoints: JSON.stringify(formData.mountPoints),
      labels: JSON.stringify(formData.labels)
    };
    onSave(dataToSave);
    onClose();
  };

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setActiveTab(newValue);
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="lg" fullWidth>
      <DialogTitle>
        <Typography variant="h6">
          {container ? 'Edit Container' : 'Add Container'} {formData.name && `- ${formData.name}`}
        </Typography>
      </DialogTitle>
      
      <DialogContent>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={activeTab} onChange={handleTabChange} variant="scrollable">
            <Tab label="Basic Info" />
            <Tab label="Image & Runtime" />
            <Tab label="Network & Ports" />
            <Tab label="Environment" />
            <Tab label="Storage" />
            <Tab label="Resources" />
            {container && <Tab label="Remote Access" />}
          </Tabs>
        </Box>

        {/* Basic Info Tab */}
        <TabPanel value={activeTab} index={0}>
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <TextField
                label="Container Name"
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
                <InputLabel>Container Type</InputLabel>
                <Select
                  value={formData.containerType}
                  onChange={(e) => handleChange('containerType', e.target.value)}
                  label="Container Type"
                >
                  <MenuItem value="DOCKER">Docker</MenuItem>
                  <MenuItem value="LXC">LXC</MenuItem>
                  <MenuItem value="LXD">LXD</MenuItem>
                  <MenuItem value="PODMAN">Podman</MenuItem>
                  <MenuItem value="CONTAINERD">Containerd</MenuItem>
                  <MenuItem value="KUBERNETES">Kubernetes Pod</MenuItem>
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
                  <MenuItem value="RUNNING">Running</MenuItem>
                  <MenuItem value="STOPPED">Stopped</MenuItem>
                  <MenuItem value="PAUSED">Paused</MenuItem>
                  <MenuItem value="RESTARTING">Restarting</MenuItem>
                  <MenuItem value="EXITED">Exited</MenuItem>
                  <MenuItem value="DEAD">Dead</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            
            <Grid item xs={6}>
              <TextField
                label="Container ID"
                value={formData.containerId}
                onChange={(e) => handleChange('containerId', e.target.value)}
                fullWidth
                helperText="Auto-generated if left empty"
              />
            </Grid>
            
            <Grid item xs={6}>
              <FormControl fullWidth>
                <InputLabel>Host Compute</InputLabel>
                <Select
                  value={formData.hostId}
                  onChange={(e) => handleChange('hostId', e.target.value)}
                  label="Host Compute"
                >
                  <MenuItem value="">None</MenuItem>
                  {computes.map((compute: any) => (
                    <MenuItem key={compute.id} value={compute.id}>
                      {compute.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
          </Grid>
        </TabPanel>

        {/* Image & Runtime Tab */}
        <TabPanel value={activeTab} index={1}>
          <Grid container spacing={2}>
            <Grid item xs={8}>
              <TextField
                label="Image"
                value={formData.image}
                onChange={(e) => handleChange('image', e.target.value)}
                fullWidth
                required
                placeholder="e.g., nginx, ubuntu, mysql"
              />
            </Grid>
            
            <Grid item xs={4}>
              <TextField
                label="Version/Tag"
                value={formData.imageVersion}
                onChange={(e) => handleChange('imageVersion', e.target.value)}
                fullWidth
                placeholder="latest"
              />
            </Grid>
            
            <Grid item xs={12}>
              <TextField
                label="Command"
                value={formData.command}
                onChange={(e) => handleChange('command', e.target.value)}
                fullWidth
                placeholder="Override default command"
              />
            </Grid>
            
            <Grid item xs={12}>
              <TextField
                label="Entrypoint"
                value={formData.entrypoint}
                onChange={(e) => handleChange('entrypoint', e.target.value)}
                fullWidth
                placeholder="Override default entrypoint"
              />
            </Grid>
            
            <Grid item xs={6}>
              <FormControl fullWidth>
                <InputLabel>Restart Policy</InputLabel>
                <Select
                  value={formData.restartPolicy}
                  onChange={(e) => handleChange('restartPolicy', e.target.value)}
                  label="Restart Policy"
                >
                  <MenuItem value="no">No</MenuItem>
                  <MenuItem value="always">Always</MenuItem>
                  <MenuItem value="on-failure">On Failure</MenuItem>
                  <MenuItem value="unless-stopped">Unless Stopped</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.privileged}
                    onChange={(e) => handleChange('privileged', e.target.checked)}
                  />
                }
                label="Privileged Mode"
              />
            </Grid>
            
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.autoRemove}
                    onChange={(e) => handleChange('autoRemove', e.target.checked)}
                  />
                }
                label="Auto Remove on Stop"
              />
            </Grid>
          </Grid>
        </TabPanel>

        {/* Network & Ports Tab */}
        <TabPanel value={activeTab} index={2}>
          <Grid container spacing={2}>
            <Grid item xs={6}>
              <TextField
                label="IP Address"
                value={formData.ipAddress}
                onChange={(e) => handleChange('ipAddress', e.target.value)}
                fullWidth
                placeholder="Auto-assigned if empty"
              />
            </Grid>
            
            <Grid item xs={6}>
              <FormControl fullWidth>
                <InputLabel>Network Mode</InputLabel>
                <Select
                  value={formData.networkMode}
                  onChange={(e) => handleChange('networkMode', e.target.value)}
                  label="Network Mode"
                >
                  <MenuItem value="bridge">Bridge</MenuItem>
                  <MenuItem value="host">Host</MenuItem>
                  <MenuItem value="none">None</MenuItem>
                  <MenuItem value="container">Container</MenuItem>
                  <MenuItem value="custom">Custom</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            
            <Grid item xs={12}>
              <Typography variant="subtitle1" gutterBottom>
                Exposed Ports
              </Typography>
              <Box sx={{ display: 'flex', gap: 1, mb: 1 }}>
                <TextField
                  value={newPort}
                  onChange={(e) => setNewPort(e.target.value)}
                  placeholder="e.g., 8080:80"
                  size="small"
                  sx={{ flex: 1 }}
                />
                <Button
                  variant="outlined"
                  startIcon={<Add />}
                  onClick={handleAddPort}
                >
                  Add Port
                </Button>
              </Box>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                {formData.exposedPorts.map((port: string, index: number) => (
                  <Chip
                    key={index}
                    label={port}
                    onDelete={() => handleRemovePort(index)}
                  />
                ))}
              </Box>
            </Grid>
          </Grid>
        </TabPanel>

        {/* Environment Tab */}
        <TabPanel value={activeTab} index={3}>
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <Typography variant="subtitle1" gutterBottom>
                Environment Variables
              </Typography>
              <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
                <TextField
                  value={newEnvVar.key}
                  onChange={(e) => setNewEnvVar({ ...newEnvVar, key: e.target.value })}
                  placeholder="Variable name"
                  size="small"
                  sx={{ flex: 1 }}
                />
                <TextField
                  value={newEnvVar.value}
                  onChange={(e) => setNewEnvVar({ ...newEnvVar, value: e.target.value })}
                  placeholder="Value"
                  size="small"
                  sx={{ flex: 1 }}
                />
                <Button
                  variant="outlined"
                  startIcon={<Add />}
                  onClick={handleAddEnvVar}
                >
                  Add
                </Button>
              </Box>
              {formData.environmentVars.map((envVar: any, index: number) => (
                <Box key={index} sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                  <Typography sx={{ flex: 1 }}>
                    {envVar.key}={envVar.value}
                  </Typography>
                  <IconButton size="small" onClick={() => handleRemoveEnvVar(index)}>
                    <Delete />
                  </IconButton>
                </Box>
              ))}
            </Grid>
          </Grid>
        </TabPanel>

        {/* Storage Tab */}
        <TabPanel value={activeTab} index={4}>
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <Typography variant="subtitle1" gutterBottom>
                Mount Points
              </Typography>
              <Box sx={{ mb: 2 }}>
                <Grid container spacing={1}>
                  <Grid item xs={5}>
                    <TextField
                      value={newMount.source}
                      onChange={(e) => setNewMount({ ...newMount, source: e.target.value })}
                      placeholder="Host path"
                      size="small"
                      fullWidth
                    />
                  </Grid>
                  <Grid item xs={5}>
                    <TextField
                      value={newMount.target}
                      onChange={(e) => setNewMount({ ...newMount, target: e.target.value })}
                      placeholder="Container path"
                      size="small"
                      fullWidth
                    />
                  </Grid>
                  <Grid item xs={2}>
                    <Button
                      variant="outlined"
                      fullWidth
                      onClick={handleAddMount}
                    >
                      Add
                    </Button>
                  </Grid>
                </Grid>
              </Box>
              {formData.mountPoints.map((mount: any, index: number) => (
                <Box key={index} sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                  <Typography sx={{ flex: 1 }}>
                    {mount.source} → {mount.target}
                  </Typography>
                  <IconButton size="small" onClick={() => handleRemoveMount(index)}>
                    <Delete />
                  </IconButton>
                </Box>
              ))}
            </Grid>
          </Grid>
        </TabPanel>

        {/* Resources Tab */}
        <TabPanel value={activeTab} index={5}>
          <Grid container spacing={2}>
            <Grid item xs={4}>
              <TextField
                label="CPU Limit"
                value={formData.cpuLimit}
                onChange={(e) => handleChange('cpuLimit', e.target.value)}
                fullWidth
                placeholder="e.g., 0.5, 2"
                helperText="Number of CPUs"
              />
            </Grid>
            
            <Grid item xs={4}>
              <TextField
                label="Memory Limit"
                value={formData.memoryLimit}
                onChange={(e) => handleChange('memoryLimit', e.target.value)}
                fullWidth
                placeholder="e.g., 512m, 2g"
                helperText="Memory allocation"
              />
            </Grid>
            
            <Grid item xs={4}>
              <TextField
                label="Disk Limit"
                value={formData.diskLimit}
                onChange={(e) => handleChange('diskLimit', e.target.value)}
                fullWidth
                placeholder="e.g., 10g, 100g"
                helperText="Disk space limit"
              />
            </Grid>
          </Grid>
        </TabPanel>

        {/* Remote Access Tab */}
        {container && (
          <TabPanel value={activeTab} index={6}>
            <RemoteAccessTab
              deviceType="CONTAINER"
              deviceId={container.id}
              deviceName={container.name}
            />
          </TabPanel>
        )}
      </DialogContent>
      
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button onClick={handleSubmit} variant="contained" color="primary">
          {container ? 'Update' : 'Create'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default ContainerEditDialog;