<template>
  <div class="admin-page">
    <section class="split-hero">
      <article class="hero-card">
        <p class="header-tag">后台概览</p>
        <h2>后台状态清晰集中，适合日常巡检与异常定位</h2>
        <p class="panel-subtitle">
          当前工作台已经接入用户、衣橱、知识库与 AI 调用监控接口，适合管理员和运营人员进行统一巡检。
        </p>

        <div class="hero-metrics">
          <div class="hero-metric">
            <span>近 24 小时调用</span>
            <strong>{{ stats.aiCalls }}</strong>
          </div>
          <div class="hero-metric">
            <span>监控健康度</span>
            <strong>{{ stats.healthUp ? '正常' : '异常' }}</strong>
          </div>
          <div class="hero-metric">
            <span>知识条目</span>
            <strong>{{ stats.knowledgeCount }}</strong>
          </div>
          <div class="hero-metric">
            <span>搭配记录</span>
            <strong>{{ stats.outfitRecordCount }}</strong>
          </div>
        </div>
      </article>

      <aside class="insight-list">
        <article class="insight-item">
          <span>当前角色</span>
          <strong>{{ isAdmin ? '管理员视角' : '运营视角' }}</strong>
          <p>{{ isAdmin ? '可访问完整后台模块。' : '当前仅开放与账号相关的后台能力。' }}</p>
        </article>
        <article class="insight-item">
          <span>衣物资产</span>
          <strong>{{ stats.clothingCount }}</strong>
          <p>当前账号或后台范围内已接入的衣物数据总量。</p>
        </article>
        <article class="insight-item">
          <span>用户规模</span>
          <strong>{{ stats.userCount }}</strong>
          <p>用于快速判断近期用户规模和后台维护范围。</p>
        </article>
      </aside>
    </section>

    <div class="stats-grid">
      <article class="stat-card">
        <span class="stat-label">用户总数</span>
        <strong>{{ stats.userCount }}</strong>
        <p class="stat-note">管理员可查看全量用户列表与状态。</p>
      </article>
      <article class="stat-card">
        <span class="stat-label">当前衣物数</span>
        <strong>{{ stats.clothingCount }}</strong>
        <p class="stat-note">按当前权限范围统计衣橱资产。</p>
      </article>
      <article class="stat-card">
        <span class="stat-label">近 24 小时 AI 调用</span>
        <strong>{{ stats.aiCalls }}</strong>
        <p class="stat-note">管理员视角默认统计全用户近 24 小时调用活跃度。</p>
      </article>
      <article v-if="isAdmin" class="stat-card">
        <span class="stat-label">AI 会话数</span>
        <strong>{{ stats.aiConversationCount }}</strong>
        <p class="stat-note">用于观察会话规模和问答活跃度。</p>
      </article>
      <article v-if="isAdmin" class="stat-card">
        <span class="stat-label">知识条目</span>
        <strong>{{ stats.knowledgeCount }}</strong>
        <p class="stat-note">面料、指南和洗护知识总量。</p>
      </article>
      <article v-if="isAdmin" class="stat-card">
        <span class="stat-label">搭配记录</span>
        <strong>{{ stats.outfitRecordCount }}</strong>
        <p class="stat-note">沉淀的 AI 搭配推荐结果与反馈。</p>
      </article>
      <article class="stat-card">
        <span class="stat-label">监控健康度</span>
        <strong :class="stats.healthUp ? 'status-up' : 'status-down'">
          {{ stats.healthUp ? '正常' : '异常' }}
        </strong>
        <p class="stat-note">基于后端健康检查接口返回结果。</p>
      </article>
    </div>

    <div class="panel-grid">
      <section class="panel-card">
        <div class="panel-head">
          <div>
            <h2>后台接入说明</h2>
            <p class="panel-subtitle">帮助当前操作者快速理解后台覆盖范围与权限边界。</p>
          </div>
        </div>
        <ul class="plain-list">
          <li>当前后台已接入真实登录、用户列表、衣物列表、知识库管理和 AI 监控接口。</li>
          <li>用户管理仅管理员可见，后端当前以 `userId = 1` 识别管理员。</li>
          <li>衣物页当前是“当前登录账号视角”，后端暂未提供完整的全局衣物管理接口。</li>
          <li>知识库页已支持面料、指南、洗护标的新建、编辑、批量导入与停启用维护。</li>
          <li>搭配记录页支持检索全局 AI 穿搭结果，并查看推荐单品与用户反馈详情。</li>
        </ul>
      </section>

      <section class="panel-card">
        <div class="panel-head">
          <div>
            <h2>最近 AI 调用</h2>
            <p class="panel-subtitle">便于快速查看场景、模型、状态与响应时间。</p>
          </div>
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
