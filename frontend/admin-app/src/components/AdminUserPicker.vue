<template>
  <div class="user-picker" @focusin="handleFocus" @focusout="handleBlur">
    <input
      :value="keyword"
      class="text-input"
      type="text"
      :placeholder="placeholder"
      autocomplete="off"
      @input="handleInput"
    />

    <div v-if="open && options.length" class="user-picker-menu">
      <button
        v-for="user in options"
        :key="user.userId || user.id"
        class="user-picker-option"
        type="button"
        @mousedown.prevent="selectUser(user)"
      >
        <strong>{{ user.username }}</strong>
        <span>{{ user.nickname || user.email || '-' }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
import { adminApi } from '../api/admin';

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '用户'
  }
});

const emit = defineEmits(['update:modelValue', 'select']);

const keyword = ref(props.modelValue);
const options = ref([]);
const open = ref(false);

let searchTimer = null;
let blurTimer = null;

watch(
  () => props.modelValue,
  (value) => {
    keyword.value = value || '';
  }
);

const loadUsers = async (value) => {
  const trimmed = value.trim();
  if (!trimmed) {
    options.value = [];
    open.value = false;
    return;
  }

  try {
    const result = await adminApi.getUsers({ keyword: trimmed, page: 1, size: 8 });
    options.value = result?.data?.list || result?.data?.records || [];
    open.value = options.value.length > 0;
  } catch {
    options.value = [];
    open.value = false;
  }
};

const handleInput = (event) => {
  const value = event.target.value;
  keyword.value = value;
  emit('update:modelValue', value);

  if (searchTimer) {
    clearTimeout(searchTimer);
  }

  searchTimer = window.setTimeout(() => {
    loadUsers(value);
  }, 180);
};

const selectUser = (user) => {
  const value = user.username || user.nickname || '';
  keyword.value = value;
  emit('update:modelValue', value);
  emit('select', user);
  open.value = false;
};

const handleFocus = () => {
  if (blurTimer) {
    clearTimeout(blurTimer);
  }
  if (options.value.length) {
    open.value = true;
  }
};

const handleBlur = () => {
  blurTimer = window.setTimeout(() => {
    open.value = false;
  }, 120);
};
</script>

<style scoped>
.user-picker {
  position: relative;
  min-width: 160px;
}

.user-picker-menu {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  z-index: 20;
  overflow: hidden;
  border: 1px solid rgba(113, 129, 153, 0.18);
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 12px 24px rgba(18, 32, 58, 0.12);
}

.user-picker-option {
  width: 100%;
  padding: 10px 12px;
  border: 0;
  border-bottom: 1px solid rgba(113, 129, 153, 0.1);
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.user-picker-option:last-child {
  border-bottom: 0;
}

.user-picker-option strong,
.user-picker-option span {
  display: block;
}

.user-picker-option span {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
}
</style>
