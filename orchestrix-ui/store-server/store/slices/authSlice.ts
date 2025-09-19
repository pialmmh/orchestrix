import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import axios from 'axios';

const API_URL = process.env.REACT_APP_API_URL || '/api/api';

interface User {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
  role: string;
  status: string;
  department?: string;
  jobTitle?: string;
  avatarUrl?: string;
  permissions: string[];
  themePreference: string;
  language: string;
}

interface AuthState {
  user: User | null;
  token: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  loading: boolean;
  error: string | null;
}

interface LoginCredentials {
  username: string;
  password: string;
}

interface AuthResponse {
  user: User;
  token: string;
  refreshToken: string;
}

const initialState: AuthState = {
  user: JSON.parse(localStorage.getItem('user') || 'null'),
  token: localStorage.getItem('token'),
  refreshToken: localStorage.getItem('refreshToken'),
  isAuthenticated: !!localStorage.getItem('token'),
  loading: false,
  error: null,
};

export const login = createAsyncThunk(
  'auth/login',
  async (credentials: LoginCredentials) => {
    try {
      console.log('Attempting login to:', `${API_URL}/auth/login`);
      const response = await axios.post<AuthResponse>(
        `${API_URL}/auth/login`,
        credentials
      );
      console.log('Login successful:', response.data);
      return response.data;
    } catch (error: any) {
      console.error('Login failed:', error);
      console.error('Error response:', error.response);
      throw error;
    }
  }
);

export const logout = createAsyncThunk('auth/logout', async () => {
  const token = localStorage.getItem('token');
  if (token) {
    await axios.post(
      `${API_URL}/auth/logout`,
      {},
      {
        headers: { Authorization: `Bearer ${token}` },
      }
    );
  }
});

export const refreshAccessToken = createAsyncThunk(
  'auth/refresh',
  async () => {
    const refreshToken = localStorage.getItem('refreshToken');
    const response = await axios.post<AuthResponse>(
      `${API_URL}/auth/refresh`,
      { refreshToken }
    );
    return response.data;
  }
);

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    clearError: (state) => {
      state.error = null;
    },
    updateThemePreference: (state, action: PayloadAction<string>) => {
      if (state.user) {
        state.user.themePreference = action.payload;
      }
    },
  },
  extraReducers: (builder) => {
    builder
      // Login
      .addCase(login.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(login.fulfilled, (state, action) => {
        state.loading = false;
        state.isAuthenticated = true;
        state.user = action.payload.user;
        state.token = action.payload.token;
        state.refreshToken = action.payload.refreshToken;
        
        localStorage.setItem('user', JSON.stringify(action.payload.user));
        localStorage.setItem('token', action.payload.token);
        localStorage.setItem('refreshToken', action.payload.refreshToken);
        
        axios.defaults.headers.common['Authorization'] = `Bearer ${action.payload.token}`;
      })
      .addCase(login.rejected, (state, action) => {
        state.loading = false;
        console.error('Login rejected in reducer:', action.error);
        console.error('Payload:', action.payload);
        state.error = action.error.message || 'Login failed';
      })
      // Logout
      .addCase(logout.fulfilled, (state) => {
        state.user = null;
        state.token = null;
        state.refreshToken = null;
        state.isAuthenticated = false;
        
        localStorage.removeItem('user');
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        
        delete axios.defaults.headers.common['Authorization'];
      })
      // Refresh token
      .addCase(refreshAccessToken.fulfilled, (state, action) => {
        state.token = action.payload.token;
        state.user = action.payload.user;
        
        localStorage.setItem('token', action.payload.token);
        localStorage.setItem('user', JSON.stringify(action.payload.user));
        
        axios.defaults.headers.common['Authorization'] = `Bearer ${action.payload.token}`;
      });
  },
});

export const { clearError, updateThemePreference } = authSlice.actions;
export default authSlice.reducer;