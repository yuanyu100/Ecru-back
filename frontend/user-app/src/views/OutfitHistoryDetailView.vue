<template>
  <div class="detail-page">
    <header class="detail-header">
      <button class="ghost-button" type="button" @click="goBack">‹</button>
      <div>
        <p class="eyebrow">Outfit Record</p>
        <h1>穿搭记录详情</h1>
      </div>
    </header>

    <div v-if="isLoading" class="state-card">正在加载穿搭详情...</div>
    <div v-else-if="!detail.record.id" class="state-card">这条穿搭记录不存在，或你没有权限查看。</div>

    <div v-else class="content-grid">
      <section class="panel hero-panel">
        <div class="hero-copy">
          <div>
            <p class="eyebrow">Summary</p>
            <h2>{{ detail.record.outfitName }}</h2>
            <p>{{ detail.record.outfitDescription || '暂无描述' }}</p>
          </div>

          <div class="hero-actions">
            <button class="primary-button" type="button" :disabled="isUpdatingFavorite" @click="toggleFavorite">
              {{ detail.record.isFavorite ? '取消收藏' : '收藏方案' }}
            </button>
            <button class="ghost-button danger" type="button" :disabled="isDeleting" @click="deleteRecord">
              删除记录
            </button>
          </div>
        </div>

        <div class="meta-grid">
          <article class="meta-card">
            <span>场景</span>
            <strong>{{ detail.record.occasion || '未标注' }}</strong>
          </article>
          <article class="meta-card">
            <span>天气</span>
            <strong>{{ detail.record.weatherCondition || '未标注' }}</strong>
          </article>
          <article class="meta-card">
            <span>季节</span>
            <strong>{{ detail.record.season || '未标注' }}</strong>
          </article>
          <article class="meta-card">
            <span>创建时间</span>
            <strong>{{ formatTime(detail.record.createdAt) }}</strong>
          </article>
        </div>
      </section>

      <section class="panel">
        <p class="eyebrow">Reasoning</p>
        <h2>搭配思路</h2>
        <p class="body-copy">{{ detail.record.reasoning || '暂无搭配思路说明。' }}</p>

        <div class="divider"></div>

        <p class="eyebrow">Suggestion</p>
        <h2>时尚建议</h2>
        <p class="body-copy">{{ detail.record.fashionSuggestions || '暂无额外建议。' }}</p>
      </section>

      <section class="panel wide">
        <div class="section-header">
          <div>
            <p class="eyebrow">Items</p>
            <h2>推荐单品</h2>
          </div>
          <span>{{ detail.items.length }} 件</span>
        </div>

        <div v-if="detail.items.length === 0" class="empty-block">这条记录没有关联单品。</div>

        <div v-else class="item-grid">
          <article v-for="item in detail.items" :key="item.id" class="item-card">
            <div class="item-image">
              <img :src="item.itemImageUrl || fallbackImage" :alt="item.itemName" />
            </div>
            <div>
              <h3>{{ item.itemName }}</h3>
              <p>{{ item.itemCategory || '未分类' }}{{ item.itemColor ? ` / ${item.itemColor}` : '' }}</p>
              <p class="item-reason">{{ item.reason || '暂无推荐理由' }}</p>
              <span :class="['badge', item.isRecommended ? 'accent' : 'plain']">
                {{ item.isRecommended ? '系统推荐' : '已有单品' }}
              </span>
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
            <p v-if="detail.feedback" class="muted-text">上次更新：{{ formatTime(detail.feedback.updatedAt) }}</p>
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
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { outfitApi } from '../api/outfit';

const route = useRoute();
const router = useRouter();
const isLoading = ref(false);
const isUpdatingFavorite = ref(false);
const isSubmittingFeedback = ref(false);
const isDeleting = ref(false);

const fallbackImage =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="320" height="420"><rect width="100%" height="100%" fill="%23f0e4cf"/><text x="50%" y="50%" text-anchor="middle" fill="%238b7355" font-size="24">LOOK</text></svg>';

const detail = ref({
  record: {},
  items: [],
  feedback: null
});

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

const applyFeedback = (feedback) => {
  feedbackForm.overallRating = Number(feedback?.overallRating || 0);
  feedbackForm.styleRating = Number(feedback?.styleRating || 0);
  feedbackForm.practicalityRating = Number(feedback?.practicalityRating || 0);
  feedbackForm.weatherRating = Number(feedback?.weatherRating || 0);
  feedbackForm.isWorn = Boolean(feedback?.isWorn);
  feedbackForm.wornAt = feedback?.wornAt || '';
  feedbackForm.feedbackText = feedback?.feedbackText || '';
};

const resetDetail = () => {
  detail.value = {
    record: {},
    items: [],
    feedback: null
  };
  applyFeedback(null);
};

const loadDetail = async () => {
  isLoading.value = true;
  try {
    const response = await outfitApi.getAdviceDetail(route.params.id);
    detail.value = response.data || { record: {}, items: [], feedback: null };
    applyFeedback(detail.value.feedback);
  } catch (error) {
    console.error('Load outfit detail failed:', error);
    if (error.response?.status === 404 || error.response?.data?.code === 404) {
      resetDetail();
      return;
    }
    alert(error.response?.data?.message || '加载穿搭详情失败');
  } finally {
    isLoading.value = false;
  }
};

const toggleFavorite = async () => {
  isUpdatingFavorite.value = true;
  try {
    await outfitApi.toggleFavorite(detail.value.record.id, !detail.value.record.isFavorite);
    detail.value = {
      ...detail.value,
      record: {
        ...detail.value.record,
        isFavorite: !detail.value.record.isFavorite
      }
    };
  } catch (error) {
    console.error('Toggle favorite failed:', error);
    alert(error.response?.data?.message || '更新收藏状态失败');
  } finally {
    isUpdatingFavorite.value = false;
  }
};

const submitFeedback = async () => {
  isSubmittingFeedback.value = true;
  try {
    const response = await outfitApi.submitFeedback(detail.value.record.id, {
      overallRating: feedbackForm.overallRating || null,
      styleRating: feedbackForm.styleRating || null,
      practicalityRating: feedbackForm.practicalityRating || null,
      weatherRating: feedbackForm.weatherRating || null,
      isWorn: feedbackForm.isWorn,
      wornAt: feedbackForm.isWorn ? feedbackForm.wornAt || null : null,
      feedbackText: feedbackForm.feedbackText || ''
    });

    detail.value = {
      ...detail.value,
      feedback: response.data
    };
    alert('反馈已保存');
  } catch (error) {
    console.error('Submit feedback failed:', error);
    alert(error.response?.data?.message || '提交反馈失败');
  } finally {
    isSubmittingFeedback.value = false;
  }
};

const deleteRecord = async () => {
  if (!window.confirm('确认删除这条穿搭记录吗？')) {
    return;
  }

  isDeleting.value = true;
  try {
    await outfitApi.deleteAdvice(detail.value.record.id);
    router.push('/');
  } catch (error) {
    console.error('Delete outfit advice failed:', error);
    alert(error.response?.data?.message || '删除记录失败');
  } finally {
    isDeleting.value = false;
  }
};

const goBack = () => {
  if (window.history.length > 1) {
    router.back();
    return;
  }
  router.push('/');
};

const formatTime = (value) => {
  if (!value) {
    return '未记录';
  }

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
    radial-gradient(circle at top, rgba(255, 243, 214, 0.88), transparent 34%),
    linear-gradient(180deg, #f8f1df 0%, #efe2ca 100%);
}

.detail-header,
.hero-copy,
.hero-actions,
.section-header,
.feedback-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.detail-header {
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

.detail-header h1,
.panel h2,
.item-card h3 {
  color: #5d4523;
}

.panel,
.state-card {
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

.hero-copy {
  gap: 16px;
  flex-wrap: wrap;
}

.hero-actions {
  gap: 12px;
  flex-wrap: wrap;
}

.hero-copy p,
.body-copy,
.item-card p,
.muted-text,
.empty-block {
  color: #7a6140;
  line-height: 1.7;
}

.meta-grid,
.item-grid,
.rating-grid {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.meta-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.meta-card,
.item-card {
  padding: 14px;
  border-radius: 18px;
  background: #fffdf8;
}

.meta-card span {
  display: block;
  color: #8b6f48;
  font-size: 12px;
}

.meta-card strong {
  display: block;
  margin-top: 8px;
  color: #5d4523;
}

.divider {
  height: 1px;
  margin: 18px 0;
  background: rgba(145, 104, 49, 0.14);
}

.item-grid {
  grid-template-columns: repeat(1, minmax(0, 1fr));
}

.item-card {
  display: grid;
  grid-template-columns: 100px 1fr;
  gap: 14px;
}

.item-image {
  border-radius: 16px;
  overflow: hidden;
  background: #f1e0c4;
}

.item-image img {
  width: 100%;
  height: 100%;
  min-height: 120px;
  object-fit: cover;
}

.item-reason {
  margin-top: 8px;
}

.badge {
  display: inline-flex;
  margin-top: 10px;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
}

.badge.accent {
  background: #6b4b1f;
  color: #fff8ef;
}

.badge.plain {
  background: #efe1c8;
  color: #7a6140;
}

.feedback-form {
  display: grid;
  gap: 14px;
}

.rating-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.feedback-form label {
  display: grid;
  gap: 8px;
  color: #6c522f;
  font-size: 14px;
}

.feedback-form select,
.feedback-form input,
.feedback-form textarea {
  border: 1px solid #d9c39b;
  border-radius: 14px;
  padding: 12px 14px;
  background: #fffdf8;
  color: #5d4523;
}

.feedback-switch {
  display: flex !important;
  align-items: center;
  gap: 10px;
}

.feedback-switch input {
  width: auto;
}

.ghost-button,
.primary-button {
  border: none;
  border-radius: 999px;
  padding: 10px 16px;
  cursor: pointer;
}

.ghost-button {
  background: #ead7b8;
  color: #5d4523;
  font-size: 22px;
  line-height: 1;
  padding: 8px 14px;
}

.ghost-button.danger {
  background: rgba(217, 93, 81, 0.12);
  color: #b04d45;
}

.primary-button {
  background: #6b4b1f;
  color: #fff8ef;
}

@media (max-width: 767px) {
  .meta-grid,
  .rating-grid {
    grid-template-columns: 1fr;
  }

  .item-card {
    grid-template-columns: 1fr;
  }
}

@media (min-width: 900px) {
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
</style>
