<template>
  <div class="edit-clothing">
    <header class="edit-clothing-header">
      <h1>编辑衣物</h1>
      <button class="back-btn" @click="goBack">返回</button>
    </header>
    
    <div v-if="isLoading" class="loading-container">
      <div class="loading-indicator">
        <span class="dot"></span>
        <span class="dot"></span>
        <span class="dot"></span>
      </div>
      <p>加载中...</p>
    </div>
    
    <div v-else class="edit-clothing-form">
      <form @submit.prevent="submitForm">
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
            <label for="size">尺寸</label>
            <input 
              type="text" 
              id="size" 
              v-model="formData.size"
              placeholder="请输入尺寸"
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
        
        <div class="form-section">
          <h2>图片</h2>
          <div class="current-image" v-if="clothingData.imageUrl">
            <h3>当前图片</h3>
            <img :src="clothingData.imageUrl" :alt="clothingData.name" />
          </div>
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
              <p>{{ imageFile ? imageFile.name : '点击上传新图片' }}</p>
            </label>
            <div v-if="imageFile" class="image-preview">
              <img :src="imagePreview" :alt="imageFile.name" />
              <button type="button" class="remove-image" @click="removeImage">删除</button>
            </div>
          </div>
        </div>
        
        <div class="form-actions">
          <button type="button" class="cancel-btn" @click="goBack">取消</button>
          <button type="submit" class="submit-btn" :disabled="isSubmitting">
            {{ isSubmitting ? '提交中...' : '保存修改' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { wardrobeApi } from '../api/wardrobe';

const router = useRouter();
const route = useRoute();
const itemId = route.params.id;

const isLoading = ref(true);
const isSubmitting = ref(false);
const clothingData = ref({});
const imageFile = ref(null);
const imagePreview = ref('');
const fileInput = ref(null);

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

const fetchClothingData = async () => {
  try {
    // 这里需要实现获取单个衣物详情的API调用
    // 暂时使用模拟数据
    clothingData.value = {
      itemId: itemId,
      name: '示例衣物',
      brand: '示例品牌',
      category: '上衣',
      color: {
        primary: '红色',
        secondary: '白色'
      },
      material: '棉',
      size: 'M',
      purchaseChannel: '淘宝',
      purchaseTime: '2026-01-01',
      frequency: 3,
      privacyLevel: 'private',
      imageUrl: 'https://via.placeholder.com/200'
    };
    
    // 填充表单数据
    formData.name = clothingData.value.name;
    formData.brand = clothingData.value.brand;
    formData.category = clothingData.value.category;
    formData.color.primary = clothingData.value.color.primary;
    formData.color.secondary = clothingData.value.color.secondary;
    formData.material = clothingData.value.material;
    formData.size = clothingData.value.size;
    formData.purchaseChannel = clothingData.value.purchaseChannel;
    formData.purchaseTime = clothingData.value.purchaseTime;
    formData.frequency = clothingData.value.frequency;
    formData.privacyLevel = clothingData.value.privacyLevel;
  } catch (error) {
    console.error('获取衣物信息失败:', error);
    alert('获取衣物信息失败，请重试');
  } finally {
    isLoading.value = false;
  }
};

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
    // 更新衣物信息
    await wardrobeApi.updateClothing(itemId, formData);
    
    // 如果有新图片，上传图片
    if (imageFile.value) {
      await wardrobeApi.uploadImage(itemId, imageFile.value);
    }
    
    // 跳转到衣橱页面
    router.push('/wardrobe');
  } catch (error) {
    console.error('更新衣物失败:', error);
    alert('更新衣物失败，请重试');
  } finally {
    isSubmitting.value = false;
  }
};

const goBack = () => {
  router.push('/wardrobe');
};

onMounted(() => {
  fetchClothingData();
});
</script>

<style scoped>
.edit-clothing {
  min-height: 100vh;
  background-color: #f0f2f5;
  padding: 20px;
}

.edit-clothing-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 15px;
  border-bottom: 2px solid #4a90e2;
}

.edit-clothing-header h1 {
  font-size: 24px;
  color: #333;
}

.back-btn {
  background-color: #f5f5f5;
  color: #333;
  border: 1px solid #e0e0e0;
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.back-btn:hover {
  background-color: #e0e0e0;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 60vh;
}

.loading-indicator {
  display: flex;
  gap: 8px;
  margin-bottom: 15px;
}

.dot {
  width: 12px;
  height: 12px;
  background-color: #4a90e2;
  border-radius: 50%;
  animation: pulse 1.4s infinite ease-in-out both;
}

.dot:nth-child(1) {
  animation-delay: -0.32s;
}

.dot:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes pulse {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.edit-clothing-form {
  background: white;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
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
  color: #333;
}

.current-image {
  margin-bottom: 20px;
}

.current-image h3 {
  font-size: 14px;
  margin-bottom: 10px;
  color: #666;
}

.current-image img {
  max-width: 200px;
  max-height: 200px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
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
  color: #666;
  font-weight: 500;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 10px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.2s ease;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #4a90e2;
  box-shadow: 0 0 0 2px rgba(74, 144, 226, 0.1);
}

.image-upload {
  border: 2px dashed #e0e0e0;
  border-radius: 8px;
  padding: 30px;
  text-align: center;
  transition: all 0.2s ease;
}

.image-upload:hover {
  border-color: #4a90e2;
  background-color: rgba(74, 144, 226, 0.05);
}

.upload-label {
  display: block;
  cursor: pointer;
  transition: all 0.2s ease;
}

.upload-label:hover {
  color: #4a90e2;
}

.upload-icon {
  font-size: 48px;
  margin-bottom: 10px;
}

.upload-label p {
  margin: 0;
  color: #666;
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
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background-color: white;
  color: #333;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.cancel-btn:hover {
  background-color: #f5f5f5;
}

.submit-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  background-color: #4a90e2;
  color: white;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.submit-btn:hover:not(:disabled) {
  background-color: #357abd;
}

.submit-btn:disabled {
  background-color: #a0c3f0;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .edit-clothing {
    padding: 10px;
  }
  
  .edit-clothing-form {
    padding: 20px;
  }
  
  .form-row {
    flex-direction: column;
    gap: 15px;
  }
  
  .form-actions {
    flex-direction: column;
  }
  
  .form-actions button {
    width: 100%;
  }
}
</style>
