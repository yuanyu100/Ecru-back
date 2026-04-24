import { apiClient } from './client';

const asArray = (value) => (Array.isArray(value) ? value : []);

const normalizeSearchItem = (item = {}) => ({
  ...item,
  documentId: item.documentId || '',
  title: item.title || '未命名知识条目',
  type: item.type || 'unknown',
  content: item.content || '',
  source: item.source || '',
  relevance: Number(item.relevance || 0),
  tags: asArray(item.tags)
});

const normalizeFabric = (item = {}) => ({
  ...item,
  fabricId: item.fabricId || null,
  name: item.name || '',
  alias: asArray(item.alias),
  type: item.type || '',
  summary: item.summary || '',
  properties: item.properties || '',
  careGuide: item.careGuide || '',
  suitableSeasons: asArray(item.suitableSeasons),
  suitableOccasions: asArray(item.suitableOccasions),
  keywords: asArray(item.keywords),
  characteristics: item.characteristics || {}
});

const normalizeCareLabel = (item = {}) => ({
  ...item,
  careLabelId: item.careLabelId || null,
  symbolCode: item.symbolCode || '',
  symbolName: item.symbolName || '',
  category: item.category || '',
  instruction: item.instruction || '',
  explanation: item.explanation || '',
  doText: item.doText || '',
  dontText: item.dontText || '',
  keywords: asArray(item.keywords),
  relevance: Number(item.relevance || 0)
});

const normalizeGuide = (item = {}) => ({
  ...item,
  title: item.title || '',
  subtitle: item.subtitle || '',
  guideType: item.guideType || '',
  content: item.content || '',
  author: item.author || '',
  publishDate: item.publishDate || '',
  tags: asArray(item.tags),
  coverImageUrl: item.coverImageUrl || '',
  coverImageCaption: item.coverImageCaption || ''
});

const normalizeAnalysis = (item = {}) => ({
  ...item,
  detectedText: item.detectedText || '',
  materials: asArray(item.materials),
  careLabels: asArray(item.careLabels),
  productType: item.productType || '',
  confidence: Number(item.confidence || 0),
  summary: item.summary || ''
});

export const knowledgeApi = {
  search: async (query, type = 'all', limit = 10) => {
    const response = await apiClient.get('/knowledge/search', {
      params: { query, type, limit }
    });
    const payload = response.data?.data || {};

    return {
      ...response.data,
      data: {
        ...payload,
        items: asArray(payload.items).map(normalizeSearchItem),
        total: Number(payload.total || 0),
        type: payload.type || type
      }
    };
  },

  getFabricDetail: async (fabricId) => {
    const response = await apiClient.get(`/knowledge/fabric/${fabricId}`);
    return {
      ...response.data,
      data: normalizeFabric(response.data?.data || {})
    };
  },

  getGuideDetail: async (guideId) => {
    const response = await apiClient.get(`/knowledge/guides/${guideId}`);
    return {
      ...response.data,
      data: normalizeGuide(response.data?.data || {})
    };
  },

  getCareLabelDetail: async (symbolCode) => {
    const response = await apiClient.get(`/knowledge/care-labels/${symbolCode}`);
    return {
      ...response.data,
      data: normalizeCareLabel(response.data?.data || {})
    };
  },

  askMaterialQuestion: async (payload) => {
    const response = await apiClient.post('/knowledge/materials/ask', payload);
    const data = response.data?.data || {};

    return {
      ...response.data,
      data: {
        ...data,
        material: data.material || '',
        answer: data.answer || '',
        answerSource: data.answerSource || '',
        matchedFabrics: asArray(data.matchedFabrics).map(normalizeFabric),
        matchedCareLabels: asArray(data.matchedCareLabels).map(normalizeCareLabel)
      }
    };
  },

  analyzeMaterialLabel: async ({ image, question, materialHint }) => {
    const formData = new FormData();
    formData.append('image', image);

    if (question) {
      formData.append('question', question);
    }
    if (materialHint) {
      formData.append('materialHint', materialHint);
    }

    const response = await apiClient.post('/knowledge/materials/analyze', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
    const data = response.data?.data || {};

    return {
      ...response.data,
      data: {
        ...data,
        material: data.material || '',
        answer: data.answer || '',
        answerSource: data.answerSource || '',
        analysis: normalizeAnalysis(data.analysis || {}),
        matchedFabrics: asArray(data.matchedFabrics).map(normalizeFabric),
        matchedCareLabels: asArray(data.matchedCareLabels).map(normalizeCareLabel)
      }
    };
  }
};
