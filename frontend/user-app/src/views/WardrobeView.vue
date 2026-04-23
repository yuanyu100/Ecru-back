<template>
  <div class="wardrobe">
    <header class="wardrobe-header">
      <div>
        <p class="eyebrow">Ecru Wardrobe</p>
        <h1>我的衣橱</h1>
      </div>
      <button class="primary-button" type="button" @click="navigateToAdd">添加衣物</button>
    </header>

    <section class="filter-card">
      <select v-model="filters.category" @change="reloadList">
        <option value="">全部分类</option>
        <option value="上装">上装</option>
        <option value="下装">下装</option>
        <option value="鞋履">鞋履</option>
        <option value="配饰">配饰</option>
      </select>
      <input
        v-model.trim="filters.keyword"
        type="text"
        placeholder="搜索名称、分类或颜色"
        @keyup.enter="reloadList"
      />
      <button class="secondary-button" type="button" @click="reloadList">查询</button>
    </section>

    <div v-if="isLoading" class="state-card">加载中...</div>
    <div v-else-if="clothingList.length === 0" class="state-card">
      <p>当前还没有衣物记录。</p>
      <button class="primary-button" type="button" @click="navigateToAdd">去上传第一件</button>
    </div>
    <section v-else class="clothing-grid">
      <article
        v-for="item in clothingList"
        :key="item.itemId"
        class="clothing-card"
        @click="navigateToDetail(item.itemId)"
      >
        <div class="image-shell">
          <img
            :src="item.imageUrl || fallbackImage"
            :alt="item.name || '衣物图片'"
          />
        </div>
        <div class="card-body">
          <h3>{{ item.name || '未命名衣物' }}</h3>
          <p>{{ item.category || '未分类' }}</p>
          <div class="meta-row">
            <span>{{ item.color.primary || '未标注颜色' }}</span>
            <span>频率 {{ item.frequency }}</span>
          </div>
        </div>
      </article>
    </section>

    <footer v-if="totalPages > 1" class="pagination">
      <button type="button" class="secondary-button" :disabled="currentPage === 1" @click="changePage(currentPage - 1)">
        上一页
      </button>
      <span>{{ currentPage }} / {{ totalPages }}</span>
      <button
        type="button"
        class="secondary-button"
        :disabled="currentPage === totalPages"
        @click="changePage(currentPage + 1)"
      >
        下一页
      </button>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { wardrobeApi } from '../api/wardrobe';

const router = useRouter();
const clothingList = ref([]);
const isLoading = ref(false);
const currentPage = ref(1);
const pageSize = ref(12);
const totalItems = ref(0);
const fallbackImage = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="320" height="420"><rect width="100%" height="100%" fill="%23f0e4cf"/><text x="50%" y="50%" text-anchor="middle" fill="%238b7355" font-size="24">No Image</text></svg>';

const filters = reactive({
  category: '',
  keyword: ''
});

const totalPages = computed(() => Math.max(1, Math.ceil(totalItems.value / pageSize.value)));

const fetchClothingList = async () => {
  isLoading.value = true;
  try {
    const result = await wardrobeApi.getClothingList({
      page: currentPage.value,
      size: pageSize.value,
      category: filters.category || undefined,
      keyword: filters.keyword || undefined
    });

    clothingList.value = result.data?.items || [];
    totalItems.value = result.data?.total || 0;
  } catch (error) {
    console.error('Load wardrobe failed:', error);
    alert(error.response?.data?.message || '获取衣物列表失败');
  } finally {
    isLoading.value = false;
  }
};

const reloadList = () => {
  currentPage.value = 1;
  fetchClothingList();
};

const changePage = (page) => {
  currentPage.value = page;
  fetchClothingList();
};

const navigateToAdd = () => {
  router.push('/wardrobe/add');
};

const navigateToDetail = (itemId) => {
  router.push(`/wardrobe/detail/${itemId}`);
};

onMounted(fetchClothingList);
</script>

<style scoped>
.wardrobe {
  min-height: 100vh;
  padding: 20px 16px 92px;
  background:
    radial-gradient(circle at top, rgba(255, 244, 214, 0.9), transparent 32%),
    linear-gradient(180deg, #f8f0de 0%, #f2e7d1 100%);
}

.wardrobe-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.eyebrow {
  margin-bottom: 6px;
  color: #8f6a37;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.wardrobe-header h1 {
  color: #5d4523;
  font-size: 28px;
}

.filter-card,
.state-card {
  background: rgba(255, 250, 240, 0.88);
  border: 1px solid rgba(145, 104, 49, 0.14);
  border-radius: 20px;
  box-shadow: 0 14px 34px rgba(109, 78, 38, 0.08);
}

.filter-card {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
  padding: 16px;
  margin-bottom: 18px;
}

.filter-card select,
.filter-card input {
  width: 100%;
  border: 1px solid #d9c39b;
  border-radius: 14px;
  padding: 12px 14px;
  background: #fffdf8;
  color: #5d4523;
}

.state-card {
  padding: 32px 20px;
  text-align: center;
}

.clothing-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.clothing-card {
  overflow: hidden;
  border-radius: 20px;
  background: #fffdf8;
  border: 1px solid rgba(145, 104, 49, 0.14);
  box-shadow: 0 14px 26px rgba(109, 78, 38, 0.08);
  cursor: pointer;
}

.image-shell {
  aspect-ratio: 3 / 4;
  background: linear-gradient(180deg, #f6ead4 0%, #ede0c7 100%);
}

.image-shell img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-body {
  padding: 12px;
}

.card-body h3 {
  color: #5d4523;
  font-size: 15px;
  line-height: 1.4;
}

.card-body p,
.meta-row {
  color: #8d6e46;
  font-size: 12px;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 18px;
  color: #6f5430;
}

.primary-button,
.secondary-button {
  border: none;
  border-radius: 999px;
  padding: 10px 16px;
  cursor: pointer;
}

.primary-button {
  background: #6b4b1f;
  color: #fff8ef;
}

.secondary-button {
  background: #e6d1ad;
  color: #5d4523;
}

.secondary-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (min-width: 768px) {
  .wardrobe {
    padding: 28px 28px 40px;
  }

  .filter-card {
    grid-template-columns: 180px 1fr auto;
    align-items: center;
  }

  .clothing-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
</style>
