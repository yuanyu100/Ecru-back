import { apiClient } from './client';

const normalizeTag = (tag = {}) => ({
  ...tag,
  id: tag.id,
  name: tag.name || '',
  category: tag.category || '',
  description: tag.description || '',
  usageCount: Number(tag.usageCount || 0)
});

const normalizeImage = (item = {}) => ({
  ...item,
  id: item.id,
  imageUrl: item.imageUrl || '',
  title: item.title || '未命名风格图',
  source: item.source || '',
  sourceUrl: item.sourceUrl || '',
  price: item.price ?? null,
  styleCategory: item.styleCategory || '',
  tags: Array.isArray(item.tags) ? item.tags.map(normalizeTag) : [],
  createdAt: item.createdAt || ''
});

const normalizeProfileItem = (item = {}) => ({
  ...item,
  preferenceScore: Number(item.preferenceScore || 0),
  interactionCount: Number(item.interactionCount || 0),
  styleTag: normalizeTag(item.styleTag || {})
});

export const stylePreferenceApi = {
  async getCategories() {
    const response = await apiClient.get('/style-preferences/tags/categories');
    return {
      ...response.data,
      data: Array.isArray(response.data?.data) ? response.data.data : []
    };
  },

  async getRandomImages(count = 12) {
    const response = await apiClient.get('/style-preferences/images/random', {
      params: { count }
    });
    return {
      ...response.data,
      data: Array.isArray(response.data?.data)
        ? response.data.data.map(normalizeImage)
        : []
    };
  },

  async getImagesByCategory(category, count = 12) {
    const response = await apiClient.get(`/style-preferences/images/category/${encodeURIComponent(category)}`, {
      params: { count }
    });
    return {
      ...response.data,
      data: Array.isArray(response.data?.data)
        ? response.data.data.map(normalizeImage)
        : []
    };
  },

  async submitFeedback(imageId, preferenceType) {
    return (
      await apiClient.post('/style-preferences/feedback', {
        imageId,
        preferenceType
      })
    ).data;
  },

  async getTopPreferences(limit = 6) {
    const response = await apiClient.get('/style-preferences/profile/top', {
      params: { limit }
    });
    return {
      ...response.data,
      data: Array.isArray(response.data?.data)
        ? response.data.data.map(normalizeProfileItem)
        : []
    };
  },

  async getLearningProgress() {
    const response = await apiClient.get('/style-preferences/progress');
    return {
      ...response.data,
      data: Number(response.data?.data || 0)
    };
  },

  async resetProfile() {
    return (await apiClient.post('/style-preferences/profile/reset')).data;
  }
};
