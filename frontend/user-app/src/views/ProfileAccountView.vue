<template>
  <div class="detail-page">
    <header class="page-header">
      <button class="icon-back" type="button" aria-label="返回" @click="goBack">
        <span></span>
      </button>
      <div>
        <p class="eyebrow">我的</p>
        <h1>账号与安全</h1>
      </div>
    </header>

    <div v-if="isLoading" class="state-card">正在读取账号信息...</div>

    <template v-else>
      <section class="info-card">
        <div class="avatar-shell">
          <img v-if="profile.avatarUrl" :src="profile.avatarUrl" alt="avatar" />
          <span v-else>{{ displayInitial }}</span>
        </div>
        <div class="info-copy">
          <strong>{{ profile.nickname || profile.username || 'Ecru 用户' }}</strong>
          <span>{{ profile.email || '未绑定邮箱' }}</span>
        </div>
      </section>

      <section class="menu-card">
        <button class="menu-item" type="button" @click="goTo('/profile/basic')">
          <div>
            <strong>基础资料</strong>
            <span>昵称、头像、邮箱、电话</span>
          </div>
          <i class="arrow"></i>
        </button>

        <button class="menu-item" type="button" @click="goTo('/profile/security')">
          <div>
            <strong>安全设置</strong>
            <span>修改密码和退出登录</span>
          </div>
          <i class="arrow"></i>
        </button>
      </section>

      <section class="meta-card">
        <div class="meta-row">
          <span>账号</span>
          <strong>{{ profile.username || '-' }}</strong>
        </div>
        <div class="meta-row">
          <span>最近登录</span>
          <strong>{{ formatDate(profile.lastLoginAt) }}</strong>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { authApi } from '../api/auth';

const router = useRouter();
const isLoading = ref(true);
const profile = ref({});

const displayInitial = computed(() => {
  const source = profile.value.nickname || profile.value.username || 'E';
  return source.charAt(0).toUpperCase();
});

const loadPage = async () => {
  isLoading.value = true;
  try {
    const response = await authApi.getCurrentProfile();
    profile.value = response.data || {};
  } catch (error) {
    console.error('Load account page failed:', error);
    alert(error.response?.data?.message || '读取账号信息失败');
  } finally {
    isLoading.value = false;
  }
};

const goTo = (path) => {
  router.push(path);
};

const goBack = () => {
  router.push('/profile');
};

const formatDate = (value) => {
  if (!value) {
    return '-';
  }

  return new Date(value).toLocaleString('zh-CN', {
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
.menu-card,
.meta-card {
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

.info-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
}

.avatar-shell {
  width: 60px;
  height: 60px;
  border-radius: 20px;
  overflow: hidden;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, var(--accent-strong) 0%, var(--accent) 100%);
  color: var(--surface-strong);
  font-size: 22px;
  font-weight: 700;
}

.avatar-shell img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info-copy strong {
  display: block;
  color: var(--text-main);
  font-size: 15px;
}

.info-copy span {
  display: block;
  margin-top: 4px;
  color: var(--text-soft);
  font-size: 11px;
}

.menu-card,
.meta-card {
  margin-top: 12px;
}

.menu-item {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.menu-item + .menu-item {
  border-top: 1px solid var(--line-soft);
}

.menu-item strong {
  display: block;
  color: var(--text-main);
  font-size: 13px;
}

.menu-item span {
  display: block;
  margin-top: 4px;
  color: var(--text-soft);
  font-size: 11px;
  line-height: 1.5;
}

.arrow {
  flex: none;
  width: 9px;
  height: 9px;
  border-top: 1.5px solid var(--text-faint);
  border-right: 1.5px solid var(--text-faint);
  transform: rotate(45deg);
}

.meta-card {
  display: grid;
  gap: 10px;
  padding: 14px 16px;
}

.meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.meta-row span {
  color: var(--text-faint);
  font-size: 11px;
}

.meta-row strong {
  color: var(--text-main);
  font-size: 11px;
}

@media (min-width: 768px) {
  .detail-page {
    padding: 28px 24px 40px;
  }
}
</style>
