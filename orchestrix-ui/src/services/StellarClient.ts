import axios, { AxiosInstance } from 'axios';
import config from '../config';

class StellarClient {
  private client: AxiosInstance;

  constructor() {
    const baseURL = config.getApiEndpoint('');
    console.log('🔧 StellarClient baseURL:', baseURL);
    this.client = axios.create({
      baseURL: baseURL,
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Add request interceptor for auth if needed
    this.client.interceptors.request.use(
      (config) => {
        // Add auth token if available (only in browser)
        if (typeof window !== 'undefined' && window.localStorage) {
          const token = localStorage.getItem('authToken');
          if (token) {
            config.headers.Authorization = `Bearer ${token}`;
          }
        }
        return config;
      },
      (error) => {
        return Promise.reject(error);
      }
    );

    // Add response interceptor for error handling
    this.client.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401) {
          // Handle unauthorized
          console.error('Unauthorized access');
        }
        return Promise.reject(error);
      }
    );
  }

  async post<T = any>(endpoint: string, data: any): Promise<T> {
    console.log('🚀 StellarClient POST to:', this.client.defaults.baseURL + endpoint);
    console.log('📦 Request data:', data);
    const response = await this.client.post(endpoint, data);
    console.log('✅ Response:', response);
    console.log('📊 Response data:', response.data);
    return response.data;
  }

  async get<T = any>(endpoint: string): Promise<T> {
    const response = await this.client.get(endpoint);
    return response.data;
  }

  async put<T = any>(endpoint: string, data: any): Promise<T> {
    const response = await this.client.put(endpoint, data);
    return response.data;
  }

  async delete<T = any>(endpoint: string): Promise<T> {
    const response = await this.client.delete(endpoint);
    return response.data;
  }
}

export default new StellarClient();