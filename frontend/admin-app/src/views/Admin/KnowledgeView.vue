<template>
  <div class="admin-page knowledge-page">
    <div class="stats-grid">
      <article class="stat-card">
        <span class="stat-label">面料知识</span>
        <strong>{{ overview.fabricTotal }}</strong>
        <p class="card-meta">启用 {{ overview.fabricActive }}</p>
      </article>
      <article class="stat-card">
        <span class="stat-label">穿搭指南</span>
        <strong>{{ overview.guideTotal }}</strong>
        <p class="card-meta">启用 {{ overview.guideActive }}</p>
      </article>
      <article class="stat-card">
        <span class="stat-label">洗护知识</span>
        <strong>{{ overview.careLabelTotal }}</strong>
        <p class="card-meta">启用 {{ overview.careLabelActive }}</p>
      </article>
      <article class="stat-card accent-card">
        <span class="stat-label">当前筛选结果</span>
        <strong>{{ currentTotal }}</strong>
        <p class="card-meta">{{ currentTabLabel }}</p>
      </article>
    </div>

    <section class="panel-card">
      <div class="panel-head stacked-head">
        <div>
          <h2>知识库列表</h2>
          <p class="panel-subtitle">列表页只负责筛选、浏览和进入新建/编辑页，不再把大表单塞在同一页。</p>
        </div>

        <div class="toolbar">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            :class="['tab-button', activeTab === tab.value ? 'active' : '']"
            type="button"
            @click="switchTab(tab.value)"
          >
            {{ tab.label }}
          </button>
        </div>

        <div class="toolbar">
          <input
            v-model.trim="filters.keyword"
            class="text-input"
            type="text"
            :placeholder="`搜索${currentTabLabel}关键词`"
          />
          <select v-model="filters.active" class="text-input select-input">
            <option value="">全部状态</option>
            <option value="1">仅启用</option>
            <option value="0">仅停用</option>
          </select>
          <button class="secondary-button" type="button" @click="loadCurrentTab">查询</button>
          <button class="primary-button" type="button" @click="goCreate">新建 {{ currentTabLabel }}</button>
        </div>
      </div>

      <div v-if="loading" class="empty-tip">加载中...</div>

      <div v-else-if="activeTab === 'fabric' && fabrics.length" class="table-shell">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>名称</th>
              <th>类型</th>
              <th>摘要</th>
              <th>关键词</th>
              <th>状态</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in fabrics" :key="item.fabricId">
              <td>{{ item.fabricId }}</td>
              <td>{{ item.name }}</td>
              <td>{{ item.fabricType || '-' }}</td>
              <td>{{ item.summary || '-' }}</td>
              <td>{{ item.keywords || '-' }}</td>
              <td>
                <span class="badge" :class="item.isActive ? 'badge-green' : 'badge-red'">
                  {{ item.isActive ? '启用' : '停用' }}
                </span>
              </td>
              <td>{{ formatDateTime(item.updatedAt) }}</td>
              <td class="action-cell">
                <button class="secondary-button" type="button" @click="goEdit(item)">编辑</button>
                <button
                  class="danger-button"
                  type="button"
                  :disabled="deletingId === item.fabricId"
                  @click="removeItem(item)"
                >
                  {{ deletingId === item.fabricId ? '删除中...' : '删除' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else-if="activeTab === 'guide' && guides.length" class="table-shell">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>标题</th>
              <th>类型</th>
              <th>摘要</th>
              <th>发布日期</th>
              <th>状态</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in guides" :key="item.guideId">
              <td>{{ item.guideId }}</td>
              <td>{{ item.title }}</td>
              <td>{{ item.guideType || '-' }}</td>
              <td>{{ item.summary || '-' }}</td>
              <td>{{ formatDate(item.publishDate) }}</td>
              <td>
                <span class="badge" :class="item.isActive ? 'badge-green' : 'badge-red'">
                  {{ item.isActive ? '启用' : '停用' }}
                </span>
              </td>
              <td>{{ formatDateTime(item.updatedAt) }}</td>
              <td class="action-cell">
                <button class="secondary-button" type="button" @click="goEdit(item)">编辑</button>
                <button
                  class="danger-button"
                  type="button"
                  :disabled="deletingId === item.guideId"
                  @click="removeItem(item)"
                >
                  {{ deletingId === item.guideId ? '删除中...' : '删除' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else-if="activeTab === 'care-label' && careLabels.length" class="table-shell">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>编码</th>
              <th>名称</th>
              <th>分类</th>
              <th>说明</th>
              <th>状态</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in careLabels" :key="item.careLabelId">
              <td>{{ item.careLabelId }}</td>
              <td>{{ item.symbolCode }}</td>
              <td>{{ item.symbolName }}</td>
              <td>{{ item.category }}</td>
              <td>{{ item.instruction }}</td>
              <td>
                <span class="badge" :class="item.isActive ? 'badge-green' : 'badge-red'">
                  {{ item.isActive ? '启用' : '停用' }}
                </span>
              </td>
              <td>{{ formatDateTime(item.updatedAt) }}</td>
              <td class="action-cell">
                <button class="secondary-button" type="button" @click="goEdit(item)">编辑</button>
                <button
                  class="danger-button"
                  type="button"
                  :disabled="deletingId === item.careLabelId"
                  @click="removeItem(item)"
                >
                  {{ deletingId === item.careLabelId ? '删除中...' : '删除' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <p v-else class="empty-tip">当前筛选下没有知识数据。</p>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { knowledgeAdminApi } from '../../api/knowledge';

const router = useRouter();

const tabs = [
  { value: 'fabric', label: '面料' },
  { value: 'guide', label: '指南' },
  { value: 'care-label', label: '洗护' }
];

const overview = reactive({
  fabricTotal: 0,
  fabricActive: 0,
  guideTotal: 0,
  guideActive: 0,
  careLabelTotal: 0,
  careLabelActive: 0
});

const activeTab = ref('fabric');
const loading = ref(false);
const deletingId = ref(null);
const fabrics = ref([]);
const guides = ref([]);
const careLabels = ref([]);
const filters = reactive({
  keyword: '',
  active: ''
});

const currentTabLabel = computed(() => tabs.find((item) => item.value === activeTab.value)?.label || '知识');

const currentTotal = computed(() => {
  if (activeTab.value === 'guide') return guides.value.length;
  if (activeTab.value === 'care-label') return careLabels.value.length;
  return fabrics.value.length;
});

const formatDateTime = (value) => (value ? String(value).replace('T', ' ') : '-');
const formatDate = (value) => (value ? String(value).slice(0, 10) : '-');

const loadOverview = async () => {
  const result = await knowledgeAdminApi.getOverview();
  if (result?.code === 200 && result.data) {
    Object.assign(overview, result.data);
  }
};

const loadCurrentTab = async () => {
  loading.value = true;
  try {
    const params = {
      page: 1,
      size: 50,
      keyword: filters.keyword,
      active: filters.active === '' ? undefined : Number(filters.active)
    };

    if (activeTab.value === 'fabric') {
      const result = await knowledgeAdminApi.getFabrics(params);
      fabrics.value = result?.data?.list || [];
      return;
    }

    if (activeTab.value === 'guide') {
      const result = await knowledgeAdminApi.getGuides(params);
      guides.value = result?.data?.list || [];
      return;
    }

    const result = await knowledgeAdminApi.getCareLabels(params);
    careLabels.value = result?.data?.list || [];
  } catch (error) {
    console.error('Load knowledge data failed:', error);
    alert(error.response?.data?.message || '加载知识库数据失败');
  } finally {
    loading.value = false;
  }
};

const switchTab = async (tab) => {
  activeTab.value = tab;
  await loadCurrentTab();
};

const goCreate = () => {
  router.push(`/knowledge/${activeTab.value}/create`);
};

const goEdit = (item) => {
  const id =
    activeTab.value === 'fabric' ? item.fabricId : activeTab.value === 'guide' ? item.guideId : item.careLabelId;
  router.push(`/knowledge/${activeTab.value}/${id}/edit`);
};

const removeItem = async (item) => {
  const itemId =
    activeTab.value === 'fabric' ? item.fabricId : activeTab.value === 'guide' ? item.guideId : item.careLabelId;
  const itemTitle =
    activeTab.value === 'fabric' ? item.name : activeTab.value === 'guide' ? item.title : item.symbolName;

  if (!window.confirm(`确认删除「${itemTitle || '该条目'}」吗？`)) {
    return;
  }

  deletingId.value = itemId;
  try {
    let result;
    if (activeTab.value === 'fabric') {
      result = await knowledgeAdminApi.deleteFabric(itemId);
    } else if (activeTab.value === 'guide') {
      result = await knowledgeAdminApi.deleteGuide(itemId);
    } else {
      result = await knowledgeAdminApi.deleteCareLabel(itemId);
    }

    if (result?.code === 200) {
      await Promise.all([loadOverview(), loadCurrentTab()]);
      alert(result.message || '知识条目删除成功');
    }
  } catch (error) {
    console.error('Delete knowledge item failed:', error);
    alert(error.response?.data?.message || '删除知识条目失败');
  } finally {
    deletingId.value = null;
  }
};

onMounted(async () => {
  await Promise.all([loadOverview(), loadCurrentTab()]);
});
</script>

<style scoped>
.knowledge-page {
  gap: 20px;
}

.stacked-head {
  align-items: flex-start;
  flex-direction: column;
}

.tab-button {
  min-height: 40px;
  padding: 0 14px;
  border: 0;
  border-radius: 12px;
  background: #e7edf7;
  color: #1e3a5f;
  cursor: pointer;
}

.tab-button.active {
  background: #1d4ed8;
  color: #fff;
}

.card-meta {
  margin: 8px 0 0;
  color: #5f6b7a;
}

.action-cell {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.accent-card {
  background: linear-gradient(135deg, rgba(29, 78, 216, 0.96), rgba(59, 130, 246, 0.92));
}

.accent-card .stat-label,
.accent-card strong,
.accent-card .card-meta {
  color: #fff;
}
</style>
