import React from 'react';
import { Box, Typography, Card, CardContent, Chip, LinearProgress, Button } from '@mui/material';
import { Add, Storage, Memory, Speed } from '@mui/icons-material';

const Servers: React.FC = () => {
  const servers = [
    { id: 1, name: 'Production Server 1', cpu: 45, memory: 67, status: 'online' },
    { id: 2, name: 'Production Server 2', cpu: 78, memory: 82, status: 'online' },
    { id: 3, name: 'Staging Server', cpu: 23, memory: 45, status: 'online' },
    { id: 4, name: 'Development Server', cpu: 12, memory: 34, status: 'maintenance' },
  ];

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Server Management</Typography>
        <Button variant="contained" startIcon={<Add />}>
          Add Server
        </Button>
      </Box>

      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 3 }}>
        {servers.map((server) => (
          <Card key={server.id} sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(50% - 12px)', lg: '0 0 calc(33.33% - 16px)' } }}>
            <CardContent>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                <Typography variant="h6">{server.name}</Typography>
                <Chip
                  label={server.status}
                  color={server.status === 'online' ? 'success' : 'warning'}
                  size="small"
                />
              </Box>
              
              <Box sx={{ mb: 2 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                  <Speed sx={{ mr: 1, fontSize: 20 }} />
                  <Typography variant="body2">CPU Usage: {server.cpu}%</Typography>
                </Box>
                <LinearProgress
                  variant="determinate"
                  value={server.cpu}
                  color={server.cpu > 80 ? 'error' : 'primary'}
                  sx={{ mb: 2 }}
                />
                
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                  <Memory sx={{ mr: 1, fontSize: 20 }} />
                  <Typography variant="body2">Memory: {server.memory}%</Typography>
                </Box>
                <LinearProgress
                  variant="determinate"
                  value={server.memory}
                  color={server.memory > 80 ? 'error' : 'primary'}
                />
              </Box>

              <Box sx={{ display: 'flex', gap: 1 }}>
                <Button size="small">View</Button>
                <Button size="small">Configure</Button>
                <Button size="small">Restart</Button>
              </Box>
            </CardContent>
          </Card>
        ))}
      </Box>
    </Box>
  );
};

export default Servers;