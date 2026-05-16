<template>
  <section class="auth-shell">
    <div class="auth-panel">
      <div class="auth-form-panel">
        <div class="auth-form-shell">
          <h2>管理后台登录</h2>

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
              {{ submitting ? '登录中...' : '登录' }}
            </button>
          </form>

          <p v-if="errorMessage" class="form-message error">{{ errorMessage }}</p>
        </div>
      </div>
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
    errorMessage.value = error.response?.data?.message || '登录失败，请稍后重试。';
  } finally {
    submitting.value = false;
  }
};
</script>
