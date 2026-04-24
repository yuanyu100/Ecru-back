<template>
  <div class="admin-page">
    <section class="panel-card">
      <div class="panel-head">
        <div>
          <h2>AI Monitor</h2>
          <p class="panel-subtitle">Dashboard, trends, latency and failure distribution.</p>
        </div>
        <div class="toolbar">
          <span class="panel-subtitle">{{ loading ? 'Refreshing...' : `Updated ${lastLoadedAt}` }}</span>
          <button class="secondary-button" type="button" :disabled="loading" @click="loadMonitor">
            Refresh
          </button>
        </div>
      </div>

      <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

      <div class="stats-grid">
        <article class="stat-card">
          <span class="stat-label">Today Calls</span>
          <strong>{{ dashboard.todayTotalCalls || 0 }}</strong>
        </article>
        <article class="stat-card">
          <span class="stat-label">Success Rate</span>
          <strong>{{ formatPercent(dashboard.todaySuccessRate) }}</strong>
        </article>
        <article class="stat-card">
          <span class="stat-label">Avg Response</span>
          <strong>{{ formatMs(dashboard.todayAvgResponseTime) }}</strong>
        </article>
        <article class="stat-card">
          <span class="stat-label">Today Tokens</span>
          <strong>{{ dashboard.todayTotalTokens || 0 }}</strong>
        </article>
      </div>
    </section>

    <div class="panel-grid">
      <section class="panel-card">
        <div class="panel-head">
          <div>
            <h2>7-Day Trend</h2>
            <p class="panel-subtitle">Aggregated by day.</p>
          </div>
        </div>

        <div class="table-shell">
          <table class="data-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Calls</th>
                <th>Success</th>
                <th>Avg Response</th>
                <th>Tokens</th>
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
          <div>
            <h2>Today by Hour</h2>
            <p class="panel-subtitle">24-hour breakdown.</p>
          </div>
        </div>

        <div class="table-shell">
          <table class="data-table">
            <thead>
              <tr>
                <th>Hour</th>
                <th>Calls</th>
                <th>Success</th>
                <th>Avg Response</th>
                <th>P95</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in hourlyTrend" :key="item.hour">
                <td>{{ String(item.hour).padStart(2, '0') }}:00</td>
                <td>{{ item.totalCalls || 0 }}</td>
                <td>{{ formatPercent(item.successRate) }}</td>
                <td>{{ formatMs(item.avgResponseTime) }}</td>
                <td>{{ formatMs(item.p95ResponseTime) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>

    <div class="panel-grid">
      <section class="panel-card">
        <div class="panel-head">
          <div>
            <h2>Scene Distribution</h2>
            <p class="panel-subtitle">Grouped by business scene.</p>
          </div>
        </div>

        <div v-if="sceneStats.length" class="table-shell">
          <table class="data-table">
            <thead>
              <tr>
                <th>Scene</th>
                <th>Calls</th>
                <th>Success</th>
                <th>Avg Response</th>
                <th>Tokens</th>
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
        <p v-else class="empty-tip">No scene statistics yet.</p>
      </section>

      <section class="panel-card">
        <div class="panel-head">
          <div>
            <h2>Model Distribution</h2>
            <p class="panel-subtitle">Grouped by model name.</p>
          </div>
        </div>

        <div v-if="modelStats.length" class="table-shell">
          <table class="data-table">
            <thead>
              <tr>
                <th>Model</th>
                <th>Calls</th>
                <th>Success</th>
                <th>Avg Response</th>
                <th>Tokens</th>
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
        <p v-else class="empty-tip">No model statistics yet.</p>
      </section>
    </div>

    <div class="panel-grid bottom-grid">
      <section class="panel-card">
        <div class="panel-head">
          <div>
            <h2>Error Distribution</h2>
            <p class="panel-subtitle">Failures in the last 24 hours.</p>
          </div>
        </div>

        <div v-if="errorDistribution.length" class="chip-grid">
          <article v-for="item in errorDistribution" :key="`${item.error_type}-${item.count}`" class="metric-chip">
            <span>{{ item.error_type || 'UNKNOWN' }}</span>
            <strong>{{ item.count || 0 }}</strong>
          </article>
        </div>
        <p v-else class="empty-tip">No failures recorded in the last 24 hours.</p>
      </section>

      <section class="panel-card">
        <div class="panel-head">
          <div>
            <h2>Recent Calls</h2>
            <p class="panel-subtitle">Latest AI API requests.</p>
          </div>
        </div>

        <div v-if="records.length" class="table-shell">
          <table class="data-table">
            <thead>
              <tr>
                <th>Time</th>
                <th>Scene</th>
                <th>Model</th>
                <th>Status</th>
                <th>Latency</th>
                <th>HTTP</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="record in records" :key="record.id">
                <td>{{ formatDate(record.createdAt) }}</td>
                <td>{{ record.scene || 'UNKNOWN' }}</td>
                <td>{{ record.model || 'UNKNOWN' }}</td>
                <td>
                  <span class="badge" :class="record.status === 1 ? 'badge-green' : 'badge-red'">
                    {{ record.status === 1 ? 'SUCCESS' : 'FAILED' }}
                  </span>
                </td>
                <td>{{ formatMs(record.responseTime) }}</td>
                <td>{{ record.httpCode ?? '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-else class="empty-tip">No recent call records.</p>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { monitorApi } from '../../api/monitor';

const loading = ref(false);
const errorMessage = ref('');
const lastLoadedAt = ref('never');
const records = ref([]);
const weeklyTrend = ref([]);
const hourlyTrend = ref([]);
const sceneStats = ref([]);
const modelStats = ref([]);
const errorDistribution = ref([]);

const dashboard = reactive({
  todayTotalCalls: 0,
  todaySuccessRate: 0,
  todayAvgResponseTime: 0,
  todayTotalTokens: 0
});

const formatDate = (value) => (value ? String(value).replace('T', ' ') : '-');

const formatMs = (value) => `${Number(value || 0).toFixed(0)} ms`;

const formatPercent = (value) => `${Number(value || 0).toFixed(2)}%`;

const loadMonitor = async () => {
  loading.value = true;
  errorMessage.value = '';

  try {
    const result = await monitorApi.getDashboard();
    if (!result?.success || !result.data) {
      throw new Error(result?.message || 'Failed to load monitor dashboard');
    }

    Object.assign(dashboard, {
      todayTotalCalls: result.data.todayTotalCalls || 0,
      todaySuccessRate: result.data.todaySuccessRate || 0,
      todayAvgResponseTime: result.data.todayAvgResponseTime || 0,
      todayTotalTokens: result.data.todayTotalTokens || 0
    });

    weeklyTrend.value = result.data.weeklyTrend || [];
    hourlyTrend.value = result.data.hourlyTrend || [];
    sceneStats.value = result.data.sceneStats || [];
    modelStats.value = result.data.modelStats || [];
    errorDistribution.value = result.data.errorDistribution || [];
    records.value = result.data.recentCalls || [];
    lastLoadedAt.value = new Date().toLocaleString('zh-CN');
  } catch (error) {
    console.error('Load monitor dashboard failed:', error);
    errorMessage.value = error.message || 'Failed to load monitor dashboard';
  } finally {
    loading.value = false;
  }
};

onMounted(loadMonitor);
</script>

<style scoped>
.bottom-grid {
  align-items: start;
}

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

.error-text {
  margin: 0 0 16px;
  color: #b91c1c;
}
</style>
