export const maxPromptLength = 20;
export const defaultPrompt = '今天想穿成什么样';
export const defaultStayMs = 2200;
export const defaultFadeMs = 820;

export const filterOptions = [
  { label: '全部', value: 'all' },
  { label: '手动添加', value: 'manual' },
  { label: 'PDF 导入', value: 'pdf' }
];

export const stayDurationOptions = [
  { label: '1.5 秒', value: 1500 },
  { label: '2.2 秒', value: 2200 },
  { label: '3 秒', value: 3000 },
  { label: '4 秒', value: 4000 },
  { label: '5 秒', value: 5000 }
];

export const fadeDurationOptions = [
  { label: '柔和 0.6 秒', value: 600 },
  { label: '舒缓 0.82 秒', value: 820 },
  { label: '更慢 1 秒', value: 1000 },
  { label: '从容 1.3 秒', value: 1300 },
  { label: '极慢 1.6 秒', value: 1600 }
];

export const sourceLabelMap = {
  manual: '手动',
  pdf: 'PDF'
};

export const promptLength = (value = '') => Array.from(String(value).trim()).length;

export const createLocalId = (prefix = 'hp') =>
  `${prefix}_${Date.now()}_${Math.random().toString(16).slice(2, 10)}`;

export const normalizePromptItem = (item = {}) => {
  const sourceType = item.sourceType === 'pdf' ? 'pdf' : 'manual';
  const enabled = item.enabled ?? item.selected ?? false;

  return {
    id: item.id || createLocalId(),
    text: String(item.text || '').trim(),
    sourceType,
    sourceLabel: String(item.sourceLabel || '').trim() || (sourceType === 'pdf' ? 'PDF 导入' : '手动添加'),
    enabled: Boolean(enabled)
  };
};

export const normalizeHomePromptSettings = (payload = {}) => {
  const nextItems = Array.isArray(payload.items)
    ? payload.items.map(normalizePromptItem).filter((item) => item.text)
    : [];

  const items = nextItems.length
    ? nextItems
    : [
        normalizePromptItem({
          id: createLocalId(),
          text: defaultPrompt,
          sourceType: 'manual',
          sourceLabel: '手动添加',
          enabled: true
        })
      ];

  if (!items.some((item) => item.enabled)) {
    items[0].enabled = true;
  }

  return {
    items,
    homeFlowDefaultVisible: payload.homeFlowDefaultVisible !== false,
    homePromptStayMs: Number(payload.homePromptStayMs) || defaultStayMs,
    homePromptFadeMs: Number(payload.homePromptFadeMs) || defaultFadeMs
  };
};

export const buildHomePromptPayload = (settings = {}) => {
  const normalized = normalizeHomePromptSettings(settings);
  return {
    items: normalized.items.map((item) => ({
      id: item.id,
      text: item.text,
      sourceType: item.sourceType,
      sourceLabel: item.sourceLabel,
      enabled: item.enabled
    })),
    homeFlowDefaultVisible: normalized.homeFlowDefaultVisible,
    homePromptStayMs: Number(normalized.homePromptStayMs) || defaultStayMs,
    homePromptFadeMs: Number(normalized.homePromptFadeMs) || defaultFadeMs
  };
};
