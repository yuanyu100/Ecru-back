<template>
  <div class="knowledge-page">
    <header class="page-header">
      <button class="icon-button" type="button" aria-label="返回" @click="goBack">
        <span></span>
      </button>
      <div>
        <p class="page-caption">面料百科</p>
        <h1>面料基础知识与文档检索</h1>
      </div>
    </header>

    <section class="panel-card">
      <div class="section-head">
        <p class="section-caption">常见面料</p>
        <h2>先快速建立基础认知</h2>
      </div>

      <div class="fabric-grid">
        <article v-for="item in fabricBasics" :key="item.name" class="fabric-card">
          <div class="fabric-top">
            <strong>{{ item.name }}</strong>
            <span>{{ item.alias }}</span>
          </div>
          <p>{{ item.summary }}</p>
          <div class="tag-row">
            <span v-for="tag in item.tags" :key="`${item.name}-${tag}`">{{ tag }}</span>
          </div>
        </article>
      </div>
    </section>

    <section class="panel-card">
      <div class="section-head">
        <p class="section-caption">水洗标速读</p>
        <h2>先看懂常见护理提醒</h2>
      </div>

      <div class="guide-list">
        <article v-for="item in careGuides" :key="item.title" class="guide-card">
          <strong>{{ item.title }}</strong>
          <p>{{ item.content }}</p>
        </article>
      </div>
    </section>

    <section class="panel-card">
      <div class="section-head">
        <p class="section-caption">RAG 文档</p>
        <h2>检索知识库里的面料与护理资料</h2>
      </div>

      <div class="filter-row">
        <button
          v-for="option in typeOptions"
          :key="option.value"
          :class="['filter-chip', searchForm.type === option.value ? 'active' : '']"
          type="button"
          @click="searchForm.type = option.value"
        >
          {{ option.label }}
        </button>
      </div>

      <form class="search-bar" @submit.prevent="submitSearch">
        <input v-model.trim="searchForm.query" type="text" placeholder="例如 羊毛、真丝护理、不可漂白" />
        <button class="solid-button" type="submit" :disabled="isSearching || !searchForm.query">
          {{ isSearching ? '检索中...' : '搜索' }}
        </button>
      </form>

      <div class="tag-row quick-row">
        <button v-for="keyword in searchKeywords" :key="keyword" class="quick-chip" type="button" @click="runQuickSearch(keyword)">
          {{ keyword }}
        </button>
      </div>

      <div v-if="searchResults.length" class="result-list">
        <article v-for="item in searchResults" :key="item.documentId" class="result-card" @click="openSearchItem(item)">
          <div class="result-top">
            <span class="type-badge">{{ typeLabelMap[item.type] || item.type }}</span>
            <strong>{{ item.relevance }}%</strong>
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.content }}</p>
        </article>
      </div>
      <div v-else class="empty-shell">
        <p>还没有检索结果。</p>
      </div>
    </section>

    <section class="panel-card">
      <div class="section-head">
        <p class="section-caption">详情</p>
        <h2>{{ detailTitle }}</h2>
      </div>

      <div v-if="!detailData" class="empty-shell thin">
        <p>点击上面的知识库结果后，详细内容会显示在这里。</p>
      </div>

      <template v-else-if="detailType === 'fabric'">
        <div class="summary-card">
          <span>面料</span>
          <strong>{{ detailData.name }}</strong>
        </div>
        <p class="detail-copy">{{ detailData.summary || detailData.properties || '暂无描述' }}</p>
        <div v-if="detailData.alias?.length" class="tag-row">
          <span v-for="tag in detailData.alias" :key="tag">{{ tag }}</span>
        </div>
        <div class="score-grid">
          <article v-for="item in fabricScores(detailData.characteristics)" :key="item.key" class="score-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        </div>
        <p class="detail-copy">{{ detailData.careGuide || '暂无护理建议' }}</p>
      </template>

      <template v-else-if="detailType === 'care-label'">
        <div class="summary-card">
          <span>水洗标</span>
          <strong>{{ detailData.symbolName }}</strong>
        </div>
        <p class="detail-copy">{{ detailData.instruction || '暂无说明' }}</p>
        <div class="score-grid">
          <article class="score-card">
            <span>建议</span>
            <strong>{{ detailData.doText || '暂无' }}</strong>
          </article>
          <article class="score-card">
            <span>避免</span>
            <strong>{{ detailData.dontText || '暂无' }}</strong>
          </article>
        </div>
        <p class="detail-copy">{{ detailData.explanation || '' }}</p>
      </template>

      <template v-else-if="detailType === 'guide'">
        <div class="summary-card">
          <span>指南</span>
          <strong>{{ detailData.title }}</strong>
        </div>
        <p v-if="detailData.subtitle" class="detail-copy">{{ detailData.subtitle }}</p>
        <p class="detail-copy">{{ detailData.content || '暂无内容' }}</p>
      </template>
    </section>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { knowledgeApi } from '../api/knowledge';

const router = useRouter();

const fabricBasics = [
  {
    name: '棉',
    alias: 'Cotton',
    summary: '亲肤、透气、日常最常见，但容易皱，也比较容易吸汗吸湿。',
    tags: ['舒适', '透气', '易皱']
  },
  {
    name: '亚麻',
    alias: 'Linen',
    summary: '清爽、轻薄、很适合夏天，但褶皱感明显，整体气质偏自然松弛。',
    tags: ['夏天', '清爽', '自然']
  },
  {
    name: '羊毛',
    alias: 'Wool',
    summary: '保暖性强，适合秋冬，但护理要求高，部分人会觉得扎。',
    tags: ['保暖', '秋冬', '护理要求高']
  },
  {
    name: '真丝',
    alias: 'Silk',
    summary: '光泽好、垂感强、贴肤舒服，但价格高，也更怕磨损和暴晒。',
    tags: ['垂感', '细腻', '娇贵']
  },
  {
    name: '涤纶',
    alias: 'Polyester',
    summary: '耐穿、抗皱、成本低，但透气性一般，闷热天气穿着体验可能偏差。',
    tags: ['耐用', '抗皱', '透气一般']
  },
  {
    name: '醋酸',
    alias: 'Acetate',
    summary: '表面高级、垂感不错，常被误认成真丝，但耐热和耐磨性没有那么强。',
    tags: ['光泽', '垂感', '怕高温']
  }
];

const careGuides = [
  {
    title: '不可漂白',
    content: '说明这件衣服不能使用含氯漂白剂，容易伤面料或让颜色失真。'
  },
  {
    title: '低温熨烫',
    content: '高温容易让合成纤维、真丝、醋酸这类面料变形或发亮。'
  },
  {
    title: '不可烘干',
    content: '说明高温滚筒烘干可能缩水、变形，尤其针织和羊毛更要注意。'
  },
  {
    title: '建议手洗',
    content: '一般说明面料更娇贵，或者有印花、拼接、结构，机洗风险更高。'
  }
];

const typeOptions = [
  { label: '全部', value: 'all' },
  { label: '面料', value: 'fabric' },
  { label: '水洗标', value: 'care-label' },
  { label: '指南', value: 'guide' }
];

const typeLabelMap = {
  fabric: '面料',
  'care-label': '水洗标',
  guide: '指南'
};

const searchKeywords = ['羊毛', '真丝', '亚麻', '不可漂白', '低温熨烫', '针织护理'];

const searchForm = reactive({
  query: '',
  type: 'all'
});

const searchResults = ref([]);
const detailType = ref('');
const detailData = ref(null);
const isSearching = ref(false);

const detailTitle = computed(() => {
  if (!detailData.value) {
    return '暂无内容';
  }
  if (detailType.value === 'fabric') {
    return '面料详情';
  }
  if (detailType.value === 'care-label') {
    return '水洗标详情';
  }
  if (detailType.value === 'guide') {
    return '指南详情';
  }
  return '详细内容';
});

const fabricScores = (scores = {}) => [
  { key: 'warmth', label: '保暖', value: scores.warmth ?? '--' },
  { key: 'breathability', label: '透气', value: scores.breathability ?? '--' },
  { key: 'comfort', label: '舒适', value: scores.comfort ?? '--' },
  { key: 'durability', label: '耐用', value: scores.durability ?? '--' }
];

const submitSearch = async () => {
  if (!searchForm.query || isSearching.value) {
    return;
  }

  isSearching.value = true;
  try {
    const response = await knowledgeApi.search(searchForm.query, searchForm.type, 12);
    searchResults.value = response.data?.items || [];
  } catch (error) {
    console.error('Search knowledge failed:', error);
    alert(error.response?.data?.message || '搜索失败');
  } finally {
    isSearching.value = false;
  }
};

const runQuickSearch = async (keyword) => {
  searchForm.query = keyword;
  await submitSearch();
};

const resolveDocumentMeta = (item = {}) => {
  const documentId = item.documentId || '';
  if (documentId.startsWith('fabric-')) {
    return { type: 'fabric', id: Number(documentId.replace('fabric-', '')) };
  }
  if (documentId.startsWith('guide-')) {
    return { type: 'guide', id: Number(documentId.replace('guide-', '')) };
  }
  if (documentId.startsWith('care-label-')) {
    return { type: 'care-label', symbolCode: documentId.replace('care-label-', '') };
  }
  return { type: item.type };
};

const openSearchItem = async (item) => {
  const meta = resolveDocumentMeta(item);

  if (meta.type === 'fabric' && meta.id) {
    try {
      const response = await knowledgeApi.getFabricDetail(meta.id);
      detailType.value = 'fabric';
      detailData.value = response.data;
    } catch (error) {
      console.error('Load fabric detail failed:', error);
      alert(error.response?.data?.message || '读取面料详情失败');
    }
    return;
  }

  if (meta.type === 'guide' && meta.id) {
    try {
      const response = await knowledgeApi.getGuideDetail(meta.id);
      detailType.value = 'guide';
      detailData.value = response.data;
    } catch (error) {
      console.error('Load guide detail failed:', error);
      alert(error.response?.data?.message || '读取指南详情失败');
    }
    return;
  }

  if (meta.type === 'care-label' && meta.symbolCode) {
    try {
      const response = await knowledgeApi.getCareLabelDetail(meta.symbolCode);
      detailType.value = 'care-label';
      detailData.value = response.data;
    } catch (error) {
      console.error('Load care label detail failed:', error);
      alert(error.response?.data?.message || '读取水洗标详情失败');
    }
  }
};

const goBack = () => {
  if (window.history.length > 1) {
    router.back();
    return;
  }
  router.push('/profile/system');
};
</script>

<style scoped>
.knowledge-page {
  min-height: 100vh;
  padding: 20px 16px 108px;
  background:
    radial-gradient(circle at top, rgba(255, 251, 245, 0.9), transparent 26%),
    linear-gradient(180deg, var(--bg-base) 0%, var(--bg-soft) 100%);
}

.page-header,
.section-head,
.result-top,
.fabric-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-header {
  gap: 14px;
  align-items: flex-start;
}

.icon-button {
  display: inline-grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--line-soft);
  border-radius: 50%;
  background: color-mix(in srgb, var(--surface-strong) 90%, transparent);
  cursor: pointer;
}

.icon-button span {
  width: 10px;
  height: 10px;
  border-left: 1.5px solid var(--text-main);
  border-bottom: 1.5px solid var(--text-main);
  transform: rotate(45deg);
  margin-left: 4px;
}

.page-caption,
.section-caption {
  color: var(--text-faint);
  font-size: 10px;
  letter-spacing: 0.12em;
}

.page-header h1 {
  margin-top: 5px;
  font-size: 22px;
}

.panel-card,
.fabric-card,
.guide-card,
.result-card,
.score-card,
.summary-card,
.empty-shell {
  border-radius: 22px;
  border: 1px solid var(--line-soft);
  background: color-mix(in srgb, var(--surface-strong) 92%, transparent);
  box-shadow: var(--shadow-card);
}

.panel-card {
  margin-top: 14px;
  padding: 16px;
}

.fabric-card p,
.guide-card p,
.result-card p,
.detail-copy,
.empty-shell p {
  color: var(--text-soft);
  font-size: 12px;
  line-height: 1.7;
}

.section-head h2 {
  margin-top: 4px;
  font-size: 15px;
}

.fabric-grid,
.guide-list,
.result-list,
.score-grid {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.fabric-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.fabric-card,
.guide-card,
.result-card,
.score-card {
  padding: 14px;
}

.fabric-top strong,
.guide-card strong,
.result-card h3,
.summary-card strong,
.score-card strong {
  color: var(--text-main);
  font-size: 13px;
}

.fabric-top span,
.score-card span,
.type-badge,
.result-top strong,
.summary-card span {
  color: var(--text-faint);
  font-size: 11px;
}

.tag-row,
.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.tag-row span,
.quick-chip,
.filter-chip {
  border: none;
  border-radius: 999px;
  padding: 6px 10px;
  background: var(--accent-soft);
  color: var(--accent-strong);
  font-size: 11px;
}

.filter-row,
.search-bar,
.quick-row,
.result-list {
  margin-top: 16px;
}

.search-bar {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
}

.search-bar input {
  width: 100%;
  border: 1px solid var(--line-soft);
  border-radius: 16px;
  padding: 12px 14px;
  background: var(--surface-strong);
  color: var(--text-main);
  font-size: 12px;
  outline: none;
}

.quick-chip,
.filter-chip,
.solid-button {
  cursor: pointer;
}

.filter-chip.active,
.solid-button {
  background: var(--accent-strong);
  color: var(--surface-strong);
}

.solid-button {
  border: none;
  border-radius: 999px;
  padding: 10px 16px;
  font-size: 11px;
}

.solid-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.result-card {
  cursor: pointer;
}

.result-card h3 {
  margin-top: 8px;
}

.type-badge {
  display: inline-flex;
  align-items: center;
}

.empty-shell {
  margin-top: 16px;
  padding: 22px 16px;
  text-align: center;
}

.empty-shell.thin {
  margin-top: 0;
}

.summary-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 14px;
}

.detail-copy,
.score-grid {
  margin-top: 14px;
}

.score-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

@media (min-width: 768px) {
  .knowledge-page {
    padding: 28px 24px 40px;
  }

  .result-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .search-bar,
  .fabric-grid,
  .score-grid {
    grid-template-columns: 1fr;
  }
}
</style>
