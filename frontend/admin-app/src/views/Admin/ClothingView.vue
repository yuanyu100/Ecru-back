<template>
  <div class="admin-page">
    <section class="panel-card">
      <div class="filter-bar">
        <input
          v-model.trim="filters.keyword"
          class="text-input filter-search"
          type="text"
          placeholder="名称 / 分类 / 颜色"
        />
        <AdminUserPicker
          v-if="isAdmin"
          v-model="filters.ownerKeyword"
          placeholder="用户"
          @select="handleOwnerSelect"
        />
        <div class="filter-actions">
          <button class="secondary-button" type="button" @click="resetFilters">重置</button>
          <button class="primary-button" type="button" @click="loadClothings">查询</button>
        </div>
      </div>

      <div v-if="loading" class="empty-tip">加载中...</div>
      <div v-else-if="items.length" class="table-shell">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>名称</th>
              <th>分类</th>
              <th>主色</th>
              <th v-if="isAdmin">所属用户</th>
              <th>标签</th>
              <th>来源</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in items" :key="item.id">
              <td>{{ item.id }}</td>
              <td>{{ item.name }}</td>
              <td>{{ item.category || '-' }}</td>
              <td>{{ item.primaryColor || '-' }}</td>
              <td v-if="isAdmin">
                {{ item.ownerUsername || '-' }}
                <span v-if="item.ownerNickname"> / {{ item.ownerNickname }}</span>
              </td>
              <td>{{ renderTags(item.styleTags) }}</td>
              <td>{{ item.sourceType || '-' }}</td>
              <td>{{ formatDate(item.createdAt) }}</td>
              <td>
                <button
                  class="danger-button"
                  type="button"
                  @click="removeItem(item.id)"
                  :disabled="deletingId === item.id"
                >
                  删除
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else class="empty-tip">当前没有可显示的衣物记录。</p>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { clothingApi } from '../../api/clothing';
import { authApi } from '../../api/auth';
import AdminUserPicker from '../../components/AdminUserPicker.vue';
import { isAdminUser } from '../../utils/adminRole';

const currentUser = authApi.getCurrentUser();
const isAdmin = isAdminUser(currentUser);
const items = ref([]);
const loading = ref(false);
const deletingId = ref(null);
const filters = reactive({
  keyword: '',
  ownerKeyword: '',
  userId: undefined
});

const formatDate = (value) => (value ? String(value).replace('T', ' ') : '-');

const renderTags = (value) => {
  if (!value) {
    return '-';
  }

  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) && parsed.length ? parsed.join(' / ') : '-';
  } catch {
    return value;
  }
};

const handleOwnerSelect = (user) => {
  filters.ownerKeyword = user.username || '';
  filters.userId = user.userId || user.id || undefined;
};

const resetFilters = () => {
  filters.keyword = '';
  filters.ownerKeyword = '';
  filters.userId = undefined;
  loadClothings();
};

const loadClothings = async () => {
  loading.value = true;
  try {
    const result = isAdmin
      ? await clothingApi.getAdminClothings({
          page: 1,
          size: 50,
          userId: filters.userId,
          keyword: filters.keyword,
          ownerKeyword: filters.ownerKeyword
        })
      : await clothingApi.getClothings({
          page: 1,
          size: 50,
          keyword: filters.keyword
        });

    items.value = result?.data?.list || [];
  } finally {
    loading.value = false;
  }
};

const removeItem = async (id) => {
  deletingId.value = id;
  try {
    const result = isAdmin
      ? await clothingApi.deleteAdminClothing(id, false)
      : await clothingApi.deleteClothing(id, false);

    if (result?.code === 200) {
      items.value = items.value.filter((item) => item.id !== id);
    }
  } finally {
    deletingId.value = null;
  }
};

onMounted(loadClothings);
</script>
