import { apiClient, deriveRole } from './client';

export const authApi = {
  async login(credentials) {
    const payload = {
      username: credentials.username,
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
  },

  logout() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  },

  getCurrentUser() {
    const userStr = localStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : null;
  },

  isAuthenticated() {
    return Boolean(localStorage.getItem('accessToken') || localStorage.getItem('token'));
  }
};
