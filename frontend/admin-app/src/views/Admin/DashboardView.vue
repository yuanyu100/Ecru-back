<template>
  <div class="admin-page">
    <div class="stats-grid">
      <article class="stat-card">
        <span class="stat-label">用户总数</span>
        <strong>{{ stats.userCount }}</strong>
      </article>
      <article class="stat-card">
        <span class="stat-label">当前衣物数</span>
        <strong>{{ stats.clothingCount }}</strong>
      </article>
      <article class="stat-card">
        <span class="stat-label">近 24 小时 AI 调用</span>
        <strong>{{ stats.aiCalls }}</strong>
      </article>
      <article v-if="isAdmin" class="stat-card">
        <span class="stat-label">AI 会话数</span>
        <strong>{{ stats.aiConversationCount }}</strong>
      </article>
      <article v-if="isAdmin" class="stat-card">
        <span class="stat-label">知识条目</span>
        <strong>{{ stats.knowledgeCount }}</strong>
      </article>
      <article v-if="isAdmin" class="stat-card">
        <span class="stat-label">搭配记录</span>
        <strong>{{ stats.outfitRecordCount }}</strong>
      </article>
      <article class="stat-card">
        <span class="stat-label">监控健康度</span>
        <strong :class="stats.healthUp ? 'status-up' : 'status-down'">
          {{ stats.healthUp ? '正常' : '异常' }}
        </strong>
      </article>
    </div>

    <section class="panel-card">
      <div class="panel-head">
        <h2>最近 AI 调用</h2>
      </div>
        <div v-if="recentCalls.length" class="table-shell compact">
          <table class="data-table">
            <thead>
              <tr>
                <th>场景</th>
                <th>模型</th>
                <th>状态</th>
                <th>耗时</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="call in recentCalls" :key="call.id">
                <td>{{ call.scene || '-' }}</td>
                <td>{{ call.model || '-' }}</td>
                <td>
                  <span class="badge" :class="call.status === 1 ? 'badge-green' : 'badge-red'">
                    {{ call.status === 1 ? '成功' : '失败' }}
                  </span>
                </td>
                <td>{{ call.responseTime ?? 0 }} ms</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-else class="empty-tip">暂无 AI 调用记录。</p>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { adminApi } from '../../api/admin';
import { aiChatAdminApi } from '../../api/aiChat';
import { clothingApi } from '../../api/clothing';
import { knowledgeAdminApi } from '../../api/knowledge';
import { monitorApi } from '../../api/monitor';
import { outfitAdminApi } from '../../api/outfit';
import { getCurrentAdminFlag } from '../../utils/adminRole';

const isAdmin = getCurrentAdminFlag();
const stats = reactive({
  userCount: 0,
  clothingCount: 0,
  aiCalls: 0,
  aiConversationCount: 0,
  outfitRecordCount: 0,
  knowledgeCount: 0,
  healthUp: false
});
const recentCalls = ref([]);

const loadDashboard = async () => {
  const requests = isAdmin
    ? [
        adminApi.getUsers({ page: 1, size: 1 }).catch(() => null),
        aiChatAdminApi.getOverview().catch(() => null),
        outfitAdminApi.getOverview().catch(() => null),
        knowledgeAdminApi.getOverview().catch(() => null),
        clothingApi.getStatistics().catch(() => null),
        monitorApi.getDashboard().catch(() => null),
        monitorApi.getHealth().catch(() => null)
      ]
    : [
        clothingApi.getStatistics().catch(() => null),
        monitorApi.getDashboard().catch(() => null),
        monitorApi.getHealth().catch(() => null)
      ];

  const results = await Promise.all(requests);
  const userResult = isAdmin ? results[0] : null;
  const aiConversationResult = isAdmin ? results[1] : null;
  const outfitRecordResult = isAdmin ? results[2] : null;
  const knowledgeResult = isAdmin ? results[3] : null;
  const clothingResult = isAdmin ? results[4] : results[0];
  const monitorResult = isAdmin ? results[5] : results[1];
  const healthResult = isAdmin ? results[6] : results[2];

  if (userResult?.code === 200) {
    stats.userCount = userResult.data?.total || 0;
  }

  if (aiConversationResult?.code === 200) {
    stats.aiConversationCount = Number(aiConversationResult.data?.conversationTotal || 0);
  }

  if (outfitRecordResult?.code === 200) {
    stats.outfitRecordCount = Number(outfitRecordResult.data?.recordTotal || 0);
  }

  if (knowledgeResult?.code === 200) {
    const payload = knowledgeResult.data || {};
    stats.knowledgeCount =
      Number(payload.fabricTotal || 0) + Number(payload.guideTotal || 0) + Number(payload.careLabelTotal || 0);
  }

  if (clothingResult?.code === 200) {
    stats.clothingCount = clothingResult.data?.overview?.totalClothings || 0;
  }

  if (monitorResult?.success) {
    stats.aiCalls = monitorResult.data?.recent24hTotalCalls || monitorResult.data?.todayTotalCalls || 0;
    recentCalls.value = monitorResult.data?.recentCalls || [];
  }

  if (healthResult?.success) {
    stats.healthUp = healthResult.status === 'UP';
  }
};

onMounted(loadDashboard);
</script>
