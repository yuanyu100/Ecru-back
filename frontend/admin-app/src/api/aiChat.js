import { apiClient } from './client';

export const aiChatAdminApi = {
  async getOverview() {
    const response = await apiClient.get('/admin/ai-chat/overview');
    return response.data;
  },

  async getConversations(params = {}) {
    const response = await apiClient.get('/admin/ai-chat/conversations', {
      params: {
        page: params.page ?? 1,
        size: params.size ?? 20,
        keyword: params.keyword || undefined,
        ownerKeyword: params.ownerKeyword || undefined,
        context: params.context || undefined,
        active: params.active ?? undefined
      }
    });
    return response.data;
  },

  async getConversationMessages(sessionId) {
    const response = await apiClient.get(`/admin/ai-chat/conversations/${sessionId}/messages`);
    return response.data;
  },

  async deleteConversation(sessionId) {
    const response = await apiClient.delete(`/admin/ai-chat/conversations/${sessionId}`);
    return response.data;
  }
};
