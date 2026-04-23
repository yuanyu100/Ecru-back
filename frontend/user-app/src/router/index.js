import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import { authApi } from '../api/auth';

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomeView
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue')
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/RegisterView.vue')
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('../views/ProfileView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/chat',
    name: 'chat',
    component: () => import('../views/ChatView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/about',
    name: 'about',
    component: () => import('../views/AboutView.vue')
  },
  {
    path: '/wardrobe',
    name: 'wardrobe',
    component: () => import('../views/WardrobeView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/wardrobe/add',
    name: 'add-clothing',
    component: () => import('../views/AddClothingView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/wardrobe/edit/:id',
    name: 'edit-clothing',
    component: () => import('../views/EditClothingView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/wardrobe/detail/:id',
    name: 'clothing-detail',
    component: () => import('../views/ClothingDetailView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/outfit/history/:id',
    name: 'outfit-history-detail',
    component: () => import('../views/OutfitHistoryDetailView.vue'),
    meta: { requiresAuth: true }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !authApi.isAuthenticated()) {
    return '/login';
  }

  return true;
});

export default router;
