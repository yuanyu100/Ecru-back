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
            <span>选择照片后会先上传到 MinIO</span>
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
              <option value="上装">上装</option>
              <option value="下装">下装</option>
              <option value="鞋履">鞋履</option>
              <option value="配饰">配饰</option>
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

.upload-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 220px;
  border: 2px dashed #d1b88b;
  border-radius: 18px;
  background: #fffdf8;
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

.switch-row {
  display: flex;
  gap: 18px;
  margin-top: 12px;
  color: #6c522f;
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

.ghost-button,
.primary-button {
  border: none;
  border-radius: 999px;
  padding: 10px 16px;
  cursor: pointer;
}

.ghost-button {
  background: #ead7b8;
  color: #5d4523;
}

.primary-button {
  background: #6b4b1f;
  color: #fff8ef;
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
