import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../utils/auth'

const routes = [
  {
    path: '/login',
    meta: {
      title: '系统登录',
      subtitle: '登录阳新县实验室水质管理平台'
    },
    component: () => import('../views/LoginView.vue')
  },
  {
    path: '/mobile/login',
    meta: {
      title: '移动端登录',
      subtitle: '进入移动端采样、检测、审核与报告闭环'
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
    children: [
      {
        path: 'dashboard',
        meta: {
          title: '运行总览',
          subtitle: '统一查看样品、检测、审核与报告发布状态'
        },
        component: () => import('../views/DashboardView.vue')
      },
      {
        path: 'monitoring',
        meta: {
          title: '监测点位',
          subtitle: '维护监测点位基础信息，支撑采样计划与业务流转'
        },
        component: () => import('../views/MonitoringPointView.vue')
      },
      {
        path: 'samples',
        meta: {
          title: '样品采样',
          subtitle: '覆盖采样计划、采样任务与样品登录全过程'
        },
        component: () => import('../views/SamplingView.vue')
      },
      {
        path: 'detections',
        meta: {
          title: '检测分析',
          subtitle: '统一查看检测记录、结果状态与异常信息'
        },
        component: () => import('../views/DetectionView.vue')
      },
      {
        path: 'reviews',
        meta: {
          title: '结果审核',
          subtitle: '管理审核记录、审核结果与退回重检流程'
        },
        component: () => import('../views/ReviewView.vue')
      },
      {
        path: 'reports',
        meta: {
          title: '报告台账',
          subtitle: '查看报告状态、模板维护与发布推送动作'
        },
        component: () => import('../views/ReportView.vue')
      },
      {
        path: 'assets',
        meta: {
          title: '仪器文档',
          subtitle: '统一维护设备台账、文档资料与共享权限'
        },
        component: () => import('../views/AssetView.vue')
      },
      {
        path: 'statistics',
        meta: {
          title: '分析统计',
          subtitle: '从样品、审核与通过率维度汇总业务运行情况'
        },
        component: () => import('../views/StatisticsView.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta?.title ? `${to.meta.title} - 阳新县实验室水质管理平台` : '阳新县实验室水质管理平台'
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
