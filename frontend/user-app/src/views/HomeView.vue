<template>
  <div class="home">
    <header class="home-header">
      <h1>AI 系统</h1>
      <div class="header-actions">
        <button v-if="!isAuthenticated" class="auth-btn" @click="navigateToLogin">登录</button>
        <button v-else class="profile-btn" @click="navigateToProfile">个人设置</button>
      </div>
      <p>智能助手，为您提供全方位的服务</p>
    </header>
    
    <main class="home-main">
      <div class="feature-grid">
        <div class="feature-card" @click="navigateToChat">
          <div class="feature-icon">💬</div>
          <h3>AI 聊天</h3>
          <p>与智能助手进行实时对话，获取所需信息</p>
          <button class="feature-btn" @click.stop="navigateToChat">开始聊天</button>
        </div>
        
        <div class="feature-card" @click="navigateToWardrobe">
          <div class="feature-icon">👕</div>
          <h3>我的衣橱</h3>
          <p>管理您的衣物，上传新衣物并分类</p>
          <button class="feature-btn">进入衣橱</button>
        </div>
        
        <div class="feature-card">
          <div class="feature-icon">📚</div>
          <h3>知识库</h3>
          <p>访问系统的知识库，获取更多信息</p>
          <button class="feature-btn">查看知识库</button>
        </div>
        
        <div class="feature-card">
          <div class="feature-icon">⚙️</div>
          <h3>系统设置</h3>
          <p>配置系统参数，个性化您的使用体验</p>
          <button class="feature-btn">进入设置</button>
        </div>
        
        <div class="feature-card">
          <div class="feature-icon">ℹ️</div>
          <h3>关于系统</h3>
          <p>了解系统的功能和使用方法</p>
          <button class="feature-btn" @click="navigateToAbout">了解更多</button>
        </div>
      </div>
    </main>
    
    <footer class="home-footer">
      <p>&copy; 2026 AI 系统. 保留所有权利.</p>
    </footer>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { ref, onMounted } from 'vue';
import { authApi } from '../api/auth';

const router = useRouter();
const isAuthenticated = ref(false);

const checkAuth = () => {
  isAuthenticated.value = authApi.isAuthenticated();
};

const navigateToChat = () => {
  router.push('/chat');
};

const navigateToWardrobe = () => {
  router.push('/wardrobe');
};

const navigateToAbout = () => {
  router.push('/about');
};

const navigateToLogin = () => {
  router.push('/login');
};

const navigateToProfile = () => {
  router.push('/profile');
};

onMounted(() => {
  checkAuth();
});
</script>

<style scoped>
.home {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f9f3e6;
}

.home-header {
  background: linear-gradient(135deg, #f5e6c3, #e8d5a2);
  color: #8b7355;
  text-align: center;
  padding: 40px 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  position: relative;
}

.header-actions {
  position: absolute;
  top: 15px;
  right: 15px;
}

.auth-btn,
.profile-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 20px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.auth-btn {
  background-color: rgba(139, 115, 85, 0.2);
  color: #8b7355;
  border: 1px solid rgba(139, 115, 85, 0.3);
}

.auth-btn:hover {
  background-color: rgba(139, 115, 85, 0.3);
}

.profile-btn {
  background-color: #8b7355;
  color: white;
}

.profile-btn:hover {
  background-color: #6d573b;
}

.home-header h1 {
  font-size: 28px;
  margin-bottom: 10px;
  font-weight: 600;
}

.home-header p {
  font-size: 16px;
  opacity: 0.9;
}

.home-main {
  flex: 1;
  padding: 20px;
  width: 100%;
}

.feature-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
  margin-top: 20px;
}

.feature-card {
  background: #fdfaf5;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  cursor: pointer;
  border: 1px solid #e8d5a2;
}

.feature-card:active {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.12);
  background: #fcf9f0;
}

.feature-icon {
  font-size: 40px;
  margin-bottom: 15px;
}

.feature-card h3 {
  font-size: 18px;
  margin-bottom: 8px;
  color: #8b7355;
}

.feature-card p {
  color: #6d573b;
  margin-bottom: 15px;
  line-height: 1.5;
  font-size: 14px;
}

.feature-btn {
  background-color: #8b7355;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.feature-btn:active {
  background-color: #6d573b;
}

.home-footer {
  background-color: #e8d5a2;
  color: #8b7355;
  text-align: center;
  padding: 15px;
  margin-top: auto;
  border-top: 1px solid #d5c18a;
  font-size: 12px;
}

@media (min-width: 768px) {
  .home-header {
    padding: 60px 20px;
  }
  
  .home-header h1 {
    font-size: 36px;
  }
  
  .home-header p {
    font-size: 18px;
  }
  
  .home-main {
    padding: 40px 20px;
    max-width: 1200px;
    margin: 0 auto;
  }
  
  .feature-grid {
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 30px;
    margin-top: 40px;
  }
  
  .feature-card {
    padding: 30px;
  }
  
  .feature-icon {
    font-size: 48px;
    margin-bottom: 20px;
  }
  
  .feature-card h3 {
    font-size: 20px;
    margin-bottom: 10px;
  }
  
  .feature-card p {
    margin-bottom: 20px;
    font-size: 16px;
  }
  
  .feature-btn {
    padding: 10px 20px;
  }
  
  .home-footer {
    padding: 20px;
    font-size: 14px;
  }
}
</style>
