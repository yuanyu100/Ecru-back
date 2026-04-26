<template>
  <div class="detail-page">
    <header class="page-header">
      <button class="icon-back" type="button" aria-label="返回" @click="goBack">
        <span></span>
      </button>
      <div>
        <p class="eyebrow">首页轮播文案</p>
        <h1>PDF 导入</h1>
      </div>
    </header>

    <section class="card">
      <div class="section-head">
        <p class="section-caption">导入方式</p>
        <h2>先预览，再入库</h2>
      </div>

      <label class="upload-panel">
        <input accept="application/pdf" type="file" @change="handlePdfChange" />
        <strong>{{ pdfFileName || '选择 PDF 文件' }}</strong>
        <span>上传后会解析成不超过 20 个字的短句，你可以按条勾选后再导入。</span>
      </label>

      <div class="action-row">
        <button
          class="ghost-button"
          type="button"
          :disabled="!selectedPdfFile || isPdfParsing"
          @click="previewPdf"
        >
          {{ isPdfParsing ? '解析中...' : '开始解析' }}
        </button>
        <button
          class="ghost-button"
          type="button"
          :disabled="!pdfPreviewItems.length"
          @click="toggleAllPreview"
        >
          {{ allChecked ? '全部取消' : '全部选中' }}
        </button>
      </div>
    </section>

    <section v-if="pdfPreviewItems.length" class="card">
      <div class="section-head">
        <p class="section-caption">解析结果</p>
        <h2>{{ checkedPreviewCount }} 句待导入</h2>
      </div>

      <div class="preview-head">
        <strong>{{ pdfSourceLabel || 'PDF 导入' }}</strong>
      </div>

      <label
        v-for="item in pdfPreviewItems"
        :key="item.id"
        class="preview-item"
      >
        <input v-model="item.checked" type="checkbox" />
        <span class="preview-text">{{ item.text }}</span>
        <span class="preview-meta">{{ promptLength(item.text) }}/{{ maxPromptLength }}</span>
      </label>

      <button class="primary-button" type="button" :disabled="isSaving" @click="importAndSave">
        {{ isSaving ? '导入中...' : '导入并保存' }}
      </button>
    </section>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { authApi } from '../api/auth';
import {
  buildHomePromptPayload,
  createLocalId,
  maxPromptLength,
  normalizeHomePromptSettings,
  normalizePromptItem,
  promptLength
} from '../utils/homePromptSettings';

const router = useRouter();

const isPdfParsing = ref(false);
const isSaving = ref(false);
const selectedPdfFile = ref(null);
const pdfFileName = ref('');
const pdfSourceLabel = ref('');
const pdfPreviewItems = ref([]);

const checkedPreviewCount = computed(() => pdfPreviewItems.value.filter((item) => item.checked).length);
const allChecked = computed(() => pdfPreviewItems.value.length && pdfPreviewItems.value.every((item) => item.checked));

const handlePdfChange = (event) => {
  const file = event.target.files?.[0] || null;
  selectedPdfFile.value = file;
  pdfFileName.value = file?.name || '';
  pdfSourceLabel.value = '';
  pdfPreviewItems.value = [];
};

const previewPdf = async () => {
  if (!selectedPdfFile.value) {
    alert('先选择一个 PDF 文件');
    return;
  }

  isPdfParsing.value = true;
  try {
    const response = await authApi.previewHomePromptsFromPdf(selectedPdfFile.value);
    const payload = response.data || {};
    pdfSourceLabel.value = payload.sourceLabel || selectedPdfFile.value.name || 'PDF 导入';
    pdfPreviewItems.value = (payload.items || []).map((item) => ({
      ...normalizePromptItem(item),
      checked: true
    }));

    if (!pdfPreviewItems.value.length) {
      alert('解析完成，但没有识别到可导入的文案');
    }
  } catch (error) {
    console.error('Preview pdf failed:', error);
    alert(error.response?.data?.message || 'PDF 解析失败');
  } finally {
    isPdfParsing.value = false;
  }
};

const toggleAllPreview = () => {
  const nextChecked = !allChecked.value;
  pdfPreviewItems.value = pdfPreviewItems.value.map((item) => ({
    ...item,
    checked: nextChecked
  }));
};

const importAndSave = async () => {
  const checkedItems = pdfPreviewItems.value.filter((item) => item.checked);
  if (!checkedItems.length) {
    alert('先勾选要导入的句子');
    return;
  }

  isSaving.value = true;
  try {
    const settingsResponse = await authApi.getHomePromptSettings();
    const settings = normalizeHomePromptSettings(settingsResponse.data || {});
    const existingTexts = new Set(settings.items.map((item) => item.text));

    const importedItems = checkedItems
      .filter((item) => !existingTexts.has(item.text))
      .map((item) => normalizePromptItem({
        id: createLocalId('pdf'),
        text: item.text,
        sourceType: 'pdf',
        sourceLabel: pdfSourceLabel.value || item.sourceLabel || 'PDF 导入',
        enabled: true
      }));

    if (!importedItems.length) {
      alert('选中的句子已全部存在');
      return;
    }

    const payload = buildHomePromptPayload({
      ...settings,
      items: [...importedItems, ...settings.items]
    });

    await authApi.updateHomePromptSettings(payload);
    alert(`已导入 ${importedItems.length} 条文案`);
    router.push('/profile/system/home/prompts');
  } catch (error) {
    console.error('Import home prompts failed:', error);
    alert(error.response?.data?.message || '导入文案失败');
  } finally {
    isSaving.value = false;
  }
};

const goBack = () => {
  router.push('/profile/system/home');
};
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  padding: 20px 16px 40px;
  background:
    radial-gradient(circle at top, rgba(255, 252, 246, 0.92), transparent 28%),
    linear-gradient(180deg, var(--bg-base) 0%, var(--bg-soft) 100%);
}

.page-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
}

.icon-back {
  display: inline-grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--line-soft);
  border-radius: 50%;
  background: color-mix(in srgb, var(--surface-strong) 92%, transparent);
  cursor: pointer;
}

.icon-back span {
  width: 10px;
  height: 10px;
  border-left: 1.5px solid var(--text-main);
  border-bottom: 1.5px solid var(--text-main);
  transform: rotate(45deg);
  margin-left: 4px;
}

.eyebrow,
.section-caption {
  color: var(--text-faint);
  font-size: 10px;
  letter-spacing: 0.12em;
}

.card {
  margin-top: 12px;
  padding: 16px;
  border-radius: 22px;
  border: 1px solid var(--line-soft);
  background: color-mix(in srgb, var(--surface-strong) 93%, transparent);
  box-shadow: var(--shadow-card);
}

.section-head h2 {
  margin-top: 4px;
  font-size: 15px;
}

.upload-panel {
  display: block;
  margin-top: 12px;
  padding: 16px;
  border: 1px dashed rgba(168, 140, 102, 0.35);
  border-radius: 18px;
  background:
    linear-gradient(135deg, rgba(245, 238, 226, 0.7), rgba(255, 252, 246, 0.95));
  cursor: pointer;
}

.upload-panel input {
  display: none;
}

.upload-panel strong {
  display: block;
  color: var(--text-main);
  font-size: 13px;
}

.upload-panel span {
  display: block;
  margin-top: 6px;
  color: var(--text-soft);
  font-size: 11px;
  line-height: 1.6;
}

.action-row {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.ghost-button,
.primary-button {
  border: none;
  cursor: pointer;
}

.ghost-button {
  padding: 0 16px;
  height: 40px;
  border-radius: 999px;
  background: rgba(194, 168, 130, 0.14);
  color: var(--accent-strong);
  font-size: 12px;
}

.ghost-button:disabled,
.primary-button:disabled {
  opacity: 0.58;
  cursor: not-allowed;
}

.preview-head {
  margin-top: 10px;
  color: var(--text-main);
  font-size: 12px;
}

.preview-item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 10px;
  padding: 12px 0;
  border-top: 1px solid var(--line-soft);
}

.preview-text {
  color: var(--text-main);
  font-size: 12px;
  line-height: 1.6;
}

.preview-meta {
  color: var(--text-faint);
  font-size: 10px;
}

.primary-button {
  width: 100%;
  margin-top: 16px;
  padding: 11px 16px;
  border-radius: 999px;
  background: var(--accent-strong);
  color: var(--surface-strong);
  font-size: 12px;
}

@media (max-width: 640px) {
  .action-row {
    flex-direction: column;
  }
}

@media (min-width: 768px) {
  .detail-page {
    padding: 28px 24px 40px;
  }
}
</style>
