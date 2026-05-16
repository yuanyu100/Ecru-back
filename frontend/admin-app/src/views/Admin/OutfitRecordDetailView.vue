<template>
  <div class="admin-page">
    <div class="detail-page-head">
      <button class="secondary-button" type="button" @click="$router.push('/outfit-records')">← 返回列表</button>
      <h2>搭配记录详情</h2>
    </div>

    <div v-if="loading" class="panel-card">
      <p class="empty-tip">加载中...</p>
    </div>

    <div v-else-if="!detail.record" class="panel-card">
      <p class="empty-tip">记录不存在或已被删除。</p>
    </div>

    <template v-else>
      <div class="stats-grid">
        <article class="stat-card">
          <span class="stat-label">用户</span>
          <strong>{{ detail.record.username || '-' }}</strong>
        </article>
        <article class="stat-card">
          <span class="stat-label">输入方式</span>
          <strong>{{ inputTypeLabel(detail.record.inputType) }}</strong>
        </article>
        <article class="stat-card">
          <span class="stat-label">适配分</span>
          <strong>{{ detail.record.suitabilityScore ?? '-' }}</strong>
        </article>
        <article class="stat-card">
          <span class="stat-label">收藏状态</span>
          <strong>
            <span class="badge" :class="detail.record.isFavorite ? 'badge-green' : 'badge-gray'">
              {{ detail.record.isFavorite ? '已收藏' : '未收藏' }}
            </span>
          </strong>
        </article>
      </div>

      <section class="panel-card">
        <div class="panel-head">
          <div>
            <h2>{{ detail.record.outfitName || '未命名方案' }}</h2>
            <p class="sub-copy">更新时间：{{ formatDateTime(detail.record.updatedAt) }}</p>
          </div>
        </div>

        <div class="info-grid">
          <div class="info-item"><span>输入描述</span><p>{{ detail.record.inputDescription || '暂无' }}</p></div>
          <div class="info-item"><span>场景</span><p>{{ detail.record.occasion || '暂无' }}</p></div>
          <div class="info-item"><span>天气</span><p>{{ renderWeather(detail.record) }}</p></div>
          <div class="info-item"><span>识别风格</span><p>{{ detail.record.detectedStyle || '暂无' }}</p></div>
          <div class="info-item"><span>识别单品</span><p>{{ detail.record.detectedItems || '暂无' }}</p></div>
          <div class="info-item"><span>颜色分析</span><p>{{ detail.record.colorAnalysis || '暂无' }}</p></div>
          <div class="info-item full-width"><span>推荐理由</span><p>{{ detail.record.reasoning || '暂无' }}</p></div>
          <div class="info-item full-width"><span>搭配建议</span><p>{{ detail.record.fashionSuggestions || '暂无' }}</p></div>
          <div class="info-item full-width"><span>购买建议</span><p>{{ detail.record.purchaseRecommendations || '暂无' }}</p></div>
        </div>

        <p v-if="detail.record.inputImageUrl" class="image-link">
          输入图片：<a :href="detail.record.inputImageUrl" target="_blank" rel="noreferrer">查看原图</a>
        </p>
      </section>

      <section class="panel-card">
        <div class="panel-head">
          <h2>推荐单品</h2>
          <span class="count-badge">{{ detail.items.length }} 项</span>
        </div>
        <div v-if="detail.items.length" class="items-grid">
          <article v-for="item in detail.items" :key="item.id" class="item-card">
            <div class="item-head">
              <strong>{{ item.itemName || '未命名单品' }}</strong>
              <span class="badge badge-blue">{{ item.itemCategory || '-' }}</span>
            </div>
            <p class="item-meta">颜色：{{ item.itemColor || '暂无' }}</p>
            <p class="item-meta">推荐原因：{{ item.reason || '暂无' }}</p>
            <p class="item-meta">来源衣物 ID：{{ item.clothingId ?? '-' }}</p>
          </article>
        </div>
        <p v-else class="empty-tip">没有推荐单品数据。</p>
      </section>

      <section class="panel-card">
        <div class="panel-head">
          <h2>用户反馈</h2>
          <span class="count-badge">{{ detail.feedbacks.length }} 条</span>
        </div>
        <div v-if="detail.feedbacks.length" class="items-grid">
          <article v-for="feedback in detail.feedbacks" :key="feedback.id" class="item-card">
            <div class="item-head">
              <strong>{{ feedback.username || '匿名用户' }}</strong>
              <span class="badge" :class="feedback.isWorn ? 'badge-green' : 'badge-gray'">
                {{ feedback.isWorn ? '已穿着' : '未穿着' }}
              </span>
            </div>
            <p class="item-meta">
              综合 / 风格 / 实用 / 天气：
              {{ renderScore(feedback.overallRating) }} /
              {{ renderScore(feedback.styleRating) }} /
              {{ renderScore(feedback.practicalityRating) }} /
              {{ renderScore(feedback.weatherRating) }}
            </p>
            <p class="item-meta">反馈内容：{{ feedback.feedbackText || '暂无' }}</p>
            <p class="item-meta">反馈时间：{{ formatDateTime(feedback.updatedAt) }}</p>
          </article>
        </div>
        <p v-else class="empty-tip">当前没有用户反馈。</p>
      </section>
    </template>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { outfitAdminApi } from '../../api/outfit';

const route = useRoute();

const loading = ref(false);
const detail = reactive({ record: null, items: [], feedbacks: [] });

const formatDateTime = (value) => (value ? String(value).replace('T', ' ') : '-');
const renderScore = (value) => (value != null ? value : '-');

const inputTypeLabel = (value) => {
  if (value === 2) return '图片识别';
  if (value === 1) return '文本描述';
  return '未知';
};

const renderWeather = (record) => {
  const parts = [record.location, record.temperature, record.weatherCondition, record.season, record.timeOfDay].filter(Boolean);
  return parts.length ? parts.join(' / ') : '暂无';
};

onMounted(async () => {
  const id = route.params.id;
  if (!id) return;
  loading.value = true;
  try {
    const result = await outfitAdminApi.getRecordDetail(id);
    if (result?.code === 200 && result.data) {
      detail.record = result.data.record || null;
      detail.items = result.data.items || [];
      detail.feedbacks = result.data.feedbacks || [];
    }
  } catch (error) {
    console.error('Load outfit record detail failed:', error);
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.sub-copy {
  margin-top: 4px;
  color: #5f6b7a;
  font-size: 12px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
  margin-top: 4px;
}

.info-item {
  padding: 12px 14px;
  border-radius: 14px;
  background: #f8fbff;
  border: 1px solid #dbe6f7;
}

.info-item.full-width {
  grid-column: 1 / -1;
}

.info-item span {
  display: block;
  color: #5f6b7a;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin-bottom: 6px;
}

.info-item p {
  margin: 0;
  white-space: pre-wrap;
}

.image-link {
  margin-top: 14px;
  font-size: 13px;
}

.items-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
  margin-top: 4px;
}

.item-card {
  padding: 14px;
  border-radius: 16px;
  background: #f8fbff;
  border: 1px solid #dbe6f7;
}

.item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.item-meta {
  margin: 6px 0 0;
  font-size: 13px;
  color: #5f6b7a;
}

.badge-blue {
  background: rgba(31, 94, 255, 0.1);
  color: #1f5eff;
  border-color: rgba(31, 94, 255, 0.14);
}

.badge-gray {
  background: #edf2f7;
  color: #4b5563;
}
</style>
