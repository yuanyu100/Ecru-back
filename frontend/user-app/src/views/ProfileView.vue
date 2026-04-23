<template>
  <div class="profile-page">
    <header class="page-header">
      <button class="ghost-button" type="button" @click="goHome">返回首页</button>
      <div>
        <p class="eyebrow">Account Center</p>
        <h1>个人设置</h1>
      </div>
    </header>

    <div v-if="isLoading" class="state-card">正在加载账户信息...</div>

    <div v-else class="content-grid">
      <section class="panel profile-hero">
        <div class="avatar-shell">
          <img v-if="avatarPreview" :src="avatarPreview" alt="avatar" />
          <span v-else>{{ displayInitial }}</span>
        </div>

        <div class="hero-copy">
          <p class="user-name">{{ profileForm.nickname || currentProfile.username || '未命名用户' }}</p>
          <p class="user-meta">{{ currentProfile.email || '未设置邮箱' }}</p>
          <p class="user-meta">角色：{{ currentProfile.role || 'USER' }}</p>
        </div>

        <label :class="['upload-avatar', isUploadingAvatar ? 'disabled' : '']">
          <input type="file" accept="image/*" :disabled="isUploadingAvatar" @change="handleAvatarUpload" />
          {{ isUploadingAvatar ? '上传中...' : '更新头像' }}
        </label>
      </section>

      <section class="panel">
        <div class="section-heading">
          <div>
            <p class="eyebrow">Profile</p>
            <h2>基础资料</h2>
          </div>
          <button class="primary-button" type="button" :disabled="isSavingProfile" @click="saveProfile">
            {{ isSavingProfile ? '保存中...' : '保存资料' }}
          </button>
        </div>

        <div class="form-grid">
          <label>
            <span>昵称</span>
            <input v-model.trim="profileForm.nickname" type="text" maxlength="50" placeholder="请输入昵称" />
          </label>
          <label>
            <span>邮箱</span>
            <input v-model.trim="profileForm.email" type="email" placeholder="例如：name@example.com" />
          </label>
          <label>
            <span>手机号</span>
            <input v-model.trim="profileForm.phone" type="text" placeholder="例如：13800138000" />
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
        </div>
      </section>

      <section class="panel">
        <div class="section-heading">
          <div>
            <p class="eyebrow">Preference</p>
            <h2>穿搭偏好</h2>
          </div>
          <button class="primary-button" type="button" :disabled="isSavingSettings" @click="saveSettings">
            {{ isSavingSettings ? '保存中...' : '保存偏好' }}
          </button>
        </div>

        <div class="chip-group">
          <button
            v-for="style in availableStyles"
            :key="style"
            type="button"
            :class="['chip', settingsForm.stylePreferences.includes(style) ? 'active' : '']"
            @click="toggleStyle(style)"
          >
            {{ style }}
          </button>
        </div>

        <div class="form-grid">
          <label>
            <span>常用尺码</span>
            <input v-model.trim="settingsForm.usualSize" type="text" placeholder="例如：M / 38 / 42" />
          </label>
          <label>
            <span>常驻地区</span>
            <input v-model.trim="settingsForm.region" type="text" placeholder="例如：上海 / 杭州" />
          </label>
        </div>
      </section>

      <section class="panel">
        <div class="section-heading">
          <div>
            <p class="eyebrow">Security</p>
            <h2>修改密码</h2>
          </div>
          <button class="primary-button" type="button" :disabled="isUpdatingPassword" @click="savePassword">
            {{ isUpdatingPassword ? '提交中...' : '更新密码' }}
          </button>
        </div>

        <div class="form-grid">
          <label>
            <span>当前密码</span>
            <input v-model="passwordForm.oldPassword" type="password" placeholder="请输入当前密码" />
          </label>
          <label>
            <span>新密码</span>
            <input v-model="passwordForm.newPassword" type="password" placeholder="不少于 6 位" />
          </label>
          <label class="wide">
            <span>确认新密码</span>
            <input v-model="passwordForm.confirmPassword" type="password" placeholder="再次输入新密码" />
          </label>
        </div>
      </section>

      <section class="panel account-panel">
        <div>
          <p class="eyebrow">Account</p>
          <h2>账户状态</h2>
        </div>

        <dl class="meta-list">
          <div>
            <dt>用户名</dt>
            <dd>{{ currentProfile.username || '-' }}</dd>
          </div>
          <div>
            <dt>注册时间</dt>
            <dd>{{ formatDate(currentProfile.createdAt) }}</dd>
          </div>
          <div>
            <dt>最近登录</dt>
            <dd>{{ formatDate(currentProfile.lastLoginAt) }}</dd>
          </div>
        </dl>

        <div class="account-actions">
          <button class="ghost-button" type="button" @click="goWardrobe">我的衣橱</button>
          <button class="danger-button" type="button" @click="logout">退出登录</button>
        </div>
      </section>
    </div>
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
const isSavingSettings = ref(false);
const isUpdatingPassword = ref(false);
const isUploadingAvatar = ref(false);
const currentProfile = ref({});
const avatarPreview = ref('');

const availableStyles = ['通勤', '休闲', '运动', '复古', '极简', '甜酷', '学院', '户外'];

const profileForm = reactive({
  nickname: '',
  email: '',
  phone: '',
  gender: 0,
  birthday: ''
});

const settingsForm = reactive({
  stylePreferences: [],
  usualSize: '',
  region: ''
});

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const displayInitial = computed(() => {
  const source = profileForm.nickname || currentProfile.value.username || 'E';
  return source.charAt(0).toUpperCase();
});

const parseStylePreferences = (value) => {
  if (!value) {
    return [];
  }

  if (Array.isArray(value)) {
    return value.filter(Boolean);
  }

  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed.filter(Boolean) : [];
  } catch (_error) {
    return String(value)
      .split(/[，,]/)
      .map((item) => item.trim())
      .filter(Boolean);
  }
};

const applyProfile = (profile = {}) => {
  currentProfile.value = profile;
  profileForm.nickname = profile.nickname || '';
  profileForm.email = profile.email || '';
  profileForm.phone = profile.phone || '';
  profileForm.gender = Number(profile.gender ?? 0);
  profileForm.birthday = profile.birthday || '';
  avatarPreview.value = profile.avatarUrl || '';
};

const applySettings = (settings = {}) => {
  settingsForm.stylePreferences = parseStylePreferences(settings.stylePreferences);
  settingsForm.usualSize = settings.usualSize || '';
  settingsForm.region = settings.region || '';
};

const loadPage = async () => {
  isLoading.value = true;
  try {
    const [profileResponse, settingsResponse] = await Promise.all([
      authApi.getCurrentProfile(),
      authApi.getUserSettings()
    ]);

    applyProfile(profileResponse.data || {});
    applySettings(settingsResponse.data || {});
  } catch (error) {
    console.error('Load profile page failed:', error);
    alert(error.response?.data?.message || '加载个人信息失败');
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
    alert('基础资料已更新');
  } catch (error) {
    console.error('Save profile failed:', error);
    alert(error.response?.data?.message || '保存基础资料失败');
  } finally {
    isSavingProfile.value = false;
  }
};

const saveSettings = async () => {
  isSavingSettings.value = true;
  try {
    const response = await authApi.updateUserSettings({
      stylePreferences: JSON.stringify(settingsForm.stylePreferences),
      usualSize: settingsForm.usualSize || '',
      region: settingsForm.region || ''
    });

    applySettings(response.data || {});
    alert('穿搭偏好已更新');
  } catch (error) {
    console.error('Save settings failed:', error);
    alert(error.response?.data?.message || '保存偏好失败');
  } finally {
    isSavingSettings.value = false;
  }
};

const savePassword = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    alert('请先填写完整的密码信息');
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
    alert(error.response?.data?.message || '修改密码失败');
  } finally {
    isUpdatingPassword.value = false;
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
    alert(error.response?.data?.message || '头像上传失败');
  } finally {
    isUploadingAvatar.value = false;
  }
};

const toggleStyle = (style) => {
  if (settingsForm.stylePreferences.includes(style)) {
    settingsForm.stylePreferences = settingsForm.stylePreferences.filter((item) => item !== style);
    return;
  }

  settingsForm.stylePreferences = [...settingsForm.stylePreferences, style];
};

const logout = () => {
  if (!window.confirm('确认退出当前账号吗？')) {
    return;
  }

  authApi.logout();
  router.push('/login');
};

const goHome = () => {
  router.push('/');
};

const goWardrobe = () => {
  router.push('/wardrobe');
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
.profile-page {
  min-height: 100vh;
  padding: 20px 16px 40px;
  background:
    radial-gradient(circle at top, rgba(255, 243, 214, 0.86), transparent 34%),
    linear-gradient(180deg, #f8f1df 0%, #efe2ca 100%);
}

.page-header,
.section-heading,
.account-actions,
.meta-list div {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-header {
  gap: 14px;
  margin-bottom: 18px;
}

.eyebrow {
  margin-bottom: 6px;
  color: #8f6a37;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.page-header h1,
.panel h2 {
  color: #5d4523;
}

.state-card,
.panel {
  border-radius: 24px;
  background: rgba(255, 251, 244, 0.92);
  border: 1px solid rgba(145, 104, 49, 0.14);
  box-shadow: 0 18px 44px rgba(109, 78, 38, 0.08);
}

.state-card {
  padding: 32px 20px;
  text-align: center;
  color: #6d573b;
}

.content-grid {
  display: grid;
  gap: 16px;
}

.panel {
  padding: 18px;
}

.profile-hero {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 16px;
  align-items: center;
}

.avatar-shell {
  width: 84px;
  height: 84px;
  border-radius: 28px;
  overflow: hidden;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #6b4b1f 0%, #b48a52 100%);
  color: #fff8ef;
  font-size: 30px;
  font-weight: 700;
}

.avatar-shell img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-copy {
  min-width: 0;
}

.user-name {
  color: #5d4523;
  font-size: 22px;
  font-weight: 700;
}

.user-meta {
  margin-top: 6px;
  color: #7d6240;
  word-break: break-all;
}

.upload-avatar {
  grid-column: 1 / -1;
  justify-self: start;
  padding: 10px 14px;
  border-radius: 999px;
  background: #ead7b8;
  color: #5d4523;
  cursor: pointer;
}

.upload-avatar input {
  display: none;
}

.section-heading {
  gap: 12px;
  margin-bottom: 16px;
}

.form-grid {
  display: grid;
  gap: 14px;
}

.form-grid label {
  display: grid;
  gap: 8px;
  color: #6c522f;
  font-size: 14px;
}

.form-grid input,
.form-grid select {
  border: 1px solid #d9c39b;
  border-radius: 14px;
  padding: 12px 14px;
  background: #fffdf8;
  color: #5d4523;
}

.wide {
  grid-column: 1 / -1;
}

.chip-group {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.chip {
  border: 1px solid #d4bc93;
  border-radius: 999px;
  padding: 10px 14px;
  background: #fff8eb;
  color: #6d573b;
  cursor: pointer;
}

.chip.active {
  background: #6b4b1f;
  border-color: #6b4b1f;
  color: #fff8ef;
}

.meta-list {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.meta-list dt {
  color: #8b6f48;
  font-size: 13px;
}

.meta-list dd {
  color: #5d4523;
  font-weight: 600;
}

.account-actions {
  gap: 12px;
  margin-top: 18px;
}

.ghost-button,
.primary-button,
.danger-button {
  border: none;
  border-radius: 999px;
  padding: 10px 16px;
  cursor: pointer;
}

.ghost-button {
  background: #ead7b8;
  color: #5d4523;
}

.primary-button {
  background: #6b4b1f;
  color: #fff8ef;
}

.danger-button {
  background: #d95d51;
  color: #fffaf7;
}

.primary-button:disabled,
.upload-avatar.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (min-width: 900px) {
  .profile-page {
    padding: 28px 28px 48px;
  }

  .content-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .profile-hero,
  .account-panel {
    grid-column: span 2;
  }

  .form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
