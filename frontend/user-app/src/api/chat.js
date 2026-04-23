import { apiClient } from './client';

export const chatApi = {
  sendMessage: async (message) => {
    try {
      const sessionId = localStorage.getItem('chatSessionId');
      const response = await apiClient.post('/ai-chat/chat', {
        message,
        sessionId,
        context: 'general'
      });

      const result = response.data;
      const nextSessionId = result?.data?.sessionId;
      if (nextSessionId) {
        localStorage.setItem('chatSessionId', nextSessionId);
      }

      return {
        ...result,
        data: result?.data?.response || ''
      };
    } catch (error) {
      console.error('Send message failed:', error);
      throw error;
    }
  }
};
