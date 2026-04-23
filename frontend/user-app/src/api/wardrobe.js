import { apiClient } from './client';

const fileToDataUrl = (file) =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = () => reject(new Error('Read image failed'));
    reader.readAsDataURL(file);
  });

const normalizeColor = (item = {}) => ({
  primary: item.primaryColor || item.color?.primary || '',
  secondary: item.secondaryColor || item.color?.secondary || ''
});

const normalizeItem = (item = {}) => ({
  ...item,
  itemId: item.id,
  color: normalizeColor(item),
  frequency: Number(item.frequencyLevel || item.frequency || 3),
  purchaseChannel: item.purchaseLink || item.purchaseChannel || '',
  purchaseTime: item.purchaseDate || item.purchaseTime || '',
  createdTime: item.createdAt || item.createdTime || '',
  updatedTime: item.updatedAt || item.updatedTime || ''
});

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
  autoRecognize: Boolean(clothing.autoRecognize)
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
  imageUrl: clothing.imageUrl,
  frequencyLevel: Number(clothing.frequency || clothing.frequencyLevel || 3)
});

export const wardrobeApi = {
  async addClothing(clothing) {
    const response = await apiClient.post('/clothings', toCreatePayload(clothing));
    return {
      ...response.data,
      data: normalizeItem(response.data?.data)
    };
  },

  async getClothingList(params = {}) {
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
  },

  async getClothingDetail(itemId) {
    const response = await apiClient.get(`/clothings/${itemId}`);
    return {
      ...response.data,
      data: normalizeItem(response.data?.data)
    };
  },

  async updateClothing(itemId, clothing) {
    const response = await apiClient.put(`/clothings/${itemId}`, toUpdatePayload(clothing));
    return {
      ...response.data,
      data: normalizeItem(response.data?.data)
    };
  },

  async deleteClothing(itemId, force = false) {
    const response = await apiClient.delete(`/clothings/${itemId}`, {
      params: { force }
    });
    return response.data;
  },

  async uploadImage(_itemId, image) {
    const data = await fileToDataUrl(image);
    const response = await apiClient.post('/clothings/images/upload-base64', {
      filename: image?.name || 'image.jpg',
      contentType: image?.type || 'image/jpeg',
      data
    });
    return response.data;
  },

  async setFrequency(itemId, frequency) {
    const response = await apiClient.put(`/clothings/${itemId}/frequency`, {
      frequencyLevel: frequency
    });
    return response.data;
  }
};
