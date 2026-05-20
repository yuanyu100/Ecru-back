<template>
  <div ref="chatPageRef" class="chat-page">
    <div v-if="historyOpen" class="history-mask" @click="historyOpen = false"></div>

    <aside :class="['history-panel', historyOpen ? 'open' : '']">
      <div class="history-head">
        <div>
          <p class="history-caption">CONVERSATIONS</p>
          <h2>最近对话</h2>
        </div>
        <button class="new-chat-button" type="button" aria-label="新建会话" @click="startNewConversation">
          <svg width="18" height="18" viewBox="0 0 20 20" fill="none" aria-hidden="true">
            <path d="M3 4a1 1 0 0 1 1-1h12a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1H7l-4 3V4z" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round" />
            <line x1="10" y1="7" x2="10" y2="11" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
            <line x1="8" y1="9" x2="12" y2="9" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
          </svg>
        </button>
      </div>

      <div v-if="isConversationLoading" class="history-state">正在读取会话...</div>
      <div v-else-if="conversations.length === 0" class="history-state">还没有历史对话。</div>
      <div v-else class="history-list">
        <article
          v-for="conversation in conversations"
          :key="conversation.sessionId"
          :class="['history-item', currentSessionId === conversation.sessionId ? 'active' : '']"
          @click="selectConversation(conversation.sessionId)"
        >
          <div class="history-copy">
            <strong>{{ conversation.title || '新的对话' }}</strong>
            <p>{{ conversation.lastMessagePreview || '继续和 Ecru 聊穿搭、面料或天气。' }}</p>
            <span>{{ formatTime(conversation.updatedAt || conversation.createdAt) }}</span>
          </div>
          <button class="delete-button" type="button" @click.stop="removeConversation(conversation.sessionId)">删除</button>
        </article>
      </div>
    </aside>

    <header ref="chatHeadRef" class="chat-head">
      <button class="icon-button" type="button" aria-label="打开会话列表" @click="historyOpen = true">
        <span></span>
        <span></span>
      </button>
      <div class="head-copy">
        <p>对话</p>
        <h1>{{ currentConversationLabel }}</h1>
      </div>
      <button class="new-chat-button" type="button" aria-label="新建会话" @click="startNewConversation">
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none" aria-hidden="true">
          <path d="M3 4a1 1 0 0 1 1-1h12a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1H7l-4 3V4z" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round" />
          <line x1="10" y1="7" x2="10" y2="11" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
          <line x1="8" y1="9" x2="12" y2="9" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
        </svg>
      </button>
    </header>

    <section
      ref="messageListRef"
      class="message-list"
      :class="{ empty: messages.length === 0 }"
      @scroll="handleMessageListScroll"
    >
      <div v-if="messages.length === 0" class="empty-state">
        <p class="empty-caption">ECRU</p>
        <h2>说一句今天想怎么穿，或者上传一张水洗标图片。</h2>
        <div class="prompt-list">
          <button v-for="item in quickPrompts" :key="item" class="prompt-chip" type="button" @click="sendQuickPrompt(item)">
            {{ item }}
          </button>
        </div>
      </div>

      <article
        v-for="message in messages"
        :key="message.id"
        :class="['message-card', message.role === 'user' ? 'user' : 'assistant']"
      >
        <div v-if="message.imagePreview" class="image-preview">
          <img :src="message.imagePreview" alt="上传图片" />
        </div>

        <p v-if="message.content && message.role === 'user'" class="message-content">{{ message.content }}</p>
        <div
          v-else-if="message.content"
          class="message-content rich-content"
          v-html="formatAssistantMessage(message.content)"
        ></div>
        <div v-else-if="message.role === 'assistant' && message.isStreaming" class="loading-dots inline-loading">
          <span></span>
          <span></span>
          <span></span>
        </div>

        <div v-if="message.analysisTags?.length" class="tag-row">
          <span v-for="tag in message.analysisTags" :key="`${message.id}-${tag}`">{{ tag }}</span>
        </div>

        <div v-if="message.analysisText" class="analysis-block">
          {{ message.analysisText }}
        </div>

        <div v-if="message.recommendations?.length" class="look-grid">
          <article v-for="(item, index) in message.recommendations" :key="`${message.id}-${index}`" class="look-card">
            <div class="look-image">
              <img :src="resolveRecommendationImage(item)" :alt="resolveRecommendationTitle(item)" />
            </div>
            <div class="look-copy">
              <strong>{{ resolveRecommendationTitle(item) }}</strong>
              <p>{{ resolveRecommendationSubtitle(item) }}</p>
            </div>
          </article>
        </div>

        <div class="message-meta">
          <div v-if="isAssistantActionVisible(message)" class="message-actions">
            <button
              :class="['message-action-button', 'copy-action', { copied: copiedMessageId === message.id }]"
              type="button"
              :aria-label="copiedMessageId === message.id ? '已复制' : '复制回答'"
              :title="copiedMessageId === message.id ? '已复制' : '复制回答'"
              @click="copyMessageContent(message)"
            >
              <svg v-if="copiedMessageId === message.id" width="15" height="15" viewBox="0 0 16 16" aria-hidden="true">
                <path d="M3.5 8.5 6.5 11.5 12.5 4.5" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
              <svg v-else width="15" height="15" viewBox="0 0 16 16" aria-hidden="true">
                <path d="M5.8 3.2h4.4A1.8 1.8 0 0 1 12 5v5.2A1.8 1.8 0 0 1 10.2 12H5.8A1.8 1.8 0 0 1 4 10.2V5a1.8 1.8 0 0 1 1.8-1.8Z" fill="none" stroke="currentColor" stroke-width="1.25" stroke-linejoin="round" />
                <path d="M3.3 9.6A1.7 1.7 0 0 1 2 7.9V4.2a1.8 1.8 0 0 1 1.8-1.8h3.4" fill="none" stroke="currentColor" stroke-width="1.25" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </button>
            <button
              :class="['message-action-button', 'feedback-up', { active: message.feedback === 'up' }]"
              type="button"
              aria-label="点赞"
              title="点赞"
              @click="toggleMessageFeedback(message, 'up')"
            >
              <svg v-if="message.feedback === 'up'" width="15" height="15" viewBox="0 0 16 16" aria-hidden="true">
                <path d="M5.9 7.1V5.2c0-1 .4-1.9 1.1-2.6l1-1 .9.8c.4.4.5 1 .4 1.5L8.8 7h3c.9 0 1.6.8 1.4 1.7l-.6 3A1.6 1.6 0 0 1 11 13H5.9Z" fill="currentColor" />
                <path d="M2.8 7h3.1v6H2.8z" fill="currentColor" />
              </svg>
              <svg v-else width="15" height="15" viewBox="0 0 16 16" aria-hidden="true">
                <path d="M5.9 7.1V5.2c0-1 .4-1.9 1.1-2.6l1-1 .9.8c.4.4.5 1 .4 1.5L8.8 7h3c.9 0 1.6.8 1.4 1.7l-.6 3A1.6 1.6 0 0 1 11 13H5.9" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round" />
                <path d="M2.8 7h3.1v6H2.8z" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linejoin="round" />
              </svg>
            </button>
            <button
              :class="['message-action-button', 'feedback-down', { active: message.feedback === 'down' }]"
              type="button"
              aria-label="点踩"
              title="点踩"
              @click="toggleMessageFeedback(message, 'down')"
            >
              <svg v-if="message.feedback === 'down'" width="15" height="15" viewBox="0 0 16 16" aria-hidden="true">
                <path d="M10.1 8.9v1.9c0 1-.4 1.9-1.1 2.6l-1 1-.9-.8c-.4-.4-.5-1-.4-1.5L7.2 9h-3c-.9 0-1.6-.8-1.4-1.7l.6-3A1.6 1.6 0 0 1 5 3h5.1Z" fill="currentColor" />
                <path d="M10.1 3h3.1v6h-3.1z" fill="currentColor" />
              </svg>
              <svg v-else width="15" height="15" viewBox="0 0 16 16" aria-hidden="true">
                <path d="M10.1 8.9v1.9c0 1-.4 1.9-1.1 2.6l-1 1-.9-.8c-.4-.4-.5-1-.4-1.5L7.2 9h-3c-.9 0-1.6-.8-1.4-1.7l.6-3A1.6 1.6 0 0 1 5 3h5.1" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round" />
                <path d="M10.1 3h3.1v6h-3.1z" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linejoin="round" />
              </svg>
            </button>
          </div>
          <time>{{ formatTime(message.createdAt) }}</time>
        </div>
      </article>

      <div ref="messageTailRef" class="message-tail" aria-hidden="true"></div>
    </section>

    <form ref="composerRef" class="composer" @submit.prevent="handleComposerAction">
      <div v-if="imagePreview" class="composer-preview">
        <img :src="imagePreview" alt="待发送图片" />
        <div class="composer-preview-copy">
          <strong>{{ selectedImage?.name || '已选择图片' }}</strong>
          <span>将按水洗标或材质图片进行分析</span>
        </div>
        <button class="remove-button" type="button" aria-label="移除图片" @click="clearSelectedImage">×</button>
      </div>

      <div class="composer-row">
        <div class="input-shell">
          <label class="plus-button" aria-label="上传图片">
            <input type="file" accept="image/*" :disabled="isSending" @change="handleFileSelect" />
            <span></span>
            <span></span>
          </label>
          <textarea
            ref="textareaRef"
            v-model.trim="userInput"
            rows="1"
            :disabled="isSending"
            placeholder="问点什么，或上传一张水洗标图片"
            @input="syncTextareaHeight"
            @keydown.enter.exact.prevent="handleComposerAction"
          ></textarea>
        </div>

        <button
          class="send-button"
          :class="{ interrupt: isAbortable }"
          type="submit"
          :disabled="isSendButtonDisabled"
          :aria-label="isAbortable ? '停止生成' : '发送消息'"
        >
          <span v-if="isAbortable" class="stop-icon" aria-hidden="true"></span>
          <span v-else-if="isSending" class="send-loading" aria-hidden="true">
            <span></span>
            <span></span>
            <span></span>
          </span>
          <span v-else class="send-arrow"></span>
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { chatApi } from '../api/chat';
import { knowledgeApi } from '../api/knowledge';
import { weatherApi } from '../api/weather';
import { formatMessageHtml } from '../utils/messageFormat';

const chatPageRef = ref(null);
const chatHeadRef = ref(null);
const route = useRoute();
const router = useRouter();
const composerRef = ref(null);
const messageListRef = ref(null);
const messageTailRef = ref(null);
const textareaRef = ref(null);

const conversations = ref([]);
const messages = ref([]);
const currentSessionId = ref(localStorage.getItem('chatSessionId') || '');
const currentConversationLabel = ref('新的对话');
const userInput = ref('');
const isSending = ref(false);
const isConversationLoading = ref(false);
const historyOpen = ref(false);
const cachedWeather = ref(weatherApi.getCachedWeather());
const selectedImage = ref(null);
const imagePreview = ref('');
const copiedMessageId = ref('');
const shouldFollowLatest = ref(true);
const currentStreamController = ref(null);
const activeAssistantMessage = ref(null);
const isConsumingPendingPrompt = ref(false);
const isChatReady = ref(false);

let copiedMessageTimer = null;
let layoutResizeObserver = null;

const latestFollowThreshold = 72;
const fallbackImage =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="320" height="420"><rect width="100%" height="100%" fill="%23e6dccd"/><text x="50%" y="50%" text-anchor="middle" fill="%238b7a63" font-size="22">LOOK</text></svg>';

const quickPrompts = [
  '今天 23 度，帮我搭一套轻松一点的出门穿搭。',
  '这件灰蓝衬衫适合搭什么下装？',
  '我想要偏极简、干净一点的穿法。',
  '这个天气适合买针织还是衬衫？'
];

const materialKnowledgeCatalog = [
  { material: '羊毛', aliases: ['羊毛', 'wool', '毛呢', '羊绒'] },
  { material: '纯棉', aliases: ['纯棉', '全棉', '棉', 'cotton'] },
  { material: '亚麻', aliases: ['亚麻', 'linen', '麻料'] },
  { material: '牛仔布', aliases: ['牛仔布', '牛仔', 'denim'] },
  { material: '聚酯纤维', aliases: ['聚酯纤维', '聚酯', '涤纶', 'polyester'] },
  { material: '真丝', aliases: ['真丝', '桑蚕丝', '丝绸', 'silk'] },
  { material: '粘胶', aliases: ['粘胶', '粘纤', 'viscose', 'rayon'] }
];

const materialKnowledgeKeywords = [
  '养护', '保养', '护理', '清洗', '怎么洗', '能洗吗', '洗护', '收纳', '熨烫',
  '面料', '材质', '特点', '优点', '缺点', '适合什么季节', '适合什么场景',
  '区别', '差别', '不同', '对比', '哪个好', '怎么买', '值不值得买'
];

const currentLocation = computed(() => cachedWeather.value?.location || '');
const isAbortable = computed(() => Boolean(currentStreamController.value));
const isSendButtonDisabled = computed(() => {
  if (isAbortable.value) {
    return false;
  }
  if (isSending.value) {
    return true;
  }
  return !userInput.value.trim() && !selectedImage.value;
});

const syncLayoutMetrics = () => {
  const page = chatPageRef.value;
  if (!page) {
    return;
  }

  const headerHeight = Math.ceil(chatHeadRef.value?.offsetHeight || 0);
  const composerHeight = Math.ceil(composerRef.value?.offsetHeight || 0);
  page.style.setProperty('--chat-header-height', `${headerHeight}px`);
  page.style.setProperty('--chat-composer-height', `${composerHeight}px`);
};

const syncTextareaHeight = async () => {
  await nextTick();
  const element = textareaRef.value;
  if (!element) {
    return;
  }

  element.style.height = '24px';
  element.style.height = `${Math.min(element.scrollHeight, 112)}px`;
  syncLayoutMetrics();
};

const isNearLatest = () => {
  const list = messageListRef.value;
  if (!list) {
    return true;
  }

  return list.scrollHeight - list.scrollTop - list.clientHeight <= latestFollowThreshold;
};

const alignToLatestMessage = async ({ force = false, behavior = 'auto' } = {}) => {
  await nextTick();
  syncLayoutMetrics();

  window.requestAnimationFrame(() => {
    const list = messageListRef.value;
    const tail = messageTailRef.value;
    if (!list || !tail) {
      return;
    }

    if (!force && !shouldFollowLatest.value) {
      return;
    }

    tail.scrollIntoView({
      block: 'end',
      behavior
    });
  });
};

const handleMessageListScroll = () => {
  shouldFollowLatest.value = isNearLatest();
};

const sortConversations = (items = []) =>
  [...items].sort((left, right) => {
    const rightTime = new Date(right.updatedAt || right.createdAt || 0).getTime();
    const leftTime = new Date(left.updatedAt || left.createdAt || 0).getTime();
    return rightTime - leftTime;
  });

const syncConversationLabel = () => {
  const currentConversation = conversations.value.find((item) => item.sessionId === currentSessionId.value);
  currentConversationLabel.value = currentConversation?.title || '新的对话';
};

const revokePreviewUrl = () => {
  if (imagePreview.value?.startsWith('blob:')) {
    URL.revokeObjectURL(imagePreview.value);
  }
};

const clearSelectedImage = () => {
  revokePreviewUrl();
  selectedImage.value = null;
  imagePreview.value = '';
};

const handleFileSelect = (event) => {
  const [file] = event.target.files || [];
  event.target.value = '';
  clearSelectedImage();

  if (!file) {
    return;
  }

  selectedImage.value = file;
  imagePreview.value = URL.createObjectURL(file);
};

const decorateAssistantMessage = (message = {}) => ({
  ...message,
  feedback: message.feedback || '',
  isStreaming: Boolean(message.isStreaming)
});

const normalizeKnowledgeMessage = (payload = {}) => {
  const matchedFabricTags = Array.isArray(payload.matchedFabrics)
    ? payload.matchedFabrics.map((item) => item.name).filter(Boolean)
    : [];
  const matchedCareTags = Array.isArray(payload.matchedCareLabels)
    ? payload.matchedCareLabels.map((item) => item.symbolName || item.instruction).filter(Boolean)
    : [];

  return decorateAssistantMessage({
    id: `assistant-${Date.now()}`,
    role: 'assistant',
    content: payload.answer || '我暂时没能从知识库里整理出可用答案。',
    analysisTags: [...new Set([...matchedFabricTags, ...matchedCareTags])].slice(0, 8),
    recommendations: [],
    createdAt: new Date().toISOString()
  });
};

const buildAnalysisMessage = (responseData = {}) => {
  const analysis = responseData.analysis || {};
  const materialTags = Array.isArray(analysis.materials)
    ? analysis.materials
        .map((item) => (item.ratio ? `${item.name || item.rawText} ${item.ratio}` : item.name || item.rawText))
        .filter(Boolean)
    : [];
  const careTags = Array.isArray(analysis.careLabels)
    ? analysis.careLabels.map((item) => item.symbolName || item.instruction || item.rawText).filter(Boolean)
    : [];

  return decorateAssistantMessage({
    id: `assistant-${Date.now()}`,
    role: 'assistant',
    content: responseData.answer || analysis.summary || '我已经看完这张图片了。',
    analysisTags: [...materialTags, ...careTags].slice(0, 8),
    analysisText: analysis.detectedText || '',
    recommendations: [],
    createdAt: new Date().toISOString()
  });
};

const createStreamingAssistantMessage = () =>
  decorateAssistantMessage({
    id: `assistant-stream-${Date.now()}`,
    role: 'assistant',
    content: '',
    recommendations: [],
    createdAt: new Date().toISOString(),
    isStreaming: true
  });

const detectMaterialKnowledgeQuestion = (text) => {
  const normalized = String(text || '').trim().toLowerCase();
  if (!normalized) {
    return null;
  }

  const matchedEntry = materialKnowledgeCatalog.find((entry) =>
    entry.aliases.some((alias) => normalized.includes(alias.toLowerCase()))
  );

  if (!matchedEntry) {
    return null;
  }

  return materialKnowledgeKeywords.some((keyword) => normalized.includes(keyword.toLowerCase()))
    ? matchedEntry.material
    : null;
};

const isAssistantActionVisible = (message = {}) =>
  message.role === 'assistant' &&
  !message.isStreaming &&
  Boolean(String(message.content || '').trim());

const copyMessageContent = async (message = {}) => {
  if (!isAssistantActionVisible(message) || !navigator.clipboard) {
    return;
  }

  try {
    await navigator.clipboard.writeText(message.content);
    copiedMessageId.value = message.id;
    if (copiedMessageTimer) {
      window.clearTimeout(copiedMessageTimer);
    }
    copiedMessageTimer = window.setTimeout(() => {
      copiedMessageId.value = '';
      copiedMessageTimer = null;
    }, 1600);
  } catch (error) {
    console.error('Copy message failed:', error);
    alert('复制失败，请检查浏览器权限。');
  }
};

const toggleMessageFeedback = (message = {}, feedback) => {
  if (!isAssistantActionVisible(message)) {
    return;
  }

  message.feedback = message.feedback === feedback ? '' : feedback;
  messages.value = [...messages.value];
};

const finishStreamingMessage = (message, fallbackText = '') => {
  if (!message) {
    return;
  }

  message.isStreaming = false;
  if (!String(message.content || '').trim() && fallbackText) {
    message.content = fallbackText;
  }
  messages.value = [...messages.value];
};

const abortCurrentStream = async () => {
  const controller = currentStreamController.value;
  if (!controller) {
    return;
  }

  controller.abort();
  currentStreamController.value = null;
  isSending.value = false;
  finishStreamingMessage(activeAssistantMessage.value, '已停止生成');
  await syncTextareaHeight();
  await alignToLatestMessage({ behavior: 'auto' });
};

const loadMessages = async (sessionId) => {
  if (!sessionId) {
    messages.value = [];
    currentConversationLabel.value = '新的对话';
    return;
  }

  try {
    const response = await chatApi.getConversationMessages(sessionId);
    messages.value = (response.data || []).map((item) =>
      item.role === 'assistant' ? decorateAssistantMessage(item) : item
    );
    syncConversationLabel();
    shouldFollowLatest.value = true;
    await alignToLatestMessage({ force: true, behavior: 'auto' });
  } catch (error) {
    console.error('Load chat messages failed:', error);
    alert(error.response?.data?.message || '读取消息失败');
  }
};

const loadConversations = async (preferredSessionId = currentSessionId.value) => {
  isConversationLoading.value = true;
  try {
    const response = await chatApi.getConversations(1, 20);
    conversations.value = sortConversations(response.data?.items || []);

    const hasPreferred =
      preferredSessionId && conversations.value.some((item) => item.sessionId === preferredSessionId);
    const hasCurrent =
      currentSessionId.value && conversations.value.some((item) => item.sessionId === currentSessionId.value);

    const targetSessionId = hasPreferred
      ? preferredSessionId
      : hasCurrent
        ? currentSessionId.value
        : conversations.value[0]?.sessionId || '';

    currentSessionId.value = targetSessionId;
    if (targetSessionId) {
      localStorage.setItem('chatSessionId', targetSessionId);
    } else {
      chatApi.clearCurrentSession();
    }

    syncConversationLabel();
  } catch (error) {
    console.error('Load conversations failed:', error);
    alert(error.response?.data?.message || '读取会话失败');
  } finally {
    isConversationLoading.value = false;
  }
};

const submitMaterialKnowledgeQuestionStream = async (text, material) => {
  const optimisticMessage = {
    id: `local-knowledge-${Date.now()}`,
    role: 'user',
    content: text,
    recommendations: [],
    createdAt: new Date().toISOString()
  };

  const assistantMessage = createStreamingAssistantMessage();
  const controller = new AbortController();

  messages.value = [...messages.value, optimisticMessage, assistantMessage];
  userInput.value = '';
  isSending.value = true;
  currentStreamController.value = controller;
  activeAssistantMessage.value = assistantMessage;
  shouldFollowLatest.value = true;

  await syncTextareaHeight();
  await alignToLatestMessage({ force: true, behavior: 'auto' });

  let aborted = false;

  try {
    await knowledgeApi.askMaterialQuestionStream(
      {
        material,
        question: text
      },
      {
        onChunk: async (chunk) => {
          assistantMessage.content += chunk;
          assistantMessage.isStreaming = false;
          messages.value = [...messages.value];
          await alignToLatestMessage({ behavior: 'auto' });
        },
        onComplete: async () => {
          finishStreamingMessage(assistantMessage, '我暂时没能从知识库里整理出可用答案。');
          await alignToLatestMessage({ behavior: 'auto' });
        }
      },
      {
        signal: controller.signal
      }
    );
  } catch (error) {
    if (error?.name === 'AbortError') {
      aborted = true;
    } else {
      console.error('Ask material knowledge stream failed:', error);
      messages.value = messages.value.filter(
        (item) => item.id !== optimisticMessage.id && item.id !== assistantMessage.id
      );
      alert(error.response?.data?.message || error.message || '知识库问答失败');
    }
  } finally {
    if (currentStreamController.value === controller) {
      currentStreamController.value = null;
    }
    if (activeAssistantMessage.value === assistantMessage) {
      activeAssistantMessage.value = null;
    }
    isSending.value = false;
    if (!aborted) {
      finishStreamingMessage(assistantMessage, '我暂时没能从知识库里整理出可用答案。');
    }
    await syncTextareaHeight();
    await alignToLatestMessage({ behavior: 'auto' });
  }
};

const submitTextMessageStream = async (text) => {
  const material = detectMaterialKnowledgeQuestion(text);
  if (material) {
    await submitMaterialKnowledgeQuestionStream(text, material);
    return;
  }

  const optimisticMessage = {
    id: `local-${Date.now()}`,
    role: 'user',
    content: text,
    recommendations: [],
    createdAt: new Date().toISOString()
  };

  const assistantMessage = createStreamingAssistantMessage();
  const controller = new AbortController();

  messages.value = [...messages.value, optimisticMessage, assistantMessage];
  userInput.value = '';
  isSending.value = true;
  currentStreamController.value = controller;
  activeAssistantMessage.value = assistantMessage;
  shouldFollowLatest.value = true;

  await syncTextareaHeight();
  await alignToLatestMessage({ force: true, behavior: 'auto' });

  let nextSessionId = currentSessionId.value;
  let aborted = false;

  try {
    await chatApi.sendMessageStream(
      {
        message: text,
        sessionId: currentSessionId.value,
        location: currentLocation.value,
        context: 'general',
        metadata: cachedWeather.value
          ? {
              weatherSummary: cachedWeather.value.summary || '',
              weatherCondition: cachedWeather.value.weatherCondition || '',
              temperature: cachedWeather.value.temperature || ''
            }
          : undefined
      },
      {
        onSession: async (session) => {
          nextSessionId = session?.sessionId || nextSessionId || '';
          currentSessionId.value = nextSessionId;
          if (nextSessionId) {
            localStorage.setItem('chatSessionId', nextSessionId);
          }
        },
        onChunk: async (chunk) => {
          assistantMessage.content += chunk;
          assistantMessage.isStreaming = false;
          messages.value = [...messages.value];
          await alignToLatestMessage({ behavior: 'auto' });
        },
        onComplete: async () => {
          finishStreamingMessage(assistantMessage, '我整理好了。');
        }
      },
      {
        signal: controller.signal
      }
    );

    if (nextSessionId) {
      await loadConversations(nextSessionId);
      await loadMessages(nextSessionId);
    } else {
      await loadConversations('');
    }
  } catch (error) {
    if (error?.name === 'AbortError') {
      aborted = true;
    } else {
      console.error('Send chat stream failed:', error);
      messages.value = messages.value.filter(
        (item) => item.id !== optimisticMessage.id && item.id !== assistantMessage.id
      );
      alert(error.response?.data?.message || error.message || '发送失败');
    }
  } finally {
    if (currentStreamController.value === controller) {
      currentStreamController.value = null;
    }
    if (activeAssistantMessage.value === assistantMessage) {
      activeAssistantMessage.value = null;
    }
    isSending.value = false;
    if (!aborted) {
      finishStreamingMessage(assistantMessage, '我整理好了。');
    }
    await syncTextareaHeight();
    await alignToLatestMessage({ behavior: 'auto' });
  }
};

const submitImageAnalysis = async (text) => {
  if (!selectedImage.value) {
    return;
  }

  const preview = imagePreview.value;
  const optimisticMessage = {
    id: `local-image-${Date.now()}`,
    role: 'user',
    content: text || '请帮我分析这张水洗标图片。',
    imagePreview: preview,
    recommendations: [],
    createdAt: new Date().toISOString()
  };

  const image = selectedImage.value;
  const question = text || '请帮我分析这张水洗标图片。';

  messages.value = [...messages.value, optimisticMessage];
  userInput.value = '';
  selectedImage.value = null;
  imagePreview.value = '';
  isSending.value = true;
  shouldFollowLatest.value = true;

  await syncTextareaHeight();
  await alignToLatestMessage({ force: true, behavior: 'auto' });

  try {
    const response = await knowledgeApi.analyzeMaterialLabel({
      image,
      question
    });
    messages.value = [...messages.value, buildAnalysisMessage(response.data)];
  } catch (error) {
    console.error('Analyze material image failed:', error);
    alert(error.response?.data?.message || '图片分析失败');
  } finally {
    isSending.value = false;
    await syncTextareaHeight();
    await alignToLatestMessage({ behavior: 'auto' });
  }
};

const submitMessage = async () => {
  if (isSending.value) {
    return;
  }

  const text = userInput.value.trim();
  if (!text && !selectedImage.value) {
    return;
  }

  if (selectedImage.value) {
    await submitImageAnalysis(text);
    return;
  }

  await submitTextMessageStream(text);
};

const handleComposerAction = async () => {
  if (isAbortable.value) {
    await abortCurrentStream();
    return;
  }

  await submitMessage();
};

const sendQuickPrompt = async (promptText) => {
  if (isSending.value) {
    return;
  }

  userInput.value = promptText;
  await syncTextareaHeight();
  await submitMessage();
};

const selectConversation = async (sessionId) => {
  if (isAbortable.value) {
    await abortCurrentStream();
  }

  currentSessionId.value = sessionId;
  localStorage.setItem('chatSessionId', sessionId);
  historyOpen.value = false;
  syncConversationLabel();
  clearSelectedImage();
  await loadMessages(sessionId);
};

const startNewConversation = async () => {
  if (isAbortable.value) {
    await abortCurrentStream();
  }

  currentSessionId.value = '';
  currentConversationLabel.value = '新的对话';
  messages.value = [];
  userInput.value = '';
  historyOpen.value = false;
  localStorage.removeItem('chatSessionId');
  clearSelectedImage();
  chatApi.clearCurrentSession();
  await nextTick();
  await syncTextareaHeight();
  shouldFollowLatest.value = true;
  await alignToLatestMessage({ force: true, behavior: 'auto' });
};

const removeConversation = async (sessionId) => {
  if (!sessionId) {
    return;
  }

  if (!window.confirm('确认删除这条会话吗？')) {
    return;
  }

  try {
    if (currentSessionId.value === sessionId && isAbortable.value) {
      await abortCurrentStream();
    }

    await chatApi.deleteConversation(sessionId);
    if (currentSessionId.value === sessionId) {
      await startNewConversation();
    }
    await loadConversations(currentSessionId.value);
    if (currentSessionId.value) {
      await loadMessages(currentSessionId.value);
    }
  } catch (error) {
    console.error('Delete conversation failed:', error);
    alert(error.response?.data?.message || '删除会话失败');
  }
};

const resolveRecommendationImage = (item = {}) => item.imageUrl || item.image || item.coverUrl || fallbackImage;
const resolveRecommendationTitle = (item = {}) => item.name || item.title || item.category || '推荐搭配';
const resolveRecommendationSubtitle = (item = {}) => {
  const values = [item.category, item.primaryColor || item.color, item.brand].filter(Boolean);
  return values.join(' / ') || '由 Ecru 生成的搭配建议';
};

const formatTime = (value) => {
  if (!value) {
    return '--';
  }

  return new Date(value).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

const formatAssistantMessage = (content) => formatMessageHtml(content);

const normalizePendingPrompt = (value) => String(value || '').trim();

const clearRoutePromptQuery = async () => {
  if (!route.query.q) {
    return;
  }

  const nextQuery = { ...route.query };
  delete nextQuery.q;
  await router.replace({ path: route.path, query: nextQuery });
};

const consumePendingPrompt = async () => {
  if (route.path !== '/chat' || !isChatReady.value || isConsumingPendingPrompt.value) {
    return;
  }

  const routePrompt = normalizePendingPrompt(route.query.q);
  const storedPrompt = normalizePendingPrompt(localStorage.getItem('pendingChatPrompt'));
  const pendingPrompt = routePrompt || storedPrompt;
  if (!pendingPrompt) {
    return;
  }

  isConsumingPendingPrompt.value = true;
  try {
    localStorage.removeItem('pendingChatPrompt');
    await clearRoutePromptQuery();
    await startNewConversation();
    userInput.value = pendingPrompt;
    await syncTextareaHeight();
    await submitMessage();
  } finally {
    isConsumingPendingPrompt.value = false;
  }
};

watch(
  () => messages.value.length,
  async () => {
    await alignToLatestMessage({ behavior: 'auto' });
  }
);

watch(historyOpen, (open) => {
  document.body.classList.toggle('drawer-open', open);
});

watch(
  () => route.fullPath,
  async () => {
    await consumePendingPrompt();
  }
);

onMounted(async () => {
  await syncTextareaHeight();
  syncLayoutMetrics();

  if (typeof ResizeObserver !== 'undefined') {
    layoutResizeObserver = new ResizeObserver(() => {
      syncLayoutMetrics();
    });

    if (chatHeadRef.value) {
      layoutResizeObserver.observe(chatHeadRef.value);
    }
    if (composerRef.value) {
      layoutResizeObserver.observe(composerRef.value);
    }
  }

  await loadConversations(currentSessionId.value);
  if (currentSessionId.value) {
    await loadMessages(currentSessionId.value);
  }

  isChatReady.value = true;
  await consumePendingPrompt();
});

onBeforeUnmount(() => {
  layoutResizeObserver?.disconnect();
  document.body.classList.remove('drawer-open');
  currentStreamController.value?.abort();

  if (copiedMessageTimer) {
    window.clearTimeout(copiedMessageTimer);
    copiedMessageTimer = null;
  }

  clearSelectedImage();
});
</script>

<style scoped>
.chat-page {
  position: fixed;
  inset: 0 0 var(--app-bottom-offset) 0;
  --chat-top-gap: 10px;
  --chat-side-gap: 14px;
  --chat-bottom-gap: 12px;
  --chat-header-height: 52px;
  --chat-composer-height: 72px;
  overflow: hidden;
  padding: 10px 14px 12px;
  background: linear-gradient(180deg, var(--bg-base) 0%, var(--bg-soft) 100%);
}

.history-mask {
  position: fixed;
  inset: 0;
  z-index: 18;
  background: rgba(25, 20, 14, 0.14);
  backdrop-filter: blur(4px);
}

.history-panel {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 19;
  width: min(82vw, 320px);
  padding: 16px 14px 18px;
  border-right: 1px solid var(--line-soft);
  background: color-mix(in srgb, var(--surface-strong) 96%, transparent);
  box-shadow: var(--shadow-soft);
  transform: translateX(-102%);
  transition: transform 0.22s ease;
}

.history-panel.open {
  transform: translateX(0);
}

.history-head,
.chat-head,
.composer-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.history-head {
  gap: 12px;
  padding-bottom: 14px;
}

.history-caption,
.head-copy p,
.empty-caption {
  color: var(--text-faint);
  font-size: 10px;
  letter-spacing: 0.12em;
}

.history-head h2 {
  margin-top: 4px;
  font-size: 17px;
}

.history-state {
  margin-top: 18px;
  color: var(--text-soft);
  font-size: 12px;
}

.history-list {
  display: grid;
  gap: 8px;
}

.history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 11px;
  border-radius: 16px;
  background: color-mix(in srgb, var(--surface) 96%, transparent);
  border: 1px solid transparent;
  cursor: pointer;
}

.history-item.active {
  background: color-mix(in srgb, var(--accent-soft) 58%, var(--surface-strong));
  border-color: var(--line-strong);
}

.history-copy {
  min-width: 0;
}

.history-copy strong {
  display: block;
  color: var(--text-main);
  font-size: 12px;
}

.history-copy p,
.history-copy span {
  margin-top: 4px;
  color: var(--text-soft);
  font-size: 10px;
  line-height: 1.5;
}

.history-copy p {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.delete-button,
.remove-button,
.prompt-chip,
.message-action-button {
  border: none;
  background: transparent;
  cursor: pointer;
}

.delete-button {
  color: var(--text-faint);
  font-size: 10px;
}

.chat-head {
  gap: 10px;
  position: absolute;
  top: var(--chat-top-gap);
  left: var(--chat-side-gap);
  right: var(--chat-side-gap);
  z-index: 8;
  padding: 4px 0 10px;
  background: linear-gradient(180deg, color-mix(in srgb, var(--bg-base) 96%, transparent) 0%, rgba(0, 0, 0, 0) 100%);
  backdrop-filter: blur(10px);
}

.head-copy {
  flex: 1;
  min-width: 0;
}

.chat-head h1 {
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 15px;
  font-weight: 500;
}

.icon-button {
  display: inline-grid;
  gap: 5px;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  cursor: pointer;
}

.icon-button span {
  display: block;
  height: 1.5px;
  border-radius: 999px;
  background: var(--text-main);
}

.icon-button span:first-child {
  width: 16px;
}

.icon-button span:last-child {
  width: 12px;
}

.new-chat-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--line-strong);
  border-radius: 50%;
  background: transparent;
  color: var(--text-soft);
  cursor: pointer;
  flex-shrink: 0;
}

.message-list {
  position: absolute;
  top: calc(var(--chat-top-gap) + var(--chat-header-height));
  right: var(--chat-side-gap);
  bottom: calc(var(--chat-bottom-gap) + var(--chat-composer-height));
  left: var(--chat-side-gap);
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
  padding: 4px 0 18px;
  overscroll-behavior: contain;
  scroll-padding-top: 16px;
  scroll-padding-bottom: 24px;
}

.message-tail {
  width: 100%;
  height: 1px;
  flex: none;
}

.message-list.empty {
  justify-content: center;
}

.empty-state {
  padding: 0 4px 24px;
}

.empty-state h2 {
  margin-top: 8px;
  font-size: 17px;
  line-height: 1.5;
  font-weight: 500;
}

.prompt-list {
  display: grid;
  gap: 8px;
  margin-top: 18px;
}

.prompt-chip {
  padding: 12px 14px;
  border: 1px solid var(--line-soft);
  border-radius: 18px;
  background: color-mix(in srgb, var(--surface-strong) 90%, transparent);
  color: var(--text-main);
  text-align: left;
  font-size: 11px;
  line-height: 1.55;
}

.message-card {
  width: fit-content;
  max-width: 78%;
  padding: 10px 12px;
  border-radius: 18px;
  font-size: 11px;
}

.message-card.user {
  align-self: flex-end;
  background: var(--accent-strong);
  color: var(--surface-strong);
  border-bottom-right-radius: 8px;
}

.message-card.assistant {
  align-self: flex-start;
  border: 1px solid var(--line-soft);
  background: color-mix(in srgb, var(--surface-strong) 96%, transparent);
  color: var(--text-main);
  border-bottom-left-radius: 8px;
}

.image-preview {
  margin-bottom: 8px;
  border-radius: 14px;
  overflow: hidden;
}

.image-preview img {
  display: block;
  width: 132px;
  height: 132px;
  object-fit: cover;
}

.message-content,
.analysis-block {
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
}

.rich-content {
  white-space: normal;
}

.rich-content :deep(p) {
  margin: 0;
}

.rich-content :deep(p + p),
.rich-content :deep(p + ul),
.rich-content :deep(p + ol),
.rich-content :deep(ul + p),
.rich-content :deep(ol + p),
.rich-content :deep(ul + ul),
.rich-content :deep(ol + ol),
.rich-content :deep(ul + ol),
.rich-content :deep(ol + ul) {
  margin-top: 8px;
}

.rich-content :deep(ul),
.rich-content :deep(ol) {
  margin: 0;
  padding-left: 18px;
}

.rich-content :deep(li + li) {
  margin-top: 4px;
}

.rich-content :deep(code) {
  padding: 1px 5px;
  border-radius: 6px;
  background: rgba(93, 69, 35, 0.08);
  font-size: 0.95em;
}

.rich-content :deep(strong) {
  font-weight: 700;
}

.analysis-block {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--line-soft);
  color: var(--text-soft);
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.tag-row span {
  padding: 4px 8px;
  border-radius: 999px;
  background: var(--accent-soft);
  color: var(--accent-strong);
  font-size: 10px;
}

.message-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
}

.message-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.message-action-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: auto;
  height: auto;
  padding: 0;
  color: color-mix(in srgb, var(--text-soft) 88%, var(--text-faint));
  opacity: 0.82;
  transition: color 0.18s ease, opacity 0.18s ease, transform 0.18s ease;
}

.message-action-button:hover {
  color: var(--text-main);
  opacity: 1;
  transform: translateY(-1px);
}

.message-action-button svg {
  display: block;
}

.copy-action.copied {
  color: #2f6b57;
  opacity: 1;
}

.feedback-up.active {
  color: #2f6b57;
  opacity: 1;
}

.feedback-down.active {
  color: #8a4f3f;
  opacity: 1;
}

.message-card time {
  display: block;
  font-size: 10px;
  opacity: 0.62;
  margin-left: auto;
}

.inline-loading {
  margin-top: 2px;
}

.look-grid {
  display: grid;
  gap: 10px;
  margin-top: 10px;
}

.look-card {
  display: grid;
  grid-template-columns: 68px 1fr;
  gap: 10px;
  padding: 8px;
  border-radius: 14px;
  background: var(--surface-quiet);
}

.look-image {
  border-radius: 10px;
  overflow: hidden;
}

.look-image img {
  display: block;
  width: 100%;
  height: 100%;
  min-height: 76px;
  object-fit: cover;
}

.look-copy strong {
  display: block;
  font-size: 11px;
}

.look-copy p {
  margin-top: 5px;
  color: var(--text-soft);
  font-size: 10px;
  line-height: 1.5;
}

.composer {
  position: absolute;
  right: var(--chat-side-gap);
  bottom: var(--chat-bottom-gap);
  left: var(--chat-side-gap);
  z-index: 8;
  padding-top: 8px;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0) 0%, color-mix(in srgb, var(--bg-base) 96%, transparent) 18%);
  backdrop-filter: blur(10px);
}

.composer-preview {
  display: grid;
  grid-template-columns: 52px 1fr auto;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
  padding: 10px 12px;
  border: 1px solid var(--line-soft);
  border-radius: 16px;
  background: color-mix(in srgb, var(--surface-strong) 95%, transparent);
}

.composer-preview img {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  object-fit: cover;
}

.composer-preview-copy strong {
  display: block;
  color: var(--text-main);
  font-size: 11px;
}

.composer-preview-copy span {
  display: block;
  margin-top: 3px;
  color: var(--text-faint);
  font-size: 10px;
}

.composer-row {
  gap: 10px;
  align-items: flex-end;
}

.input-shell {
  flex: 1;
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 10px;
  align-items: end;
  min-height: 44px;
  padding: 8px 10px;
  border: 1px solid var(--line-soft);
  border-radius: 18px;
  background: color-mix(in srgb, var(--surface-strong) 95%, transparent);
}

.plus-button {
  position: relative;
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  margin-bottom: 1px;
  border-radius: 50%;
  background: var(--surface-quiet);
  cursor: pointer;
}

.plus-button span {
  position: absolute;
  width: 12px;
  height: 1.5px;
  border-radius: 999px;
  background: var(--text-faint);
}

.plus-button span:last-child {
  transform: rotate(90deg);
}

.plus-button input {
  display: none;
}

.composer textarea {
  width: 100%;
  height: 24px;
  min-height: 24px;
  max-height: 112px;
  border: none;
  background: transparent;
  resize: none;
  color: var(--text-main);
  font-size: 12px;
  line-height: 1.7;
  outline: none;
}

.remove-button {
  color: var(--text-faint);
  font-size: 18px;
}

.send-button {
  flex: none;
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 50%;
  background: var(--accent-strong);
  color: var(--surface-strong);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.send-button.interrupt {
  background: #9a4d3a;
}

.send-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.send-arrow {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-top: 1.6px solid currentColor;
  border-right: 1.6px solid currentColor;
  transform: rotate(45deg);
}

.stop-icon {
  width: 12px;
  height: 12px;
  border-radius: 3px;
  background: currentColor;
}

.send-loading {
  display: inline-flex;
  gap: 3px;
}

.send-loading span,
.loading-dots span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--accent);
  animation: pulse 1.1s infinite ease-in-out;
}

.send-loading span {
  width: 5px;
  height: 5px;
  background: currentColor;
}

.loading-dots {
  display: flex;
  gap: 6px;
  padding: 4px 2px;
}

.send-loading span:nth-child(2),
.loading-dots span:nth-child(2) {
  animation-delay: 0.16s;
}

.send-loading span:nth-child(3),
.loading-dots span:nth-child(3) {
  animation-delay: 0.32s;
}

@keyframes pulse {
  0%,
  80%,
  100% {
    transform: scale(0.84);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

@media (min-width: 768px) {
  .chat-page {
    inset: 0;
    --chat-top-gap: 16px;
    --chat-side-gap: 22px;
    --chat-bottom-gap: 18px;
    padding: 16px 22px 18px;
  }

  .chat-head h1 {
    font-size: 16px;
  }

  .message-card {
    max-width: min(620px, 72%);
  }
}
</style>
