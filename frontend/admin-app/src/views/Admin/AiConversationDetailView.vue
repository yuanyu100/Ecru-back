<template>
  <div class="admin-page">
    <div class="detail-page-head">
      <button class="secondary-button" type="button" @click="$router.push('/ai-conversations')">← 返回列表</button>
      <h2>会话详情</h2>
    </div>

    <div v-if="!conversation" class="panel-card">
      <p class="empty-tip">会话信息不可用，请从列表页进入。</p>
    </div>

    <template v-else>
      <div class="stats-grid">
        <article class="stat-card">
          <span class="stat-label">用户</span>
          <strong>{{ conversation.username || '-' }}</strong>
        </article>
        <article class="stat-card">
          <span class="stat-label">场景</span>
          <strong>{{ conversation.context || '-' }}</strong>
        </article>
        <article class="stat-card">
          <span class="stat-label">状态</span>
          <strong>
            <span class="badge" :class="conversation.isActive ? 'badge-green' : 'badge-red'">
              {{ conversation.isActive ? '活跃' : '归档' }}
            </span>
          </strong>
        </article>
        <article class="stat-card">
          <span class="stat-label">消息数</span>
          <strong>{{ conversation.messageCount || 0 }}</strong>
        </article>
      </div>

      <section class="panel-card">
        <div class="panel-head">
          <div>
            <h2>{{ conversation.title || '未命名会话' }}</h2>
            <p class="sub-copy">Session: {{ conversation.sessionId }}</p>
            <p class="sub-copy">更新时间：{{ formatDateTime(conversation.updatedAt) }}</p>
            <p v-if="conversation.metadata && Object.keys(conversation.metadata).length" class="sub-copy">
              元数据：{{ JSON.stringify(conversation.metadata) }}
            </p>
          </div>
        </div>

        <div v-if="messageLoading" class="empty-tip">消息加载中...</div>
        <div v-else-if="messages.length" class="message-list">
          <article v-for="message in messages" :key="message.id" :class="['message-card', message.role]">
            <div class="message-top">
              <strong>{{ roleLabel(message.role) }}</strong>
              <span>{{ formatDateTime(message.createdAt) }}</span>
            </div>
            <p class="message-content">{{ message.content || '-' }}</p>
            <p v-if="message.recommendations?.length" class="message-meta">
              推荐：{{ message.recommendations.map((r) => r.name || r.title || r.clothingId).join(' / ') }}
            </p>
          </article>
        </div>
        <p v-else class="empty-tip">当前会话没有消息记录。</p>
      </section>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { aiChatAdminApi } from '../../api/aiChat';

const route = useRoute();

const conversation = ref(history.state?.conversation || null);
const messages = ref([]);
const messageLoading = ref(false);

const formatDateTime = (value) => (value ? String(value).replace('T', ' ') : '-');

const roleLabel = (role) => {
  if (role === 'user') return '用户';
  if (role === 'assistant') return 'AI';
  if (role === 'system') return '系统';
  return role || '未知';
};

onMounted(async () => {
  const sessionId = route.params.sessionId;
  if (!sessionId) return;
  messageLoading.value = true;
  try {
    const result = await aiChatAdminApi.getConversationMessages(sessionId);
    messages.value = result?.data || [];
  } catch (error) {
    console.error('Load conversation messages failed:', error);
  } finally {
    messageLoading.value = false;
  }
});
</script>

<style scoped>
.sub-copy {
  margin-top: 4px;
  color: #5f6b7a;
  font-size: 12px;
}

.message-list {
  display: grid;
  gap: 12px;
  margin-top: 4px;
}

.message-card {
  padding: 14px;
  border-radius: 16px;
  background: #f7f9fc;
  border: 1px solid #e8edf3;
}

.message-card.user { background: #eef4ff; }
.message-card.assistant { background: #f8fbf3; }

.message-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.message-top span {
  color: #5f6b7a;
  font-size: 12px;
}

.message-content,
.message-meta {
  margin: 10px 0 0;
  white-space: pre-wrap;
}

.message-meta {
  color: #5f6b7a;
  font-size: 12px;
}
</style>
