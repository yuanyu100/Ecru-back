<template>
  <div class="detail-page">
    <header class="page-header">
      <button class="icon-back" type="button" aria-label="返回" @click="goBack">
        <span></span>
      </button>
      <div>
        <p class="eyebrow">我的</p>
        <h1>安全设置</h1>
      </div>
    </header>

    <div v-if="isLoading" class="state-card">正在读取账号信息...</div>

    <template v-else>
      <section class="info-card">
        <div class="meta-row">
          <span>用户名</span>
          <strong>{{ currentProfile.username || '-' }}</strong>
        </div>
        <div class="meta-row">
          <span>创建时间</span>
          <strong>{{ formatDate(currentProfile.createdAt) }}</strong>
        </div>
        <div class="meta-row">
          <span>最近登录</span>
          <strong>{{ formatDate(currentProfile.lastLoginAt) }}</strong>
        </div>
      </section>

      <form class="content-card" @submit.prevent="savePassword">
        <div class="form-grid">
          <label>
            <span>当前密码</span>
            <input v-model="passwordForm.oldPassword" type="password" placeholder="输入当前密码" />
          </label>
          <label>
            <span>新密码</span>
            <input v-model="passwordForm.newPassword" type="password" placeholder="至少 6 位" />
          </label>
          <label>
            <span>确认新密码</span>
            <input v-model="passwordForm.confirmPassword" type="password" placeholder="再次输入新密码" />
          </label>
        </div>

        <button class="primary-button" type="submit" :disabled="isUpdatingPassword">
          {{ isUpdatingPassword ? '更新中...' : '更新密码' }}
        </button>
      </form>

      <button class="logout-button" type="button" @click="logout">退出登录</button>
    </template>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { authApi } from '../api/auth';

const router = useRouter();
const isLoading = ref(true);
const isUpdatingPassword = ref(false);
const currentProfile = ref({});

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const loadPage = async () => {
  isLoading.value = true;
  try {
    const response = await authApi.getCurrentProfile();
    currentProfile.value = response.data || {};
  } catch (error) {
    console.error('Load profile security failed:', error);
    alert(error.response?.data?.message || '读取账号信息失败');
  } finally {
    isLoading.value = false;
  }
};

const savePassword = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    alert('请先输入当前密码和新密码');
    return;
  }

  if (passwordForm.newPassword.length < 6) {
    alert('新密码至少 6 位');
    return;
  }

  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    alert('两次输入的新密码不一致');
    return;
  }

  isUpdatingPassword.value = true;
  try {
    await authApi.updatePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    });
    passwordForm.oldPassword = '';
    passwordForm.newPassword = '';
    passwordForm.confirmPassword = '';
    alert('密码已更新');
  } catch (error) {
    console.error('Update password failed:', error);
    alert(error.response?.data?.message || '更新密码失败');
  } finally {
    isUpdatingPassword.value = false;
  }
};

const logout = () => {
  if (!window.confirm('确认退出当前账号吗？')) {
    return;
  }

  authApi.logout();
  if (window.history.length > 1) {
    router.back();
    return;
  }

  router.replace('/login');
};

const goBack = () => {
  if (window.history.length > 1) {
    router.back();
    return;
  }

  router.push('/profile/account');
};

const formatDate = (value) => {
  if (!value) {
    return '-';
  }

  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

onMounted(loadPage);
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  padding: 20px 16px 40px;
  background:
    radial-gradient(circle at top, rgba(255, 252, 246, 0.9), transparent 28%),
    linear-gradient(180deg, var(--bg-base) 0%, var(--bg-soft) 100%);
}

.page-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
}

.icon-back {
  display: inline-grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--line-soft);
  border-radius: 50%;
  background: color-mix(in srgb, var(--surface-strong) 90%, transparent);
  cursor: pointer;
}

.icon-back span {
  width: 10px;
  height: 10px;
  border-left: 1.5px solid var(--text-main);
  border-bottom: 1.5px solid var(--text-main);
  transform: rotate(45deg);
  margin-left: 4px;
}

.eyebrow {
  color: var(--text-faint);
  font-size: 10px;
  letter-spacing: 0.12em;
}

.state-card,
.info-card,
.content-card {
  border-radius: 22px;
  border: 1px solid var(--line-soft);
  background: color-mix(in srgb, var(--surface-strong) 92%, transparent);
  box-shadow: var(--shadow-card);
}

.state-card {
  padding: 22px 16px;
  text-align: center;
  color: var(--text-soft);
  font-size: 12px;
}

.info-card,
.content-card {
  padding: 16px;
}

.content-card {
  margin-top: 12px;
}

.meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.meta-row + .meta-row {
  margin-top: 14px;
}

.meta-row span {
  color: var(--text-faint);
  font-size: 11px;
}

.meta-row strong {
  color: var(--text-main);
  font-size: 11px;
  text-align: right;
}

.form-grid {
  display: grid;
  gap: 14px;
}

.form-grid label {
  display: grid;
  gap: 8px;
  color: var(--text-soft);
}

.form-grid input {
  border: 1px solid var(--line-soft);
  border-radius: 16px;
  padding: 12px 14px;
  background: var(--surface-strong);
  color: var(--text-main);
}

.primary-button,
.logout-button {
  width: 100%;
  border: none;
  border-radius: 999px;
  padding: 14px 18px;
  cursor: pointer;
}

.primary-button {
  margin-top: 22px;
  background: var(--accent-strong);
  color: var(--surface-strong);
}

.primary-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.logout-button {
  margin-top: 16px;
  background: var(--danger-soft);
  color: var(--danger);
}

@media (min-width: 768px) {
  .detail-page {
    padding: 28px 24px 40px;
  }
}
</style>
