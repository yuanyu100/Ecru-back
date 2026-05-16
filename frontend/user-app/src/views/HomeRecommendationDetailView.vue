<template>
  <div class="detail-page">
    <header class="detail-header">
      <button class="ghost-button" type="button" @click="goBack">‹</button>
      <div>
        <p class="eyebrow">Home Look</p>
        <h1>推荐搭配详情</h1>
      </div>
    </header>

    <div v-if="isLoading" class="state-card">正在加载搭配详情...</div>
    <div v-else-if="!look.id" class="state-card">这条推荐不存在，或者已经失效。</div>

    <div v-else class="content-grid">
      <section class="panel hero-panel">
        <div class="hero-copy">
          <div>
            <p class="eyebrow">Summary</p>
            <h2>{{ look.title }}</h2>
            <p>{{ look.note || '这套搭配没有额外说明。' }}</p>
          </div>
          <button class="primary-button" type="button" :disabled="isRefreshing" @click="refreshRecommendations">
            {{ isRefreshing ? '刷新中...' : '重新生成推荐流' }}
          </button>
        </div>

        <div class="hero-board">
          <div
            v-for="(item, index) in boardItems"
            :key="`${item.clothingId || item.name || index}`"
            :class="['board-piece', `piece-${index + 1}`]"
          >
            <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.name || '搭配单品'" loading="lazy" />
            <div v-else class="piece-placeholder">{{ shortCategory(item.category) }}</div>
          </div>
        </div>

        <div class="tag-row">
          <span v-for="tag in look.tags" :key="tag">#{{ tag }}</span>
        </div>
      </section>

      <section class="panel wide">
        <div class="section-header">
          <div>
            <p class="eyebrow">Items</p>
            <h2>单品清单</h2>
          </div>
          <span>{{ look.items.length }} 件</span>
        </div>

        <div class="item-grid">
          <article v-for="item in look.items" :key="item.clothingId || item.name" class="item-card">
            <div class="item-image">
              <img :src="item.imageUrl || fallbackImage" :alt="item.name || '单品图片'" />
            </div>

            <div class="item-copy">
              <div class="item-head">
                <div>
                  <h3>{{ item.name || '未命名单品' }}</h3>
                  <p>{{ item.category || '单品' }}{{ item.color ? ` / ${item.color}` : '' }}</p>
                </div>
                <span :class="['source-badge', item.fromWardrobe ? 'owned' : 'external']">
                  {{ item.fromWardrobe ? '来自衣柜' : '外部补位' }}
                </span>
              </div>

              <p class="item-reason">{{ item.reason || '这件单品被选入当前搭配。' }}</p>

              <div class="stats-row">
                <span>穿着次数 {{ item.wearCount ?? 0 }}</span>
                <span>当前频率 {{ item.frequencyLevel || 3 }}</span>
              </div>

              <div v-if="item.fromWardrobe && item.clothingId" class="frequency-editor">
                <span>希望搭配频率</span>
                <div class="frequency-options">
                  <button
                    v-for="level in frequencyLevels"
                    :key="level"
                    type="button"
                    :class="['frequency-chip', (item.frequencyLevel || 3) === level ? 'active' : '']"
                    :disabled="updatingItemIds.has(item.clothingId)"
                    @click="updateFrequency(item, level)"
                  >
                    {{ level }}
                  </button>
                </div>
              </div>
            </div>
          </article>
        </div>
      </section>

      <section class="panel wide">
        <div class="section-header">
          <div>
            <p class="eyebrow">Feedback</p>
            <h2>我的反馈</h2>
          </div>
        </div>

        <form class="feedback-form" @submit.prevent="submitFeedback">
          <div class="rating-grid">
            <label v-for="field in ratingFields" :key="field.key">
              <span>{{ field.label }}</span>
              <select v-model.number="feedbackForm[field.key]">
                <option :value="0">未评分</option>
                <option v-for="score in [1, 2, 3, 4, 5]" :key="score" :value="score">
                  {{ score }} 分
                </option>
              </select>
            </label>
          </div>

          <label class="feedback-switch">
            <input v-model="feedbackForm.isWorn" type="checkbox" />
            <span>我已经实际穿过这套搭配</span>
          </label>

          <label v-if="feedbackForm.isWorn">
            <span>穿着日期</span>
            <input v-model="feedbackForm.wornAt" type="date" />
          </label>

          <label>
            <span>文字反馈</span>
            <textarea
              v-model.trim="feedbackForm.feedbackText"
              rows="4"
              placeholder="例如：颜色不错，但更适合天气再凉一点的时候穿。"
            ></textarea>
          </label>

          <div class="feedback-actions">
            <p v-if="savedFeedback" class="muted-text">上次更新：{{ formatTime(savedFeedback.updatedAt) }}</p>
            <button class="primary-button" type="submit" :disabled="isSubmittingFeedback">
              {{ isSubmittingFeedback ? '提交中...' : '保存反馈' }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { outfitApi } from '../api/outfit';
import { wardrobeApi } from '../api/wardrobe';

const route = useRoute();
const router = useRouter();

const isLoading = ref(false);
const isRefreshing = ref(false);
const isSubmittingFeedback = ref(false);
const updatingItemIds = ref(new Set());
const frequencyLevels = [1, 2, 3, 4, 5];
const savedFeedback = ref(null);

const feedbackForm = reactive({
  overallRating: 0,
  styleRating: 0,
  practicalityRating: 0,
  weatherRating: 0,
  isWorn: false,
  wornAt: '',
  feedbackText: ''
});

const ratingFields = [
  { key: 'overallRating', label: '整体评分' },
  { key: 'styleRating', label: '风格匹配' },
  { key: 'practicalityRating', label: '实用性' },
  { key: 'weatherRating', label: '天气适配' }
];
const fallbackImage =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="400" height="480"><rect width="100%" height="100%" fill="%23f4eee4"/><text x="50%" y="50%" text-anchor="middle" fill="%23816a4d" font-size="24">ECRU</text></svg>';

const emptyLook = () => ({
  id: null,
  mood: '',
  title: '',
  note: '',
  tags: [],
  items: [],
  createdAt: ''
});

const look = ref(emptyLook());

const boardItems = computed(() => {
  const items = Array.isArray(look.value.items) ? look.value.items.filter(Boolean).slice(0, 4) : [];
  return items;
});

const shortCategory = (category) => String(category || '单品').trim() || '单品';

const applyFeedback = (feedback) => {
  feedbackForm.overallRating = Number(feedback?.overallRating || 0);
  feedbackForm.styleRating = Number(feedback?.styleRating || 0);
  feedbackForm.practicalityRating = Number(feedback?.practicalityRating || 0);
  feedbackForm.weatherRating = Number(feedback?.weatherRating || 0);
  feedbackForm.isWorn = Boolean(feedback?.isWorn);
  feedbackForm.wornAt = feedback?.wornAt || '';
  feedbackForm.feedbackText = feedback?.feedbackText || '';
};

const loadDetail = async () => {
  isLoading.value = true;
  try {
    const response = await outfitApi.getHomeRecommendationDetail(route.params.id);
    look.value = response.data?.id ? response.data : emptyLook();

    if (look.value.id) {
      try {
        const detailResponse = await outfitApi.getAdviceDetail(look.value.id);
        const feedback = detailResponse.data?.feedback || null;
        savedFeedback.value = feedback;
        applyFeedback(feedback);
      } catch {
        // feedback load failure is non-critical
      }
    }
  } catch (error) {
    console.error('Load home recommendation detail failed:', error);
    if (error.response?.status === 404 || error.response?.data?.code === 404) {
      look.value = emptyLook();
    } else {
      alert(error.response?.data?.message || '加载搭配详情失败');
    }
  } finally {
    isLoading.value = false;
  }
};

const refreshRecommendations = async () => {
  isRefreshing.value = true;
  try {
    await outfitApi.getHomeRecommendations({ refresh: true });
    await loadDetail();
  } catch (error) {
    console.error('Refresh home recommendations failed:', error);
    alert(error.response?.data?.message || '刷新推荐流失败');
  } finally {
    isRefreshing.value = false;
  }
};

const updateFrequency = async (item, level) => {
  if (!item.clothingId || (item.frequencyLevel || 3) === level) {
    return;
  }

  const next = new Set(updatingItemIds.value);
  next.add(item.clothingId);
  updatingItemIds.value = next;

  try {
    await wardrobeApi.setFrequency(item.clothingId, level);
    look.value = {
      ...look.value,
      items: look.value.items.map((entry) =>
        entry.clothingId === item.clothingId ? { ...entry, frequencyLevel: level } : entry
      )
    };
  } catch (error) {
    console.error('Update clothing frequency failed:', error);
    alert(error.response?.data?.message || '更新搭配频率失败');
  } finally {
    const done = new Set(updatingItemIds.value);
    done.delete(item.clothingId);
    updatingItemIds.value = done;
  }
};

const goBack = () => {
  if (window.history.length > 1) {
    router.back();
    return;
  }
  router.push('/');
};

const submitFeedback = async () => {
  isSubmittingFeedback.value = true;
  try {
    const response = await outfitApi.submitFeedback(look.value.id, {
      overallRating: feedbackForm.overallRating || null,
      styleRating: feedbackForm.styleRating || null,
      practicalityRating: feedbackForm.practicalityRating || null,
      weatherRating: feedbackForm.weatherRating || null,
      isWorn: feedbackForm.isWorn,
      wornAt: feedbackForm.isWorn ? feedbackForm.wornAt || null : null,
      feedbackText: feedbackForm.feedbackText || ''
    });
    savedFeedback.value = response.data;
    alert('反馈已保存');
  } catch (error) {
    console.error('Submit feedback failed:', error);
    alert(error.response?.data?.message || '提交反馈失败');
  } finally {
    isSubmittingFeedback.value = false;
  }
};

const formatTime = (value) => {
  if (!value) return '未记录';
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

onMounted(loadDetail);
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  padding: 20px 16px 40px;
  background:
    radial-gradient(circle at top, color-mix(in srgb, var(--surface-strong) 92%, transparent), transparent 30%),
    linear-gradient(180deg, var(--bg-base) 0%, var(--bg-soft) 100%);
}

.detail-header,
.hero-copy,
.section-header,
.item-head,
.frequency-editor,
.stats-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.detail-header {
  gap: 14px;
  margin-bottom: 18px;
}

.detail-header h1,
.panel h2,
.item-card h3 {
  margin: 0;
  color: var(--text-main);
}

.eyebrow {
  margin: 0 0 6px;
  color: var(--text-soft);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.panel,
.state-card {
  border-radius: 26px;
  background: var(--surface);
  border: 1px solid var(--line-soft);
  box-shadow: var(--shadow-card);
}

.state-card {
  padding: 32px 20px;
  text-align: center;
  color: var(--text-soft);
}

.content-grid {
  display: grid;
  gap: 16px;
}

.panel {
  padding: 20px;
}

.hero-copy {
  gap: 12px;
  flex-wrap: wrap;
}

.hero-copy p,
.item-copy p,
.state-card,
.section-header span {
  color: var(--text-soft);
  line-height: 1.7;
}

.hero-board {
  position: relative;
  height: min(62vw, 430px);
  margin-top: 18px;
  border-radius: 24px;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--surface-strong) 98%, transparent), color-mix(in srgb, var(--bg-soft) 98%, transparent)),
    var(--surface-strong);
  box-shadow:
    inset 0 0 0 1px var(--line-soft),
    var(--shadow-card);
}

.board-piece {
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 18px;
  background: var(--bg-soft);
}

.board-piece img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.piece-placeholder {
  color: var(--text-soft);
  font-size: 13px;
}

.piece-1 {
  top: 18px;
  left: 18px;
  width: calc(50% - 24px);
  height: 46%;
}

.piece-2 {
  top: 18px;
  right: 18px;
  width: calc(50% - 24px);
  height: 56%;
}

.piece-3 {
  bottom: 18px;
  left: 18px;
  width: calc(42% - 14px);
  height: 28%;
}

.piece-4 {
  bottom: 18px;
  right: 18px;
  width: calc(58% - 16px);
  height: 22%;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.tag-row span,
.source-badge,
.frequency-chip {
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
}

.tag-row span {
  background: var(--accent-soft);
  color: var(--accent);
}

.item-grid {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

.item-card {
  display: grid;
  grid-template-columns: 110px 1fr;
  gap: 14px;
  padding: 14px;
  border-radius: 20px;
  background: var(--surface-strong);
}

.item-image {
  overflow: hidden;
  border-radius: 16px;
  background: var(--bg-soft);
}

.item-image img {
  width: 100%;
  height: 100%;
  min-height: 132px;
  object-fit: cover;
}

.item-copy {
  display: grid;
  gap: 12px;
}

.item-head {
  align-items: flex-start;
  gap: 10px;
}

.item-head p {
  margin: 6px 0 0;
}

.source-badge.owned {
  background: var(--accent-soft);
  color: var(--accent);
}

.source-badge.external {
  background: var(--line-soft);
  color: var(--text-soft);
}

.item-reason {
  margin: 0;
}

.stats-row {
  justify-content: flex-start;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 12px;
}

.frequency-editor {
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;
}

.frequency-editor span {
  color: var(--text-soft);
  font-size: 13px;
}

.frequency-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.frequency-chip {
  border: none;
  background: var(--bg-soft);
  color: var(--text-soft);
  cursor: pointer;
}

.frequency-chip.active {
  background: var(--accent-strong);
  color: var(--surface-strong);
}

.ghost-button,
.primary-button {
  border: none;
  border-radius: 999px;
  padding: 10px 16px;
  cursor: pointer;
}

.ghost-button {
  background: var(--bg-soft);
  color: var(--text-main);
  font-size: 22px;
  line-height: 1;
  padding: 8px 14px;
}

.primary-button {
  background: var(--accent-strong);
  color: var(--surface-strong);
}

@media (max-width: 767px) {
  .item-card {
    grid-template-columns: 1fr;
  }

  .hero-board {
    height: 320px;
  }
}

@media (min-width: 960px) {
  .detail-page {
    padding: 28px 28px 48px;
  }

  .content-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .wide,
  .hero-panel {
    grid-column: span 2;
  }

  .item-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.feedback-form {
  display: grid;
  gap: 16px;
  margin-top: 18px;
}

.rating-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.rating-grid label,
.feedback-form > label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: var(--text-soft);
}

.rating-grid select,
.feedback-form input[type='date'],
.feedback-form textarea {
  padding: 8px 10px;
  border: 1px solid var(--line-soft);
  border-radius: 10px;
  background: var(--bg-soft);
  color: var(--text-main);
  font-size: 14px;
}

.feedback-form textarea {
  resize: vertical;
}

.feedback-switch {
  flex-direction: row !important;
  align-items: center;
  gap: 10px !important;
}

.feedback-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
}

.muted-text {
  margin: 0;
  font-size: 12px;
  color: var(--text-soft);
}
</style>
