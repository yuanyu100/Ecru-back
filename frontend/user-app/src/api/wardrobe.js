import { apiClient } from './client';

const toCreatePayload = (clothing = {}) => ({
  name: clothing.name,
  brand: clothing.brand,
  category: clothing.category,
  primaryColor: clothing.color?.primary || clothing.primaryColor,
  secondaryColor: clothing.color?.secondary || clothing.secondaryColor,
  material: clothing.material,
  size: clothing.size,
  purchaseDate: clothing.purchaseTime || clothing.purchaseDate,
  purchaseLink: clothing.purchaseChannel || clothing.purchaseLink,
  imageUrl: clothing.imageUrl,
  autoRecognize: clothing.autoRecognize || false
});

const toUpdatePayload = (clothing = {}) => ({
  name: clothing.name,
  brand: clothing.brand,
  category: clothing.category,
  primaryColor: clothing.color?.primary || clothing.primaryColor,
  secondaryColor: clothing.color?.secondary || clothing.secondaryColor,
  material: clothing.material,
  size: clothing.size,
  purchaseDate: clothing.purchaseTime || clothing.purchaseDate,
  purchaseLink: clothing.purchaseChannel || clothing.purchaseLink,
  frequencyLevel: Number(clothing.frequency || clothing.frequencyLevel || 3)
});

const normalizeItem = (item = {}) => ({
  ...item,
  itemId: item.id,
  color: {
    primary: item.primaryColor || '',
    secondary: item.secondaryColor || ''
  }
});

export const wardrobeApi = {
  addClothing: async (clothing) => {
    try {
      const response = await apiClient.post('/clothings', toCreatePayload(clothing));
      return {
        ...response.data,
        data: {
          ...response.data?.data,
          itemId: response.data?.data?.id
        }
      };
    } catch (error) {
      console.error('Add clothing failed:', error);
      throw error;
    }
  },

  getClothingList: async (params = {}) => {
    try {
      const response = await apiClient.get('/clothings', { params });
      const pageInfo = response.data?.data || {};
      return {
        ...response.data,
        data: {
          ...pageInfo,
          items: (pageInfo.list || []).map(normalizeItem),
          total: pageInfo.total || 0
        }
      };
    } catch (error) {
      console.error('Get clothing list failed:', error);
      throw error;
    }
  },

  updateClothing: async (itemId, clothing) => {
    try {
      const response = await apiClient.put(`/clothings/${itemId}`, toUpdatePayload(clothing));
      return response.data;
    } catch (error) {
      console.error('Update clothing failed:', error);
      throw error;
    }
  },

  deleteClothing: async (itemId) => {
    try {
      const response = await apiClient.delete(`/clothings/${itemId}`);
      return response.data;
    } catch (error) {
      console.error('Delete clothing failed:', error);
      throw error;
    }
  },

  uploadImage: async (_itemId, image) => {
    try {
      const formData = new FormData();
      formData.append('file', image);
      const response = await apiClient.post('/images/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });
      return response.data;
    } catch (error) {
      console.error('Upload image failed:', error);
      throw error;
    }
  },

  setFrequency: async (itemId, frequency) => {
    try {
      const response = await apiClient.put(`/clothings/${itemId}/frequency`, {
        frequencyLevel: frequency
      });
      return response.data;
    } catch (error) {
      console.error('Set frequency failed:', error);
      throw error;
    }
  }
};
