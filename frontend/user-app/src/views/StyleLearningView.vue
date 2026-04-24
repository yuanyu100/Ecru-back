<template>
  <div class="style-learning-page">
    <header class="page-header">
      <button class="ghost-button" type="button" @click="goHome">返回首页</button>
      <div>
        <p class="eyebrow">Style Learning</p>
        <h1>风格偏好学习</h1>
        <p class="page-copy">通过快速标记喜欢、不喜欢或跳过，让推荐系统更懂你的审美取向。</p>
      </div>
    </header>

    <section class="summary-grid">
      <article class="summary-card progress-card">
        <div class="progress-head">
          <div>
            <p class="eyebrow">Learning Progress</p>
            <h2>学习进度</h2>
          </div>
          <strong>{{ progress }}%</strong>
        </div>
        <div class="progress-track">
          <span :style="{ width: `${progress}%` }"></span>
        </div>
        <p class="summary-copy">
          当前画像会基于你的标记持续更新。进度达到 100% 后，推荐会更稳定。
        </p>
      </article>

      <article class="summary-card">
        <div class="section-head">
          <div>
            <p class="eyebrow">Preference Map</p>
            <h2>当前偏好画像</h2>
          </div>
          <button class="ghost-button small" type="button" :disabled="isResetting" @click="resetProfile">
            {{ isResetting ? '重置中...' : '重置画像' }}
          </button>
        </div>

        <div v-if="topPreferences.length" class="preference-list">
          <article v-for="item in topPreferences" :key="item.styleTag.id || item.styleTag.name" class="preference-item">
            <div class="preference-meta">
              <div>
                <strong>{{ item.styleTag.name || '未命名标签' }}</strong>
                <span>{{ item.styleTag.category || '未分类' }}</span>
              </div>
              <em>{{ formatScore(item.preferenceScore) }}</em>
            </div>
            <div class="score-track">
              <span :style="{ width: `${toScoreWidth(item.preferenceScore)}%` }"></span>
            </div>
          </article>
        </div>
        <p v-else class="empty-tip">还没有形成明显的偏好画像，先完成几次标记。</p>
      </article>
    </section>

    <section class="filter-card">
      <div class="section-head">
        <div>
          <p class="eyebrow">Category</p>
          <h2>筛选风格方向</h2>
        </div>
        <button class="ghost-button small" type="button" :disabled="isLoadingImages" @click="refreshQueue">
          {{ isLoadingImages ? '加载中...' : '换一组图片' }}
        </button>
      </div>

      <div class="chip-row">
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
      <div v-if="isLoadingImages" class="state-card">正在加载风格图...</div>

      <div v-else-if="currentImage" class="learning-shell">
        <div class="image-shell">
          <img :src="currentImage.imageUrl" :alt="currentImage.title" />
        </div>

        <div class="image-panel">
          <div class="image-head">
            <div>
              <p class="eyebrow">Current Card</p>
              <h2>{{ currentImage.title }}</h2>
            </div>
            <span class="category-badge">{{ currentImage.styleCategory || selectedCategory }}</span>
          </div>

          <div class="meta-grid">
            <div>
              <span>来源</span>
              <strong>{{ currentImage.source || '未知来源' }}</strong>
            </div>
            <div>
              <span>价格</span>
              <strong>{{ formatPrice(currentImage.price) }}</strong>
            </div>
          </div>

          <div class="tag-block">
            <p>关联标签</p>
            <div class="tag-row">
              <span v-for="tag in currentImage.tags" :key="tag.id || tag.name" class="tag-chip">
                {{ tag.name }}
              </span>
              <span v-if="currentImage.tags.length === 0" class="muted-text">当前图片还没有标签信息</span>
            </div>
          </div>

          <div class="action-row">
            <button
              class="feedback-button dislike"
              type="button"
              :disabled="isSubmitting"
              @click="submitFeedback(2)"
            >
              不喜欢
            </button>
            <button
              class="feedback-button skip"
              type="button"
              :disabled="isSubmitting"
              @click="submitFeedback(0)"
            >
              跳过
            </button>
            <button
              class="feedback-button like"
              type="button"
              :disabled="isSubmitting"
              @click="submitFeedback(1)"
            >
              喜欢
            </button>
          </div>

          <p class="summary-copy">
            每次标记都会更新你的风格画像，后续 AI 穿搭建议会更贴近这些偏好。
          </p>

          <a
            v-if="currentImage.sourceUrl"
            class="source-link"
            :href="currentImage.sourceUrl"
            target="_blank"
            rel="noreferrer"
          >
            查看原始链接
          </a>
        </div>
      </div>

      <div v-else class="state-card">
        <p>当前没有可用的风格图。</p>
        <p class="muted-text">可能是样本数据还没准备好，也可能这组图片已经全部标记完了。</p>
        <button class="primary-button" type="button" @click="refreshQueue">重新尝试</button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { stylePreferenceApi } from '../api/stylePreference';

const router = useRouter();
const progress = ref(0);
const topPreferences = ref([]);
const categories = ref([]);
const selectedCategory = ref('全部');
const imageQueue = ref([]);
const isLoadingImages = ref(false);
const isSubmitting = ref(false);
const isResetting = ref(false);
const seenImageIds = new Set();

const categoryOptions = computed(() => ['全部', ...categories.value.filter(Boolean)]);
const currentImage = computed(() => imageQueue.value[0] || null);

const toScoreWidth = (score) => {
  const normalized = (Number(score || 0) + 1) / 2;
  return Math.max(6, Math.min(100, Math.round(normalized * 100)));
};

const formatScore = (score) => {
  const numeric = Number(score || 0);
  return numeric > 0 ? `+${numeric.toFixed(2)}` : numeric.toFixed(2);
};

const formatPrice = (price) => {
  if (price === null || price === undefined || price === '') {
    return '未提供';
  }

  const numeric = Number(price);
  if (Number.isNaN(numeric)) {
    return String(price);
  }

  return `¥${numeric.toFixed(2)}`;
};

const syncSummary = async () => {
  const [progressResult, topResult, categoryResult] = await Promise.all([
    stylePreferenceApi.getLearningProgress().catch(() => ({ data: 0 })),
    stylePreferenceApi.getTopPreferences(6).catch(() => ({ data: [] })),
    stylePreferenceApi.getCategories().catch(() => ({ data: [] }))
  ]);

  progress.value = progressResult.data || 0;
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
      selectedCategory.value === '全部'
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
  if (!window.confirm('确认清空当前风格画像和已标记记录吗？')) {
    return;
  }

  isResetting.value = true;
  try {
    await stylePreferenceApi.resetProfile();
    topPreferences.value = [];
    progress.value = 0;
    await refreshQueue();
  } catch (error) {
    console.error('Reset style profile failed:', error);
    alert(error.response?.data?.message || '重置风格画像失败');
  } finally {
    isResetting.value = false;
  }
};

const goHome = () => {
  router.push('/');
};

onMounted(async () => {
  await syncSummary();
  await fetchImages({ allowReuse: true });
});
</script>

<style scoped>
.style-learning-page {
  min-height: 100vh;
  padding: 20px 16px 40px;
  background:
    radial-gradient(circle at top left, rgba(255, 243, 214, 0.86), transparent 32%),
    radial-gradient(circle at bottom right, rgba(198, 138, 74, 0.18), transparent 28%),
    linear-gradient(180deg, #f7efdc 0%, #ead9b8 100%);
}

.page-header,
.progress-head,
.section-head,
.image-head,
.meta-grid,
.preference-meta,
.action-row {
  display: flex;
}

.page-header,
.progress-head,
.section-head,
.image-head,
.preference-meta,
.action-row {
  align-items: center;
  justify-content: space-between;
}

.page-header {
  gap: 14px;
  margin-bottom: 18px;
}

.page-copy,
.summary-copy,
.muted-text {
  color: #7a6040;
}

.eyebrow {
  margin-bottom: 6px;
  color: #8f6a37;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.page-header h1,
.summary-card h2,
.image-panel h2 {
  color: #5d4523;
}

.summary-grid,
.learning-shell {
  display: grid;
  gap: 16px;
}

.summary-card,
.filter-card,
.learning-card,
.state-card {
  border-radius: 28px;
  background: rgba(255, 251, 244, 0.94);
  border: 1px solid rgba(145, 104, 49, 0.14);
  box-shadow: 0 18px 44px rgba(109, 78, 38, 0.08);
}

.summary-card,
.filter-card,
.learning-card,
.state-card {
  padding: 18px;
}

.progress-card strong {
  color: #6b4b1f;
  font-size: 32px;
}

.progress-track,
.score-track {
  overflow: hidden;
  border-radius: 999px;
  background: #f0e1c5;
}

.progress-track {
  height: 12px;
  margin: 14px 0 12px;
}

.progress-track span,
.score-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #b77b30 0%, #6b4b1f 100%);
}

.preference-list {
  display: grid;
  gap: 12px;
}

.preference-item {
  padding: 12px 14px;
  border-radius: 18px;
  background: #fffdf8;
}

.preference-meta strong {
  display: block;
  color: #5d4523;
}

.preference-meta span,
.preference-meta em,
.meta-grid span,
.category-badge {
  color: #8b6f48;
  font-size: 12px;
}

.preference-meta em {
  font-style: normal;
  font-weight: 700;
}

.score-track {
  height: 10px;
  margin-top: 10px;
}

.chip-row,
.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-chip,
.tag-chip,
.ghost-button,
.primary-button,
.feedback-button {
  border-radius: 999px;
}

.filter-chip,
.ghost-button,
.primary-button,
.feedback-button {
  border: none;
  cursor: pointer;
}

.filter-chip {
  padding: 10px 14px;
  background: #f3e4c9;
  color: #6b4b1f;
}

.filter-chip.active {
  background: #6b4b1f;
  color: #fff8ef;
}

.learning-shell {
  align-items: start;
}

.image-shell {
  overflow: hidden;
  border-radius: 24px;
  background: #f0e1c5;
  min-height: 320px;
}

.image-shell img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-panel {
  display: grid;
  gap: 16px;
}

.category-badge,
.tag-chip {
  padding: 8px 12px;
  background: #f1debd;
}

.meta-grid {
  gap: 12px;
}

.meta-grid > div {
  flex: 1;
  padding: 14px;
  border-radius: 18px;
  background: #fffdf8;
}

.meta-grid strong {
  display: block;
  margin-top: 8px;
  color: #5d4523;
}

.tag-block p {
  margin-bottom: 10px;
  color: #6e5433;
}

.action-row {
  gap: 12px;
}

.feedback-button {
  flex: 1;
  padding: 14px 16px;
  color: #fffaf4;
  font-weight: 600;
}

.feedback-button.dislike {
  background: #d65f53;
}

.feedback-button.skip {
  background: #b49568;
}

.feedback-button.like {
  background: #6b4b1f;
}

.source-link {
  color: #7d5930;
  text-decoration: none;
  font-weight: 600;
}

.empty-tip,
.state-card {
  color: #6d573b;
}

.state-card {
  text-align: center;
}

.ghost-button,
.primary-button {
  padding: 10px 16px;
}

.ghost-button {
  background: #ead7b8;
  color: #5d4523;
}

.ghost-button.small {
  padding: 8px 12px;
}

.primary-button {
  margin-top: 12px;
  background: #6b4b1f;
  color: #fff8ef;
}

.ghost-button:disabled,
.feedback-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (min-width: 900px) {
  .style-learning-page {
    padding: 28px 28px 48px;
  }

  .summary-grid {
    grid-template-columns: minmax(280px, 360px) minmax(0, 1fr);
  }

  .learning-shell {
    grid-template-columns: minmax(320px, 460px) minmax(0, 1fr);
  }

  .image-shell {
    min-height: 560px;
  }
}
</style>
