<template>
  <div class="register">
    <div class="register-container">
      <div class="register-header">
        <h1>注册</h1>
        <p>创建一个新账号</p>
      </div>
      
      <form @submit.prevent="register">
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
          <label for="email">邮箱</label>
          <input 
            type="email" 
            id="email" 
            v-model="formData.email" 
            required
            placeholder="请输入邮箱"
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
        
        <div class="form-group">
          <label for="confirmPassword">确认密码</label>
          <input 
            type="password" 
            id="confirmPassword" 
            v-model="formData.confirmPassword" 
            required
            placeholder="请确认密码"
          />
        </div>
        
        <button type="submit" class="register-btn" :disabled="isLoading">
          {{ isLoading ? '注册中...' : '注册' }}
        </button>
        
        <div class="register-footer">
          <p>已有账号？<a href="/login">立即登录</a></p>
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
  email: '',
  password: '',
  confirmPassword: ''
});

const register = async () => {
  if (formData.password !== formData.confirmPassword) {
    alert('两次输入的密码不一致');
    return;
  }
  
  isLoading.value = true;
  try {
    await authApi.register(formData);
    await authApi.login({
      username: formData.username,
      password: formData.password
    });
    router.push('/');
  } catch (error) {
    console.error('注册失败:', error);
    alert('注册失败，请检查输入信息');
  } finally {
    isLoading.value = false;
  }
};
</script>

<style scoped>
.register {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5e6c3, #e8d5a2);
  padding: 20px;
}

.register-container {
  background: #fdfaf5;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
  border: 1px solid #e8d5a2;
}

.register-header {
  text-align: center;
  margin-bottom: 30px;
}

.register-header h1 {
  font-size: 24px;
  color: #8b7355;
  margin-bottom: 10px;
}

.register-header p {
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

.register-btn {
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

.register-btn:hover:not(:disabled) {
  background-color: #6d573b;
}

.register-btn:disabled {
  background-color: #d5c18a;
  cursor: not-allowed;
}

.register-footer {
  text-align: center;
}

.register-footer p {
  color: #6d573b;
  font-size: 14px;
}

.register-footer a {
  color: #8b7355;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s ease;
}

.register-footer a:hover {
  color: #6d573b;
  text-decoration: underline;
}

@media (max-width: 768px) {
  .register-container {
    padding: 30px 20px;
  }
}
</style>
