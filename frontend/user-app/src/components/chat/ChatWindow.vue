<template>
  <div class="chat-window">
    <div class="chat-header">
      <h2>AI 聊天</h2>
      <button class="clear-btn" @click="clearChat">清除记录</button>
    </div>
    <div class="chat-messages" ref="messagesContainer">
      <div 
        v-for="(message, index) in messages" 
        :key="index"
        :class="['message', message.type]"
      >
        <div class="message-content">
          {{ message.content }}
        </div>
        <div class="message-time">{{ message.timestamp }}</div>
      </div>
      <div v-if="isLoading" class="message loading">
        <div class="message-content">
          <div class="loading-indicator">
            <span class="dot"></span>
            <span class="dot"></span>
            <span class="dot"></span>
          </div>
        </div>
      </div>
    </div>
    <div class="chat-input">
      <input 
        v-model="inputMessage" 
        type="text" 
        placeholder="输入消息..."
        @keyup.enter="sendMessage"
      />
      <button @click="sendMessage" :disabled="isLoading">
        发送
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { chatApi } from '../../api/chat';

// 从 localStorage 加载历史聊天记录
const loadChatHistory = () => {
  const history = localStorage.getItem('chatHistory');
  if (history) {
    return JSON.parse(history);
  }
  return [
    {
      type: 'ai',
      content: '你好！我是AI助手，有什么可以帮助你的吗？',
      timestamp: new Date().toLocaleTimeString()
    }
  ];
};

// 保存聊天记录到 localStorage
const saveChatHistory = (messages) => {
  localStorage.setItem('chatHistory', JSON.stringify(messages));
};

const messages = ref(loadChatHistory());
const inputMessage = ref('');
const isLoading = ref(false);
const messagesContainer = ref(null);

const sendMessage = async () => {
  if (!inputMessage.value.trim() || isLoading.value) return;
  
  const userMessage = inputMessage.value.trim();
  const timestamp = new Date().toLocaleTimeString();
  
  messages.value.push({
    type: 'user',
    content: userMessage,
    timestamp
  });
  
  saveChatHistory(messages.value);
  inputMessage.value = '';
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
    scrollToBottom();
  }
};

const clearChat = () => {
  if (confirm('确定要清除聊天记录吗？')) {
    messages.value = [
      {
        type: 'ai',
        content: '你好！我是AI助手，有什么可以帮助你的吗？',
        timestamp: new Date().toLocaleTimeString()
      }
    ];
    saveChatHistory(messages.value);
    scrollToBottom();
  }
};

const scrollToBottom = () => {
  setTimeout(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
    }
  }, 100);
};

watch(messages, () => {
  scrollToBottom();
}, { deep: true });

onMounted(() => {
  scrollToBottom();
});
</script>

<style scoped>
.chat-window {
  display: flex;
  flex-direction: column;
  height: 100vh;
  max-width: 800px;
  margin: 0 auto;
  border: 1px solid #e8d5a2;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  background-color: #fdfaf5;
}

.chat-header {
  background-color: #f5e6c3;
  color: #8b7355;
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e8d5a2;
}

.chat-header h2 {
  margin: 0;
  font-size: 18px;
}

.clear-btn {
  background-color: rgba(139, 115, 85, 0.2);
  color: #8b7355;
  border: 1px solid rgba(139, 115, 85, 0.3);
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.clear-btn:active {
  background-color: rgba(139, 115, 85, 0.3);
}

.chat-messages {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #f9f3e6;
  display: flex;
  flex-direction: column;
}

.message {
  margin-bottom: 16px;
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
  margin-left: auto;
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

.chat-input {
  display: flex;
  padding: 16px;
  background-color: #fdfaf5;
  border-top: 1px solid #e8d5a2;
}

.chat-input input {
  flex: 1;
  padding: 12px;
  border: 1px solid #e8d5a2;
  border-radius: 20px;
  margin-right: 10px;
  font-size: 14px;
  background-color: #fcf9f0;
}

.chat-input button {
  padding: 12px 24px;
  background-color: #8b7355;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.chat-input button:hover:not(:disabled) {
  background-color: #6d573b;
}

.chat-input button:disabled {
  background-color: #d5c18a;
  cursor: not-allowed;
}

/* 滚动条样式 */
.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-track {
  background: #f5e6c3;
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #d5c18a;
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
  background: #a68d6a;
}
</style>
