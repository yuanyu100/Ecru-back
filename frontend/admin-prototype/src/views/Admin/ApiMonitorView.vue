<template>
  <div class="api-monitor-view">
    <div class="monitor-header">
      <h2>API流量监控</h2>
      <div class="header-actions">
        <select v-model="timeRange" class="time-select">
          <option value="today">今日</option>
          <option value="week">本周</option>
          <option value="month">本月</option>
          <option value="year">本年</option>
        </select>
      </div>
    </div>
    
    <div class="monitor-cards">
      <div class="monitor-card">
        <div class="card-icon">📊</div>
        <div class="card-content">
          <h3>总请求数</h3>
          <p class="card-value">{{ totalRequests }}</p>
        </div>
      </div>
      <div class="monitor-card">
        <div class="card-icon">📈</div>
        <div class="card-content">
          <h3>平均响应时间</h3>
          <p class="card-value">{{ averageResponseTime }}ms</p>
        </div>
      </div>
      <div class="monitor-card">
        <div class="card-icon">❌</div>
        <div class="card-content">
          <h3>错误率</h3>
          <p class="card-value">{{ errorRate }}%</p>
        </div>
      </div>
      <div class="monitor-card">
        <div class="card-icon">🚀</div>
        <div class="card-content">
          <h3>最高QPS</h3>
          <p class="card-value">{{ maxQps }}</p>
        </div>
      </div>
    </div>
    
    <div class="monitor-charts">
      <div class="chart-container">
        <h3>请求趋势</h3>
        <div class="chart-placeholder">
          <div class="chart-line">
            <div class="line-point" style="left: 0%; top: 60%;"></div>
            <div class="line-point" style="left: 16.6%; top: 45%;"></div>
            <div class="line-point" style="left: 33.3%; top: 55%;"></div>
            <div class="line-point" style="left: 50%; top: 30%;"></div>
            <div class="line-point" style="left: 66.6%; top: 40%;"></div>
            <div class="line-point" style="left: 83.3%; top: 25%;"></div>
            <div class="line-point" style="left: 100%; top: 35%;"></div>
          </div>
        </div>
      </div>
      <div class="chart-container">
        <h3>API调用分布</h3>
        <div class="chart-placeholder pie-chart">
          <div class="pie-slice slice-1"></div>
          <div class="pie-slice slice-2"></div>
          <div class="pie-slice slice-3"></div>
          <div class="pie-slice slice-4"></div>
        </div>
      </div>
    </div>
    
    <div class="api-details">
      <h3>API调用详情</h3>
      <div class="api-table-container">
        <table class="api-table">
          <thead>
            <tr>
              <th>API路径</th>
              <th>请求方法</th>
              <th>调用次数</th>
              <th>平均响应时间</th>
              <th>成功率</th>
              <th>最近调用</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="api in apiDetails" :key="api.path">
              <td>{{ api.path }}</td>
              <td>
                <span class="method-badge" :class="api.method.toLowerCase()">
                  {{ api.method }}
                </span>
              </td>
              <td>{{ api.count }}</td>
              <td>{{ api.avgResponseTime }}ms</td>
              <td>{{ api.successRate }}%</td>
              <td>{{ api.lastCalled }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue';
import { authApi } from '../../api/auth';

export default {
  name: 'ApiMonitorView',
  setup() {
    const timeRange = ref('today');
    const totalRequests = ref(1568);
    const averageResponseTime = ref(120);
    const errorRate = ref(2.5);
    const maxQps = ref(50);
    
    const allApiDetails = ref([
      { path: '/api/v1/users/login', method: 'POST', count: 256, avgResponseTime: 80, successRate: 98, lastCalled: '2024-01-01 10:30', userId: null },
      { path: '/api/v1/wardrobe/items', method: 'GET', count: 458, avgResponseTime: 100, successRate: 99, lastCalled: '2024-01-01 10:25', userId: 1 },
      { path: '/api/v1/wardrobe/items', method: 'POST', count: 123, avgResponseTime: 150, successRate: 97, lastCalled: '2024-01-01 10:20', userId: 2 },
      { path: '/api/v1/users', method: 'GET', count: 89, avgResponseTime: 90, successRate: 99, lastCalled: '2024-01-01 10:15', userId: 1 }
    ]);
    
    const isAdmin = computed(() => {
      const currentUser = authApi.getCurrentUser();
      return currentUser?.role === 'ADMIN';
    });
    
    const currentUserId = computed(() => {
      const currentUser = authApi.getCurrentUser();
      return currentUser?.id;
    });
    
    const apiDetails = computed(() => {
      if (isAdmin.value) {
        return allApiDetails.value;
      } else {
        // 普通用户只看到自己的API请求
        return allApiDetails.value.filter(api => api.userId === currentUserId.value);
      }
    });
    
    onMounted(() => {
      // 这里可以添加实际的数据获取逻辑
      console.log('API monitor view mounted');
    });
    
    return {
      timeRange,
      totalRequests,
      averageResponseTime,
      errorRate,
      maxQps,
      apiDetails
    };
  }
};
</script>

<style scoped>
.api-monitor-view {
  padding: 20px;
}

.monitor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.monitor-header h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.time-select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.monitor-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.monitor-card {
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 20px;
}

.card-icon {
  font-size: 32px;
  width: 60px;
  height: 60px;
  background-color: #f0f4f8;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-content h3 {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.card-value {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.monitor-charts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.chart-container {
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.chart-container h3 {
  margin: 0 0 20px 0;
  font-size: 16px;
  color: #333;
  font-weight: 600;
}

.chart-placeholder {
  height: 200px;
  position: relative;
  overflow: hidden;
}

.chart-line {
  position: relative;
  height: 100%;
  width: 100%;
}

.line-point {
  position: absolute;
  width: 8px;
  height: 8px;
  background-color: #3498db;
  border-radius: 50%;
  transform: translate(-50%, -50%);
}

.line-point::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 2px;
  height: 100%;
  background-color: #3498db;
  transform: translate(-50%, 0);
  z-index: -1;
}

.pie-chart {
  position: relative;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  margin: 0 auto;
  overflow: hidden;
}

.pie-slice {
  position: absolute;
  width: 100%;
  height: 100%;
  clip-path: polygon(50% 50%, 50% 0%, 100% 0%, 100% 100%, 50% 100%);
}

.slice-1 {
  background-color: #3498db;
  transform: rotate(0deg);
}

.slice-2 {
  background-color: #27ae60;
  transform: rotate(90deg);
}

.slice-3 {
  background-color: #f39c12;
  transform: rotate(180deg);
}

.slice-4 {
  background-color: #e74c3c;
  transform: rotate(270deg);
}

.api-details {
  margin-top: 30px;
}

.api-details h3 {
  margin: 0 0 20px 0;
  font-size: 16px;
  color: #333;
  font-weight: 600;
}

.api-table-container {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.api-table {
  width: 100%;
  border-collapse: collapse;
}

.api-table th,
.api-table td {
  padding: 12px 15px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
}

.api-table th {
  background-color: #f8f9fa;
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

.api-table td {
  font-size: 14px;
  color: #555;
}

.method-badge {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.method-badge.get {
  background-color: #27ae60;
  color: white;
}

.method-badge.post {
  background-color: #3498db;
  color: white;
}

.method-badge.put {
  background-color: #f39c12;
  color: white;
}

.method-badge.delete {
  background-color: #e74c3c;
  color: white;
}
</style>