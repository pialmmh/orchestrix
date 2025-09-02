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
  phoneNumber?: string;
  role: string;
  status: string;
  department?: string;
  jobTitle?: string;
  lastLoginAt?: string;
  createdAt: string;
  updatedAt: string;
}

interface UserState {
  users: User[];
  selectedUser: User | null;
  loading: boolean;
  error: string | null;
  totalUsers: number;
  currentPage: number;
  pageSize: number;
}

interface CreateUserDto {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  role: string;
  department?: string;
  jobTitle?: string;
  phoneNumber?: string;
}

interface UpdateUserDto {
  email?: string;
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  department?: string;
  jobTitle?: string;
  role?: string;
  status?: string;
}

interface ChangePasswordDto {
  userId: string;
  currentPassword: string;
  newPassword: string;
}

interface ResetPasswordDto {
  email: string;
}

const initialState: UserState = {
  users: [],
  selectedUser: null,
  loading: false,
  error: null,
  totalUsers: 0,
  currentPage: 1,
  pageSize: 10,
};

export const fetchUsers = createAsyncThunk(
  'users/fetchAll',
  async (params?: { page?: number; size?: number; status?: string }) => {
    const response = await axios.get(`${API_URL}/users`, { params });
    return response.data;
  }
);

export const fetchUserById = createAsyncThunk(
  'users/fetchById',
  async (userId: string) => {
    const response = await axios.get(`${API_URL}/users/${userId}`);
    return response.data;
  }
);

export const createUser = createAsyncThunk(
  'users/create',
  async (userData: CreateUserDto) => {
    const response = await axios.post(`${API_URL}/users`, userData);
    return response.data;
  }
);

export const updateUser = createAsyncThunk(
  'users/update',
  async ({ userId, data }: { userId: string; data: UpdateUserDto }) => {
    const response = await axios.put(`${API_URL}/users/${userId}`, data);
    return response.data;
  }
);

export const deleteUser = createAsyncThunk(
  'users/delete',
  async (userId: string) => {
    await axios.delete(`${API_URL}/users/${userId}`);
    return userId;
  }
);

export const changePassword = createAsyncThunk(
  'users/changePassword',
  async (data: ChangePasswordDto) => {
    const response = await axios.post(
      `${API_URL}/users/${data.userId}/change-password`,
      {
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
      }
    );
    return response.data;
  }
);

export const resetPassword = createAsyncThunk(
  'users/resetPassword',
  async (data: ResetPasswordDto) => {
    const response = await axios.post(`${API_URL}/users/reset-password`, data);
    return response.data;
  }
);

export const updateUserStatus = createAsyncThunk(
  'users/updateStatus',
  async ({ userId, status }: { userId: string; status: string }) => {
    const response = await axios.put(`${API_URL}/users/${userId}/status`, {
      status,
    });
    return response.data;
  }
);

const userSlice = createSlice({
  name: 'users',
  initialState,
  reducers: {
    clearError: (state) => {
      state.error = null;
    },
    setSelectedUser: (state, action: PayloadAction<User | null>) => {
      state.selectedUser = action.payload;
    },
    setCurrentPage: (state, action: PayloadAction<number>) => {
      state.currentPage = action.payload;
    },
    setPageSize: (state, action: PayloadAction<number>) => {
      state.pageSize = action.payload;
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch users
      .addCase(fetchUsers.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchUsers.fulfilled, (state, action) => {
        state.loading = false;
        state.users = action.payload.users || action.payload.content || action.payload;
        state.totalUsers = action.payload.totalUsers || action.payload.totalElements || action.payload.length;
      })
      .addCase(fetchUsers.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message || 'Failed to fetch users';
      })
      // Fetch user by ID
      .addCase(fetchUserById.fulfilled, (state, action) => {
        state.selectedUser = action.payload;
      })
      // Create user
      .addCase(createUser.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(createUser.fulfilled, (state, action) => {
        state.loading = false;
        state.users.push(action.payload);
        state.totalUsers += 1;
      })
      .addCase(createUser.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message || 'Failed to create user';
      })
      // Update user
      .addCase(updateUser.fulfilled, (state, action) => {
        const index = state.users.findIndex((u) => u.id === action.payload.id);
        if (index !== -1) {
          state.users[index] = action.payload;
        }
        if (state.selectedUser?.id === action.payload.id) {
          state.selectedUser = action.payload;
        }
      })
      // Delete user
      .addCase(deleteUser.fulfilled, (state, action) => {
        state.users = state.users.filter((u) => u.id !== action.payload);
        state.totalUsers -= 1;
        if (state.selectedUser?.id === action.payload) {
          state.selectedUser = null;
        }
      })
      // Update user status
      .addCase(updateUserStatus.fulfilled, (state, action) => {
        const index = state.users.findIndex((u) => u.id === action.payload.id);
        if (index !== -1) {
          state.users[index] = action.payload;
        }
        if (state.selectedUser?.id === action.payload.id) {
          state.selectedUser = action.payload;
        }
      });
  },
});

export const { clearError, setSelectedUser, setCurrentPage, setPageSize } =
  userSlice.actions;
export default userSlice.reducer;