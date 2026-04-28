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
    path: '/profile/account',
    name: 'profile-account',
    component: () => import('../views/ProfileAccountView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile/system',
    name: 'profile-system',
    component: () => import('../views/ProfileSystemView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile/system/home',
    name: 'profile-system-home',
    component: () => import('../views/ProfileHomeSettingsView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile/system/home/prompts',
    name: 'profile-system-home-prompts',
    component: () => import('../views/ProfileHomePromptListView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile/system/home/import',
    name: 'profile-system-home-import',
    component: () => import('../views/ProfileHomePromptImportView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile/system/home/display',
    name: 'profile-system-home-display',
    component: () => import('../views/ProfileHomeDisplayView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile/basic',
    name: 'profile-basic',
    component: () => import('../views/ProfileBasicView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile/preferences',
    redirect: '/profile/system'
  },
  {
    path: '/profile/security',
    name: 'profile-security',
    component: () => import('../views/ProfileSecurityView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/style-learning',
    name: 'style-learning',
    component: () => import('../views/StyleLearningView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/chat',
    name: 'chat',
    component: () => import('../views/ChatView.vue'),
    meta: { requiresAuth: true, keepAlive: true }
  },
  {
    path: '/home/recommendations/:id',
    name: 'home-recommendation-detail',
    component: () => import('../views/HomeRecommendationDetailView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/materials',
    name: 'materials',
    component: () => import('../views/MaterialKnowledgeView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/saved-looks',
    name: 'saved-looks',
    component: () => import('../views/SavedLooksView.vue')
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
    path: '/wardrobe/import',
    name: 'import-clothing',
    component: () => import('../views/ImportClothingView.vue'),
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
