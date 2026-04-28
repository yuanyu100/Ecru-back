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

const streamPost = async (path, payload, handlers = {}, options = {}) => {
  const token = localStorage.getItem('accessToken') || localStorage.getItem('token');
  const baseURL = apiClient.defaults.baseURL || '/api/v1';
  const response = await fetch(`${baseURL}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    signal: options.signal,
    body: JSON.stringify(payload || {})
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Stream request failed: ${response.status}`);
  }

  if (!response.body) {
    throw new Error('Stream response body is empty');
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';

  const emitData = async (rawEvent) => {
    const eventText = String(rawEvent || '').trim();
    if (!eventText) {
      return false;
    }

    const dataLines = eventText
      .split(/\r?\n/)
      .filter((line) => line.startsWith('data:'))
      .map((line) => line.slice(5).trimStart());

    const data = (dataLines.length ? dataLines.join('\n') : eventText).trim();
    if (!data) {
      return false;
    }

    if (data === '[DONE]') {
      await handlers.onComplete?.();
      return true;
    }

    if (data.startsWith('[ERROR]')) {
      const error = new Error(data.slice(7) || 'Stream response failed');
      await handlers.onError?.(error);
      throw error;
    }

    await handlers.onChunk?.(data);
    return false;
  };

  while (true) {
    const { value, done } = await reader.read();
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done });

    let separatorIndex = buffer.search(/\r?\n\r?\n/);
    while (separatorIndex !== -1) {
      const eventChunk = buffer.slice(0, separatorIndex);
      buffer = buffer.slice(separatorIndex + (buffer[separatorIndex] === '\r' ? 4 : 2));
      const shouldStop = await emitData(eventChunk);
      if (shouldStop) {
        return;
      }
      separatorIndex = buffer.search(/\r?\n\r?\n/);
    }

    if (done) {
      const finalChunk = buffer.trim();
      if (finalChunk) {
        await emitData(finalChunk);
      }
      return;
    }
  }
};

export const knowledgeApi = {
  search: async (query, type = 'all', limit = 10) => {
    const response = await apiClient.get('/knowledge/search', {
      params: { query, type, limit }
    });
    const payload = response.data?.data || {};
    const rawItems = asArray(payload.items).length ? payload.items : payload.results;
    const normalizedItems = asArray(rawItems).map(normalizeSearchItem);

    return {
      ...response.data,
      data: {
        ...payload,
        items: normalizedItems,
        results: normalizedItems,
        total: Number(payload.total || normalizedItems.length || 0),
        type: payload.type || type,
        query: payload.query || query
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

  askMaterialQuestionStream: async (payload, handlers = {}, options = {}) =>
    streamPost('/knowledge/materials/ask/stream', payload, handlers, options),

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
