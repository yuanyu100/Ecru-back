import { apiClient } from './client';

export const clothingApi = {
  async getClothings(params = {}) {
    const response = await apiClient.get('/clothings', {
      params: {
        page: params.page ?? 1,
        size: params.size ?? 20,
        keyword: params.keyword || undefined
      }
    });
    return response.data;
  },

  async getStatistics(period = 'all') {
    const response = await apiClient.get('/clothings/statistics', {
      params: { period }
    });
    return response.data;
  },

  async createClothing(payload) {
    const response = await apiClient.post('/clothings', payload);
    return response.data;
  },

  async deleteClothing(id, force = false) {
    const response = await apiClient.delete(`/clothings/${id}`, {
      params: { force }
    });
    return response.data;
  },

  async getAdminClothings(params = {}) {
    const response = await apiClient.get('/admin/clothings', {
      params: {
        page: params.page ?? 1,
        size: params.size ?? 20,
        userId: params.userId ?? undefined,
        keyword: params.keyword || undefined,
        ownerKeyword: params.ownerKeyword || undefined,
        category: params.category || undefined,
        primaryColor: params.primaryColor || undefined,
        sourceType: params.sourceType || undefined
      }
    });
    return response.data;
  },

  async deleteAdminClothing(id, force = false) {
    const response = await apiClient.delete(`/admin/clothings/${id}`, {
      params: { force }
    });
    return response.data;
  }
};
