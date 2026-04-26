<template>
  <div class="profile-page">
    <header class="page-header">
      <p class="eyebrow">我的</p>
      <h1>个人中心</h1>
    </header>

    <div v-if="isLoading" class="state-card">正在读取资料...</div>

    <template v-else>
      <button class="entry-card" type="button" @click="goTo('/profile/account')">
        <div class="entry-avatar">
          <img v-if="avatarPreview" :src="avatarPreview" alt="avatar" />
          <span v-else>{{ displayInitial }}</span>
        </div>
        <div class="entry-copy">
          <strong>{{ profile.nickname || profile.username || 'Ecru 用户' }}</strong>
          <p>{{ profile.email || '基础资料、安全设置、退出登录' }}</p>
        </div>
        <i class="arrow"></i>
      </button>

      <button class="entry-card" type="button" @click="goTo('/profile/system')">
        <div class="entry-icon">设</div>
        <div class="entry-copy">
          <strong>系统设置</strong>
          <p>主题、首页展示、提示语、内容入口</p>
        </div>
        <i class="arrow"></i>
      </button>
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
const avatarPreview = ref('');

const displayInitial = computed(() => {
  const source = profile.value.nickname || profile.value.username || 'E';
  return source.charAt(0).toUpperCase();
});

const loadPage = async () => {
  isLoading.value = true;
  try {
    const profileResponse = await authApi.getCurrentProfile();
    profile.value = profileResponse.data || {};
    avatarPreview.value = profile.value.avatarUrl || '';
  } catch (error) {
    console.error('Load profile page failed:', error);
    alert(error.response?.data?.message || '读取个人信息失败');
  } finally {
    isLoading.value = false;
  }
};

const goTo = (path) => {
  router.push(path);
};

onMounted(loadPage);
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  padding: 16px 16px 108px;
  background:
    radial-gradient(circle at top, rgba(255, 252, 246, 0.9), transparent 28%),
    linear-gradient(180deg, var(--bg-base) 0%, var(--bg-soft) 100%);
}

.page-header {
  margin-bottom: 12px;
}

.eyebrow {
  color: var(--text-faint);
  font-size: 10px;
  letter-spacing: 0.12em;
}

.page-header h1 {
  margin-top: 4px;
  font-size: 18px;
}

.state-card,
.entry-card {
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

.entry-card {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 10px;
  padding: 14px;
  border: 1px solid var(--line-soft);
  text-align: left;
  cursor: pointer;
}

.entry-avatar,
.entry-icon {
  flex: none;
  width: 48px;
  height: 48px;
  border-radius: 16px;
  overflow: hidden;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, var(--accent-strong) 0%, var(--accent) 100%);
  color: var(--surface-strong);
  font-size: 18px;
  font-weight: 700;
}

.entry-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.entry-copy {
  min-width: 0;
  flex: 1;
}

.entry-copy strong {
  display: block;
  color: var(--text-main);
  font-size: 13px;
}

.entry-copy p {
  margin-top: 5px;
  color: var(--text-soft);
  font-size: 10px;
  line-height: 1.55;
}

.arrow {
  flex: none;
  width: 9px;
  height: 9px;
  border-top: 1.5px solid var(--text-faint);
  border-right: 1.5px solid var(--text-faint);
  transform: rotate(45deg);
}

@media (min-width: 768px) {
  .profile-page {
    padding: 28px 24px 40px;
  }
}
</style>
