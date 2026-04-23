<template>
  <div class="clothing-view">
    <div class="clothing-header">
      <h2>衣物管理</h2>
      <div class="header-actions">
        <input 
          type="text" 
          v-model="searchQuery" 
          placeholder="搜索衣物..." 
          class="search-input"
        />
        <button class="btn btn-primary" @click="showAddClothingDialog = true">
          添加衣物
        </button>
        <button class="btn btn-danger" @click="batchDelete" :disabled="selectedClothing.length === 0">
          批量删除
        </button>
      </div>
    </div>
    
    <div class="clothing-table-container">
      <table class="clothing-table">
        <thead>
          <tr>
            <th>
              <input type="checkbox" v-model="selectAll" @change="toggleSelectAll" />
            </th>
            <th>ID</th>
            <th>名称</th>
            <th>分类</th>
            <th>颜色</th>
            <th>尺码</th>
            <th>用户</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filteredClothing" :key="item.id">
            <td>
              <input type="checkbox" v-model="selectedClothing" :value="item.id" />
            </td>
            <td>{{ item.id }}</td>
            <td>{{ item.name }}</td>
            <td>{{ item.category }}</td>
            <td>{{ item.color }}</td>
            <td>{{ item.size }}</td>
            <td>{{ item.user.username }}</td>
            <td class="action-buttons">
              <button class="btn btn-edit" @click="editClothing(item)">
                编辑
              </button>
              <button class="btn btn-delete" @click="deleteClothing(item.id)">
                删除
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <!-- 添加衣物对话框 -->
    <div class="dialog-overlay" v-if="showAddClothingDialog">
      <div class="dialog">
        <div class="dialog-header">
          <h3>添加衣物</h3>
          <button class="dialog-close" @click="showAddClothingDialog = false">×</button>
        </div>
        <div class="dialog-body">
          <form @submit.prevent="addClothing">
            <div class="form-group">
              <label>图片上传</label>
              <input type="file" accept="image/*" @change="handleImageUpload" />
              <div v-if="newClothing.image" class="image-preview">
                <img :src="newClothing.image" alt="预览" />
              </div>
            </div>
            <div class="form-group">
              <label>
                <input type="checkbox" v-model="newClothing.aiRecognize" />
                AI识别
              </label>
              <p class="form-hint">勾选后将使用AI自动识别衣物信息，无需手动填写</p>
            </div>
            <div v-if="!newClothing.aiRecognize">
              <div class="form-group">
                <label>名称</label>
                <input type="text" v-model="newClothing.name" required />
              </div>
              <div class="form-group">
                <label>分类</label>
                <select v-model="newClothing.category">
                  <option value="上衣">上衣</option>
                  <option value="裤子">裤子</option>
                  <option value="鞋子">鞋子</option>
                  <option value="配饰">配饰</option>
                </select>
              </div>
              <div class="form-group">
                <label>颜色</label>
                <input type="text" v-model="newClothing.color" required />
              </div>
              <div class="form-group">
                <label>尺码</label>
                <input type="text" v-model="newClothing.size" required />
              </div>
            </div>
            <div class="form-group" v-if="isAdmin">
              <label>用户</label>
              <select v-model="newClothing.userId">
                <option v-for="user in users" :key="user.id" :value="user.id">
                  {{ user.username }}
                </option>
              </select>
            </div>
            <div class="form-actions">
              <button type="button" class="btn btn-secondary" @click="showAddClothingDialog = false">
                取消
              </button>
              <button type="submit" class="btn btn-primary">
                保存
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
    
    <!-- 编辑衣物对话框 -->
    <div class="dialog-overlay" v-if="showEditClothingDialog">
      <div class="dialog">
        <div class="dialog-header">
          <h3>编辑衣物</h3>
          <button class="dialog-close" @click="showEditClothingDialog = false">×</button>
        </div>
        <div class="dialog-body">
          <form @submit.prevent="updateClothing">
            <div class="form-group">
              <label>名称</label>
              <input type="text" v-model="editClothingForm.name" required />
            </div>
            <div class="form-group">
              <label>分类</label>
              <select v-model="editClothingForm.category">
                <option value="上衣">上衣</option>
                <option value="裤子">裤子</option>
                <option value="鞋子">鞋子</option>
                <option value="配饰">配饰</option>
              </select>
            </div>
            <div class="form-group">
              <label>颜色</label>
              <input type="text" v-model="editClothingForm.color" required />
            </div>
            <div class="form-group">
              <label>尺码</label>
              <input type="text" v-model="editClothingForm.size" required />
            </div>
            <div class="form-actions">
              <button type="button" class="btn btn-secondary" @click="showEditClothingDialog = false">
                取消
              </button>
              <button type="submit" class="btn btn-primary">
                保存
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue';
import { authApi } from '../../api/auth';

export default {
  name: 'ClothingView',
  setup() {
    const clothing = ref([
      { id: 1, name: '白色T恤', category: '上衣', color: '白色', size: 'M', user: { id: 1, username: 'admin' } },
      { id: 2, name: '蓝色牛仔裤', category: '裤子', color: '蓝色', size: 'L', user: { id: 2, username: 'user1' } },
      { id: 3, name: '黑色运动鞋', category: '鞋子', color: '黑色', size: '42', user: { id: 3, username: 'user2' } }
    ]);
    
    const users = ref([
      { id: 1, username: 'admin' },
      { id: 2, username: 'user1' },
      { id: 3, username: 'user2' }
    ]);
    
    const searchQuery = ref('');
    const selectedClothing = ref([]);
    const selectAll = ref(false);
    const showAddClothingDialog = ref(false);
    const showEditClothingDialog = ref(false);
    
    const currentUser = ref(authApi.getCurrentUser());
    
    const isAdmin = computed(() => {
      return currentUser.value?.role === 'ADMIN';
    });
    
    const newClothing = ref({
      name: '',
      category: '上衣',
      color: '',
      size: '',
      userId: currentUser.value?.id || 1,
      image: '',
      aiRecognize: false
    });
    
    const editClothingForm = ref({
      id: '',
      name: '',
      category: '上衣',
      color: '',
      size: ''
    });
    
    const filteredClothing = computed(() => {
      if (isAdmin.value) {
        if (!searchQuery.value) return clothing.value;
        return clothing.value.filter(item => 
          item.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
          item.category.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
          item.color.toLowerCase().includes(searchQuery.value.toLowerCase())
        );
      } else {
        // 普通用户只看到自己的衣物
        const userClothing = clothing.value.filter(item => item.user.id === currentUser.value?.id);
        if (!searchQuery.value) return userClothing;
        return userClothing.filter(item => 
          item.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
          item.category.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
          item.color.toLowerCase().includes(searchQuery.value.toLowerCase())
        );
      }
    });
    
    const toggleSelectAll = () => {
      if (selectAll.value) {
        selectedClothing.value = filteredClothing.value.map(item => item.id);
      } else {
        selectedClothing.value = [];
      }
    };
    
    const handleImageUpload = (event) => {
      const file = event.target.files[0];
      if (file) {
        const reader = new FileReader();
        reader.onload = (e) => {
          newClothing.value.image = e.target.result;
        };
        reader.readAsDataURL(file);
      }
    };
    
    const addClothing = () => {
      // 这里可以添加实际的API调用
      const newClothingId = clothing.value.length + 1;
      const userId = isAdmin.value ? newClothing.value.userId : currentUser.value?.id;
      const user = users.value.find(u => u.id === userId);
      
      // 如果是AI识别，模拟AI识别结果
      let clothingData = { ...newClothing.value };
      if (newClothing.value.aiRecognize) {
        clothingData = {
          ...clothingData,
          name: 'AI识别衣物',
          category: '上衣',
          color: '黑色',
          size: 'M'
        };
      }
      
      clothing.value.push({
        id: newClothingId,
        ...clothingData,
        user: user
      });
      showAddClothingDialog.value = false;
      // 重置表单
      newClothing.value = {
        name: '',
        category: '上衣',
        color: '',
        size: '',
        userId: currentUser.value?.id || 1,
        image: '',
        aiRecognize: false
      };
    };
    
    const editClothing = (item) => {
      editClothingForm.value = { ...item };
      showEditClothingDialog.value = true;
    };
    
    const updateClothing = () => {
      // 这里可以添加实际的API调用
      const index = clothing.value.findIndex(item => item.id === editClothingForm.value.id);
      if (index !== -1) {
        clothing.value[index] = { ...editClothingForm.value };
      }
      showEditClothingDialog.value = false;
    };
    
    const deleteClothing = (id) => {
      if (confirm('确定要删除这个衣物吗？')) {
        // 这里可以添加实际的API调用
        clothing.value = clothing.value.filter(item => item.id !== id);
      }
    };
    
    const batchDelete = () => {
      if (selectedClothing.value.length === 0) return;
      if (confirm(`确定要删除选中的 ${selectedClothing.value.length} 个衣物吗？`)) {
        // 这里可以添加实际的API调用
        clothing.value = clothing.value.filter(item => !selectedClothing.value.includes(item.id));
        selectedClothing.value = [];
        selectAll.value = false;
      }
    };
    
    onMounted(() => {
      // 这里可以添加实际的数据获取逻辑
      console.log('Clothing view mounted');
    });
    
    return {
      clothing,
      users,
      searchQuery,
      selectedClothing,
      selectAll,
      showAddClothingDialog,
      showEditClothingDialog,
      newClothing,
      editClothingForm,
      filteredClothing,
      isAdmin,
      toggleSelectAll,
      handleImageUpload,
      addClothing,
      editClothing,
      updateClothing,
      deleteClothing,
      batchDelete
    };
  }
};
</script>

<style scoped>
.clothing-view {
  padding: 20px;
}

.clothing-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.clothing-header h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.search-input {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-primary {
  background-color: #3498db;
  color: white;
}

.btn-primary:hover {
  background-color: #2980b9;
}

.btn-secondary {
  background-color: #95a5a6;
  color: white;
}

.btn-secondary:hover {
  background-color: #7f8c8d;
}

.btn-edit {
  background-color: #f39c12;
  color: white;
  margin-right: 5px;
}

.btn-edit:hover {
  background-color: #e67e22;
}

.btn-delete {
  background-color: #e74c3c;
  color: white;
}

.btn-delete:hover {
  background-color: #c0392b;
}

.btn-danger {
  background-color: #e74c3c;
  color: white;
}

.btn-danger:hover {
  background-color: #c0392b;
}

.btn:disabled {
  background-color: #bdc3c7;
  cursor: not-allowed;
}

.clothing-table-container {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.clothing-table {
  width: 100%;
  border-collapse: collapse;
}

.clothing-table th,
.clothing-table td {
  padding: 12px 15px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
}

.clothing-table th {
  background-color: #f8f9fa;
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

.clothing-table td {
  font-size: 14px;
  color: #555;
}

.action-buttons {
  display: flex;
  gap: 5px;
}

.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog {
  background-color: white;
  border-radius: 8px;
  width: 400px;
  max-width: 90%;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.dialog-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.dialog-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
}

.dialog-body {
  padding: 20px;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.image-preview {
  margin-top: 10px;
  max-width: 200px;
  max-height: 200px;
  overflow: hidden;
  border-radius: 4px;
  border: 1px solid #ddd;
}

.image-preview img {
  width: 100%;
  height: auto;
  display: block;
}

.form-hint {
  margin: 5px 0 0 0;
  font-size: 12px;
  color: #666;
  font-style: italic;
}
</style>