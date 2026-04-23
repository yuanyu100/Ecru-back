<template>
  <div class="home-page">
    <header class="hero-card">
      <div class="hero-topbar">
        <p class="eyebrow">Ecru Mobile</p>
        <div class="topbar-actions">
          <button v-if="!isAuthenticated" class="ghost-button" type="button" @click="navigateToLogin">
            登录
          </button>
          <template v-else>
            <button class="ghost-button" type="button" @click="navigateToProfile">个人页</button>
            <button class="ghost-button" type="button" @click="navigateToChat">搭配助手</button>
          </template>
        </div>
      </div>

      <div class="hero-content">
        <div>
          <h1>{{ headline }}</h1>
          <p class="hero-copy">
            先把移动端主链路走通：衣橱录入、AI 对话、偏好设置和最近搭配，都集中在这里。
          </p>
        </div>

        <div class="hero-metrics">
          <article class="metric-pill">
            <span>最近会话</span>
            <strong>{{ conversationList.length }}</strong>
          </article>
          <article class="metric-pill">
            <span>搭配记录</span>
            <strong>{{ outfitHistory.length }}</strong>
          </article>
          <article class="metric-pill">
            <span>偏好标签</span>
            <strong>{{ preferredStyleCount }}</strong>
          </article>
        </div>
      </div>
    </header>

    <section class="action-grid">
      <article class="action-card primary" @click="navigateToChat">
        <p class="eyebrow">AI Flow</p>
        <h2>开始智能搭配</h2>
        <p>进入真实聊天链路，直接让后端结合衣橱推荐单品。</p>
        <button class="primary-button" type="button" @click.stop="navigateToChat">去聊天</button>
      </article>

      <article class="action-card" @click="navigateToAddClothing">
        <p class="eyebrow">Wardrobe</p>
        <h2>录入新衣物</h2>
        <p>支持上传图片到 MinIO，并走 AI 识别或手动录入。</p>
        <button class="ghost-button" type="button" @click.stop="navigateToAddClothing">去上传</button>
      </article>

      <article class="action-card" @click="navigateToWardrobe">
        <p class="eyebrow">Collection</p>
        <h2>管理我的衣橱</h2>
        <p>查看已录入衣物、编辑主图、完善颜色和使用频率。</p>
        <button class="ghost-button" type="button" @click.stop="navigateToWardrobe">去衣橱</button>
      </article>

      <article class="action-card" @click="navigateToProfile">
        <p class="eyebrow">Profile</p>
        <h2>维护个人偏好</h2>
        <p>同步风格偏好、尺码、地区和头像，方便后续推荐更稳定。</p>
        <button class="ghost-button" type="button" @click.stop="navigateToProfile">去设置</button>
      </article>
    </section>

    <div v-if="isLoading" class="state-card">正在加载首页数据...</div>

    <div v-else class="content-grid">
      <section class="panel">
        <div class="section-header">
          <div>
            <p class="eyebrow">Style Profile</p>
            <h2>风格画像</h2>
          </div>
          <button class="ghost-button" type="button" @click="navigateToProfile">完善资料</button>
        </div>

        <div class="profile-summary">
          <div class="summary-item">
            <span>气质类型</span>
            <strong>{{ styleProfile.temperamentType || '待完善' }}</strong>
          </div>
          <div class="summary-item">
            <span>体型</span>
            <strong>{{ styleProfile.bodyType || '待完善' }}</strong>
          </div>
          <div class="summary-item">
            <span>职业</span>
            <strong>{{ styleProfile.occupation || '待完善' }}</strong>
          </div>
        </div>

        <div class="tag-section">
          <p>偏好风格</p>
          <div class="tag-row">
            <span v-for="tag in styleProfile.preferredStylesList" :key="tag" class="tag-chip">
              {{ tag }}
            </span>
            <span v-if="styleProfile.preferredStylesList.length === 0" class="muted-text">还没有偏好风格</span>
          </div>
        </div>

        <div class="tag-section">
          <p>偏好颜色</p>
          <div class="tag-row">
            <span v-for="tag in styleProfile.preferredColorsList" :key="tag" class="tag-chip soft">
              {{ tag }}
            </span>
            <span v-if="styleProfile.preferredColorsList.length === 0" class="muted-text">还没有偏好颜色</span>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="section-header">
          <div>
            <p class="eyebrow">Recent Chat</p>
            <h2>最近会话</h2>
          </div>
          <button class="ghost-button" type="button" @click="navigateToChat">查看全部</button>
        </div>

        <div v-if="conversationList.length === 0" class="empty-block">
          <p>还没有会话记录。</p>
          <button class="primary-button" type="button" @click="navigateToChat">开始第一次搭配咨询</button>
        </div>

        <div v-else class="list-block">
          <article
            v-for="conversation in conversationList"
            :key="conversation.sessionId"
            class="list-card"
            @click="openConversation(conversation.sessionId)"
          >
            <div>
              <h3>{{ conversation.title }}</h3>
              <p>{{ conversation.lastMessagePreview || '暂无摘要' }}</p>
            </div>
            <span>{{ formatTime(conversation.updatedAt || conversation.createdAt) }}</span>
          </article>
        </div>
      </section>

      <section class="panel wide">
        <div class="section-header">
          <div>
            <p class="eyebrow">Outfit History</p>
            <h2>最近搭配记录</h2>
          </div>
          <button class="ghost-button" type="button" @click="navigateToChat">继续生成</button>
        </div>

        <div v-if="outfitHistory.length === 0" class="empty-block">
          <p>目前还没有落库的搭配记录。</p>
          <p class="muted-text">这通常说明你还没有使用“搭配建议记录”这条旧接口，现阶段先走 AI 聊天也没问题。</p>
        </div>

        <div v-else class="history-grid">
          <article v-for="item in outfitHistory" :key="item.id" class="history-card">
            <div class="history-card-top">
              <div>
                <h3>{{ item.outfitName }}</h3>
                <p>{{ item.outfitDescription || '暂无描述' }}</p>
              </div>
              <span :class="['favorite-badge', item.isFavorite ? 'active' : '']">
                {{ item.isFavorite ? '已收藏' : '未收藏' }}
              </span>
            </div>

            <div class="history-meta">
              <span>{{ item.occasion || '未标注场景' }}</span>
              <span>{{ item.weatherCondition || '未标注天气' }}</span>
              <span>{{ formatTime(item.createdAt) }}</span>
            </div>
          </article>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { authApi } from '../api/auth';
import { chatApi } from '../api/chat';
import { outfitApi } from '../api/outfit';

const router = useRouter();
const isAuthenticated = ref(false);
const isLoading = ref(false);
const currentProfile = ref(null);
const styleProfile = ref({
  preferredStylesList: [],
  preferredColorsList: []
});
const conversationList = ref([]);
const outfitHistory = ref([]);

const headline = computed(() => {
  if (!isAuthenticated.value) {
    return '你的智能穿搭工作台';
  }

  const nickname = currentProfile.value?.nickname || currentProfile.value?.username || '同学';
  return `${nickname}，今天想怎么穿？`;
});

const preferredStyleCount = computed(() => {
  return styleProfile.value?.preferredStylesList?.length || 0;
});

const loadDashboard = async () => {
  isAuthenticated.value = authApi.isAuthenticated();

  if (!isAuthenticated.value) {
    return;
  }

  isLoading.value = true;
  try {
    const [profileResponse, styleResponse, conversationResponse, historyResponse] = await Promise.all([
      authApi.getCurrentProfile(),
      outfitApi.getStyleProfile(),
      chatApi.getConversations(1, 4),
      outfitApi.getHistory(1, 4)
    ]);

    currentProfile.value = profileResponse.data;
    styleProfile.value = styleResponse.data || {
      preferredStylesList: [],
      preferredColorsList: []
    };
    conversationList.value = conversationResponse.data?.items || [];
    outfitHistory.value = historyResponse.data || [];
  } catch (error) {
    console.error('Load dashboard failed:', error);
    alert(error.response?.data?.message || '加载首页数据失败');
  } finally {
    isLoading.value = false;
  }
};

const navigateToLogin = () => {
  router.push('/login');
};

const navigateToProfile = () => {
  router.push('/profile');
};

const navigateToChat = () => {
  router.push('/chat');
};

const navigateToWardrobe = () => {
  router.push('/wardrobe');
};

const navigateToAddClothing = () => {
  router.push('/wardrobe/add');
};

const openConversation = (sessionId) => {
  if (sessionId) {
    localStorage.setItem('chatSessionId', sessionId);
  }
  router.push('/chat');
};

const formatTime = (value) => {
  if (!value) {
    return '刚刚';
  }

  return new Date(value).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

onMounted(loadDashboard);
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  padding: 18px 16px 40px;
  background:
    radial-gradient(circle at top left, rgba(255, 243, 214, 0.88), transparent 30%),
    radial-gradient(circle at top right, rgba(228, 208, 172, 0.55), transparent 26%),
    linear-gradient(180deg, #f8f1df 0%, #efe2ca 100%);
}

.hero-card,
.action-card,
.panel,
.state-card {
  border-radius: 26px;
  background: rgba(255, 251, 244, 0.92);
  border: 1px solid rgba(145, 104, 49, 0.14);
  box-shadow: 0 18px 44px rgba(109, 78, 38, 0.08);
}

.hero-card {
  padding: 18px;
}

.hero-topbar,
.topbar-actions,
.hero-metrics,
.section-header,
.history-card-top,
.history-meta {
  display: flex;
  align-items: center;
}

.hero-topbar,
.section-header,
.history-card-top {
  justify-content: space-between;
}

.topbar-actions,
.hero-metrics,
.action-grid,
.content-grid,
.list-block,
.history-grid,
.tag-row {
  gap: 12px;
}

.eyebrow {
  margin-bottom: 6px;
  color: #8f6a37;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-content {
  display: grid;
  gap: 18px;
  margin-top: 18px;
}

.hero-content h1,
.action-card h2,
.panel h2,
.list-card h3,
.history-card h3 {
  color: #5d4523;
}

.hero-content h1 {
  font-size: 32px;
  line-height: 1.2;
}

.hero-copy,
.action-card p,
.list-card p,
.history-card p,
.muted-text {
  color: #7a6140;
}

.hero-metrics {
  flex-wrap: wrap;
}

.metric-pill {
  min-width: 110px;
  padding: 12px 14px;
  border-radius: 18px;
  background: linear-gradient(180deg, #fffdf8 0%, #f4ead9 100%);
}

.metric-pill span {
  display: block;
  color: #8b6f48;
  font-size: 12px;
}

.metric-pill strong {
  display: block;
  margin-top: 6px;
  color: #5d4523;
  font-size: 24px;
}

.action-grid,
.content-grid,
.profile-summary {
  display: grid;
  margin-top: 18px;
}

.action-card {
  padding: 18px;
  cursor: pointer;
}

.action-card.primary {
  background:
    linear-gradient(135deg, rgba(107, 75, 31, 0.96), rgba(154, 114, 58, 0.92)),
    rgba(255, 251, 244, 0.92);
}

.action-card.primary .eyebrow,
.action-card.primary h2,
.action-card.primary p {
  color: #fff5e8;
}

.profile-summary {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.summary-item {
  padding: 14px;
  border-radius: 18px;
  background: #fffdf8;
}

.summary-item span {
  display: block;
  color: #8b6f48;
  font-size: 12px;
}

.summary-item strong {
  display: block;
  margin-top: 8px;
  color: #5d4523;
  font-size: 16px;
}

.panel,
.state-card {
  padding: 18px;
}

.state-card,
.empty-block {
  text-align: center;
  color: #6c522f;
}

.tag-section + .tag-section {
  margin-top: 16px;
}

.tag-section p {
  margin-bottom: 10px;
  color: #7c6241;
  font-size: 14px;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
}

.tag-chip {
  border-radius: 999px;
  padding: 8px 12px;
  background: #f1debd;
  color: #6b4b1f;
  font-size: 12px;
}

.tag-chip.soft {
  background: #fff1d8;
}

.list-card,
.history-card {
  padding: 14px;
  border-radius: 18px;
  background: #fffdf8;
  border: 1px solid rgba(145, 104, 49, 0.12);
}

.list-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  cursor: pointer;
}

.list-card p {
  margin-top: 8px;
  line-height: 1.6;
}

.list-card span,
.history-meta span {
  color: #8b6f48;
  font-size: 12px;
}

.history-grid {
  display: grid;
}

.history-card-top {
  gap: 10px;
}

.history-card p {
  margin-top: 8px;
  line-height: 1.6;
}

.history-meta {
  flex-wrap: wrap;
  margin-top: 12px;
}

.favorite-badge {
  flex-shrink: 0;
  border-radius: 999px;
  padding: 8px 10px;
  background: #efe1c8;
  color: #7c6241;
  font-size: 12px;
}

.favorite-badge.active {
  background: #6b4b1f;
  color: #fff8ef;
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
}

.primary-button {
  background: #6b4b1f;
  color: #fff8ef;
}

@media (max-width: 767px) {
  .topbar-actions {
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  .profile-summary {
    grid-template-columns: 1fr;
  }
}

@media (min-width: 900px) {
  .home-page {
    padding: 28px 28px 48px;
  }

  .action-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .content-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .panel.wide {
    grid-column: span 2;
  }

  .history-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
