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

    <div class="panel-grid knowledge-grid">
      <section class="panel-card">
        <div class="panel-head stacked-head">
          <div>
            <h2>知识库列表</h2>
            <p class="panel-subtitle">支持按类型维护面料、洗护标和搭配指南。</p>
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
            <button class="primary-button" type="button" @click="createNew">新建{{ currentTabLabel }}</button>
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
                <td>
                  <button class="secondary-button" type="button" @click="editItem(item)">编辑</button>
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
                <td>
                  <button class="secondary-button" type="button" @click="editItem(item)">编辑</button>
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
                <td>
                  <button class="secondary-button" type="button" @click="editItem(item)">编辑</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <p v-else class="empty-tip">当前筛选下没有知识数据。</p>
      </section>

      <section class="panel-card">
        <div class="panel-head">
          <div>
            <h2>{{ editingId ? `编辑${currentTabLabel}` : `新建${currentTabLabel}` }}</h2>
            <p class="panel-subtitle">保存后会直接写入知识库表，可立刻被用户端搜索与问答命中。</p>
          </div>
          <button class="secondary-button" type="button" @click="resetDraft">清空</button>
        </div>

        <form class="knowledge-form" @submit.prevent="submitDraft">
          <template v-if="activeTab === 'fabric'">
            <div class="form-grid">
              <label>
                <span>面料名称</span>
                <input v-model.trim="draft.name" class="text-input" type="text" required />
              </label>
              <label>
                <span>面料类型</span>
                <input v-model.trim="draft.fabricType" class="text-input" type="text" placeholder="例如：天然纤维" />
              </label>
              <label class="wide">
                <span>别名</span>
                <input v-model.trim="draft.alias" class="text-input" type="text" placeholder="用英文逗号分隔" />
              </label>
              <label class="wide">
                <span>摘要</span>
                <textarea v-model.trim="draft.summary" class="text-input textarea-input" rows="3"></textarea>
              </label>
              <label class="wide">
                <span>特性说明</span>
                <textarea v-model.trim="draft.properties" class="text-input textarea-input" rows="4"></textarea>
              </label>
              <label class="wide">
                <span>洗护建议</span>
                <textarea v-model.trim="draft.careGuide" class="text-input textarea-input" rows="4"></textarea>
              </label>
              <label>
                <span>适用季节</span>
                <input v-model.trim="draft.suitableSeasons" class="text-input" type="text" placeholder="春,夏" />
              </label>
              <label>
                <span>适用场景</span>
                <input v-model.trim="draft.suitableOccasions" class="text-input" type="text" placeholder="通勤,日常" />
              </label>
              <label>
                <span>保暖分</span>
                <input v-model.number="draft.warmthScore" class="text-input" type="number" min="0" max="100" />
              </label>
              <label>
                <span>透气分</span>
                <input v-model.number="draft.breathabilityScore" class="text-input" type="number" min="0" max="100" />
              </label>
              <label>
                <span>舒适分</span>
                <input v-model.number="draft.comfortScore" class="text-input" type="number" min="0" max="100" />
              </label>
              <label>
                <span>耐用分</span>
                <input v-model.number="draft.durabilityScore" class="text-input" type="number" min="0" max="100" />
              </label>
              <label class="wide">
                <span>关键词</span>
                <input v-model.trim="draft.keywords" class="text-input" type="text" placeholder="用英文逗号分隔" />
              </label>
            </div>
          </template>

          <template v-else-if="activeTab === 'guide'">
            <div class="form-grid">
              <label>
                <span>标题</span>
                <input v-model.trim="draft.title" class="text-input" type="text" required />
              </label>
              <label>
                <span>指南类型</span>
                <input v-model.trim="draft.guideType" class="text-input" type="text" placeholder="例如：通勤穿搭" />
              </label>
              <label class="wide">
                <span>副标题</span>
                <input v-model.trim="draft.subtitle" class="text-input" type="text" />
              </label>
              <label class="wide">
                <span>摘要</span>
                <textarea v-model.trim="draft.summary" class="text-input textarea-input" rows="3"></textarea>
              </label>
              <label class="wide">
                <span>正文</span>
                <textarea v-model.trim="draft.content" class="text-input textarea-input" rows="8"></textarea>
              </label>
              <label>
                <span>作者</span>
                <input v-model.trim="draft.author" class="text-input" type="text" />
              </label>
              <label>
                <span>发布日期</span>
                <input v-model="draft.publishDate" class="text-input" type="date" />
              </label>
              <label class="wide">
                <span>标签</span>
                <input v-model.trim="draft.tags" class="text-input" type="text" placeholder="用英文逗号分隔" />
              </label>
              <label class="wide">
                <span>封面图 URL</span>
                <input v-model.trim="draft.coverImageUrl" class="text-input" type="text" />
              </label>
              <label class="wide">
                <span>封面说明</span>
                <input v-model.trim="draft.coverImageCaption" class="text-input" type="text" />
              </label>
              <label class="wide">
                <span>关键词</span>
                <input v-model.trim="draft.keywords" class="text-input" type="text" placeholder="用英文逗号分隔" />
              </label>
            </div>
          </template>

          <template v-else>
            <div class="form-grid">
              <label>
                <span>洗护标编码</span>
                <input v-model.trim="draft.symbolCode" class="text-input" type="text" required />
              </label>
              <label>
                <span>洗护标名称</span>
                <input v-model.trim="draft.symbolName" class="text-input" type="text" required />
              </label>
              <label>
                <span>分类</span>
                <input v-model.trim="draft.category" class="text-input" type="text" required />
              </label>
              <label class="wide">
                <span>说明</span>
                <textarea v-model.trim="draft.instruction" class="text-input textarea-input" rows="3" required></textarea>
              </label>
              <label class="wide">
                <span>解释</span>
                <textarea v-model.trim="draft.explanation" class="text-input textarea-input" rows="4"></textarea>
              </label>
              <label>
                <span>建议做法</span>
                <input v-model.trim="draft.doText" class="text-input" type="text" />
              </label>
              <label>
                <span>避免操作</span>
                <input v-model.trim="draft.dontText" class="text-input" type="text" />
              </label>
              <label class="wide">
                <span>关键词</span>
                <input v-model.trim="draft.keywords" class="text-input" type="text" placeholder="用英文逗号分隔" />
              </label>
            </div>
          </template>

          <div class="form-grid compact-grid">
            <label>
              <span>来源</span>
              <input v-model.trim="draft.source" class="text-input" type="text" placeholder="默认 admin-console" />
            </label>
            <label>
              <span>状态</span>
              <select v-model="draft.active" class="text-input select-input">
                <option :value="true">启用</option>
                <option :value="false">停用</option>
              </select>
            </label>
          </div>

          <div class="form-actions">
            <button class="secondary-button" type="button" @click="resetDraft">重置</button>
            <button class="primary-button" type="submit" :disabled="saving">
              {{ saving ? '保存中...' : editingId ? '保存修改' : '创建知识' }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { knowledgeAdminApi } from '../../api/knowledge';

const tabs = [
  { value: 'fabric', label: '面料' },
  { value: 'guide', label: '指南' },
  { value: 'care-label', label: '洗护' }
];

const defaultDrafts = {
  fabric: {
    name: '',
    alias: '',
    fabricType: '',
    warmthScore: 0,
    breathabilityScore: 0,
    comfortScore: 0,
    durabilityScore: 0,
    summary: '',
    properties: '',
    careGuide: '',
    suitableSeasons: '',
    suitableOccasions: '',
    keywords: '',
    source: '',
    active: true
  },
  guide: {
    title: '',
    subtitle: '',
    guideType: '',
    summary: '',
    content: '',
    author: '',
    publishDate: '',
    tags: '',
    coverImageUrl: '',
    coverImageCaption: '',
    keywords: '',
    source: '',
    active: true
  },
  'care-label': {
    symbolCode: '',
    symbolName: '',
    category: '',
    instruction: '',
    explanation: '',
    doText: '',
    dontText: '',
    keywords: '',
    source: '',
    active: true
  }
};

const overview = reactive({
  fabricTotal: 0,
  fabricActive: 0,
  guideTotal: 0,
  guideActive: 0,
  careLabelTotal: 0,
  careLabelActive: 0
});

const activeTab = ref('fabric');
const editingId = ref(null);
const loading = ref(false);
const saving = ref(false);
const fabrics = ref([]);
const guides = ref([]);
const careLabels = ref([]);
const filters = reactive({
  keyword: '',
  active: ''
});
const draft = reactive({ ...defaultDrafts.fabric });

const currentTabLabel = computed(() => {
  return tabs.find((item) => item.value === activeTab.value)?.label || '知识';
});

const currentTotal = computed(() => {
  if (activeTab.value === 'guide') {
    return guides.value.length;
  }
  if (activeTab.value === 'care-label') {
    return careLabels.value.length;
  }
  return fabrics.value.length;
});

const formatDateTime = (value) => (value ? String(value).replace('T', ' ') : '-');
const formatDate = (value) => (value ? String(value).slice(0, 10) : '-');

const assignDraft = (nextDraft) => {
  Object.keys(draft).forEach((key) => {
    delete draft[key];
  });
  Object.assign(draft, nextDraft);
};

const resetDraft = () => {
  editingId.value = null;
  assignDraft({ ...defaultDrafts[activeTab.value] });
};

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
  resetDraft();
  await loadCurrentTab();
};

const createNew = () => {
  resetDraft();
};

const editItem = (item) => {
  if (activeTab.value === 'fabric') {
    editingId.value = item.fabricId;
    assignDraft({
      name: item.name || '',
      alias: item.alias || '',
      fabricType: item.fabricType || '',
      warmthScore: item.warmthScore ?? 0,
      breathabilityScore: item.breathabilityScore ?? 0,
      comfortScore: item.comfortScore ?? 0,
      durabilityScore: item.durabilityScore ?? 0,
      summary: item.summary || '',
      properties: item.properties || '',
      careGuide: item.careGuide || '',
      suitableSeasons: item.suitableSeasons || '',
      suitableOccasions: item.suitableOccasions || '',
      keywords: item.keywords || '',
      source: item.source || '',
      active: Boolean(item.isActive)
    });
    return;
  }

  if (activeTab.value === 'guide') {
    editingId.value = item.guideId;
    assignDraft({
      title: item.title || '',
      subtitle: item.subtitle || '',
      guideType: item.guideType || '',
      summary: item.summary || '',
      content: item.content || '',
      author: item.author || '',
      publishDate: formatDate(item.publishDate) === '-' ? '' : formatDate(item.publishDate),
      tags: item.tags || '',
      coverImageUrl: item.coverImageUrl || '',
      coverImageCaption: item.coverImageCaption || '',
      keywords: item.keywords || '',
      source: item.source || '',
      active: Boolean(item.isActive)
    });
    return;
  }

  editingId.value = item.careLabelId;
  assignDraft({
    symbolCode: item.symbolCode || '',
    symbolName: item.symbolName || '',
    category: item.category || '',
    instruction: item.instruction || '',
    explanation: item.explanation || '',
    doText: item.doText || '',
    dontText: item.dontText || '',
    keywords: item.keywords || '',
    source: item.source || '',
    active: Boolean(item.isActive)
  });
};

const submitDraft = async () => {
  saving.value = true;
  try {
    if (activeTab.value === 'fabric') {
      const payload = {
        ...draft,
        active: Boolean(draft.active)
      };
      const result = editingId.value
        ? await knowledgeAdminApi.updateFabric(editingId.value, payload)
        : await knowledgeAdminApi.createFabric(payload);
      if (result?.code === 200) {
        alert(result.message || '面料知识保存成功');
      }
    } else if (activeTab.value === 'guide') {
      const payload = {
        ...draft,
        active: Boolean(draft.active)
      };
      const result = editingId.value
        ? await knowledgeAdminApi.updateGuide(editingId.value, payload)
        : await knowledgeAdminApi.createGuide(payload);
      if (result?.code === 200) {
        alert(result.message || '指南知识保存成功');
      }
    } else {
      const payload = {
        ...draft,
        active: Boolean(draft.active)
      };
      const result = editingId.value
        ? await knowledgeAdminApi.updateCareLabel(editingId.value, payload)
        : await knowledgeAdminApi.createCareLabel(payload);
      if (result?.code === 200) {
        alert(result.message || '洗护知识保存成功');
      }
    }

    await Promise.all([loadOverview(), loadCurrentTab()]);
    resetDraft();
  } catch (error) {
    console.error('Save knowledge data failed:', error);
    alert(error.response?.data?.message || '保存知识库数据失败');
  } finally {
    saving.value = false;
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

.knowledge-grid {
  grid-template-columns: minmax(0, 1.2fr) minmax(360px, 0.8fr);
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

.accent-card {
  background: linear-gradient(135deg, rgba(29, 78, 216, 0.96), rgba(59, 130, 246, 0.92));
}

.accent-card .stat-label,
.accent-card strong,
.accent-card .card-meta {
  color: #fff;
}

.knowledge-form,
.form-grid {
  display: grid;
  gap: 14px;
}

.form-grid label {
  display: grid;
  gap: 8px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.wide {
  grid-column: 1 / -1;
}

.textarea-input {
  min-height: 96px;
  padding: 12px 14px;
  resize: vertical;
}

.select-input {
  padding-right: 36px;
}

.compact-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

@media (max-width: 1100px) {
  .knowledge-grid {
    grid-template-columns: 1fr;
  }
}
</style>
