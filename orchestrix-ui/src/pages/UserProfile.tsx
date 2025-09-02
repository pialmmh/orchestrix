import React, { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useSelector } from 'react-redux';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  TextField,
  Avatar,
  Divider,
  Tab,
  Tabs,
  Switch,
  FormControlLabel,
  Alert,
} from '@mui/material';
import {
  Person,
  Email,
  Phone,
  Business,
  Lock,
  Notifications,
  Settings,
} from '@mui/icons-material';
import { RootState } from '../store/store';

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
      id={`profile-tabpanel-${index}`}
      aria-labelledby={`profile-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ py: 3 }}>{children}</Box>}
    </div>
  );
}

const UserProfile: React.FC = () => {
  const { id } = useParams();
  const { user } = useSelector((state: RootState) => state.auth);
  const [tabValue, setTabValue] = useState(0);
  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [showSuccess, setShowSuccess] = useState(false);

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  const handlePasswordChange = () => {
    // Handle password change
    setShowSuccess(true);
    setTimeout(() => setShowSuccess(false), 3000);
  };

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        User Profile
      </Typography>

      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 3 }}>
        {/* Profile Summary */}
        <Box sx={{ flex: { xs: '0 0 100%', md: '0 0 calc(33.33% - 16px)' } }}>
          <Card>
            <CardContent sx={{ textAlign: 'center', py: 4 }}>
              <Avatar
                sx={{
                  width: 120,
                  height: 120,
                  margin: '0 auto',
                  mb: 2,
                  fontSize: '3rem',
                }}
              >
                {user?.firstName?.[0] || user?.username?.[0]?.toUpperCase()}
              </Avatar>
              <Typography variant="h5" gutterBottom>
                {user?.fullName || user?.username}
              </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                @{user?.username}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {user?.role?.replace('_', ' ')}
              </Typography>
              <Divider sx={{ my: 2 }} />
              <Box sx={{ textAlign: 'left' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                  <Email sx={{ mr: 1, fontSize: 20, color: 'text.secondary' }} />
                  <Typography variant="body2">{user?.email}</Typography>
                </Box>
                {user?.department && (
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                    <Business sx={{ mr: 1, fontSize: 20, color: 'text.secondary' }} />
                    <Typography variant="body2">{user.department}</Typography>
                  </Box>
                )}
              </Box>
            </CardContent>
          </Card>
        </Box>

        {/* Profile Details */}
        <Box sx={{ flex: { xs: '0 0 100%', md: '0 0 calc(66.66% - 16px)' } }}>
          <Card>
            <CardContent>
              <Tabs value={tabValue} onChange={handleTabChange}>
                <Tab icon={<Person />} label="Profile" />
                <Tab icon={<Lock />} label="Security" />
                <Tab icon={<Notifications />} label="Notifications" />
                <Tab icon={<Settings />} label="Preferences" />
              </Tabs>

              <TabPanel value={tabValue} index={0}>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2 }}>
                  <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 8px)' } }}>
                    <TextField
                      fullWidth
                      label="First Name"
                      defaultValue={user?.firstName}
                    />
                  </Box>
                  <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 8px)' } }}>
                    <TextField
                      fullWidth
                      label="Last Name"
                      defaultValue={user?.lastName}
                    />
                  </Box>
                  <Box sx={{ flex: '0 0 100%' }}>
                    <TextField
                      fullWidth
                      label="Email"
                      type="email"
                      defaultValue={user?.email}
                    />
                  </Box>
                  <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 8px)' } }}>
                    <TextField
                      fullWidth
                      label="Department"
                      defaultValue={user?.department}
                    />
                  </Box>
                  <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 8px)' } }}>
                    <TextField
                      fullWidth
                      label="Job Title"
                      defaultValue={user?.jobTitle}
                    />
                  </Box>
                  <Box sx={{ flex: '0 0 100%' }}>
                    <Button variant="contained" sx={{ mt: 2 }}>
                      Update Profile
                    </Button>
                  </Box>
                </Box>
              </TabPanel>

              <TabPanel value={tabValue} index={1}>
                {showSuccess && (
                  <Alert severity="success" sx={{ mb: 2 }}>
                    Password changed successfully!
                  </Alert>
                )}
                <Typography variant="h6" gutterBottom>
                  Change Password
                </Typography>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2 }}>
                  <Box sx={{ flex: '0 0 100%' }}>
                    <TextField
                      fullWidth
                      label="Current Password"
                      type="password"
                      value={passwordData.currentPassword}
                      onChange={(e) =>
                        setPasswordData({
                          ...passwordData,
                          currentPassword: e.target.value,
                        })
                      }
                    />
                  </Box>
                  <Box sx={{ flex: '0 0 100%' }}>
                    <TextField
                      fullWidth
                      label="New Password"
                      type="password"
                      value={passwordData.newPassword}
                      onChange={(e) =>
                        setPasswordData({
                          ...passwordData,
                          newPassword: e.target.value,
                        })
                      }
                    />
                  </Box>
                  <Box sx={{ flex: '0 0 100%' }}>
                    <TextField
                      fullWidth
                      label="Confirm New Password"
                      type="password"
                      value={passwordData.confirmPassword}
                      onChange={(e) =>
                        setPasswordData({
                          ...passwordData,
                          confirmPassword: e.target.value,
                        })
                      }
                    />
                  </Box>
                  <Box sx={{ flex: '0 0 100%' }}>
                    <Button
                      variant="contained"
                      onClick={handlePasswordChange}
                      sx={{ mt: 2 }}
                    >
                      Change Password
                    </Button>
                  </Box>
                </Box>
              </TabPanel>

              <TabPanel value={tabValue} index={2}>
                <Typography variant="h6" gutterBottom>
                  Notification Preferences
                </Typography>
                <FormControlLabel
                  control={<Switch defaultChecked />}
                  label="Email Notifications"
                  sx={{ display: 'block', mb: 2 }}
                />
                <FormControlLabel
                  control={<Switch />}
                  label="SMS Notifications"
                  sx={{ display: 'block', mb: 2 }}
                />
                <FormControlLabel
                  control={<Switch defaultChecked />}
                  label="Push Notifications"
                  sx={{ display: 'block', mb: 2 }}
                />
                <FormControlLabel
                  control={<Switch defaultChecked />}
                  label="Security Alerts"
                  sx={{ display: 'block', mb: 2 }}
                />
                <Button variant="contained" sx={{ mt: 2 }}>
                  Save Preferences
                </Button>
              </TabPanel>

              <TabPanel value={tabValue} index={3}>
                <Typography variant="h6" gutterBottom>
                  Application Preferences
                </Typography>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2 }}>
                  <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 8px)' } }}>
                    <TextField
                      fullWidth
                      select
                      label="Theme"
                      defaultValue={user?.themePreference || 'light'}
                      SelectProps={{
                        native: true,
                      }}
                    >
                      <option value="light">Light</option>
                      <option value="dark">Dark</option>
                      <option value="auto">Auto</option>
                    </TextField>
                  </Box>
                  <Box sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 8px)' } }}>
                    <TextField
                      fullWidth
                      select
                      label="Language"
                      defaultValue={user?.language || 'en'}
                      SelectProps={{
                        native: true,
                      }}
                    >
                      <option value="en">English</option>
                      <option value="es">Spanish</option>
                      <option value="fr">French</option>
                    </TextField>
                  </Box>
                  <Box sx={{ flex: '0 0 100%' }}>
                    <TextField
                      fullWidth
                      select
                      label="Timezone"
                      defaultValue="UTC"
                      SelectProps={{
                        native: true,
                      }}
                    >
                      <option value="UTC">UTC</option>
                      <option value="America/New_York">Eastern Time</option>
                      <option value="America/Chicago">Central Time</option>
                      <option value="America/Los_Angeles">Pacific Time</option>
                    </TextField>
                  </Box>
                  <Box sx={{ flex: '0 0 100%' }}>
                    <Button variant="contained" sx={{ mt: 2 }}>
                      Save Preferences
                    </Button>
                  </Box>
                </Box>
              </TabPanel>
            </CardContent>
          </Card>
        </Box>
      </Box>
    </Box>
  );
};

export default UserProfile;