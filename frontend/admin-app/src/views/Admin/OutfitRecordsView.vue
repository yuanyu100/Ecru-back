<template>
  <div class="admin-page outfit-record-page">
    <div class="stats-grid">
      <article class="stat-card">
        <span class="stat-label">穿搭记录</span>
        <strong>{{ overview.recordTotal }}</strong>
      </article>
      <article class="stat-card">
        <span class="stat-label">收藏记录</span>
        <strong>{{ overview.favoriteTotal }}</strong>
      </article>
      <article class="stat-card">
        <span class="stat-label">用户反馈</span>
        <strong>{{ overview.feedbackTotal }}</strong>
      </article>
      <article class="stat-card accent-card">
        <span class="stat-label">已穿着反馈</span>
        <strong>{{ overview.wornTotal }}</strong>
      </article>
    </div>

    <section class="panel-card">
      <div class="panel-head">
        <h2>搭配记录</h2>
      </div>

      <div class="filter-bar">
        <input v-model.trim="filters.keyword" class="text-input filter-search" type="text" placeholder="标题 / 描述 / 建议" />
        <AdminUserPicker v-model="filters.ownerKeyword" placeholder="用户" @select="handleOwnerSelect" />
        <input v-model.trim="filters.occasion" class="text-input" type="text" placeholder="场景" />
        <select v-model="filters.favorite" class="text-input">
          <option value="">全部收藏状态</option>
          <option value="1">仅收藏</option>
          <option value="0">仅未收藏</option>
        </select>
        <div class="filter-actions">
          <button class="secondary-button" type="button" @click="resetFilters">重置</button>
          <button class="primary-button" type="button" @click="loadRecords">查询</button>
        </div>
      </div>

      <div v-if="loading" class="empty-tip">加载中...</div>

      <div v-else-if="records.length" class="table-shell">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>用户</th>
              <th>穿搭方案</th>
              <th>场景</th>
              <th>单品数</th>
              <th>评分</th>
              <th>收藏</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in records" :key="item.id">
              <td>{{ item.id }}</td>
              <td>
                <strong>{{ item.username || '-' }}</strong>
                <div class="sub-copy">{{ item.nickname || item.email || '-' }}</div>
              </td>
              <td>
                <strong>{{ item.outfitName || '未命名方案' }}</strong>
                <div class="sub-copy clamp-text">{{ item.outfitDescription || item.inputDescription || '-' }}</div>
              </td>
              <td>{{ item.occasion || '-' }}</td>
              <td>{{ item.itemCount || 0 }}</td>
              <td>{{ item.overallRating ?? '-' }}</td>
              <td>
                <span class="badge" :class="item.isFavorite ? 'badge-green' : 'badge-gray'">
                  {{ item.isFavorite ? '已收藏' : '未收藏' }}
                </span>
              </td>
              <td>{{ formatDateTime(item.updatedAt) }}</td>
              <td class="action-cell">
                <button class="secondary-button" type="button" @click="goDetail(item)">查看详情</button>
                <button
                  class="danger-button"
                  type="button"
                  :disabled="deletingId === item.id"
                  @click="removeRecord(item)"
                >
                  {{ deletingId === item.id ? '删除中...' : '删除' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <p v-else class="empty-tip">当前没有匹配的穿搭记录。</p>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { outfitAdminApi } from '../../api/outfit';
import AdminUserPicker from '../../components/AdminUserPicker.vue';

const router = useRouter();

const overview = reactive({ recordTotal: 0, favoriteTotal: 0, feedbackTotal: 0, wornTotal: 0 });
const filters = reactive({ keyword: '', ownerKeyword: '', occasion: '', favorite: '' });

const loading = ref(false);
const deletingId = ref(null);
const records = ref([]);

const formatDateTime = (value) => (value ? String(value).replace('T', ' ') : '-');

const handleOwnerSelect = (user) => { filters.ownerKeyword = user.username || ''; };

const resetFilters = () => {
  filters.keyword = '';
  filters.ownerKeyword = '';
  filters.occasion = '';
  filters.favorite = '';
  loadRecords();
};

const loadOverview = async () => {
  const result = await outfitAdminApi.getOverview();
  if (result?.code === 200 && result.data) Object.assign(overview, result.data);
};

const loadRecords = async () => {
  loading.value = true;
  try {
    const result = await outfitAdminApi.getRecords({
      page: 1, size: 50,
      keyword: filters.keyword,
      ownerKeyword: filters.ownerKeyword,
      occasion: filters.occasion,
      favorite: filters.favorite === '' ? undefined : Number(filters.favorite)
    });
    records.value = result?.data?.list || [];
  } catch (error) {
    console.error('Load outfit records failed:', error);
    alert(error.response?.data?.message || '加载穿搭记录失败');
  } finally {
    loading.value = false;
  }
};

const goDetail = (item) => {
  router.push({ name: 'admin-outfit-record-detail', params: { id: item.id } });
};

const removeRecord = async (item) => {
  if (!window.confirm(`确认删除穿搭记录「${item.outfitName || `#${item.id}`}」吗？相关推荐单品和反馈会一并删除。`)) return;
  deletingId.value = item.id;
  try {
    const result = await outfitAdminApi.deleteRecord(item.id);
    if (result?.code === 200) {
      await Promise.all([loadOverview(), loadRecords()]);
      alert(result.message || '穿搭记录删除成功');
    }
  } catch (error) {
    console.error('Delete outfit record failed:', error);
    alert(error.response?.data?.message || '删除穿搭记录失败');
  } finally {
    deletingId.value = null;
  }
};

onMounted(async () => {
  await Promise.all([loadOverview(), loadRecords()]);
});
</script>

<style scoped>
.outfit-record-page {
  gap: 20px;
}

.accent-card {
  background: linear-gradient(135deg, rgba(14, 116, 144, 0.96), rgba(6, 182, 212, 0.92));
}

.accent-card .stat-label,
.accent-card strong {
  color: #fff;
}

.sub-copy {
  margin-top: 4px;
  color: #5f6b7a;
  font-size: 12px;
}

.clamp-text {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.action-cell {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.badge-gray {
  background: #edf2f7;
  color: #4b5563;
}
</style>
