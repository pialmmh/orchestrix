import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Typography,
  Box,
  IconButton,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Switch,
  FormControlLabel,
  Tabs,
  Tab,
  Chip,
  Accordion,
  AccordionSummary,
  AccordionDetails
} from '@mui/material';
import {
  Close as CloseIcon,
  Edit as EditIcon,
  Save as SaveIcon,
  Cancel as CancelIcon,
  ExpandMore as ExpandMoreIcon
} from '@mui/icons-material';

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
      id={`entity-tabpanel-${index}`}
      aria-labelledby={`entity-tab-${index}`}
      {...other}
    >
      {value === index && (
        <Box sx={{ p: 2 }}>
          {children}
        </Box>
      )}
    </div>
  );
}

interface FormField {
  name: string;
  label: string;
  type: 'text' | 'number' | 'select' | 'boolean' | 'textarea' | 'array' | 'object';
  required?: boolean;
  readOnly?: boolean;
  options?: { label: string; value: any }[];
  multiline?: boolean;
  rows?: number;
  category?: string;
  section?: string;
}

export interface FieldGroup {
  title: string;
  fields: FormField[];
  collapsible?: boolean;
  defaultExpanded?: boolean;
}

interface TabbedEntityModalProps {
  open: boolean;
  onClose: () => void;
  title: string;
  data: any;
  fieldGroups: { [tabName: string]: FieldGroup[] };
  mode: 'view' | 'edit' | 'create';
  onSave?: (data: any) => Promise<void>;
  onDelete?: (data: any) => Promise<void>;
  loading?: boolean;
}

const TabbedEntityModal: React.FC<TabbedEntityModalProps> = ({
  open,
  onClose,
  title,
  data,
  fieldGroups,
  mode,
  onSave,
  onDelete,
  loading = false
}) => {
  const [formData, setFormData] = useState<any>({});
  const [editMode, setEditMode] = useState(mode === 'edit' || mode === 'create');
  const [saving, setSaving] = useState(false);
  const [currentTab, setCurrentTab] = useState(0);

  const tabNames = Object.keys(fieldGroups);

  useEffect(() => {
    if (data) {
      setFormData({ ...data });
    } else {
      // Initialize with default values for create mode
      const defaultData: any = {};
      Object.values(fieldGroups).flat().forEach(group => {
        group.fields.forEach(field => {
          if (field.type === 'boolean') {
            defaultData[field.name] = false;
          } else if (field.type === 'number') {
            defaultData[field.name] = 0;
          } else if (field.type === 'array') {
            defaultData[field.name] = [];
          } else if (field.type === 'object') {
            defaultData[field.name] = {};
          } else {
            defaultData[field.name] = '';
          }
        });
      });
      setFormData(defaultData);
    }
    setEditMode(mode === 'edit' || mode === 'create');
  }, [data, fieldGroups, mode]);

  const handleChange = (fieldName: string, value: any) => {
    setFormData((prev: any) => ({
      ...prev,
      [fieldName]: value
    }));
  };

  const handleSave = async () => {
    if (!onSave) return;
    
    setSaving(true);
    try {
      await onSave(formData);
      onClose();
    } catch (error) {
      console.error('Error saving:', error);
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!onDelete) return;
    
    setSaving(true);
    try {
      await onDelete(formData);
      onClose();
    } catch (error) {
      console.error('Error deleting:', error);
    } finally {
      setSaving(false);
    }
  };

  const renderField = (field: FormField) => {
    const value = formData[field.name] || '';
    const isReadOnly = !editMode || field.readOnly;

    switch (field.type) {
      case 'select':
        return (
          <FormControl fullWidth size="small">
            <InputLabel>{field.label}</InputLabel>
            <Select
              value={value}
              label={field.label}
              onChange={(e) => handleChange(field.name, e.target.value)}
              disabled={isReadOnly}
            >
              {field.options?.map((option) => (
                <MenuItem key={option.value} value={option.value}>
                  {option.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        );

      case 'boolean':
        return (
          <FormControlLabel
            control={
              <Switch
                checked={Boolean(value)}
                onChange={(e) => handleChange(field.name, e.target.checked)}
                disabled={isReadOnly}
                size="small"
              />
            }
            label={field.label}
          />
        );

      case 'textarea':
        return (
          <TextField
            fullWidth
            label={field.label}
            value={value}
            onChange={(e) => handleChange(field.name, e.target.value)}
            multiline
            rows={field.rows || 2}
            size="small"
            InputProps={{ readOnly: isReadOnly }}
            variant={isReadOnly ? "filled" : "outlined"}
          />
        );

      case 'number':
        return (
          <TextField
            fullWidth
            type="number"
            label={field.label}
            value={value}
            onChange={(e) => handleChange(field.name, Number(e.target.value))}
            size="small"
            InputProps={{ readOnly: isReadOnly }}
            variant={isReadOnly ? "filled" : "outlined"}
          />
        );

      case 'array':
        return (
          <Box>
            <Typography variant="caption" color="textSecondary">
              {field.label}
            </Typography>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mt: 0.5 }}>
              {Array.isArray(value) && value.length > 0 ? (
                value.map((item: any, index: number) => (
                  <Chip
                    key={index}
                    label={typeof item === 'object' ? JSON.stringify(item) : item}
                    size="small"
                    variant="outlined"
                  />
                ))
              ) : (
                <Typography variant="body2" color="textSecondary">
                  No items
                </Typography>
              )}
            </Box>
          </Box>
        );

      case 'object':
        return (
          <Box>
            <Typography variant="caption" color="textSecondary">
              {field.label}
            </Typography>
            <Box sx={{ mt: 0.5 }}>
              {value && Object.keys(value).length > 0 ? (
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                  {Object.entries(value).map(([key, val]) => (
                    <Chip
                      key={key}
                      label={`${key}: ${val}`}
                      size="small"
                      variant="outlined"
                    />
                  ))}
                </Box>
              ) : (
                <Typography variant="body2" color="textSecondary">
                  No data
                </Typography>
              )}
            </Box>
          </Box>
        );

      default:
        return (
          <TextField
            fullWidth
            label={field.label}
            value={value}
            onChange={(e) => handleChange(field.name, e.target.value)}
            size="small"
            InputProps={{ readOnly: isReadOnly }}
            variant={isReadOnly ? "filled" : "outlined"}
          />
        );
    }
  };

  const renderFieldGroup = (group: FieldGroup, index: number) => {
    if (group.collapsible) {
      return (
        <Accordion 
          key={index} 
          defaultExpanded={group.defaultExpanded !== false}
          sx={{ mb: 1 }}
        >
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Typography variant="subtitle2">{group.title}</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1.5 }}>
              {group.fields.map((field) => (
                <Box 
                  key={field.name}
                  sx={{ 
                    flex: field.type === 'textarea' || field.type === 'array' || field.type === 'object' ? '1 1 100%' : '1 1 calc(50% - 6px)',
                    minWidth: field.type === 'textarea' || field.type === 'array' || field.type === 'object' ? '100%' : '250px'
                  }}
                >
                  {renderField(field)}
                </Box>
              ))}
            </Box>
          </AccordionDetails>
        </Accordion>
      );
    }

    return (
      <Box key={index} sx={{ mb: 2 }}>
        <Typography variant="subtitle2" gutterBottom color="primary">
          {group.title}
        </Typography>
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1.5 }}>
          {group.fields.map((field) => (
            <Box 
              key={field.name}
              sx={{ 
                flex: field.type === 'textarea' || field.type === 'array' || field.type === 'object' ? '1 1 100%' : '1 1 calc(50% - 6px)',
                minWidth: field.type === 'textarea' || field.type === 'array' || field.type === 'object' ? '100%' : '250px'
              }}
            >
              {renderField(field)}
            </Box>
          ))}
        </Box>
      </Box>
    );
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Typography variant="h6">
            {mode === 'create' ? `Create New ${title}` : 
             mode === 'edit' ? `Edit ${title}` : 
             `View ${title}`}
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            {mode === 'view' && onSave && (
              <IconButton
                onClick={() => setEditMode(!editMode)}
                color="primary"
                size="small"
              >
                <EditIcon />
              </IconButton>
            )}
            <IconButton onClick={onClose} size="small">
              <CloseIcon />
            </IconButton>
          </Box>
        </Box>
      </DialogTitle>

      <DialogContent dividers sx={{ p: 0 }}>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs
            value={currentTab}
            onChange={(event, newValue) => setCurrentTab(newValue)}
            variant="scrollable"
            scrollButtons="auto"
            sx={{ px: 2 }}
          >
            {tabNames.map((tabName, index) => (
              <Tab key={tabName} label={tabName} id={`entity-tab-${index}`} />
            ))}
          </Tabs>
        </Box>

        {tabNames.map((tabName, index) => (
          <TabPanel key={tabName} value={currentTab} index={index}>
            <Box sx={{ maxHeight: '60vh', overflow: 'auto', px: 3, py: 2 }}>
              {fieldGroups[tabName].map((group, groupIndex) =>
                renderFieldGroup(group, groupIndex)
              )}
            </Box>
          </TabPanel>
        ))}

        {mode === 'view' && !editMode && formData.id && (
          <Box sx={{ p: 2, borderTop: 1, borderColor: 'divider', bgcolor: 'grey.50' }}>
            <Typography variant="caption" color="textSecondary" gutterBottom>
              Metadata
            </Typography>
            <Box sx={{ display: 'flex', gap: 3, flexWrap: 'wrap' }}>
              <Typography variant="body2">
                <strong>ID:</strong> {formData.id}
              </Typography>
              {formData.createdAt && (
                <Typography variant="body2">
                  <strong>Created:</strong> {new Date(formData.createdAt).toLocaleString()}
                </Typography>
              )}
              {formData.updatedAt && (
                <Typography variant="body2">
                  <strong>Updated:</strong> {new Date(formData.updatedAt).toLocaleString()}
                </Typography>
              )}
              {formData.provisionedBy && (
                <Typography variant="body2">
                  <strong>Provisioned By:</strong> {formData.provisionedBy}
                </Typography>
              )}
              {formData.provisionedAt && (
                <Typography variant="body2">
                  <strong>Provisioned:</strong> {new Date(formData.provisionedAt).toLocaleString()}
                </Typography>
              )}
            </Box>
          </Box>
        )}
      </DialogContent>

      <DialogActions>
        {editMode && onSave && (
          <>
            <Button 
              onClick={handleSave}
              variant="contained"
              startIcon={<SaveIcon />}
              disabled={saving || loading}
            >
              {saving ? 'Saving...' : 'Save'}
            </Button>
            {mode === 'view' && (
              <Button
                onClick={() => setEditMode(false)}
                startIcon={<CancelIcon />}
              >
                Cancel
              </Button>
            )}
          </>
        )}
        
        {mode !== 'create' && onDelete && !editMode && (
          <Button
            onClick={handleDelete}
            color="error"
            disabled={saving || loading}
          >
            {saving ? 'Deleting...' : 'Delete'}
          </Button>
        )}
        
        <Button onClick={onClose}>
          Close
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default TabbedEntityModal;