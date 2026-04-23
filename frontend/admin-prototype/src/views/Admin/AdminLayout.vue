<template>
  <div class="admin-layout">
    <div class="admin-sidebar">
      <div class="sidebar-header">
        <h2>Ecru 管理后台</h2>
      </div>
      <div class="sidebar-menu">
        <router-link to="/admin/dashboard" class="menu-item">
          <span class="menu-icon">📊</span>
          <span class="menu-text">仪表盘</span>
        </router-link>
        <router-link to="/admin/users" class="menu-item" v-if="isAdmin">
          <span class="menu-icon">👥</span>
          <span class="menu-text">用户管理</span>
        </router-link>
        <router-link to="/admin/clothing" class="menu-item">
          <span class="menu-icon">👕</span>
          <span class="menu-text">衣物管理</span>
        </router-link>
        <router-link to="/admin/api-monitor" class="menu-item" v-if="isAdmin">
          <span class="menu-icon">📈</span>
          <span class="menu-text">API监控</span>
        </router-link>
        <div class="menu-item logout" @click="logout">
          <span class="menu-icon">🚪</span>
          <span class="menu-text">退出登录</span>
        </div>
      </div>
    </div>
    <div class="admin-content">
      <div class="content-header">
        <div class="header-left">
          <h1>{{ pageTitle }}</h1>
        </div>
        <div class="header-right">
          <div class="user-info">
            <span>{{ currentUser?.username }}</span>
            <span v-if="isAdmin" class="admin-badge">Admin</span>
          </div>
        </div>
      </div>
      <div class="content-body">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, watch } from 'vue';
import { useRoute } from 'vue-router';
import { authApi } from '../../api/auth';

export default {
  name: 'AdminLayout',
  setup() {
    const route = useRoute();
    const currentUser = ref(authApi.getCurrentUser());
    
    const pageTitle = ref('仪表盘');
    
    const isAdmin = computed(() => {
      return currentUser.value?.role === 'ADMIN';
    });
    
    const logout = () => {
      authApi.logout();
      window.location.href = '/login';
    };
    
    const updatePageTitle = () => {
      const titleMap = {
        '/admin/dashboard': '仪表盘',
        '/admin/users': '用户管理',
        '/admin/clothing': '衣物管理',
        '/admin/api-monitor': 'API监控'
      };
      pageTitle.value = titleMap[route.path] || '仪表盘';
    };
    
    watch(() => route.path, updatePageTitle);
    updatePageTitle();
    
    return {
      currentUser,
      pageTitle,
      isAdmin,
      logout
    };
  }
};
</script>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  background-color: #f5f5f5;
}

.admin-sidebar {
  width: 250px;
  background-color: #2c3e50;
  color: white;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #34495e;
}

.sidebar-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.sidebar-menu {
  flex: 1;
  padding: 20px 0;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  color: #ecf0f1;
  text-decoration: none;
  transition: all 0.3s ease;
  cursor: pointer;
}

.menu-item:hover {
  background-color: #34495e;
  color: white;
}

.menu-icon {
  margin-right: 10px;
  font-size: 16px;
}

.menu-text {
  font-size: 14px;
}

.logout {
  margin-top: auto;
  border-top: 1px solid #34495e;
}

.admin-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.content-header {
  background-color: white;
  padding: 20px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.content-header h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin-badge {
  background-color: #3498db;
  color: white;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

.content-body {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}
</style>