<template>
  <div class="admin-page">
    <section class="panel-card">
      <div class="panel-head stacked-head">
        <h2>AI 提示词</h2>
        <div class="toolbar">
          <button class="secondary-button" type="button" :disabled="loading || saving" @click="loadSettings">
            刷新
          </button>
          <button class="primary-button" type="button" :disabled="loading || saving" @click="saveSettings">
            {{ saving ? '保存中...' : '保存配置' }}
          </button>
        </div>
      </div>

      <p v-if="message.text" :class="['form-message', message.type]">{{ message.text }}</p>

      <div v-if="loading" class="empty-tip">加载中...</div>
      <div v-else class="prompt-grid">
        <label class="prompt-field">
          <span>系统提示词</span>
          <textarea
            v-model.trim="form.chatSystemPrompt"
            class="text-input textarea-input prompt-textarea prompt-textarea-lg"
            rows="16"
            placeholder="请输入主系统提示词"
          ></textarea>
        </label>

        <div class="prompt-side">
          <label class="prompt-field">
            <span>问候语快捷回复</span>
            <textarea
              v-model.trim="form.greetingReply"
              class="text-input textarea-input prompt-textarea"
              rows="6"
              placeholder="例如：你好，我在。"
            ></textarea>
          </label>

          <label class="prompt-field">
            <span>身份介绍快捷回复</span>
            <textarea
              v-model.trim="form.identityReply"
              class="text-input textarea-input prompt-textarea"
              rows="10"
              placeholder="例如：我是 Ecru。"
            ></textarea>
          </label>

          <label class="prompt-field">
            <span>会话标题生成提示词</span>
            <textarea
              v-model.trim="form.conversationTitlePrompt"
              class="text-input textarea-input prompt-textarea"
              rows="7"
              placeholder="例如：根据用户消息和 AI 回复生成简洁中文标题"
            ></textarea>
          </label>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { aiPromptApi } from '../../api/aiPrompt';

const loading = ref(false);
const saving = ref(false);
const form = reactive({
  chatSystemPrompt: '',
  greetingReply: '',
  identityReply: '',
  conversationTitlePrompt: ''
});

const message = reactive({
  text: '',
  type: ''
});

const setMessage = (text = '', type = '') => {
  message.text = text;
  message.type = type;
};

const applyPayload = (payload = {}) => {
  form.chatSystemPrompt = payload.chatSystemPrompt || '';
  form.greetingReply = payload.greetingReply || '';
  form.identityReply = payload.identityReply || '';
  form.conversationTitlePrompt = payload.conversationTitlePrompt || '';
};

const loadSettings = async () => {
  loading.value = true;
  setMessage();
  try {
    const result = await aiPromptApi.getChatSettings();
    if (result?.code === 200) {
      applyPayload(result.data);
      return;
    }
    setMessage(result?.message || '加载提示词配置失败。', 'error');
  } catch (error) {
    setMessage(error?.response?.data?.message || error?.message || '加载提示词配置失败。', 'error');
  } finally {
    loading.value = false;
  }
};

const saveSettings = async () => {
  saving.value = true;
  setMessage();
  try {
    const result = await aiPromptApi.updateChatSettings({
      chatSystemPrompt: form.chatSystemPrompt,
      greetingReply: form.greetingReply,
      identityReply: form.identityReply,
      conversationTitlePrompt: form.conversationTitlePrompt
    });

    if (result?.code === 200) {
      applyPayload(result.data);
      setMessage('提示词配置已保存并入库。', 'success');
      return;
    }

    setMessage(result?.message || '保存提示词配置失败。', 'error');
  } catch (error) {
    setMessage(error?.response?.data?.message || error?.message || '保存提示词配置失败。', 'error');
  } finally {
    saving.value = false;
  }
};

onMounted(loadSettings);
</script>

<style scoped>
.stacked-head {
  align-items: flex-start;
}

.prompt-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.9fr);
}

.prompt-side {
  display: grid;
  gap: 16px;
}

.prompt-field {
  display: grid;
  gap: 8px;
}

.prompt-field span {
  font-weight: 600;
}

.prompt-textarea {
  min-height: 160px;
}

.prompt-textarea-lg {
  min-height: 520px;
}

.form-message {
  margin: 0 0 16px;
}

.form-message.success {
  color: #15803d;
}

.form-message.error {
  color: #b91c1c;
}

@media (max-width: 960px) {
  .prompt-grid {
    grid-template-columns: 1fr;
  }

  .prompt-textarea-lg {
    min-height: 320px;
  }
}
</style>
