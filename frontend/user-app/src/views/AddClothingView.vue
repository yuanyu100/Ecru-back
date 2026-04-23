<template>
  <div class="add-clothing">
    <header class="add-clothing-header">
      <h1>添加衣物</h1>
      <button class="back-btn" @click="goBack">返回</button>
    </header>
    
    <div class="add-clothing-form">
      <form @submit.prevent="submitForm">
        <div class="form-section">
          <h2>图片上传</h2>
          <div class="image-upload">
            <input 
              type="file" 
              id="image" 
              ref="fileInput"
              @change="handleImageUpload"
              accept="image/*"
            />
            <label for="image" class="upload-label">
              <div class="upload-icon">📷</div>
              <p>{{ imageFile ? imageFile.name : '点击上传图片' }}</p>
            </label>
            <div v-if="imageFile" class="image-preview">
              <img :src="imagePreview" :alt="imageFile.name" />
              <button type="button" class="remove-image" @click="removeImage">删除</button>
            </div>
          </div>
        </div>
        
        <div class="form-section">
          <h2>AI识别</h2>
          <div class="form-group ai-recognition">
            <label>是否使用 AI 识别衣物信息？</label>
            <div class="radio-group">
              <label class="radio-option">
                <input type="radio" v-model="useAiRecognition" value="true" />
                <span>是</span>
              </label>
              <label class="radio-option">
                <input type="radio" v-model="useAiRecognition" value="false" />
                <span>否</span>
              </label>
            </div>
          </div>
        </div>
        
        <div v-if="useAiRecognition === 'false'" class="manual-form">
          <div class="form-section">
            <h2>基本信息</h2>
            <div class="form-group">
              <label for="name">衣物名称 *</label>
              <input 
                type="text" 
                id="name" 
                v-model="formData.name" 
                required
                placeholder="请输入衣物名称"
              />
            </div>
            <div class="form-group">
              <label for="brand">品牌</label>
              <input 
                type="text" 
                id="brand" 
                v-model="formData.brand"
                placeholder="请输入品牌"
              />
            </div>
            <div class="form-group">
              <label for="category">类别 *</label>
              <select 
                id="category" 
                v-model="formData.category" 
                required
              >
                <option value="">请选择类别</option>
                <option value="上衣">上衣</option>
                <option value="下装">下装</option>
                <option value="鞋子">鞋子</option>
                <option value="配饰">配饰</option>
              </select>
            </div>
          </div>
          
          <div class="form-section">
            <h2>详细信息</h2>
            <div class="form-row">
              <div class="form-group">
                <label for="primaryColor">主颜色 *</label>
                <input 
                  type="text" 
                  id="primaryColor" 
                  v-model="formData.color.primary" 
                  required
                  placeholder="请输入主颜色"
                />
              </div>
              <div class="form-group">
                <label for="secondaryColor">次颜色</label>
                <input 
                  type="text" 
                  id="secondaryColor" 
                  v-model="formData.color.secondary"
                  placeholder="请输入次颜色"
                />
              </div>
            </div>
            <div class="form-group">
              <label for="material">材质</label>
              <input 
                type="text" 
                id="material" 
                v-model="formData.material"
                placeholder="请输入材质"
              />
            </div>
            <div class="form-group">
              <label for="size">尺码</label>
              <input 
                type="text" 
                id="size" 
                v-model="formData.size"
                placeholder="请输入尺码"
              />
            </div>
            <div class="form-group">
              <label for="purchaseChannel">购买渠道</label>
              <input 
                type="text" 
                id="purchaseChannel" 
                v-model="formData.purchaseChannel"
                placeholder="请输入购买渠道"
              />
            </div>
            <div class="form-group">
              <label for="purchaseTime">购买时间</label>
              <input 
                type="date" 
                id="purchaseTime" 
                v-model="formData.purchaseTime"
              />
            </div>
            <div class="form-group">
              <label for="frequency">搭配频率</label>
              <select 
                id="frequency" 
                v-model="formData.frequency"
              >
                <option value="1">1 (很少)</option>
                <option value="2">2</option>
                <option value="3">3 (一般)</option>
                <option value="4">4</option>
                <option value="5">5 (经常)</option>
              </select>
            </div>
            <div class="form-group">
              <label for="privacyLevel">隐私级别</label>
              <select 
                id="privacyLevel" 
                v-model="formData.privacyLevel"
              >
                <option value="private">私有</option>
                <option value="public">公开</option>
              </select>
            </div>
          </div>
        </div>
        
        <div class="form-actions">
          <button type="button" class="cancel-btn" @click="goBack">取消</button>
          <button type="submit" class="submit-btn" :disabled="isSubmitting">
            {{ isSubmitting ? '提交中...' : '提交' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { wardrobeApi } from '../api/wardrobe';

const router = useRouter();
const isSubmitting = ref(false);
const imageFile = ref(null);
const imagePreview = ref('');
const fileInput = ref(null);
const useAiRecognition = ref('false');

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
  privacyLevel: 'private'
});

const handleImageUpload = (e) => {
  const file = e.target.files[0];
  if (file) {
    imageFile.value = file;
    const reader = new FileReader();
    reader.onload = (event) => {
      imagePreview.value = event.target.result;
    };
    reader.readAsDataURL(file);
  }
};

const removeImage = () => {
  imageFile.value = null;
  imagePreview.value = '';
  if (fileInput.value) {
    fileInput.value.value = '';
  }
};

const submitForm = async () => {
  isSubmitting.value = true;
  try {
    const payload = {
      ...formData,
      color: { ...formData.color },
      autoRecognize: useAiRecognition.value === 'true'
    };

    if (imageFile.value) {
      const uploadResponse = await wardrobeApi.uploadImage(null, imageFile.value);
      payload.imageUrl = uploadResponse.data;
    }

    await wardrobeApi.addClothing(payload);
    router.push('/wardrobe');
  } catch (error) {
    console.error('Add clothing failed:', error);
    alert('添加衣物失败，请重试');
  } finally {
    isSubmitting.value = false;
  }
};

const goBack = () => {
  router.push('/wardrobe');
};
</script>

<style scoped>
.add-clothing {
  min-height: 100vh;
  background-color: #f9f3e6;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.add-clothing-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 15px;
  border-bottom: 2px solid #e8d5a2;
}

.add-clothing-header h1 {
  font-size: 24px;
  color: #8b7355;
}

.back-btn {
  background-color: #f5e6c3;
  color: #8b7355;
  border: 1px solid #e8d5a2;
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.back-btn:hover {
  background-color: #e8d5a2;
}

.add-clothing-form {
  background: #fdfaf5;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  border: 1px solid #e8d5a2;
  width: 100%;
  max-width: 600px;
}

.form-section {
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.form-section:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.form-section h2 {
  font-size: 18px;
  margin-bottom: 20px;
  color: #8b7355;
}

.form-row {
  display: flex;
  gap: 20px;
  margin-bottom: 15px;
}

.form-group {
  flex: 1;
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-size: 14px;
  color: #6d573b;
  font-weight: 500;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 10px;
  border: 1px solid #e8d5a2;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.2s ease;
  background-color: #fdfaf5;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #8b7355;
  box-shadow: 0 0 0 2px rgba(139, 115, 85, 0.1);
}

.ai-recognition {
  margin-top: 10px;
}

.radio-group {
  display: flex;
  gap: 30px;
  margin-top: 10px;
}

.radio-option {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #8b7355;
}

.radio-option input[type="radio"] {
  width: auto;
  margin: 0;
}

.image-upload {
  border: 2px dashed #e8d5a2;
  border-radius: 8px;
  padding: 30px;
  text-align: center;
  transition: all 0.2s ease;
  background-color: #fdfaf5;
}

.image-upload:hover {
  border-color: #8b7355;
  background-color: rgba(139, 115, 85, 0.05);
}

.upload-label {
  display: block;
  cursor: pointer;
  transition: all 0.2s ease;
}

.upload-label:hover {
  color: #8b7355;
}

.upload-icon {
  font-size: 48px;
  margin-bottom: 10px;
}

.upload-label p {
  margin: 0;
  color: #6d573b;
  font-size: 14px;
}

.image-preview {
  margin-top: 20px;
  position: relative;
  display: inline-block;
}

.image-preview img {
  max-width: 200px;
  max-height: 200px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.remove-image {
  position: absolute;
  top: -10px;
  right: -10px;
  background-color: #ff4d4f;
  color: white;
  border: none;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  font-size: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.remove-image:hover {
  background-color: #ff7875;
  transform: scale(1.1);
}

.form-actions {
  display: flex;
  gap: 15px;
  justify-content: flex-end;
  margin-top: 30px;
}

.cancel-btn {
  padding: 10px 20px;
  border: 1px solid #e8d5a2;
  border-radius: 4px;
  background-color: #fdfaf5;
  color: #8b7355;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.cancel-btn:hover {
  background-color: #f5e6c3;
}

.submit-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  background-color: #8b7355;
  color: white;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.submit-btn:hover:not(:disabled) {
  background-color: #6d573b;
}

.submit-btn:disabled {
  background-color: #a68d6a;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .add-clothing {
    padding: 10px;
  }
  
  .add-clothing-form {
    padding: 20px;
    max-width: 100%;
  }
  
  .form-row {
    flex-direction: column;
    gap: 15px;
  }
  
  .radio-group {
    flex-direction: column;
    gap: 10px;
  }
  
  .form-actions {
    flex-direction: column;
  }
  
  .form-actions button {
    width: 100%;
  }
}
</style>
