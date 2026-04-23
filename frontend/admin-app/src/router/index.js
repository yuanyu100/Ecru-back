import { createRouter, createWebHistory } from 'vue-router';
import { authApi } from '../api/auth';

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
        path: 'clothing',
        name: 'admin-clothing',
        component: () => import('../views/Admin/ClothingView.vue'),
        meta: { title: '衣物台账' }
      },
      {
        path: 'api-monitor',
        name: 'admin-api-monitor',
        component: () => import('../views/Admin/ApiMonitorView.vue'),
        meta: { requiresAdmin: true, title: 'AI 监控' }
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

  if (requiresAdmin && currentUser?.role !== 'ADMIN') {
    return '/dashboard';
  }

  return true;
});

export default router;
