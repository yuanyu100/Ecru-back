<template>
  <div class="admin-page">
    <section class="panel-card">
      <div class="panel-head">
        <h2>AI 监控</h2>
        <div class="toolbar">
          <button class="secondary-button" type="button" :disabled="loading" @click="loadMonitor">
            {{ loading ? '刷新中...' : '刷新' }}
          </button>
        </div>
      </div>

      <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

      <div class="stats-grid">
        <article class="stat-card">
          <span class="stat-label">近 24 小时调用</span>
          <strong>{{ dashboard.recent24hTotalCalls || 0 }}</strong>
        </article>
        <article class="stat-card">
          <span class="stat-label">近 24 小时成功率</span>
          <strong>{{ formatPercent(dashboard.recent24hSuccessRate) }}</strong>
        </article>
        <article class="stat-card">
          <span class="stat-label">近 24 小时平均耗时</span>
          <strong>{{ formatMs(dashboard.recent24hAvgResponseTime) }}</strong>
        </article>
        <article class="stat-card">
          <span class="stat-label">近 24 小时令牌消耗</span>
          <strong>{{ dashboard.recent24hTotalTokens || 0 }}</strong>
        </article>
      </div>
    </section>

    <section class="panel-card">
      <div class="panel-head">
        <h2>最近调用</h2>
      </div>

      <div v-if="records.length" class="table-shell">
        <table class="data-table">
          <thead>
            <tr>
              <th>时间</th>
              <th>场景</th>
              <th>模型</th>
              <th>状态</th>
              <th>耗时</th>
              <th>状态码</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="record in records" :key="record.id">
              <td>{{ formatDate(record.createdAt) }}</td>
              <td>{{ record.scene || '未知场景' }}</td>
              <td>{{ record.model || '未知模型' }}</td>
              <td>
                <span class="badge" :class="record.status === 1 ? 'badge-green' : 'badge-red'">
                  {{ record.status === 1 ? '成功' : '失败' }}
                </span>
              </td>
              <td>{{ formatMs(record.responseTime) }}</td>
              <td>{{ record.httpCode ?? '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else class="empty-tip">暂无最近调用记录。</p>
    </section>

    <section class="panel-card">
      <div class="panel-head">
        <h2>按用户</h2>
      </div>

      <div v-if="userStats.length" class="table-shell">
        <table class="data-table">
          <thead>
            <tr>
              <th>用户</th>
              <th>角色</th>
              <th>调用次数</th>
              <th>成功次数</th>
              <th>失败次数</th>
              <th>成功率</th>
              <th>平均耗时</th>
              <th>令牌消耗</th>
              <th>最近调用</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in userStats" :key="`${item.userId}-${item.username}`">
              <td>
                <div class="user-cell">
                  <strong>{{ item.nickname || item.username || '未知用户' }}</strong>
                  <span>{{ item.username || '-' }}</span>
                </div>
              </td>
              <td>{{ formatRole(item.role) }}</td>
              <td>{{ item.totalCalls || 0 }}</td>
              <td>{{ item.successCalls || 0 }}</td>
              <td>{{ item.failedCalls || 0 }}</td>
              <td>{{ formatPercent(item.successRate) }}</td>
              <td>{{ formatMs(item.avgResponseTime) }}</td>
              <td>{{ item.totalTokens || 0 }}</td>
              <td>{{ formatDate(item.lastCallAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else class="empty-tip">近 24 小时暂无可展示的用户调用数据。</p>
    </section>

    <div class="panel-grid">
      <section class="panel-card">
        <div class="panel-head">
          <h2>近 7 天</h2>
        </div>

        <div class="table-shell">
          <table class="data-table">
            <thead>
              <tr>
                <th>日期</th>
                <th>调用次数</th>
                <th>成功率</th>
                <th>平均耗时</th>
                <th>令牌消耗</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in weeklyTrend" :key="item.date">
                <td>{{ item.date }}</td>
                <td>{{ item.totalCalls || 0 }}</td>
                <td>{{ formatPercent(item.successRate) }}</td>
                <td>{{ formatMs(item.avgResponseTime) }}</td>
                <td>{{ item.totalTokens || 0 }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="panel-card">
        <div class="panel-head">
          <h2>分时（有调用）</h2>
        </div>

        <div class="table-shell">
          <table class="data-table">
            <thead>
              <tr>
                <th>小时</th>
                <th>调用次数</th>
                <th>成功率</th>
                <th>平均耗时</th>
                <th>P95 耗时</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in activeHourlyTrend" :key="item.hour">
                <td>{{ String(item.hour).padStart(2, '0') }}:00</td>
                <td>{{ item.totalCalls || 0 }}</td>
                <td>{{ formatPercent(item.successRate) }}</td>
                <td>{{ formatMs(item.avgResponseTime) }}</td>
                <td>{{ formatMs(item.p95ResponseTime) }}</td>
              </tr>
              <tr v-if="!activeHourlyTrend.length">
                <td colspan="5" style="text-align:center;color:#7c8796;">暂无分时数据</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>

    <div class="panel-grid">
      <section class="panel-card">
        <div class="panel-head">
          <h2>按场景</h2>
        </div>

        <div v-if="sceneStats.length" class="table-shell">
          <table class="data-table">
            <thead>
              <tr>
                <th>场景</th>
                <th>调用次数</th>
                <th>成功率</th>
                <th>平均耗时</th>
                <th>令牌消耗</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in sceneStats" :key="item.scene">
                <td>{{ item.scene }}</td>
                <td>{{ item.totalCalls || 0 }}</td>
                <td>{{ formatPercent(item.successRate) }}</td>
                <td>{{ formatMs(item.avgResponseTime) }}</td>
                <td>{{ item.totalTokens || 0 }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-else class="empty-tip">近 24 小时暂无场景统计数据。</p>
      </section>

      <section class="panel-card">
        <div class="panel-head">
          <h2>按模型</h2>
        </div>

        <div v-if="modelStats.length" class="table-shell">
          <table class="data-table">
            <thead>
              <tr>
                <th>模型</th>
                <th>调用次数</th>
                <th>成功率</th>
                <th>平均耗时</th>
                <th>Token</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in modelStats" :key="item.model">
                <td>{{ item.model }}</td>
                <td>{{ item.totalCalls || 0 }}</td>
                <td>{{ formatPercent(item.successRate) }}</td>
                <td>{{ formatMs(item.avgResponseTime) }}</td>
                <td>{{ item.totalTokens || 0 }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-else class="empty-tip">近 24 小时暂无模型统计数据。</p>
      </section>
    </div>

    <section class="panel-card">
        <div class="panel-head">
          <h2>错误</h2>
        </div>

        <div v-if="errorDistribution.length" class="chip-grid">
          <article v-for="item in errorDistribution" :key="`${item.error_type}-${item.count}`" class="metric-chip">
            <span>{{ item.error_type || '未知错误' }}</span>
            <strong>{{ item.count || 0 }}</strong>
          </article>
        </div>
        <p v-else class="empty-tip">近 24 小时没有失败调用。</p>
      </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { monitorApi } from '../../api/monitor';

const loading = ref(false);
const errorMessage = ref('');
const records = ref([]);
const weeklyTrend = ref([]);
const hourlyTrend = ref([]);
const sceneStats = ref([]);
const modelStats = ref([]);
const errorDistribution = ref([]);
const userStats = ref([]);

const dashboard = reactive({
  todayTotalCalls: 0,
  todaySuccessRate: 0,
  todayAvgResponseTime: 0,
  todayTotalTokens: 0,
  recent24hTotalCalls: 0,
  recent24hSuccessRate: 0,
  recent24hAvgResponseTime: 0,
  recent24hTotalTokens: 0
});

const formatDate = (value) => (value ? String(value).replace('T', ' ') : '-');
const formatMs = (value) => `${Number(value || 0).toFixed(0)} ms`;
const formatPercent = (value) => `${Number(value || 0).toFixed(2)}%`;
const formatRole = (role) => (String(role || '').toUpperCase().includes('ADMIN') ? '管理员' : '普通用户');

const activeHourlyTrend = computed(() => hourlyTrend.value.filter((item) => (item.totalCalls || 0) > 0));

const loadMonitor = async () => {
  loading.value = true;
  errorMessage.value = '';

  try {
    const result = await monitorApi.getDashboard();
    if (!result?.success || !result.data) {
      throw new Error(result?.message || '加载 AI 监控数据失败');
    }

    Object.assign(dashboard, {
      todayTotalCalls: result.data.todayTotalCalls || 0,
      todaySuccessRate: result.data.todaySuccessRate || 0,
      todayAvgResponseTime: result.data.todayAvgResponseTime || 0,
      todayTotalTokens: result.data.todayTotalTokens || 0,
      recent24hTotalCalls: result.data.recent24hTotalCalls || 0,
      recent24hSuccessRate: result.data.recent24hSuccessRate || 0,
      recent24hAvgResponseTime: result.data.recent24hAvgResponseTime || 0,
      recent24hTotalTokens: result.data.recent24hTotalTokens || 0
    });

    weeklyTrend.value = result.data.weeklyTrend || [];
    hourlyTrend.value = result.data.hourlyTrend || [];
    sceneStats.value = result.data.sceneStats || [];
    modelStats.value = result.data.modelStats || [];
    errorDistribution.value = result.data.errorDistribution || [];
    records.value = result.data.recentCalls || [];
    userStats.value = result.data.userStats || [];
  } catch (error) {
    console.error('Load monitor dashboard failed:', error);
    errorMessage.value = error.message || '加载 AI 监控数据失败';
  } finally {
    loading.value = false;
  }
};

onMounted(loadMonitor);
</script>

<style scoped>
.chip-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
}

.metric-chip {
  padding: 14px;
  border-radius: 16px;
  background: #f7f9fc;
  border: 1px solid #e8edf3;
  display: grid;
  gap: 6px;
}

.metric-chip span {
  color: #5f6b7a;
  font-size: 13px;
}

.metric-chip strong {
  font-size: 24px;
  color: #18212f;
}

.user-cell {
  display: grid;
  gap: 4px;
}

.user-cell span {
  color: #5f6b7a;
  font-size: 12px;
}

.error-text {
  margin: 0 0 16px;
  color: #b91c1c;
}
</style>
