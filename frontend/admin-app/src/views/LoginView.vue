<template>
  <section class="auth-shell">
    <div class="auth-panel">
      <p class="eyebrow">Ecru Admin</p>
      <h1>后台管理入口</h1>
      <p class="auth-copy">当前后台直接对接真实登录接口，管理员权限由后端角色控制。</p>

      <form class="auth-form" @submit.prevent="handleSubmit">
        <label>
          <span>用户名</span>
          <input v-model.trim="form.username" type="text" autocomplete="username" required />
        </label>

        <label>
          <span>密码</span>
          <input v-model="form.password" type="password" autocomplete="current-password" required />
        </label>

        <button class="primary-button" type="submit" :disabled="submitting">
          {{ submitting ? '登录中...' : '登录后台' }}
        </button>
      </form>

      <p v-if="errorMessage" class="form-message error">{{ errorMessage }}</p>
      <p class="form-tip">Nginx 部署后，后台入口会固定在 `/admin/`。</p>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { authApi } from '../api/auth';

const router = useRouter();
const submitting = ref(false);
const errorMessage = ref('');
const form = reactive({
  username: '',
  password: ''
});

const handleSubmit = async () => {
  submitting.value = true;
  errorMessage.value = '';

  try {
    const result = await authApi.login(form);
    if (result?.code !== 200) {
      errorMessage.value = result?.message || '登录失败，请检查用户名和密码。';
      return;
    }

    router.push('/dashboard');
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '登录失败，请确认后端服务已启动。';
  } finally {
    submitting.value = false;
  }
};
</script>
