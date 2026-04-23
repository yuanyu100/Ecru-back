<template>
  <div class="detail-page">
    <header class="detail-header">
      <button class="ghost-button" type="button" @click="goBack">返回</button>
      <div>
        <p class="eyebrow">Wardrobe Detail</p>
        <h1>衣物详情</h1>
      </div>
    </header>

    <div v-if="isLoading" class="detail-card">加载中...</div>
    <div v-else class="detail-card">
      <div class="image-panel">
        <img :src="clothingData.imageUrl || fallbackImage" :alt="clothingData.name || '衣物图片'" />
      </div>

      <div class="info-panel">
        <h2>{{ clothingData.name || '未命名衣物' }}</h2>
        <p class="subline">{{ clothingData.brand || '未填写品牌' }}</p>

        <div class="info-list">
          <div><span>分类</span><strong>{{ clothingData.category || '-' }}</strong></div>
          <div><span>颜色</span><strong>{{ clothingData.color?.primary || '-' }}{{ clothingData.color?.secondary ? ` / ${clothingData.color.secondary}` : '' }}</strong></div>
          <div><span>材质</span><strong>{{ clothingData.material || '-' }}</strong></div>
          <div><span>尺码</span><strong>{{ clothingData.size || '-' }}</strong></div>
          <div><span>购买日期</span><strong>{{ clothingData.purchaseTime || '-' }}</strong></div>
          <div><span>购买渠道</span><strong>{{ clothingData.purchaseChannel || '-' }}</strong></div>
          <div><span>搭配频率</span><strong>{{ clothingData.frequency || 3 }}</strong></div>
          <div><span>穿着次数</span><strong>{{ clothingData.wearCount ?? 0 }}</strong></div>
          <div><span>来源</span><strong>{{ clothingData.sourceType || 'manual' }}</strong></div>
          <div><span>创建时间</span><strong>{{ formatDate(clothingData.createdTime) }}</strong></div>
        </div>

        <div class="action-row">
          <button class="ghost-button" type="button" @click="navigateToEdit">编辑</button>
          <button class="danger-button" type="button" @click="deleteClothing">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { wardrobeApi } from '../api/wardrobe';

const router = useRouter();
const route = useRoute();
const itemId = route.params.id;
const isLoading = ref(true);
const clothingData = ref({});
const fallbackImage = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="320" height="420"><rect width="100%" height="100%" fill="%23f0e4cf"/><text x="50%" y="50%" text-anchor="middle" fill="%238b7355" font-size="24">No Image</text></svg>';

const fetchClothingData = async () => {
  try {
    const result = await wardrobeApi.getClothingDetail(itemId);
    clothingData.value = result.data || {};
  } catch (error) {
    console.error('Load clothing detail failed:', error);
    alert(error.response?.data?.message || '获取衣物详情失败');
    router.push('/wardrobe');
  } finally {
    isLoading.value = false;
  }
};

const formatDate = (value) => (value ? String(value).replace('T', ' ') : '-');

const navigateToEdit = () => {
  router.push(`/wardrobe/edit/${itemId}`);
};

const deleteClothing = async () => {
  if (!confirm('确认删除这件衣物吗？')) {
    return;
  }

  try {
    await wardrobeApi.deleteClothing(itemId);
    router.push('/wardrobe');
  } catch (error) {
    console.error('Delete clothing failed:', error);
    alert(error.response?.data?.message || '删除衣物失败');
  }
};

const goBack = () => {
  router.push('/wardrobe');
};

onMounted(fetchClothingData);
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  padding: 20px 16px 92px;
  background: linear-gradient(180deg, #f7efdf 0%, #efe1c8 100%);
}

.detail-header {
  display: flex;
  gap: 14px;
  align-items: center;
  margin-bottom: 18px;
}

.eyebrow {
  margin-bottom: 4px;
  font-size: 12px;
  color: #8f6a37;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.detail-card {
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 251, 244, 0.92);
  border: 1px solid rgba(145, 104, 49, 0.14);
  box-shadow: 0 18px 44px rgba(109, 78, 38, 0.08);
}

.image-panel {
  overflow: hidden;
  border-radius: 18px;
  background: linear-gradient(180deg, #f6ead4 0%, #ede0c7 100%);
}

.image-panel img {
  width: 100%;
  aspect-ratio: 3 / 4;
  object-fit: cover;
}

.info-panel {
  margin-top: 18px;
}

.info-panel h2 {
  color: #5d4523;
}

.subline {
  margin-top: 4px;
  color: #8d6e46;
}

.info-list {
  display: grid;
  gap: 10px;
  margin-top: 18px;
}

.info-list div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(145, 104, 49, 0.12);
}

.info-list span {
  color: #8d6e46;
}

.info-list strong {
  color: #5d4523;
  text-align: right;
}

.action-row {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.ghost-button,
.danger-button {
  border: none;
  border-radius: 999px;
  padding: 10px 16px;
  cursor: pointer;
}

.ghost-button {
  background: #ead7b8;
  color: #5d4523;
}

.danger-button {
  background: #c84e44;
  color: #fff8ef;
}

@media (min-width: 768px) {
  .detail-page {
    padding: 28px 28px 40px;
  }

  .detail-card {
    display: grid;
    grid-template-columns: minmax(320px, 420px) 1fr;
    gap: 24px;
    align-items: start;
  }

  .info-panel {
    margin-top: 0;
  }
}
</style>
