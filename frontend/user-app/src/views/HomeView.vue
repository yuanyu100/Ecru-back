<template>
  <div class="home-page">
    <div class="weather-area">
      <span class="status-dot" aria-hidden="true"></span>
      <button class="weather-text" type="button" @click="refreshWeather" :disabled="isWeatherLoading">
        <span>{{ weatherDisplay.location }}</span>
        <span>{{ isWeatherLoading ? '定位中' : `${weatherDisplay.temperatureText} ${weatherDisplay.weatherCondition}` }}</span>
      </button>
    </div>

    <main class="page-main">
      <section class="prompt-stage">
        <div class="prompt-copy">
          <p
            :class="['typed-line', headlineVisible ? 'visible' : 'hidden']"
            :style="headlineTransitionStyle"
          >
            {{ typedHeadline }}
          </p>
        </div>

        <div class="prompt-line" role="group" aria-label="穿搭对话输入">
          <input
            v-model="promptInput"
            class="prompt-line-input"
            type="text"
            placeholder="我想..."
            aria-label="输入穿搭想法"
            @keydown.enter="sendPrompt"
          />
          <button class="prompt-line-send" type="button" :disabled="!promptInput.trim()" @click="sendPrompt" aria-label="发送">
            <span class="prompt-line-arrow" aria-hidden="true"></span>
          </button>
        </div>

        <button
          v-if="showFlowToggle"
          class="flow-toggle"
          type="button"
          :aria-label="isRecommendationVisible ? '收起推荐流' : '展开推荐流'"
          @click="toggleRecommendations"
        >
          <span
            :class="['flow-toggle-icon', isRecommendationVisible ? 'expanded' : 'collapsed']"
            aria-hidden="true"
          ></span>
        </button>
      </section>

      <section
        v-if="isRecommendationVisible && visibleCards.length"
        ref="recommendationSectionRef"
        class="recommendation-section"
      >
        <div class="recommendation-wrap">
          <button class="refresh-btn" type="button" @click="rebuildInspirations" aria-label="换一组">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 8A9 9 0 1 0 21 13" stroke-linecap="round"/><polyline points="20 3 20 8 15 8" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </button>
          <div class="recommendation-grid">
          <article
            v-for="card in visibleCards"
            :key="card.id"
            class="look-card"
            role="button"
            tabindex="0"
            @click="openRecommendation(card)"
            @keydown.enter.prevent="openRecommendation(card)"
            @keydown.space.prevent="openRecommendation(card)"
          >
            <button
              :class="['save-button', isCardSaved(card.id) ? 'saved' : '']"
              type="button"
              :aria-label="isCardSaved(card.id) ? '取消收藏' : '收藏这套搭配'"
              @click.stop="toggleSaveCard(card)"
            >
              {{ isCardSaved(card.id) ? '★' : '☆' }}
            </button>

            <div class="look-board">
              <div
                v-for="(item, index) in boardItems(card)"
                :key="`${card.id}-${item.clothingId || item.name || index}`"
                :class="['board-piece', `piece-${index + 1}`]"
              >
                <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.name || '搭配单品'" loading="lazy" />
                <div v-else class="piece-placeholder">
                  <span>{{ shortCategory(item.category) }}</span>
                </div>
              </div>
            </div>

            <div class="look-copy">
              <strong>{{ card.title }}</strong>
              <p>{{ card.note }}</p>
            </div>
          </article>
          </div>
        </div>
      </section>

      <section v-else-if="showEmptySection" class="empty-section">
        <div class="recommendation-wrap">
          <button class="refresh-btn" type="button" @click="rebuildInspirations" aria-label="重试">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 8A9 9 0 1 0 21 13" stroke-linecap="round"/><polyline points="20 3 20 8 15 8" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </button>
          <p>{{ emptyReason }}</p>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { authApi } from '../api/auth';
import { outfitApi } from '../api/outfit';
import { detectBrowserPosition, weatherApi } from '../api/weather';

const router = useRouter();

const defaultHeadline = '今天想穿成什么样。';
const defaultEmptyReason = '还没有生成新的推荐流，可以再试一次，或者先去补充衣橱。';
const homeFlowPreferenceKey = 'ecru-home-flow-default-visible';
const defaultHeadlineStayMs = 2200;
const defaultHeadlineFadeMs = 820;
const headlineRevealDelayMs = 120;
const fallbackLocation = '所在城市';

const fallbackCards = [
  {
    id: 'guest-look-1',
    title: '通勤衬衫 + 直筒下装',
    note: '先用更利落的上装把轮廓立起来，再用低干扰下装稳住通勤感。',
    items: [
      { name: '衬衫', category: '上装', imageUrl: '' },
      { name: '长裤', category: '下装', imageUrl: '' }
    ]
  },
  {
    id: 'guest-look-2',
    title: '针织上衣 + 半裙',
    note: '整体更柔和，也更适合日常想穿得轻松一点的时候。',
    items: [
      { name: '针织', category: '上装', imageUrl: '' },
      { name: '半裙', category: '下装', imageUrl: '' }
    ]
  },
  {
    id: 'guest-look-3',
    title: '内搭 + 外套',
    note: '用层次感把整套搭配撑起来，适合早晚温差更明显的天气。',
    items: [
      { name: '内搭', category: '上装', imageUrl: '' },
      { name: '外套', category: '外套', imageUrl: '' }
    ]
  }
];

const promptInput = ref('');
const typedHeadline = ref('');
const headlineVisible = ref(true);
const homeHeadlines = ref([defaultHeadline]);
const inspirations = ref([]);
const savedLooks = ref([]);
const emptyReason = ref(defaultEmptyReason);
const rotateTimer = ref(null);
const fadeTimer = ref(null);
const recommendationSectionRef = ref(null);
const currentWeather = ref(weatherApi.getCachedWeather());
const isWeatherLoading = ref(false);
const isRecommendationVisible = ref(localStorage.getItem(homeFlowPreferenceKey) !== 'false');
const homeHeadlineStayMs = ref(defaultHeadlineStayMs);
const homeHeadlineFadeMs = ref(defaultHeadlineFadeMs);

const isAuthenticated = computed(() => authApi.isAuthenticated());
const visibleCards = computed(() => inspirations.value.slice(0, 6));
const savedLookIds = computed(() => new Set(savedLooks.value.map((item) => item.id)));
const showFlowToggle = computed(() => visibleCards.value.length > 0 || showEmptySection.value);
const showEmptySection = computed(
  () => isAuthenticated.value && isRecommendationVisible.value && visibleCards.value.length === 0
);
const headlineTransitionStyle = computed(() => ({
  transitionDuration: `${homeHeadlineFadeMs.value}ms`
}));

const weatherDisplay = computed(() => {
  const weather = currentWeather.value || {};
  const numericTemperature = Number(weather.temperature);
  return {
    location: weather.location || fallbackLocation,
    weatherCondition: weather.weatherCondition || '天气',
    temperatureText: Number.isFinite(numericTemperature) ? `${Math.round(numericTemperature)}°C` : '--'
  };
});

const loadSavedLooks = () => {
  try {
    savedLooks.value = JSON.parse(localStorage.getItem('savedLooks') || '[]');
  } catch {
    savedLooks.value = [];
  }
};

const persistSavedLooks = (next) => {
  savedLooks.value = next;
  localStorage.setItem('savedLooks', JSON.stringify(next));
};

const isCardSaved = (cardId) => savedLookIds.value.has(cardId);

const parseHeadlineStayMs = (settings = {}) => {
  const numericValue = Number(settings.homePromptStayMs);
  if (!Number.isFinite(numericValue)) {
    return defaultHeadlineStayMs;
  }
  return Math.min(Math.max(Math.round(numericValue), 1200), 8000);
};

const parseHeadlineFadeMs = (settings = {}) => {
  const numericValue = Number(settings.homePromptFadeMs);
  if (!Number.isFinite(numericValue)) {
    return defaultHeadlineFadeMs;
  }
  return Math.min(Math.max(Math.round(numericValue), 400), 2200);
};

const parseHomeHeadlines = (settings = {}) => {
  if (typeof settings.homePrompts === 'string' && settings.homePrompts.trim()) {
    try {
      const parsed = JSON.parse(settings.homePrompts);
      if (Array.isArray(parsed)) {
        const prompts = parsed.map((item) => String(item || '').trim()).filter(Boolean);
        if (prompts.length) {
          return prompts;
        }
      }
    } catch {
      // fall through
    }
  }

  const prompts = String(settings.homePrompt || defaultHeadline)
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean);

  return prompts.length ? prompts : [defaultHeadline];
};

const clearHeadlineTimers = () => {
  if (rotateTimer.value) {
    window.clearTimeout(rotateTimer.value);
    rotateTimer.value = null;
  }
  if (fadeTimer.value) {
    window.clearTimeout(fadeTimer.value);
    fadeTimer.value = null;
  }
};

const startHeadlineRotation = (sources) => {
  clearHeadlineTimers();

  const promptList = (Array.isArray(sources) ? sources : [sources])
    .map((item) => String(item || '').trim())
    .filter(Boolean);
  const cycleSources = promptList.length ? promptList : [defaultHeadline];
  let activeIndex = 0;

  const playCurrent = () => {
    typedHeadline.value = cycleSources[activeIndex] || defaultHeadline;
    headlineVisible.value = false;

    fadeTimer.value = window.setTimeout(() => {
      headlineVisible.value = true;
    }, headlineRevealDelayMs);

    if (cycleSources.length > 1) {
      rotateTimer.value = window.setTimeout(() => {
        headlineVisible.value = false;
        fadeTimer.value = window.setTimeout(() => {
          activeIndex = (activeIndex + 1) % cycleSources.length;
          playCurrent();
        }, homeHeadlineFadeMs.value);
      }, homeHeadlineStayMs.value + headlineRevealDelayMs);
    }
  };

  playCurrent();
};

const buildRecommendationParams = (refresh = false) => {
  const weather = currentWeather.value || {};
  const params = { refresh };
  if (weather.location) {
    params.location = weather.location;
  }
  if (weather.temperature !== undefined && weather.temperature !== null && weather.temperature !== '') {
    params.temperature = weather.temperature;
  }
  if (weather.weatherCondition) {
    params.weatherCondition = weather.weatherCondition;
  }
  return params;
};

const loadRecommendations = async ({ refresh = false } = {}) => {
  if (!isAuthenticated.value) {
    emptyReason.value = '';
    inspirations.value = [...fallbackCards];
    return;
  }

  try {
    const response = await outfitApi.getHomeRecommendations(buildRecommendationParams(refresh));
    inspirations.value = response.data?.looks || [];
    emptyReason.value = response.data?.emptyReason || defaultEmptyReason;
  } catch (error) {
    console.error('Load home recommendations failed:', error);
    inspirations.value = [];
    emptyReason.value = '推荐流加载失败了，可以稍后再试。';
  }
};

const rebuildInspirations = async () => {
  await loadRecommendations({ refresh: isAuthenticated.value });
};

const saveLocalLooks = (payload) => {
  const next = [payload, ...savedLooks.value.filter((item) => item.id !== payload.id)].slice(0, 20);
  persistSavedLooks(next);
};

const removeSavedLook = (cardId) => {
  persistSavedLooks(savedLooks.value.filter((item) => item.id !== cardId));
};

const toggleSaveCard = (card) => {
  if (isCardSaved(card.id)) {
    removeSavedLook(card.id);
    return;
  }
  saveLocalLooks(card);
};

const openChat = () => {
  router.push('/chat');
};

const sendPrompt = () => {
  const text = promptInput.value.trim();
  if (!text) return;
  localStorage.setItem('pendingChatPrompt', text);
  router.push({ path: '/chat', query: { q: text } });
};

const openRecommendation = (card) => {
  if (!isAuthenticated.value) {
    router.push('/login');
    return;
  }
  router.push(`/home/recommendations/${card.id}`);
};

const scrollToRecommendations = async () => {
  await nextTick();
  recommendationSectionRef.value?.scrollIntoView({
    behavior: 'smooth',
    block: 'start'
  });
};

const toggleRecommendations = async () => {
  isRecommendationVisible.value = !isRecommendationVisible.value;
  localStorage.setItem(homeFlowPreferenceKey, isRecommendationVisible.value ? 'true' : 'false');

  if (isRecommendationVisible.value) {
    await scrollToRecommendations();
    return;
  }

  window.scrollTo({ top: 0, behavior: 'smooth' });
};

const parseVisibleSetting = (value) => {
  if (typeof value === 'boolean') {
    return value;
  }
  if (typeof value === 'string') {
    return value !== 'false';
  }
  return localStorage.getItem(homeFlowPreferenceKey) !== 'false';
};

const shortCategory = (category) => {
  const value = String(category || '').trim();
  return value || '单品';
};

const boardItems = (card) => {
  const items = Array.isArray(card.items) ? card.items.filter(Boolean).slice(0, 4) : [];
  if (items.length >= 2) {
    return items;
  }
  return [...items, ...fallbackCards[0].items].slice(0, 2);
};

const loadHomeData = async () => {
  loadSavedLooks();

  if (!isAuthenticated.value) {
    await loadRecommendations();
    startHeadlineRotation(homeHeadlines.value);
    return;
  }

  try {
    const settingsResponse = await authApi.getUserSettings();
    const settings = settingsResponse.data || {};
    homeHeadlines.value = parseHomeHeadlines(settings);
    homeHeadlineStayMs.value = parseHeadlineStayMs(settings);
    homeHeadlineFadeMs.value = parseHeadlineFadeMs(settings);
    isRecommendationVisible.value = parseVisibleSetting(settings.homeFlowDefaultVisible);
    localStorage.setItem(homeFlowPreferenceKey, isRecommendationVisible.value ? 'true' : 'false');
  } catch (error) {
    console.error('Load home data failed:', error);
  } finally {
    await loadRecommendations();
    startHeadlineRotation(homeHeadlines.value);
  }
};

const fetchWeatherByParams = async (params) => {
  const response = await weatherApi.getCurrentWeather(params);
  const weather = response?.data || null;
  if (weather) {
    currentWeather.value = weather;
    weatherApi.setCachedWeather(weather);
  }
};

const refreshWeather = async () => {
  if (weatherApi.hasFreshWeatherCache() && currentWeather.value) {
    return;
  }

  isWeatherLoading.value = true;
  try {
    const position = await detectBrowserPosition();
    await fetchWeatherByParams(position);
  } catch (error) {
    console.warn('Detect browser position failed:', error);
    try {
      const cachedLocation = weatherApi.getCachedWeather()?.location || fallbackLocation;
      await fetchWeatherByParams({ location: cachedLocation });
    } catch (fallbackError) {
      console.warn('Fetch fallback weather failed:', fallbackError);
    }
  } finally {
    isWeatherLoading.value = false;
  }
};

onMounted(async () => {
  if (weatherApi.hasFreshWeatherCache()) {
    currentWeather.value = weatherApi.getCachedWeather();
  }

  await refreshWeather();
  await loadHomeData();
});

onBeforeUnmount(() => {
  clearHeadlineTimers();
});
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  padding: 0 20px 124px;
  background:
    radial-gradient(circle at top, rgba(255, 252, 246, 0.94), transparent 28%),
    linear-gradient(180deg, var(--bg-base) 0%, var(--bg-soft) 100%);
}

.weather-area {
  position: fixed;
  top: 18px;
  right: 18px;
  z-index: 8;
  display: flex;
  align-items: center;
  gap: 8px;
}

.weather-text {
  display: grid;
  gap: 2px;
  border: none;
  background: transparent;
  color: var(--text-faint);
  text-align: right;
  cursor: pointer;
}

.weather-text:disabled {
  cursor: default;
}

.weather-text span:first-child {
  font-size: 11px;
  letter-spacing: 0.08em;
}

.weather-text span:last-child {
  font-size: 12px;
  color: var(--text-soft);
}

.page-main {
  width: min(100%, 760px);
  margin: 0 auto;
}

.prompt-stage {
  min-height: 62vh;
  padding-top: 38vh;
}

.prompt-copy {
  display: flex;
  justify-content: center;
  text-align: center;
  transform: translateY(-28px);
}

.typed-line {
  min-height: 38px;
  color: var(--text-soft);
  font-family: 'Iowan Old Style', 'Noto Serif SC', 'Songti SC', serif;
  font-size: 29px;
  letter-spacing: 0.03em;
  transition: opacity 0.82s ease, transform 0.82s ease, filter 0.82s ease;
}

.typed-line.visible {
  opacity: 1;
  transform: translateY(0);
  filter: blur(0);
}

.typed-line.hidden {
  opacity: 0;
  transform: translateY(10px);
  filter: blur(4px);
}

.prompt-line {
  display: flex;
  align-items: center;
  width: min(100%, 420px);
  margin: 34px auto 0;
  border-bottom: 1px solid var(--line-strong);
  background: transparent;
}

.prompt-line-input {
  flex: 1;
  padding: 14px 0 12px;
  border: none;
  background: transparent;
  color: var(--text-main);
  font-size: 15px;
  outline: none;
}

.prompt-line-input::placeholder {
  color: var(--text-faint);
}

.prompt-line-send {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  cursor: pointer;
  opacity: 0.5;
  transition: opacity 150ms ease;
}

.prompt-line-send:not(:disabled) {
  opacity: 1;
}

.prompt-line-send:disabled {
  cursor: default;
}

.prompt-line-arrow {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-right: 2px;
  border-top: 1.5px solid var(--accent);
  border-right: 1.5px solid var(--accent);
  transform: rotate(45deg);
}

.flow-toggle {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  margin: 64px 0 0 auto;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: color-mix(in srgb, var(--text-faint) 68%, transparent);
  cursor: pointer;
  transition: color 150ms ease, transform 150ms ease;
}

.flow-toggle:hover {
  color: color-mix(in srgb, var(--text-soft) 76%, transparent);
  transform: translateY(-1px);
}

.flow-toggle-icon {
  position: relative;
  display: block;
  width: 8px;
  height: 12px;
}

.flow-toggle-icon::before,
.flow-toggle-icon::after {
  content: '';
  position: absolute;
  left: 50%;
  width: 6px;
  height: 6px;
  border-right: 1.25px solid currentColor;
  border-bottom: 1.25px solid currentColor;
}

.flow-toggle-icon.collapsed::before {
  top: 0;
  transform: translateX(-50%) rotate(45deg);
}

.flow-toggle-icon.collapsed::after {
  top: 5px;
  transform: translateX(-50%) rotate(45deg);
}

.flow-toggle-icon.expanded::before {
  top: 3px;
  transform: translateX(-50%) rotate(-135deg);
}

.flow-toggle-icon.expanded::after {
  top: 8px;
  transform: translateX(-50%) rotate(-135deg);
}

.recommendation-section,
.empty-section {
  padding-top: 24px;
}

.recommendation-wrap {
  position: relative;
  padding: 36px 16px 20px;
  border-radius: 24px;
  border: 1px solid var(--line-soft);
  background: color-mix(in srgb, var(--surface-strong) 90%, transparent);
  box-shadow: 0 4px 20px rgba(62, 52, 38, 0.06);
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.text-link {
  border: none;
  background: transparent;
  color: var(--text-soft);
  font-size: 11px;
  cursor: pointer;
}

.refresh-btn {
  position: absolute;
  top: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: none;
  background: transparent;
  color: var(--text-faint);
  cursor: pointer;
  transition: color 150ms ease, transform 400ms ease;
}

.refresh-btn:hover {
  color: var(--text-soft);
  transform: rotate(180deg);
}

.refresh-btn svg {
  width: 14px;
  height: 14px;
}

.refresh-btn svg path,
.refresh-btn svg polyline {
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
  fill: none;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--accent-strong) 64%, white 12%);
  animation: status-breathe 3.6s ease-in-out infinite;
  flex-shrink: 0;
}

@keyframes status-breathe {
  0%, 100% { transform: scale(0.92); opacity: 0.55; }
  50%       { transform: scale(1.14); opacity: 0.9; }
}

.recommendation-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.look-card {
  position: relative;
  border-radius: 22px;
  border: 1px solid var(--line-soft);
  background: color-mix(in srgb, var(--surface-strong) 93%, transparent);
  box-shadow: var(--shadow-card);
  overflow: hidden;
  cursor: pointer;
}

.save-button {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 2;
  border: none;
  background: transparent;
  color: rgba(118, 103, 82, 0.72);
  font-size: 18px;
  cursor: pointer;
}

.save-button.saved {
  color: #d2ab62;
}

.look-board {
  position: relative;
  aspect-ratio: 1 / 1.02;
  margin: 16px;
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(250, 247, 241, 0.98)),
    #fff;
  box-shadow:
    inset 0 0 0 1px rgba(170, 153, 130, 0.08),
    0 10px 24px rgba(122, 104, 82, 0.08);
}

.board-piece {
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 16px;
  background: #f5f1ea;
}

.board-piece img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.piece-1 {
  top: 14px;
  left: 14px;
  width: calc(50% - 20px);
  height: 44%;
}

.piece-2 {
  top: 14px;
  right: 14px;
  width: calc(50% - 20px);
  height: 52%;
}

.piece-3 {
  bottom: 14px;
  left: 14px;
  width: calc(44% - 12px);
  height: 30%;
}

.piece-4 {
  bottom: 14px;
  right: 14px;
  width: calc(56% - 16px);
  height: 22%;
}

.piece-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: var(--text-soft);
  font-size: 12px;
  letter-spacing: 0.08em;
}

.look-copy {
  padding: 0 14px 16px;
}

.look-copy strong {
  display: block;
  color: var(--text-main);
  font-size: 13px;
  font-weight: 600;
}

.look-copy p {
  margin-top: 6px;
  color: var(--text-soft);
  line-height: 1.6;
  font-size: 11px;
}

.empty-section p {
  margin: 18px 0 0;
  color: var(--text-soft);
  line-height: 1.8;
}

@media (min-width: 768px) {
  .home-page {
    padding: 0 28px 40px;
  }

  .weather-area {
    top: 24px;
    right: 30px;
  }

  .prompt-stage {
    min-height: 58vh;
    padding-top: 32vh;
  }

  .prompt-copy {
    transform: translateY(-40px);
  }

  .typed-line {
    font-size: 37px;
  }

  .flow-toggle {
    margin-top: 34px;
  }

  .recommendation-section,
  .empty-section {
    padding-top: 28px;
  }

  .recommendation-grid {
    gap: 18px;
  }
}
</style>
