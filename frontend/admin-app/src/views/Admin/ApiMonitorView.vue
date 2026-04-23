<template>
  <div class="admin-page">
    <div class="stats-grid">
      <article class="stat-card">
        <span class="stat-label">今日调用数</span>
        <strong>{{ dashboard.todayTotalCalls || 0 }}</strong>
      </article>
      <article class="stat-card">
        <span class="stat-label">今日成功率</span>
        <strong>{{ dashboard.todaySuccessRate || 0 }}%</strong>
      </article>
      <article class="stat-card">
        <span class="stat-label">平均响应</span>
        <strong>{{ dashboard.todayAvgResponseTime || 0 }} ms</strong>
      </article>
      <article class="stat-card">
        <span class="stat-label">总 Token</span>
        <strong>{{ dashboard.todayTotalTokens || 0 }}</strong>
      </article>
    </div>

    <section class="panel-card">
      <div class="panel-head">
        <div>
          <h2>最近调用记录</h2>
          <p class="panel-subtitle">来自 `/ai-monitor/recent-calls`。</p>
        </div>
      </div>

      <div v-if="records.length" class="table-shell">
        <table class="data-table">
          <thead>
            <tr>
              <th>时间</th>
              <th>场景</th>
              <th>模型</th>
              <th>用户</th>
              <th>状态</th>
              <th>耗时</th>
              <th>HTTP</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="record in records" :key="record.id">
              <td>{{ formatDate(record.createdAt) }}</td>
              <td>{{ record.scene || '-' }}</td>
              <td>{{ record.model || '-' }}</td>
              <td>{{ record.userId ?? '-' }}</td>
              <td>
                <span class="badge" :class="record.status === 1 ? 'badge-green' : 'badge-red'">
                  {{ record.status === 1 ? '成功' : '失败' }}
                </span>
              </td>
              <td>{{ record.responseTime ?? 0 }} ms</td>
              <td>{{ record.httpCode ?? '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else class="empty-tip">暂无监控数据。</p>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { monitorApi } from '../../api/monitor';

const dashboard = reactive({
  todayTotalCalls: 0,
  todaySuccessRate: 0,
  todayAvgResponseTime: 0,
  todayTotalTokens: 0
});
const records = ref([]);

const formatDate = (value) => (value ? String(value).replace('T', ' ') : '-');

const loadMonitor = async () => {
  const [dashboardResult, recordsResult] = await Promise.all([
    monitorApi.getDashboard().catch(() => null),
    monitorApi.getRecentCalls(20).catch(() => null)
  ]);

  if (dashboardResult?.success && dashboardResult.data) {
    Object.assign(dashboard, dashboardResult.data);
  }

  if (recordsResult?.success) {
    records.value = recordsResult.data || [];
  }
};

onMounted(loadMonitor);
</script>
