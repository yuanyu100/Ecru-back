<template>
  <div class="chat-view" :class="{ 'has-messages': hasChatMessages }">
    <!-- 左上角汉堡菜单 -->
    <div class="menu-icon" @click="toggleMenu">
      <div class="menu-line"></div>
      <div class="menu-line"></div>
      <div class="menu-line"></div>
    </div>

    <!-- 左侧抽屉菜单 -->
    <div class="drawer" :class="{ open: isMenuOpen }">
      <div class="drawer-content">
        <div class="drawer-header">
          <h3>菜单</h3>
          <div class="close-icon" @click="toggleMenu">×</div>
        </div>
        <div class="drawer-menu">
          <div class="menu-item" @click="navigateTo('/')">
            <span class="menu-icon-text">🏠</span>
            <span>首页</span>
          </div>
          <div class="menu-item" @click="navigateTo('/wardrobe')">
            <span class="menu-icon-text">👕</span>
            <span>我的衣橱</span>
          </div>
          <div class="menu-item" @click="navigateTo('/profile')">
            <span class="menu-icon-text">⚙️</span>
            <span>设置</span>
          </div>
          <div class="menu-item" @click="toggleTheme">
            <span class="menu-icon-text">🎨</span>
            <span>切换主题</span>
          </div>
          <div class="menu-item" @click="clearChat">
            <span class="menu-icon-text">🗑️</span>
            <span>清除聊天记录</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 遮罩层 -->
    <div class="overlay" v-if="isMenuOpen" @click="toggleMenu"></div>

    <!-- 聊天消息区域 -->
    <div class="chat-messages" v-if="hasChatMessages" ref="chatMessagesRef">
      <div
        v-for="(message, index) in messages"
        :key="index"
        :class="['message', message.type]"
      >
        <div class="message-content">{{ message.content }}</div>
        <div class="message-time">{{ message.timestamp }}</div>
      </div>
      <div v-if="isLoading" class="message loading">
        <div class="loading-indicator">
          <span class="dot"></span>
          <span class="dot"></span>
          <span class="dot"></span>
        </div>
      </div>
    </div>

    <!-- 推荐流区域 -->
    <div class="recommendation-flow" :class="{ expanded: isFlowExpanded }" v-if="!hasChatMessages">
      <div class="flow-header" @click="toggleFlow">
        <span class="flow-title">今日精选</span>
        <span class="flow-arrow" :class="{ rotated: isFlowExpanded }">⌄</span>
      </div>
      <div class="flow-content">
        <div
          v-for="(item, index) in recommendations"
          :key="index"
          class="recommendation-card"
          @dblclick="likeItem(index)"
          @mousedown="startDislikeTimer(index)"
          @mouseup="cancelDislikeTimer"
          @mouseleave="cancelDislikeTimer"
        >
          <div class="card-image">
            <img :src="item.image" :alt="item.title" />
            <div class="like-overlay" v-if="item.liked">
              <span class="like-icon">❤️</span>
            </div>
          </div>
          <div class="card-title">{{ item.title }}</div>
        </div>
      </div>
    </div>

    <!-- 核心交互区 -->
    <div class="prompt-area">
      <div class="prompt-text" v-if="!hasUserInput && !hasChatMessages">
        <span class="typing-text">今天想穿成什么样。</span>
      </div>
      <div class="input-area">
        <input
          v-model="userInput"
          type="text"
          placeholder=""
          class="chat-input"
          @keyup.enter="sendMessage"
        />
        <div class="input-line"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { chatApi } from '../api/chat';

const router = useRouter();
const isMenuOpen = ref(false);
const isFlowExpanded = ref(false);
const userInput = ref('');
const isLoading = ref(false);
const hasUserInput = ref(false);
const dislikeTimer = ref(null);
const currentDislikeIndex = ref(-1);
const chatMessagesRef = ref(null);

// 从 localStorage 加载历史聊天记录
const loadChatHistory = () => {
  const history = localStorage.getItem('chatHistory');
  if (history) {
    return JSON.parse(history);
  }
  return [];
};

// 保存聊天记录到 localStorage
const saveChatHistory = (messages) => {
  localStorage.setItem('chatHistory', JSON.stringify(messages));
};

const messages = ref(loadChatHistory());

// 推荐数据
const recommendations = ref([
  {
    id: 1,
    title: '春日休闲风',
    image: 'https://via.placeholder.com/300x400?text=Spring+Casual',
    liked: false
  },
  {
    id: 2,
    title: '职场商务风',
    image: 'https://via.placeholder.com/300x400?text=Business+Style',
    liked: false
  }
]);

// 计算属性
const hasChatMessages = computed(() => {
  return messages.value.length > 0;
});

// 切换菜单
const toggleMenu = () => {
  isMenuOpen.value = !isMenuOpen.value;
};

// 切换推荐流
const toggleFlow = () => {
  isFlowExpanded.value = !isFlowExpanded.value;
};

// 导航到其他页面
const navigateTo = (path) => {
  router.push(path);
  isMenuOpen.value = false;
};

// 切换主题
const toggleTheme = () => {
  // 这里可以实现主题切换逻辑
  alert('主题切换功能开发中');
};

// 清除聊天记录
const clearChat = () => {
  if (confirm('确定要清除聊天记录吗？')) {
    messages.value = [];
    saveChatHistory(messages.value);
    hasUserInput.value = false;
    isFlowExpanded.value = false;
  }
};

// 自动滚动到聊天消息底部
const scrollToBottom = () => {
  setTimeout(() => {
    if (chatMessagesRef.value) {
      chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight;
    }
  }, 100);
};

// 发送消息
const sendMessage = async () => {
  if (!userInput.value.trim() || isLoading.value) return;
  
  const userMessage = userInput.value.trim();
  const timestamp = new Date().toLocaleTimeString();
  
  messages.value.push({
    type: 'user',
    content: userMessage,
    timestamp
  });
  
  saveChatHistory(messages.value);
  userInput.value = '';
  hasUserInput.value = true;
  isFlowExpanded.value = true;
  
  // 滚动到底部
  scrollToBottom();
  
  isLoading.value = true;
  try {
    const response = await chatApi.sendMessage(userMessage);
    messages.value.push({
      type: 'ai',
      content: response.data || '抱歉，我无法理解你的问题。',
      timestamp: new Date().toLocaleTimeString()
    });
  } catch (error) {
    messages.value.push({
      type: 'ai',
      content: '抱歉，服务暂时不可用，请稍后再试。',
      timestamp: new Date().toLocaleTimeString()
    });
  } finally {
    isLoading.value = false;
    saveChatHistory(messages.value);
    // 滚动到底部
    scrollToBottom();
  }
};

// 喜欢推荐
const likeItem = (index) => {
  recommendations.value[index].liked = true;
  // 这里可以添加收藏逻辑
};

// 开始不喜欢计时
const startDislikeTimer = (index) => {
  currentDislikeIndex.value = index;
  dislikeTimer.value = setTimeout(() => {
    // 移除当前推荐并添加新推荐
    recommendations.value.splice(index, 1);
    recommendations.value.push({
      id: Date.now(),
      title: '新推荐',
      image: `https://via.placeholder.com/300x400?text=New+Recommendation+${Date.now()}`,
      liked: false
    });
  }, 1000);
};

// 取消不喜欢计时
const cancelDislikeTimer = () => {
  if (dislikeTimer.value) {
    clearTimeout(dislikeTimer.value);
    dislikeTimer.value = null;
    currentDislikeIndex.value = -1;
  }
};

onMounted(() => {
  // 初始化
});
</script>

<style scoped>
.chat-view {
  min-height: 100vh;
  background-color: #f9f3e6;
  position: relative;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.chat-view.has-messages {
  justify-content: flex-end;
  padding-bottom: 40px;
}

/* 菜单图标 */
.menu-icon {
  position: absolute;
  top: 20px;
  left: 20px;
  cursor: pointer;
  z-index: 100;
}

.menu-line {
  width: 24px;
  height: 2px;
  background-color: #8b7355;
  margin-bottom: 6px;
  transition: all 0.3s ease;
}

/* 左侧抽屉菜单 */
.drawer {
  position: fixed;
  top: 0;
  left: -300px;
  width: 250px;
  height: 100vh;
  background-color: rgba(253, 250, 245, 0.95);
  backdrop-filter: blur(10px);
  border-right: 1px solid #e8d5a2;
  transition: left 0.3s ease;
  z-index: 200;
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
}

.drawer.open {
  left: 0;
}

.drawer-content {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e8d5a2;
}

.drawer-header h3 {
  color: #8b7355;
  margin: 0;
  font-size: 18px;
}

.close-icon {
  font-size: 24px;
  color: #8b7355;
  cursor: pointer;
}

.drawer-menu {
  flex: 1;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #6d573b;
}

.menu-item:hover {
  color: #8b7355;
  padding-left: 10px;
}

.menu-icon-text {
  font-size: 18px;
  margin-right: 12px;
  width: 24px;
}

/* 遮罩层 */
.overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.3);
  z-index: 150;
}

/* 核心交互区 */
.prompt-area {
  text-align: center;
  max-width: 600px;
  width: 100%;
  transition: all 0.3s ease;
}

.chat-view.has-messages .prompt-area {
  margin-top: 20px;
}

.prompt-text {
  margin-bottom: 20px;
}

.typing-text {
  font-size: 24px;
  color: #8b7355;
  font-weight: 300;
  letter-spacing: 1px;
}

.input-area {
  position: relative;
  max-width: 400px;
  margin: 0 auto;
}

.chat-input {
  width: 100%;
  padding: 12px 0;
  border: none;
  background: transparent;
  font-size: 16px;
  color: #8b7355;
  outline: none;
  text-align: center;
}

.input-line {
  width: 100%;
  height: 1px;
  background-color: #e8d5a2;
  position: absolute;
  bottom: 0;
  left: 0;
  transition: all 0.3s ease;
}

.chat-input:focus + .input-line {
  background-color: #8b7355;
  height: 2px;
}

/* 推荐流区域 */
.recommendation-flow {
  max-width: 600px;
  width: 100%;
  margin-top: 20px;
  transition: all 0.3s ease;
}

.flow-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  cursor: pointer;
  margin-bottom: 20px;
}

.flow-title {
  font-size: 16px;
  color: #8b7355;
  font-weight: 500;
}

.flow-arrow {
  font-size: 20px;
  color: #8b7355;
  transition: transform 0.3s ease;
}

.flow-arrow.rotated {
  transform: rotate(180deg);
}

.flow-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 20px;
  max-height: 0;
  overflow: hidden;
  transition: max-height 0.3s ease;
}

.recommendation-flow.expanded .flow-content {
  max-height: 500px;
}

.recommendation-card {
  cursor: pointer;
  transition: all 0.3s ease;
}

.card-image {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.card-image img {
  width: 100%;
  height: 200px;
  object-fit: cover;
  transition: all 0.3s ease;
}

.recommendation-card:hover .card-image img {
  transform: scale(1.05);
}

.like-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: likeAnimation 0.5s ease;
}

@keyframes likeAnimation {
  0% {
    transform: scale(0);
    opacity: 0;
  }
  50% {
    transform: scale(1.2);
    opacity: 1;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.like-icon {
  font-size: 40px;
}

.card-title {
  margin-top: 10px;
  font-size: 14px;
  color: #6d573b;
  text-align: center;
}

/* 聊天消息区域 */
.chat-messages {
  max-width: 600px;
  width: 100%;
  max-height: 60vh;
  overflow-y: auto;
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  scroll-behavior: smooth;
}

.message {
  max-width: 80%;
  padding: 10px 14px;
  border-radius: 18px;
  animation: fadeIn 0.3s ease;
  position: relative;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message.user {
  align-self: flex-end;
  background-color: #8b7355;
  color: white;
  border-bottom-right-radius: 4px;
}

.message.ai {
  align-self: flex-start;
  background-color: #fdfaf5;
  color: #6d573b;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  border: 1px solid #e8d5a2;
}

.message.loading {
  align-self: flex-start;
  background-color: #fdfaf5;
  color: #6d573b;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  border: 1px solid #e8d5a2;
}

.message-time {
  font-size: 10px;
  opacity: 0.7;
  margin-top: 4px;
  text-align: right;
}

.message.user .message-time {
  color: rgba(255, 255, 255, 0.7);
}

.message.ai .message-time {
  color: rgba(109, 87, 59, 0.7);
  text-align: left;
}

.loading-indicator {
  display: flex;
  gap: 4px;
}

.dot {
  width: 8px;
  height: 8px;
  background-color: #8b7355;
  border-radius: 50%;
  animation: pulse 1.4s infinite ease-in-out both;
}

.dot:nth-child(1) {
  animation-delay: -0.32s;
}

.dot:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes pulse {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .chat-view {
    padding: 15px;
  }
  
  .typing-text {
    font-size: 20px;
  }
  
  .recommendation-flow {
    max-width: 100%;
  }
  
  .flow-content {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .card-image img {
    height: 150px;
  }
}
</style>