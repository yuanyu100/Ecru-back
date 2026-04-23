<template>
  <div class="about">
    <header class="about-header">
      <h1>关于系统</h1>
    </header>
    
    <main class="about-main">
      <div class="about-content">
        <section class="about-section">
          <h2>系统介绍</h2>
          <p>本系统是一个基于AI技术的智能助手系统，旨在为用户提供便捷、高效的服务。系统集成了先进的自然语言处理技术，能够理解用户的需求并提供准确的响应。</p>
        </section>
        
        <section class="about-section">
          <h2>功能特点</h2>
          <ul class="feature-list">
            <li>智能聊天：与AI助手进行实时对话</li>
            <li>知识库：访问系统的知识资源</li>
            <li>系统设置：个性化配置</li>
            <li>响应式设计：适配不同设备</li>
          </ul>
        </section>
        
        <section class="about-section">
          <h2>技术架构</h2>
          <p>前端使用Vue 3 + Vite构建，后端基于Java Spring Boot框架，集成了先进的AI模型。</p>
        </section>
        
        <section class="about-section">
          <h2>联系我们</h2>
          <p>如果您有任何问题或建议，欢迎联系我们。</p>
        </section>
      </div>
    </main>
    
    <footer class="about-footer">
      <button class="back-btn" @click="goBack">返回首页</button>
    </footer>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';

const router = useRouter();

const goBack = () => {
  router.push('/');
};
</script>

<style scoped>
.about {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f0f2f5;
}

.about-header {
  background: linear-gradient(135deg, #4a90e2, #357abd);
  color: white;
  text-align: center;
  padding: 40px 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.about-header h1 {
  font-size: 28px;
  margin: 0;
  font-weight: 600;
}

.about-main {
  flex: 1;
  padding: 40px 20px;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}

.about-content {
  background: white;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
}

.about-section {
  margin-bottom: 30px;
}

.about-section h2 {
  color: #333;
  margin-bottom: 15px;
  font-size: 20px;
  border-bottom: 2px solid #4a90e2;
  padding-bottom: 8px;
}

.about-section p {
  color: #666;
  line-height: 1.6;
  margin-bottom: 15px;
}

.feature-list {
  list-style: none;
  padding: 0;
}

.feature-list li {
  color: #666;
  margin-bottom: 10px;
  padding-left: 25px;
  position: relative;
}

.feature-list li:before {
  content: '✓';
  position: absolute;
  left: 0;
  color: #4a90e2;
  font-weight: bold;
}

.about-footer {
  padding: 20px;
  text-align: center;
  margin-top: 20px;
}

.back-btn {
  background-color: #4a90e2;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.back-btn:hover {
  background-color: #357abd;
}

@media (max-width: 768px) {
  .about-header {
    padding: 30px 20px;
  }
  
  .about-header h1 {
    font-size: 24px;
  }
  
  .about-main {
    padding: 20px;
  }
  
  .about-content {
    padding: 20px;
  }
}
</style>
