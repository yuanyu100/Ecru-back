<script setup>
import BottomNav from './components/BottomNav.vue';
</script>

<template>
  <div class="app-container">
    <router-view />
    <BottomNav />
  </div>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
  background-color: #f9f3e6;
  color: #333;
}

.app-container {
  min-height: 100vh;
  padding-bottom: 60px; /* 为底部导航栏留出空间 */
}
</style>
