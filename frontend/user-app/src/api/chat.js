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

  sendMessageStream: async (input, handlers = {}, options = {}) => {
    const payload =
      typeof input === 'string'
        ? { message: input }
        : input || {};

    const sessionId = payload.sessionId || localStorage.getItem('chatSessionId');
    const token = localStorage.getItem('accessToken') || localStorage.getItem('token');
    const baseURL = apiClient.defaults.baseURL || '/api/v1';

    const response = await fetch(`${baseURL}/ai-chat-stream/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      signal: options.signal,
      body: JSON.stringify({
        message: payload.message,
        sessionId,
        location: payload.location,
        occasion: payload.occasion,
        context: payload.context || 'general',
        metadata: payload.metadata
      })
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

      if (data.startsWith('[SESSION]')) {
        const [nextSessionId = '', title = '', isNewConversation = 'false'] = data.slice(9).split('|');
        if (nextSessionId) {
          localStorage.setItem('chatSessionId', nextSessionId);
        }
        await handlers.onSession?.({
          sessionId: nextSessionId,
          title,
          isNewConversation: isNewConversation === 'true'
        });
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

  updateConversationTitle: async (sessionId, title) => {
    try {
      const response = await apiClient.put(`/ai-chat/conversations/${sessionId}/title`, null, {
        params: { title }
      });
      return response.data;
    } catch (error) {
      console.error('Update conversation title failed:', error);
      throw error;
    }
  },

  setConversationActive: async (sessionId, isActive) => {
    try {
      const response = await apiClient.put(`/ai-chat/conversations/${sessionId}/active`, null, {
        params: { isActive }
      });
      return response.data;
    } catch (error) {
      console.error('Set conversation active failed:', error);
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
