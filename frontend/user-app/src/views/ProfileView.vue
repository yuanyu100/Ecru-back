<template>
  <div class="profile">
    <header class="profile-header">
      <h1>个人设置</h1>
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
    
    <div v-else class="profile-content">
      <div class="profile-section">
        <h2>基本信息</h2>
        <div class="info-item">
          <span class="info-label">用户名:</span>
          <span class="info-value">{{ userInfo.nickname }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">邮箱:</span>
          <span class="info-value">{{ userInfo.email }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">注册时间:</span>
          <span class="info-value">{{ formatDate(userInfo.registrationTime) }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">最后登录:</span>
          <span class="info-value">{{ formatDate(userInfo.lastLoginTime) }}</span>
        </div>
      </div>
      
      <div class="profile-section">
        <h2>风格偏好</h2>
        <form @submit.prevent="updatePreferences">
          <div class="form-group">
            <label>风格偏好</label>
            <div class="style-tags">
              <span 
                v-for="style in availableStyles" 
                :key="style"
                :class="['style-tag', userPreferences.stylePreferences.includes(style) ? 'active' : '']"
                @click="toggleStyle(style)"
              >
                {{ style }}
              </span>
            </div>
          </div>
          
          <div class="form-group">
            <label for="usualSize">常用尺码</label>
            <input 
              type="text" 
              id="usualSize" 
              v-model="userPreferences.usualSize"
              placeholder="请输入常用尺码"
            />
          </div>
          
          <div class="form-group">
            <label for="region">所在地区</label>
            <input 
              type="text" 
              id="region" 
              v-model="userPreferences.region"
              placeholder="请输入所在地区"
            />
          </div>
          
          <button type="submit" class="save-btn" :disabled="isSaving">
            {{ isSaving ? '保存中...' : '保存修改' }}
          </button>
        </form>
      </div>
      
      <div class="profile-section">
        <h2>账号安全</h2>
        <div class="security-actions">
          <button class="security-btn">修改密码</button>
          <button class="logout-btn" @click="logout">退出登录</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { authApi } from '../api/auth';

const router = useRouter();
const isLoading = ref(true);
const isSaving = ref(false);
const userInfo = ref({});
const userPreferences = reactive({
  stylePreferences: [],
  usualSize: '',
  region: ''
});

const availableStyles = ['休闲', '商务', '运动', '复古', '时尚', '简约'];

const fetchUserInfo = async () => {
  try {
    const currentUser = authApi.getCurrentUser();
    if (currentUser) {
      // 这里需要实现获取用户详情的API调用
      // 暂时使用模拟数据
      userInfo.value = {
        userId: currentUser.userId,
        nickname: currentUser.nickname,
        email: currentUser.email,
        registrationTime: '2026-01-01T00:00:00Z',
        lastLoginTime: '2026-03-24T12:00:00Z'
      };
      
      // 模拟用户偏好
      userPreferences.stylePreferences = ['休闲', '商务'];
      userPreferences.usualSize = 'M';
      userPreferences.region = '北京市';
    }
  } catch (error) {
    console.error('获取用户信息失败:', error);
  } finally {
    isLoading.value = false;
  }
};

const toggleStyle = (style) => {
  const index = userPreferences.stylePreferences.indexOf(style);
  if (index > -1) {
    userPreferences.stylePreferences.splice(index, 1);
  } else {
    userPreferences.stylePreferences.push(style);
  }
};

const updatePreferences = async () => {
  isSaving.value = true;
  try {
    const currentUser = authApi.getCurrentUser();
    if (currentUser) {
      await authApi.updatePreferences(currentUser.userId, userPreferences);
      alert('保存成功');
    }
  } catch (error) {
    console.error('更新偏好失败:', error);
    alert('保存失败，请重试');
  } finally {
    isSaving.value = false;
  }
};

const logout = () => {
  if (confirm('确定要退出登录吗？')) {
    authApi.logout();
    router.push('/login');
  }
};

const goBack = () => {
  router.push('/');
};

const formatDate = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return date.toLocaleString();
};

onMounted(() => {
  fetchUserInfo();
});
</script>

<style scoped>
.profile {
  min-height: 100vh;
  background-color: #f9f3e6;
  padding: 10px;
}

.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 2px solid #e8d5a2;
}

.profile-header h1 {
  font-size: 20px;
  color: #8b7355;
}

.back-btn {
  background-color: #f5e6c3;
  color: #8b7355;
  border: 1px solid #e8d5a2;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.back-btn:active {
  background-color: #e8d5a2;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 50vh;
}

.loading-indicator {
  display: flex;
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

.profile-content {
  background: #fdfaf5;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  border: 1px solid #e8d5a2;
}

.profile-section {
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #f5e6c3;
}

.profile-section:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.profile-section h2 {
  font-size: 16px;
  margin-bottom: 15px;
  color: #8b7355;
  font-weight: 600;
}

.info-item {
  display: flex;
  margin-bottom: 10px;
  align-items: center;
}

.info-label {
  width: 90px;
  color: #6d573b;
  font-size: 14px;
}

.info-value {
  flex: 1;
  color: #8b7355;
  font-size: 14px;
  word-break: break-all;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  color: #6d573b;
  font-size: 14px;
  font-weight: 500;
}

.form-group input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e8d5a2;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.2s ease;
  background-color: #fcf9f0;
}

.form-group input:focus {
  outline: none;
  border-color: #8b7355;
  box-shadow: 0 0 0 2px rgba(139, 115, 85, 0.1);
}

.style-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.style-tag {
  padding: 6px 12px;
  border: 1px solid #e8d5a2;
  border-radius: 20px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #6d573b;
}

.style-tag:active {
  border-color: #8b7355;
  color: #8b7355;
}

.style-tag.active {
  background-color: #8b7355;
  color: white;
  border-color: #8b7355;
}

.save-btn {
  padding: 8px 16px;
  background-color: #8b7355;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  width: 100%;
}

.save-btn:active:not(:disabled) {
  background-color: #6d573b;
}

.save-btn:disabled {
  background-color: #d5c18a;
  cursor: not-allowed;
}

.security-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.security-btn {
  flex: 1;
  padding: 10px;
  background-color: #f5e6c3;
  color: #8b7355;
  border: 1px solid #e8d5a2;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.security-btn:active {
  background-color: #e8d5a2;
}

.logout-btn {
  flex: 1;
  padding: 10px;
  background-color: #ff4d4f;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.logout-btn:active {
  background-color: #ff7875;
}

@media (min-width: 768px) {
  .profile {
    padding: 20px;
  }
  
  .profile-header {
    margin-bottom: 30px;
    padding-bottom: 15px;
  }
  
  .profile-header h1 {
    font-size: 24px;
  }
  
  .back-btn {
    padding: 8px 16px;
  }
  
  .profile-content {
    padding: 30px;
  }
  
  .profile-section {
    margin-bottom: 30px;
    padding-bottom: 20px;
  }
  
  .profile-section h2 {
    font-size: 18px;
    margin-bottom: 20px;
  }
  
  .info-label {
    width: 100px;
  }
  
  .form-group {
    margin-bottom: 20px;
  }
  
  .form-group label {
    margin-bottom: 8px;
  }
  
  .form-group input {
    padding: 10px;
  }
  
  .style-tags {
    gap: 10px;
  }
  
  .style-tag {
    padding: 8px 16px;
    font-size: 14px;
  }
  
  .save-btn {
    padding: 10px 20px;
    width: auto;
  }
  
  .security-actions {
    flex-direction: row;
    gap: 15px;
  }
  
  .security-btn,
  .logout-btn {
    padding: 12px;
  }
}
</style>
