import { apiClient } from './client';

export const adminApi = {
  async getUsers(params = {}) {
    const response = await apiClient.get('/admin/users', {
      params: {
        page: params.page ?? 1,
        size: params.size ?? 20,
        keyword: params.keyword || undefined,
        status: params.status ?? undefined
      }
    });
    return response.data;
  },

  async getUserDetail(userId) {
    const response = await apiClient.get(`/admin/users/${userId}`);
    return response.data;
  },

  async updateUserStatus(userId, status) {
    const response = await apiClient.put(`/admin/users/${userId}/status`, { status });
    return response.data;
  }
};
