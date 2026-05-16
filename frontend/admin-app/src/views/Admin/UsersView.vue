<template>
  <div class="admin-page">
    <section class="panel-card">
      <div class="panel-head">
        <h2>用户管理</h2>
      </div>
      <div class="filter-bar">
        <input
          v-model.trim="keyword"
          class="text-input filter-search"
          type="text"
          placeholder="按用户名或邮箱搜索"
        />
        <div class="filter-actions">
          <button class="secondary-button" type="button" @click="keyword = ''; loadUsers()">重置</button>
          <button class="primary-button" type="button" @click="loadUsers">搜索</button>
        </div>
      </div>

      <div v-if="loading" class="empty-tip">正在加载用户数据...</div>
      <div v-else-if="users.length" class="table-shell">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>用户名</th>
              <th>邮箱</th>
              <th>昵称</th>
              <th>角色</th>
              <th>状态</th>
              <th>最后登录</th>
              <th>注册时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="user in users"
              :key="user.id || user.userId"
              class="clickable-row"
              @click="goDetail(user)"
            >
              <td>{{ user.id || user.userId }}</td>
              <td>{{ user.username || '-' }}</td>
              <td>{{ user.email || '-' }}</td>
              <td>{{ user.nickname || '-' }}</td>
              <td>{{ formatRole(user.role) }}</td>
              <td>
                <span class="badge" :class="user.status === 1 ? 'badge-green' : 'badge-red'">
                  {{ user.status === 1 ? '正常' : '禁用' }}
                </span>
              </td>
              <td>{{ formatDate(user.lastLoginAt) }}</td>
              <td>{{ formatDate(user.createdAt) }}</td>
              <td class="action-cell">
                <button class="secondary-button" type="button" @click.stop="goDetail(user)">
                  查看详情
                </button>
                <button
                  class="secondary-button"
                  type="button"
                  @click.stop="toggleStatus(user)"
                  :disabled="submittingId === (user.id || user.userId)"
                >
                  {{ user.status === 1 ? '禁用' : '启用' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else class="empty-tip">暂无可展示的用户数据。</p>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { adminApi } from '../../api/admin';

const router = useRouter();
const users = ref([]);
const keyword = ref('');
const loading = ref(false);
const submittingId = ref(null);

const formatDate = (value) => (value ? String(value).replace('T', ' ') : '-');
const formatRole = (role) => (String(role || '').toUpperCase().includes('ADMIN') ? '管理员' : '普通用户');

const loadUsers = async () => {
  loading.value = true;
  try {
    const result = await adminApi.getUsers({ keyword: keyword.value, page: 1, size: 50 });
    users.value = result?.data?.list || result?.data?.records || [];
  } finally {
    loading.value = false;
  }
};

const goDetail = (user) => {
  const userId = user.id || user.userId;
  if (!userId) return;
  router.push({ name: 'admin-user-detail', params: { id: userId } });
};

const toggleStatus = async (user) => {
  const userId = user.id || user.userId;
  submittingId.value = userId;
  try {
    const nextStatus = user.status === 1 ? 0 : 1;
    const result = await adminApi.updateUserStatus(userId, nextStatus);
    if (result?.code === 200) {
      user.status = nextStatus;
    }
  } finally {
    submittingId.value = null;
  }
};

onMounted(loadUsers);
</script>

<style scoped>
.action-cell {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.clickable-row {
  cursor: pointer;
}
</style>
