<template>
  <div class="knowledge-page">
    <header class="knowledge-header">
      <div>
        <p class="eyebrow">Material Lab</p>
        <h1>材质与洗护助手</h1>
        <p class="header-copy">上传成分标或面料图片，识别材质、洗护信息，并直接追问“这种面料好不好”。</p>
      </div>

      <div class="header-actions">
        <button class="ghost-button" type="button" @click="goHome">首页</button>
        <button class="ghost-button" type="button" @click="goChat">穿搭助手</button>
      </div>
    </header>

    <section class="hero-grid">
      <article class="hero-card primary">
        <p class="eyebrow">识别</p>
        <h2>看标签就能问</h2>
        <p>支持识别成分、材质比例、洗护标提示，并自动联动知识库给出解释。</p>
      </article>

      <article class="hero-card">
        <p class="eyebrow">问答</p>
        <h2>不止“能不能水洗”</h2>
        <p>还可以继续问透气性、舒适度、适用季节、优缺点和购买建议。</p>
      </article>

      <article class="hero-card">
        <p class="eyebrow">知识库</p>
        <h2>面料 / 洗护 / 指南</h2>
        <p>可独立搜索棉、羊毛、真丝、不可漂白等知识条目，直接查看详情。</p>
      </article>
    </section>

    <div class="content-grid">
      <section class="panel-card">
        <div class="section-head">
          <div>
            <p class="eyebrow">Image Analyze</p>
            <h2>图片识别</h2>
          </div>
          <span class="section-tip">支持成分标、洗护标、面料局部图</span>
        </div>

        <label class="upload-panel">
          <input type="file" accept="image/*" @change="handleFileChange" />
          <template v-if="imagePreview">
            <img :src="imagePreview" alt="材质识别预览图" />
          </template>
          <template v-else>
            <div class="upload-copy">
              <strong>上传图片开始识别</strong>
              <span>建议拍清楚成分表、洗护标或衣物局部面料纹理</span>
            </div>
          </template>
        </label>

        <div class="form-grid">
          <label>
            <span>材质补充提示</span>
            <input v-model.trim="analyzeForm.materialHint" type="text" placeholder="例如：像羊毛大衣里料，或者像棉麻衬衫" />
          </label>
          <label class="wide">
            <span>识别后想问什么</span>
            <textarea
              v-model.trim="analyzeForm.question"
              rows="3"
              placeholder="例如：这种材质适合夏天吗？会不会容易皱？"
            ></textarea>
          </label>
        </div>

        <div class="quick-row">
          <button v-for="prompt in analyzePrompts" :key="prompt" class="chip-button" type="button" @click="analyzeForm.question = prompt">
            {{ prompt }}
          </button>
        </div>

        <div class="action-row">
          <span class="muted-text">{{ selectedImage ? `已选择：${selectedImage.name}` : '还没有选择图片' }}</span>
          <button class="primary-button" type="button" :disabled="isAnalyzing || !selectedImage" @click="submitAnalyze">
            {{ isAnalyzing ? '识别中...' : '开始识别' }}
          </button>
        </div>
      </section>

      <section class="panel-card">
        <div class="section-head">
          <div>
            <p class="eyebrow">Ask Directly</p>
            <h2>直接提问</h2>
          </div>
          <span class="section-tip">适合已知材质名称时快速问答</span>
        </div>

        <div class="form-grid">
          <label>
            <span>材质 / 面料</span>
            <input v-model.trim="askForm.material" type="text" placeholder="例如：棉、羊毛、牛仔、真丝、黏胶" />
          </label>
          <label class="wide">
            <span>问题</span>
            <textarea
              v-model.trim="askForm.question"
              rows="4"
              placeholder="例如：这种材质好吗？优缺点是什么？适合日常通勤吗？"
            ></textarea>
          </label>
        </div>

        <div class="quick-row">
          <button v-for="prompt in askPrompts" :key="prompt" class="chip-button" type="button" @click="useAskPrompt(prompt)">
            {{ prompt }}
          </button>
        </div>

        <div class="action-row">
          <span class="muted-text">建议直接问优缺点、场景、季节、洗护方式</span>
          <button
            class="primary-button"
            type="button"
            :disabled="isAsking || !askForm.material || !askForm.question"
            @click="submitAsk"
          >
            {{ isAsking ? '回答中...' : '发送问题' }}
          </button>
        </div>
      </section>
    </div>

    <section class="panel-card">
      <div class="section-head search-head">
        <div>
          <p class="eyebrow">Knowledge Search</p>
          <h2>知识库搜索</h2>
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
      </div>

      <form class="search-bar" @submit.prevent="submitSearch">
        <input v-model.trim="searchForm.query" type="text" placeholder="搜索面料、洗护说明或搭配指南，例如：棉、不可漂白、春秋通勤" />
        <button class="primary-button" type="submit" :disabled="isSearching || !searchForm.query">
          {{ isSearching ? '搜索中...' : '搜索' }}
        </button>
      </form>

      <div class="quick-row">
        <button v-for="keyword in searchKeywords" :key="keyword" class="chip-button" type="button" @click="runQuickSearch(keyword)">
          {{ keyword }}
        </button>
      </div>

      <div v-if="searchResults.length" class="result-grid">
        <article v-for="item in searchResults" :key="item.documentId" class="result-card" @click="openSearchItem(item)">
          <div class="result-top">
            <span :class="['type-badge', item.type]">{{ typeLabelMap[item.type] || item.type }}</span>
            <strong>{{ item.relevance }}%</strong>
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.content }}</p>
          <div class="tag-row">
            <span v-for="tag in item.tags" :key="`${item.documentId}-${tag}`" class="tag-chip soft">
              {{ tag }}
            </span>
          </div>
        </article>
      </div>

      <div v-else class="empty-block">
        <p>还没有搜索结果。</p>
        <p class="muted-text">可以试试“羊毛”“真丝”“不可漂白”“通勤指南”。</p>
      </div>
    </section>

    <div class="result-columns">
      <section class="panel-card">
        <div class="section-head">
          <div>
            <p class="eyebrow">Latest Result</p>
            <h2>最近一次识别 / 问答</h2>
          </div>
          <span class="section-tip">{{ latestResultLabel }}</span>
        </div>

        <div v-if="!latestResponse" class="empty-block">
          <p>还没有结果。</p>
          <p class="muted-text">先上传图片识别，或直接输入材质提问。</p>
        </div>

        <template v-else>
          <div v-if="latestResponse.material" class="summary-banner">
            <span>推断材质</span>
            <strong>{{ latestResponse.material }}</strong>
          </div>

          <div v-if="latestResponse.analysis" class="analysis-block">
            <div class="mini-grid">
              <article class="mini-card">
                <span>识别品类</span>
                <strong>{{ latestResponse.analysis.productType || '未识别' }}</strong>
              </article>
              <article class="mini-card">
                <span>识别置信度</span>
                <strong>{{ formatConfidence(latestResponse.analysis.confidence) }}</strong>
              </article>
            </div>

            <p v-if="latestResponse.analysis.summary" class="result-copy">{{ latestResponse.analysis.summary }}</p>

            <div v-if="latestResponse.analysis.materials.length" class="info-group">
              <h3>识别到的材质成分</h3>
              <div class="tag-row">
                <span
                  v-for="(item, index) in latestResponse.analysis.materials"
                  :key="`material-${index}`"
                  class="tag-chip"
                >
                  {{ formatDetectedMaterial(item) }}
                </span>
              </div>
            </div>

            <div v-if="latestResponse.analysis.careLabels.length" class="info-group">
              <h3>识别到的洗护提示</h3>
              <div class="tag-row">
                <span
                  v-for="(item, index) in latestResponse.analysis.careLabels"
                  :key="`care-${index}`"
                  class="tag-chip soft"
                >
                  {{ formatDetectedCareLabel(item) }}
                </span>
              </div>
            </div>

            <div v-if="latestResponse.analysis.detectedText" class="info-group">
              <h3>OCR 文本</h3>
              <p class="ocr-block">{{ latestResponse.analysis.detectedText }}</p>
            </div>
          </div>

          <div v-if="latestResponse.answer" class="info-group">
            <h3>回答</h3>
            <p class="answer-copy">{{ latestResponse.answer }}</p>
            <span class="source-label">来源：{{ latestResponse.answerSource === 'ai' ? 'AI + 知识库' : '知识库回退结果' }}</span>
          </div>

          <div v-if="latestResponse.matchedFabrics?.length" class="info-group">
            <div class="subsection-title">
              <h3>匹配面料</h3>
              <span>{{ latestResponse.matchedFabrics.length }} 条</span>
            </div>
            <div class="stack-list">
              <article
                v-for="fabric in latestResponse.matchedFabrics"
                :key="fabric.fabricId"
                class="info-card"
                @click="openFabricDetail(fabric)"
              >
                <div class="info-card-top">
                  <strong>{{ fabric.name }}</strong>
                  <span>{{ fabric.relevance || '--' }}%</span>
                </div>
                <p>{{ fabric.summary || fabric.properties || '暂无摘要' }}</p>
                <div class="tag-row">
                  <span v-for="tag in fabric.suitableSeasons" :key="`${fabric.fabricId}-${tag}`" class="tag-chip">
                    {{ tag }}
                  </span>
                </div>
              </article>
            </div>
          </div>

          <div v-if="latestResponse.matchedCareLabels?.length" class="info-group">
            <div class="subsection-title">
              <h3>匹配洗护说明</h3>
              <span>{{ latestResponse.matchedCareLabels.length }} 条</span>
            </div>
            <div class="stack-list">
              <article
                v-for="label in latestResponse.matchedCareLabels"
                :key="label.symbolCode || label.careLabelId"
                class="info-card"
                @click="openCareLabelDetail(label)"
              >
                <div class="info-card-top">
                  <strong>{{ label.symbolName }}</strong>
                  <span>{{ label.relevance || '--' }}%</span>
                </div>
                <p>{{ label.instruction || label.explanation || '暂无说明' }}</p>
              </article>
            </div>
          </div>
        </template>
      </section>

      <section class="panel-card detail-panel">
        <div class="section-head">
          <div>
            <p class="eyebrow">Detail</p>
            <h2>知识详情</h2>
          </div>
          <span class="section-tip">{{ detailTitle }}</span>
        </div>

        <div v-if="isDetailLoading" class="empty-block">
          <p>正在加载详情...</p>
        </div>

        <div v-else-if="!detailData" class="empty-block">
          <p>点一下面料、洗护说明或搜索结果。</p>
          <p class="muted-text">右侧会展示更完整的材质特性、场景建议和洗护说明。</p>
        </div>

        <template v-else-if="detailType === 'fabric'">
          <div class="detail-header">
            <h3>{{ detailData.name }}</h3>
            <span class="type-badge fabric">面料</span>
          </div>
          <p class="result-copy">{{ detailData.summary || '暂无摘要' }}</p>

          <div class="info-group">
            <h3>别名</h3>
            <div class="tag-row">
              <span v-for="tag in detailData.alias" :key="tag" class="tag-chip">{{ tag }}</span>
            </div>
          </div>

          <div class="info-group">
            <h3>材质特征</h3>
            <div class="score-grid">
              <article v-for="item in fabricScores(detailData.characteristics)" :key="item.key" class="score-card">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </article>
            </div>
            <p class="result-copy">{{ detailData.properties || '暂无详细特征说明' }}</p>
          </div>

          <div class="info-group">
            <h3>适用场景</h3>
            <div class="tag-row">
              <span v-for="tag in detailData.suitableSeasons" :key="`season-${tag}`" class="tag-chip soft">{{ tag }}</span>
              <span v-for="tag in detailData.suitableOccasions" :key="`occasion-${tag}`" class="tag-chip">{{ tag }}</span>
            </div>
          </div>

          <div class="info-group">
            <h3>洗护建议</h3>
            <p class="result-copy">{{ detailData.careGuide || '暂无洗护建议' }}</p>
          </div>
        </template>

        <template v-else-if="detailType === 'care-label'">
          <div class="detail-header">
            <h3>{{ detailData.symbolName }}</h3>
            <span class="type-badge care-label">洗护</span>
          </div>
          <p class="result-copy">{{ detailData.instruction || '暂无说明' }}</p>

          <div class="info-group">
            <h3>解释</h3>
            <p class="result-copy">{{ detailData.explanation || '暂无补充解释' }}</p>
          </div>

          <div class="mini-grid">
            <article class="mini-card">
              <span>建议做法</span>
              <strong>{{ detailData.doText || '暂无' }}</strong>
            </article>
            <article class="mini-card">
              <span>避免操作</span>
              <strong>{{ detailData.dontText || '暂无' }}</strong>
            </article>
          </div>
        </template>

        <template v-else-if="detailType === 'guide'">
          <div class="detail-header">
            <h3>{{ detailData.title }}</h3>
            <span class="type-badge guide">指南</span>
          </div>
          <p v-if="detailData.subtitle" class="muted-text">{{ detailData.subtitle }}</p>
          <p class="result-copy">{{ detailData.content || '暂无正文' }}</p>

          <div class="mini-grid">
            <article class="mini-card">
              <span>类型</span>
              <strong>{{ detailData.guideType || '未分类' }}</strong>
            </article>
            <article class="mini-card">
              <span>发布时间</span>
              <strong>{{ formatDate(detailData.publishDate) }}</strong>
            </article>
          </div>

          <div class="tag-row">
            <span v-for="tag in detailData.tags" :key="tag" class="tag-chip">{{ tag }}</span>
          </div>
        </template>

        <template v-else>
          <div class="detail-header">
            <h3>{{ detailData.title }}</h3>
            <span class="type-badge">{{ detailTitle }}</span>
          </div>
          <p class="result-copy">{{ detailData.content || '暂无详情' }}</p>
        </template>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { knowledgeApi } from '../api/knowledge';

const router = useRouter();

const typeOptions = [
  { label: '全部', value: 'all' },
  { label: '面料', value: 'fabric' },
  { label: '洗护', value: 'care-label' },
  { label: '指南', value: 'guide' }
];

const typeLabelMap = {
  fabric: '面料',
  'care-label': '洗护',
  guide: '指南'
};

const analyzePrompts = [
  '这种材质适合夏天吗？',
  '这种面料有什么优缺点？',
  '应该怎么洗才不容易坏？'
];

const askPrompts = [
  '这种材质好吗？',
  '优缺点是什么？',
  '适合日常通勤吗？',
  '应该怎么洗护？'
];

const searchKeywords = ['棉', '羊毛', '真丝', '牛仔', '不可漂白', '通勤'];

const searchForm = reactive({
  query: '',
  type: 'all'
});

const askForm = reactive({
  material: '',
  question: ''
});

const analyzeForm = reactive({
  materialHint: '',
  question: ''
});

const searchResults = ref([]);
const selectedImage = ref(null);
const imagePreview = ref('');
const latestResponse = ref(null);
const latestResultType = ref('');
const detailType = ref('');
const detailData = ref(null);
const isSearching = ref(false);
const isAsking = ref(false);
const isAnalyzing = ref(false);
const isDetailLoading = ref(false);

const latestResultLabel = computed(() => {
  if (latestResultType.value === 'analyze') {
    return '来自图片识别';
  }
  if (latestResultType.value === 'ask') {
    return '来自直接问答';
  }
  return '等待输入';
});

const detailTitle = computed(() => typeLabelMap[detailType.value] || '详情');

const handleFileChange = (event) => {
  const [file] = event.target.files || [];
  if (!file) {
    selectedImage.value = null;
    imagePreview.value = '';
    return;
  }

  selectedImage.value = file;
  imagePreview.value = URL.createObjectURL(file);
};

const submitAnalyze = async () => {
  if (!selectedImage.value || isAnalyzing.value) {
    return;
  }

  isAnalyzing.value = true;
  try {
    const response = await knowledgeApi.analyzeMaterialLabel({
      image: selectedImage.value,
      question: analyzeForm.question,
      materialHint: analyzeForm.materialHint
    });
    latestResponse.value = response.data;
    latestResultType.value = 'analyze';

    if (response.data?.matchedFabrics?.length) {
      openFabricDetail(response.data.matchedFabrics[0]);
    } else if (response.data?.matchedCareLabels?.length) {
      openCareLabelDetail(response.data.matchedCareLabels[0]);
    }
  } catch (error) {
    console.error('Analyze material label failed:', error);
    alert(error.response?.data?.message || '图片识别失败');
  } finally {
    isAnalyzing.value = false;
  }
};

const submitAsk = async () => {
  if (!askForm.material || !askForm.question || isAsking.value) {
    return;
  }

  isAsking.value = true;
  try {
    const response = await knowledgeApi.askMaterialQuestion({
      material: askForm.material,
      question: askForm.question
    });
    latestResponse.value = response.data;
    latestResultType.value = 'ask';

    if (response.data?.matchedFabrics?.length) {
      openFabricDetail(response.data.matchedFabrics[0]);
    } else if (response.data?.matchedCareLabels?.length) {
      openCareLabelDetail(response.data.matchedCareLabels[0]);
    }
  } catch (error) {
    console.error('Ask material question failed:', error);
    alert(error.response?.data?.message || '材质问答失败');
  } finally {
    isAsking.value = false;
  }
};

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
    alert(error.response?.data?.message || '知识库搜索失败');
  } finally {
    isSearching.value = false;
  }
};

const runQuickSearch = async (keyword) => {
  searchForm.query = keyword;
  await submitSearch();
};

const useAskPrompt = (prompt) => {
  askForm.question = prompt;
};

const parseDocumentId = (documentId = '') => {
  const [type, rawId] = documentId.split('-', 2);
  const id = Number(rawId || 0);
  return { type, id };
};

const openSearchItem = async (item) => {
  if (!item?.documentId) {
    return;
  }

  if (item.type === 'fabric') {
    const { id } = parseDocumentId(item.documentId);
    if (id) {
      isDetailLoading.value = true;
      try {
        const response = await knowledgeApi.getFabricDetail(id);
        detailType.value = 'fabric';
        detailData.value = response.data;
      } catch (error) {
        console.error('Load fabric detail failed:', error);
        alert(error.response?.data?.message || '加载面料详情失败');
      } finally {
        isDetailLoading.value = false;
      }
      return;
    }
  }

  if (item.type === 'guide') {
    const { id } = parseDocumentId(item.documentId);
    if (id) {
      isDetailLoading.value = true;
      try {
        const response = await knowledgeApi.getGuideDetail(id);
        detailType.value = 'guide';
        detailData.value = response.data;
      } catch (error) {
        console.error('Load guide detail failed:', error);
        alert(error.response?.data?.message || '加载指南详情失败');
      } finally {
        isDetailLoading.value = false;
      }
      return;
    }
  }

  detailType.value = item.type;
  detailData.value = item;
};

const openFabricDetail = (fabric) => {
  detailType.value = 'fabric';
  detailData.value = fabric;
};

const openCareLabelDetail = (label) => {
  detailType.value = 'care-label';
  detailData.value = label;
};

const formatDetectedMaterial = (item = {}) => {
  const name = item.name || item.rawText || '未识别材质';
  return item.ratio ? `${name} ${item.ratio}` : name;
};

const formatDetectedCareLabel = (item = {}) => item.symbolName || item.instruction || item.rawText || '未识别洗护信息';

const formatConfidence = (value) => {
  if (!value) {
    return '未知';
  }
  return `${Math.round(Number(value) * 100)}%`;
};

const formatDate = (value) => {
  if (!value) {
    return '未知';
  }

  return new Date(value).toLocaleDateString('zh-CN');
};

const fabricScores = (scores = {}) => [
  { key: 'warmth', label: '保暖', value: scores.warmth ?? '--' },
  { key: 'breathability', label: '透气', value: scores.breathability ?? '--' },
  { key: 'comfort', label: '舒适', value: scores.comfort ?? '--' },
  { key: 'durability', label: '耐用', value: scores.durability ?? '--' }
];

const goHome = () => {
  router.push('/');
};

const goChat = () => {
  router.push('/chat');
};
</script>

<style scoped>
.knowledge-page {
  min-height: 100vh;
  padding: 20px 16px 92px;
  background:
    radial-gradient(circle at top left, rgba(255, 244, 224, 0.88), transparent 28%),
    radial-gradient(circle at top right, rgba(217, 190, 147, 0.48), transparent 30%),
    linear-gradient(180deg, #f8f1df 0%, #efe2ca 100%);
}

.knowledge-header,
.header-actions,
.section-head,
.action-row,
.search-head,
.filter-row,
.result-top,
.info-card-top,
.subsection-title,
.detail-header {
  display: flex;
  align-items: center;
}

.knowledge-header,
.section-head,
.search-head,
.action-row,
.result-top,
.info-card-top,
.subsection-title,
.detail-header {
  justify-content: space-between;
}

.header-actions,
.filter-row,
.quick-row,
.tag-row,
.hero-grid,
.content-grid,
.result-grid,
.result-columns,
.mini-grid,
.score-grid,
.stack-list {
  gap: 12px;
}

.eyebrow {
  margin-bottom: 6px;
  color: #8f6a37;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.knowledge-header {
  gap: 16px;
  margin-bottom: 18px;
}

.knowledge-header h1,
.hero-card h2,
.panel-card h2,
.result-card h3,
.detail-header h3 {
  color: #5d4523;
}

.header-copy,
.hero-card p,
.result-card p,
.muted-text,
.result-copy,
.ocr-block,
.answer-copy {
  color: #7d6240;
}

.hero-grid,
.content-grid,
.result-columns,
.mini-grid,
.score-grid,
.result-grid {
  display: grid;
}

.hero-card,
.panel-card,
.result-card,
.info-card,
.mini-card,
.score-card,
.summary-banner {
  border-radius: 24px;
  background: rgba(255, 251, 244, 0.92);
  border: 1px solid rgba(145, 104, 49, 0.14);
  box-shadow: 0 18px 44px rgba(109, 78, 38, 0.08);
}

.hero-card,
.panel-card {
  padding: 18px;
}

.hero-card.primary {
  background: linear-gradient(135deg, rgba(107, 75, 31, 0.96), rgba(159, 118, 57, 0.92));
}

.hero-card.primary .eyebrow,
.hero-card.primary h2,
.hero-card.primary p {
  color: #fff7eb;
}

.section-tip {
  color: #8b6f48;
  font-size: 12px;
}

.upload-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 220px;
  margin-top: 14px;
  border-radius: 20px;
  border: 2px dashed #d1b88b;
  background: #fffdf8;
  overflow: hidden;
  cursor: pointer;
}

.upload-panel input {
  display: none;
}

.upload-panel img {
  width: 100%;
  height: 220px;
  object-fit: cover;
}

.upload-copy {
  display: grid;
  gap: 8px;
  text-align: center;
  color: #7d6240;
}

.form-grid {
  display: grid;
  gap: 14px;
  margin-top: 16px;
}

.form-grid label {
  display: grid;
  gap: 8px;
  color: #6d573b;
  font-size: 14px;
}

.form-grid input,
.form-grid textarea,
.search-bar input {
  width: 100%;
  border: 1px solid #d9c39b;
  border-radius: 16px;
  padding: 12px 14px;
  background: #fffdf8;
  color: #5d4523;
}

.form-grid textarea {
  resize: vertical;
}

.wide {
  grid-column: 1 / -1;
}

.quick-row,
.tag-row {
  display: flex;
  flex-wrap: wrap;
}

.quick-row {
  margin-top: 14px;
}

.action-row {
  gap: 12px;
  margin-top: 16px;
}

.search-bar {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  margin-top: 16px;
}

.result-grid,
.stack-list {
  margin-top: 16px;
}

.result-card,
.info-card,
.mini-card,
.score-card {
  padding: 14px;
}

.result-card,
.info-card {
  cursor: pointer;
}

.result-top strong,
.info-card-top span {
  color: #8b6f48;
  font-size: 12px;
}

.result-card h3,
.info-card strong {
  font-size: 16px;
}

.result-card p,
.info-card p,
.answer-copy,
.ocr-block {
  margin-top: 8px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.empty-block {
  padding: 24px 0 8px;
  text-align: center;
  color: #6d573b;
}

.summary-banner {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
}

.summary-banner span,
.mini-card span,
.score-card span,
.source-label {
  color: #8b6f48;
  font-size: 12px;
}

.summary-banner strong,
.mini-card strong,
.score-card strong {
  color: #5d4523;
}

.analysis-block,
.info-group {
  margin-top: 16px;
}

.info-group h3 {
  color: #5d4523;
  font-size: 15px;
}

.subsection-title {
  margin-bottom: 10px;
}

.subsection-title span {
  color: #8b6f48;
  font-size: 12px;
}

.mini-grid,
.score-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 12px;
}

.score-card strong,
.mini-card strong {
  display: block;
  margin-top: 8px;
  font-size: 18px;
}

.tag-chip,
.filter-chip,
.chip-button,
.type-badge,
.ghost-button,
.primary-button {
  border: none;
  border-radius: 999px;
}

.tag-chip,
.filter-chip,
.chip-button,
.type-badge {
  padding: 8px 12px;
  font-size: 12px;
}

.tag-chip {
  background: #f1debd;
  color: #6b4b1f;
}

.tag-chip.soft {
  background: #fff1d8;
}

.filter-chip,
.chip-button,
.ghost-button {
  background: #ead7b8;
  color: #5d4523;
  cursor: pointer;
}

.filter-chip.active,
.primary-button {
  background: #6b4b1f;
  color: #fff8ef;
}

.type-badge {
  background: rgba(107, 75, 31, 0.12);
  color: #6b4b1f;
}

.type-badge.fabric {
  background: rgba(189, 148, 88, 0.18);
}

.type-badge.care-label {
  background: rgba(83, 148, 103, 0.16);
  color: #2f7a47;
}

.type-badge.guide {
  background: rgba(75, 122, 184, 0.16);
  color: #2f5f9b;
}

.ghost-button,
.primary-button {
  padding: 10px 16px;
}

.primary-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.detail-panel {
  min-height: 100%;
}

@media (max-width: 767px) {
  .knowledge-header,
  .action-row,
  .search-bar {
    display: grid;
  }

  .header-actions,
  .search-bar {
    width: 100%;
  }

  .search-bar {
    grid-template-columns: 1fr;
  }

  .mini-grid,
  .score-grid {
    grid-template-columns: 1fr;
  }
}

@media (min-width: 900px) {
  .knowledge-page {
    padding: 28px 28px 40px;
  }

  .hero-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .content-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .result-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .result-columns {
    grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
    align-items: start;
  }
}
</style>
