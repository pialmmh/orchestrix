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
} from '@mui/icons-material';
import { AppDispatch, RootState } from '../store/store';
import { logout } from '../store/slices/authSlice';
import OrchestrixLogo from './OrchestrixLogo';

const drawerWidth = 290;

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


  const handleResourcesClick = () => {
    setResourcesOpen(!resourcesOpen);
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
          
          <Box sx={{ display: 'flex', alignItems: 'center', flexGrow: 1 }}>
            <OrchestrixLogo sx={{ mr: 1 }} fontSize="large" />
            <Typography variant="h6" noWrap component="div">
              Orchestrix
            </Typography>
          </Box>

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
            background: 'linear-gradient(180deg, #f5f5f5 0%, #e0e0e0 100%)',
            color: '#424242',
            borderRight: 'none',
            boxShadow: '2px 0 10px rgba(0,0,0,0.1)',
            display: 'flex',
            flexDirection: 'column',
          },
        }}
        variant={isMobile ? 'temporary' : 'persistent'}
        anchor="left"
        open={open}
        onClose={handleDrawerToggle}
      >
        {/* Add toolbar spacing */}
        <Toolbar />
        
        {/* Scrollable Content Section */}
        <Box sx={{ flexGrow: 1, overflow: 'auto' }}>
          <List sx={{ px: 1 }}>
          {/* Dashboard */}
          <ListItem disablePadding sx={{ mb: 0.5 }}>
            <ListItemButton
              onClick={() => navigate('/dashboard')}
              selected={location.pathname === '/dashboard'}
              sx={{
                borderRadius: 1,
                '&.Mui-selected': {
                  backgroundColor: alpha('#000', 0.08),
                  '&:hover': {
                    backgroundColor: alpha('#000', 0.12),
                  },
                },
                '&:hover': {
                  backgroundColor: alpha('#000', 0.05),
                },
              }}
            >
              <ListItemIcon sx={{ color: '#424242', minWidth: 40 }}>
                <Dashboard />
              </ListItemIcon>
              <ListItemText primary="Dashboard" />
            </ListItemButton>
          </ListItem>

          {/* Infrastructure */}
          <ListItem disablePadding sx={{ mb: 0.5 }}>
            <ListItemButton
              onClick={() => navigate('/infrastructure')}
              selected={location.pathname === '/infrastructure'}
              sx={{
                borderRadius: 1,
                '&.Mui-selected': {
                  backgroundColor: alpha('#000', 0.08),
                  '&:hover': {
                    backgroundColor: alpha('#000', 0.12),
                  },
                },
                '&:hover': {
                  backgroundColor: alpha('#000', 0.05),
                },
              }}
            >
              <ListItemIcon sx={{ color: '#424242', minWidth: 40 }}>
                <Cloud />
              </ListItemIcon>
              <ListItemText primary="Infrastructure" />
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
                  backgroundColor: alpha('#000', 0.08),
                  '&:hover': {
                    backgroundColor: alpha('#000', 0.12),
                  },
                },
                '&:hover': {
                  backgroundColor: alpha('#000', 0.05),
                },
              }}
            >
              <ListItemIcon sx={{ color: '#424242', minWidth: 40 }}>
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
                  backgroundColor: alpha('#000', 0.05),
                },
              }}
            >
              <ListItemIcon sx={{ color: '#424242', minWidth: 40 }}>
                <Folder />
              </ListItemIcon>
              <ListItemText primary="Resources" />
              {resourcesOpen ? <ExpandLess sx={{ color: '#424242' }} /> : <ExpandMore sx={{ color: '#424242' }} />}
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
                      backgroundColor: alpha('#000', 0.08),
                      '&:hover': {
                        backgroundColor: alpha('#000', 0.12),
                      },
                    },
                    '&:hover': {
                      backgroundColor: alpha('#000', 0.05),
                    },
                  }}
                >
                  <ListItemIcon sx={{ color: '#424242', minWidth: 40 }}>
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
                      backgroundColor: alpha('#000', 0.08),
                      '&:hover': {
                        backgroundColor: alpha('#000', 0.12),
                      },
                    },
                    '&:hover': {
                      backgroundColor: alpha('#000', 0.05),
                    },
                  }}
                >
                  <ListItemIcon sx={{ color: '#424242', minWidth: 40 }}>
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
                      backgroundColor: alpha('#000', 0.08),
                      '&:hover': {
                        backgroundColor: alpha('#000', 0.12),
                      },
                    },
                    '&:hover': {
                      backgroundColor: alpha('#000', 0.05),
                    },
                  }}
                >
                  <ListItemIcon sx={{ color: '#424242', minWidth: 40 }}>
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
                      backgroundColor: alpha('#000', 0.08),
                      '&:hover': {
                        backgroundColor: alpha('#000', 0.12),
                      },
                    },
                    '&:hover': {
                      backgroundColor: alpha('#000', 0.05),
                    },
                  }}
                >
                  <ListItemIcon sx={{ color: '#424242', minWidth: 40 }}>
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
                  backgroundColor: alpha('#000', 0.08),
                  '&:hover': {
                    backgroundColor: alpha('#000', 0.12),
                  },
                },
                '&:hover': {
                  backgroundColor: alpha('#000', 0.05),
                },
              }}
            >
              <ListItemIcon sx={{ color: '#424242', minWidth: 40 }}>
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
                  backgroundColor: alpha('#000', 0.08),
                  '&:hover': {
                    backgroundColor: alpha('#000', 0.12),
                  },
                },
                '&:hover': {
                  backgroundColor: alpha('#000', 0.05),
                },
              }}
            >
              <ListItemIcon sx={{ color: '#424242', minWidth: 40 }}>
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
                  backgroundColor: alpha('#000', 0.08),
                  '&:hover': {
                    backgroundColor: alpha('#000', 0.12),
                  },
                },
                '&:hover': {
                  backgroundColor: alpha('#000', 0.05),
                },
              }}
            >
              <ListItemIcon sx={{ color: '#424242', minWidth: 40 }}>
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
                  backgroundColor: alpha('#000', 0.08),
                  '&:hover': {
                    backgroundColor: alpha('#000', 0.12),
                  },
                },
                '&:hover': {
                  backgroundColor: alpha('#000', 0.05),
                },
              }}
            >
              <ListItemIcon sx={{ color: '#424242', minWidth: 40 }}>
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
                  backgroundColor: alpha('#000', 0.08),
                  '&:hover': {
                    backgroundColor: alpha('#000', 0.12),
                  },
                },
                '&:hover': {
                  backgroundColor: alpha('#000', 0.05),
                },
              }}
            >
              <ListItemIcon sx={{ color: '#424242', minWidth: 40 }}>
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
                  backgroundColor: alpha('#000', 0.08),
                  '&:hover': {
                    backgroundColor: alpha('#000', 0.12),
                  },
                },
                '&:hover': {
                  backgroundColor: alpha('#000', 0.05),
                },
              }}
            >
              <ListItemIcon sx={{ color: '#424242', minWidth: 40 }}>
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
                  backgroundColor: alpha('#000', 0.08),
                  '&:hover': {
                    backgroundColor: alpha('#000', 0.12),
                  },
                },
                '&:hover': {
                  backgroundColor: alpha('#000', 0.05),
                },
              }}
            >
              <ListItemIcon sx={{ color: '#424242', minWidth: 40 }}>
                <Settings />
              </ListItemIcon>
              <ListItemText primary="Settings" />
            </ListItemButton>
          </ListItem>
          </List>
        </Box>
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