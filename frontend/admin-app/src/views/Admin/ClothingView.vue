<template>
  <div class="admin-page">
    <section class="panel-card">
      <div class="panel-head">
        <div>
          <h2>当前账号衣物台账</h2>
          <p class="panel-subtitle">后端目前按当前登录账号查询，不是全局衣柜管理。</p>
        </div>
        <div class="toolbar">
          <input v-model.trim="keyword" class="text-input" type="text" placeholder="搜索名称/分类/颜色" />
          <button class="secondary-button" type="button" @click="loadClothings">刷新</button>
        </div>
      </div>

      <form class="inline-form" @submit.prevent="createItem">
        <input v-model.trim="draft.name" class="text-input" type="text" placeholder="名称" required />
        <input v-model.trim="draft.category" class="text-input" type="text" placeholder="分类" />
        <input v-model.trim="draft.primaryColor" class="text-input" type="text" placeholder="主色" />
        <button class="primary-button" type="submit" :disabled="creating">
          {{ creating ? '创建中...' : '新增衣物' }}
        </button>
      </form>

      <div v-if="loading" class="empty-tip">加载中...</div>
      <div v-else-if="items.length" class="table-shell">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>名称</th>
              <th>分类</th>
              <th>主色</th>
              <th>风格标签</th>
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
              <td>{{ item.styleTags?.join(' / ') || '-' }}</td>
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
      <p v-else class="empty-tip">当前账号还没有衣物记录。</p>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { clothingApi } from '../../api/clothing';

const items = ref([]);
const keyword = ref('');
const loading = ref(false);
const creating = ref(false);
const deletingId = ref(null);
const draft = reactive({
  name: '',
  category: '',
  primaryColor: ''
});

const formatDate = (value) => (value ? String(value).replace('T', ' ') : '-');

const loadClothings = async () => {
  loading.value = true;
  try {
    const result = await clothingApi.getClothings({ page: 1, size: 50, keyword: keyword.value });
    items.value = result?.data?.list || [];
  } finally {
    loading.value = false;
  }
};

const createItem = async () => {
  creating.value = true;
  try {
    const result = await clothingApi.createClothing({
      name: draft.name,
      category: draft.category || null,
      primaryColor: draft.primaryColor || null
    });

    if (result?.code === 200) {
      draft.name = '';
      draft.category = '';
      draft.primaryColor = '';
      await loadClothings();
    }
  } finally {
    creating.value = false;
  }
};

const removeItem = async (id) => {
  deletingId.value = id;
  try {
    const result = await clothingApi.deleteClothing(id, false);
    if (result?.code === 200) {
      items.value = items.value.filter((item) => item.id !== id);
    }
  } finally {
    deletingId.value = null;
  }
};

onMounted(loadClothings);
</script>
