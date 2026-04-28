<template>
  <div class="editor-page">
    <header class="editor-header">
      <button class="icon-back" type="button" aria-label="返回" @click="goBack">
        <span></span>
      </button>
      <div>
        <p class="eyebrow">衣橱</p>
        <h1>添加衣物</h1>
      </div>
      <button class="import-link" type="button" @click="goToImport">订单导入</button>
    </header>

    <form class="editor-card" @submit.prevent="submitForm">
      <section class="section-block">
        <h2>图片</h2>
        <label class="upload-panel">
          <input ref="fileInput" type="file" accept="image/*" @change="handleImageUpload" />
          <template v-if="imagePreview">
            <img :src="imagePreview" alt="preview" />
          </template>
          <template v-else>
            <span class="upload-hint">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                <rect x="3" y="3" width="18" height="18" rx="4" stroke="currentColor" stroke-width="1.4"/>
                <circle cx="8.5" cy="8.5" r="1.5" fill="currentColor"/>
                <path d="M3 15l5-5 4 4 3-3 6 6" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <span>点击上传衣物图片</span>
            </span>
          </template>
        </label>
      </section>

      <section class="section-block">
        <h2>录入方式</h2>
        <div class="switch-row">
          <label><input v-model="useAiRecognition" type="radio" :value="true" /> AI 识别</label>
          <label><input v-model="useAiRecognition" type="radio" :value="false" /> 手动填写</label>
        </div>
      </section>

      <section class="section-block">
        <h2>基本信息</h2>
        <div class="form-grid">
          <label>
            <span>名称</span>
            <input v-model.trim="formData.name" type="text" :required="!useAiRecognition" placeholder="例如：白色衬衫" />
          </label>
          <label>
            <span>品牌</span>
            <input v-model.trim="formData.brand" type="text" placeholder="可选" />
          </label>
          <label>
            <span>分类</span>
            <select v-model="formData.category" :required="!useAiRecognition">
              <option value="">请选择分类</option>
              <option v-for="option in clothingCategoryOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
            </select>
          </label>
          <label>
            <span>主颜色</span>
            <input v-model.trim="formData.color.primary" type="text" :required="!useAiRecognition" placeholder="例如：白色" />
          </label>
          <label>
            <span>次颜色</span>
            <input v-model.trim="formData.color.secondary" type="text" placeholder="可选" />
          </label>
          <label>
            <span>材质</span>
            <input v-model.trim="formData.material" type="text" placeholder="例如：棉" />
          </label>
          <label>
            <span>尺码</span>
            <input v-model.trim="formData.size" type="text" placeholder="例如：M" />
          </label>
          <label>
            <span>购买日期</span>
            <input v-model="formData.purchaseTime" type="date" />
          </label>
          <label class="wide">
            <span>购买渠道 / 链接</span>
            <input v-model.trim="formData.purchaseChannel" type="text" placeholder="例如：淘宝 / 商品链接" />
          </label>
        </div>
      </section>

      <div class="form-actions">
        <button class="ghost-button" type="button" @click="goBack">取消</button>
        <button class="primary-button" type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? '提交中...' : '保存衣物' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { wardrobeApi } from '../api/wardrobe';
import { clothingCategoryOptions } from '../constants/clothingCategories';

const router = useRouter();
const fileInput = ref(null);
const imageFile = ref(null);
const imagePreview = ref('');
const isSubmitting = ref(false);
const useAiRecognition = ref(true);

const formData = reactive({
  name: '',
  brand: '',
  category: '',
  color: {
    primary: '',
    secondary: ''
  },
  material: '',
  size: '',
  purchaseChannel: '',
  purchaseTime: ''
});

const handleImageUpload = (event) => {
  const [file] = event.target.files || [];
  if (!file) {
    return;
  }

  imageFile.value = file;
  imagePreview.value = URL.createObjectURL(file);
};

const submitForm = async () => {
  if (useAiRecognition.value && !imageFile.value) {
    alert('AI 识别模式必须先上传图片。');
    return;
  }

  isSubmitting.value = true;
  try {
    const payload = {
      ...formData,
      color: { ...formData.color },
      autoRecognize: useAiRecognition.value
    };

    if (imageFile.value) {
      const uploadResponse = await wardrobeApi.uploadImage(null, imageFile.value);
      payload.imageUrl = uploadResponse.data;
    }

    const result = await wardrobeApi.addClothing(payload);
    const itemId = result.data?.itemId;
    router.push(itemId ? `/wardrobe/detail/${itemId}` : '/wardrobe');
  } catch (error) {
    console.error('Create clothing failed:', error);
    alert(error.response?.data?.message || '添加衣物失败');
  } finally {
    isSubmitting.value = false;
  }
};

const goBack = () => {
  router.push('/wardrobe');
};

const goToImport = () => {
  router.push('/wardrobe/import');
};
</script>

<style scoped>
.editor-page {
  min-height: 100vh;
  padding: 20px 16px 92px;
  background: linear-gradient(180deg, var(--bg-base) 0%, var(--bg-soft) 100%);
}

.editor-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
}

.icon-back {
  display: inline-grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 1px solid var(--line-soft);
  border-radius: 50%;
  background: var(--surface-strong);
  cursor: pointer;
  flex-shrink: 0;
}

.icon-back span {
  width: 10px;
  height: 10px;
  border-left: 1.5px solid var(--accent-strong);
  border-bottom: 1.5px solid var(--accent-strong);
  transform: rotate(45deg);
  margin-left: 4px;
}

.import-link {
  margin-left: auto;
  border: none;
  background: transparent;
  color: var(--accent);
  font-size: 13px;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.eyebrow {
  margin-bottom: 4px;
  font-size: 12px;
  color: var(--text-soft);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.editor-header h1,
.section-block h2 {
  color: var(--text-main);
}

.editor-card {
  padding: 18px;
  border-radius: 24px;
  background: var(--surface);
  border: 1px solid var(--line-soft);
  box-shadow: var(--shadow-card);
}

.section-block + .section-block {
  margin-top: 20px;
}

.upload-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 220px;
  border: 2px dashed var(--line-strong);
  border-radius: 18px;
  background: var(--surface-strong);
  overflow: hidden;
  cursor: pointer;
}

.upload-panel input {
  display: none;
}

.upload-panel img {
  width: 100%;
  height: 220px;
  object-fit: cover;
}

.upload-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: var(--text-faint);
  font-size: 13px;
}

.switch-row {
  display: flex;
  gap: 18px;
  margin-top: 12px;
  color: var(--text-soft);
}

.form-grid {
  display: grid;
  gap: 14px;
  margin-top: 14px;
}

.form-grid label {
  display: grid;
  gap: 8px;
  color: var(--text-soft);
  font-size: 14px;
}

.form-grid input,
.form-grid select {
  border: 1px solid var(--line-strong);
  border-radius: 14px;
  padding: 12px 14px;
  background: var(--surface-strong);
  color: var(--text-main);
}

.wide {
  grid-column: 1 / -1;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 22px;
}

.ghost-button,
.primary-button {
  border: none;
  border-radius: 999px;
  padding: 10px 16px;
  cursor: pointer;
}

.ghost-button {
  background: var(--bg-soft);
  color: var(--text-main);
}

.primary-button {
  background: var(--accent-strong);
  color: var(--surface-strong);
}

.primary-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (min-width: 768px) {
  .editor-page {
    padding: 28px 28px 40px;
  }

  .form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
