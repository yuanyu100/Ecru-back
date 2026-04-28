import { apiClient } from './client';

const tagIdLabelMap = {
  23: { name: '极简', category: '风格', description: '偏向干净克制的穿搭表达，注重轮廓与留白。' },
  24: { name: '柔和', category: '风格', description: '整体气质轻盈温柔，颜色和线条更舒展。' },
  25: { name: '通勤', category: '场景', description: '适合上班、见客户或日常职场通勤的穿搭方向。' },
  26: { name: '学院', category: '风格', description: '强调书卷感与层次感，常见衬衫、针织和半裙组合。' },
  27: { name: '休闲', category: '风格', description: '更强调舒适和日常感，适合轻松的生活场景。' },
  28: { name: '复古', category: '风格', description: '通过廓形、材质或配色营造怀旧氛围。' },
  29: { name: '利落', category: '场景', description: '强调线条清晰和气场感，适合更有存在感的表达。' }
};

const imageIdLabelMap = {
  25: { title: '极简通勤灵感', styleCategory: '通勤', source: '系统示例' },
  26: { title: '柔和通勤灵感', styleCategory: '通勤', source: '系统示例' },
  27: { title: '学院风穿搭灵感', styleCategory: '学院', source: '系统示例' },
  28: { title: '休闲风穿搭灵感', styleCategory: '休闲', source: '系统示例' },
  29: { title: '复古风穿搭灵感', styleCategory: '复古', source: '系统示例' },
  30: { title: '利落通勤灵感', styleCategory: '通勤', source: '系统示例' }
};

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
  name: tagIdLabelMap[tag.id]?.name || toDisplayLabel(tag.name) || '',
  category: tagIdLabelMap[tag.id]?.category || toDisplayLabel(tag.category) || '',
  description: tagIdLabelMap[tag.id]?.description || tag.description || '',
  usageCount: Number(tag.usageCount || 0)
});

const normalizeImage = (item = {}) => ({
  ...item,
  id: item.id,
  imageUrl: item.imageUrl || '',
  title: imageIdLabelMap[item.id]?.title || item.title || '未命名风格图片',
  source: imageIdLabelMap[item.id]?.source || toDisplayLabel(item.source) || '',
  sourceUrl: item.sourceUrl || '',
  price: item.price ?? null,
  styleCategory: imageIdLabelMap[item.id]?.styleCategory || toDisplayLabel(item.styleCategory) || '',
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
