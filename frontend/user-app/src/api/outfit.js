import { apiClient } from './client';

const splitTags = (value) =>
  String(value || '')
    .split(/[，,、]/)
    .map((item) => item.trim())
    .filter(Boolean);

const normalizeHistoryItem = (item = {}) => ({
  ...item,
  id: item.id,
  outfitName: item.outfitName || '未命名搭配',
  outfitDescription: item.outfitDescription || '',
  occasion: item.occasion || '',
  weatherCondition: item.weatherCondition || '',
  season: item.season || '',
  isFavorite: Boolean(item.isFavorite),
  createdAt: item.createdAt || '',
  updatedAt: item.updatedAt || ''
});

const normalizeStyleProfile = (profile = {}) => ({
  ...profile,
  preferredStylesList: splitTags(profile.preferredStyles),
  preferredColorsList: splitTags(profile.preferredColors),
  avoidedStylesList: splitTags(profile.avoidedStyles),
  avoidedColorsList: splitTags(profile.avoidedColors),
  lifestyleTagsList: splitTags(profile.lifestyleTags)
});

export const outfitApi = {
  async getHistory(page = 1, size = 5) {
    const response = await apiClient.get('/outfit/history', {
      params: { page, size }
    });

    const items = Array.isArray(response.data?.data)
      ? response.data.data.map(normalizeHistoryItem)
      : [];

    return {
      ...response.data,
      data: items
    };
  },

  async getAdviceDetail(id) {
    const response = await apiClient.get(`/outfit/history/${id}`);
    return {
      ...response.data,
      data: normalizeHistoryItem(response.data?.data)
    };
  },

  async toggleFavorite(id, isFavorite) {
    return (
      await apiClient.post(`/outfit/history/${id}/favorite`, null, {
        params: { isFavorite }
      })
    ).data;
  },

  async getStyleProfile() {
    const response = await apiClient.get('/outfit/style-profile');
    return {
      ...response.data,
      data: normalizeStyleProfile(response.data?.data || {})
    };
  },

  async updateStyleProfile(payload) {
    const response = await apiClient.put('/outfit/style-profile', payload);
    return response.data;
  }
};
