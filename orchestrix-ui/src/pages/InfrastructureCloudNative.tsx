import React from 'react';
import { Box, Typography, Paper } from '@mui/material';

const InfrastructureCloudNative: React.FC = () => {
  return (
    <Box sx={{ p: 3 }}>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h5">Infrastructure (Original)</Typography>
        <Typography variant="body1" sx={{ mt: 2 }}>
          This page is temporarily disabled for maintenance. 
          Please use the Stellar infrastructure page at /infrastructure-stellar
        </Typography>
      </Paper>
    </Box>
  );
};

export default InfrastructureCloudNative;