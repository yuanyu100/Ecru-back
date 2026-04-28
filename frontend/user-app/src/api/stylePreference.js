import { apiClient } from './client';

const labelMap = {
  commute: '通勤',
  academia: '学院',
  casual: '休闲',
  vintage: '复古',
  minimal: '极简',
  soft: '柔和',
  sharp: '利落',
  style: '风格',
  mood: '场景',
  'demo-seed': '系统示例'
};

const reverseLabelMap = Object.entries(labelMap).reduce((accumulator, [raw, display]) => {
  accumulator[display] = raw;
  return accumulator;
}, {});

const toDisplayLabel = (value) => {
  const raw = String(value || '').trim();
  return labelMap[raw] || raw;
};

const toRawLabel = (value) => {
  const raw = String(value || '').trim();
  return reverseLabelMap[raw] || raw;
};

const normalizeTag = (tag = {}) => ({
  ...tag,
  id: tag.id,
  name: toDisplayLabel(tag.name) || '',
  category: toDisplayLabel(tag.category) || '',
  description: tag.description || '',
  usageCount: Number(tag.usageCount || 0)
});

const normalizeImage = (item = {}) => ({
  ...item,
  id: item.id,
  imageUrl: item.imageUrl || '',
  title: item.title || toDisplayLabel(item.styleCategory) || '未命名风格图片',
  source: toDisplayLabel(item.source) || '手工标注',
  sourceUrl: item.sourceUrl || '',
  price: item.price ?? null,
  styleCategory: toDisplayLabel(item.styleCategory) || '',
  tags: Array.isArray(item.tags) ? item.tags.map(normalizeTag) : [],
  createdAt: item.createdAt || '',
  updatedAt: item.updatedAt || ''
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
      data: Array.isArray(response.data?.data)
        ? response.data.data.map((item) => toDisplayLabel(item)).filter(Boolean)
        : []
    };
  },

  async getRandomImages(count = 12) {
    const response = await apiClient.get('/style-preferences/images/random', {
      params: { count }
    });
    return {
      ...response.data,
      data: Array.isArray(response.data?.data) ? response.data.data.map(normalizeImage) : []
    };
  },

  async getImagesByCategory(category, count = 12) {
    const response = await apiClient.get(`/style-preferences/images/category/${encodeURIComponent(toRawLabel(category))}`, {
      params: { count }
    });
    return {
      ...response.data,
      data: Array.isArray(response.data?.data) ? response.data.data.map(normalizeImage) : []
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
      data: Array.isArray(response.data?.data) ? response.data.data.map(normalizeProfileItem) : []
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
