import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  Alert,
  Paper,
  useTheme,
  alpha,
} from '@mui/material';
import { Email, ArrowBack } from '@mui/icons-material';

const ForgotPassword: React.FC = () => {
  const theme = useTheme();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!email) {
      setError('Please enter your email address');
      return;
    }
    // Handle password reset request
    setSubmitted(true);
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: `linear-gradient(135deg, ${theme.palette.primary.dark} 0%, ${theme.palette.primary.main} 50%, ${theme.palette.secondary.main} 100%)`,
      }}
    >
      <Card
        sx={{
          width: '100%',
          maxWidth: 480,
          m: 2,
          backdropFilter: 'blur(20px)',
          backgroundColor: alpha(theme.palette.background.paper, 0.9),
          boxShadow: '0 8px 32px 0 rgba(31, 38, 135, 0.37)',
        }}
      >
        <CardContent sx={{ p: 4 }}>
          {!submitted ? (
            <>
              <Typography variant="h5" gutterBottom sx={{ textAlign: 'center' }}>
                Forgot Password?
              </Typography>
              <Typography
                variant="body2"
                color="text.secondary"
                sx={{ textAlign: 'center', mb: 3 }}
              >
                Enter your email address and we'll send you instructions to reset your
                password.
              </Typography>

              {error && (
                <Alert severity="error" sx={{ mb: 2 }}>
                  {error}
                </Alert>
              )}

              <form onSubmit={handleSubmit}>
                <TextField
                  fullWidth
                  label="Email Address"
                  type="email"
                  value={email}
                  onChange={(e) => {
                    setEmail(e.target.value);
                    setError('');
                  }}
                  margin="normal"
                  required
                  autoFocus
                  InputProps={{
                    startAdornment: <Email sx={{ mr: 1, color: 'action.active' }} />,
                  }}
                />

                <Button
                  type="submit"
                  fullWidth
                  variant="contained"
                  size="large"
                  sx={{
                    mt: 3,
                    mb: 2,
                    py: 1.5,
                    background: `linear-gradient(135deg, ${theme.palette.primary.main}, ${theme.palette.secondary.main})`,
                  }}
                >
                  Send Reset Instructions
                </Button>
              </form>
            </>
          ) : (
            <Box sx={{ textAlign: 'center' }}>
              <Paper
                elevation={0}
                sx={{
                  p: 2,
                  borderRadius: '50%',
                  backgroundColor: alpha(theme.palette.success.main, 0.1),
                  width: 80,
                  height: 80,
                  margin: '0 auto',
                  mb: 3,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <Email sx={{ fontSize: 40, color: theme.palette.success.main }} />
              </Paper>
              <Typography variant="h5" gutterBottom>
                Check Your Email
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                We've sent password reset instructions to {email}
              </Typography>
              <Button
                variant="contained"
                onClick={() => navigate('/login')}
                sx={{
                  background: `linear-gradient(135deg, ${theme.palette.primary.main}, ${theme.palette.secondary.main})`,
                }}
              >
                Back to Login
              </Button>
            </Box>
          )}

          {!submitted && (
            <Box sx={{ mt: 3, textAlign: 'center' }}>
              <Button
                startIcon={<ArrowBack />}
                onClick={() => navigate('/login')}
                sx={{ textTransform: 'none' }}
              >
                Back to Login
              </Button>
            </Box>
          )}
        </CardContent>
      </Card>
    </Box>
  );
};

export default ForgotPassword;