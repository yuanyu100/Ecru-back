<template>
  <div class="wardrobe" @click="closeAddOptions">
    <header class="wardrobe-header">
      <div class="header-content">
        <div class="header-icon">👕</div>
        <h1>我的衣橱</h1>
      </div>
    </header>
    
    <div class="wardrobe-filters">
      <select v-model="filters.category" @change="fetchClothingList">
        <option value="">全部分类</option>
        <option value="上衣">上衣</option>
        <option value="下装">下装</option>
        <option value="鞋子">鞋子</option>
        <option value="配饰">配饰</option>
      </select>
      <input 
        type="text" 
        v-model="filters.color" 
        placeholder="按颜色筛选"
        @input="fetchClothingList"
      />
    </div>
    
    <div class="clothing-masonry">
      <div v-if="isLoading" class="loading-container">
        <div class="loading-indicator">
          <span class="dot"></span>
          <span class="dot"></span>
          <span class="dot"></span>
        </div>
        <p>加载中...</p>
      </div>
      <div v-else-if="clothingList.length === 0" class="empty-state">
        <div class="empty-icon">👕</div>
        <h3>衣橱是空的</h3>
        <p>添加一些衣物到你的衣橱吧</p>
        <button class="add-btn" @click="navigateToAdd">添加衣物</button>
      </div>
      <div 
        v-for="item in clothingList" 
        :key="item.itemId"
        class="clothing-item"
        @click="navigateToDetail(item.itemId)"
      >
        <div class="hanger"></div>
        <div class="clothing-image">
          <img 
            :src="item.imageUrl || 'https://via.placeholder.com/300x400'" 
            :alt="item.name"
          />
        </div>
        <div class="clothing-info">
          <h3>{{ item.name }}</h3>
          <div class="clothing-tags">
            <span class="tag">#{{ item.category }}</span>
            <span class="tag">#{{ item.color.primary }}</span>
          </div>
        </div>
      </div>
    </div>
    
    <div v-if="!isLoading && clothingList.length > 0" class="pagination">
      <button 
        class="page-btn" 
        :disabled="currentPage === 1"
        @click="changePage(currentPage - 1)"
        aria-label="上一页"
      >
        ←
      </button>
      <span class="page-info">
        {{ currentPage }} / {{ totalPages }}
      </span>
      <button 
        class="page-btn" 
        :disabled="currentPage === totalPages"
        @click="changePage(currentPage + 1)"
        aria-label="下一页"
      >
        →
      </button>
    </div>
    
    <!-- 添加按钮 -->
    <div class="add-button-container" @click.stop>
      <div class="add-button" @click="toggleAddOptions">
        <span class="add-icon">+</span>
      </div>
      <div class="add-options" v-if="showAddOptions">
        <button class="option-btn" @click="navigateToAdd">
          <span class="option-icon">📷</span>
          <span>拍照</span>
        </button>
        <button class="option-btn" @click="navigateToAdd">
          <span class="option-icon">🖼️</span>
          <span>相册</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { wardrobeApi } from '../api/wardrobe';

const router = useRouter();
const clothingList = ref([]);
const isLoading = ref(false);
const currentPage = ref(1);
const pageSize = ref(12);
const totalItems = ref(0);
const filters = ref({
  category: '',
  color: ''
});
const showAddOptions = ref(false);

const totalPages = computed(() => {
  return Math.ceil(totalItems.value / pageSize.value);
});

const fetchClothingList = async () => {
  isLoading.value = true;
  try {
    // 模拟数据，展示衣橱中有衣物的效果
    const mockData = {
      data: {
        items: [
          {
            itemId: 1,
            name: '白色T恤',
            category: '上衣',
            color: {
              primary: '白色',
              secondary: ''
            },
            imageUrl: 'https://via.placeholder.com/300x400?text=白色T恤'
          },
          {
            itemId: 2,
            name: '蓝色牛仔裤',
            category: '下装',
            color: {
              primary: '蓝色',
              secondary: ''
            },
            imageUrl: 'https://via.placeholder.com/300x400?text=蓝色牛仔裤'
          },
          {
            itemId: 3,
            name: '黑色运动鞋',
            category: '鞋子',
            color: {
              primary: '黑色',
              secondary: '白色'
            },
            imageUrl: 'https://via.placeholder.com/300x400?text=黑色运动鞋'
          },
          {
            itemId: 4,
            name: '红色围巾',
            category: '配饰',
            color: {
              primary: '红色',
              secondary: ''
            },
            imageUrl: 'https://via.placeholder.com/300x400?text=红色围巾'
          },
          {
            itemId: 5,
            name: '灰色卫衣',
            category: '上衣',
            color: {
              primary: '灰色',
              secondary: ''
            },
            imageUrl: 'https://via.placeholder.com/300x400?text=灰色卫衣'
          },
          {
            itemId: 6,
            name: '卡其色休闲裤',
            category: '下装',
            color: {
              primary: '卡其色',
              secondary: ''
            },
            imageUrl: 'https://via.placeholder.com/300x400?text=卡其色休闲裤'
          }
        ],
        total: 6
      }
    };
    clothingList.value = mockData.data.items;
    totalItems.value = mockData.data.total;
  } catch (error) {
    console.error('获取衣物列表失败:', error);
  } finally {
    isLoading.value = false;
  }
};

const toggleAddOptions = () => {
  showAddOptions.value = !showAddOptions.value;
};

const closeAddOptions = () => {
  showAddOptions.value = false;
};

const navigateToAdd = () => {
  showAddOptions.value = false;
  router.push('/wardrobe/add');
};

const navigateToEdit = (itemId) => {
  router.push(`/wardrobe/edit/${itemId}`);
};

const navigateToDetail = (itemId) => {
  router.push(`/wardrobe/detail/${itemId}`);
};

const deleteClothing = async (itemId) => {
  if (confirm('确定要删除这件衣物吗？')) {
    try {
      await wardrobeApi.deleteClothing(itemId);
      fetchClothingList();
    } catch (error) {
      console.error('删除衣物失败:', error);
    }
  }
};

const changePage = (page) => {
  currentPage.value = page;
  fetchClothingList();
};

onMounted(() => {
  fetchClothingList();
});
</script>

<style scoped>
.wardrobe {
  min-height: 100vh;
  background-color: #f9f3e6;
  padding: 10px;
  padding-bottom: 80px;
  position: relative;
}

.wardrobe-header {
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 2px solid #e8d5a2;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  font-size: 24px;
}

.wardrobe-header h1 {
  font-size: 20px;
  color: #8b7355;
  margin: 0;
}

.add-btn {
  background-color: #8b7355;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.add-btn:active {
  background-color: #6d573b;
}

.wardrobe-filters {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 20px;
}

.wardrobe-filters select,
.wardrobe-filters input {
  padding: 8px 12px;
  border: 1px solid #e8d5a2;
  border-radius: 4px;
  font-size: 14px;
  width: 100%;
  background-color: #fdfaf5;
}

.clothing-masonry {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
  margin-bottom: 20px;
}

.loading-container {
  grid-column: 1 / -1;
  text-align: center;
  padding: 40px 0;
}

.loading-indicator {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 15px;
}

.dot {
  width: 12px;
  height: 12px;
  background-color: #8b7355;
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

.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 40px 0;
  background-color: #fdfaf5;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  border: 1px solid #e8d5a2;
}

.empty-icon {
  font-size: 40px;
  margin-bottom: 15px;
}

.empty-state h3 {
  font-size: 18px;
  margin-bottom: 10px;
  color: #8b7355;
}

.empty-state p {
  color: #6d573b;
  margin-bottom: 20px;
  font-size: 14px;
}

.clothing-item {
  background: #fdfaf5;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  cursor: pointer;
  border: 1px solid #e8d5a2;
  position: relative;
  padding-top: 20px;
}

.clothing-item:active {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
  background: #fcf9f0;
}

.hanger {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 40px;
  height: 20px;
  background-color: #e8d5a2;
  border-radius: 10px 10px 0 0;
  z-index: 1;
}

.hanger::before {
  content: '';
  position: absolute;
  top: -10px;
  left: 50%;
  transform: translateX(-50%);
  width: 30px;
  height: 10px;
  background-color: #d5c18a;
  border-radius: 5px;
}

.clothing-image {
  height: 200px;
  overflow: hidden;
  background-color: #f5e6c3;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.clothing-image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: transform 0.3s ease;
}

.clothing-item:active .clothing-image img {
  transform: scale(1.05);
}

.clothing-info {
  padding: 15px;
}

.clothing-info h3 {
  font-size: 16px;
  margin-bottom: 10px;
  color: #8b7355;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.clothing-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tag {
  font-size: 12px;
  color: #8b7355;
  background-color: #f5e6c3;
  padding: 4px 8px;
  border-radius: 12px;
  border: 1px solid #e8d5a2;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  margin-top: 20px;
  flex-wrap: wrap;
}

.page-btn {
  padding: 6px 12px;
  border: 1px solid #e8d5a2;
  border-radius: 4px;
  background-color: #fdfaf5;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #8b7355;
  font-size: 14px;
}

.page-btn:active:not(:disabled) {
  background-color: #f5e6c3;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  color: #6d573b;
  font-size: 14px;
  margin: 0 10px;
}

.add-button-container {
  position: fixed;
  bottom: 80px;
  right: 20px;
  z-index: 100;
}

.add-button {
  width: 56px;
  height: 56px;
  border: 2px solid #8b7355;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background-color: #fdfaf5;
  box-shadow: 0 4px 12px rgba(139, 115, 85, 0.2);
  transition: all 0.3s ease;
}

.add-button:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(139, 115, 85, 0.3);
}

.add-button:active {
  transform: scale(0.95);
  background-color: #fcf9f0;
}

.add-icon {
  font-size: 28px;
  color: #8b7355;
  font-weight: 300;
  line-height: 1;
}

.add-options {
  position: absolute;
  bottom: 70px;
  right: 0;
  background-color: #fdfaf5;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border: 1px solid #e8d5a2;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 120px;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.option-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 15px;
  border: none;
  background: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s ease;
  color: #8b7355;
  font-size: 14px;
  text-align: left;
}

.option-btn:active {
  background-color: #f5e6c3;
}

.option-icon {
  font-size: 16px;
}

@media (min-width: 768px) {
  .wardrobe {
    padding: 20px;
    padding-bottom: 100px;
  }
  
  .wardrobe-header {
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 30px;
    padding-bottom: 15px;
  }
  
  .wardrobe-header h1 {
    font-size: 24px;
  }
  
  .add-btn {
    padding: 10px 20px;
  }
  
  .wardrobe-filters {
    flex-direction: row;
    flex-wrap: wrap;
    gap: 15px;
  }
  
  .wardrobe-filters select,
  .wardrobe-filters input {
    flex: 1;
    min-width: 200px;
  }
  
  .clothing-masonry {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 20px;
    margin-bottom: 30px;
  }
  
  .loading-container,
  .empty-state {
    padding: 60px 0;
  }
  
  .empty-icon {
    font-size: 48px;
  }
  
  .empty-state h3 {
    font-size: 20px;
  }
  
  .empty-state p {
    font-size: 16px;
  }
  
  .pagination {
    gap: 15px;
  }
  
  .page-btn {
    padding: 8px 16px;
  }
  
  .add-button {
    width: 64px;
    height: 64px;
  }
  
  .add-icon {
    font-size: 28px;
  }
  
  .add-options {
    min-width: 140px;
  }
  
  .option-btn {
    font-size: 16px;
  }
  
  .option-icon {
    font-size: 18px;
  }
}
</style>
