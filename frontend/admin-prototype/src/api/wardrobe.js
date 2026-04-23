import axios from 'axios';

const apiClient = axios.create({
  baseURL: 'http://localhost:8081/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
});

export const wardrobeApi = {
  /**
   * 添加衣物
   * @param {Object} clothing - 衣物信息
   * @returns {Promise} - 返回添加结果
   */
  addClothing: async (clothing) => {
    try {
      const response = await apiClient.post('/wardrobe/items', clothing);
      return response.data;
    } catch (error) {
      console.error('添加衣物失败:', error);
      throw error;
    }
  },

  /**
   * 获取衣物列表
   * @param {Object} params - 查询参数
   * @returns {Promise} - 返回衣物列表
   */
  getClothingList: async (params = {}) => {
    try {
      const response = await apiClient.get('/wardrobe/items', { params });
      return response.data;
    } catch (error) {
      console.error('获取衣物列表失败:', error);
      throw error;
    }
  },

  /**
   * 更新衣物信息
   * @param {string} itemId - 衣物ID
   * @param {Object} clothing - 衣物信息
   * @returns {Promise} - 返回更新结果
   */
  updateClothing: async (itemId, clothing) => {
    try {
      const response = await apiClient.put(`/wardrobe/items/${itemId}`, clothing);
      return response.data;
    } catch (error) {
      console.error('更新衣物失败:', error);
      throw error;
    }
  },

  /**
   * 删除衣物
   * @param {string} itemId - 衣物ID
   * @returns {Promise} - 返回删除结果
   */
  deleteClothing: async (itemId) => {
    try {
      const response = await apiClient.delete(`/wardrobe/items/${itemId}`);
      return response.data;
    } catch (error) {
      console.error('删除衣物失败:', error);
      throw error;
    }
  },

  /**
   * 上传衣物图片
   * @param {string} itemId - 衣物ID
   * @param {File} image - 图片文件
   * @returns {Promise} - 返回上传结果
   */
  uploadImage: async (itemId, image) => {
    try {
      const formData = new FormData();
      formData.append('image', image);
      const response = await apiClient.post(`/wardrobe/items/${itemId}/images`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });
      return response.data;
    } catch (error) {
      console.error('上传图片失败:', error);
      throw error;
    }
  },

  /**
   * 设置搭配频率
   * @param {string} itemId - 衣物ID
   * @param {number} frequency - 搭配频率
   * @returns {Promise} - 返回设置结果
   */
  setFrequency: async (itemId, frequency) => {
    try {
      const response = await apiClient.put(`/wardrobe/items/${itemId}/frequency`, { frequency });
      return response.data;
    } catch (error) {
      console.error('设置频率失败:', error);
      throw error;
    }
  }
};