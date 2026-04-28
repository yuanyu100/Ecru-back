<template>
  <div class="admin-page style-images-page">
    <section class="panel-card">
      <div class="panel-head stacked-head">
        <div>
          <h2>风格方向图片库</h2>
          <p class="panel-subtitle">这里可以上传、维护用户偏好学习页使用的风格图片。</p>
        </div>

        <div class="toolbar">
          <input
            v-model.trim="filters.keyword"
            class="text-input"
            type="text"
            placeholder="搜索标题、来源或分类"
          />
          <input
            v-model.trim="filters.styleCategory"
            class="text-input"
            type="text"
            placeholder="风格分类，如通勤 / 学院"
          />
          <select v-model="filters.active" class="text-input select-input">
            <option value="">全部状态</option>
            <option :value="true">启用</option>
            <option :value="false">停用</option>
          </select>
          <button class="secondary-button" type="button" @click="loadImages">查询</button>
          <button class="primary-button" type="button" @click="resetDraft">新增图片</button>
        </div>
      </div>

      <div v-if="loading" class="empty-tip">正在加载...</div>
      <div v-else-if="items.length" class="table-shell">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>预览</th>
              <th>标题</th>
              <th>分类</th>
              <th>来源</th>
              <th>价格</th>
              <th>状态</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in items" :key="item.id">
              <td>{{ item.id }}</td>
              <td>
                <div class="table-image">
                  <img :src="item.imageUrl" :alt="item.title || '风格图片'" />
                </div>
              </td>
              <td>{{ item.title || '-' }}</td>
              <td>{{ item.styleCategory || '-' }}</td>
              <td>{{ item.source || '-' }}</td>
              <td>{{ formatPrice(item.price) }}</td>
              <td>
                <span class="badge" :class="item.isActive ? 'badge-green' : 'badge-red'">
                  {{ item.isActive ? '启用' : '停用' }}
                </span>
              </td>
              <td>{{ formatDate(item.updatedAt) }}</td>
              <td class="action-cell">
                <button class="secondary-button" type="button" @click="editItem(item)">编辑</button>
                <button
                  class="danger-button"
                  type="button"
                  :disabled="deletingId === item.id"
                  @click="removeItem(item)"
                >
                  {{ deletingId === item.id ? '删除中...' : '删除' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else class="empty-tip">当前没有风格图片数据。</p>
    </section>

    <section class="panel-card editor-card">
      <div class="panel-head">
        <div>
          <h2>{{ draft.id ? '编辑风格图片' : '新增风格图片' }}</h2>
          <p class="panel-subtitle">建议优先上传白底拼接风格图，便于和推荐流视觉保持一致。</p>
        </div>
        <button class="secondary-button" type="button" @click="resetDraft">重置</button>
      </div>

      <div class="editor-grid">
        <div class="preview-card">
          <div class="preview-shell">
            <img v-if="draft.imageUrl" :src="draft.imageUrl" :alt="draft.title || '风格图片预览'" />
            <p v-else>上传后在这里预览</p>
          </div>

          <label class="upload-box">
            <span>{{ uploading ? '上传中...' : '上传风格图片' }}</span>
            <input type="file" accept="image/*" :disabled="uploading" @change="handleFileChange" />
          </label>

          <input
            v-model.trim="draft.imageUrl"
            class="text-input"
            type="text"
            placeholder="或直接粘贴图片 URL"
          />
        </div>

        <form class="editor-form" @submit.prevent="saveItem">
          <label>
            <span>图片标题</span>
            <input v-model.trim="draft.title" class="text-input" type="text" placeholder="例如：学院风穿搭灵感" />
          </label>

          <label>
            <span>风格分类</span>
            <input
              v-model.trim="draft.styleCategory"
              class="text-input"
              type="text"
              placeholder="例如：通勤、学院、休闲、复古"
              required
            />
          </label>

          <label>
            <span>来源</span>
            <input v-model.trim="draft.source" class="text-input" type="text" placeholder="例如：系统示例 / 小红书" />
          </label>

          <label>
            <span>原始链接</span>
            <input v-model.trim="draft.sourceUrl" class="text-input" type="url" placeholder="https://..." />
          </label>

          <label>
            <span>参考价格</span>
            <input v-model.number="draft.price" class="text-input" type="number" min="0" step="0.01" />
          </label>

          <label>
            <span>状态</span>
            <select v-model="draft.isActive" class="text-input select-input">
              <option :value="true">启用</option>
              <option :value="false">停用</option>
            </select>
          </label>

          <div class="form-actions">
            <button class="primary-button" type="submit" :disabled="saving">
              {{ saving ? '保存中...' : draft.id ? '保存修改' : '创建图片' }}
            </button>
          </div>
        </form>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { styleImageAdminApi } from '../../api/styleImage';

const createEmptyDraft = () => ({
  id: null,
  imageUrl: '',
  title: '',
  source: '',
  sourceUrl: '',
  price: null,
  styleCategory: '',
  isActive: true
});

const items = ref([]);
const loading = ref(false);
const saving = ref(false);
const uploading = ref(false);
const deletingId = ref(null);
const filters = reactive({
  keyword: '',
  styleCategory: '',
  active: ''
});
const draft = reactive(createEmptyDraft());

const formatDate = (value) => (value ? String(value).replace('T', ' ') : '-');

const formatPrice = (value) => {
  if (value === null || value === undefined || value === '') {
    return '-';
  }

  const numeric = Number(value);
  return Number.isNaN(numeric) ? String(value) : `¥${numeric.toFixed(2)}`;
};

const normalizePayload = () => ({
  imageUrl: draft.imageUrl.trim(),
  title: draft.title.trim() || null,
  source: draft.source.trim() || null,
  sourceUrl: draft.sourceUrl.trim() || null,
  price: draft.price === '' || draft.price === null || draft.price === undefined ? null : Number(draft.price),
  styleCategory: draft.styleCategory.trim(),
  isActive: Boolean(draft.isActive)
});

const loadImages = async () => {
  loading.value = true;
  try {
    const result = await styleImageAdminApi.getStyleImages({
      page: 1,
      size: 100,
      keyword: filters.keyword,
      styleCategory: filters.styleCategory,
      active: filters.active === '' ? undefined : filters.active
    });
    items.value = result?.data?.records || [];
  } finally {
    loading.value = false;
  }
};

const resetDraft = () => {
  Object.assign(draft, createEmptyDraft());
};

const editItem = (item) => {
  Object.assign(draft, {
    id: item.id,
    imageUrl: item.imageUrl || '',
    title: item.title || '',
    source: item.source || '',
    sourceUrl: item.sourceUrl || '',
    price: item.price ?? null,
    styleCategory: item.styleCategory || '',
    isActive: Boolean(item.isActive)
  });
};

const saveItem = async () => {
  if (!draft.imageUrl.trim()) {
    alert('请先上传图片或填写图片 URL');
    return;
  }

  if (!draft.styleCategory.trim()) {
    alert('请填写风格分类');
    return;
  }

  saving.value = true;
  try {
    const payload = normalizePayload();
    if (draft.id) {
      await styleImageAdminApi.updateStyleImage(draft.id, payload);
    } else {
      await styleImageAdminApi.createStyleImage(payload);
    }

    await loadImages();
    resetDraft();
  } catch (error) {
    console.error('Save style image failed:', error);
    alert(error.response?.data?.message || '保存风格图片失败');
  } finally {
    saving.value = false;
  }
};

const removeItem = async (item) => {
  if (!window.confirm(`确认删除“${item.title || `图片 ${item.id}`}”吗？`)) {
    return;
  }

  deletingId.value = item.id;
  try {
    await styleImageAdminApi.deleteStyleImage(item.id);
    items.value = items.value.filter((current) => current.id !== item.id);
    if (draft.id === item.id) {
      resetDraft();
    }
  } catch (error) {
    console.error('Delete style image failed:', error);
    alert(error.response?.data?.message || '删除风格图片失败');
  } finally {
    deletingId.value = null;
  }
};

const handleFileChange = async (event) => {
  const file = event.target.files?.[0];
  event.target.value = '';

  if (!file) {
    return;
  }

  uploading.value = true;
  try {
    const result = await styleImageAdminApi.uploadStyleImage(file);
    draft.imageUrl = result?.data || '';
  } catch (error) {
    console.error('Upload style image failed:', error);
    alert(error.response?.data?.message || '上传风格图片失败');
  } finally {
    uploading.value = false;
  }
};

onMounted(loadImages);
</script>

<style scoped>
.stacked-head {
  align-items: flex-start;
  flex-direction: column;
}

.editor-card {
  gap: 18px;
}

.editor-grid {
  display: grid;
  gap: 18px;
}

.preview-card,
.editor-form {
  display: grid;
  gap: 14px;
}

.preview-shell {
  min-height: 360px;
  display: grid;
  place-items: center;
  border: 1px solid #e8edf3;
  border-radius: 18px;
  background: #fff;
  overflow: hidden;
}

.preview-shell img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #fff;
}

.preview-shell p,
.panel-subtitle {
  color: #5f6b7a;
}

.upload-box {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  padding: 0 16px;
  border-radius: 12px;
  background: #dbe6f7;
  color: #1e3a5f;
  cursor: pointer;
  overflow: hidden;
}

.upload-box input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.table-image {
  width: 88px;
  height: 120px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e8edf3;
  background: #fff;
}

.table-image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.form-actions {
  padding-top: 8px;
}

@media (min-width: 1100px) {
  .editor-grid {
    grid-template-columns: minmax(320px, 420px) minmax(0, 1fr);
  }
}
</style>
