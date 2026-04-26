<template>
  <div class="detail-page">
    <header class="page-header">
      <button class="icon-back" type="button" aria-label="返回" @click="goBack">
        <span></span>
      </button>
      <div>
        <p class="eyebrow">首页轮播文案</p>
        <h1>展示节奏</h1>
      </div>
    </header>

    <div v-if="isLoading" class="state-card">正在读取展示设置...</div>

    <section v-else class="card">
      <div class="setting-row">
        <div class="setting-copy">
          <strong>今日推荐默认展开</strong>
          <span>{{ settings.homeFlowDefaultVisible ? '进入首页时直接展开推荐内容' : '进入首页时默认收起推荐内容' }}</span>
        </div>
        <button
          :class="['switch-button', settings.homeFlowDefaultVisible ? 'active' : '']"
          type="button"
          @click="toggleHomeFlowDefault"
        >
          <span></span>
        </button>
      </div>

      <label class="field-block">
        <span>文案停留时间</span>
        <select v-model.number="settings.homePromptStayMs">
          <option v-for="item in stayDurationOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </option>
        </select>
      </label>

      <label class="field-block">
        <span>淡入淡出速度</span>
        <select v-model.number="settings.homePromptFadeMs">
          <option v-for="item in fadeDurationOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </option>
        </select>
      </label>

      <button class="primary-button" type="button" :disabled="isSaving" @click="saveSettings">
        {{ isSaving ? '保存中...' : '保存展示设置' }}
      </button>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { authApi } from '../api/auth';
import {
  buildHomePromptPayload,
  fadeDurationOptions,
  normalizeHomePromptSettings,
  stayDurationOptions
} from '../utils/homePromptSettings';

const router = useRouter();

const isLoading = ref(true);
const isSaving = ref(false);
const settings = ref(normalizeHomePromptSettings());

const loadPage = async () => {
  isLoading.value = true;
  try {
    const response = await authApi.getHomePromptSettings();
    settings.value = normalizeHomePromptSettings(response.data || {});
  } catch (error) {
    console.error('Load display settings failed:', error);
    alert(error.response?.data?.message || '读取展示设置失败');
  } finally {
    isLoading.value = false;
  }
};

const toggleHomeFlowDefault = () => {
  settings.value = {
    ...settings.value,
    homeFlowDefaultVisible: !settings.value.homeFlowDefaultVisible
  };
};

const saveSettings = async () => {
  isSaving.value = true;
  try {
    const response = await authApi.updateHomePromptSettings(buildHomePromptPayload(settings.value));
    settings.value = normalizeHomePromptSettings(response.data || {});
    alert('展示设置已保存');
  } catch (error) {
    console.error('Save display settings failed:', error);
    alert(error.response?.data?.message || '保存展示设置失败');
  } finally {
    isSaving.value = false;
  }
};

const goBack = () => {
  router.push('/profile/system/home');
};

onMounted(loadPage);
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  padding: 20px 16px 40px;
  background:
    radial-gradient(circle at top, rgba(255, 252, 246, 0.92), transparent 28%),
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
  background: color-mix(in srgb, var(--surface-strong) 92%, transparent);
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

.card,
.state-card {
  border-radius: 22px;
  border: 1px solid var(--line-soft);
  background: color-mix(in srgb, var(--surface-strong) 92%, transparent);
  box-shadow: var(--shadow-card);
}

.card {
  padding: 16px;
}

.state-card {
  padding: 22px 16px;
  text-align: center;
  color: var(--text-soft);
  font-size: 12px;
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.setting-copy strong,
.field-block > span {
  display: block;
  color: var(--text-main);
  font-size: 13px;
}

.setting-copy span {
  display: block;
  margin-top: 4px;
  color: var(--text-soft);
  font-size: 10px;
  line-height: 1.55;
}

.switch-button {
  position: relative;
  flex: none;
  width: 46px;
  height: 28px;
  border: none;
  border-radius: 999px;
  background: var(--line-soft);
  cursor: pointer;
}

.switch-button span {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--surface-strong);
  transition: transform 0.2s ease;
}

.switch-button.active {
  background: var(--accent-strong);
}

.switch-button.active span {
  transform: translateX(18px);
}

.field-block {
  display: block;
  margin-top: 18px;
}

.field-block select {
  width: 100%;
  margin-top: 10px;
  padding: 12px 14px;
  border: 1px solid var(--line-soft);
  border-radius: 18px;
  background: color-mix(in srgb, var(--surface-strong) 92%, transparent);
  color: var(--text-main);
  font-size: 12px;
  outline: none;
}

.primary-button {
  width: 100%;
  margin-top: 16px;
  padding: 11px 16px;
  border: none;
  border-radius: 999px;
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
}
</style>
