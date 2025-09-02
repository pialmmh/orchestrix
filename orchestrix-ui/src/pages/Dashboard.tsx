import React from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Paper,
  useTheme,
  alpha,
  LinearProgress,
} from '@mui/material';
import {
  People,
  BusinessCenter,
  Public,
  Storage,
  TrendingUp,
  Security,
  CloudQueue,
  Speed,
} from '@mui/icons-material';
import {
  LineChart,
  Line,
  AreaChart,
  Area,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';

const Dashboard: React.FC = () => {
  const theme = useTheme();

  const stats = [
    {
      title: 'Total Users',
      value: '156',
      change: '+12%',
      icon: <People />,
      color: theme.palette.primary.main,
    },
    {
      title: 'Active Clients',
      value: '42',
      change: '+8%',
      icon: <BusinessCenter />,
      color: theme.palette.success.main,
    },
    {
      title: 'Partners',
      value: '8',
      change: '+2',
      icon: <Public />,
      color: theme.palette.warning.main,
    },
    {
      title: 'Active Servers',
      value: '234',
      change: '+18',
      icon: <Storage />,
      color: theme.palette.info.main,
    },
  ];

  const deploymentData = [
    { name: 'Jan', deployments: 65 },
    { name: 'Feb', deployments: 78 },
    { name: 'Mar', deployments: 90 },
    { name: 'Apr', deployments: 81 },
    { name: 'May', deployments: 96 },
    { name: 'Jun', deployments: 112 },
  ];

  const serverUtilization = [
    { name: 'CPU', value: 68 },
    { name: 'Memory', value: 75 },
    { name: 'Storage', value: 42 },
    { name: 'Network', value: 55 },
  ];

  const environmentDistribution = [
    { name: 'Production', value: 45, color: '#f44336' },
    { name: 'Staging', value: 30, color: '#ff9800' },
    { name: 'Development', value: 25, color: '#4caf50' },
  ];

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Dashboard
      </Typography>

      {/* Stats Cards */}
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 3, mb: 3 }}>
        {stats.map((stat, index) => (
          <Box key={index} sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 12px)', md: '0 0 calc(25% - 18px)' } }}>
            <Card
              sx={{
                background: `linear-gradient(135deg, ${alpha(
                  stat.color,
                  0.1
                )}, ${alpha(stat.color, 0.05)})`,
                border: `1px solid ${alpha(stat.color, 0.2)}`,
              }}
            >
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                  <Box
                    sx={{
                      p: 1.5,
                      borderRadius: 2,
                      backgroundColor: stat.color,
                      color: 'white',
                      mr: 2,
                    }}
                  >
                    {stat.icon}
                  </Box>
                  <Box sx={{ flexGrow: 1 }}>
                    <Typography color="text.secondary" variant="body2">
                      {stat.title}
                    </Typography>
                    <Typography variant="h4" sx={{ fontWeight: 600 }}>
                      {stat.value}
                    </Typography>
                  </Box>
                  <Typography
                    variant="body2"
                    sx={{
                      color: stat.change.startsWith('+')
                        ? theme.palette.success.main
                        : theme.palette.error.main,
                      fontWeight: 600,
                    }}
                  >
                    {stat.change}
                  </Typography>
                </Box>
              </CardContent>
            </Card>
          </Box>
        ))}
      </Box>

      {/* Charts */}
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 3 }}>
        {/* Deployment Trends */}
        <Box sx={{ flex: { xs: '0 0 100%', md: '0 0 calc(66.66% - 12px)' } }}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Deployment Trends
            </Typography>
            <ResponsiveContainer width="100%" height={300}>
              <AreaChart data={deploymentData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" />
                <YAxis />
                <Tooltip />
                <Area
                  type="monotone"
                  dataKey="deployments"
                  stroke={theme.palette.primary.main}
                  fill={alpha(theme.palette.primary.main, 0.3)}
                />
              </AreaChart>
            </ResponsiveContainer>
          </Paper>
        </Box>

        {/* Environment Distribution */}
        <Box sx={{ flex: { xs: '0 0 100%', md: '0 0 calc(33.33% - 12px)' } }}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Environment Distribution
            </Typography>
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={environmentDistribution}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={(entry) => `${entry.name}: ${entry.value}%`}
                  outerRadius={80}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {environmentDistribution.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </Paper>
        </Box>

        {/* Server Utilization */}
        <Box sx={{ flex: { xs: '0 0 100%', md: '0 0 calc(50% - 12px)' } }}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Resource Utilization
            </Typography>
            <Box sx={{ mt: 3 }}>
              {serverUtilization.map((item, index) => (
                <Box key={index} sx={{ mb: 3 }}>
                  <Box
                    sx={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      mb: 1,
                    }}
                  >
                    <Typography variant="body2">{item.name}</Typography>
                    <Typography variant="body2" fontWeight={600}>
                      {item.value}%
                    </Typography>
                  </Box>
                  <LinearProgress
                    variant="determinate"
                    value={item.value}
                    sx={{
                      height: 8,
                      borderRadius: 4,
                      backgroundColor: alpha(theme.palette.primary.main, 0.1),
                      '& .MuiLinearProgress-bar': {
                        borderRadius: 4,
                        backgroundColor:
                          item.value > 80
                            ? theme.palette.error.main
                            : item.value > 60
                            ? theme.palette.warning.main
                            : theme.palette.success.main,
                      },
                    }}
                  />
                </Box>
              ))}
            </Box>
          </Paper>
        </Box>

        {/* Recent Activities */}
        <Box sx={{ flex: { xs: '0 0 100%', md: '0 0 calc(50% - 12px)' } }}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Recent Activities
            </Typography>
            <Box sx={{ mt: 2 }}>
              {[
                {
                  action: 'New deployment completed',
                  time: '2 minutes ago',
                  icon: <CloudQueue />,
                  color: theme.palette.success.main,
                },
                {
                  action: 'Server maintenance scheduled',
                  time: '1 hour ago',
                  icon: <Storage />,
                  color: theme.palette.warning.main,
                },
                {
                  action: 'Security scan completed',
                  time: '3 hours ago',
                  icon: <Security />,
                  color: theme.palette.info.main,
                },
                {
                  action: 'Performance optimization applied',
                  time: '5 hours ago',
                  icon: <Speed />,
                  color: theme.palette.primary.main,
                },
              ].map((activity, index) => (
                <Box
                  key={index}
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    mb: 2,
                    p: 2,
                    borderRadius: 1,
                    backgroundColor: alpha(theme.palette.background.default, 0.5),
                  }}
                >
                  <Box
                    sx={{
                      p: 1,
                      borderRadius: 1,
                      backgroundColor: alpha(activity.color, 0.1),
                      color: activity.color,
                      mr: 2,
                    }}
                  >
                    {activity.icon}
                  </Box>
                  <Box sx={{ flexGrow: 1 }}>
                    <Typography variant="body2">{activity.action}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {activity.time}
                    </Typography>
                  </Box>
                </Box>
              ))}
            </Box>
          </Paper>
        </Box>
      </Box>
    </Box>
  );
};

export default Dashboard;