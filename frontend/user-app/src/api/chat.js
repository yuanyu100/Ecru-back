import { apiClient } from './client';

const normalizeConversation = (item = {}) => ({
  ...item,
  sessionId: item.sessionId || '',
  title: item.title || '未命名会话',
  context: item.context || 'general',
  isActive: Boolean(item.isActive),
  messageCount: Number(item.messageCount || 0),
  lastMessagePreview: item.lastMessagePreview || '',
  createdAt: item.createdAt || '',
  updatedAt: item.updatedAt || ''
});

const normalizeMessage = (item = {}) => ({
  ...item,
  role: item.role || 'assistant',
  content: item.content || '',
  messageType: item.messageType || 'text',
  recommendations: Array.isArray(item.recommendations) ? item.recommendations : [],
  createdAt: item.createdAt || ''
});

const extractList = (payload) => {
  if (Array.isArray(payload)) {
    return payload;
  }

  if (Array.isArray(payload?.list)) {
    return payload.list;
  }

  if (Array.isArray(payload?.result)) {
    return payload.result;
  }

  return [];
};

export const chatApi = {
  clearCurrentSession: () => {
    localStorage.removeItem('chatSessionId');
  },

  sendMessage: async (input) => {
    try {
      const payload =
        typeof input === 'string'
          ? { message: input }
          : input || {};

      const sessionId = payload.sessionId || localStorage.getItem('chatSessionId');
      const response = await apiClient.post('/ai-chat/chat', {
        message: payload.message,
        sessionId,
        location: payload.location,
        occasion: payload.occasion,
        context: payload.context || 'general',
        metadata: payload.metadata
      });

      const result = response.data?.data || {};
      const nextSessionId = result.sessionId;
      if (nextSessionId) {
        localStorage.setItem('chatSessionId', nextSessionId);
      }

      return {
        ...response.data,
        data: {
          ...result,
          response: result.response || '',
          recommendedClothes: Array.isArray(result.recommendedClothes)
            ? result.recommendedClothes
            : []
        }
      };
    } catch (error) {
      console.error('Send message failed:', error);
      throw error;
    }
  },

  getConversations: async (page = 1, size = 20) => {
    try {
      const response = await apiClient.get('/ai-chat/conversations', {
        params: { page, size }
      });
      const pageInfo = response.data?.data || {};
      const items = extractList(pageInfo).map(normalizeConversation);

      return {
        ...response.data,
        data: {
          ...pageInfo,
          items,
          total: Number(pageInfo.total || items.length)
        }
      };
    } catch (error) {
      console.error('Get conversations failed:', error);
      throw error;
    }
  },

  getConversationMessages: async (sessionId) => {
    try {
      const response = await apiClient.get(`/ai-chat/conversations/${sessionId}/messages/all`);
      return {
        ...response.data,
        data: extractList(response.data?.data).map(normalizeMessage)
      };
    } catch (error) {
      console.error('Get conversation messages failed:', error);
      throw error;
    }
  },

  deleteConversation: async (sessionId) => {
    try {
      const response = await apiClient.delete(`/ai-chat/conversations/${sessionId}`);
      if (localStorage.getItem('chatSessionId') === sessionId) {
        localStorage.removeItem('chatSessionId');
      }
      return response.data;
    } catch (error) {
      console.error('Delete conversation failed:', error);
      throw error;
    }
  }
};
