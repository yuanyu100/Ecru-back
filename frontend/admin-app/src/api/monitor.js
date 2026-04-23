import { apiClient } from './client';

export const monitorApi = {
  async getDashboard() {
    const response = await apiClient.get('/ai-monitor/dashboard');
    return response.data;
  },

  async getRecentCalls(limit = 10) {
    const response = await apiClient.get('/ai-monitor/recent-calls', {
      params: { limit }
    });
    return response.data;
  },

  async getHealth() {
    const response = await apiClient.get('/ai-monitor/health');
    return response.data;
  }
};
