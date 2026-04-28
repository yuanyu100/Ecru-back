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
        meta: { title: '仪表盘' }
      },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('../views/Admin/UsersView.vue'),
        meta: { requiresAdmin: true, title: '用户管理' }
      },
      {
        path: 'clothing',
        name: 'admin-clothing',
        component: () => import('../views/Admin/ClothingView.vue'),
        meta: { title: '衣橱管理' }
      },
      {
        path: 'style-images',
        name: 'admin-style-images',
        component: () => import('../views/Admin/StyleImagesView.vue'),
        meta: { requiresAdmin: true, title: '风格图片管理' }
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
        meta: { requiresAdmin: true, title: 'AI 会话管理' }
      },
      {
        path: 'ai-prompts',
        name: 'admin-ai-prompts',
        component: () => import('../views/Admin/AiPromptView.vue'),
        meta: { requiresAdmin: true, title: '提示词配置' }
      },
      {
        path: 'knowledge',
        name: 'admin-knowledge',
        component: () => import('../views/Admin/KnowledgeView.vue'),
        meta: { requiresAdmin: true, title: '知识库管理' }
      },
      {
        path: 'knowledge/:type/create',
        name: 'admin-knowledge-create',
        component: () => import('../views/Admin/KnowledgeEditorView.vue'),
        meta: { requiresAdmin: true, title: '新建知识条目' }
      },
      {
        path: 'knowledge/:type/:id/edit',
        name: 'admin-knowledge-edit',
        component: () => import('../views/Admin/KnowledgeEditorView.vue'),
        meta: { requiresAdmin: true, title: '编辑知识条目' }
      },
      {
        path: 'outfit-records',
        name: 'admin-outfit-records',
        component: () => import('../views/Admin/OutfitRecordsView.vue'),
        meta: { requiresAdmin: true, title: '搭配记录管理' }
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
