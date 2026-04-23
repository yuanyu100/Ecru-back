<template>
  <div class="clothing-detail">
    <header class="clothing-detail-header">
      <h1>衣物详情</h1>
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
    
    <div v-else class="clothing-detail-content">
      <div class="clothing-image">
        <img :src="clothingData.imageUrl || 'https://via.placeholder.com/400'" :alt="clothingData.name" />
      </div>
      
      <div class="clothing-info">
        <h2>{{ clothingData.name }}</h2>
        <p class="clothing-brand">{{ clothingData.brand }}</p>
        
        <div class="info-section">
          <h3>基本信息</h3>
          <div class="info-item">
            <span class="info-label">类别:</span>
            <span class="info-value">{{ clothingData.category }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">颜色:</span>
            <span class="info-value">{{ clothingData.color.primary }}{{ clothingData.color.secondary ? `, ${clothingData.color.secondary}` : '' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">材质:</span>
            <span class="info-value">{{ clothingData.material }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">尺寸:</span>
            <span class="info-value">{{ clothingData.size }}</span>
          </div>
        </div>
        
        <div class="info-section">
          <h3>购买信息</h3>
          <div class="info-item">
            <span class="info-label">购买渠道:</span>
            <span class="info-value">{{ clothingData.purchaseChannel }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">购买时间:</span>
            <span class="info-value">{{ clothingData.purchaseTime }}</span>
          </div>
        </div>
        
        <div class="info-section">
          <h3>其他信息</h3>
          <div class="info-item">
            <span class="info-label">搭配频率:</span>
            <div class="frequency-stars">
              <span 
                v-for="i in 5" 
                :key="i"
                :class="['star', i <= clothingData.frequency ? 'active' : '']"
              >★</span>
            </div>
          </div>
          <div class="info-item">
            <span class="info-label">隐私级别:</span>
            <span class="info-value">{{ clothingData.privacyLevel === 'private' ? '私有' : '公开' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">创建时间:</span>
            <span class="info-value">{{ clothingData.createdTime }}</span>
          </div>
          <div class="info-item" v-if="clothingData.updatedTime">
            <span class="info-label">更新时间:</span>
            <span class="info-value">{{ clothingData.updatedTime }}</span>
          </div>
        </div>
        
        <div class="action-buttons">
          <button class="edit-btn" @click="navigateToEdit">编辑</button>
          <button class="delete-btn" @click="deleteClothing">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { wardrobeApi } from '../api/wardrobe';

const router = useRouter();
const route = useRoute();
const itemId = route.params.id;

const isLoading = ref(true);
const clothingData = ref({});

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
      imageUrl: 'https://via.placeholder.com/400',
      createdTime: '2026-01-29T12:00:00Z',
      updatedTime: '2026-01-30T10:00:00Z'
    };
  } catch (error) {
    console.error('获取衣物信息失败:', error);
    alert('获取衣物信息失败，请重试');
  } finally {
    isLoading.value = false;
  }
};

const navigateToEdit = () => {
  router.push(`/wardrobe/edit/${itemId}`);
};

const deleteClothing = async () => {
  if (confirm('确定要删除这件衣物吗？')) {
    try {
      await wardrobeApi.deleteClothing(itemId);
      router.push('/wardrobe');
    } catch (error) {
      console.error('删除衣物失败:', error);
      alert('删除衣物失败，请重试');
    }
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
.clothing-detail {
  min-height: 100vh;
  background-color: #f0f2f5;
  padding: 20px;
}

.clothing-detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 15px;
  border-bottom: 2px solid #4a90e2;
}

.clothing-detail-header h1 {
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

.clothing-detail-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 30px;
  background: white;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.clothing-image {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f5f5;
  border-radius: 8px;
  padding: 20px;
}

.clothing-image img {
  max-width: 100%;
  max-height: 500px;
  object-fit: contain;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.clothing-info h2 {
  font-size: 24px;
  margin-bottom: 10px;
  color: #333;
}

.clothing-brand {
  font-size: 16px;
  color: #666;
  margin-bottom: 20px;
}

.info-section {
  margin-bottom: 25px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.info-section:last-child {
  border-bottom: none;
  margin-bottom: 30px;
  padding-bottom: 0;
}

.info-section h3 {
  font-size: 16px;
  margin-bottom: 15px;
  color: #333;
  font-weight: 600;
}

.info-item {
  display: flex;
  margin-bottom: 10px;
  align-items: center;
}

.info-label {
  width: 100px;
  color: #666;
  font-size: 14px;
}

.info-value {
  flex: 1;
  color: #333;
  font-size: 14px;
}

.frequency-stars {
  display: flex;
  gap: 2px;
}

.star {
  color: #ddd;
  font-size: 16px;
}

.star.active {
  color: #ffc107;
}

.action-buttons {
  display: flex;
  gap: 15px;
}

.edit-btn {
  flex: 1;
  padding: 12px;
  background-color: #4a90e2;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.edit-btn:hover {
  background-color: #357abd;
}

.delete-btn {
  flex: 1;
  padding: 12px;
  background-color: #ff4d4f;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.delete-btn:hover {
  background-color: #ff7875;
}

@media (max-width: 768px) {
  .clothing-detail {
    padding: 10px;
  }
  
  .clothing-detail-content {
    grid-template-columns: 1fr;
    padding: 20px;
  }
  
  .clothing-image {
    order: -1;
  }
  
  .action-buttons {
    flex-direction: column;
  }
}
</style>
