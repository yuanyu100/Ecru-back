<template>
  <div class="login">
    <div class="login-container">
      <div class="login-header">
        <h1>登录</h1>
        <p>欢迎回到 AI 穿搭系统</p>
      </div>
      
      <form @submit.prevent="login">
        <div class="form-group">
          <label for="username">用户名</label>
          <input 
            type="text" 
            id="username" 
            v-model="formData.username" 
            required
            placeholder="请输入用户名"
          />
        </div>
        
        <div class="form-group">
          <label for="password">密码</label>
          <input 
            type="password" 
            id="password" 
            v-model="formData.password" 
            required
            placeholder="请输入密码"
          />
        </div>
        
        <button type="submit" class="login-btn" :disabled="isLoading">
          {{ isLoading ? '登录中...' : '登录' }}
        </button>
        
        <div class="login-footer">
          <p>还没有账号？<a href="/register">立即注册</a></p>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { authApi } from '../api/auth';

const router = useRouter();
const isLoading = ref(false);
const formData = reactive({
  username: '',
  password: ''
});

const login = async () => {
  isLoading.value = true;
  try {
    await authApi.login(formData);
    router.push('/');
  } catch (error) {
    console.error('登录失败:', error);
    alert('登录失败，请检查用户名和密码');
  } finally {
    isLoading.value = false;
  }
};
</script>

<style scoped>
.login {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5e6c3, #e8d5a2);
  padding: 20px;
}

.login-container {
  background: #fdfaf5;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
  border: 1px solid #e8d5a2;
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h1 {
  font-size: 24px;
  color: #8b7355;
  margin-bottom: 10px;
}

.login-header p {
  color: #6d573b;
  font-size: 14px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #6d573b;
  font-size: 14px;
  font-weight: 500;
}

.form-group input {
  width: 100%;
  padding: 12px;
  border: 1px solid #e8d5a2;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.2s ease;
  background-color: #fcf9f0;
}

.form-group input:focus {
  outline: none;
  border-color: #8b7355;
  box-shadow: 0 0 0 2px rgba(139, 115, 85, 0.1);
}

.login-btn {
  width: 100%;
  padding: 14px;
  background-color: #8b7355;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease;
  margin-bottom: 20px;
}

.login-btn:hover:not(:disabled) {
  background-color: #6d573b;
}

.login-btn:disabled {
  background-color: #d5c18a;
  cursor: not-allowed;
}

.login-footer {
  text-align: center;
}

.login-footer p {
  color: #6d573b;
  font-size: 14px;
}

.login-footer a {
  color: #8b7355;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s ease;
}

.login-footer a:hover {
  color: #6d573b;
  text-decoration: underline;
}

@media (max-width: 768px) {
  .login-container {
    padding: 30px 20px;
  }
}
</style>
