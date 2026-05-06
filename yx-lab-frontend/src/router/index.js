import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../utils/auth'

const routes = [
  {
    path: '/login',
    component: () => import('../views/LoginView.vue')
  },
  {
    path: '/',
    component: () => import('../views/layout/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('../views/DashboardView.vue') },
      { path: 'monitoring', component: () => import('../views/MonitoringPointView.vue') },
      { path: 'samples', component: () => import('../views/SamplingView.vue') },
      { path: 'detections', component: () => import('../views/DetectionView.vue') },
      { path: 'reviews', component: () => import('../views/ReviewView.vue') },
      { path: 'reports', component: () => import('../views/ReportView.vue') },
      { path: 'assets', component: () => import('../views/AssetView.vue') },
      { path: 'statistics', component: () => import('../views/StatisticsView.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.path === '/login') {
    next()
    return
  }
  if (!getToken()) {
    next('/login')
    return
  }
  next()
})

export default router
