import { apiClient } from './client';

export const knowledgeAdminApi = {
  async getOverview() {
    const response = await apiClient.get('/admin/knowledge/overview');
    return response.data;
  },

  async getFabrics(params = {}) {
    const response = await apiClient.get('/admin/knowledge/fabrics', {
      params: {
        page: params.page ?? 1,
        size: params.size ?? 20,
        keyword: params.keyword || undefined,
        active: params.active ?? undefined
      }
    });
    return response.data;
  },

  async createFabric(payload) {
    const response = await apiClient.post('/admin/knowledge/fabrics', payload);
    return response.data;
  },

  async importFabrics(payload) {
    const response = await apiClient.post('/admin/knowledge/fabrics/import', payload);
    return response.data;
  },

  async updateFabric(id, payload) {
    const response = await apiClient.put(`/admin/knowledge/fabrics/${id}`, payload);
    return response.data;
  },

  async deleteFabric(id) {
    const response = await apiClient.delete(`/admin/knowledge/fabrics/${id}`);
    return response.data;
  },

  async getGuides(params = {}) {
    const response = await apiClient.get('/admin/knowledge/guides', {
      params: {
        page: params.page ?? 1,
        size: params.size ?? 20,
        keyword: params.keyword || undefined,
        active: params.active ?? undefined
      }
    });
    return response.data;
  },

  async createGuide(payload) {
    const response = await apiClient.post('/admin/knowledge/guides', payload);
    return response.data;
  },

  async importGuides(payload) {
    const response = await apiClient.post('/admin/knowledge/guides/import', payload);
    return response.data;
  },

  async updateGuide(id, payload) {
    const response = await apiClient.put(`/admin/knowledge/guides/${id}`, payload);
    return response.data;
  },

  async deleteGuide(id) {
    const response = await apiClient.delete(`/admin/knowledge/guides/${id}`);
    return response.data;
  },

  async getCareLabels(params = {}) {
    const response = await apiClient.get('/admin/knowledge/care-labels', {
      params: {
        page: params.page ?? 1,
        size: params.size ?? 20,
        keyword: params.keyword || undefined,
        active: params.active ?? undefined
      }
    });
    return response.data;
  },

  async createCareLabel(payload) {
    const response = await apiClient.post('/admin/knowledge/care-labels', payload);
    return response.data;
  },

  async importCareLabels(payload) {
    const response = await apiClient.post('/admin/knowledge/care-labels/import', payload);
    return response.data;
  },

  async updateCareLabel(id, payload) {
    const response = await apiClient.put(`/admin/knowledge/care-labels/${id}`, payload);
    return response.data;
  },

  async deleteCareLabel(id) {
    const response = await apiClient.delete(`/admin/knowledge/care-labels/${id}`);
    return response.data;
  }
};
