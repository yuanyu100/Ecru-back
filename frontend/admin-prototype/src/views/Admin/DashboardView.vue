<template>
  <div class="dashboard-view">
    <div class="dashboard-cards">
      <div class="dashboard-card" v-if="isAdmin">
        <div class="card-icon">👥</div>
        <div class="card-content">
          <h3>用户总数</h3>
          <p class="card-value">{{ userCount }}</p>
        </div>
      </div>
      <div class="dashboard-card">
        <div class="card-icon">👕</div>
        <div class="card-content">
          <h3>衣物总数</h3>
          <p class="card-value">{{ clothingCount }}</p>
        </div>
      </div>
      <div class="dashboard-card">
        <div class="card-icon">📊</div>
        <div class="card-content">
          <h3>今日API请求</h3>
          <p class="card-value">{{ todayApiRequests }}</p>
        </div>
      </div>
      <div class="dashboard-card">
        <div class="card-icon">📈</div>
        <div class="card-content">
          <h3>系统状态</h3>
          <p class="card-value status-{{ systemStatus }}">{{ systemStatusText }}</p>
        </div>
      </div>
    </div>
    
    <div class="dashboard-charts">
      <div class="chart-container">
        <h3>最近7天API请求趋势</h3>
        <div class="chart-placeholder">
          <div class="chart-bar" style="height: 30%;"></div>
          <div class="chart-bar" style="height: 45%;"></div>
          <div class="chart-bar" style="height: 35%;"></div>
          <div class="chart-bar" style="height: 60%;"></div>
          <div class="chart-bar" style="height: 50%;"></div>
          <div class="chart-bar" style="height: 70%;"></div>
          <div class="chart-bar" style="height: 40%;"></div>
        </div>
      </div>
      <div class="chart-container">
        <h3>衣物分类分布</h3>
        <div class="chart-placeholder pie-chart">
          <div class="pie-slice slice-1"></div>
          <div class="pie-slice slice-2"></div>
          <div class="pie-slice slice-3"></div>
          <div class="pie-slice slice-4"></div>
        </div>
      </div>
    </div>
    
    <div class="dashboard-recent">
      <h3>最近操作</h3>
      <div class="recent-activities">
        <div class="activity-item">
          <span class="activity-time">2024-01-01 10:30</span>
          <span class="activity-action">用户 admin 添加了新衣物</span>
        </div>
        <div class="activity-item">
          <span class="activity-time">2024-01-01 09:15</span>
          <span class="activity-action">用户 user1 更新了个人资料</span>
        </div>
        <div class="activity-item">
          <span class="activity-time">2023-12-31 16:45</span>
          <span class="activity-action">管理员删除了用户 user2</span>
        </div>
        <div class="activity-item">
          <span class="activity-time">2023-12-31 14:20</span>
          <span class="activity-action">用户 user3 上传了衣物图片</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue';
import { authApi } from '../../api/auth';

export default {
  name: 'DashboardView',
  setup() {
    const userCount = ref(128);
    const clothingCount = ref(512);
    const todayApiRequests = ref(1568);
    const systemStatus = ref('normal');
    
    const systemStatusText = ref('正常');
    
    const isAdmin = computed(() => {
      const currentUser = authApi.getCurrentUser();
      return currentUser?.role === 'ADMIN';
    });
    
    onMounted(() => {
      // 这里可以添加实际的数据获取逻辑
      console.log('Dashboard mounted');
    });
    
    return {
      userCount,
      clothingCount,
      todayApiRequests,
      systemStatus,
      systemStatusText,
      isAdmin
    };
  }
};
</script>

<style scoped>
.dashboard-view {
  padding: 20px;
}

.dashboard-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.dashboard-card {
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

.status-normal {
  color: #27ae60;
}

.status-warning {
  color: #f39c12;
}

.status-error {
  color: #e74c3c;
}

.dashboard-charts {
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
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  gap: 10px;
}

.chart-bar {
  flex: 1;
  background-color: #3498db;
  border-radius: 4px 4px 0 0;
  transition: height 0.3s ease;
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

.dashboard-recent {
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.dashboard-recent h3 {
  margin: 0 0 20px 0;
  font-size: 16px;
  color: #333;
  font-weight: 600;
}

.recent-activities {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.activity-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.activity-time {
  font-size: 14px;
  color: #999;
}

.activity-action {
  font-size: 14px;
  color: #333;
}
</style>