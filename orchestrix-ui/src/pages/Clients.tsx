import React from 'react';
import { Box, Typography, Card, CardContent, Button } from '@mui/material';
import { Add } from '@mui/icons-material';

const Clients: React.FC = () => {
  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Client Management
      </Typography>
      
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
            <Typography variant="h6">
              Orchestrix Clients
            </Typography>
            <Button variant="contained" startIcon={<Add />}>
              Add Client
            </Button>
          </Box>
          
          <Typography color="text.secondary">
            Client management functionality will be implemented here.
          </Typography>
        </CardContent>
      </Card>
    </Box>
  );
};

export default Clients;