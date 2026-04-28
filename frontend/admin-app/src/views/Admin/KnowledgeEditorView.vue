<template>
  <div class="admin-page knowledge-editor-page">
    <section class="panel-card">
      <div class="panel-head">
        <div>
          <h2>{{ isEdit ? `编辑${currentTabLabel}` : `新建${currentTabLabel}` }}</h2>
          <p class="panel-subtitle">独立编辑页只处理当前条目，避免列表页和表单页混在一起。</p>
        </div>
        <div class="toolbar">
          <button class="secondary-button" type="button" @click="goBack">返回列表</button>
        </div>
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
          <p class="panel-subtitle">直接粘贴 JSON 数组即可。</p>
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
            {{ saving ? '保存中...' : isEdit ? '保存修改' : '创建知识' }}
          </button>
        </div>
      </form>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { knowledgeAdminApi } from '../../api/knowledge';

const route = useRoute();
const router = useRouter();

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

const draft = reactive({ ...defaultDrafts.fabric });
const saving = ref(false);
const importing = ref(false);
const importPayload = ref('');
const importUpdateExisting = ref(true);
const pdfFile = ref(null);
const pdfFileInputRef = ref(null);
const pdfUpdateExisting = ref(true);
const importingPdf = ref(false);

const activeTab = computed(() => route.params.type);
const itemId = computed(() => route.params.id);
const isEdit = computed(() => Boolean(itemId.value));
const currentTabLabel = computed(() => {
  if (activeTab.value === 'guide') return '指南';
  if (activeTab.value === 'care-label') return '洗护';
  return '面料';
});

const importPlaceholder = computed(() => {
  if (activeTab.value === 'guide') {
    return '[\n  {\n    "title": "春秋通勤衬衫搭配",\n    "guideType": "通勤穿搭"\n  }\n]';
  }
  if (activeTab.value === 'care-label') {
    return '[\n  {\n    "symbolCode": "DO_NOT_BLEACH",\n    "symbolName": "不可漂白"\n  }\n]';
  }
  return '[\n  {\n    "name": "棉麻混纺",\n    "fabricType": "混纺面料"\n  }\n]';
});

const assignDraft = (nextDraft) => {
  Object.keys(draft).forEach((key) => {
    delete draft[key];
  });
  Object.assign(draft, nextDraft);
};

const resetDraft = () => {
  assignDraft({ ...defaultDrafts[activeTab.value] });
};

const goBack = () => {
  router.push('/knowledge');
};

const fillImportTemplate = () => {
  importPayload.value = importPlaceholder.value;
};

const clearImportPayload = () => {
  importPayload.value = '';
};

const loadItem = async () => {
  if (!isEdit.value) {
    resetDraft();
    return;
  }

  try {
    const listResult =
      activeTab.value === 'fabric'
        ? await knowledgeAdminApi.getFabrics({ page: 1, size: 200 })
        : activeTab.value === 'guide'
          ? await knowledgeAdminApi.getGuides({ page: 1, size: 200 })
          : await knowledgeAdminApi.getCareLabels({ page: 1, size: 200 });

    const items = listResult?.data?.list || [];
    const target = items.find((item) => String(resolveItemId(item)) === String(itemId.value));
    if (!target) {
      alert('未找到对应知识条目');
      goBack();
      return;
    }

    if (activeTab.value === 'fabric') {
      assignDraft({
        name: target.name || '',
        alias: target.alias || '',
        fabricType: target.fabricType || '',
        warmthScore: target.warmthScore ?? 0,
        breathabilityScore: target.breathabilityScore ?? 0,
        comfortScore: target.comfortScore ?? 0,
        durabilityScore: target.durabilityScore ?? 0,
        summary: target.summary || '',
        properties: target.properties || '',
        careGuide: target.careGuide || '',
        suitableSeasons: target.suitableSeasons || '',
        suitableOccasions: target.suitableOccasions || '',
        keywords: target.keywords || '',
        source: target.source || '',
        active: Boolean(target.isActive)
      });
      return;
    }

    if (activeTab.value === 'guide') {
      assignDraft({
        title: target.title || '',
        subtitle: target.subtitle || '',
        guideType: target.guideType || '',
        summary: target.summary || '',
        content: target.content || '',
        author: target.author || '',
        publishDate: target.publishDate ? String(target.publishDate).slice(0, 10) : '',
        tags: target.tags || '',
        coverImageUrl: target.coverImageUrl || '',
        coverImageCaption: target.coverImageCaption || '',
        keywords: target.keywords || '',
        source: target.source || '',
        active: Boolean(target.isActive)
      });
      return;
    }

    assignDraft({
      symbolCode: target.symbolCode || '',
      symbolName: target.symbolName || '',
      category: target.category || '',
      instruction: target.instruction || '',
      explanation: target.explanation || '',
      doText: target.doText || '',
      dontText: target.dontText || '',
      keywords: target.keywords || '',
      source: target.source || '',
      active: Boolean(target.isActive)
    });
  } catch (error) {
    console.error('Load knowledge item failed:', error);
    alert(error.response?.data?.message || '加载知识条目失败');
  }
};

const resolveItemId = (item) => {
  if (activeTab.value === 'fabric') return item.fabricId;
  if (activeTab.value === 'guide') return item.guideId;
  return item.careLabelId;
};

const submitDraft = async () => {
  saving.value = true;
  try {
    if (activeTab.value === 'fabric') {
      const result = isEdit.value
        ? await knowledgeAdminApi.updateFabric(itemId.value, { ...draft, active: Boolean(draft.active) })
        : await knowledgeAdminApi.createFabric({ ...draft, active: Boolean(draft.active) });
      if (result?.code === 200) {
        alert(result.message || '面料知识保存成功');
      }
    } else if (activeTab.value === 'guide') {
      const result = isEdit.value
        ? await knowledgeAdminApi.updateGuide(itemId.value, { ...draft, active: Boolean(draft.active) })
        : await knowledgeAdminApi.createGuide({ ...draft, active: Boolean(draft.active) });
      if (result?.code === 200) {
        alert(result.message || '指南知识保存成功');
      }
    } else {
      const result = isEdit.value
        ? await knowledgeAdminApi.updateCareLabel(itemId.value, { ...draft, active: Boolean(draft.active) })
        : await knowledgeAdminApi.createCareLabel({ ...draft, active: Boolean(draft.active) });
      if (result?.code === 200) {
        alert(result.message || '洗护知识保存成功');
      }
    }

    goBack();
  } catch (error) {
    console.error('Save knowledge data failed:', error);
    alert(error.response?.data?.message || '保存知识库数据失败');
  } finally {
    saving.value = false;
  }
};

const submitImport = async () => {
  if (!importPayload.value) return;

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

    const result =
      activeTab.value === 'fabric'
        ? await knowledgeAdminApi.importFabrics(payload)
        : activeTab.value === 'guide'
          ? await knowledgeAdminApi.importGuides(payload)
          : await knowledgeAdminApi.importCareLabels(payload);

    if (result?.code === 200) {
      alert(result.message || '批量导入成功');
      clearImportPayload();
    }
  } catch (error) {
    console.error('Import knowledge data failed:', error);
    alert(error.response?.data?.message || '批量导入失败');
  } finally {
    importing.value = false;
  }
};

const onPdfFileChange = (event) => {
  pdfFile.value = event.target.files?.[0] || null;
};

const submitPdfImport = async () => {
  if (!pdfFile.value || activeTab.value !== 'guide') return;
  importingPdf.value = true;
  try {
    const result = await knowledgeAdminApi.importGuideFromPdf(pdfFile.value, pdfUpdateExisting.value);
    if (result?.code === 200) {
      alert(result.message || 'PDF 导入成功');
      pdfFile.value = null;
      if (pdfFileInputRef.value) {
        pdfFileInputRef.value.value = '';
      }
    }
  } catch (error) {
    console.error('PDF import failed:', error);
    alert(error.response?.data?.message || 'PDF 导入失败');
  } finally {
    importingPdf.value = false;
  }
};

onMounted(loadItem);
</script>

<style scoped>
.knowledge-editor-page {
  gap: 20px;
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
  .import-footer,
  .import-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
