<template>
  <div class="users-view">
    <div class="users-header">
      <h2>用户管理</h2>
      <div class="header-actions">
        <input 
          type="text" 
          v-model="searchQuery" 
          placeholder="搜索用户..." 
          class="search-input"
        />
        <button class="btn btn-primary" @click="showAddUserDialog = true">
          添加用户
        </button>
      </div>
    </div>
    
    <div class="users-table-container">
      <table class="users-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>邮箱</th>
            <th>角色</th>
            <th>注册时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in filteredUsers" :key="user.id">
            <td>{{ user.id }}</td>
            <td>{{ user.username }}</td>
            <td>{{ user.email }}</td>
            <td>
              <span class="role-badge" :class="user.role.toLowerCase()">
                {{ user.role }}
              </span>
            </td>
            <td>{{ user.createdAt }}</td>
            <td class="action-buttons">
              <button class="btn btn-edit" @click="editUser(user)">
                编辑
              </button>
              <button class="btn btn-delete" @click="deleteUser(user.id)">
                删除
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <!-- 添加用户对话框 -->
    <div class="dialog-overlay" v-if="showAddUserDialog">
      <div class="dialog">
        <div class="dialog-header">
          <h3>添加用户</h3>
          <button class="dialog-close" @click="showAddUserDialog = false">×</button>
        </div>
        <div class="dialog-body">
          <form @submit.prevent="addUser">
            <div class="form-group">
              <label>用户名</label>
              <input type="text" v-model="newUser.username" required />
            </div>
            <div class="form-group">
              <label>邮箱</label>
              <input type="email" v-model="newUser.email" required />
            </div>
            <div class="form-group">
              <label>密码</label>
              <input type="password" v-model="newUser.password" required />
            </div>
            <div class="form-group">
              <label>角色</label>
              <select v-model="newUser.role">
                <option value="USER">USER</option>
                <option value="ADMIN">ADMIN</option>
              </select>
            </div>
            <div class="form-actions">
              <button type="button" class="btn btn-secondary" @click="showAddUserDialog = false">
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
    
    <!-- 编辑用户对话框 -->
    <div class="dialog-overlay" v-if="showEditUserDialog">
      <div class="dialog">
        <div class="dialog-header">
          <h3>编辑用户</h3>
          <button class="dialog-close" @click="showEditUserDialog = false">×</button>
        </div>
        <div class="dialog-body">
          <form @submit.prevent="updateUser">
            <div class="form-group">
              <label>用户名</label>
              <input type="text" v-model="editUserForm.username" required />
            </div>
            <div class="form-group">
              <label>邮箱</label>
              <input type="email" v-model="editUserForm.email" required />
            </div>
            <div class="form-group">
              <label>角色</label>
              <select v-model="editUserForm.role">
                <option value="USER">USER</option>
                <option value="ADMIN">ADMIN</option>
              </select>
            </div>
            <div class="form-actions">
              <button type="button" class="btn btn-secondary" @click="showEditUserDialog = false">
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

export default {
  name: 'UsersView',
  setup() {
    const users = ref([
      { id: 1, username: 'admin', email: 'admin@example.com', role: 'ADMIN', createdAt: '2024-01-01 00:00:00' },
      { id: 2, username: 'user1', email: 'user1@example.com', role: 'USER', createdAt: '2024-01-02 00:00:00' },
      { id: 3, username: 'user2', email: 'user2@example.com', role: 'USER', createdAt: '2024-01-03 00:00:00' }
    ]);
    
    const searchQuery = ref('');
    const showAddUserDialog = ref(false);
    const showEditUserDialog = ref(false);
    
    const newUser = ref({
      username: '',
      email: '',
      password: '',
      role: 'USER'
    });
    
    const editUserForm = ref({
      id: '',
      username: '',
      email: '',
      role: 'USER'
    });
    
    const filteredUsers = computed(() => {
      if (!searchQuery.value) return users.value;
      return users.value.filter(user => 
        user.username.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
        user.email.toLowerCase().includes(searchQuery.value.toLowerCase())
      );
    });
    
    const addUser = () => {
      // 这里可以添加实际的API调用
      const newUserId = users.value.length + 1;
      users.value.push({
        id: newUserId,
        ...newUser.value,
        createdAt: new Date().toISOString().slice(0, 19).replace('T', ' ')
      });
      showAddUserDialog.value = false;
      // 重置表单
      newUser.value = {
        username: '',
        email: '',
        password: '',
        role: 'USER'
      };
    };
    
    const editUser = (user) => {
      editUserForm.value = { ...user };
      showEditUserDialog.value = true;
    };
    
    const updateUser = () => {
      // 这里可以添加实际的API调用
      const index = users.value.findIndex(user => user.id === editUserForm.value.id);
      if (index !== -1) {
        users.value[index] = { ...editUserForm.value };
      }
      showEditUserDialog.value = false;
    };
    
    const deleteUser = (userId) => {
      if (confirm('确定要删除这个用户吗？')) {
        // 这里可以添加实际的API调用
        users.value = users.value.filter(user => user.id !== userId);
      }
    };
    
    onMounted(() => {
      // 这里可以添加实际的数据获取逻辑
      console.log('Users view mounted');
    });
    
    return {
      users,
      searchQuery,
      filteredUsers,
      showAddUserDialog,
      showEditUserDialog,
      newUser,
      editUserForm,
      addUser,
      editUser,
      updateUser,
      deleteUser
    };
  }
};
</script>

<style scoped>
.users-view {
  padding: 20px;
}

.users-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.users-header h2 {
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

.users-table-container {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.users-table {
  width: 100%;
  border-collapse: collapse;
}

.users-table th,
.users-table td {
  padding: 12px 15px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
}

.users-table th {
  background-color: #f8f9fa;
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

.users-table td {
  font-size: 14px;
  color: #555;
}

.role-badge {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.role-badge.admin {
  background-color: #3498db;
  color: white;
}

.role-badge.user {
  background-color: #27ae60;
  color: white;
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
</style>