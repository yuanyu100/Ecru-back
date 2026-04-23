<template>
  <div class="admin-page">
    <section class="panel-card">
      <div class="panel-head">
        <div>
          <h2>用户列表</h2>
          <p class="panel-subtitle">直接对应后端 `/admin/users` 接口。</p>
        </div>
        <div class="toolbar">
          <input v-model.trim="keyword" class="text-input" type="text" placeholder="搜索用户名或邮箱" />
          <button class="primary-button" type="button" @click="loadUsers">查询</button>
        </div>
      </div>

      <div v-if="loading" class="empty-tip">加载中...</div>
      <div v-else-if="users.length" class="table-shell">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>用户名</th>
              <th>邮箱</th>
              <th>昵称</th>
              <th>状态</th>
              <th>注册时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id">
              <td>{{ user.id }}</td>
              <td>{{ user.username }}</td>
              <td>{{ user.email || '-' }}</td>
              <td>{{ user.nickname || '-' }}</td>
              <td>
                <span class="badge" :class="user.status === 1 ? 'badge-green' : 'badge-red'">
                  {{ user.status === 1 ? '启用' : '禁用' }}
                </span>
              </td>
              <td>{{ formatDate(user.createdAt) }}</td>
              <td>
                <button
                  class="secondary-button"
                  type="button"
                  @click="toggleStatus(user)"
                  :disabled="submittingId === user.id"
                >
                  {{ user.status === 1 ? '禁用' : '启用' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else class="empty-tip">暂无用户数据。</p>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { adminApi } from '../../api/admin';

const users = ref([]);
const keyword = ref('');
const loading = ref(false);
const submittingId = ref(null);

const formatDate = (value) => (value ? String(value).replace('T', ' ') : '-');

const loadUsers = async () => {
  loading.value = true;
  try {
    const result = await adminApi.getUsers({ keyword: keyword.value, page: 1, size: 50 });
    users.value = result?.data?.records || [];
  } finally {
    loading.value = false;
  }
};

const toggleStatus = async (user) => {
  submittingId.value = user.id;
  try {
    const nextStatus = user.status === 1 ? 0 : 1;
    const result = await adminApi.updateUserStatus(user.id, nextStatus);
    if (result?.code === 200) {
      user.status = nextStatus;
    }
  } finally {
    submittingId.value = null;
  }
};

onMounted(loadUsers);
</script>
