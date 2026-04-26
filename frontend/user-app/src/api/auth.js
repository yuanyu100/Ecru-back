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

const persistCurrentUser = (user) => {
  if (!user) {
    localStorage.removeItem('user');
    return null;
  }

  const storedUser = authApi.getCurrentUser() || {};
  const nextUser = {
    ...storedUser,
    ...user,
    role: deriveRole(user)
  };

  localStorage.setItem('user', JSON.stringify(nextUser));
  return nextUser;
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
      return {
        ...response.data,
        data: response.data?.data
          ? {
              ...response.data.data,
              role: deriveRole(response.data.data)
            }
          : null
      };
    } catch (error) {
      console.error('Get user info failed:', error);
      throw error;
    }
  },

  getCurrentProfile: async () => {
    try {
      const response = await apiClient.get('/user/me');
      const currentUser = response.data?.data
        ? persistCurrentUser(response.data.data)
        : null;

      return {
        ...response.data,
        data: currentUser
      };
    } catch (error) {
      console.error('Get current profile failed:', error);
      throw error;
    }
  },

  updateCurrentProfile: async (payload) => {
    try {
      const response = await apiClient.put('/user/me', payload);
      const currentUser = response.data?.data
        ? persistCurrentUser(response.data.data)
        : null;

      return {
        ...response.data,
        data: currentUser
      };
    } catch (error) {
      console.error('Update current profile failed:', error);
      throw error;
    }
  },

  updateAvatar: async (avatarUrl) => {
    try {
      const response = await apiClient.put('/user/me/avatar', null, {
        params: { avatarUrl }
      });
      const currentUser = response.data?.data
        ? persistCurrentUser(response.data.data)
        : null;

      return {
        ...response.data,
        data: currentUser
      };
    } catch (error) {
      console.error('Update avatar failed:', error);
      throw error;
    }
  },

  getUserSettings: async () => {
    try {
      const response = await apiClient.get('/user/settings');
      return {
        ...response.data,
        data: response.data?.data || {}
      };
    } catch (error) {
      console.error('Get user settings failed:', error);
      throw error;
    }
  },

  updateUserSettings: async (settings) => {
    try {
      const response = await apiClient.put('/user/settings', settings);
      return {
        ...response.data,
        data: response.data?.data || {}
      };
    } catch (error) {
      console.error('Update user settings failed:', error);
      throw error;
    }
  },

  getHomePromptSettings: async () => {
    try {
      const response = await apiClient.get('/user/settings/home-prompts');
      return {
        ...response.data,
        data: response.data?.data || {}
      };
    } catch (error) {
      console.error('Get home prompt settings failed:', error);
      throw error;
    }
  },

  updateHomePromptSettings: async (payload) => {
    try {
      const response = await apiClient.put('/user/settings/home-prompts', payload);
      return {
        ...response.data,
        data: response.data?.data || {}
      };
    } catch (error) {
      console.error('Update home prompt settings failed:', error);
      throw error;
    }
  },

  previewHomePromptsFromPdf: async (file) => {
    try {
      const formData = new FormData();
      formData.append('file', file);

      const response = await apiClient.post('/user/settings/home-prompts/pdf-preview', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });

      return {
        ...response.data,
        data: response.data?.data || {}
      };
    } catch (error) {
      console.error('Preview home prompts from pdf failed:', error);
      throw error;
    }
  },

  updatePassword: async (payload) => {
    try {
      return (await apiClient.put('/user/me/password', payload)).data;
    } catch (error) {
      console.error('Update password failed:', error);
      throw error;
    }
  },

  updatePreferences: async (userId, preferences) => {
    try {
      const settings = {
        stylePreferences: JSON.stringify(preferences.stylePreferences || []),
        usualSize: preferences.usualSize || '',
        region: preferences.region || ''
      };

      const response = await authApi.updateUserSettings(settings);
      const storedPreferences = { ...preferences, userId, settings: response.data };
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
