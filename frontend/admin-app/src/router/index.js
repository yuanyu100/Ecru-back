import { createRouter, createWebHistory } from 'vue-router';
import { authApi } from '../api/auth';
import { isAdminUser } from '../utils/adminRole';

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { guestOnly: true }
  },
  {
    path: '/',
    component: () => import('../views/Admin/AdminLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/dashboard'
      },
      {
        path: 'dashboard',
        name: 'admin-dashboard',
        component: () => import('../views/Admin/DashboardView.vue'),
        meta: { title: '控制台' }
      },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('../views/Admin/UsersView.vue'),
        meta: { requiresAdmin: true, title: '用户管理' }
      },
      {
        path: 'users/:id',
        name: 'admin-user-detail',
        component: () => import('../views/Admin/UserDetailView.vue'),
        meta: { requiresAdmin: true, title: '用户详情' }
      },
      {
        path: 'clothing',
        name: 'admin-clothing',
        component: () => import('../views/Admin/ClothingView.vue'),
        meta: { title: '衣物台账' }
      },
      {
        path: 'style-images',
        name: 'admin-style-images',
        component: () => import('../views/Admin/StyleImagesView.vue'),
        meta: { requiresAdmin: true, title: '风格图库' }
      },
      {
        path: 'style-images/create',
        name: 'admin-style-image-create',
        component: () => import('../views/Admin/StyleImageEditorView.vue'),
        meta: { requiresAdmin: true, title: '新增风格图' }
      },
      {
        path: 'style-images/:id/edit',
        name: 'admin-style-image-edit',
        component: () => import('../views/Admin/StyleImageEditorView.vue'),
        meta: { requiresAdmin: true, title: '编辑风格图' }
      },
      {
        path: 'api-monitor',
        name: 'admin-api-monitor',
        component: () => import('../views/Admin/ApiMonitorView.vue'),
        meta: { requiresAdmin: true, title: 'AI 监控' }
      },
      {
        path: 'ai-conversations',
        name: 'admin-ai-conversations',
        component: () => import('../views/Admin/AiConversationView.vue'),
        meta: { requiresAdmin: true, title: 'AI 会话' }
      },
      {
        path: 'ai-conversations/:sessionId',
        name: 'admin-ai-conversation-detail',
        component: () => import('../views/Admin/AiConversationDetailView.vue'),
        meta: { requiresAdmin: true, title: 'AI 会话详情' }
      },
      {
        path: 'ai-prompts',
        name: 'admin-ai-prompts',
        component: () => import('../views/Admin/AiPromptView.vue'),
        meta: { requiresAdmin: true, title: 'Prompt 配置' }
      },
      {
        path: 'knowledge',
        name: 'admin-knowledge',
        component: () => import('../views/Admin/KnowledgeView.vue'),
        meta: { requiresAdmin: true, title: '知识库' }
      },
      {
        path: 'knowledge/:type/create',
        name: 'admin-knowledge-create',
        component: () => import('../views/Admin/KnowledgeEditorView.vue'),
        meta: { requiresAdmin: true, title: '新增知识' }
      },
      {
        path: 'knowledge/:type/:id/edit',
        name: 'admin-knowledge-edit',
        component: () => import('../views/Admin/KnowledgeEditorView.vue'),
        meta: { requiresAdmin: true, title: '编辑知识' }
      },
      {
        path: 'outfit-records',
        name: 'admin-outfit-records',
        component: () => import('../views/Admin/OutfitRecordsView.vue'),
        meta: { requiresAdmin: true, title: '搭配记录' }
      },
      {
        path: 'outfit-records/:id',
        name: 'admin-outfit-record-detail',
        component: () => import('../views/Admin/OutfitRecordDetailView.vue'),
        meta: { requiresAdmin: true, title: '搭配记录详情' }
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
});

router.beforeEach((to) => {
  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth);
  const requiresAdmin = to.matched.some((record) => record.meta.requiresAdmin);
  const currentUser = authApi.getCurrentUser();

  if (to.meta.guestOnly && authApi.isAuthenticated()) {
    return '/dashboard';
  }

  if (requiresAuth && !authApi.isAuthenticated()) {
    return '/login';
  }

  if (requiresAdmin && !isAdminUser(currentUser)) {
    return '/dashboard';
  }

  return true;
});

export default router;
