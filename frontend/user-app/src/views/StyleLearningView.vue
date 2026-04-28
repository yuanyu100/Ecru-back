<template>
  <div class="style-learning-page">
    <header class="page-header">
      <button class="icon-back" type="button" aria-label="返回上一级" @click="goBack">
        <span></span>
      </button>
      <h1>风格学习</h1>
    </header>

    <section class="control-card">
      <div class="control-top">
        <p class="progress-copy">进度</p>
        <div class="tool-row">
          <span class="progress-percent">{{ progressPercent }}%</span>
          <button
            class="icon-button"
            type="button"
            aria-label="重置风格偏好"
            title="重置风格偏好"
            :disabled="isResetting"
            @click="resetProfile"
          >
            {{ isResetting ? '…' : '↺' }}
          </button>
        </div>
      </div>

      <div class="progress-track">
        <span :style="{ width: `${progressPercent}%` }"></span>
      </div>

      <div class="chip-row compact-scroll">
        <button
          v-for="category in categoryOptions"
          :key="category"
          type="button"
          :class="['filter-chip', selectedCategory === category ? 'active' : '']"
          @click="selectCategory(category)"
        >
          {{ category }}
        </button>
      </div>
    </section>

    <section class="learning-card">
      <div v-if="isLoadingImages" class="state-card">加载中...</div>

      <template v-else-if="currentImage">
        <div class="image-shell">
          <img :src="currentImage.imageUrl" :alt="currentImage.title || currentImage.styleCategory || '风格图片'" />
        </div>

        <div class="meta-row">
          <span v-if="currentImage.styleCategory" class="category-badge">
            {{ currentImage.styleCategory }}
          </span>
          <div v-if="visibleTags.length" class="tag-row compact-scroll">
            <span v-for="tag in visibleTags" :key="tag.id || tag.name" class="tag-chip">
              {{ tag.name }}
            </span>
          </div>
        </div>

        <div class="action-row">
          <button
            class="feedback-button dislike"
            type="button"
            aria-label="不喜欢"
            title="不喜欢"
            :disabled="isSubmitting"
            @click="submitFeedback(2)"
          >
            ×
          </button>
          <button
            class="feedback-button skip"
            type="button"
            aria-label="跳过"
            title="跳过"
            :disabled="isSubmitting"
            @click="submitFeedback(0)"
          >
            ○
          </button>
          <button
            class="feedback-button like"
            type="button"
            aria-label="喜欢"
            title="喜欢"
            :disabled="isSubmitting"
            @click="submitFeedback(1)"
          >
            ♥
          </button>
        </div>
      </template>

      <div v-else class="state-card">
        <p>当前没有可展示的图片</p>
        <button class="ghost-button" type="button" @click="refreshQueue">重新加载</button>
      </div>
    </section>

    <section class="profile-card">
      <div class="profile-head">
        <h2>我的风格</h2>
      </div>

      <div v-if="topPreferences.length" class="profile-grid">
        <article
          v-for="item in topPreferences"
          :key="item.styleTag.id || item.styleTag.name"
          class="profile-item"
        >
          <div class="profile-meta">
            <strong>{{ item.styleTag.name || '未命名' }}</strong>
            <span>{{ formatPreferenceScore(item.preferenceScore) }}</span>
          </div>
          <div class="mini-track">
            <span :style="{ width: `${toPreferenceWidth(item.preferenceScore)}%` }"></span>
          </div>
        </article>
      </div>

      <div v-else class="empty-profile">
        暂无风格
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { stylePreferenceApi } from '../api/stylePreference';

const ALL_CATEGORY = '全部';

const router = useRouter();
const progressPercent = ref(0);
const topPreferences = ref([]);
const categories = ref([]);
const selectedCategory = ref(ALL_CATEGORY);
const imageQueue = ref([]);
const isLoadingImages = ref(false);
const isSubmitting = ref(false);
const isResetting = ref(false);
const seenImageIds = new Set();

const categoryOptions = computed(() => [ALL_CATEGORY, ...categories.value.filter(Boolean)]);
const currentImage = computed(() => imageQueue.value[0] || null);
const visibleTags = computed(() => (currentImage.value?.tags || []).slice(0, 6));

const toPreferenceWidth = (score) => {
  const normalized = (Number(score || 0) + 1) / 2;
  return Math.max(10, Math.min(100, Math.round(normalized * 100)));
};

const formatPreferenceScore = (score) => {
  const numeric = Number(score || 0);
  return numeric > 0 ? `+${numeric.toFixed(2)}` : numeric.toFixed(2);
};

const goBack = () => {
  if (window.history.length > 1) {
    router.back();
    return;
  }

  router.push('/profile/system');
};

const syncSummary = async () => {
  const [progressResult, topResult, categoryResult] = await Promise.all([
    stylePreferenceApi.getLearningProgress().catch(() => ({
      data: {
        progressPercent: 0,
        coveredTagCount: 0,
        totalTagCount: 0
      }
    })),
    stylePreferenceApi.getTopPreferences(6).catch(() => ({ data: [] })),
    stylePreferenceApi.getCategories().catch(() => ({ data: [] }))
  ]);

  progressPercent.value = Number(progressResult.data?.progressPercent || 0);
  topPreferences.value = topResult.data || [];
  categories.value = categoryResult.data || [];
};

const pickUniqueImages = (items = []) => {
  const existingIds = new Set(imageQueue.value.map((item) => item.id));
  return items.filter((item) => item.id && !existingIds.has(item.id) && !seenImageIds.has(item.id));
};

const fetchImages = async ({ append = false, allowReuse = false } = {}) => {
  isLoadingImages.value = true;
  try {
    if (!append) {
      imageQueue.value = [];
    }

    const result =
      selectedCategory.value === ALL_CATEGORY
        ? await stylePreferenceApi.getRandomImages(12)
        : await stylePreferenceApi.getImagesByCategory(selectedCategory.value, 12);

    let nextItems = pickUniqueImages(result.data || []);
    if (!nextItems.length && allowReuse) {
      seenImageIds.clear();
      nextItems = (result.data || []).filter((item) => item.id);
    }

    imageQueue.value = append ? [...imageQueue.value, ...nextItems] : nextItems;
  } finally {
    isLoadingImages.value = false;
  }
};

const refreshQueue = async () => {
  seenImageIds.clear();
  await fetchImages({ allowReuse: true });
};

const selectCategory = async (category) => {
  if (selectedCategory.value === category && imageQueue.value.length) {
    return;
  }

  selectedCategory.value = category;
  seenImageIds.clear();
  await fetchImages({ allowReuse: true });
};

const submitFeedback = async (preferenceType) => {
  if (!currentImage.value || isSubmitting.value) {
    return;
  }

  const imageId = currentImage.value.id;
  isSubmitting.value = true;
  try {
    await stylePreferenceApi.submitFeedback(imageId, preferenceType);
    seenImageIds.add(imageId);
    imageQueue.value = imageQueue.value.filter((item) => item.id !== imageId);
    await syncSummary();

    if (imageQueue.value.length < 4) {
      await fetchImages({ append: true });
    }
  } catch (error) {
    console.error('Submit style preference failed:', error);
    alert(error.response?.data?.message || '提交风格反馈失败');
  } finally {
    isSubmitting.value = false;
  }
};

const resetProfile = async () => {
  if (!window.confirm('确认重置当前风格偏好吗？')) {
    return;
  }

  isResetting.value = true;
  try {
    await stylePreferenceApi.resetProfile();
    progressPercent.value = 0;
    topPreferences.value = [];
    seenImageIds.clear();
    await refreshQueue();
  } catch (error) {
    console.error('Reset style profile failed:', error);
    alert(error.response?.data?.message || '重置风格偏好失败');
  } finally {
    isResetting.value = false;
  }
};

onMounted(async () => {
  await syncSummary();
  await fetchImages({ allowReuse: true });
});
</script>

<style scoped>
.style-learning-page {
  min-height: 100vh;
  max-width: 760px;
  margin: 0 auto;
  padding: 14px 14px 28px;
  background:
    radial-gradient(circle at top left, color-mix(in srgb, var(--accent-soft) 24%, transparent), transparent 34%),
    linear-gradient(180deg, var(--bg-base) 0%, var(--bg-soft) 100%);
}

.page-header,
.control-top,
.tool-row,
.meta-row,
.action-row {
  display: flex;
  align-items: center;
}

.page-header,
.control-top,
.meta-row {
  justify-content: space-between;
}

.page-header {
  gap: 12px;
  margin-bottom: 10px;
}

.page-header h1 {
  flex: 1;
  margin: 0;
  color: var(--text-main);
  font-size: 18px;
  font-weight: 600;
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
  width: 9px;
  height: 9px;
  margin-left: 4px;
  border-left: 1.5px solid var(--text-main);
  border-bottom: 1.5px solid var(--text-main);
  transform: rotate(45deg);
}

.control-card,
.profile-card,
.learning-card,
.state-card {
  border: 1px solid var(--line-soft);
  border-radius: 22px;
  background: color-mix(in srgb, var(--surface-strong) 96%, transparent);
  box-shadow: var(--shadow-soft);
}

.control-card,
.profile-card,
.learning-card,
.state-card {
  padding: 12px;
}

.control-card,
.learning-card {
  margin-bottom: 10px;
}

.control-top {
  gap: 12px;
}

.progress-copy {
  margin: 0;
  color: var(--text-main);
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
}

.tool-row {
  gap: 8px;
}

.progress-percent {
  color: var(--text-main);
  font-size: 14px;
  font-weight: 600;
}

.progress-track {
  height: 6px;
  margin: 10px 0 12px;
  overflow: hidden;
  border-radius: 999px;
  background: color-mix(in srgb, var(--surface-quiet) 88%, transparent);
}

.progress-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--accent) 0%, var(--accent-strong) 100%);
}

.chip-row,
.tag-row {
  display: flex;
  gap: 8px;
}

.compact-scroll {
  overflow-x: auto;
  scrollbar-width: none;
}

.compact-scroll::-webkit-scrollbar {
  display: none;
}

.filter-chip,
.ghost-button,
.icon-button,
.feedback-button,
.category-badge,
.tag-chip {
  border: none;
  border-radius: 999px;
}

.filter-chip,
.ghost-button {
  background: color-mix(in srgb, var(--surface-quiet) 88%, transparent);
  color: var(--text-soft);
}

.filter-chip {
  flex: 0 0 auto;
  padding: 6px 11px;
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
}

.filter-chip.active {
  background: var(--text-main);
  color: var(--surface-strong);
}

.ghost-button {
  padding: 6px 10px;
  font-size: 12px;
  cursor: pointer;
}

.icon-button {
  display: inline-grid;
  place-items: center;
  width: 28px;
  height: 28px;
  padding: 0;
  background: color-mix(in srgb, var(--surface-quiet) 88%, transparent);
  color: var(--text-soft);
  font-size: 15px;
  cursor: pointer;
}

.learning-card {
  display: grid;
  gap: 10px;
}

.profile-card {
  display: grid;
  gap: 10px;
}

.profile-head,
.profile-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.profile-head h2,
.profile-meta strong {
  margin: 0;
  color: var(--text-main);
}

.profile-head h2 {
  font-size: 15px;
  font-weight: 600;
}

.profile-meta,
.empty-profile {
  color: var(--text-soft);
  font-size: 12px;
}

.profile-grid {
  display: grid;
  gap: 8px;
}

.profile-item {
  display: grid;
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--line-soft);
  border-radius: 16px;
  background: color-mix(in srgb, var(--surface-quiet) 56%, transparent);
}

.mini-track {
  height: 5px;
  overflow: hidden;
  border-radius: 999px;
  background: color-mix(in srgb, var(--surface-quiet) 92%, transparent);
}

.mini-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--accent) 0%, var(--accent-strong) 100%);
}

.image-shell {
  overflow: hidden;
  border: 1px solid var(--line-soft);
  border-radius: 18px;
  background: #fff;
  height: clamp(420px, 62vh, 720px);
}

.image-shell img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #fff;
}

.meta-row {
  gap: 10px;
  min-height: 30px;
}

.category-badge,
.tag-chip {
  padding: 6px 10px;
  background: color-mix(in srgb, var(--accent-soft) 58%, var(--surface-strong));
  color: var(--text-soft);
  font-size: 12px;
  white-space: nowrap;
}

.tag-row {
  flex: 1;
  justify-content: flex-end;
}

.action-row {
  gap: 8px;
}

.feedback-button {
  display: inline-flex;
  flex: 1;
  justify-content: center;
  min-height: 42px;
  background: color-mix(in srgb, var(--surface-quiet) 92%, transparent);
  color: var(--text-main);
  cursor: pointer;
  font-size: 22px;
  line-height: 1;
}

.feedback-button.dislike {
  color: #8b6e62;
}

.feedback-button.skip {
  color: var(--text-soft);
}

.feedback-button.like {
  color: #476056;
}

.state-card {
  text-align: center;
  color: var(--text-soft);
}

.state-card p {
  margin: 0 0 10px;
}

.empty-profile {
  padding: 6px 0 2px;
}

.ghost-button:disabled,
.feedback-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (min-width: 900px) {
  .image-shell {
    height: clamp(520px, 66vh, 760px);
  }
}

@media (max-width: 640px) {
  .style-learning-page {
    padding: 12px 12px 24px;
  }

  .control-top,
  .meta-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .tool-row,
  .tag-row,
  .action-row {
    width: 100%;
  }

  .tool-row {
    justify-content: space-between;
  }

  .tag-row {
    justify-content: flex-start;
  }

  .image-shell {
    height: clamp(360px, 54vh, 520px);
  }
}
</style>
