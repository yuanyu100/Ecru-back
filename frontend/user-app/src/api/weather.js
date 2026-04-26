import { apiClient } from './client';

const WEATHER_CACHE_KEY = 'ecru-current-weather';
const WEATHER_CACHE_TTL = 30 * 60 * 1000;
const POSITION_CACHE_KEY = 'ecru-browser-position';
const POSITION_CACHE_TTL = 30 * 60 * 1000;

const readCache = (key) => {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
};

const writeCache = (key, data) => {
  localStorage.setItem(
    key,
    JSON.stringify({
      data,
      timestamp: Date.now()
    })
  );
};

export const weatherApi = {
  getCachedWeather() {
    return readCache(WEATHER_CACHE_KEY)?.data || null;
  },

  hasFreshWeatherCache() {
    const cached = readCache(WEATHER_CACHE_KEY);
    return Boolean(cached?.data && cached?.timestamp && Date.now() - cached.timestamp < WEATHER_CACHE_TTL);
  },

  setCachedWeather(weather) {
    writeCache(WEATHER_CACHE_KEY, weather);
  },

  async getCurrentWeather(params = {}) {
    const response = await apiClient.get('/weather/current', { params });
    return response.data;
  }
};

export const detectBrowserPosition = () =>
  new Promise((resolve, reject) => {
    const cached = readCache(POSITION_CACHE_KEY);
    if (cached?.data && cached?.timestamp && Date.now() - cached.timestamp < POSITION_CACHE_TTL) {
      resolve(cached.data);
      return;
    }

    if (!navigator.geolocation) {
      reject(new Error('浏览器不支持定位'));
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const data = {
          longitude: position.coords.longitude,
          latitude: position.coords.latitude
        };

        writeCache(POSITION_CACHE_KEY, data);
        resolve(data);
      },
      (error) => reject(error),
      {
        enableHighAccuracy: false,
        timeout: 8000,
        maximumAge: POSITION_CACHE_TTL
      }
    );
  });
