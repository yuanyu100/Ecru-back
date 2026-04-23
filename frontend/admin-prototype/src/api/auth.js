import axios from 'axios';

const apiClient = axios.create({
  baseURL: 'http://localhost:8081/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// 请求拦截器，添加token
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export const authApi = {
  /**
   * 用户注册
   * @param {Object} user - 用户信息
   * @returns {Promise} - 返回注册结果
   */
  register: async (user) => {
    try {
      const response = await apiClient.post('/users/register', user);
      return response.data;
    } catch (error) {
      console.error('注册失败:', error);
      throw error;
    }
  },

  /**
   * 用户登录
   * @param {Object} credentials - 登录凭证
   * @returns {Promise} - 返回登录结果
   */
  login: async (credentials) => {
    try {
      const response = await apiClient.post('/users/login', credentials);
      // 保存token到本地存储
      if (response.data && response.data.data && response.data.data.token) {
        localStorage.setItem('token', response.data.data.token);
        localStorage.setItem('user', JSON.stringify(response.data.data));
      }
      return response.data;
    } catch (error) {
      console.error('登录失败:', error);
      throw error;
    }
  },

  /**
   * 获取用户信息
   * @param {string} userId - 用户ID
   * @returns {Promise} - 返回用户信息
   */
  getUserInfo: async (userId) => {
    try {
      const response = await apiClient.get(`/users/${userId}`);
      return response.data;
    } catch (error) {
      console.error('获取用户信息失败:', error);
      throw error;
    }
  },

  /**
   * 更新用户偏好
   * @param {string} userId - 用户ID
   * @param {Object} preferences - 用户偏好
   * @returns {Promise} - 返回更新结果
   */
  updatePreferences: async (userId, preferences) => {
    try {
      const response = await apiClient.put(`/users/${userId}/preferences`, preferences);
      return response.data;
    } catch (error) {
      console.error('更新用户偏好失败:', error);
      throw error;
    }
  },

  /**
   * 登出
   */
  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  },

  /**
   * 获取当前登录用户
   * @returns {Object|null} - 当前用户信息
   */
  getCurrentUser: () => {
    const userStr = localStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : null;
  },

  /**
   * 检查是否已登录
   * @returns {boolean} - 是否已登录
   */
  isAuthenticated: () => {
    return localStorage.getItem('token') !== null;
  }
};