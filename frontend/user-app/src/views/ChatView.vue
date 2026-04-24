<template>
  <div class="chat-page">
    <header class="chat-header">
      <button class="ghost-button mobile-only" type="button" @click="toggleSidebar">
        {{ isSidebarOpen ? '收起会话' : '查看会话' }}
      </button>
      <div>
        <p class="eyebrow">AI Stylist</p>
        <h1>搭配助手</h1>
      </div>
      <div class="header-actions">
        <button class="ghost-button" type="button" @click="goHome">首页</button>
        <button class="ghost-button" type="button" @click="goWardrobe">衣橱</button>
        <button class="ghost-button" type="button" @click="goProfile">个人页</button>
      </div>
    </header>

    <div class="chat-layout">
      <aside class="conversation-panel" :class="{ open: isSidebarOpen }">
        <div class="panel-header">
          <div>
            <p class="eyebrow">Conversation</p>
            <h2>会话列表</h2>
          </div>
          <button class="primary-button" type="button" @click="startNewConversation">新会话</button>
        </div>

        <div v-if="isConversationLoading" class="panel-state">正在加载会话...</div>
        <div v-else-if="conversations.length === 0" class="panel-state">还没有会话，直接开始提问即可。</div>
        <template v-else>
          <div class="conversation-filter">
            <button
              v-for="item in filterOptions"
              :key="item.value"
              :class="['filter-chip', activeFilter === item.value ? 'active' : '']"
              type="button"
              @click="activeFilter = item.value"
            >
              {{ item.label }}
            </button>
          </div>

          <div v-if="filteredConversations.length === 0" class="panel-state">当前筛选下没有会话。</div>

          <div v-else class="conversation-list">
            <article
              v-for="conversation in filteredConversations"
              :key="conversation.sessionId"
              :class="['conversation-item', currentSessionId === conversation.sessionId ? 'active' : '']"
              @click="selectConversation(conversation.sessionId)"
            >
              <div class="conversation-copy">
                <div class="conversation-title-row">
                  <h3>{{ conversation.title }}</h3>
                  <span :class="['status-badge', conversation.isActive ? 'active' : 'archived']">
                    {{ conversation.isActive ? '活跃' : '已归档' }}
                  </span>
                </div>
                <p>{{ conversation.lastMessagePreview || '暂无摘要' }}</p>
                <span>{{ formatTime(conversation.updatedAt || conversation.createdAt) }}</span>
              </div>
              <div class="conversation-actions">
                <button class="text-link" type="button" @click.stop="renameConversation(conversation)">改名</button>
                <button class="text-link" type="button" @click.stop="toggleConversationActive(conversation)">
                  {{ conversation.isActive ? '归档' : '恢复' }}
                </button>
                <button class="text-danger" type="button" @click.stop="removeConversation(conversation.sessionId)">
                  删除
                </button>
              </div>
            </article>
          </div>
        </template>
      </aside>

      <main class="chat-main">
        <section v-if="messages.length === 0" class="empty-state">
          <p class="eyebrow">Quick Start</p>
          <h2>今天想怎么穿？</h2>
          <p>你可以直接输入天气、场景、颜色偏好，后端会结合衣橱给出建议。</p>

          <div class="prompt-grid">
            <button v-for="prompt in quickPrompts" :key="prompt" class="prompt-chip" type="button" @click="sendQuickPrompt(prompt)">
              {{ prompt }}
            </button>
          </div>
        </section>

        <section ref="messageListRef" class="message-list">
          <article
            v-for="message in messages"
            :key="message.id"
            :class="['message-card', message.role === 'user' ? 'user' : 'assistant']"
          >
            <div class="message-role">{{ message.role === 'user' ? '我' : 'AI 造型师' }}</div>
            <p class="message-content">{{ message.content }}</p>

            <div v-if="message.recommendations?.length" class="recommendation-grid">
              <article v-for="(item, index) in message.recommendations" :key="`${message.id}-${index}`" class="recommendation-card">
                <div class="recommendation-image">
                  <img :src="resolveRecommendationImage(item)" :alt="resolveRecommendationTitle(item)" />
                </div>
                <div class="recommendation-copy">
                  <h4>{{ resolveRecommendationTitle(item) }}</h4>
                  <p>{{ resolveRecommendationSubtitle(item) }}</p>
                </div>
              </article>
            </div>

            <time class="message-time">{{ formatTime(message.createdAt) }}</time>
          </article>

          <div v-if="isSending" class="message-card assistant pending-card">
            <div class="message-role">AI 造型师</div>
            <div class="loading-dots">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        </section>

        <form class="composer" @submit.prevent="submitMessage">
          <textarea
            v-model.trim="userInput"
            rows="3"
            placeholder="例如：明天上海 20 度，通勤场景，帮我搭一套不显臃肿的穿搭"
            :disabled="isSending"
            @keydown.enter.exact.prevent="submitMessage"
          ></textarea>

          <div class="composer-actions">
            <p>{{ currentConversationLabel }}</p>
            <button class="primary-button" type="submit" :disabled="isSending || !userInput.trim()">
              {{ isSending ? '发送中...' : '发送' }}
            </button>
          </div>
        </form>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { chatApi } from '../api/chat';

const router = useRouter();
const messageListRef = ref(null);
const conversations = ref([]);
const messages = ref([]);
const currentSessionId = ref(localStorage.getItem('chatSessionId') || '');
const currentConversationLabel = ref('新会话');
const userInput = ref('');
const isSending = ref(false);
const isConversationLoading = ref(false);
const isSidebarOpen = ref(false);
const activeFilter = ref('active');

const filterOptions = [
  { label: '活跃', value: 'active' },
  { label: '全部', value: 'all' },
  { label: '已归档', value: 'archived' }
];

const quickPrompts = [
  '今天下雨，帮我搭一套通勤穿搭',
  '周末出游，想穿得轻松一点',
  '面试场景，需要稳重但不要老气',
  '我想用衣橱里的黑白单品做搭配'
];

const fallbackImage =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="320" height="420"><rect width="100%" height="100%" fill="%23f0e4cf"/><text x="50%" y="50%" text-anchor="middle" fill="%238b7355" font-size="24">LOOK</text></svg>';

const filteredConversations = computed(() => {
  if (activeFilter.value === 'all') {
    return conversations.value;
  }

  if (activeFilter.value === 'archived') {
    return conversations.value.filter((item) => !item.isActive);
  }

  return conversations.value.filter((item) => item.isActive !== false);
});

const scrollToBottom = async () => {
  await nextTick();
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight;
  }
};

const syncConversationLabel = () => {
  const currentConversation = conversations.value.find((item) => item.sessionId === currentSessionId.value);
  currentConversationLabel.value = currentConversation?.title || '新会话';
};

const loadMessages = async (sessionId) => {
  if (!sessionId) {
    messages.value = [];
    currentConversationLabel.value = '新会话';
    return;
  }

  try {
    const response = await chatApi.getConversationMessages(sessionId);
    messages.value = response.data || [];
    syncConversationLabel();
    await scrollToBottom();
  } catch (error) {
    console.error('Load chat messages failed:', error);
    alert(error.response?.data?.message || '加载聊天记录失败');
  }
};

const loadConversations = async (preferredSessionId = currentSessionId.value) => {
  isConversationLoading.value = true;
  try {
    const response = await chatApi.getConversations(1, 20);
    conversations.value = response.data?.items || [];

    const targetSessionId =
      preferredSessionId && conversations.value.some((item) => item.sessionId === preferredSessionId)
        ? preferredSessionId
        : conversations.value[0]?.sessionId || '';

    currentSessionId.value = targetSessionId;

    if (targetSessionId) {
      localStorage.setItem('chatSessionId', targetSessionId);
    } else {
      chatApi.clearCurrentSession();
    }

    syncConversationLabel();
  } catch (error) {
    console.error('Load conversations failed:', error);
    alert(error.response?.data?.message || '加载会话列表失败');
  } finally {
    isConversationLoading.value = false;
  }
};

const selectConversation = async (sessionId) => {
  currentSessionId.value = sessionId;
  localStorage.setItem('chatSessionId', sessionId);
  isSidebarOpen.value = false;
  syncConversationLabel();
  await loadMessages(sessionId);
};

const startNewConversation = () => {
  currentSessionId.value = '';
  currentConversationLabel.value = '新会话';
  messages.value = [];
  userInput.value = '';
  isSidebarOpen.value = false;
  chatApi.clearCurrentSession();
};

const submitMessage = async () => {
  const text = userInput.value.trim();
  if (!text || isSending.value) {
    return;
  }

  const optimisticMessage = {
    id: `local-${Date.now()}`,
    role: 'user',
    content: text,
    recommendations: [],
    createdAt: new Date().toISOString()
  };

  messages.value = [...messages.value, optimisticMessage];
  userInput.value = '';
  isSending.value = true;
  await scrollToBottom();

  try {
    const response = await chatApi.sendMessage({
      message: text,
      sessionId: currentSessionId.value,
      context: 'general'
    });

    const nextSessionId = response.data?.sessionId || currentSessionId.value;
    currentSessionId.value = nextSessionId || '';

    if (nextSessionId) {
      localStorage.setItem('chatSessionId', nextSessionId);
      await loadConversations(nextSessionId);
      await loadMessages(nextSessionId);
    } else {
      await loadConversations('');
    }
  } catch (error) {
    console.error('Send chat message failed:', error);
    messages.value = messages.value.filter((item) => item.id !== optimisticMessage.id);
    alert(error.response?.data?.message || '发送消息失败，请检查后端 AI 服务');
  } finally {
    isSending.value = false;
    await scrollToBottom();
  }
};

const sendQuickPrompt = async (prompt) => {
  userInput.value = prompt;
  await submitMessage();
};

const renameConversation = async (conversation) => {
  const nextTitle = window.prompt('请输入新的会话标题', conversation.title || '');
  if (nextTitle === null) {
    return;
  }

  const title = nextTitle.trim();
  if (!title) {
    alert('会话标题不能为空');
    return;
  }

  try {
    const result = await chatApi.updateConversationTitle(conversation.sessionId, title);
    if (result?.code === 200) {
      conversation.title = title;
      syncConversationLabel();
    }
  } catch (error) {
    console.error('Rename conversation failed:', error);
    alert(error.response?.data?.message || '修改会话标题失败');
  }
};

const toggleConversationActive = async (conversation) => {
  const nextActive = !conversation.isActive;
  const actionLabel = nextActive ? '恢复' : '归档';

  if (!window.confirm(`确认${actionLabel}这个会话吗？`)) {
    return;
  }

  try {
    const result = await chatApi.setConversationActive(conversation.sessionId, nextActive);
    if (result?.code === 200) {
      conversation.isActive = nextActive;
      if (!nextActive && activeFilter.value === 'active' && currentSessionId.value === conversation.sessionId) {
        activeFilter.value = 'all';
      }
    }
  } catch (error) {
    console.error('Toggle conversation active failed:', error);
    alert(error.response?.data?.message || `${actionLabel}会话失败`);
  }
};

const removeConversation = async (sessionId) => {
  if (!window.confirm('确认删除这个会话吗？')) {
    return;
  }

  try {
    await chatApi.deleteConversation(sessionId);

    if (currentSessionId.value === sessionId) {
      startNewConversation();
    }

    await loadConversations(currentSessionId.value);

    if (currentSessionId.value) {
      await loadMessages(currentSessionId.value);
    }
  } catch (error) {
    console.error('Delete conversation failed:', error);
    alert(error.response?.data?.message || '删除会话失败');
  }
};

const resolveRecommendationImage = (item = {}) =>
  item.imageUrl || item.image || item.coverUrl || fallbackImage;

const resolveRecommendationTitle = (item = {}) =>
  item.name || item.title || item.category || '推荐单品';

const resolveRecommendationSubtitle = (item = {}) => {
  const values = [item.category, item.primaryColor || item.color, item.brand].filter(Boolean);
  return values.join(' / ') || '来自你的衣橱推荐';
};

const formatTime = (value) => {
  if (!value) {
    return '刚刚';
  }

  return new Date(value).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

const goHome = () => {
  router.push('/');
};

const goWardrobe = () => {
  router.push('/wardrobe');
};

const goProfile = () => {
  router.push('/profile');
};

const toggleSidebar = () => {
  isSidebarOpen.value = !isSidebarOpen.value;
};

watch(
  () => messages.value.length,
  async () => {
    await scrollToBottom();
  }
);

onMounted(async () => {
  await loadConversations(currentSessionId.value);

  if (currentSessionId.value) {
    await loadMessages(currentSessionId.value);
  }
});
</script>

<style scoped>
.chat-page {
  min-height: 100vh;
  padding: 20px 16px 28px;
  background:
    radial-gradient(circle at top, rgba(255, 243, 214, 0.85), transparent 34%),
    linear-gradient(180deg, #f8f1df 0%, #efe2ca 100%);
}

.chat-header,
.panel-header,
.header-actions,
.composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chat-header {
  gap: 12px;
  margin-bottom: 18px;
}

.eyebrow {
  margin-bottom: 6px;
  color: #8f6a37;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.chat-header h1,
.panel-header h2,
.empty-state h2 {
  color: #5d4523;
}

.header-actions {
  gap: 8px;
}

.chat-layout {
  display: grid;
  gap: 16px;
}

.conversation-panel,
.chat-main {
  border-radius: 24px;
  background: rgba(255, 251, 244, 0.92);
  border: 1px solid rgba(145, 104, 49, 0.14);
  box-shadow: 0 18px 44px rgba(109, 78, 38, 0.08);
}

.conversation-panel {
  padding: 18px;
}

.panel-header {
  gap: 12px;
  margin-bottom: 16px;
}

.panel-state {
  color: #7d6240;
  padding: 18px 0 6px;
}

.conversation-filter,
.conversation-title-row,
.conversation-actions {
  display: flex;
  align-items: center;
}

.conversation-filter,
.conversation-actions {
  gap: 8px;
}

.conversation-filter {
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.conversation-list {
  display: grid;
  gap: 12px;
}

.conversation-item {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  padding: 14px;
  border-radius: 18px;
  background: #fffdf8;
  border: 1px solid rgba(145, 104, 49, 0.14);
  cursor: pointer;
}

.conversation-item.active {
  border-color: #6b4b1f;
  box-shadow: inset 0 0 0 1px #6b4b1f;
}

.conversation-copy {
  min-width: 0;
}

.conversation-copy h3 {
  color: #5d4523;
  font-size: 15px;
}

.conversation-title-row {
  gap: 8px;
  justify-content: space-between;
}

.conversation-copy p,
.conversation-copy span {
  margin-top: 6px;
  color: #8b6f48;
  font-size: 12px;
}

.conversation-copy p {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.conversation-actions {
  flex-direction: column;
  align-items: stretch;
}

.status-badge {
  flex-shrink: 0;
  border-radius: 999px;
  padding: 4px 8px;
  font-size: 11px;
}

.status-badge.active {
  background: rgba(83, 148, 103, 0.16);
  color: #2f7a47;
}

.status-badge.archived {
  background: rgba(139, 115, 85, 0.16);
  color: #7d6240;
}

.chat-main {
  padding: 18px;
  display: grid;
  gap: 16px;
}

.empty-state {
  padding: 8px 4px;
  color: #6d573b;
}

.empty-state p:last-of-type {
  margin-top: 8px;
}

.prompt-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.prompt-chip {
  padding: 10px 14px;
  border: 1px solid #d4bc93;
  border-radius: 999px;
  background: #fff8eb;
  color: #6d573b;
  cursor: pointer;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 220px;
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 2px;
}

.message-card {
  max-width: 88%;
  padding: 14px;
  border-radius: 22px;
}

.message-card.user {
  align-self: flex-end;
  background: #6b4b1f;
  color: #fff8ef;
}

.message-card.assistant {
  align-self: flex-start;
  background: #fffdf8;
  color: #5d4523;
  border: 1px solid rgba(145, 104, 49, 0.14);
}

.message-role {
  font-size: 12px;
  opacity: 0.75;
}

.message-content {
  margin-top: 8px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.message-time {
  display: block;
  margin-top: 10px;
  font-size: 11px;
  opacity: 0.7;
}

.recommendation-grid {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.recommendation-card {
  display: grid;
  grid-template-columns: 76px 1fr;
  gap: 10px;
  padding: 10px;
  border-radius: 16px;
  background: #f8f0df;
}

.recommendation-image {
  border-radius: 12px;
  overflow: hidden;
  background: #ead7b8;
}

.recommendation-image img {
  width: 100%;
  height: 100%;
  min-height: 88px;
  object-fit: cover;
}

.recommendation-copy h4 {
  color: #5d4523;
  font-size: 14px;
}

.recommendation-copy p {
  margin-top: 8px;
  color: #8b6f48;
  font-size: 12px;
  line-height: 1.5;
}

.composer {
  display: grid;
  gap: 10px;
  padding-top: 4px;
}

.composer textarea {
  width: 100%;
  resize: none;
  border: 1px solid #d9c39b;
  border-radius: 18px;
  padding: 14px;
  background: #fffdf8;
  color: #5d4523;
}

.composer-actions {
  gap: 12px;
}

.composer-actions p {
  color: #8b6f48;
  font-size: 13px;
}

.loading-dots {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.loading-dots span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #8b7355;
  animation: pulse 1.2s infinite ease-in-out;
}

.loading-dots span:nth-child(2) {
  animation-delay: 0.15s;
}

.loading-dots span:nth-child(3) {
  animation-delay: 0.3s;
}

.ghost-button,
.primary-button,
.text-danger,
.text-link,
.filter-chip {
  border: none;
  border-radius: 999px;
  padding: 10px 16px;
  cursor: pointer;
}

.ghost-button {
  background: #ead7b8;
  color: #5d4523;
}

.primary-button {
  background: #6b4b1f;
  color: #fff8ef;
}

.filter-chip {
  padding: 8px 12px;
  background: #f1e2c7;
  color: #6b4b1f;
}

.filter-chip.active {
  background: #6b4b1f;
  color: #fff8ef;
}

.text-link {
  padding: 6px 10px;
  background: rgba(107, 75, 31, 0.1);
  color: #6b4b1f;
}

.text-danger {
  padding: 6px 10px;
  background: rgba(217, 93, 81, 0.12);
  color: #b04d45;
}

.mobile-only {
  display: inline-flex;
}

@keyframes pulse {
  0%,
  80%,
  100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

@media (max-width: 899px) {
  .header-actions {
    display: none;
  }

  .conversation-panel {
    display: none;
  }

  .conversation-panel.open {
    display: block;
  }
}

@media (min-width: 900px) {
  .chat-page {
    padding: 28px 28px 32px;
  }

  .chat-layout {
    grid-template-columns: 320px minmax(0, 1fr);
    align-items: start;
  }

  .mobile-only {
    display: none;
  }
}
</style>
