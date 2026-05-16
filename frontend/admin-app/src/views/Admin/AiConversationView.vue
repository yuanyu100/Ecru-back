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

    <section class="panel-card">
      <div class="panel-head">
        <h2>AI 会话</h2>
      </div>

      <div class="filter-bar">
        <input v-model.trim="filters.keyword" class="text-input filter-search" type="text" placeholder="标题 / Session / 消息" />
        <AdminUserPicker v-model="filters.ownerKeyword" placeholder="用户" @select="handleOwnerSelect" />
        <select v-model="filters.context" class="text-input">
          <option value="">全部场景</option>
          <option value="general">general</option>
          <option value="outfit">outfit</option>
          <option value="style">style</option>
        </select>
        <select v-model="filters.active" class="text-input">
          <option value="">全部状态</option>
          <option value="1">仅活跃</option>
          <option value="0">仅归档</option>
        </select>
        <div class="filter-actions">
          <button class="secondary-button" type="button" @click="resetFilters">重置</button>
          <button class="primary-button" type="button" @click="loadConversations">查询</button>
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
            <tr v-for="item in conversations" :key="item.sessionId">
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
              <td><div class="preview-clamp">{{ item.lastMessagePreview || '-' }}</div></td>
              <td>{{ formatDateTime(item.updatedAt) }}</td>
              <td class="action-cell">
                <button class="secondary-button" type="button" @click="goDetail(item)">查看详情</button>
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { aiChatAdminApi } from '../../api/aiChat';
import AdminUserPicker from '../../components/AdminUserPicker.vue';

const router = useRouter();

const overview = reactive({
  conversationTotal: 0,
  conversationActive: 0,
  messageTotal: 0,
  userConversationUsers: 0
});

const filters = reactive({ keyword: '', ownerKeyword: '', context: '', active: '' });

const loading = ref(false);
const deletingSessionId = ref('');
const conversations = ref([]);

const formatDateTime = (value) => (value ? String(value).replace('T', ' ') : '-');

const handleOwnerSelect = (user) => { filters.ownerKeyword = user.username || ''; };

const resetFilters = () => {
  filters.keyword = '';
  filters.ownerKeyword = '';
  filters.context = '';
  filters.active = '';
  loadConversations();
};

const loadOverview = async () => {
  const result = await aiChatAdminApi.getOverview();
  if (result?.code === 200 && result.data) Object.assign(overview, result.data);
};

const loadConversations = async () => {
  loading.value = true;
  try {
    const result = await aiChatAdminApi.getConversations({
      page: 1, size: 50,
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

const goDetail = (item) => {
  router.push({ name: 'admin-ai-conversation-detail', params: { sessionId: item.sessionId }, state: { conversation: item } });
};

const removeConversation = async (item) => {
  if (!window.confirm(`确认删除会话「${item.title || item.sessionId}」吗？`)) return;
  deletingSessionId.value = item.sessionId;
  try {
    const result = await aiChatAdminApi.deleteConversation(item.sessionId);
    if (result?.code === 200) {
      await Promise.all([loadOverview(), loadConversations()]);
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

.preview-clamp {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  max-width: 200px;
  font-size: 13px;
  color: #5f6b7a;
}
</style>
