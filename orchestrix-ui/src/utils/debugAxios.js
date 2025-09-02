import axios from 'axios';

// Add request interceptor for debugging
axios.interceptors.request.use(
  (config) => {
    console.log('Making request to:', config.url);
    console.log('Request config:', config);
    return config;
  },
  (error) => {
    console.error('Request error:', error);
    return Promise.reject(error);
  }
);

// Add response interceptor for debugging
axios.interceptors.response.use(
  (response) => {
    console.log('Response from:', response.config.url);
    console.log('Response data:', response.data);
    return response;
  },
  (error) => {
    console.error('Response error from:', error.config?.url);
    console.error('Error status:', error.response?.status);
    console.error('Error data:', error.response?.data);
    console.error('Full error:', error);
    return Promise.reject(error);
  }
);

export default axios;