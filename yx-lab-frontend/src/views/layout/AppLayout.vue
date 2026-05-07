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
            <span class="primary-nav__icon">
              <el-icon>
                <component :is="item.icon" />
              </el-icon>
            </span>
            <span class="primary-nav__text">{{ item.shortTitle }}</span>
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

        <el-popover
          placement="bottom-end"
          trigger="click"
          width="320"
          popper-class="skin-popover"
        >
          <template #reference>
            <button type="button" class="topbar-action skin-trigger">
              <el-icon><SetUp /></el-icon>
              <span>换肤</span>
            </button>
          </template>

          <div class="skin-panel">
            <div class="skin-panel__head">
              <strong>系统换肤</strong>
              <span>按规范色板切换主题</span>
            </div>
            <button
              v-for="theme in themeOptions"
              :key="theme.id"
              type="button"
              :class="['skin-option', { 'is-active': currentThemeId === theme.id }]"
              @click="changeTheme(theme.id)"
            >
              <span class="skin-option__palette">
                <i :style="{ background: theme.primary }"></i>
                <i :style="{ background: theme.secondary }"></i>
                <i :style="{ background: theme.accent }"></i>
              </span>
              <span class="skin-option__meta">
                <strong>{{ theme.name }}</strong>
                <small>{{ theme.primary }} / {{ theme.secondary }}</small>
              </span>
            </button>
          </div>
        </el-popover>

        <button type="button" class="topbar-action" @click="showMessageTip">
          <el-icon><Bell /></el-icon>
          <span>消息</span>
          <em>99+</em>
        </button>

        <button type="button" class="topbar-action icon-only" @click="toggleFullscreen">
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
import { computed, onMounted, ref } from 'vue'
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
  SetUp,
  Tickets
} from '@element-plus/icons-vue'
import { clearToken, getUser } from '../../utils/auth'

const THEME_STORAGE_KEY = 'yx-lab-theme'

const router = useRouter()
const route = useRoute()
const menuKeyword = ref('')
const currentThemeId = ref('科技蓝')

const themeOptions = [
  { id: '科技蓝', name: '科技蓝', primary: '#1677FF', secondary: '#20BEF5', accent: '#8CC8FF' },
  { id: '湖湾青', name: '湖湾青', primary: '#0F9B8E', secondary: '#36CFC9', accent: '#A0E7E0' },
  { id: '政务绿', name: '政务绿', primary: '#18A058', secondary: '#52C41A', accent: '#B7EB8F' },
  { id: '暖阳橙', name: '暖阳橙', primary: '#F08C2E', secondary: '#FAAD14', accent: '#FFD591' },
  { id: '星夜紫', name: '星夜紫', primary: '#6F62FF', secondary: '#B37FEB', accent: '#D3ADF7' }
]

const menuItems = [
  {
    path: '/dashboard',
    title: '运行总览',
    shortTitle: '总览',
    subtitle: '统一查看实验室整体运行情况与核心业务指标',
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
const currentTitle = computed(() => currentMenu.value.title || route.meta?.title || '阳新县实验室水质管理平台')
const currentSubtitle = computed(() => currentMenu.value.subtitle || route.meta?.subtitle || '实验室业务闭环管理')
const todayText = computed(() => dayjs().format('YYYY年M月D日 dddd'))

function applyTheme(themeId) {
  const targetTheme = themeOptions.some((item) => item.id === themeId) ? themeId : themeOptions[0].id
  currentThemeId.value = targetTheme
  document.documentElement.setAttribute('data-theme', targetTheme)
  localStorage.setItem(THEME_STORAGE_KEY, targetTheme)
}

function changeTheme(themeId) {
  applyTheme(themeId)
  ElMessage.success(`已切换为${themeId}主题`)
}

function initTheme() {
  const savedTheme = localStorage.getItem(THEME_STORAGE_KEY)
  applyTheme(savedTheme || themeOptions[0].id)
}

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
    ElMessage.warning('未找到匹配的菜单，请重新输入关键字。')
    return
  }
  goRoute(matchedMenu.path)
}

function showMessageTip() {
  ElMessage.info('当前为演示消息入口，后续可接入正式消息中心。')
}

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen?.()
    return
  }
  document.exitFullscreen?.()
}

function logout() {
  clearToken()
  router.push('/login')
}

onMounted(() => {
  initTheme()
})
</script>

<style scoped>
.layout-root {
  height: 100vh;
  min-height: 100vh;
  padding-top: var(--layout-topbar-height);
  background: var(--bg-page);
  overflow: hidden;
}

.layout-topbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  height: var(--layout-topbar-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 22px;
  background: linear-gradient(90deg, var(--brand-gradient-start) 0%, var(--brand-gradient-end) 100%);
  box-shadow: 0 4px 16px rgba(17, 61, 122, 0.18);
}

.topbar-left,
.topbar-right {
  display: flex;
  align-items: center;
  min-width: 0;
}

.topbar-left {
  flex: 1;
  gap: 20px;
}

.topbar-right {
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.brand-panel {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0;
  border: none;
  background: transparent;
  color: #ffffff;
  cursor: pointer;
}

.brand-mark {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.24);
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 1px;
}

.brand-copy strong,
.user-copy strong {
  display: block;
  line-height: 1.3;
}

.brand-copy strong {
  font-size: 16px;
  font-weight: 600;
}

.brand-copy p,
.user-copy p {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.2;
}

.brand-copy p {
  color: rgba(255, 255, 255, 0.76);
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
  min-width: 78px;
  height: var(--layout-topbar-height);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 10px;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.82);
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.primary-nav__icon {
  width: var(--nav-entry-size);
  height: var(--nav-entry-size);
  display: grid;
  place-items: center;
}

.primary-nav__icon .el-icon {
  font-size: 22px;
}

.primary-nav__text {
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
  width: 248px;
}

.topbar-action {
  height: 38px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 14px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 19px;
  background: rgba(255, 255, 255, 0.12);
  color: #ffffff;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease;
}

.topbar-action:hover {
  background: rgba(255, 255, 255, 0.18);
  border-color: rgba(255, 255, 255, 0.28);
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
  width: 38px;
  justify-content: center;
  padding: 0;
}

.skin-trigger {
  min-width: 86px;
}

.skin-panel {
  display: grid;
  gap: 10px;
}

.skin-panel__head {
  display: grid;
  gap: 4px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--line-soft);
}

.skin-panel__head strong {
  color: var(--text-main);
  font-size: 15px;
}

.skin-panel__head span {
  color: var(--text-sub);
  font-size: 12px;
}

.skin-option {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #dce6f2;
  border-radius: 12px;
  background: #ffffff;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}

.skin-option:hover,
.skin-option.is-active {
  border-color: var(--brand);
  background: var(--brand-soft);
}

.skin-option__palette {
  display: inline-flex;
  gap: 6px;
}

.skin-option__palette i {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.06) inset;
}

.skin-option__meta {
  display: grid;
  gap: 2px;
}

.skin-option__meta strong {
  color: var(--text-main);
  font-size: 14px;
}

.skin-option__meta small {
  color: var(--text-sub);
  font-size: 12px;
}

.user-box {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #ffffff;
}

.user-avatar {
  width: 38px;
  height: 38px;
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
  height: calc(100vh - var(--layout-topbar-height));
  min-height: calc(100vh - var(--layout-topbar-height));
  overflow: hidden;
}

.sidebar {
  position: fixed;
  top: var(--layout-topbar-height);
  left: 0;
  bottom: 0;
  width: var(--layout-sidebar-width);
  background: var(--bg-sidebar);
  color: #d4dbeb;
  display: flex;
  flex-direction: column;
  padding: 14px 0 12px;
  box-shadow: inset -1px 0 0 rgba(255, 255, 255, 0.04);
  overflow-y: auto;
}

.sidebar-title {
  padding: 0 18px 12px;
  color: #8b95af;
  font-size: 12px;
  letter-spacing: 1px;
}

.sidebar-footer {
  margin-top: auto;
  padding: 16px 18px 6px;
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
  display: flex;
  flex-direction: column;
  height: calc(100vh - var(--layout-topbar-height));
  min-height: 0;
  min-width: 0;
  margin-left: var(--layout-sidebar-width);
  overflow: hidden;
  padding: 14px 16px 18px;
}

.workspace-head {
  padding: 12px 16px 14px;
  background: #ffffff;
  border: 1px solid var(--line-soft);
  border-radius: 12px;
  overflow: hidden;
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
  width: 28px;
  height: 28px;
  display: inline-grid;
  place-items: center;
  border: 1px solid #d8e4f0;
  border-radius: 8px;
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
  margin-top: 12px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.route-tab {
  min-width: 96px;
  height: 34px;
  padding: 0 16px;
  border: 1px solid #dce6f2;
  border-radius: 10px;
  background: #f6f8fb;
  color: var(--text-sub);
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
}

.route-tab:hover,
.route-tab.is-active {
  border-color: var(--brand);
  background: var(--brand-soft);
  color: var(--brand);
}

.view-body {
  flex: 1;
  min-height: 0;
  min-width: 0;
  overflow: auto;
  padding-top: 14px;
}

:deep(.skin-popover) {
  padding: 14px !important;
  border-radius: 16px !important;
}

:deep(.topbar-search .el-input__wrapper) {
  min-height: 38px;
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
  --el-menu-hover-bg-color: var(--bg-sidebar-hover);
  --el-menu-active-color: #ffffff;
}

:deep(.menu-panel .el-menu-item) {
  height: 46px;
  margin: 0 0 6px;
  padding-left: 18px !important;
  border-left: 3px solid transparent;
  color: #d4dbeb;
  gap: 10px;
}

:deep(.menu-panel .el-menu-item:hover) {
  background: var(--bg-sidebar-hover);
  color: #ffffff;
}

:deep(.menu-panel .el-menu-item.is-active) {
  background: var(--bg-sidebar-active);
  border-left-color: var(--brand);
  color: #ffffff;
}

:deep(.menu-panel .el-menu-item .el-icon) {
  font-size: 16px;
}

@media (max-width: 1440px) {
  .topbar-search {
    width: 220px;
  }
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
  .layout-root {
    height: auto;
    padding-top: 0;
    overflow: visible;
  }

  .layout-topbar {
    position: static;
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

  .topbar-right {
    gap: 10px;
  }

  .sidebar {
    position: static;
    width: 100%;
    overflow: visible;
  }

  .sidebar-footer {
    display: none;
  }

  .workspace {
    height: auto;
    margin-left: 0;
    overflow: visible;
  }

  .layout-main,
  .view-body {
    height: auto;
    overflow: visible;
  }
}

@media (max-width: 768px) {
  .topbar-left,
  .topbar-right,
  .breadcrumb-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .primary-nav,
  .topbar-search {
    width: 100%;
  }

  .user-box {
    width: 100%;
    justify-content: space-between;
  }

  .topbar-action,
  .skin-trigger {
    min-width: 0;
  }

  .workspace {
    padding: 10px;
  }

  .workspace-head {
    padding: 10px 12px;
  }
}
</style>
