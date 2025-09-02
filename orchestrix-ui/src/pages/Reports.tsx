import React from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  IconButton,
} from '@mui/material';
import {
  Download,
  Visibility,
  Schedule,
  Assessment,
  TrendingUp,
  Description,
} from '@mui/icons-material';

const Reports: React.FC = () => {
  const reports = [
    {
      id: 1,
      name: 'Monthly Performance Report',
      type: 'Performance',
      generated: '2024-01-15',
      size: '2.4 MB',
      status: 'ready',
    },
    {
      id: 2,
      name: 'Security Audit Report',
      type: 'Security',
      generated: '2024-01-14',
      size: '1.8 MB',
      status: 'ready',
    },
    {
      id: 3,
      name: 'User Activity Report',
      type: 'Activity',
      generated: '2024-01-13',
      size: '3.1 MB',
      status: 'ready',
    },
    {
      id: 4,
      name: 'System Health Report',
      type: 'System',
      generated: '2024-01-12',
      size: '1.2 MB',
      status: 'generating',
    },
  ];

  const scheduledReports = [
    { name: 'Weekly Summary', schedule: 'Every Monday 9:00 AM', next: 'In 2 days' },
    { name: 'Monthly Analytics', schedule: 'First day of month', next: 'In 15 days' },
    { name: 'Quarterly Review', schedule: 'Every 3 months', next: 'In 45 days' },
  ];

  const getTypeColor = (type: string) => {
    switch (type) {
      case 'Performance': return 'primary';
      case 'Security': return 'error';
      case 'Activity': return 'info';
      case 'System': return 'warning';
      default: return 'default';
    }
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Reports</Typography>
        <Button variant="contained" startIcon={<Assessment />}>
          Generate Report
        </Button>
      </Box>

      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 3, mb: 3 }}>
        <Card sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(25% - 18px)' } }}>
          <CardContent>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
              <Description sx={{ mr: 1, color: 'primary.main' }} />
              <Typography variant="h6">24</Typography>
            </Box>
            <Typography variant="body2" color="text.secondary">
              Total Reports
            </Typography>
          </CardContent>
        </Card>

        <Card sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(25% - 18px)' } }}>
          <CardContent>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
              <TrendingUp sx={{ mr: 1, color: 'success.main' }} />
              <Typography variant="h6">12</Typography>
            </Box>
            <Typography variant="body2" color="text.secondary">
              Generated This Month
            </Typography>
          </CardContent>
        </Card>

        <Card sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(25% - 18px)' } }}>
          <CardContent>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
              <Schedule sx={{ mr: 1, color: 'warning.main' }} />
              <Typography variant="h6">3</Typography>
            </Box>
            <Typography variant="body2" color="text.secondary">
              Scheduled Reports
            </Typography>
          </CardContent>
        </Card>

        <Card sx={{ flex: { xs: '0 0 100%', sm: '0 0 calc(25% - 18px)' } }}>
          <CardContent>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
              <Download sx={{ mr: 1, color: 'info.main' }} />
              <Typography variant="h6">45</Typography>
            </Box>
            <Typography variant="body2" color="text.secondary">
              Downloads This Week
            </Typography>
          </CardContent>
        </Card>
      </Box>

      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 3 }}>
        <Box sx={{ flex: { xs: '0 0 100%', lg: '0 0 calc(65% - 12px)' } }}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Recent Reports
              </Typography>
              <TableContainer component={Paper}>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Report Name</TableCell>
                      <TableCell>Type</TableCell>
                      <TableCell>Generated</TableCell>
                      <TableCell>Size</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell align="right">Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {reports.map((report) => (
                      <TableRow key={report.id}>
                        <TableCell>{report.name}</TableCell>
                        <TableCell>
                          <Chip
                            label={report.type}
                            color={getTypeColor(report.type) as any}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>{report.generated}</TableCell>
                        <TableCell>{report.size}</TableCell>
                        <TableCell>
                          <Chip
                            label={report.status}
                            color={report.status === 'ready' ? 'success' : 'warning'}
                            size="small"
                          />
                        </TableCell>
                        <TableCell align="right">
                          <IconButton size="small">
                            <Visibility />
                          </IconButton>
                          <IconButton size="small">
                            <Download />
                          </IconButton>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        </Box>

        <Box sx={{ flex: { xs: '0 0 100%', lg: '0 0 calc(35% - 12px)' } }}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Scheduled Reports
              </Typography>
              {scheduledReports.map((report, index) => (
                <Box key={index} sx={{ mb: 2, pb: 2, borderBottom: '1px solid #e0e0e0' }}>
                  <Typography variant="body1" fontWeight="medium">
                    {report.name}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {report.schedule}
                  </Typography>
                  <Typography variant="caption" color="primary">
                    Next: {report.next}
                  </Typography>
                </Box>
              ))}
              <Button fullWidth variant="outlined" sx={{ mt: 2 }}>
                Manage Schedules
              </Button>
            </CardContent>
          </Card>
        </Box>
      </Box>
    </Box>
  );
};

export default Reports;