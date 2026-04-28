<template>
  <div class="style-learning-page">
    <header class="page-header">
      <button class="icon-back" type="button" aria-label="返回上一级" @click="goBack">
        <span></span>
      </button>
      <div>
        <p class="eyebrow">风格偏好学习</p>
        <h1>用喜欢和跳过，训练你的穿搭偏好</h1>
        <p class="page-copy">
          每次选择都会帮助系统更快理解你偏爱的风格方向、场景气质和搭配倾向。
        </p>
      </div>
    </header>

    <section class="summary-grid">
      <article class="summary-card progress-card">
        <div class="progress-head">
          <div>
            <p class="eyebrow">学习进度</p>
            <h2>当前偏好建模</h2>
          </div>
          <strong>{{ progress }}%</strong>
        </div>
        <div class="progress-track">
          <span :style="{ width: `${progress}%` }"></span>
        </div>
        <p class="summary-copy">
          当进度接近 100% 时，推荐流和搭配建议会更贴近你的真实审美。
        </p>
      </article>

      <article class="summary-card">
        <div class="section-head">
          <div>
            <p class="eyebrow">当前画像</p>
            <h2>已学习到的偏好</h2>
          </div>
          <button class="ghost-button small" type="button" :disabled="isResetting" @click="resetProfile">
            {{ isResetting ? '重置中...' : '重置偏好' }}
          </button>
        </div>

        <div v-if="topPreferences.length" class="preference-list">
          <article
            v-for="item in topPreferences"
            :key="item.styleTag.id || item.styleTag.name"
            class="preference-item"
          >
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
        <p v-else class="empty-tip">还没有形成明确偏好，先看几组风格图试试。</p>
      </article>
    </section>

    <section class="filter-card">
      <div class="section-head">
        <div>
          <p class="eyebrow">风格筛选</p>
          <h2>按方向浏览灵感图</h2>
        </div>
        <button class="ghost-button small" type="button" :disabled="isLoadingImages" @click="refreshQueue">
          {{ isLoadingImages ? '刷新中...' : '换一组图片' }}
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
      <div v-if="isLoadingImages" class="state-card">正在加载风格图片...</div>

      <div v-else-if="currentImage" class="learning-shell">
        <div class="image-shell">
          <img :src="currentImage.imageUrl" :alt="currentImage.title" />
        </div>

        <div class="image-panel">
          <div class="image-head">
            <div>
              <p class="eyebrow">当前图片</p>
              <h2>{{ currentImage.title }}</h2>
            </div>
            <span class="category-badge">{{ currentImage.styleCategory || selectedCategory }}</span>
          </div>

          <div class="meta-grid">
            <div>
              <span>来源</span>
              <strong>{{ currentImage.source || '未标注来源' }}</strong>
            </div>
            <div>
              <span>参考价格</span>
              <strong>{{ formatPrice(currentImage.price) }}</strong>
            </div>
          </div>

          <div class="tag-block">
            <p>相关标签</p>
            <div class="tag-row">
              <span v-for="tag in currentImage.tags" :key="tag.id || tag.name" class="tag-chip">
                {{ tag.name }}
              </span>
              <span v-if="currentImage.tags.length === 0" class="muted-text">这张图暂时还没有标签。</span>
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
            多点几轮后，系统会更稳定地识别你偏爱的风格路线，并反映到首页推荐和搭配建议里。
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
        <p>当前没有可展示的风格图。</p>
        <p class="muted-text">你可以刷新图片，或者切换一个风格分类再试。</p>
        <button class="primary-button" type="button" @click="refreshQueue">重新加载</button>
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
const progress = ref(0);
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
    return '暂无';
  }

  const numeric = Number(price);
  if (Number.isNaN(numeric)) {
    return String(price);
  }

  return `¥${numeric.toFixed(2)}`;
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
  if (!window.confirm('确认重置当前风格偏好吗？已学习到的偏好分数会清空。')) {
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
  padding: 20px 16px 40px;
  background:
    radial-gradient(circle at top left, color-mix(in srgb, var(--accent-soft) 70%, transparent), transparent 32%),
    radial-gradient(circle at bottom right, color-mix(in srgb, var(--surface-quiet) 92%, transparent), transparent 28%),
    linear-gradient(180deg, var(--bg-base) 0%, var(--bg-soft) 100%);
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

.icon-back {
  display: inline-grid;
  place-items: center;
  width: 34px;
  height: 34px;
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

.page-copy,
.summary-copy,
.muted-text {
  color: var(--text-soft);
}

.eyebrow {
  margin-bottom: 6px;
  color: var(--text-faint);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
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
  background: color-mix(in srgb, var(--surface-strong) 94%, transparent);
  border: 1px solid var(--line-soft);
  box-shadow: var(--shadow-soft);
}

.summary-card,
.filter-card,
.learning-card,
.state-card {
  padding: 18px;
}

.progress-card strong {
  color: var(--accent-strong);
  font-size: 32px;
}

.progress-track,
.score-track {
  overflow: hidden;
  border-radius: 999px;
  background: color-mix(in srgb, var(--accent-soft) 70%, var(--surface-quiet));
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
  background: linear-gradient(90deg, var(--accent) 0%, var(--accent-strong) 100%);
}

.preference-list {
  display: grid;
  gap: 12px;
}

.preference-item {
  padding: 12px 14px;
  border-radius: 18px;
  background: var(--surface-strong);
}

.preference-meta strong,
.meta-grid strong,
.image-panel h2 {
  color: var(--text-main);
}

.preference-meta span,
.preference-meta em,
.meta-grid span,
.category-badge {
  color: var(--text-faint);
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
  background: var(--accent-soft);
  color: var(--accent-strong);
}

.filter-chip.active {
  background: var(--accent-strong);
  color: var(--surface-strong);
}

.learning-shell {
  align-items: start;
}

.image-shell {
  overflow: hidden;
  border-radius: 24px;
  background: #fff;
  border: 1px solid var(--line-soft);
  min-height: 320px;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.4);
}

.image-shell img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #fff;
}

.image-panel {
  display: grid;
  gap: 16px;
}

.category-badge,
.tag-chip {
  padding: 8px 12px;
  background: var(--accent-soft);
}

.meta-grid {
  gap: 12px;
}

.meta-grid > div {
  flex: 1;
  padding: 14px;
  border-radius: 18px;
  background: var(--surface-strong);
}

.tag-block p {
  margin-bottom: 10px;
  color: var(--text-soft);
}

.action-row {
  gap: 12px;
}

.feedback-button {
  flex: 1;
  padding: 14px 16px;
  color: #fff;
  font-weight: 600;
}

.feedback-button.dislike {
  background: var(--danger);
}

.feedback-button.skip {
  background: var(--accent);
}

.feedback-button.like {
  background: var(--accent-strong);
}

.source-link {
  color: var(--accent-strong);
  font-weight: 600;
}

.empty-tip,
.state-card {
  color: var(--text-soft);
}

.state-card {
  text-align: center;
}

.ghost-button,
.primary-button {
  padding: 10px 16px;
}

.ghost-button {
  background: var(--accent-soft);
  color: var(--accent-strong);
}

.ghost-button.small {
  padding: 8px 12px;
}

.primary-button {
  margin-top: 12px;
  background: var(--accent-strong);
  color: var(--surface-strong);
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
