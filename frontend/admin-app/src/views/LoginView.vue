<template>
  <section class="auth-shell">
    <div class="auth-panel">
      <div class="auth-grid">
        <div class="auth-showcase">
          <p class="eyebrow">Ecru 后台</p>
          <h1>专业化的内容与运营控制台</h1>
          <p class="auth-copy">
            在同一工作台内管理用户资产、风格素材、知识库内容与 AI 运行质量，降低运营分散和排查成本。
          </p>

          <div class="auth-showcase-grid">
            <article class="showcase-card">
              <span>管理范围</span>
              <strong>用户、衣橱、知识、AI 监控一体化维护</strong>
            </article>
            <article class="showcase-card">
              <span>工作重点</span>
              <strong>优先支持高频列表、状态判断与异常定位</strong>
            </article>
            <article class="showcase-card">
              <span>权限控制</span>
              <strong>管理员与运营角色按权限进入不同视图</strong>
            </article>
          </div>
        </div>

        <div class="auth-form-panel">
          <div class="auth-form-shell">
            <div>
              <p class="header-tag">后台登录</p>
              <h2>管理后台登录</h2>
              <p class="panel-subtitle">登录后可进入管理后台首页，继续维护业务数据与系统运行状态。</p>
            </div>

            <form class="auth-form" @submit.prevent="handleSubmit">
              <label>
                <span class="label-row">
                  <span>用户名</span>
                  <span class="label-hint">管理员 / 运营账号</span>
                </span>
                <input v-model.trim="form.username" type="text" autocomplete="username" required />
              </label>

              <label>
                <span class="label-row">
                  <span>密码</span>
                  <span class="label-hint">当前账户密码</span>
                </span>
                <input v-model="form.password" type="password" autocomplete="current-password" required />
              </label>

              <button class="primary-button" type="submit" :disabled="submitting">
                {{ submitting ? '登录中...' : '进入后台' }}
              </button>
            </form>

            <p v-if="errorMessage" class="form-message error">{{ errorMessage }}</p>
            <p class="form-tip">部署后可通过 `/admin/` 访问管理后台。</p>
          </div>
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
