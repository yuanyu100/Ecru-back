import { apiClient } from './client';

export const aiPromptApi = {
  async getChatSettings() {
    const response = await apiClient.get('/admin/ai-prompts/chat');
    return response.data;
  },

  async updateChatSettings(payload) {
    const response = await apiClient.put('/admin/ai-prompts/chat', payload);
    return response.data;
  }
};
