<template>
  <div class="login-container">
    <div class="login-card">
      <h2>Ecru 管理后台登录</h2>
      <form @submit.prevent="login">
        <div class="form-group">
          <label>用户名</label>
          <input type="text" v-model="credentials.username" required />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input type="password" v-model="credentials.password" required />
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary">登录</button>
        </div>
        <div v-if="error" class="error-message">
          {{ error }}
        </div>
        <div class="quick-login">
          <h3>快速登录</h3>
          <div class="quick-login-buttons">
            <button type="button" class="btn btn-admin" @click="quickLogin('admin')">
              以管理员身份登录
            </button>
            <button type="button" class="btn btn-user" @click="quickLogin('user')">
              以普通用户身份登录
            </button>
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { authApi } from '../api/auth';

export default {
  name: 'LoginView',
  setup() {
    const router = useRouter();
    const credentials = ref({
      username: '',
      password: ''
    });
    const error = ref('');
    
    const login = async () => {
      try {
        error.value = '';
        const response = await authApi.login(credentials.value);
        if (response.success) {
          router.push('/admin/dashboard');
        } else {
          error.value = response.message || '登录失败';
        }
      } catch (err) {
        error.value = '登录失败，请检查用户名和密码';
        console.error('登录错误:', err);
      }
    };
    
    const quickLogin = (type) => {
      // 模拟登录，直接设置localStorage
      if (type === 'admin') {
        localStorage.setItem('token', 'admin-token-123456');
        localStorage.setItem('user', JSON.stringify({
          id: 1,
          username: 'admin',
          email: 'admin@example.com',
          role: 'ADMIN'
        }));
      } else {
        localStorage.setItem('token', 'user-token-123456');
        localStorage.setItem('user', JSON.stringify({
          id: 2,
          username: 'user1',
          email: 'user1@example.com',
          role: 'USER'
        }));
      }
      router.push('/admin/dashboard');
    };
    
    return {
      credentials,
      error,
      login,
      quickLogin
    };
  }
};
</script>

<style scoped>
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background-color: #f5f5f5;
}

.login-card {
  background-color: white;
  border-radius: 8px;
  padding: 40px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  width: 400px;
  max-width: 90%;
}

.login-card h2 {
  margin: 0 0 30px 0;
  font-size: 24px;
  color: #333;
  text-align: center;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.form-group input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 16px;
  transition: border-color 0.3s ease;
}

.form-group input:focus {
  outline: none;
  border-color: #3498db;
}

.form-actions {
  margin-top: 30px;
}

.btn {
  padding: 12px 24px;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-primary {
  background-color: #3498db;
  color: white;
  width: 100%;
}

.btn-primary:hover {
  background-color: #2980b9;
}

.error-message {
  margin-top: 20px;
  padding: 12px;
  background-color: #fee;
  border: 1px solid #fcc;
  border-radius: 4px;
  color: #c00;
  font-size: 14px;
}

.quick-login {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #e0e0e0;
}

.quick-login h3 {
  margin: 0 0 15px 0;
  font-size: 16px;
  color: #333;
  text-align: center;
}

.quick-login-buttons {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.btn-admin {
  background-color: #3498db;
  color: white;
  width: 100%;
}

.btn-admin:hover {
  background-color: #2980b9;
}

.btn-user {
  background-color: #27ae60;
  color: white;
  width: 100%;
}

.btn-user:hover {
  background-color: #219a52;
}
</style>