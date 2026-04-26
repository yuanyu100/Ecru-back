<template>
  <div class="detail-page">
    <header class="page-header">
      <button class="icon-back" type="button" aria-label="返回" @click="goBack">
        <span></span>
      </button>
      <div>
        <p class="eyebrow">我的</p>
        <h1>偏好设置</h1>
      </div>
    </header>

    <div v-if="isLoading" class="state-card">正在读取偏好设置...</div>

    <form v-else class="content-card" @submit.prevent="saveSettings">
      <section>
        <p class="section-caption">常用风格</p>
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
      </section>

      <section class="form-grid">
        <label class="wide">
          <span>首页提示语</span>
          <textarea
            v-model.trim="settingsForm.homePrompt"
            rows="3"
            maxlength="40"
            placeholder="例如：今天想穿成什么样。"
          ></textarea>
        </label>
        <label>
          <span>常穿尺码</span>
          <input v-model.trim="settingsForm.usualSize" type="text" placeholder="例如 S / 38 / 42" />
        </label>
        <label>
          <span>所在地区</span>
          <input v-model.trim="settingsForm.region" type="text" placeholder="例如 上海 / 杭州" />
        </label>
      </section>

      <button class="primary-button" type="submit" :disabled="isSavingSettings">
        {{ isSavingSettings ? '保存中...' : '保存偏好' }}
      </button>
    </form>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { authApi } from '../api/auth';

const router = useRouter();
const isLoading = ref(true);
const isSavingSettings = ref(false);

const availableStyles = ['原木感', '极简通勤', '法式松弛', '日常休闲', '低饱和', '复古', '都市轻熟', '黑白简约'];

const settingsForm = reactive({
  stylePreferences: [],
  usualSize: '',
  region: '',
  homePrompt: ''
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
      .split(/[，,、/]/)
      .map((item) => item.trim())
      .filter(Boolean);
  }
};

const applySettings = (settings = {}) => {
  settingsForm.stylePreferences = parseStylePreferences(settings.stylePreferences);
  settingsForm.usualSize = settings.usualSize || '';
  settingsForm.region = settings.region || '';
  settingsForm.homePrompt = settings.homePrompt || '今天想穿成什么样。';
};

const loadPage = async () => {
  isLoading.value = true;
  try {
    const response = await authApi.getUserSettings();
    applySettings(response.data || {});
  } catch (error) {
    console.error('Load profile preferences failed:', error);
    alert(error.response?.data?.message || '读取偏好设置失败');
  } finally {
    isLoading.value = false;
  }
};

const toggleStyle = (style) => {
  if (settingsForm.stylePreferences.includes(style)) {
    settingsForm.stylePreferences = settingsForm.stylePreferences.filter((item) => item !== style);
    return;
  }

  settingsForm.stylePreferences = [...settingsForm.stylePreferences, style];
};

const saveSettings = async () => {
  isSavingSettings.value = true;
  try {
    const response = await authApi.updateUserSettings({
      stylePreferences: JSON.stringify(settingsForm.stylePreferences),
      usualSize: settingsForm.usualSize || '',
      region: settingsForm.region || '',
      homePrompt: settingsForm.homePrompt || '今天想穿成什么样。'
    });
    applySettings(response.data || {});
    alert('偏好已保存');
  } catch (error) {
    console.error('Save profile preferences failed:', error);
    alert(error.response?.data?.message || '保存偏好设置失败');
  } finally {
    isSavingSettings.value = false;
  }
};

const goBack = () => {
  router.push('/profile');
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

.eyebrow,
.section-caption {
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
  padding: 24px 16px;
  text-align: center;
  color: var(--text-soft);
  font-size: 12px;
}

.content-card {
  padding: 16px;
}

.chip-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.chip {
  border: 1px solid var(--line-soft);
  border-radius: 999px;
  padding: 8px 12px;
  background: var(--surface-strong);
  color: var(--text-soft);
  font-size: 11px;
  cursor: pointer;
}

.chip.active {
  border-color: var(--accent-strong);
  background: var(--accent-strong);
  color: var(--surface-strong);
}

.form-grid {
  display: grid;
  gap: 14px;
  margin-top: 20px;
}

.form-grid label {
  display: grid;
  gap: 8px;
}

.form-grid span {
  color: var(--text-soft);
  font-size: 11px;
}

.form-grid input,
.form-grid textarea {
  width: 100%;
  border: 1px solid var(--line-soft);
  border-radius: 16px;
  padding: 12px 14px;
  background: var(--surface-strong);
  color: var(--text-main);
  font-size: 12px;
  outline: none;
}

.form-grid textarea {
  resize: vertical;
}

.wide {
  grid-column: 1 / -1;
}

.primary-button {
  width: 100%;
  margin-top: 22px;
  border: none;
  border-radius: 999px;
  padding: 13px 18px;
  background: var(--accent-strong);
  color: var(--surface-strong);
  font-size: 12px;
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
