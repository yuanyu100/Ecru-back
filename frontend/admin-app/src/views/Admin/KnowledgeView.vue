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
      <section ref="editorSectionRef" :class="['panel-card', editorHighlight ? 'editor-highlight' : '']">
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
            <button class="primary-button" type="button" @click="createNew">＋ {{ currentTabLabel }}</button>
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
              <tr
                v-for="item in fabrics"
                :key="item.fabricId"
                :class="{ 'active-row': selectedItemId === item.fabricId }"
                @click="previewItem(item)"
              >
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
                  <button class="secondary-button" type="button" @click="editItem(item)">编辑</button>
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
              <tr
                v-for="item in guides"
                :key="item.guideId"
                :class="{ 'active-row': selectedItemId === item.guideId }"
                @click="previewItem(item)"
              >
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
                  <button class="secondary-button" type="button" @click="editItem(item)">编辑</button>
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
              <tr
                v-for="item in careLabels"
                :key="item.careLabelId"
                :class="{ 'active-row': selectedItemId === item.careLabelId }"
                @click="previewItem(item)"
              >
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
                  <button class="secondary-button" type="button" @click="editItem(item)">编辑</button>
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

      <section class="panel-card">
        <div class="panel-head">
          <div>
            <h2>{{ editingId ? `编辑${currentTabLabel}` : `新建${currentTabLabel}` }}</h2>
            <p class="panel-subtitle">保存后会直接写入知识库表，可立刻被用户端搜索与问答命中。</p>
          </div>
          <button class="secondary-button" type="button" @click="resetDraft">清空</button>
        </div>

        <div class="preview-card">
          <div class="preview-head">
            <strong>当前选中预览</strong>
            <span>{{ selectedItem ? '已选中列表条目' : '未选中' }}</span>
          </div>

          <div v-if="selectedItem" class="preview-body">
            <template v-if="activeTab === 'fabric'">
              <h3>{{ selectedItem.name }}</h3>
              <p>{{ selectedItem.summary || '暂无摘要' }}</p>
              <div class="preview-tags">
                <span class="mini-chip">{{ selectedItem.fabricType || '未分类' }}</span>
                <span class="mini-chip">{{ selectedItem.isActive ? '启用' : '停用' }}</span>
              </div>
              <p class="preview-text">特性：{{ selectedItem.properties || '暂无' }}</p>
              <p class="preview-text">洗护：{{ selectedItem.careGuide || '暂无' }}</p>
            </template>

            <template v-else-if="activeTab === 'guide'">
              <h3>{{ selectedItem.title }}</h3>
              <p>{{ selectedItem.summary || '暂无摘要' }}</p>
              <div class="preview-tags">
                <span class="mini-chip">{{ selectedItem.guideType || '未分类' }}</span>
                <span class="mini-chip">{{ selectedItem.isActive ? '启用' : '停用' }}</span>
              </div>
              <p class="preview-text">副标题：{{ selectedItem.subtitle || '暂无' }}</p>
              <p class="preview-text">正文：{{ selectedItem.content || '暂无' }}</p>
            </template>

            <template v-else>
              <h3>{{ selectedItem.symbolName }}</h3>
              <p>{{ selectedItem.instruction || '暂无说明' }}</p>
              <div class="preview-tags">
                <span class="mini-chip">{{ selectedItem.symbolCode || '未编码' }}</span>
                <span class="mini-chip">{{ selectedItem.category || '未分类' }}</span>
                <span class="mini-chip">{{ selectedItem.isActive ? '启用' : '停用' }}</span>
              </div>
              <p class="preview-text">解释：{{ selectedItem.explanation || '暂无' }}</p>
              <p class="preview-text">建议：{{ selectedItem.doText || '暂无' }}</p>
              <p class="preview-text">避免：{{ selectedItem.dontText || '暂无' }}</p>
            </template>
          </div>

          <p v-else class="empty-tip">点击左侧表格任意一行，可以先预览，再决定是否编辑或删除。</p>
        </div>

        <div v-if="activeTab === 'guide'" class="preview-card">
          <div class="preview-head">
            <strong>PDF 导入指南</strong>
            <span>自动解析文本写入知识库</span>
          </div>
          <div class="toolbar import-toolbar">
            <label class="checkbox-row">
              <input v-model="pdfUpdateExisting" type="checkbox" />
              <span>同名文档自动覆盖</span>
            </label>
            <label class="pdf-file-label">
              <input
                ref="pdfFileInputRef"
                type="file"
                accept=".pdf"
                class="pdf-file-input"
                @change="onPdfFileChange"
              />
              <span class="secondary-button" role="button">选择 PDF</span>
            </label>
          </div>
          <p v-if="pdfFile" class="pdf-filename">已选：{{ pdfFile.name }}</p>
          <div class="import-footer">
            <p class="panel-subtitle">上传后自动提取全文，以文件名作为指南标题。</p>
            <button
              class="primary-button"
              type="button"
              :disabled="importingPdf || !pdfFile"
              @click="submitPdfImport"
            >
              {{ importingPdf ? '导入中...' : '开始 PDF 导入' }}
            </button>
          </div>
        </div>

        <div class="preview-card">
          <div class="preview-head">
            <strong>批量导入 {{ currentTabLabel }}</strong>
            <span>支持 JSON 数组</span>
          </div>

          <div class="toolbar import-toolbar">
            <label class="checkbox-row">
              <input v-model="importUpdateExisting" type="checkbox" />
              <span>遇到同名 / 同编码时自动更新旧数据</span>
            </label>
            <button class="secondary-button" type="button" @click="fillImportTemplate">填入模板</button>
            <button class="secondary-button" type="button" @click="clearImportPayload">清空导入区</button>
          </div>

          <textarea
            v-model.trim="importPayload"
            class="text-input textarea-input import-textarea"
            rows="10"
            :placeholder="importPlaceholder"
          ></textarea>

          <div class="import-footer">
            <p class="panel-subtitle">直接粘贴 JSON 数组即可，例如 `[{"name":"棉",...}]`。</p>
            <button class="primary-button" type="button" :disabled="importing || !importPayload" @click="submitImport">
              {{ importing ? '导入中...' : '开始批量导入' }}
            </button>
          </div>
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
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
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
const selectedItem = ref(null);
const selectedItemId = ref(null);
const loading = ref(false);
const saving = ref(false);
const deletingId = ref(null);
const importing = ref(false);
const fabrics = ref([]);
const guides = ref([]);
const careLabels = ref([]);
const filters = reactive({
  keyword: '',
  active: ''
});
const draft = reactive({ ...defaultDrafts.fabric });
const importPayload = ref('');
const importUpdateExisting = ref(true);
const pdfFile = ref(null);
const pdfFileInputRef = ref(null);
const pdfUpdateExisting = ref(true);
const importingPdf = ref(false);
const editorSectionRef = ref(null);
const editorHighlight = ref(false);
let editorHighlightTimer = null;

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

const importPlaceholder = computed(() => {
  if (activeTab.value === 'guide') {
    return '[\n  {\n    "title": "春秋通勤衬衫搭配",\n    "subtitle": "基础款也能更利落",\n    "guideType": "通勤穿搭",\n    "summary": "适合春秋上班场景的衬衫搭配建议",\n    "content": "可搭配西裤或半裙，外层叠西装。",\n    "author": "运营后台",\n    "publishDate": "2026-04-24",\n    "tags": "通勤,衬衫,春秋",\n    "keywords": "通勤,衬衫,西装"\n  }\n]';
  }

  if (activeTab.value === 'care-label') {
    return '[\n  {\n    "symbolCode": "DO_NOT_BLEACH",\n    "symbolName": "不可漂白",\n    "category": "漂白",\n    "instruction": "请勿使用含氯漂白剂",\n    "explanation": "会破坏纤维与染料稳定性",\n    "doText": "使用中性洗涤剂",\n    "dontText": "避免漂白液",\n    "keywords": "漂白,洗护,不可漂白"\n  }\n]';
  }

  return '[\n  {\n    "name": "棉麻混纺",\n    "alias": "cotton linen blend,棉麻",\n    "fabricType": "混纺面料",\n    "warmthScore": 48,\n    "breathabilityScore": 88,\n    "comfortScore": 82,\n    "durabilityScore": 70,\n    "summary": "兼顾棉的舒适与亚麻的清爽",\n    "properties": "透气、自然纹理明显，但较易起皱。",\n    "careGuide": "建议轻柔机洗或手洗，阴干。",\n    "suitableSeasons": "春,夏",\n    "suitableOccasions": "日常,通勤,度假",\n    "keywords": "棉麻,透气,夏季"\n  }\n]';
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

const resetSelection = () => {
  selectedItem.value = null;
  selectedItemId.value = null;
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
  resetSelection();
  await loadCurrentTab();
};

const createNew = () => {
  resetDraft();
  resetSelection();
  focusEditorSection();
};

const focusEditorSection = async () => {
  await nextTick();
  editorSectionRef.value?.scrollIntoView({
    behavior: 'smooth',
    block: 'start'
  });
  editorHighlight.value = true;
  if (editorHighlightTimer) {
    clearTimeout(editorHighlightTimer);
  }
  editorHighlightTimer = window.setTimeout(() => {
    editorHighlight.value = false;
    editorHighlightTimer = null;
  }, 1800);
};

const fillImportTemplate = () => {
  importPayload.value = importPlaceholder.value;
};

const clearImportPayload = () => {
  importPayload.value = '';
};

const previewItem = (item) => {
  selectedItem.value = item;
  selectedItemId.value =
    activeTab.value === 'fabric' ? item.fabricId : activeTab.value === 'guide' ? item.guideId : item.careLabelId;
};

const editItem = (item) => {
  previewItem(item);
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
  focusEditorSection();
};

const removeItem = async (item) => {
  const itemId = activeTab.value === 'fabric' ? item.fabricId : activeTab.value === 'guide' ? item.guideId : item.careLabelId;
  const itemTitle = activeTab.value === 'fabric' ? item.name : activeTab.value === 'guide' ? item.title : item.symbolName;

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

      if (editingId.value === itemId) {
        resetDraft();
      }
      if (selectedItemId.value === itemId) {
        resetSelection();
      }
      alert(result.message || '知识条目删除成功');
    }
  } catch (error) {
    console.error('Delete knowledge item failed:', error);
    alert(error.response?.data?.message || '删除知识条目失败');
  } finally {
    deletingId.value = null;
  }
};

const submitImport = async () => {
  if (!importPayload.value) {
    return;
  }

  let parsed;
  try {
    parsed = JSON.parse(importPayload.value);
  } catch {
    alert('导入内容不是合法 JSON，请检查格式');
    return;
  }

  if (!Array.isArray(parsed) || parsed.length === 0) {
    alert('导入内容必须是非空 JSON 数组');
    return;
  }

  importing.value = true;
  try {
    const payload = {
      updateExisting: Boolean(importUpdateExisting.value),
      items: parsed
    };

    let result;
    if (activeTab.value === 'fabric') {
      result = await knowledgeAdminApi.importFabrics(payload);
    } else if (activeTab.value === 'guide') {
      result = await knowledgeAdminApi.importGuides(payload);
    } else {
      result = await knowledgeAdminApi.importCareLabels(payload);
    }

    if (result?.code === 200) {
      const summary = result.data || {};
      await Promise.all([loadOverview(), loadCurrentTab()]);
      clearImportPayload();
      alert(
        `${result.message || '批量导入成功'}\n新增 ${summary.created || 0} 条，更新 ${summary.updated || 0} 条，跳过 ${summary.skipped || 0} 条`
      );
    }
  } catch (error) {
    console.error('Import knowledge data failed:', error);
    alert(error.response?.data?.message || '批量导入失败');
  } finally {
    importing.value = false;
  }
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

const onPdfFileChange = (event) => {
  pdfFile.value = event.target.files?.[0] || null;
};

const submitPdfImport = async () => {
  if (!pdfFile.value) return;
  importingPdf.value = true;
  try {
    const result = await knowledgeAdminApi.importGuideFromPdf(pdfFile.value, pdfUpdateExisting.value);
    if (result?.code === 200) {
      const d = result.data || {};
      await Promise.all([loadOverview(), loadCurrentTab()]);
      pdfFile.value = null;
      if (pdfFileInputRef.value) pdfFileInputRef.value.value = '';
      alert(`PDF导入成功\n标题：${d.title || '-'}\n字符数：${d.contentLength || 0}\n新增 ${d.created || 0} 条，更新 ${d.updated || 0} 条`);
    }
  } catch (error) {
    console.error('PDF import failed:', error);
    alert(error.response?.data?.message || 'PDF导入失败');
  } finally {
    importingPdf.value = false;
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

.action-cell {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.active-row {
  background: rgba(29, 78, 216, 0.05);
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

.preview-card {
  margin-bottom: 18px;
  padding: 16px;
  border-radius: 16px;
  background: #f8fbff;
  border: 1px solid #dbe6f7;
}

.editor-highlight {
  box-shadow: 0 0 0 3px rgba(29, 78, 216, 0.16);
  transition: box-shadow 0.25s ease;
}

.preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.preview-head span {
  color: #5f6b7a;
  font-size: 12px;
}

.preview-body h3 {
  margin: 0 0 8px;
}

.preview-body p {
  margin: 0;
}

.preview-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin: 10px 0;
}

.mini-chip {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: #e7edf7;
  color: #1e3a5f;
  font-size: 12px;
}

.preview-text + .preview-text {
  margin-top: 8px;
}

.import-toolbar {
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.checkbox-row {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #4b5563;
}

.import-textarea {
  min-height: 220px;
  font-family: Consolas, 'Courier New', monospace;
  font-size: 13px;
}

.import-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
}

.pdf-file-input {
  display: none;
}

.pdf-file-label {
  cursor: pointer;
}

.pdf-filename {
  margin: 8px 0 0;
  font-size: 13px;
  color: #4b5563;
  word-break: break-all;
}

@media (max-width: 1100px) {
  .knowledge-grid {
    grid-template-columns: 1fr;
  }

  .import-footer,
  .import-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
