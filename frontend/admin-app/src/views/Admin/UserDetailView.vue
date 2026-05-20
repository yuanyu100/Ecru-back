<template>
  <div class="admin-page">
    <div class="detail-page-head">
      <button class="secondary-button" type="button" @click="$router.push('/users')">返回列表</button>
      <h2>用户详情</h2>
    </div>

    <div v-if="loading" class="panel-card">
      <p class="empty-tip">正在加载用户详情...</p>
    </div>

    <div v-else-if="errorMessage" class="panel-card">
      <p class="empty-tip">{{ errorMessage }}</p>
    </div>

    <div v-else-if="!detail.user" class="panel-card">
      <p class="empty-tip">未找到用户详情数据。</p>
    </div>

    <template v-else>
      <div class="stats-grid">
        <article class="stat-card">
          <span class="stat-label">用户名</span>
          <strong>{{ detail.user.username || '-' }}</strong>
        </article>
        <article class="stat-card">
          <span class="stat-label">昵称</span>
          <strong>{{ detail.user.nickname || '-' }}</strong>
        </article>
        <article class="stat-card">
          <span class="stat-label">衣物数量</span>
          <strong>{{ detail.clothingCount ?? 0 }}</strong>
        </article>
        <article class="stat-card">
          <span class="stat-label">账号状态</span>
          <strong>
            <span class="badge" :class="detail.user.status === 1 ? 'badge-green' : 'badge-red'">
              {{ detail.user.status === 1 ? '正常' : '禁用' }}
            </span>
          </strong>
        </article>
      </div>

      <section class="panel-card">
        <div class="panel-head">
          <h2>基础资料</h2>
        </div>
        <div class="detail-grid">
          <div class="info-item"><span>用户 ID</span><p>{{ detail.user.userId ?? '-' }}</p></div>
          <div class="info-item"><span>邮箱</span><p>{{ detail.user.email || '-' }}</p></div>
          <div class="info-item"><span>手机号</span><p>{{ detail.user.phone || '-' }}</p></div>
          <div class="info-item"><span>角色</span><p>{{ formatRole(detail.user.role) }}</p></div>
          <div class="info-item"><span>性别</span><p>{{ formatGender(detail.user.gender) }}</p></div>
          <div class="info-item"><span>生日</span><p>{{ formatDate(detail.user.birthday) }}</p></div>
          <div class="info-item"><span>注册时间</span><p>{{ formatDate(detail.user.createdAt) }}</p></div>
          <div class="info-item"><span>最后登录</span><p>{{ formatDate(detail.user.lastLoginAt) }}</p></div>
        </div>
      </section>

      <section class="panel-card">
        <div class="panel-head">
          <h2>风格档案</h2>
        </div>
        <div v-if="detail.styleProfile" class="detail-grid">
          <div class="info-item"><span>气质类型</span><p>{{ detail.styleProfile.temperamentType || '-' }}</p></div>
          <div class="info-item"><span>身高</span><p>{{ formatNumber(detail.styleProfile.heightCm, ' cm') }}</p></div>
          <div class="info-item"><span>体重</span><p>{{ formatNumber(detail.styleProfile.weightKg, ' kg') }}</p></div>
          <div class="info-item"><span>体型</span><p>{{ detail.styleProfile.bodyType || '-' }}</p></div>
          <div class="info-item"><span>肤色</span><p>{{ detail.styleProfile.skinTone || '-' }}</p></div>
          <div class="info-item"><span>职业</span><p>{{ detail.styleProfile.occupation || '-' }}</p></div>
          <div class="info-item full-width"><span>偏好风格</span><p>{{ detail.styleProfile.preferredStyles || '-' }}</p></div>
          <div class="info-item full-width"><span>回避风格</span><p>{{ detail.styleProfile.avoidedStyles || '-' }}</p></div>
          <div class="info-item full-width"><span>偏好颜色</span><p>{{ detail.styleProfile.preferredColors || '-' }}</p></div>
          <div class="info-item full-width"><span>回避颜色</span><p>{{ detail.styleProfile.avoidedColors || '-' }}</p></div>
          <div class="info-item full-width"><span>生活方式标签</span><p>{{ detail.styleProfile.lifestyleTags || '-' }}</p></div>
        </div>
        <p v-else class="empty-tip">该用户暂无风格档案。</p>
      </section>
    </template>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { adminApi } from '../../api/admin';

const route = useRoute();
const loading = ref(false);
const errorMessage = ref('');
const detail = reactive({
  user: null,
  styleProfile: null,
  clothingCount: 0
});

const formatDate = (value) => (value ? String(value).replace('T', ' ') : '-');
const formatNumber = (value, suffix = '') => (value || value === 0 ? `${value}${suffix}` : '-');
const formatRole = (role) => (String(role || '').toUpperCase().includes('ADMIN') ? '管理员' : '普通用户');
const formatGender = (gender) => {
  if (gender === 1) return '男';
  if (gender === 2) return '女';
  return '-';
};

onMounted(async () => {
  const userId = route.params.id;
  if (!userId) return;
  loading.value = true;
  errorMessage.value = '';
  try {
    const result = await adminApi.getUserDetail(userId);
    if (result?.code !== 200) {
      errorMessage.value = result?.message || '用户详情加载失败。';
      return;
    }
    detail.user = result?.data?.user || null;
    detail.styleProfile = result?.data?.styleProfile || null;
    detail.clothingCount = result?.data?.clothingCount ?? 0;
    if (!detail.user) {
      errorMessage.value = '用户详情接口未返回有效数据。';
    }
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || '用户详情加载失败。';
    console.error('Load user detail failed:', error);
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}

.info-item {
  padding: 14px 16px;
  border-radius: 16px;
  background: #f7f9fc;
  border: 1px solid #e8edf3;
}

.info-item.full-width {
  grid-column: 1 / -1;
}

.info-item span {
  display: block;
  color: #5f6b7a;
  font-size: 12px;
  margin-bottom: 8px;
}

.info-item p {
  margin: 0;
  color: #1b2430;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
