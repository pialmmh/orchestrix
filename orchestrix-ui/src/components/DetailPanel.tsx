import React, { useState } from 'react';
import {
  Box,
  Typography,
  TextField,
  Button,
  Card,
  CardContent,
  Chip,
  Divider,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Switch,
  FormControlLabel,
  Paper
} from '@mui/material';
import {
  Save as SaveIcon,
  Edit as EditIcon,
  Cancel as CancelIcon
} from '@mui/icons-material';
import { 
  TreeNode, 
  Cloud, 
  Datacenter, 
  Compute, 
  Container, 
  ComputeType, 
  ContainerType 
} from '../types/CloudHierarchy';

interface DetailPanelProps {
  selectedNode?: TreeNode;
  onSave?: (data: any) => void;
}

const DetailPanel: React.FC<DetailPanelProps> = ({ selectedNode, onSave }) => {
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState<any>({});

  React.useEffect(() => {
    if (selectedNode?.data) {
      setFormData({ ...selectedNode.data });
      setIsEditing(false);
    }
  }, [selectedNode]);

  const handleEdit = () => {
    setIsEditing(true);
  };

  const handleSave = () => {
    onSave?.(formData);
    setIsEditing(false);
  };

  const handleCancel = () => {
    setFormData({ ...selectedNode?.data });
    setIsEditing(false);
  };

  const handleChange = (field: string, value: any) => {
    setFormData((prev: any) => ({ ...prev, [field]: value }));
  };

  const renderField = (label: string, value: any, field: string, type: 'text' | 'number' | 'select' | 'switch' = 'text', options?: string[]) => {
    if (isEditing) {
      switch (type) {
        case 'select':
          return (
            <FormControl fullWidth size="small">
              <InputLabel>{label}</InputLabel>
              <Select
                value={value || ''}
                label={label}
                onChange={(e) => handleChange(field, e.target.value)}
              >
                {options?.map(option => (
                  <MenuItem key={option} value={option}>{option}</MenuItem>
                ))}
              </Select>
            </FormControl>
          );
        case 'switch':
          return (
            <FormControlLabel
              control={
                <Switch
                  checked={Boolean(value)}
                  onChange={(e) => handleChange(field, e.target.checked)}
                />
              }
              label={label}
            />
          );
        case 'number':
          return (
            <TextField
              fullWidth
              label={label}
              type="number"
              value={value || ''}
              onChange={(e) => handleChange(field, parseInt(e.target.value))}
              size="small"
            />
          );
        default:
          return (
            <TextField
              fullWidth
              label={label}
              value={value || ''}
              onChange={(e) => handleChange(field, e.target.value)}
              size="small"
            />
          );
      }
    }

    return (
      <Box>
        <Typography variant="caption" color="textSecondary">
          {label}
        </Typography>
        <Typography variant="body2">
          {type === 'switch' ? (value ? 'Yes' : 'No') : (value || 'N/A')}
        </Typography>
      </Box>
    );
  };

  const renderCloudDetails = (cloud: Cloud) => (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 2 }}>
        {renderField('Name', formData.name, 'name')}
        {renderField('Client Name', formData.clientName, 'clientName')}
      </Box>
      {renderField('Description', formData.description, 'description')}
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 2 }}>
        {renderField('Deployment Region', formData.deploymentRegion, 'deploymentRegion')}
        {renderField('Status', formData.status, 'status', 'select', ['ACTIVE', 'INACTIVE', 'MAINTENANCE'])}
      </Box>
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 2 }}>
        <Box>
          <Typography variant="caption" color="textSecondary">
            Datacenters
          </Typography>
          <Typography variant="body2">
            {cloud.datacenters?.length || 0}
          </Typography>
        </Box>
        <Box>
          <Typography variant="caption" color="textSecondary">
            Computes
          </Typography>
          <Typography variant="body2">
            {cloud.computes?.length || 0}
          </Typography>
        </Box>
      </Box>
    </Box>
  );

  const renderDatacenterDetails = (datacenter: Datacenter) => (
    <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
      <Box>
        {renderField('Name', formData.name, 'name')}
      </Box>
      <Box>
        {renderField('Type', formData.type, 'type')}
      </Box>
      <Box>
        {renderField('Provider', formData.provider, 'provider')}
      </Box>
      <Box>
        {renderField('Status', formData.status, 'status', 'select', ['ACTIVE', 'INACTIVE', 'MAINTENANCE'])}
      </Box>
      <Box>
        {renderField('Country', datacenter.country?.name, 'country')}
      </Box>
      <Box>
        {renderField('State', datacenter.state?.name, 'state')}
      </Box>
      <Box>
        {renderField('City', datacenter.city?.name, 'city')}
      </Box>
      <Box>
        {renderField('Location Other', formData.locationOther, 'locationOther')}
      </Box>
      <Box>
        {renderField('Servers', formData.servers, 'servers', 'number')}
      </Box>
      <Box>
        {renderField('Storage (TB)', formData.storageTb, 'storageTb', 'number')}
      </Box>
      <Box>
        {renderField('Utilization (%)', formData.utilization, 'utilization', 'number')}
      </Box>
      <Box>
        {renderField('DR Site', formData.isDrSite, 'isDrSite', 'switch')}
      </Box>
    </Box>
  );

  const renderComputeDetails = (compute: Compute) => (
    <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
      <Box>
        {renderField('Name', formData.name, 'name')}
      </Box>
      <Box>
        {renderField('Hostname', formData.hostname, 'hostname')}
      </Box>
      <Box>
        {renderField('Description', formData.description, 'description')}
      </Box>
      <Box>
        {renderField('Compute Type', formData.computeType, 'computeType', 'select', Object.values(ComputeType))}
      </Box>
      <Box>
        {renderField('Status', formData.status, 'status', 'select', ['ACTIVE', 'INACTIVE', 'MAINTENANCE'])}
      </Box>
      <Box>
        {renderField('IP Address', formData.ipAddress, 'ipAddress')}
      </Box>
      <Box>
        {renderField('Operating System', formData.operatingSystem, 'operatingSystem')}
      </Box>
      <Box>
        {renderField('CPU Cores', formData.cpuCores, 'cpuCores', 'number')}
      </Box>
      <Box>
        {renderField('Memory (GB)', formData.memoryGb, 'memoryGb', 'number')}
      </Box>
      <Box>
        {renderField('Disk (GB)', formData.diskGb, 'diskGb', 'number')}
      </Box>
      <Box>
        <Box>
          <Typography variant="caption" color="textSecondary">
            Containers
          </Typography>
          <Typography variant="body2">
            {compute.containers?.length || 0}
          </Typography>
        </Box>
      </Box>
    </Box>
  );

  const renderContainerDetails = (container: Container) => (
    <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
      <Box>
        {renderField('Name', formData.name, 'name')}
      </Box>
      <Box>
        {renderField('Container ID', formData.containerId, 'containerId')}
      </Box>
      <Box>
        {renderField('Description', formData.description, 'description')}
      </Box>
      <Box>
        {renderField('Container Type', formData.containerType, 'containerType', 'select', Object.values(ContainerType))}
      </Box>
      <Box>
        {renderField('Status', formData.status, 'status', 'select', ['RUNNING', 'STOPPED', 'PAUSED', 'RESTARTING'])}
      </Box>
      <Box>
        {renderField('Image', formData.image, 'image')}
      </Box>
      <Box>
        {renderField('Image Version', formData.imageVersion, 'imageVersion')}
      </Box>
      <Box>
        {renderField('IP Address', formData.ipAddress, 'ipAddress')}
      </Box>
      <Box>
        {renderField('Exposed Ports', formData.exposedPorts, 'exposedPorts')}
      </Box>
      <Box>
        {renderField('Environment Variables', formData.environmentVars, 'environmentVars')}
      </Box>
      <Box>
        {renderField('Mount Points', formData.mountPoints, 'mountPoints')}
      </Box>
      <Box>
        {renderField('CPU Limit', formData.cpuLimit, 'cpuLimit')}
      </Box>
      <Box>
        {renderField('Memory Limit', formData.memoryLimit, 'memoryLimit')}
      </Box>
      <Box>
        {renderField('Auto Start', formData.autoStart, 'autoStart', 'switch')}
      </Box>
    </Box>
  );

  if (!selectedNode) {
    return (
      <Paper sx={{ p: 3, height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <Typography variant="h6" color="textSecondary">
          Select a node from the tree to view details
        </Typography>
      </Paper>
    );
  }

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Box>
            <Typography variant="h6" gutterBottom>
              {selectedNode.name}
            </Typography>
            <Chip 
              label={selectedNode.type.replace('-', ' ').toUpperCase()} 
              size="small" 
              color="primary" 
              variant="outlined" 
            />
          </Box>
          <Box>
            {isEditing ? (
              <>
                <Button
                  startIcon={<SaveIcon />}
                  onClick={handleSave}
                  variant="contained"
                  size="small"
                  sx={{ mr: 1 }}
                >
                  Save
                </Button>
                <Button
                  startIcon={<CancelIcon />}
                  onClick={handleCancel}
                  size="small"
                >
                  Cancel
                </Button>
              </>
            ) : (
              <Button
                startIcon={<EditIcon />}
                onClick={handleEdit}
                size="small"
                disabled={!selectedNode.data || selectedNode.type === 'container-group'}
              >
                Edit
              </Button>
            )}
          </Box>
        </Box>

        <Divider sx={{ mb: 2 }} />

        <Box sx={{ maxHeight: 'calc(100vh - 200px)', overflow: 'auto' }}>
          {selectedNode.type === 'cloud' && renderCloudDetails(selectedNode.data)}
          {selectedNode.type === 'datacenter' && renderDatacenterDetails(selectedNode.data)}
          {selectedNode.type === 'compute' && renderComputeDetails(selectedNode.data)}
          {selectedNode.type === 'container' && renderContainerDetails(selectedNode.data)}
          {selectedNode.type === 'container-group' && (
            <Typography variant="body2" color="textSecondary">
              This is a container group. Select individual containers to view details.
            </Typography>
          )}
        </Box>

        {selectedNode.data?.createdAt && (
          <Box sx={{ mt: 2, pt: 2, borderTop: 1, borderColor: 'divider' }}>
            <Typography variant="caption" color="textSecondary">
              Created: {new Date(selectedNode.data.createdAt).toLocaleString()}
            </Typography>
            {selectedNode.data.updatedAt && (
              <Typography variant="caption" color="textSecondary" sx={{ ml: 2 }}>
                Updated: {new Date(selectedNode.data.updatedAt).toLocaleString()}
              </Typography>
            )}
          </Box>
        )}
      </CardContent>
    </Card>
  );
};

export default DetailPanel;