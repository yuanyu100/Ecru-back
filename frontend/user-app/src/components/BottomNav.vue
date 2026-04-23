<template>
  <div class="bottom-nav">
    <div 
      class="nav-item" 
      :class="{ active: currentRoute === '/' || currentRoute === '/chat' }"
      @click="navigateTo('/')"
    >
      <div class="nav-icon">💬</div>
      <div class="nav-text">聊天</div>
    </div>
    <div 
      class="nav-item" 
      :class="{ active: currentRoute.includes('/wardrobe') }"
      @click="navigateTo('/wardrobe')"
    >
      <div class="nav-icon">👕</div>
      <div class="nav-text">衣柜</div>
    </div>
    <div 
      class="nav-item" 
      :class="{ active: currentRoute === '/profile' || currentRoute === '/login' || currentRoute === '/register' }"
      @click="navigateTo('/profile')"
    >
      <div class="nav-icon">⚙️</div>
      <div class="nav-text">设置</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { authApi } from '../api/auth';

const router = useRouter();
const route = useRoute();
const currentRoute = ref(route.path);

const navigateTo = (path) => {
  if (path === '/profile' && !authApi.isAuthenticated()) {
    router.push('/login');
  } else {
    router.push(path);
  }
};

const updateCurrentRoute = () => {
  currentRoute.value = route.path;
};

watch(() => route.path, updateCurrentRoute);

onMounted(() => {
  updateCurrentRoute();
});
</script>

<style scoped>
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  background-color: #f5e6c3;
  display: flex;
  justify-content: space-around;
  align-items: center;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  border-top: 1px solid #e8d5a2;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  height: 100%;
  cursor: pointer;
  transition: all 0.3s ease;
  color: #8b7355;
}

.nav-item:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

.nav-item.active {
  color: #4a90e2;
  font-weight: 600;
}

.nav-icon {
  font-size: 20px;
  margin-bottom: 4px;
}

.nav-text {
  font-size: 12px;
}

@media (min-width: 768px) {
  .bottom-nav {
    display: none;
  }
}
</style>
