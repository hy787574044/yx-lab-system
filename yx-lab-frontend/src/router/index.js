import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../utils/auth'
import { labMenuGroups, legacyRedirects } from './menuConfig'

const componentMap = {
  DashboardView: () => import('../views/DashboardView.vue'),
  MonitoringPointView: () => import('../views/MonitoringPointView.vue'),
  SamplingView: () => import('../views/SamplingView.vue'),
  DetectionView: () => import('../views/DetectionView.vue'),
  ReviewView: () => import('../views/ReviewView.vue'),
  ReportView: () => import('../views/ReportView.vue'),
  AssetView: () => import('../views/AssetView.vue'),
  InstrumentMaintenanceView: () => import('../views/InstrumentMaintenanceView.vue'),
  SystemManagementView: () => import('../views/SystemManagementView.vue'),
  StatisticsView: () => import('../views/StatisticsView.vue'),
  FeaturePlaceholderView: () => import('../views/FeaturePlaceholderView.vue')
}

function buildMenuChildrenRoutes() {
  return labMenuGroups.flatMap((group) => (
    group.children.map((item) => ({
      path: item.path.replace(/^\//, ''),
      component: componentMap[item.componentKey],
      meta: {
        title: item.title,
        subtitle: item.subtitle,
        primaryId: group.id,
        primaryMenu: group.title,
        primaryShortTitle: group.shortTitle,
        secondaryMenu: item.title,
        secondaryShortTitle: item.shortTitle,
        defaultTab: item.defaultTab,
        defaultStatKey: item.defaultStatKey,
        defaultStatLabel: item.defaultStatLabel,
        placeholderNote: item.placeholderNote
      }
    }))
  ))
}

const routes = [
  {
    path: '/login',
    meta: {
      title: '系统登录',
      subtitle: '登录阳新实验室水质管理平台'
    },
    component: () => import('../views/LoginView.vue')
  },
  {
    path: '/mobile/login',
    meta: {
      title: '移动端登录',
      subtitle: '进入移动端采样、检测、审查与报告闭环'
    },
    component: () => import('../views/MobileLoginView.vue')
  },
  {
    path: '/mobile',
    meta: {
      title: '移动工作台',
      subtitle: '面向移动终端的一体化实验室业务工作台'
    },
    component: () => import('../views/MobileWorkbenchView.vue')
  },
  {
    path: '/',
    component: () => import('../views/layout/AppLayout.vue'),
    redirect: '/dashboard',
    children: buildMenuChildrenRoutes()
  },
  ...legacyRedirects,
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta?.title
    ? `${to.meta.title} - 阳新实验室水质管理平台`
    : '阳新实验室水质管理平台'

  if (to.path === '/login' || to.path === '/mobile/login') {
    next()
    return
  }

  if (!getToken()) {
    next(to.path.startsWith('/mobile') ? '/mobile/login' : '/login')
    return
  }

  next()
})

export default router
