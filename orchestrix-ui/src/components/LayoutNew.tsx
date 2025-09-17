import React, { useState, useEffect } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import {
  Box,
  Drawer,
  AppBar,
  Toolbar,
  List,
  Typography,
  Divider,
  IconButton,
  ListItem,
  ListItemIcon,
  ListItemText,
  ListItemButton,
  Avatar,
  Menu,
  MenuItem,
  useTheme,
  useMediaQuery,
  Badge,
  Tooltip,
  alpha,
  Button,
  Chip,
  Paper,
} from '@mui/material';
import {
  Menu as MenuIcon,
  ChevronLeft,
  Dashboard,
  People,
  Settings,
  Storage,
  Security,
  Assessment,
  Notifications,
  AccountCircle,
  Logout,
  BusinessCenter,
  Public,
  DeveloperBoard,
  ExpandLess,
  ExpandMore,
  Computer,
  Dns,
  Folder,
  Cloud,
  VpnKey,
  Terminal,
  BugReport,
  KeyboardArrowDown,
  FilterList,
  CheckCircle,
} from '@mui/icons-material';
import { AppDispatch, RootState } from '../store/store';
import { logout } from '../store/slices/authSlice';
import OrchestrixLogo from './OrchestrixLogo';
import { getStoreDebugConfig } from '../config/storeDebugConfig';
import { observer } from 'mobx-react-lite';
import { useStores } from '../stores/base/useStores';

const drawerWidth = 240;

interface MenuCategory {
  label: string;
  items: {
    label: string;
    path: string;
    icon: React.ReactNode;
  }[];
}

const menuCategories: MenuCategory[] = [
  {
    label: 'Infrastructure',
    items: [
      { label: 'Organization', path: '/infrastructure', icon: <Cloud /> },
      { label: 'Partners', path: '/partners', icon: <Public /> },
      { label: 'Clients', path: '/clients', icon: <BusinessCenter /> },
      { label: 'Datacenters', path: '/resources/datacenters', icon: <Dns /> },
      { label: 'Servers', path: '/servers', icon: <Storage /> },
    ],
  },
  {
    label: 'Resources',
    items: [
      { label: 'Countries', path: '/resources/countries', icon: <Public /> },
      { label: 'Storage', path: '/resources/storage', icon: <Storage /> },
      { label: 'Deployments', path: '/deployments', icon: <DeveloperBoard /> },
    ],
  },
  {
    label: 'Administration',
    items: [
      { label: 'Users', path: '/users', icon: <People /> },
      { label: 'Security', path: '/security', icon: <Security /> },
      { label: 'Secret Providers', path: '/secret-providers', icon: <VpnKey /> },
      { label: 'Remote Access', path: '/remote-access', icon: <VpnKey /> },
    ],
  },
  {
    label: 'Analytics',
    items: [
      { label: 'Dashboard', path: '/dashboard', icon: <Dashboard /> },
      { label: 'Reports', path: '/reports', icon: <Assessment /> },
    ],
  },
  {
    label: 'Developer',
    items: [
      { label: 'WebSocket Test', path: '/websocket-test', icon: <Terminal /> },
      { label: 'Settings', path: '/settings', icon: <Settings /> },
    ],
  },
];

// Environment filter options
const environments = [
  { id: 'all', label: 'All Environments', color: '#9e9e9e' },
  { id: 'production', label: 'Production', color: '#f44336' },
  { id: 'staging', label: 'Staging', color: '#ff9800' },
  { id: 'development', label: 'Development', color: '#4caf50' },
  { id: 'testing', label: 'Testing', color: '#2196f3' },
];

const LayoutNew: React.FC = observer(() => {
  const theme = useTheme();
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useDispatch<AppDispatch>();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
  const { organizationInfraStore } = useStores();

  const { user } = useSelector((state: RootState) => state.auth);

  const [sidebarOpen, setSidebarOpen] = useState(!isMobile);
  const [profileAnchor, setProfileAnchor] = useState<null | HTMLElement>(null);
  const [categoryAnchors, setCategoryAnchors] = useState<{ [key: string]: HTMLElement | null }>({});
  const [selectedEnvironment, setSelectedEnvironment] = useState('all');
  const [storeDebugMode, setStoreDebugMode] = useState(false);

  useEffect(() => {
    const config = getStoreDebugConfig();
    setStoreDebugMode(config.store_debug);
  }, []);

  const handleSidebarToggle = () => {
    setSidebarOpen(!sidebarOpen);
  };

  const handleProfileMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setProfileAnchor(event.currentTarget);
  };

  const handleProfileMenuClose = () => {
    setProfileAnchor(null);
  };

  const handleCategoryMenuOpen = (event: React.MouseEvent<HTMLElement>, category: string) => {
    setCategoryAnchors({ ...categoryAnchors, [category]: event.currentTarget });
  };

  const handleCategoryMenuClose = (category: string) => {
    setCategoryAnchors({ ...categoryAnchors, [category]: null });
  };

  const handleMenuItemClick = (path: string, category: string) => {
    navigate(path);
    handleCategoryMenuClose(category);
  };

  const handleLogout = () => {
    dispatch(logout());
    navigate('/login');
  };

  const handleEnvironmentSelect = (envId: string) => {
    setSelectedEnvironment(envId);
    // Set the environment filter in the store
    if (envId === 'all') {
      organizationInfraStore.setEnvironmentFilter(null);
    } else if (envId === 'production') {
      organizationInfraStore.setEnvironmentFilter('PRODUCTION');
    } else if (envId === 'staging') {
      organizationInfraStore.setEnvironmentFilter('STAGING');
    } else if (envId === 'development') {
      organizationInfraStore.setEnvironmentFilter('DEVELOPMENT');
    } else if (envId === 'testing') {
      organizationInfraStore.setEnvironmentFilter('TESTING');
    }
  };

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar
        position="fixed"
        sx={{
          zIndex: theme.zIndex.drawer + 1,
          background: `linear-gradient(135deg, ${theme.palette.primary.main}, ${theme.palette.primary.dark})`,
          boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
        }}
      >
        <Toolbar variant="dense" sx={{ minHeight: 48, height: 48 }}>
          {/* Sidebar Toggle */}
          <IconButton
            color="inherit"
            aria-label="toggle sidebar"
            onClick={handleSidebarToggle}
            edge="start"
            sx={{ mr: 1, p: 0.5 }}
          >
            <MenuIcon />
          </IconButton>

          {/* Logo and Brand */}
          <Box sx={{ display: 'flex', alignItems: 'center', mr: 3 }}>
            <OrchestrixLogo sx={{ mr: 0.5, fontSize: 24 }} />
            <Typography variant="subtitle1" noWrap component="div" sx={{ fontWeight: 'medium' }}>
              Orchestrix
            </Typography>
            {storeDebugMode && (
              <Chip
                icon={<BugReport sx={{ fontSize: 16 }} />}
                label="DEBUG"
                size="small"
                sx={{
                  ml: 1.5,
                  height: 22,
                  fontSize: '0.7rem',
                  backgroundColor: '#ff9800',
                  color: 'white',
                  fontWeight: 'bold',
                  '& .MuiChip-icon': {
                    color: 'white',
                    marginLeft: '4px',
                  },
                }}
              />
            )}
          </Box>

          {/* Category Menu Buttons */}
          <Box sx={{ flexGrow: 1, display: 'flex', gap: 1 }}>
            {menuCategories.map((category) => (
              <React.Fragment key={category.label}>
                <Button
                  color="inherit"
                  onClick={(e) => handleCategoryMenuOpen(e, category.label)}
                  endIcon={<KeyboardArrowDown />}
                  sx={{
                    textTransform: 'none',
                    fontSize: '0.875rem',
                    py: 0.5,
                    px: 1,
                    minHeight: 32,
                    '&:hover': {
                      backgroundColor: alpha(theme.palette.common.white, 0.1),
                    },
                  }}
                >
                  {category.label}
                </Button>
                <Menu
                  anchorEl={categoryAnchors[category.label]}
                  open={Boolean(categoryAnchors[category.label])}
                  onClose={() => handleCategoryMenuClose(category.label)}
                  MenuListProps={{
                    'aria-labelledby': `${category.label}-button`,
                  }}
                  transformOrigin={{ horizontal: 'left', vertical: 'top' }}
                  anchorOrigin={{ horizontal: 'left', vertical: 'bottom' }}
                  PaperProps={{
                    sx: {
                      mt: 0.5,
                      minWidth: 200,
                    },
                  }}
                >
                  {category.items.map((item) => (
                    <MenuItem
                      key={item.path}
                      onClick={() => handleMenuItemClick(item.path, category.label)}
                      selected={location.pathname === item.path}
                    >
                      <ListItemIcon>{item.icon}</ListItemIcon>
                      <ListItemText primary={item.label} />
                    </MenuItem>
                  ))}
                </Menu>
              </React.Fragment>
            ))}
          </Box>

          {/* Right Side Icons */}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Tooltip title="Notifications">
              <IconButton color="inherit" size="small">
                <Badge badgeContent={4} color="error">
                  <Notifications sx={{ fontSize: 20 }} />
                </Badge>
              </IconButton>
            </Tooltip>

            <Tooltip title="Profile">
              <IconButton
                onClick={handleProfileMenuOpen}
                size="small"
                sx={{ ml: 1 }}
                aria-controls={Boolean(profileAnchor) ? 'profile-menu' : undefined}
                aria-haspopup="true"
                aria-expanded={Boolean(profileAnchor) ? 'true' : undefined}
              >
                <Avatar
                  sx={{
                    width: 28,
                    height: 28,
                    bgcolor: theme.palette.secondary.main,
                  }}
                >
                  {user?.firstName?.[0] || user?.username?.[0]?.toUpperCase()}
                </Avatar>
              </IconButton>
            </Tooltip>
          </Box>
        </Toolbar>
      </AppBar>

      {/* Profile Menu */}
      <Menu
        id="profile-menu"
        anchorEl={profileAnchor}
        open={Boolean(profileAnchor)}
        onClose={handleProfileMenuClose}
        MenuListProps={{
          'aria-labelledby': 'profile-button',
        }}
        transformOrigin={{ horizontal: 'right', vertical: 'top' }}
        anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
      >
        <MenuItem onClick={() => { navigate('/profile'); handleProfileMenuClose(); }}>
          <ListItemIcon>
            <AccountCircle fontSize="small" />
          </ListItemIcon>
          My Profile
        </MenuItem>
        <MenuItem onClick={() => { navigate('/settings'); handleProfileMenuClose(); }}>
          <ListItemIcon>
            <Settings fontSize="small" />
          </ListItemIcon>
          Settings
        </MenuItem>
        <Divider />
        <MenuItem onClick={handleLogout}>
          <ListItemIcon>
            <Logout fontSize="small" />
          </ListItemIcon>
          Logout
        </MenuItem>
      </Menu>

      {/* Left Sidebar - Environment Filter */}
      <Drawer
        sx={{
          width: drawerWidth,
          flexShrink: 0,
          '& .MuiDrawer-paper': {
            width: drawerWidth,
            boxSizing: 'border-box',
            background: 'linear-gradient(180deg, #fafafa 0%, #f5f5f5 100%)',
            borderRight: '1px solid rgba(0, 0, 0, 0.08)',
            boxShadow: '2px 0 8px rgba(0,0,0,0.05)',
          },
        }}
        variant={isMobile ? 'temporary' : 'persistent'}
        anchor="left"
        open={sidebarOpen}
        onClose={handleSidebarToggle}
      >
        <Toolbar variant="dense" sx={{ minHeight: 48 }} />

        <Box sx={{ p: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
            <FilterList sx={{ mr: 1, color: 'text.secondary' }} />
            <Typography variant="subtitle1" fontWeight="medium">
              Environment Filter
            </Typography>
          </Box>

          <Divider sx={{ mb: 2 }} />

          <List sx={{ p: 0 }}>
            {environments.map((env) => (
              <ListItem key={env.id} disablePadding sx={{ mb: 1 }}>
                <ListItemButton
                  onClick={() => handleEnvironmentSelect(env.id)}
                  selected={selectedEnvironment === env.id}
                  sx={{
                    borderRadius: 1,
                    border: '1px solid',
                    borderColor: selectedEnvironment === env.id ? env.color : 'transparent',
                    backgroundColor: selectedEnvironment === env.id
                      ? alpha(env.color, 0.08)
                      : 'transparent',
                    '&:hover': {
                      backgroundColor: alpha(env.color, 0.12),
                      borderColor: env.color,
                    },
                    '&.Mui-selected': {
                      backgroundColor: alpha(env.color, 0.08),
                      '&:hover': {
                        backgroundColor: alpha(env.color, 0.16),
                      },
                    },
                  }}
                >
                  <ListItemIcon sx={{ minWidth: 36 }}>
                    <CheckCircle
                      sx={{
                        color: selectedEnvironment === env.id ? env.color : 'action.disabled',
                        fontSize: 20,
                      }}
                    />
                  </ListItemIcon>
                  <ListItemText
                    primary={env.label}
                    primaryTypographyProps={{
                      fontSize: '0.9rem',
                      fontWeight: selectedEnvironment === env.id ? 'medium' : 'regular',
                    }}
                  />
                </ListItemButton>
              </ListItem>
            ))}
          </List>

          <Divider sx={{ my: 2 }} />

          {/* Quick Stats */}
          <Paper
            elevation={0}
            sx={{
              p: 2,
              backgroundColor: 'background.default',
              border: '1px solid',
              borderColor: 'divider',
            }}
          >
            <Typography variant="caption" color="text.secondary" gutterBottom>
              ENVIRONMENT SUMMARY
            </Typography>
            <Box sx={{ mt: 1 }}>
              <Typography variant="body2" sx={{ mb: 0.5 }}>
                <strong>Production:</strong> 12 servers
              </Typography>
              <Typography variant="body2" sx={{ mb: 0.5 }}>
                <strong>Staging:</strong> 8 servers
              </Typography>
              <Typography variant="body2" sx={{ mb: 0.5 }}>
                <strong>Development:</strong> 15 servers
              </Typography>
              <Typography variant="body2">
                <strong>Testing:</strong> 5 servers
              </Typography>
            </Box>
          </Paper>

          {/* Additional Filters */}
          <Box sx={{ mt: 2 }}>
            <Typography variant="caption" color="text.secondary" gutterBottom>
              ADDITIONAL FILTERS
            </Typography>
            <List sx={{ p: 0, mt: 1 }}>
              <ListItem disablePadding>
                <ListItemButton sx={{ borderRadius: 1, py: 0.5 }}>
                  <ListItemText
                    primary="Active Resources Only"
                    primaryTypographyProps={{ fontSize: '0.85rem' }}
                  />
                </ListItemButton>
              </ListItem>
              <ListItem disablePadding>
                <ListItemButton sx={{ borderRadius: 1, py: 0.5 }}>
                  <ListItemText
                    primary="Show Shared Resources"
                    primaryTypographyProps={{ fontSize: '0.85rem' }}
                  />
                </ListItemButton>
              </ListItem>
            </List>
          </Box>
        </Box>
      </Drawer>

      {/* Main Content Area */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: { sm: `calc(100% - ${sidebarOpen ? drawerWidth : 0}px)` },
          ml: { sm: `${sidebarOpen ? 0 : 0}px` },
          transition: theme.transitions.create(['margin', 'width'], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
          }),
          backgroundColor: theme.palette.background.default,
          minHeight: '100vh',
        }}
      >
        <Toolbar variant="dense" sx={{ minHeight: 48 }} />
        <Outlet />
      </Box>
    </Box>
  );
});

export default LayoutNew;