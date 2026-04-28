import { apiClient } from './client';

export const styleImageAdminApi = {
  async getStyleImages(params = {}) {
    const response = await apiClient.get('/admin/style-images', {
      params: {
        page: params.page ?? 1,
        size: params.size ?? 20,
        keyword: params.keyword || undefined,
        styleCategory: params.styleCategory || undefined,
        active: params.active ?? undefined
      }
    });
    return response.data;
  },

  async createStyleImage(payload) {
    const response = await apiClient.post('/admin/style-images', payload);
    return response.data;
  },

  async updateStyleImage(id, payload) {
    const response = await apiClient.put(`/admin/style-images/${id}`, payload);
    return response.data;
  },

  async deleteStyleImage(id) {
    const response = await apiClient.delete(`/admin/style-images/${id}`);
    return response.data;
  },

  async uploadStyleImage(file) {
    const form = new FormData();
    form.append('file', file);
    const response = await apiClient.post('/admin/style-images/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    return response.data;
  },

  async getStyleTags() {
    const response = await apiClient.get('/style-preferences/tags');
    return response.data;
  }
};
