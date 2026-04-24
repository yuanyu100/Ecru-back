<template>
  <div class="admin-page">
    <div class="stats-grid">
      <article class="stat-card">
        <span class="stat-label">会话总数</span>
        <strong>{{ overview.conversationTotal }}</strong>
      </article>
      <article class="stat-card">
        <span class="stat-label">活跃会话</span>
        <strong>{{ overview.conversationActive }}</strong>
      </article>
      <article class="stat-card">
        <span class="stat-label">消息总数</span>
        <strong>{{ overview.messageTotal }}</strong>
      </article>
      <article class="stat-card">
        <span class="stat-label">有会话用户</span>
        <strong>{{ overview.userConversationUsers }}</strong>
      </article>
    </div>

    <div class="panel-grid chat-grid">
      <section class="panel-card">
        <div class="panel-head stacked-head">
          <div>
            <h2>全局 AI 会话</h2>
            <p class="panel-subtitle">管理员可查看所有用户的会话、消息摘要与当前状态。</p>
          </div>

          <div class="toolbar">
            <input v-model.trim="filters.keyword" class="text-input" type="text" placeholder="搜索标题、sessionId 或消息内容" />
            <input v-model.trim="filters.ownerKeyword" class="text-input" type="text" placeholder="搜索用户名 / 邮箱" />
            <select v-model="filters.context" class="text-input select-input">
              <option value="">全部场景</option>
              <option value="general">general</option>
              <option value="outfit">outfit</option>
              <option value="style">style</option>
            </select>
            <select v-model="filters.active" class="text-input select-input">
              <option value="">全部状态</option>
              <option value="1">仅活跃</option>
              <option value="0">仅归档</option>
            </select>
            <button class="secondary-button" type="button" @click="loadConversations">查询</button>
          </div>
        </div>

        <div v-if="loading" class="empty-tip">加载中...</div>

        <div v-else-if="conversations.length" class="table-shell">
          <table class="data-table">
            <thead>
              <tr>
                <th>用户</th>
                <th>标题</th>
                <th>场景</th>
                <th>状态</th>
                <th>消息数</th>
                <th>最后摘要</th>
                <th>更新时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="item in conversations"
                :key="item.sessionId"
                :class="{ 'active-row': selectedSessionId === item.sessionId }"
                @click="previewConversation(item)"
              >
                <td>
                  <strong>{{ item.username || '-' }}</strong>
                  <div class="sub-copy">{{ item.nickname || item.email || '-' }}</div>
                </td>
                <td>
                  <strong>{{ item.title || '未命名会话' }}</strong>
                  <div class="sub-copy">{{ item.sessionId }}</div>
                </td>
                <td>{{ item.context || '-' }}</td>
                <td>
                  <span class="badge" :class="item.isActive ? 'badge-green' : 'badge-red'">
                    {{ item.isActive ? '活跃' : '归档' }}
                  </span>
                </td>
                <td>{{ item.messageCount || 0 }}</td>
                <td>{{ item.lastMessagePreview || '-' }}</td>
                <td>{{ formatDateTime(item.updatedAt) }}</td>
                <td class="action-cell">
                  <button class="secondary-button" type="button" @click="openMessages(item)">查看消息</button>
                  <button
                    class="danger-button"
                    type="button"
                    :disabled="deletingSessionId === item.sessionId"
                    @click="removeConversation(item)"
                  >
                    {{ deletingSessionId === item.sessionId ? '删除中...' : '删除' }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <p v-else class="empty-tip">当前没有匹配的 AI 会话。</p>
      </section>

      <section class="panel-card">
        <div class="panel-head">
          <div>
            <h2>会话详情</h2>
            <p class="panel-subtitle">点击左侧任意会话后，这里展示元数据和完整消息链路。</p>
          </div>
          <button class="secondary-button" type="button" @click="clearSelection">清空选择</button>
        </div>

        <div v-if="!selectedConversation" class="empty-tip">还没有选中会话。</div>

        <template v-else>
          <div class="detail-meta">
            <article class="mini-card">
              <span>用户</span>
              <strong>{{ selectedConversation.username || '-' }}</strong>
            </article>
            <article class="mini-card">
              <span>场景</span>
              <strong>{{ selectedConversation.context || '-' }}</strong>
            </article>
            <article class="mini-card">
              <span>状态</span>
              <strong>{{ selectedConversation.isActive ? '活跃' : '归档' }}</strong>
            </article>
            <article class="mini-card">
              <span>消息数</span>
              <strong>{{ selectedConversation.messageCount || 0 }}</strong>
            </article>
          </div>

          <div class="conversation-card">
            <h3>{{ selectedConversation.title || '未命名会话' }}</h3>
            <p class="sub-copy">Session: {{ selectedConversation.sessionId }}</p>
            <p class="sub-copy">更新时间：{{ formatDateTime(selectedConversation.updatedAt) }}</p>
            <p class="sub-copy">元数据：{{ formatMetadata(selectedConversation.metadata) }}</p>
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
                推荐：{{ message.recommendations.map((item) => item.name || item.title || item.clothingId).join(' / ') }}
              </p>
              <p v-if="hasMessageMeta(message)" class="message-meta">
                扩展：{{ formatMessageMeta(message) }}
              </p>
            </article>
          </div>
          <p v-else class="empty-tip">当前会话没有消息记录。</p>
        </template>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { aiChatAdminApi } from '../../api/aiChat';

const overview = reactive({
  conversationTotal: 0,
  conversationActive: 0,
  messageTotal: 0,
  userConversationUsers: 0
});

const filters = reactive({
  keyword: '',
  ownerKeyword: '',
  context: '',
  active: ''
});

const loading = ref(false);
const messageLoading = ref(false);
const deletingSessionId = ref('');
const conversations = ref([]);
const selectedConversation = ref(null);
const selectedSessionId = ref('');
const messages = ref([]);

const formatDateTime = (value) => (value ? String(value).replace('T', ' ') : '-');

const roleLabel = (role) => {
  if (role === 'user') {
    return '用户';
  }
  if (role === 'assistant') {
    return 'AI';
  }
  if (role === 'system') {
    return '系统';
  }
  return role || '未知';
};

const formatMetadata = (value) => {
  if (!value || !Object.keys(value).length) {
    return '暂无';
  }
  return JSON.stringify(value, null, 2);
};

const hasMessageMeta = (message) => {
  return Boolean(
    (message.contextSnapshot && Object.keys(message.contextSnapshot).length) ||
      (message.metadata && Object.keys(message.metadata).length)
  );
};

const formatMessageMeta = (message) => {
  const payload = {};
  if (message.contextSnapshot && Object.keys(message.contextSnapshot).length) {
    payload.contextSnapshot = message.contextSnapshot;
  }
  if (message.metadata && Object.keys(message.metadata).length) {
    payload.metadata = message.metadata;
  }
  return JSON.stringify(payload, null, 2);
};

const loadOverview = async () => {
  const result = await aiChatAdminApi.getOverview();
  if (result?.code === 200 && result.data) {
    Object.assign(overview, result.data);
  }
};

const loadConversations = async () => {
  loading.value = true;
  try {
    const result = await aiChatAdminApi.getConversations({
      page: 1,
      size: 50,
      keyword: filters.keyword,
      ownerKeyword: filters.ownerKeyword,
      context: filters.context,
      active: filters.active === '' ? undefined : Number(filters.active)
    });
    conversations.value = result?.data?.list || [];
  } catch (error) {
    console.error('Load admin conversations failed:', error);
    alert(error.response?.data?.message || '加载全局 AI 会话失败');
  } finally {
    loading.value = false;
  }
};

const previewConversation = (item) => {
  selectedConversation.value = item;
  selectedSessionId.value = item.sessionId || '';
};

const openMessages = async (item) => {
  previewConversation(item);
  messageLoading.value = true;
  try {
    const result = await aiChatAdminApi.getConversationMessages(item.sessionId);
    messages.value = result?.data || [];
  } catch (error) {
    console.error('Load admin conversation messages failed:', error);
    alert(error.response?.data?.message || '加载会话消息失败');
  } finally {
    messageLoading.value = false;
  }
};

const clearSelection = () => {
  selectedConversation.value = null;
  selectedSessionId.value = '';
  messages.value = [];
};

const removeConversation = async (item) => {
  if (!window.confirm(`确认删除会话「${item.title || item.sessionId}」吗？`)) {
    return;
  }

  deletingSessionId.value = item.sessionId;
  try {
    const result = await aiChatAdminApi.deleteConversation(item.sessionId);
    if (result?.code === 200) {
      await Promise.all([loadOverview(), loadConversations()]);
      if (selectedSessionId.value === item.sessionId) {
        clearSelection();
      }
      alert(result.message || '会话删除成功');
    }
  } catch (error) {
    console.error('Delete admin conversation failed:', error);
    alert(error.response?.data?.message || '删除会话失败');
  } finally {
    deletingSessionId.value = '';
  }
};

onMounted(async () => {
  await Promise.all([loadOverview(), loadConversations()]);
});
</script>

<style scoped>
.chat-grid {
  grid-template-columns: minmax(0, 1.3fr) minmax(360px, 0.7fr);
}

.stacked-head {
  align-items: flex-start;
  flex-direction: column;
}

.sub-copy {
  margin-top: 4px;
  color: #5f6b7a;
  font-size: 12px;
}

.action-cell {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.active-row {
  background: rgba(29, 78, 216, 0.05);
}

.detail-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.mini-card,
.conversation-card,
.message-card {
  padding: 14px;
  border-radius: 16px;
  background: #f7f9fc;
  border: 1px solid #e8edf3;
}

.mini-card span {
  color: #5f6b7a;
  font-size: 12px;
}

.mini-card strong {
  display: block;
  margin-top: 8px;
}

.conversation-card {
  margin-top: 16px;
}

.conversation-card h3 {
  margin: 0 0 8px;
}

.message-list {
  display: grid;
  gap: 12px;
  margin-top: 16px;
  max-height: 60vh;
  overflow-y: auto;
}

.message-card.user {
  background: #eef4ff;
}

.message-card.assistant {
  background: #f8fbf3;
}

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

@media (max-width: 1100px) {
  .chat-grid {
    grid-template-columns: 1fr;
  }

  .detail-meta {
    grid-template-columns: 1fr;
  }
}
</style>
