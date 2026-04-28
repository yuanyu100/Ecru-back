import { apiClient } from './client';

const splitTags = (value) =>
  String(value || '')
    .split(/[，、,]/)
    .map((item) => item.trim())
    .filter(Boolean);

const normalizeHistoryItem = (item = {}) => ({
  ...item,
  id: item.id,
  outfitName: item.outfitName || '未命名穿搭',
  outfitDescription: item.outfitDescription || '',
  occasion: item.occasion || '',
  weatherCondition: item.weatherCondition || '',
  season: item.season || '',
  reasoning: item.reasoning || '',
  fashionSuggestions: item.fashionSuggestions || '',
  isFavorite: Boolean(item.isFavorite),
  createdAt: item.createdAt || '',
  updatedAt: item.updatedAt || ''
});

const normalizeOutfitItem = (item = {}) => ({
  ...item,
  id: item.id,
  itemName: item.itemName || '未命名单品',
  itemCategory: item.itemCategory || '',
  itemColor: item.itemColor || '',
  itemImageUrl: item.itemImageUrl || '',
  isRecommended: Boolean(item.isRecommended),
  reason: item.reason || ''
});

const normalizeFeedback = (feedback) => {
  if (!feedback) {
    return null;
  }

  return {
    ...feedback,
    overallRating: Number(feedback.overallRating || 0),
    styleRating: Number(feedback.styleRating || 0),
    practicalityRating: Number(feedback.practicalityRating || 0),
    weatherRating: Number(feedback.weatherRating || 0),
    isWorn: Boolean(feedback.isWorn),
    feedbackText: feedback.feedbackText || '',
    wornAt: feedback.wornAt || ''
  };
};

const normalizeStyleProfile = (profile = {}) => ({
  ...profile,
  preferredStylesList: splitTags(profile.preferredStyles),
  preferredColorsList: splitTags(profile.preferredColors),
  avoidedStylesList: splitTags(profile.avoidedStyles),
  avoidedColorsList: splitTags(profile.avoidedColors),
  lifestyleTagsList: splitTags(profile.lifestyleTags)
});

const normalizeAdviceDetail = (payload = {}) => ({
  record: normalizeHistoryItem(payload.record || {}),
  items: Array.isArray(payload.items) ? payload.items.map(normalizeOutfitItem) : [],
  feedback: normalizeFeedback(payload.feedback)
});

const normalizeHomeRecommendationLook = (item = {}) => ({
  ...item,
  id: item.id,
  mood: item.mood || '今日搭配',
  title: item.title || '未命名搭配',
  note: item.note || '',
  tags: Array.isArray(item.tags) ? item.tags.filter(Boolean) : [],
  palette: Array.isArray(item.palette) && item.palette.length ? item.palette : ['#D7C1A1', '#F4EEE3'],
  items: Array.isArray(item.items)
    ? item.items.map((lookItem) => ({
        clothingId: lookItem.clothingId,
        name: lookItem.name || '',
        category: lookItem.category || '',
        color: lookItem.color || '',
        imageUrl: lookItem.imageUrl || '',
        reason: lookItem.reason || '',
        frequencyLevel: Number(lookItem.frequencyLevel || 0),
        wearCount: Number(lookItem.wearCount || 0),
        sourceType: lookItem.sourceType || '',
        sourcePlatform: lookItem.sourcePlatform || '',
        fromWardrobe: lookItem.fromWardrobe !== false
      }))
    : [],
  createdAt: item.createdAt || ''
});

export const outfitApi = {
  async getHistory(page = 1, size = 5) {
    const response = await apiClient.get('/outfit/history', {
      params: { page, size }
    });

    return {
      ...response.data,
      data: Array.isArray(response.data?.data)
        ? response.data.data.map(normalizeHistoryItem)
        : []
    };
  },

  async getAdviceDetail(id) {
    const response = await apiClient.get(`/outfit/history/${id}`);
    return {
      ...response.data,
      data: normalizeAdviceDetail(response.data?.data || {})
    };
  },

  async deleteAdvice(id) {
    return (await apiClient.delete(`/outfit/history/${id}`)).data;
  },

  async toggleFavorite(id, isFavorite) {
    return (
      await apiClient.post(`/outfit/history/${id}/favorite`, null, {
        params: { isFavorite }
      })
    ).data;
  },

  async submitFeedback(id, payload) {
    const response = await apiClient.post(`/outfit/${id}/feedback`, payload);
    return {
      ...response.data,
      data: normalizeFeedback(response.data?.data)
    };
  },

  async getStyleProfile() {
    const response = await apiClient.get('/outfit/style-profile');
    return {
      ...response.data,
      data: normalizeStyleProfile(response.data?.data || {})
    };
  },

  async updateStyleProfile(payload) {
    return (await apiClient.put('/outfit/style-profile', payload)).data;
  },

  async getHomeRecommendations(params = {}) {
    const response = await apiClient.get('/outfit/home-recommendations', { params });
    return {
      ...response.data,
      data: {
        ...(response.data?.data || {}),
        emptyReason: response.data?.data?.emptyReason || '',
        looks: Array.isArray(response.data?.data?.looks)
          ? response.data.data.looks.map(normalizeHomeRecommendationLook)
          : []
      }
    };
  },

  async getHomeRecommendationDetail(id) {
    const response = await apiClient.get(`/outfit/home-recommendations/${id}`);
    return {
      ...response.data,
      data: normalizeHomeRecommendationLook(response.data?.data || {})
    };
  }
};
