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
  Divider
} from '@mui/material';
import {
  Close as CloseIcon,
  Edit as EditIcon,
  Save as SaveIcon,
  Cancel as CancelIcon
} from '@mui/icons-material';

interface FormField {
  name: string;
  label: string;
  type: 'text' | 'number' | 'select' | 'boolean' | 'textarea';
  required?: boolean;
  readOnly?: boolean;
  options?: { label: string; value: any }[];
  multiline?: boolean;
  rows?: number;
}

interface EntityModalProps {
  open: boolean;
  onClose: () => void;
  title: string;
  data: any;
  fields: FormField[];
  mode: 'view' | 'edit' | 'create';
  onSave?: (data: any) => Promise<void>;
  onDelete?: (data: any) => Promise<void>;
  loading?: boolean;
}

const EntityModal: React.FC<EntityModalProps> = ({
  open,
  onClose,
  title,
  data,
  fields,
  mode,
  onSave,
  onDelete,
  loading = false
}) => {
  const [formData, setFormData] = useState<any>({});
  const [editMode, setEditMode] = useState(mode === 'edit' || mode === 'create');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (data) {
      setFormData({ ...data });
    } else {
      // Initialize with default values for create mode
      const defaultData: any = {};
      fields.forEach(field => {
        if (field.type === 'boolean') {
          defaultData[field.name] = false;
        } else if (field.type === 'number') {
          defaultData[field.name] = 0;
        } else {
          defaultData[field.name] = '';
        }
      });
      setFormData(defaultData);
    }
    setEditMode(mode === 'edit' || mode === 'create');
  }, [data, fields, mode]);

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
            rows={field.rows || 3}
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

      <DialogContent dividers>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
          {fields.map((field) => (
            <Box 
              key={field.name}
              sx={{ 
                display: field.type === 'textarea' ? 'block' : 'inline-block',
                width: field.type === 'textarea' ? '100%' : 'calc(50% - 8px)',
                marginRight: field.type === 'textarea' ? 0 : '16px',
                marginBottom: '16px'
              }}
            >
              {renderField(field)}
            </Box>
          ))}
        </Box>

        {mode === 'view' && !editMode && (
          <>
            <Divider sx={{ my: 3 }} />
            <Box>
              <Typography variant="subtitle2" color="textSecondary" gutterBottom>
                Metadata
              </Typography>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2 }}>
                {formData.createdAt && (
                  <Box sx={{ width: '48%' }}>
                    <Typography variant="body2">
                      <strong>Created:</strong> {new Date(formData.createdAt).toLocaleString()}
                    </Typography>
                  </Box>
                )}
                {formData.updatedAt && (
                  <Box sx={{ width: '48%' }}>
                    <Typography variant="body2">
                      <strong>Updated:</strong> {new Date(formData.updatedAt).toLocaleString()}
                    </Typography>
                  </Box>
                )}
                {formData.id && (
                  <Box sx={{ width: '48%' }}>
                    <Typography variant="body2">
                      <strong>ID:</strong> {formData.id}
                    </Typography>
                  </Box>
                )}
              </Box>
            </Box>
          </>
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

export default EntityModal;