<template>
  <div class="import-page">
    <header class="import-header">
      <button class="icon-back" type="button" aria-label="返回" @click="goBack">
        <span></span>
      </button>
      <div>
        <p class="eyebrow">衣橱导入</p>
        <h1>从拼多多订单导入衣物</h1>
      </div>
    </header>

    <section class="import-card">
      <div class="mode-row">
        <button
          v-for="mode in modeOptions"
          :key="mode.value"
          type="button"
          :class="['mode-chip', activeMode === mode.value ? 'active' : '']"
          @click="activeMode = mode.value"
        >
          {{ mode.label }}
        </button>
      </div>

      <div v-if="activeMode === 'structured_json'" class="panel-stack">
        <div class="hint-panel">
          <strong>方式一：浏览器侧提取</strong>
          <p>在已登录的拼多多订单页打开浏览器控制台，执行下面脚本，再把输出 JSON 粘贴回来。</p>
          <button class="line-button" type="button" @click="copyExtractorScript">复制提取脚本</button>
        </div>

        <label class="field-block">
          <span>提取脚本</span>
          <textarea :value="extractorScript" readonly rows="10"></textarea>
        </label>

        <label class="field-block">
          <span>粘贴浏览器输出的 JSON</span>
          <textarea
            v-model="structuredPayload"
            rows="12"
            placeholder="把控制台输出的 JSON 粘贴到这里"
          ></textarea>
        </label>
      </div>

      <div v-else class="panel-stack">
        <div class="hint-panel">
          <strong>方式二：原始页面 / HAR</strong>
          <p>可以直接粘贴订单页 HTML，或者上传浏览器导出的 HAR 文件。HAR 通常比 HTML 更稳定。</p>
        </div>

        <div class="inline-fields">
          <label class="field-block compact">
            <span>原始数据类型</span>
            <select v-model="rawMode">
              <option value="auto">自动判断</option>
              <option value="html">页面 HTML</option>
              <option value="har">HAR JSON</option>
            </select>
          </label>

          <label class="field-block compact">
            <span>上传文件</span>
            <input type="file" accept=".json,.har,.html,.txt" @change="handleFileUpload" />
          </label>
        </div>

        <label class="field-block">
          <span>粘贴原始内容</span>
          <textarea
            v-model="rawPayload"
            rows="14"
            placeholder="粘贴 HTML / HAR / 订单接口 JSON"
          ></textarea>
        </label>
      </div>

      <div class="switch-row">
        <label><input v-model="onlyClothing" type="checkbox" /> 仅保留看起来像衣物的商品</label>
        <label><input v-model="autoRecognize" type="checkbox" /> 导入后自动尝试 AI 识别</label>
      </div>

      <div class="action-row">
        <button class="primary-button" type="button" :disabled="isPreviewing" @click="previewImport">
          {{ isPreviewing ? '解析中...' : '预览待导入商品' }}
        </button>
      </div>

      <p v-if="previewHint" class="preview-hint">{{ previewHint }}</p>
    </section>

    <section v-if="previewItems.length" class="import-card preview-card">
      <div class="preview-header">
        <div>
          <strong>预览结果</strong>
          <p>{{ selectedCount }} / {{ previewItems.length }} 项已选</p>
        </div>
        <div class="preview-actions">
          <button class="line-button" type="button" @click="toggleSelectAll(true)">全选可导入</button>
          <button class="line-button" type="button" @click="toggleSelectAll(false)">清空选择</button>
        </div>
      </div>

      <article v-for="(item, index) in previewItems" :key="item.orderId || item.productUrl || `${item.productName}-${index}`" class="preview-item">
        <label class="check-col">
          <input v-model="item.selected" type="checkbox" :disabled="item.duplicate" />
        </label>

        <div class="thumb-shell">
          <img :src="item.imageUrl || fallbackImage" :alt="item.productName || '商品图'" />
        </div>

        <div class="item-main">
          <div class="item-title-row">
            <strong>{{ item.productName || '未命名商品' }}</strong>
            <span :class="['status-badge', item.duplicate ? 'duplicate' : 'fresh']">
              {{ item.duplicate ? '已存在' : '可导入' }}
            </span>
          </div>

          <div class="meta-grid">
            <label>
              <span>分类</span>
              <select v-model="item.category">
                <option v-for="option in clothingCategoryOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
              </select>
            </label>

            <label>
              <span>材质</span>
              <input v-model.trim="item.material" type="text" placeholder="可选" />
            </label>

            <label>
              <span>尺码/规格</span>
              <input v-model.trim="item.size" type="text" placeholder="可选" />
            </label>

            <label>
              <span>品牌</span>
              <input v-model.trim="item.brand" type="text" placeholder="可选" />
            </label>
          </div>

          <div class="plain-list">
            <p>店铺：{{ item.shopName || '-' }}</p>
            <p>规格：{{ item.skuText || '-' }}</p>
            <p>下单日期：{{ item.orderTime || '-' }}</p>
            <p>价格：{{ formatPrice(item.price) }}</p>
            <p>订单号：{{ item.orderId || '-' }}</p>
          </div>
        </div>
      </article>

      <div class="action-row final">
        <button class="primary-button" type="button" :disabled="isImporting || selectedCount === 0" @click="importSelected">
          {{ isImporting ? '导入中...' : `导入选中的 ${selectedCount} 项` }}
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { wardrobeApi } from '../api/wardrobe';
import { clothingCategoryOptions } from '../constants/clothingCategories';

const router = useRouter();
const activeMode = ref('structured_json');
const rawMode = ref('auto');
const onlyClothing = ref(true);
const autoRecognize = ref(false);
const structuredPayload = ref('');
const rawPayload = ref('');
const isPreviewing = ref(false);
const isImporting = ref(false);
const previewItems = ref([]);
const previewHint = ref('');

const fallbackImage =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="280" height="360"><rect width="100%" height="100%" fill="%23efe6d8"/><text x="50%" y="50%" text-anchor="middle" fill="%2382725d" font-size="20">ITEM</text></svg>';

const modeOptions = [
  { label: '浏览器提取 JSON', value: 'structured_json' },
  { label: 'HTML / HAR', value: 'raw' }
];

const selectedCount = computed(() => previewItems.value.filter((item) => item.selected).length);

const extractorScript = String.raw`(() => {
  const nameKeys = ['goodsName', 'goods_name', 'itemName', 'item_name', 'productName', 'product_name', 'goodsTitle', 'goods_title', 'title', 'name'];
  const imageKeys = ['goodsImg', 'goods_img', 'goodsImage', 'goods_image', 'goodsImageUrl', 'goods_image_url', 'thumbUrl', 'thumb_url', 'hdThumbUrl', 'hd_thumb_url', 'imageUrl', 'image_url', 'coverUrl', 'cover_url', 'image', 'thumbnail'];
  const linkKeys = ['goodsLink', 'goods_link', 'goodsUrl', 'goods_url', 'productUrl', 'product_url', 'detailUrl', 'detail_url', 'linkUrl', 'link_url', 'url', 'pageUrl', 'page_url'];
  const orderTimeKeys = ['orderTime', 'order_time', 'createTime', 'create_time', 'createdAt', 'created_at', 'payTime', 'pay_time', 'orderDate', 'order_date'];
  const priceKeys = ['price', 'goodsPrice', 'goods_price', 'payAmount', 'pay_amount', 'amount', 'orderAmount', 'order_amount'];
  const shopKeys = ['mallName', 'mall_name', 'shopName', 'shop_name', 'storeName', 'store_name', 'merchantName'];
  const skuKeys = ['skuText', 'sku_text', 'skuSpec', 'sku_spec', 'goodsSpec', 'goods_spec', 'spec', 'specs', 'specDesc', 'spec_desc', 'sizeDesc', 'size_desc'];
  const orderIdKeys = ['orderSn', 'order_sn', 'orderId', 'order_id', 'groupOrderId', 'group_order_id'];
  const brandKeys = ['brand', 'brandName', 'brand_name'];
  const results = [];
  const seen = new Set();

  const text = (value) => {
    if (value == null) return null;
    if (Array.isArray(value)) return value.map(text).filter(Boolean).join(' / ') || null;
    if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') return String(value).trim() || null;
    return null;
  };

  const first = (obj, keys) => {
    for (const key of keys) {
      const value = text(obj?.[key]);
      if (value) return value;
    }
    return null;
  };

  const visit = (node, context = {}) => {
    if (!node) return;
    if (Array.isArray(node)) {
      node.forEach((child) => visit(child, context));
      return;
    }
    if (typeof node !== 'object') return;

    const next = {
      orderTime: first(node, orderTimeKeys) || context.orderTime || null,
      orderId: first(node, orderIdKeys) || context.orderId || null,
      shopName: first(node, shopKeys) || context.shopName || null,
      brand: first(node, brandKeys) || context.brand || null
    };

    const item = {
      productName: first(node, nameKeys),
      productUrl: first(node, linkKeys),
      imageUrl: first(node, imageKeys),
      orderTime: first(node, orderTimeKeys) || next.orderTime,
      price: (() => {
        const raw = first(node, priceKeys);
        if (!raw) return null;
        const normalized = raw.replace(/[¥￥,]/g, '');
        return /^-?\d+(\.\d+)?$/.test(normalized) ? Number(normalized) : null;
      })(),
      shopName: first(node, shopKeys) || next.shopName,
      skuText: first(node, skuKeys),
      orderId: first(node, orderIdKeys) || next.orderId,
      brand: first(node, brandKeys) || next.brand
    };

    if (item.productName && (item.imageUrl || item.productUrl || item.skuText || item.price != null)) {
      const key = [item.orderId, item.productUrl, item.imageUrl, item.productName].filter(Boolean).join('|');
      if (!seen.has(key)) {
        seen.add(key);
        results.push(item);
      }
    }

    Object.values(node).forEach((value) => visit(value, next));
  };

  const parseScripts = () => {
    const scripts = Array.from(document.querySelectorAll('script')).map((node) => node.textContent || '');
    scripts.forEach((content) => {
      const trimmed = content.trim();
      if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
        try { visit(JSON.parse(trimmed)); } catch (_) {}
      }
    });
  };

  const globals = ['rawData', '__NEXT_DATA__', '__INITIAL_STATE__', '__APP_DATA__', 'data'];
  globals.forEach((key) => {
    try {
      if (window[key]) visit(window[key]);
    } catch (_) {}
  });

  parseScripts();
  console.log(JSON.stringify({
    source: 'pinduoduo',
    extractedAt: new Date().toISOString(),
    items: results
  }, null, 2));
})();`;

const copyExtractorScript = async () => {
  try {
    await navigator.clipboard.writeText(extractorScript);
    alert('提取脚本已复制');
  } catch (error) {
    console.error('Copy extractor script failed:', error);
    alert('复制失败，请手动复制脚本');
  }
};

const handleFileUpload = async (event) => {
  const [file] = event.target.files || [];
  if (!file) {
    return;
  }

  try {
    rawPayload.value = await file.text();
  } catch (error) {
    console.error('Read import file failed:', error);
    alert('读取文件失败');
  }
};

const previewImport = async () => {
  const payload = activeMode.value === 'structured_json' ? structuredPayload.value.trim() : rawPayload.value.trim();
  if (!payload) {
    alert('先提供待解析的订单数据');
    return;
  }

  isPreviewing.value = true;
  previewHint.value = '';
  try {
    const result = await wardrobeApi.previewPinduoduoImport({
      mode: activeMode.value === 'structured_json' ? 'structured_json' : rawMode.value,
      payload,
      onlyClothing: onlyClothing.value
    });

    const items = result.data?.items || [];
    previewItems.value = items.map((item) => ({
      ...item,
      selected: !item.duplicate
    }));

    if (items.length === 0) {
      previewHint.value = onlyClothing.value
        ? '解析完成，但没有识别到可导入衣物。先取消“仅保留看起来像衣物的商品”再试一次。'
        : '解析完成，但没有识别到可导入商品。请检查粘贴内容是否完整，或改用 HTML / HAR 模式。';
      alert(previewHint.value);
      return;
    }

    const duplicateCount = items.filter((item) => item.duplicate).length;
    previewHint.value = `解析到 ${items.length} 条候选商品，${items.length - duplicateCount} 条可导入，${duplicateCount} 条疑似已存在。`;
  } catch (error) {
    console.error('Preview pinduoduo import failed:', error);
    alert(error.response?.data?.message || '解析导入内容失败');
  } finally {
    isPreviewing.value = false;
  }
};

const toggleSelectAll = (checked) => {
  previewItems.value = previewItems.value.map((item) => ({
    ...item,
    selected: checked ? !item.duplicate : false
  }));
};

const importSelected = async () => {
  const items = previewItems.value
    .filter((item) => item.selected)
    .map((item) => ({
      productName: item.productName,
      productUrl: item.productUrl,
      imageUrl: item.imageUrl,
      orderTime: item.orderTime,
      price: item.price,
      shopName: item.shopName,
      skuText: item.skuText,
      orderId: item.orderId,
      brand: item.brand,
      category: item.category,
      material: item.material,
      size: item.size
    }));

  if (items.length === 0) {
    alert('先选择至少一项商品');
    return;
  }

  isImporting.value = true;
  try {
    const result = await wardrobeApi.importPinduoduoItems(items, {
      autoRecognize: autoRecognize.value,
      skipExisting: true
    });

    const summary = result.data || {};
    alert(`导入完成\n新增 ${summary.created || 0} 项，跳过 ${summary.skipped || 0} 项，失败 ${summary.failed || 0} 项`);
    router.push('/wardrobe');
  } catch (error) {
    console.error('Import selected pinduoduo items failed:', error);
    alert(error.response?.data?.message || '导入失败');
  } finally {
    isImporting.value = false;
  }
};

const formatPrice = (price) => {
  if (price == null || price === '') {
    return '-';
  }
  return `¥${Number(price).toFixed(2)}`;
};

const goBack = () => {
  router.push('/wardrobe');
};
</script>

<style scoped>
.import-page {
  min-height: 100vh;
  padding: 20px 16px 92px;
  background: linear-gradient(180deg, var(--bg-base) 0%, var(--bg-soft) 100%);
}

.import-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
}

.icon-back {
  display: inline-grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 1px solid var(--line-soft);
  border-radius: 50%;
  background: var(--surface-strong);
  cursor: pointer;
}

.icon-back span {
  width: 10px;
  height: 10px;
  border-left: 1.5px solid var(--accent-strong);
  border-bottom: 1.5px solid var(--accent-strong);
  transform: rotate(45deg);
  margin-left: 4px;
}

.eyebrow {
  margin-bottom: 4px;
  font-size: 12px;
  color: var(--text-soft);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.import-header h1,
.hint-panel strong,
.preview-header strong {
  color: var(--text-main);
}

.import-card {
  padding: 18px;
  border-radius: 24px;
  background: var(--surface);
  border: 1px solid var(--line-soft);
  box-shadow: var(--shadow-card);
}

.import-card + .import-card {
  margin-top: 16px;
}

.mode-row,
.switch-row,
.preview-actions,
.action-row,
.item-title-row {
  display: flex;
  gap: 10px;
}

.mode-row {
  flex-wrap: wrap;
}

.mode-chip,
.line-button,
.primary-button {
  border: none;
  border-radius: 999px;
  cursor: pointer;
}

.mode-chip,
.line-button {
  padding: 10px 14px;
  background: var(--bg-soft);
  color: var(--text-soft);
}

.mode-chip.active,
.primary-button {
  background: var(--accent-strong);
  color: var(--surface-strong);
}

.panel-stack {
  display: grid;
  gap: 14px;
  margin-top: 16px;
}

.hint-panel {
  padding: 14px 16px;
  border-radius: 18px;
  background: var(--accent-soft);
  color: var(--text-soft);
}

.hint-panel p {
  margin-top: 6px;
  line-height: 1.6;
}

.field-block {
  display: grid;
  gap: 8px;
  color: var(--text-soft);
  font-size: 14px;
}

.inline-fields {
  display: grid;
  gap: 14px;
}

.field-block textarea,
.field-block input,
.field-block select {
  width: 100%;
  border: 1px solid var(--line-strong);
  border-radius: 16px;
  padding: 12px 14px;
  background: var(--surface-strong);
  color: var(--text-main);
}

.field-block textarea {
  resize: vertical;
  line-height: 1.5;
}

.switch-row {
  flex-wrap: wrap;
  margin-top: 16px;
  color: var(--text-soft);
}

.switch-row label,
.check-col {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-row {
  justify-content: flex-end;
  margin-top: 18px;
}

.preview-hint {
  margin-top: 12px;
  color: var(--text-soft);
  font-size: 13px;
  line-height: 1.6;
}

.primary-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.preview-card {
  display: grid;
  gap: 14px;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.preview-header p,
.plain-list p {
  color: var(--text-soft);
}

.preview-item {
  display: grid;
  grid-template-columns: 24px 92px minmax(0, 1fr);
  gap: 14px;
  padding: 14px;
  border-radius: 20px;
  border: 1px solid var(--line-soft);
  background: var(--surface-strong);
}

.check-col {
  justify-content: center;
}

.thumb-shell {
  overflow: hidden;
  border-radius: 16px;
  background: var(--bg-soft);
}

.thumb-shell img {
  width: 100%;
  height: 100%;
  min-height: 120px;
  object-fit: cover;
}

.item-main {
  min-width: 0;
}

.item-title-row {
  justify-content: space-between;
  align-items: flex-start;
}

.item-title-row strong {
  color: var(--text-main);
  line-height: 1.5;
}

.status-badge {
  flex: none;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
}

.status-badge.fresh {
  background: rgba(61, 125, 76, 0.12);
  color: #2f7142;
}

.status-badge.duplicate {
  background: rgba(193, 83, 64, 0.12);
  color: #a44536;
}

.meta-grid {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.meta-grid label {
  display: grid;
  gap: 6px;
  color: var(--text-soft);
  font-size: 13px;
}

.plain-list {
  display: grid;
  gap: 6px;
  margin-top: 12px;
  font-size: 13px;
}

.final {
  margin-top: 4px;
}

@media (min-width: 768px) {
  .import-page {
    padding: 28px 28px 40px;
  }

  .inline-fields {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
