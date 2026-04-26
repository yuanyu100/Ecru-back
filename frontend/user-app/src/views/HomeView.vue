<template>
  <div class="home-page">
    <button class="weather-text" type="button" @click="refreshWeather" :disabled="isWeatherLoading">
      <span>{{ weatherDisplay.location }}</span>
      <span>{{ isWeatherLoading ? '定位中' : `${weatherDisplay.temperatureText} ${weatherDisplay.weatherCondition}` }}</span>
    </button>

    <main class="page-main">
      <section class="prompt-stage">
        <div class="prompt-copy">
          <p
            :class="['typed-line', headlineVisible ? 'visible' : 'hidden']"
            :style="headlineTransitionStyle"
          >{{ typedHeadline }}</p>
        </div>

        <form class="prompt-form" @submit.prevent="submitPrompt">
          <input
            v-model.trim="prompt"
            type="text"
            placeholder=""
            autocomplete="off"
            @keydown.enter.exact.prevent="submitPrompt"
          />
          <button class="submit-button" type="submit" aria-label="发送">
            <span></span>
          </button>
        </form>

        <button
          v-if="visibleCards.length"
          class="flow-toggle"
          type="button"
          :aria-label="isRecommendationVisible ? '收起今日推荐' : '展开今日推荐'"
          @click="toggleRecommendations"
        >
          <span :class="['flow-toggle-icon', isRecommendationVisible ? 'expanded' : 'collapsed']" aria-hidden="true"></span>
        </button>
      </section>

      <section
        v-if="isRecommendationVisible && visibleCards.length"
        ref="recommendationSectionRef"
        class="recommendation-section"
      >
        <div class="section-head">
          <p>今日推荐</p>
          <button class="text-link" type="button" @click="rebuildInspirations">换一组</button>
        </div>

        <div class="recommendation-grid">
          <article v-for="card in visibleCards" :key="card.id" class="look-card">
            <button
              :class="['save-button', isCardSaved(card.id) ? 'saved' : '']"
              type="button"
              :aria-label="isCardSaved(card.id) ? '取消收藏' : '收藏'"
              @click.stop="toggleSaveCard(card)"
            >
              {{ isCardSaved(card.id) ? '★' : '☆' }}
            </button>

            <div class="look-image" :style="cardStyle(card)">
              <span>{{ card.mood }}</span>
            </div>

            <div class="look-copy">
              <strong>{{ card.title }}</strong>
              <p>{{ card.note }}</p>
              <div class="tag-row">
                <span v-for="tag in card.tags" :key="`${card.id}-${tag}`">#{{ tag }}</span>
              </div>
            </div>
          </article>
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
const homeFlowPreferenceKey = 'ecru-home-flow-default-visible';
const defaultHeadlineStayMs = 2200;
const defaultHeadlineFadeMs = 820;
const headlineRevealDelayMs = 120;
const fallbackLocation = '所在城市';

const fallbackCards = [
  {
    id: 'look-wood-commute',
    mood: '轻木通勤',
    title: '米白针织配浅卡其长裤',
    note: '轻松、干净，适合上班或简单见面。',
    tags: ['针织', '卡其', '通勤'],
    palette: ['#d9c3a5', '#f5ecde']
  },
  {
    id: 'look-ink-weekend',
    mood: '周末散步',
    title: '灰蓝衬衫配牛仔裤',
    note: '颜色安静，适合天气舒服的时候随手穿。',
    tags: ['灰蓝', '牛仔', '周末'],
    palette: ['#85909a', '#dfe6eb']
  },
  {
    id: 'look-soft-layer',
    mood: '柔和叠穿',
    title: '燕麦色马甲叠白衬衫',
    note: '适合有温差的天气，层次很自然。',
    tags: ['燕麦色', '叠穿', '层次'],
    palette: ['#b59f86', '#ece0d1']
  },
  {
    id: 'look-night-simple',
    mood: '夜色极简',
    title: '深色上衣配直筒半裙',
    note: '更收敛，也更显轮廓，适合晚间场景。',
    tags: ['深色', '半裙', '极简'],
    palette: ['#5d5752', '#d6cec4']
  }
];

const prompt = ref('');
const typedHeadline = ref('');
const headlineVisible = ref(true);
const homeHeadlines = ref([defaultHeadline]);
const outfitHistory = ref([]);
const inspirations = ref([]);
const savedLooks = ref([]);
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
const headlineTransitionStyle = computed(() => ({
  transitionDuration: `${homeHeadlineFadeMs.value}ms`
}));

const weatherDisplay = computed(() => {
  const weather = currentWeather.value || {};
  const numericTemperature = Number(weather.temperature);
  const temperatureText = Number.isFinite(numericTemperature) ? `${Math.round(numericTemperature)}°C` : '--';

  return {
    location: weather.location || fallbackLocation,
    weatherCondition: weather.weatherCondition || '天气',
    temperatureText
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
    } catch (_error) {
      // Fall back to the legacy single-string format below.
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

  const promptList = (Array.isArray(sources) ? sources : [sources]).map((item) => String(item || '').trim()).filter(Boolean);
  const cycleSources = promptList.length ? promptList : [defaultHeadline];
  let activeIndex = 0;

  const playCurrent = () => {
    const source = cycleSources[activeIndex] || defaultHeadline;
    typedHeadline.value = source;
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

const buildHistoryCards = () =>
  outfitHistory.value.slice(0, 2).map((item, index) => ({
    id: `history-${item.id}`,
    mood: item.occasion || '穿搭记录',
    title: item.outfitName || '最近生成的搭配',
    note: item.outfitDescription || item.reasoning || '这是你最近保留的一次搭配结果。',
    tags: [item.weatherCondition || '天气', item.season || '当季', item.isFavorite ? '已收藏' : '搭配'],
    palette: index % 2 === 0 ? ['#b6c0cf', '#ece4d8'] : ['#b5a28a', '#f4ede1']
  }));

const rebuildInspirations = () => {
  const historyCards = buildHistoryCards();
  const shuffledFallbackCards = [...fallbackCards].sort(() => Math.random() - 0.5);
  inspirations.value = [...historyCards, ...shuffledFallbackCards].filter(
    (item, index, list) => list.findIndex((current) => current.id === item.id) === index
  );
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

const submitPrompt = () => {
  if (!prompt.value.trim()) {
    return;
  }

  localStorage.setItem('pendingChatPrompt', prompt.value.trim());
  router.push('/chat');
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

const cardStyle = (card) => ({
  background: `linear-gradient(135deg, ${card.palette[0]}, ${card.palette[1] || card.palette[0]})`
});

const parseVisibleSetting = (value) => {
  if (typeof value === 'boolean') {
    return value;
  }
  if (typeof value === 'string') {
    return value !== 'false';
  }
  return localStorage.getItem(homeFlowPreferenceKey) !== 'false';
};

const loadHomeData = async () => {
  loadSavedLooks();

  if (!isAuthenticated.value) {
    rebuildInspirations();
    startHeadlineRotation(homeHeadlines.value);
    return;
  }

  try {
    const [historyResponse, settingsResponse] = await Promise.all([
      outfitApi.getHistory(1, 6),
      authApi.getUserSettings()
    ]);

    outfitHistory.value = historyResponse.data || [];
    const settings = settingsResponse.data || {};
    homeHeadlines.value = parseHomeHeadlines(settings);
    homeHeadlineStayMs.value = parseHeadlineStayMs(settings);
    homeHeadlineFadeMs.value = parseHeadlineFadeMs(settings);
    isRecommendationVisible.value = parseVisibleSetting(settings.homeFlowDefaultVisible);
    localStorage.setItem(homeFlowPreferenceKey, isRecommendationVisible.value ? 'true' : 'false');
  } catch (error) {
    console.error('Load home data failed:', error);
  } finally {
    rebuildInspirations();
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

  await Promise.all([loadHomeData(), refreshWeather()]);
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

.weather-text {
  position: fixed;
  top: 18px;
  right: 18px;
  z-index: 8;
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
  min-height: 78vh;
  padding-top: 52vh;
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

.prompt-form {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: end;
  gap: 10px;
  width: min(100%, 420px);
  margin: 34px auto 0;
  border-bottom: 1px solid var(--line-strong);
}

.prompt-form input {
  width: 100%;
  padding: 15px 0 12px;
  border: none;
  background: transparent;
  color: var(--text-main);
  font-size: 15px;
  outline: none;
}

.submit-button {
  width: 28px;
  height: 28px;
  margin-bottom: 8px;
  border: none;
  background: transparent;
  cursor: pointer;
}

.submit-button span {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-top: 1.5px solid var(--accent);
  border-right: 1.5px solid var(--accent);
  transform: rotate(45deg);
}

.flow-toggle {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  margin: 42px 0 0 auto;
  border: none;
  border-radius: 999px;
  background: color-mix(in srgb, var(--surface-strong) 94%, transparent);
  color: var(--text-faint);
  box-shadow: 0 6px 16px rgba(62, 52, 38, 0.08);
  cursor: pointer;
}

.flow-toggle-icon {
  position: relative;
  display: block;
  width: 14px;
  height: 18px;
}

.flow-toggle-icon::before,
.flow-toggle-icon::after {
  content: '';
  position: absolute;
  left: 50%;
  width: 8px;
  height: 8px;
  border-right: 1.6px solid currentColor;
  border-bottom: 1.6px solid currentColor;
}

.flow-toggle-icon.collapsed::before {
  top: 0;
  transform: translateX(-50%) rotate(45deg);
}

.flow-toggle-icon.collapsed::after {
  top: 6px;
  transform: translateX(-50%) rotate(45deg);
}

.flow-toggle-icon.expanded::before {
  top: 4px;
  transform: translateX(-50%) rotate(-135deg);
}

.flow-toggle-icon.expanded::after {
  top: 10px;
  transform: translateX(-50%) rotate(-135deg);
}

.recommendation-section {
  padding-top: 36px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-head p {
  color: var(--text-faint);
  font-size: 10px;
  letter-spacing: 0.18em;
}

.text-link {
  border: none;
  background: transparent;
  color: var(--text-soft);
  font-size: 11px;
  cursor: pointer;
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
}

.save-button {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 2;
  border: none;
  background: transparent;
  color: rgba(255, 250, 242, 0.92);
  font-size: 18px;
  cursor: pointer;
}

.save-button.saved {
  color: #f2d18d;
}

.look-image {
  position: relative;
  aspect-ratio: 4 / 5;
  padding: 16px;
}

.look-image::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.46), transparent 36%),
    linear-gradient(180deg, transparent 24%, rgba(28, 22, 15, 0.16) 100%);
}

.look-image span {
  position: absolute;
  left: 14px;
  bottom: 12px;
  z-index: 1;
  color: rgba(58, 47, 34, 0.88);
  font-family: 'Iowan Old Style', 'Noto Serif SC', 'Songti SC', serif;
  font-size: 14px;
}

.look-copy {
  padding: 12px 12px 14px;
}

.look-copy strong {
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

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.tag-row span {
  color: var(--text-faint);
  font-size: 10px;
}

@media (min-width: 768px) {
  .home-page {
    padding: 0 28px 40px;
  }

  .weather-text {
    top: 24px;
    right: 30px;
  }

  .prompt-stage {
    min-height: 74vh;
    padding-top: 42vh;
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

  .recommendation-section {
    padding-top: 28px;
  }

  .recommendation-grid {
    gap: 18px;
  }
}
</style>
