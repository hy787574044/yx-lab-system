<template>
  <div class="layout-root">
    <header class="layout-topbar">
      <div class="topbar-left">
        <button type="button" class="brand-panel" @click="goRoute('/dashboard')">
          <div class="brand-mark">YX</div>
          <div class="brand-copy">
            <strong>阳新县实验室水质管理平台</strong>
            <p>Yangxin Laboratory Water Quality Platform</p>
          </div>
        </button>

        <nav class="primary-nav">
          <button
            v-for="item in menuItems"
            :key="item.path"
            type="button"
            :class="['primary-nav__item', { 'is-active': currentMenu.path === item.path }]"
            @click="goRoute(item.path)"
          >
            <el-icon>
              <component :is="item.icon" />
            </el-icon>
            <span>{{ item.shortTitle }}</span>
          </button>
        </nav>
      </div>

      <div class="topbar-right">
        <el-input
          v-model="menuKeyword"
          class="topbar-search"
          placeholder="请输入菜单名称"
          @keyup.enter="handleGlobalSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <button type="button" class="topbar-action">
          <el-icon><Bell /></el-icon>
          <span>消息</span>
          <em>99+</em>
        </button>

        <button type="button" class="topbar-action icon-only">
          <el-icon><FullScreen /></el-icon>
        </button>

        <div class="user-box">
          <div class="user-avatar">
            {{ (user.realName || user.username || '管').slice(0, 1) }}
          </div>
          <div class="user-copy">
            <strong>{{ user.realName || user.username || '管理员' }}</strong>
            <p>{{ user.roleCode || 'ADMIN' }}</p>
          </div>
          <el-button text class="logout-button" @click="logout">退出</el-button>
        </div>
      </div>
    </header>

    <div class="layout-main">
      <aside class="sidebar">
        <div class="sidebar-title">功能导航</div>

        <el-menu :default-active="$route.path" router class="menu-panel">
          <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </el-menu>

        <div class="sidebar-footer">
          <span>当前模块</span>
          <strong>{{ currentTitle }}</strong>
        </div>
      </aside>

      <main class="workspace">
        <section class="workspace-head">
          <div class="breadcrumb-row">
            <div class="breadcrumb">
              <button type="button" class="home-trigger" @click="goRoute('/dashboard')">
                <el-icon><House /></el-icon>
              </button>
              <span>首页</span>
              <el-icon class="breadcrumb-separator"><ArrowRight /></el-icon>
              <span class="is-current">{{ currentTitle }}</span>
            </div>

            <div class="workspace-meta">
              <span>{{ todayText }}</span>
              <span class="workspace-meta__dot"></span>
              <span>{{ currentSubtitle }}</span>
            </div>
          </div>

          <div class="route-tabs">
            <button
              v-for="item in menuItems"
              :key="`tab-${item.path}`"
              type="button"
              :class="['route-tab', { 'is-active': currentMenu.path === item.path }]"
              @click="goRoute(item.path)"
            >
              {{ item.title }}
            </button>
          </div>
        </section>

        <section class="view-body">
          <router-view />
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Bell,
  DataAnalysis,
  Document,
  DocumentChecked,
  Files,
  FullScreen,
  House,
  List,
  LocationFilled,
  PieChart,
  Search,
  Tickets
} from '@element-plus/icons-vue'
import { clearToken, getUser } from '../../utils/auth'

const router = useRouter()
const route = useRoute()
const menuKeyword = ref('')

const menuItems = [
  {
    path: '/dashboard',
    title: '运行总览',
    shortTitle: '总览',
    subtitle: '统一查看实验室总体运行情况与核心业务指标',
    icon: DataAnalysis
  },
  {
    path: '/monitoring',
    title: '监测点位',
    shortTitle: '点位',
    subtitle: '维护监测点位与基础信息档案',
    icon: LocationFilled
  },
  {
    path: '/samples',
    title: '样品采样',
    shortTitle: '采样',
    subtitle: '覆盖采样计划、采样任务和样品登录流程',
    icon: Tickets
  },
  {
    path: '/detections',
    title: '检测分析',
    shortTitle: '检测',
    subtitle: '处理检测记录、检测结果和异常事项',
    icon: List
  },
  {
    path: '/reviews',
    title: '结果审核',
    shortTitle: '审核',
    subtitle: '处理审核流转、退回重检和结果确认',
    icon: DocumentChecked
  },
  {
    path: '/reports',
    title: '报告台账',
    shortTitle: '报告',
    subtitle: '管理报告生成、发布和推送留痕',
    icon: Document
  },
  {
    path: '/assets',
    title: '仪器文档',
    shortTitle: '资产',
    subtitle: '统一管理仪器资产、附件与共享文档',
    icon: Files
  },
  {
    path: '/statistics',
    title: '分析统计',
    shortTitle: '统计',
    subtitle: '按样品、审核和报告维度汇总统计结果',
    icon: PieChart
  }
]

const user = computed(() => getUser() || {})
const currentMenu = computed(() => (
  menuItems.find((item) => route.path === item.path)
  || menuItems.find((item) => route.path.startsWith(item.path))
  || menuItems[0]
))
const currentTitle = computed(() => route.meta?.title || currentMenu.value.title)
const currentSubtitle = computed(() => route.meta?.subtitle || currentMenu.value.subtitle)
const todayText = computed(() => dayjs().format('YYYY年MM月DD日 dddd'))

function goRoute(path) {
  if (route.path !== path) {
    router.push(path)
  }
}

function handleGlobalSearch() {
  const keyword = menuKeyword.value.trim()
  if (!keyword) {
    return
  }
  const matchedMenu = menuItems.find((item) => (
    item.title.includes(keyword)
    || item.shortTitle.includes(keyword)
    || item.subtitle.includes(keyword)
  ))
  if (!matchedMenu) {
    ElMessage.warning('未找到匹配的菜单，请重新输入关键词')
    return
  }
  goRoute(matchedMenu.path)
}

function logout() {
  clearToken()
  router.push('/login')
}
</script>

<style scoped>
.layout-root {
  min-height: 100vh;
  background: var(--bg-page);
}

.layout-topbar {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 18px;
  background: linear-gradient(90deg, #1b5fd8 0%, #1d8dff 55%, #20bef5 100%);
  box-shadow: 0 2px 12px rgba(14, 72, 160, 0.18);
}

.topbar-left,
.topbar-right {
  display: flex;
  align-items: center;
  min-width: 0;
}

.topbar-left {
  flex: 1;
  gap: 16px;
}

.topbar-right {
  gap: 12px;
}

.brand-panel {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0;
  border: none;
  background: transparent;
  color: #ffffff;
  cursor: pointer;
}

.brand-mark {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.28);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 1px;
}

.brand-copy strong,
.user-copy strong {
  display: block;
  line-height: 1.3;
}

.brand-copy strong {
  font-size: 14px;
  font-weight: 600;
}

.brand-copy p,
.user-copy p {
  margin: 2px 0 0;
  font-size: 12px;
  line-height: 1.2;
}

.brand-copy p {
  color: rgba(255, 255, 255, 0.72);
}

.primary-nav {
  display: flex;
  align-items: stretch;
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: none;
}

.primary-nav::-webkit-scrollbar,
.route-tabs::-webkit-scrollbar {
  display: none;
}

.primary-nav__item {
  min-width: 72px;
  height: 56px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 0 10px;
  border: none;
  border-radius: 0;
  background: transparent;
  color: rgba(255, 255, 255, 0.78);
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.primary-nav__item .el-icon {
  font-size: 18px;
}

.primary-nav__item span {
  font-size: 12px;
  line-height: 1;
  white-space: nowrap;
}

.primary-nav__item:hover,
.primary-nav__item.is-active {
  background: rgba(255, 255, 255, 0.14);
  color: #ffffff;
}

.topbar-search {
  width: 220px;
}

.topbar-action {
  height: 34px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 17px;
  background: rgba(255, 255, 255, 0.12);
  color: #ffffff;
  cursor: pointer;
}

.topbar-action em {
  font-style: normal;
  min-width: 24px;
  padding: 0 6px;
  border-radius: 999px;
  background: #ff6b6b;
  color: #ffffff;
  font-size: 12px;
  line-height: 18px;
}

.topbar-action.icon-only {
  width: 34px;
  justify-content: center;
  padding: 0;
}

.user-box {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #ffffff;
}

.user-avatar {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  border: 1px solid rgba(255, 255, 255, 0.28);
  font-weight: 700;
}

.user-copy p {
  color: rgba(255, 255, 255, 0.72);
}

.logout-button {
  color: rgba(255, 255, 255, 0.92);
}

.layout-main {
  display: flex;
  min-height: calc(100vh - 56px);
}

.sidebar {
  width: 148px;
  background: var(--bg-sidebar);
  color: #d4dbeb;
  display: flex;
  flex-direction: column;
  padding: 12px 0;
  box-shadow: inset -1px 0 0 rgba(255, 255, 255, 0.04);
}

.sidebar-title {
  padding: 0 18px 10px;
  color: #8b95af;
  font-size: 12px;
  letter-spacing: 1px;
}

.sidebar-footer {
  margin-top: auto;
  padding: 14px 18px 4px;
  color: #8b95af;
  font-size: 12px;
  line-height: 1.6;
}

.sidebar-footer strong {
  display: block;
  margin-top: 4px;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
}

.workspace {
  flex: 1;
  min-width: 0;
  padding: 12px 14px 16px;
}

.workspace-head {
  padding: 10px 14px 12px;
  background: #ffffff;
  border: 1px solid var(--line-soft);
  border-radius: 6px;
}

.breadcrumb-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.breadcrumb,
.workspace-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.breadcrumb {
  color: var(--text-sub);
  font-size: 13px;
}

.home-trigger {
  width: 26px;
  height: 26px;
  display: inline-grid;
  place-items: center;
  border: 1px solid #d8e4f0;
  border-radius: 4px;
  background: #f5f9ff;
  color: var(--brand);
  cursor: pointer;
}

.breadcrumb-separator {
  color: #9aa9bf;
  font-size: 12px;
}

.breadcrumb .is-current {
  color: var(--text-main);
  font-weight: 600;
}

.workspace-meta {
  color: var(--text-light);
  font-size: 12px;
  white-space: nowrap;
}

.workspace-meta__dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #c1cfdf;
}

.route-tabs {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  overflow-x: auto;
}

.route-tab {
  min-width: 92px;
  height: 32px;
  padding: 0 14px;
  border: 1px solid #dce6f2;
  border-radius: 4px;
  background: #f6f8fb;
  color: var(--text-sub);
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
}

.route-tab:hover,
.route-tab.is-active {
  border-color: #83b7ff;
  background: #eaf3ff;
  color: var(--brand);
}

.view-body {
  min-width: 0;
  padding-top: 12px;
}

:deep(.topbar-search .el-input__wrapper) {
  box-shadow: none !important;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.16);
}

:deep(.topbar-search .el-input__inner) {
  color: #ffffff;
}

:deep(.topbar-search .el-input__inner::placeholder),
:deep(.topbar-search .el-input__prefix) {
  color: rgba(255, 255, 255, 0.74);
}

:deep(.menu-panel) {
  border-right: none;
  background: transparent;
  --el-menu-bg-color: transparent;
  --el-menu-border-color: transparent;
  --el-menu-text-color: #d4dbeb;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.08);
  --el-menu-active-color: #ffffff;
}

:deep(.menu-panel .el-menu-item) {
  height: 46px;
  margin: 0 0 4px;
  padding-left: 18px !important;
  border-left: 3px solid transparent;
  color: #d4dbeb;
  gap: 10px;
}

:deep(.menu-panel .el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08);
  color: #ffffff;
}

:deep(.menu-panel .el-menu-item.is-active) {
  background: rgba(57, 121, 233, 0.22);
  border-left-color: #54a8ff;
  color: #ffffff;
}

:deep(.menu-panel .el-menu-item .el-icon) {
  font-size: 16px;
}

@media (max-width: 1280px) {
  .brand-copy p,
  .workspace-meta {
    display: none;
  }

  .topbar-search {
    width: 180px;
  }
}

@media (max-width: 1080px) {
  .layout-topbar {
    height: auto;
    align-items: stretch;
    flex-direction: column;
    padding: 10px 12px;
  }

  .topbar-left,
  .topbar-right {
    width: 100%;
    justify-content: space-between;
  }

  .layout-main {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    padding-bottom: 8px;
  }

  .sidebar-footer {
    display: none;
  }
}

@media (max-width: 768px) {
  .topbar-left,
  .topbar-right,
  .breadcrumb-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .primary-nav {
    width: 100%;
  }

  .topbar-search {
    width: 100%;
  }

  .user-box {
    width: 100%;
    justify-content: space-between;
  }

  .workspace {
    padding: 10px;
  }

  .workspace-head {
    padding: 10px;
  }
}
</style>
