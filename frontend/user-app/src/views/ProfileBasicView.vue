<template>
  <div class="detail-page">
    <header class="page-header">
      <button class="icon-back" type="button" aria-label="返回" @click="goBack">
        <span></span>
      </button>
      <div>
        <p class="eyebrow">我的</p>
        <h1>基础资料</h1>
      </div>
    </header>

    <div v-if="isLoading" class="state-card">正在读取资料...</div>

    <form v-else class="content-card" @submit.prevent="saveProfile">
      <section class="avatar-section">
        <div class="avatar-shell">
          <img v-if="avatarPreview" :src="avatarPreview" alt="avatar" />
          <span v-else>{{ displayInitial }}</span>
        </div>
        <label :class="['avatar-button', isUploadingAvatar ? 'disabled' : '']">
          <input type="file" accept="image/*" :disabled="isUploadingAvatar" @change="handleAvatarUpload" />
          {{ isUploadingAvatar ? '上传中...' : '更换头像' }}
        </label>
      </section>

      <section class="form-grid">
        <label>
          <span>昵称</span>
          <input v-model.trim="profileForm.nickname" type="text" maxlength="50" placeholder="输入昵称" />
        </label>
        <label>
          <span>邮箱</span>
          <input v-model.trim="profileForm.email" type="email" placeholder="name@example.com" />
        </label>
        <label>
          <span>电话</span>
          <input v-model.trim="profileForm.phone" type="text" placeholder="输入手机号" />
        </label>
        <label>
          <span>性别</span>
          <select v-model.number="profileForm.gender">
            <option :value="0">未设置</option>
            <option :value="1">男</option>
            <option :value="2">女</option>
          </select>
        </label>
        <label class="wide">
          <span>生日</span>
          <input v-model="profileForm.birthday" type="date" />
        </label>
      </section>

      <button class="primary-button" type="submit" :disabled="isSavingProfile">
        {{ isSavingProfile ? '保存中...' : '保存资料' }}
      </button>
    </form>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { authApi } from '../api/auth';
import { wardrobeApi } from '../api/wardrobe';

const router = useRouter();
const isLoading = ref(true);
const isSavingProfile = ref(false);
const isUploadingAvatar = ref(false);
const currentProfile = ref({});
const avatarPreview = ref('');

const profileForm = reactive({
  nickname: '',
  email: '',
  phone: '',
  gender: 0,
  birthday: ''
});

const displayInitial = computed(() => {
  const source = profileForm.nickname || currentProfile.value.username || 'E';
  return source.charAt(0).toUpperCase();
});

const applyProfile = (profile = {}) => {
  currentProfile.value = profile;
  profileForm.nickname = profile.nickname || '';
  profileForm.email = profile.email || '';
  profileForm.phone = profile.phone || '';
  profileForm.gender = Number(profile.gender ?? 0);
  profileForm.birthday = profile.birthday || '';
  avatarPreview.value = profile.avatarUrl || '';
};

const loadPage = async () => {
  isLoading.value = true;
  try {
    const response = await authApi.getCurrentProfile();
    applyProfile(response.data || {});
  } catch (error) {
    console.error('Load profile basic failed:', error);
    alert(error.response?.data?.message || '读取基础资料失败');
  } finally {
    isLoading.value = false;
  }
};

const saveProfile = async () => {
  isSavingProfile.value = true;
  try {
    const response = await authApi.updateCurrentProfile({
      nickname: profileForm.nickname || null,
      email: profileForm.email || null,
      phone: profileForm.phone || null,
      gender: Number(profileForm.gender ?? 0),
      birthday: profileForm.birthday || null
    });
    applyProfile(response.data || {});
    alert('基础资料已保存');
  } catch (error) {
    console.error('Save profile basic failed:', error);
    alert(error.response?.data?.message || '保存基础资料失败');
  } finally {
    isSavingProfile.value = false;
  }
};

const handleAvatarUpload = async (event) => {
  const [file] = event.target.files || [];
  event.target.value = '';

  if (!file) {
    return;
  }

  isUploadingAvatar.value = true;
  try {
    const uploadResponse = await wardrobeApi.uploadImage(null, file);
    const imageUrl = uploadResponse.data;
    const profileResponse = await authApi.updateAvatar(imageUrl);
    applyProfile(profileResponse.data || {});
    alert('头像已更新');
  } catch (error) {
    console.error('Upload avatar failed:', error);
    alert(error.response?.data?.message || '上传头像失败');
  } finally {
    isUploadingAvatar.value = false;
  }
};

const goBack = () => {
  router.push('/profile/account');
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

.content-card {
  padding: 16px;
}

.avatar-section {
  display: grid;
  justify-items: center;
  gap: 14px;
  margin-bottom: 22px;
}

.avatar-shell {
  width: 92px;
  height: 92px;
  border-radius: 30px;
  overflow: hidden;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, var(--accent-strong) 0%, var(--accent) 100%);
  color: var(--surface-strong);
  font-size: 32px;
  font-weight: 700;
}

.avatar-shell img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-button {
  border-radius: 999px;
  padding: 10px 16px;
  background: var(--accent-soft);
  color: var(--accent-strong);
  cursor: pointer;
}

.avatar-button input {
  display: none;
}

.avatar-button.disabled {
  opacity: 0.6;
  cursor: not-allowed;
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

.form-grid input,
.form-grid select {
  border: 1px solid var(--line-soft);
  border-radius: 16px;
  padding: 12px 14px;
  background: var(--surface-strong);
  color: var(--text-main);
}

.wide {
  grid-column: 1 / -1;
}

.primary-button {
  width: 100%;
  margin-top: 22px;
  border: none;
  border-radius: 999px;
  padding: 14px 18px;
  background: var(--accent-strong);
  color: var(--surface-strong);
  cursor: pointer;
}

.primary-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (min-width: 768px) {
  .detail-page {
    padding: 28px 24px 40px;
  }

  .form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
