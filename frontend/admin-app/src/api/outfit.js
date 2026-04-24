import { apiClient } from './client';

export const outfitAdminApi = {
  async getOverview() {
    const response = await apiClient.get('/admin/outfits/overview');
    return response.data;
  },

  async getRecords(params = {}) {
    const response = await apiClient.get('/admin/outfits/records', {
      params: {
        page: params.page ?? 1,
        size: params.size ?? 20,
        keyword: params.keyword || undefined,
        ownerKeyword: params.ownerKeyword || undefined,
        occasion: params.occasion || undefined,
        favorite: params.favorite ?? undefined
      }
    });
    return response.data;
  },

  async getRecordDetail(id) {
    const response = await apiClient.get(`/admin/outfits/records/${id}`);
    return response.data;
  },

  async deleteRecord(id) {
    const response = await apiClient.delete(`/admin/outfits/records/${id}`);
    return response.data;
  }
};
