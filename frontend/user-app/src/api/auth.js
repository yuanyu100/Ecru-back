import { apiClient, deriveRole } from './client';

const sanitizeUsername = (value = '') =>
  value
    .trim()
    .replace(/[^a-zA-Z0-9_]/g, '_')
    .slice(0, 20);

const buildUsername = (user = {}) => {
  if (user.username) {
    return sanitizeUsername(user.username);
  }

  if (user.email) {
    const localPart = user.email.split('@')[0];
    const normalized = sanitizeUsername(localPart);
    if (normalized.length >= 4) {
      return normalized;
    }
  }

  const fallback = sanitizeUsername(user.nickname || 'user0001');
  return fallback.length >= 4 ? fallback : 'user0001';
};

export const authApi = {
  register: async (user) => {
    try {
      const payload = {
        username: buildUsername(user),
        password: user.password,
        email: user.email,
        nickname: user.nickname || user.username || buildUsername(user)
      };
      const response = await apiClient.post('/auth/register', payload);
      return response.data;
    } catch (error) {
      console.error('Register failed:', error);
      throw error;
    }
  },

  login: async (credentials) => {
    try {
      const payload = {
        username: credentials.username || credentials.email,
        password: credentials.password
      };
      const response = await apiClient.post('/auth/login', payload);
      const loginResult = response.data?.data;

      if (loginResult?.accessToken) {
        const currentUser = {
          ...(loginResult.user || {}),
          role: deriveRole(loginResult.user),
          accessToken: loginResult.accessToken,
          refreshToken: loginResult.refreshToken
        };

        localStorage.setItem('accessToken', loginResult.accessToken);
        localStorage.setItem('token', loginResult.accessToken);
        if (loginResult.refreshToken) {
          localStorage.setItem('refreshToken', loginResult.refreshToken);
        }
        localStorage.setItem('user', JSON.stringify(currentUser));
      }

      return response.data;
    } catch (error) {
      console.error('Login failed:', error);
      throw error;
    }
  },

  getUserInfo: async (userId) => {
    try {
      const response = await apiClient.get(`/user/${userId}`);
      return response.data;
    } catch (error) {
      console.error('Get user info failed:', error);
      throw error;
    }
  },

  updatePreferences: async (userId, preferences) => {
    try {
      const storedPreferences = {
        ...preferences,
        userId
      };
      localStorage.setItem('userPreferences', JSON.stringify(storedPreferences));
      return {
        code: 200,
        message: 'success',
        data: storedPreferences
      };
    } catch (error) {
      console.error('Update preferences failed:', error);
      throw error;
    }
  },

  logout: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  },

  getCurrentUser: () => {
    const userStr = localStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : null;
  },

  isAuthenticated: () => {
    return localStorage.getItem('accessToken') !== null || localStorage.getItem('token') !== null;
  }
};
