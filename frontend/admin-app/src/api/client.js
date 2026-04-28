import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
});

apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken') || localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

export const deriveRole = (user) => {
  if (!user) {
    return 'USER';
  }

  if (user.role) {
    const normalizedRole = String(user.role).toUpperCase();
    if (normalizedRole.includes('ADMIN')) {
      return 'ADMIN';
    }
    return 'USER';
  }

  if (user.userId === 1 || user.id === 1) {
    return 'ADMIN';
  }

  return 'USER';
};

export const normalizeCurrentUser = (user) => {
  if (!user) {
    return null;
  }

  return {
    ...user,
    role: deriveRole(user)
  };
};

export const unwrapResult = (response) => response?.data ?? response;
