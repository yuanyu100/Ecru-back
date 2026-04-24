<template>
  <div class="admin-page outfit-record-page">
    <div class="stats-grid">
      <article class="stat-card">
        <span class="stat-label">穿搭记录</span>
        <strong>{{ overview.recordTotal }}</strong>
      </article>
      <article class="stat-card">
        <span class="stat-label">收藏记录</span>
        <strong>{{ overview.favoriteTotal }}</strong>
      </article>
      <article class="stat-card">
        <span class="stat-label">用户反馈</span>
        <strong>{{ overview.feedbackTotal }}</strong>
      </article>
      <article class="stat-card accent-card">
        <span class="stat-label">已穿着反馈</span>
        <strong>{{ overview.wornTotal }}</strong>
      </article>
    </div>

    <div class="panel-grid outfit-record-grid">
      <section class="panel-card">
        <div class="panel-head stacked-head">
          <div>
            <h2>全局穿搭记录</h2>
            <p class="panel-subtitle">管理员可以检索所有用户的 AI 穿搭建议、推荐单品和反馈记录。</p>
          </div>

          <div class="toolbar">
            <input
              v-model.trim="filters.keyword"
              class="text-input"
              type="text"
              placeholder="搜索穿搭标题、描述、建议"
            />
            <input
              v-model.trim="filters.ownerKeyword"
              class="text-input"
              type="text"
              placeholder="搜索用户名 / 昵称 / 邮箱"
            />
            <input
              v-model.trim="filters.occasion"
              class="text-input"
              type="text"
              placeholder="搜索场景，如通勤 / 约会"
            />
            <select v-model="filters.favorite" class="text-input select-input">
              <option value="">全部收藏状态</option>
              <option value="1">仅收藏</option>
              <option value="0">仅未收藏</option>
            </select>
            <button class="secondary-button" type="button" @click="loadRecords">查询</button>
          </div>
        </div>

        <div v-if="loading" class="empty-tip">加载中...</div>

        <div v-else-if="records.length" class="table-shell">
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>用户</th>
                <th>穿搭方案</th>
                <th>场景</th>
                <th>单品数</th>
                <th>评分</th>
                <th>收藏</th>
                <th>更新时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="item in records"
                :key="item.id"
                :class="{ 'active-row': selectedRecordId === item.id }"
                @click="openDetail(item)"
              >
                <td>{{ item.id }}</td>
                <td>
                  <strong>{{ item.username || '-' }}</strong>
                  <div class="sub-copy">{{ item.nickname || item.email || '-' }}</div>
                </td>
                <td>
                  <strong>{{ item.outfitName || '未命名方案' }}</strong>
                  <div class="sub-copy clamp-text">{{ item.outfitDescription || item.inputDescription || '-' }}</div>
                </td>
                <td>{{ item.occasion || '-' }}</td>
                <td>{{ item.itemCount || 0 }}</td>
                <td>{{ item.overallRating ?? '-' }}</td>
                <td>
                  <span class="badge" :class="item.isFavorite ? 'badge-green' : 'badge-gray'">
                    {{ item.isFavorite ? '已收藏' : '未收藏' }}
                  </span>
                </td>
                <td>{{ formatDateTime(item.updatedAt) }}</td>
                <td class="action-cell">
                  <button class="secondary-button" type="button" @click.stop="openDetail(item)">查看详情</button>
                  <button
                    class="danger-button"
                    type="button"
                    :disabled="deletingId === item.id"
                    @click.stop="removeRecord(item)"
                  >
                    {{ deletingId === item.id ? '删除中...' : '删除' }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <p v-else class="empty-tip">当前没有匹配的穿搭记录。</p>
      </section>

      <section class="panel-card">
        <div class="panel-head">
          <div>
            <h2>记录详情</h2>
            <p class="panel-subtitle">展示原始输入、AI 推荐单品和用户反馈，便于联调排查与内容巡检。</p>
          </div>
          <button class="secondary-button" type="button" @click="clearSelection">清空选择</button>
        </div>

        <div v-if="detailLoading" class="empty-tip">详情加载中...</div>
        <p v-else-if="!detail.record" class="empty-tip">点击左侧记录后，这里展示完整详情。</p>

        <template v-else>
          <div class="detail-meta">
            <article class="mini-card">
              <span>用户</span>
              <strong>{{ detail.record.username || '-' }}</strong>
            </article>
            <article class="mini-card">
              <span>输入方式</span>
              <strong>{{ inputTypeLabel(detail.record.inputType) }}</strong>
            </article>
            <article class="mini-card">
              <span>适配分</span>
              <strong>{{ detail.record.suitabilityScore ?? '-' }}</strong>
            </article>
            <article class="mini-card">
              <span>收藏状态</span>
              <strong>{{ detail.record.isFavorite ? '已收藏' : '未收藏' }}</strong>
            </article>
          </div>

          <div class="preview-card">
            <div class="preview-head">
              <strong>{{ detail.record.outfitName || '未命名方案' }}</strong>
              <span>{{ formatDateTime(detail.record.updatedAt) }}</span>
            </div>
            <p class="preview-text">输入描述：{{ detail.record.inputDescription || '暂无' }}</p>
            <p class="preview-text">场景：{{ detail.record.occasion || '暂无' }}</p>
            <p class="preview-text">天气：{{ renderWeather(detail.record) }}</p>
            <p class="preview-text">识别风格：{{ detail.record.detectedStyle || '暂无' }}</p>
            <p class="preview-text">识别单品：{{ detail.record.detectedItems || '暂无' }}</p>
            <p class="preview-text">颜色分析：{{ detail.record.colorAnalysis || '暂无' }}</p>
            <p class="preview-text">推荐理由：{{ detail.record.reasoning || '暂无' }}</p>
            <p class="preview-text">搭配建议：{{ detail.record.fashionSuggestions || '暂无' }}</p>
            <p class="preview-text">购买建议：{{ detail.record.purchaseRecommendations || '暂无' }}</p>
            <p v-if="detail.record.inputImageUrl" class="preview-text">
              输入图片：<a :href="detail.record.inputImageUrl" target="_blank" rel="noreferrer">查看原图</a>
            </p>
          </div>

          <div class="preview-card">
            <div class="preview-head">
              <strong>推荐单品</strong>
              <span>{{ detail.items.length }} 项</span>
            </div>
            <div v-if="detail.items.length" class="list-stack">
              <article v-for="item in detail.items" :key="item.id" class="list-card">
                <div class="list-card-head">
                  <strong>{{ item.itemName || '未命名单品' }}</strong>
                  <span>{{ item.itemCategory || '-' }}</span>
                </div>
                <p class="preview-text">颜色：{{ item.itemColor || '暂无' }}</p>
                <p class="preview-text">推荐原因：{{ item.reason || '暂无' }}</p>
                <p class="preview-text">来源衣物 ID：{{ item.clothingId ?? '-' }}</p>
              </article>
            </div>
            <p v-else class="empty-tip">没有推荐单品数据。</p>
          </div>

          <div class="preview-card">
            <div class="preview-head">
              <strong>用户反馈</strong>
              <span>{{ detail.feedbacks.length }} 条</span>
            </div>
            <div v-if="detail.feedbacks.length" class="list-stack">
              <article v-for="feedback in detail.feedbacks" :key="feedback.id" class="list-card">
                <div class="list-card-head">
                  <strong>{{ feedback.username || '匿名用户' }}</strong>
                  <span>{{ feedback.isWorn ? '已穿着' : '未穿着' }}</span>
                </div>
                <p class="preview-text">
                  综合 / 风格 / 实用 / 天气：
                  {{ renderScore(feedback.overallRating) }} /
                  {{ renderScore(feedback.styleRating) }} /
                  {{ renderScore(feedback.practicalityRating) }} /
                  {{ renderScore(feedback.weatherRating) }}
                </p>
                <p class="preview-text">反馈内容：{{ feedback.feedbackText || '暂无' }}</p>
                <p class="preview-text">反馈时间：{{ formatDateTime(feedback.updatedAt) }}</p>
              </article>
            </div>
            <p v-else class="empty-tip">当前没有用户反馈。</p>
          </div>
        </template>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { outfitAdminApi } from '../../api/outfit';

const overview = reactive({
  recordTotal: 0,
  favoriteTotal: 0,
  feedbackTotal: 0,
  wornTotal: 0
});

const filters = reactive({
  keyword: '',
  ownerKeyword: '',
  occasion: '',
  favorite: ''
});

const loading = ref(false);
const detailLoading = ref(false);
const deletingId = ref(null);
const selectedRecordId = ref(null);
const records = ref([]);
const detail = reactive({
  record: null,
  items: [],
  feedbacks: []
});

const formatDateTime = (value) => (value ? String(value).replace('T', ' ') : '-');
const renderScore = (value) => (value ?? value === 0 ? value : '-');

const inputTypeLabel = (value) => {
  if (value === 2) {
    return '图片识别';
  }
  if (value === 1) {
    return '文本描述';
  }
  return '未知';
};

const renderWeather = (record) => {
  const fragments = [record.location, record.temperature, record.weatherCondition, record.season, record.timeOfDay].filter(Boolean);
  return fragments.length ? fragments.join(' / ') : '暂无';
};

const resetDetail = () => {
  detail.record = null;
  detail.items = [];
  detail.feedbacks = [];
};

const loadOverview = async () => {
  const result = await outfitAdminApi.getOverview();
  if (result?.code === 200 && result.data) {
    Object.assign(overview, result.data);
  }
};

const loadRecords = async () => {
  loading.value = true;
  try {
    const result = await outfitAdminApi.getRecords({
      page: 1,
      size: 50,
      keyword: filters.keyword,
      ownerKeyword: filters.ownerKeyword,
      occasion: filters.occasion,
      favorite: filters.favorite === '' ? undefined : Number(filters.favorite)
    });
    records.value = result?.data?.list || [];
  } catch (error) {
    console.error('Load outfit records failed:', error);
    alert(error.response?.data?.message || '加载穿搭记录失败');
  } finally {
    loading.value = false;
  }
};

const previewRecord = (item) => {
  selectedRecordId.value = item.id;
};

const openDetail = async (item) => {
  previewRecord(item);
  detailLoading.value = true;
  try {
    const result = await outfitAdminApi.getRecordDetail(item.id);
    if (result?.code === 200 && result.data) {
      detail.record = result.data.record || null;
      detail.items = result.data.items || [];
      detail.feedbacks = result.data.feedbacks || [];
    }
  } catch (error) {
    console.error('Load outfit record detail failed:', error);
    resetDetail();
    alert(error.response?.data?.message || '加载穿搭记录详情失败');
  } finally {
    detailLoading.value = false;
  }
};

const clearSelection = () => {
  selectedRecordId.value = null;
  resetDetail();
};

const removeRecord = async (item) => {
  if (!window.confirm(`确认删除穿搭记录「${item.outfitName || `#${item.id}`}」吗？相关推荐单品和反馈会一并删除。`)) {
    return;
  }

  deletingId.value = item.id;
  try {
    const result = await outfitAdminApi.deleteRecord(item.id);
    if (result?.code === 200) {
      await Promise.all([loadOverview(), loadRecords()]);
      if (selectedRecordId.value === item.id) {
        clearSelection();
      }
      alert(result.message || '穿搭记录删除成功');
    }
  } catch (error) {
    console.error('Delete outfit record failed:', error);
    alert(error.response?.data?.message || '删除穿搭记录失败');
  } finally {
    deletingId.value = null;
  }
};

onMounted(async () => {
  await Promise.all([loadOverview(), loadRecords()]);
});
</script>

<style scoped>
.outfit-record-page {
  gap: 20px;
}

.outfit-record-grid {
  grid-template-columns: minmax(0, 1.2fr) minmax(360px, 0.8fr);
}

.stacked-head {
  align-items: flex-start;
  flex-direction: column;
}

.accent-card {
  background: linear-gradient(135deg, rgba(14, 116, 144, 0.96), rgba(6, 182, 212, 0.92));
}

.accent-card .stat-label,
.accent-card strong {
  color: #fff;
}

.sub-copy {
  margin-top: 4px;
  color: #5f6b7a;
  font-size: 12px;
}

.clamp-text {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.action-cell {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.active-row {
  background: rgba(14, 116, 144, 0.06);
}

.detail-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.mini-card,
.preview-card,
.list-card {
  padding: 14px;
  border-radius: 16px;
  background: #f8fbff;
  border: 1px solid #dbe6f7;
}

.mini-card span {
  color: #5f6b7a;
  font-size: 12px;
}

.mini-card strong {
  display: block;
  margin-top: 8px;
}

.preview-card {
  margin-top: 16px;
}

.preview-head,
.list-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.preview-head span,
.list-card-head span {
  color: #5f6b7a;
  font-size: 12px;
}

.preview-text {
  margin: 10px 0 0;
  white-space: pre-wrap;
}

.list-stack {
  display: grid;
  gap: 12px;
}

.badge-gray {
  background: #edf2f7;
  color: #4b5563;
}

@media (max-width: 1100px) {
  .outfit-record-grid {
    grid-template-columns: 1fr;
  }

  .detail-meta {
    grid-template-columns: 1fr;
  }
}
</style>
