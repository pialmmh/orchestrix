import React, { useState } from 'react';
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
  Collapse,
} from '@mui/material';
import {
  Menu as MenuIcon,
  ChevronLeft,
  Dashboard,
  People,
  Settings,
  CloudQueue,
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
} from '@mui/icons-material';
import { AppDispatch, RootState } from '../store/store';
import { logout } from '../store/slices/authSlice';

const drawerWidth = 260;

const Layout: React.FC = () => {
  const theme = useTheme();
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useDispatch<AppDispatch>();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
  
  const { user } = useSelector((state: RootState) => state.auth);
  
  const [open, setOpen] = useState(!isMobile);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [resourcesOpen, setResourcesOpen] = useState(false);

  const handleDrawerToggle = () => {
    setOpen(!open);
  };

  const handleProfileMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleProfileMenuClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = () => {
    dispatch(logout());
    navigate('/login');
  };

  const menuItems = [
    { text: 'Dashboard', icon: <Dashboard />, path: '/dashboard' },
    { text: 'Users', icon: <People />, path: '/users' },
    { text: 'Clients', icon: <BusinessCenter />, path: '/clients' },
    { text: 'Partners', icon: <Public />, path: '/partners' },
    { text: 'Deployments', icon: <DeveloperBoard />, path: '/deployments' },
    { text: 'Servers', icon: <Storage />, path: '/servers' },
    { text: 'Security', icon: <Security />, path: '/security' },
    { text: 'Reports', icon: <Assessment />, path: '/reports' },
    { text: 'Settings', icon: <Settings />, path: '/settings' },
  ];

  const handleResourcesClick = () => {
    setResourcesOpen(!resourcesOpen);
  };

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar
        position="fixed"
        sx={{
          width: { sm: `calc(100% - ${open ? drawerWidth : 0}px)` },
          ml: { sm: `${open ? drawerWidth : 0}px` },
          transition: theme.transitions.create(['margin', 'width'], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
          }),
          background: `linear-gradient(135deg, ${theme.palette.primary.main}, ${theme.palette.primary.dark})`,
          boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
        }}
      >
        <Toolbar>
          <IconButton
            color="inherit"
            aria-label="open drawer"
            onClick={handleDrawerToggle}
            edge="start"
            sx={{ mr: 2 }}
          >
            <MenuIcon />
          </IconButton>
          
          <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
            Orchestrix
          </Typography>

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Tooltip title="Notifications">
              <IconButton color="inherit">
                <Badge badgeContent={4} color="error">
                  <Notifications />
                </Badge>
              </IconButton>
            </Tooltip>
            
            <Tooltip title="Profile">
              <IconButton
                onClick={handleProfileMenuOpen}
                size="small"
                sx={{ ml: 2 }}
                aria-controls={Boolean(anchorEl) ? 'profile-menu' : undefined}
                aria-haspopup="true"
                aria-expanded={Boolean(anchorEl) ? 'true' : undefined}
              >
                <Avatar
                  sx={{
                    width: 32,
                    height: 32,
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

      <Menu
        id="profile-menu"
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
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

      <Drawer
        sx={{
          width: drawerWidth,
          flexShrink: 0,
          '& .MuiDrawer-paper': {
            width: drawerWidth,
            boxSizing: 'border-box',
            background: 'linear-gradient(180deg, #1a237e 0%, #283593 100%)',
            color: 'white',
            borderRight: 'none',
            boxShadow: '2px 0 10px rgba(0,0,0,0.1)',
          },
        }}
        variant={isMobile ? 'temporary' : 'persistent'}
        anchor="left"
        open={open}
        onClose={handleDrawerToggle}
      >
        <Toolbar
          sx={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            px: [1],
            background: alpha('#000', 0.1),
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <CloudQueue sx={{ mr: 1, fontSize: 28 }} />
            <Typography variant="h6" noWrap>
              Orchestrix
            </Typography>
          </Box>
          <IconButton onClick={handleDrawerToggle} sx={{ color: 'white' }}>
            <ChevronLeft />
          </IconButton>
        </Toolbar>
        
        <Divider sx={{ borderColor: alpha('#fff', 0.1) }} />
        
        <Box sx={{ p: 2, textAlign: 'center' }}>
          <Avatar
            sx={{
              width: 80,
              height: 80,
              margin: '0 auto',
              bgcolor: theme.palette.secondary.main,
              fontSize: '2rem',
            }}
          >
            {user?.firstName?.[0] || user?.username?.[0]?.toUpperCase()}
          </Avatar>
          <Typography variant="h6" sx={{ mt: 1 }}>
            {user?.fullName || user?.username}
          </Typography>
          <Typography variant="caption" sx={{ opacity: 0.8 }}>
            {user?.role?.replace('_', ' ')}
          </Typography>
        </Box>
        
        <Divider sx={{ borderColor: alpha('#fff', 0.1) }} />
        
        <List sx={{ px: 1 }}>
          {/* Dashboard */}
          <ListItem disablePadding sx={{ mb: 0.5 }}>
            <ListItemButton
              onClick={() => navigate('/dashboard')}
              selected={location.pathname === '/dashboard'}
              sx={{
                borderRadius: 1,
                '&.Mui-selected': {
                  backgroundColor: alpha('#fff', 0.15),
                  '&:hover': {
                    backgroundColor: alpha('#fff', 0.2),
                  },
                },
                '&:hover': {
                  backgroundColor: alpha('#fff', 0.1),
                },
              }}
            >
              <ListItemIcon sx={{ color: 'white', minWidth: 40 }}>
                <Dashboard />
              </ListItemIcon>
              <ListItemText primary="Dashboard" />
            </ListItemButton>
          </ListItem>

          {/* Users */}
          <ListItem disablePadding sx={{ mb: 0.5 }}>
            <ListItemButton
              onClick={() => navigate('/users')}
              selected={location.pathname === '/users'}
              sx={{
                borderRadius: 1,
                '&.Mui-selected': {
                  backgroundColor: alpha('#fff', 0.15),
                  '&:hover': {
                    backgroundColor: alpha('#fff', 0.2),
                  },
                },
                '&:hover': {
                  backgroundColor: alpha('#fff', 0.1),
                },
              }}
            >
              <ListItemIcon sx={{ color: 'white', minWidth: 40 }}>
                <People />
              </ListItemIcon>
              <ListItemText primary="Users" />
            </ListItemButton>
          </ListItem>

          {/* Resources with submenu */}
          <ListItem disablePadding sx={{ mb: 0.5 }}>
            <ListItemButton
              onClick={handleResourcesClick}
              sx={{
                borderRadius: 1,
                '&:hover': {
                  backgroundColor: alpha('#fff', 0.1),
                },
              }}
            >
              <ListItemIcon sx={{ color: 'white', minWidth: 40 }}>
                <Folder />
              </ListItemIcon>
              <ListItemText primary="Resources" />
              {resourcesOpen ? <ExpandLess sx={{ color: 'white' }} /> : <ExpandMore sx={{ color: 'white' }} />}
            </ListItemButton>
          </ListItem>
          <Collapse in={resourcesOpen} timeout="auto" unmountOnExit>
            <List component="div" disablePadding>
              <ListItem disablePadding sx={{ mb: 0.5, pl: 2 }}>
                <ListItemButton
                  onClick={() => navigate('/resources/countries')}
                  selected={location.pathname === '/resources/countries'}
                  sx={{
                    borderRadius: 1,
                    '&.Mui-selected': {
                      backgroundColor: alpha('#fff', 0.15),
                      '&:hover': {
                        backgroundColor: alpha('#fff', 0.2),
                      },
                    },
                    '&:hover': {
                      backgroundColor: alpha('#fff', 0.1),
                    },
                  }}
                >
                  <ListItemIcon sx={{ color: 'white', minWidth: 40 }}>
                    <Public />
                  </ListItemIcon>
                  <ListItemText primary="Countries" />
                </ListItemButton>
              </ListItem>
              <ListItem disablePadding sx={{ mb: 0.5, pl: 2 }}>
                <ListItemButton
                  onClick={() => navigate('/resources/datacenters')}
                  selected={location.pathname === '/resources/datacenters'}
                  sx={{
                    borderRadius: 1,
                    '&.Mui-selected': {
                      backgroundColor: alpha('#fff', 0.15),
                      '&:hover': {
                        backgroundColor: alpha('#fff', 0.2),
                      },
                    },
                    '&:hover': {
                      backgroundColor: alpha('#fff', 0.1),
                    },
                  }}
                >
                  <ListItemIcon sx={{ color: 'white', minWidth: 40 }}>
                    <Dns />
                  </ListItemIcon>
                  <ListItemText primary="Datacenters" />
                </ListItemButton>
              </ListItem>
              <ListItem disablePadding sx={{ mb: 0.5, pl: 2 }}>
                <ListItemButton
                  onClick={() => navigate('/resources/compute')}
                  selected={location.pathname === '/resources/compute'}
                  sx={{
                    borderRadius: 1,
                    '&.Mui-selected': {
                      backgroundColor: alpha('#fff', 0.15),
                      '&:hover': {
                        backgroundColor: alpha('#fff', 0.2),
                      },
                    },
                    '&:hover': {
                      backgroundColor: alpha('#fff', 0.1),
                    },
                  }}
                >
                  <ListItemIcon sx={{ color: 'white', minWidth: 40 }}>
                    <Computer />
                  </ListItemIcon>
                  <ListItemText primary="Compute" />
                </ListItemButton>
              </ListItem>
              <ListItem disablePadding sx={{ mb: 0.5, pl: 2 }}>
                <ListItemButton
                  onClick={() => navigate('/resources/storage')}
                  selected={location.pathname === '/resources/storage'}
                  sx={{
                    borderRadius: 1,
                    '&.Mui-selected': {
                      backgroundColor: alpha('#fff', 0.15),
                      '&:hover': {
                        backgroundColor: alpha('#fff', 0.2),
                      },
                    },
                    '&:hover': {
                      backgroundColor: alpha('#fff', 0.1),
                    },
                  }}
                >
                  <ListItemIcon sx={{ color: 'white', minWidth: 40 }}>
                    <Storage />
                  </ListItemIcon>
                  <ListItemText primary="Storage" />
                </ListItemButton>
              </ListItem>
            </List>
          </Collapse>

          {/* Rest of the menu items */}
          <ListItem disablePadding sx={{ mb: 0.5 }}>
            <ListItemButton
              onClick={() => navigate('/clients')}
              selected={location.pathname === '/clients'}
              sx={{
                borderRadius: 1,
                '&.Mui-selected': {
                  backgroundColor: alpha('#fff', 0.15),
                  '&:hover': {
                    backgroundColor: alpha('#fff', 0.2),
                  },
                },
                '&:hover': {
                  backgroundColor: alpha('#fff', 0.1),
                },
              }}
            >
              <ListItemIcon sx={{ color: 'white', minWidth: 40 }}>
                <BusinessCenter />
              </ListItemIcon>
              <ListItemText primary="Clients" />
            </ListItemButton>
          </ListItem>

          <ListItem disablePadding sx={{ mb: 0.5 }}>
            <ListItemButton
              onClick={() => navigate('/partners')}
              selected={location.pathname === '/partners'}
              sx={{
                borderRadius: 1,
                '&.Mui-selected': {
                  backgroundColor: alpha('#fff', 0.15),
                  '&:hover': {
                    backgroundColor: alpha('#fff', 0.2),
                  },
                },
                '&:hover': {
                  backgroundColor: alpha('#fff', 0.1),
                },
              }}
            >
              <ListItemIcon sx={{ color: 'white', minWidth: 40 }}>
                <Public />
              </ListItemIcon>
              <ListItemText primary="Partners" />
            </ListItemButton>
          </ListItem>

          <ListItem disablePadding sx={{ mb: 0.5 }}>
            <ListItemButton
              onClick={() => navigate('/deployments')}
              selected={location.pathname === '/deployments'}
              sx={{
                borderRadius: 1,
                '&.Mui-selected': {
                  backgroundColor: alpha('#fff', 0.15),
                  '&:hover': {
                    backgroundColor: alpha('#fff', 0.2),
                  },
                },
                '&:hover': {
                  backgroundColor: alpha('#fff', 0.1),
                },
              }}
            >
              <ListItemIcon sx={{ color: 'white', minWidth: 40 }}>
                <DeveloperBoard />
              </ListItemIcon>
              <ListItemText primary="Deployments" />
            </ListItemButton>
          </ListItem>

          <ListItem disablePadding sx={{ mb: 0.5 }}>
            <ListItemButton
              onClick={() => navigate('/servers')}
              selected={location.pathname === '/servers'}
              sx={{
                borderRadius: 1,
                '&.Mui-selected': {
                  backgroundColor: alpha('#fff', 0.15),
                  '&:hover': {
                    backgroundColor: alpha('#fff', 0.2),
                  },
                },
                '&:hover': {
                  backgroundColor: alpha('#fff', 0.1),
                },
              }}
            >
              <ListItemIcon sx={{ color: 'white', minWidth: 40 }}>
                <Storage />
              </ListItemIcon>
              <ListItemText primary="Servers" />
            </ListItemButton>
          </ListItem>

          <ListItem disablePadding sx={{ mb: 0.5 }}>
            <ListItemButton
              onClick={() => navigate('/security')}
              selected={location.pathname === '/security'}
              sx={{
                borderRadius: 1,
                '&.Mui-selected': {
                  backgroundColor: alpha('#fff', 0.15),
                  '&:hover': {
                    backgroundColor: alpha('#fff', 0.2),
                  },
                },
                '&:hover': {
                  backgroundColor: alpha('#fff', 0.1),
                },
              }}
            >
              <ListItemIcon sx={{ color: 'white', minWidth: 40 }}>
                <Security />
              </ListItemIcon>
              <ListItemText primary="Security" />
            </ListItemButton>
          </ListItem>

          <ListItem disablePadding sx={{ mb: 0.5 }}>
            <ListItemButton
              onClick={() => navigate('/reports')}
              selected={location.pathname === '/reports'}
              sx={{
                borderRadius: 1,
                '&.Mui-selected': {
                  backgroundColor: alpha('#fff', 0.15),
                  '&:hover': {
                    backgroundColor: alpha('#fff', 0.2),
                  },
                },
                '&:hover': {
                  backgroundColor: alpha('#fff', 0.1),
                },
              }}
            >
              <ListItemIcon sx={{ color: 'white', minWidth: 40 }}>
                <Assessment />
              </ListItemIcon>
              <ListItemText primary="Reports" />
            </ListItemButton>
          </ListItem>

          <ListItem disablePadding sx={{ mb: 0.5 }}>
            <ListItemButton
              onClick={() => navigate('/settings')}
              selected={location.pathname === '/settings'}
              sx={{
                borderRadius: 1,
                '&.Mui-selected': {
                  backgroundColor: alpha('#fff', 0.15),
                  '&:hover': {
                    backgroundColor: alpha('#fff', 0.2),
                  },
                },
                '&:hover': {
                  backgroundColor: alpha('#fff', 0.1),
                },
              }}
            >
              <ListItemIcon sx={{ color: 'white', minWidth: 40 }}>
                <Settings />
              </ListItemIcon>
              <ListItemText primary="Settings" />
            </ListItemButton>
          </ListItem>
        </List>
      </Drawer>

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: { sm: `calc(100% - ${open ? drawerWidth : 0}px)` },
          ml: { sm: `${open ? 0 : 0}px` },
          transition: theme.transitions.create(['margin', 'width'], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
          }),
          backgroundColor: theme.palette.background.default,
          minHeight: '100vh',
        }}
      >
        <Toolbar />
        <Outlet />
      </Box>
    </Box>
  );
};

export default Layout;