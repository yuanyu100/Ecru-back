import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';

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
    component: () => import('../views/ProfileView.vue')
  },
  {
    path: '/chat',
    name: 'chat',
    component: () => import('../views/ChatView.vue')
  },
  {
    path: '/about',
    name: 'about',
    component: () => import('../views/AboutView.vue')
  },
  {
    path: '/wardrobe',
    name: 'wardrobe',
    component: () => import('../views/WardrobeView.vue')
  },
  {
    path: '/wardrobe/add',
    name: 'add-clothing',
    component: () => import('../views/AddClothingView.vue')
  },
  {
    path: '/wardrobe/edit/:id',
    name: 'edit-clothing',
    component: () => import('../views/EditClothingView.vue')
  },
  {
    path: '/wardrobe/detail/:id',
    name: 'clothing-detail',
    component: () => import('../views/ClothingDetailView.vue')
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;
