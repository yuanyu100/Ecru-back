<template>
  <div class="editor-page">
    <header class="editor-header">
      <button class="icon-back" type="button" aria-label="返回" @click="goBack">
        <span></span>
      </button>
      <div>
        <p class="eyebrow">衣橱</p>
        <h1>编辑衣物</h1>
      </div>
    </header>

    <div v-if="isLoading" class="editor-card">加载中...</div>
    <form v-else class="editor-card" @submit.prevent="submitForm">
      <section class="section-block">
        <h2>图片</h2>
        <div class="preview-shell">
          <img :src="imagePreview || formData.imageUrl || fallbackImage" alt="preview" />
        </div>
        <label class="upload-trigger">
          <input ref="fileInput" type="file" accept="image/*" @change="handleImageUpload" />
          <span>{{ imageFile ? '已选择新图片' : '更换图片' }}</span>
        </label>
      </section>

      <section class="section-block">
        <h2>基本信息</h2>
        <div class="form-grid">
          <label>
            <span>名称</span>
            <input v-model.trim="formData.name" type="text" required />
          </label>
          <label>
            <span>品牌</span>
            <input v-model.trim="formData.brand" type="text" />
          </label>
          <label>
            <span>分类</span>
            <select v-model="formData.category" required>
              <option value="">请选择分类</option>
              <option v-for="option in clothingCategoryOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
            </select>
          </label>
          <label>
            <span>主颜色</span>
            <input v-model.trim="formData.color.primary" type="text" required />
          </label>
          <label>
            <span>次颜色</span>
            <input v-model.trim="formData.color.secondary" type="text" />
          </label>
          <label>
            <span>材质</span>
            <input v-model.trim="formData.material" type="text" />
          </label>
          <label>
            <span>尺码</span>
            <input v-model.trim="formData.size" type="text" />
          </label>
          <label>
            <span>购买日期</span>
            <input v-model="formData.purchaseTime" type="date" />
          </label>
          <label>
            <span>搭配频率</span>
            <select v-model="formData.frequency">
              <option :value="1">1</option>
              <option :value="2">2</option>
              <option :value="3">3</option>
              <option :value="4">4</option>
              <option :value="5">5</option>
            </select>
          </label>
          <label class="wide">
            <span>购买渠道 / 链接</span>
            <input v-model.trim="formData.purchaseChannel" type="text" />
          </label>
        </div>
      </section>

      <div class="form-actions">
        <button class="ghost-button" type="button" @click="goBack">取消</button>
        <button class="primary-button" type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? '保存中...' : '保存修改' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { wardrobeApi } from '../api/wardrobe';
import { clothingCategoryOptions } from '../constants/clothingCategories';

const router = useRouter();
const route = useRoute();
const itemId = route.params.id;
const fileInput = ref(null);
const imageFile = ref(null);
const imagePreview = ref('');
const isLoading = ref(true);
const isSubmitting = ref(false);
const fallbackImage = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="320" height="420"><rect width="100%" height="100%" fill="%23f0e4cf"/><text x="50%" y="50%" text-anchor="middle" fill="%238b7355" font-size="24">No Image</text></svg>';

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
  purchaseTime: '',
  frequency: 3,
  imageUrl: ''
});

const fillForm = (item = {}) => {
  formData.name = item.name || '';
  formData.brand = item.brand || '';
  formData.category = item.category || '';
  formData.color.primary = item.color?.primary || '';
  formData.color.secondary = item.color?.secondary || '';
  formData.material = item.material || '';
  formData.size = item.size || '';
  formData.purchaseChannel = item.purchaseChannel || '';
  formData.purchaseTime = item.purchaseTime || '';
  formData.frequency = Number(item.frequency || 3);
  formData.imageUrl = item.imageUrl || '';
};

const fetchClothingData = async () => {
  try {
    const result = await wardrobeApi.getClothingDetail(itemId);
    fillForm(result.data || {});
  } catch (error) {
    console.error('Load clothing detail failed:', error);
    alert(error.response?.data?.message || '获取衣物详情失败');
    router.push('/wardrobe');
  } finally {
    isLoading.value = false;
  }
};

const handleImageUpload = (event) => {
  const [file] = event.target.files || [];
  if (!file) {
    return;
  }

  imageFile.value = file;
  imagePreview.value = URL.createObjectURL(file);
};

const submitForm = async () => {
  isSubmitting.value = true;
  try {
    const payload = {
      ...formData,
      color: { ...formData.color }
    };

    if (imageFile.value) {
      const uploadResponse = await wardrobeApi.uploadImage(itemId, imageFile.value);
      payload.imageUrl = uploadResponse.data;
    }

    await wardrobeApi.updateClothing(itemId, payload);
    router.push(`/wardrobe/detail/${itemId}`);
  } catch (error) {
    console.error('Update clothing failed:', error);
    alert(error.response?.data?.message || '更新衣物失败');
  } finally {
    isSubmitting.value = false;
  }
};

const goBack = () => {
  router.push(`/wardrobe/detail/${itemId}`);
};

onMounted(fetchClothingData);
</script>

<style scoped>
.editor-page {
  min-height: 100vh;
  padding: 20px 16px 92px;
  background: linear-gradient(180deg, #f7efdf 0%, #efe1c8 100%);
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
  border: 1px solid rgba(145, 104, 49, 0.14);
  border-radius: 50%;
  background: rgba(255, 251, 244, 0.88);
  cursor: pointer;
}

.icon-back span {
  width: 10px;
  height: 10px;
  border-left: 1.5px solid #5d4523;
  border-bottom: 1.5px solid #5d4523;
  transform: rotate(45deg);
  margin-left: 4px;
}

.eyebrow {
  margin-bottom: 4px;
  font-size: 12px;
  color: #8f6a37;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.editor-header h1,
.section-block h2 {
  color: #5d4523;
}

.editor-card {
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 251, 244, 0.92);
  border: 1px solid rgba(145, 104, 49, 0.14);
  box-shadow: 0 18px 44px rgba(109, 78, 38, 0.08);
}

.section-block + .section-block {
  margin-top: 20px;
}

.preview-shell {
  overflow: hidden;
  border-radius: 18px;
  background: linear-gradient(180deg, #f6ead4 0%, #ede0c7 100%);
}

.preview-shell img {
  width: 100%;
  aspect-ratio: 3 / 4;
  object-fit: cover;
}

.upload-trigger {
  display: inline-flex;
  margin-top: 12px;
  cursor: pointer;
}

.upload-trigger input {
  display: none;
}

.upload-trigger span,
.ghost-button,
.primary-button {
  border: none;
  border-radius: 999px;
  padding: 10px 16px;
}

.upload-trigger span,
.ghost-button {
  background: #ead7b8;
  color: #5d4523;
}

.form-grid {
  display: grid;
  gap: 14px;
  margin-top: 14px;
}

.form-grid label {
  display: grid;
  gap: 8px;
  color: #6c522f;
  font-size: 14px;
}

.form-grid input,
.form-grid select {
  border: 1px solid #d9c39b;
  border-radius: 14px;
  padding: 12px 14px;
  background: #fffdf8;
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

.primary-button {
  background: #6b4b1f;
  color: #fff8ef;
  cursor: pointer;
}

.ghost-button {
  cursor: pointer;
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
