import { createRouter, createWebHistory } from 'vue-router'
import { getToken, getUser, setToken } from '../utils/auth'
import { labMenuGroups, legacyRedirects } from './menuConfig'
import { hasPermission } from '../utils/permission'
import { getMenuPermissionCode } from '../utils/menuPermission'
import { setEmbeddedMode } from '../utils/embedMode'

const componentMap = {
  DashboardView: () => import('../views/DashboardView.vue'),
  MonitoringPointView: () => import('../views/MonitoringPointView.vue'),
  SamplingView: () => import('../views/SamplingView.vue'),
  DetectionSplitView: () => import('../views/DetectionSplitView.vue'),
  DetectionAnalysisView: () => import('../views/DetectionAnalysisView.vue'),
  DetectionView: () => import('../views/DetectionView.vue'),
  DetectionConfigView: () => import('../views/DetectionConfigView.vue'),
  DetectionMethodView: () => import('../views/DetectionMethodView.vue'),
  ReviewView: () => import('../views/ReviewView.vue'),
  ReportView: () => import('../views/ReportView.vue'),
  AssetView: () => import('../views/AssetView.vue'),
  InstrumentMaintenanceView: () => import('../views/InstrumentMaintenanceView.vue'),
  SystemManagementView: () => import('../views/SystemManagementView.vue'),
  FlowConfigView: () => import('../views/FlowConfigView.vue'),
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
        permissionCode: getMenuPermissionCode(item.path),
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
      subtitle: '登录阳新化验室水质管理平台'
    },
    component: () => import('../views/LoginView.vue')
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

function resolveQueryValue(value) {
  if (Array.isArray(value)) {
    return value[0] || ''
  }
  return value || ''
}

router.beforeEach((to, from, next) => {
  document.title = to.meta?.title
    ? `${to.meta.title} - 阳新化验室水质管理平台`
    : '阳新化验室水质管理平台'

  const queryToken = String(resolveQueryValue(to.query?.token) || '').trim()
  if (queryToken) {
    setToken(queryToken)
    setEmbeddedMode(true)
    const { token, ...queryWithoutToken } = to.query
    next({
      path: to.path === '/login' ? '/dashboard' : to.path,
      query: queryWithoutToken,
      hash: to.hash,
      replace: true
    })
    return
  }

  if (to.path === '/login') {
    next()
    return
  }

  if (!getToken()) {
    next('/login')
    return
  }

  const user = getUser()
  const hasLocalPermissions = Array.isArray(user.permissionCodes) && user.permissionCodes.length > 0
  if (to.meta?.permissionCode && hasLocalPermissions && !hasPermission(to.meta.permissionCode)) {
    const firstAllowedPath = labMenuGroups
      .flatMap((group) => group.children)
      .map((item) => item.path)
      .find((path) => path !== to.path && hasPermission(getMenuPermissionCode(path)))

    if (firstAllowedPath) {
      next(firstAllowedPath)
      return
    }

    next()
    return
  }

  next()
})

export default router
