<template>
  <div class="layout-root">
    <header class="layout-topbar">
      <div class="topbar-left">
        <button type="button" class="brand-panel" @click="goRoute('/dashboard')">
          <div class="brand-mark">YX</div>
          <div class="brand-copy">
            <strong>阳新化验室水质管理平台</strong>
            <p>Yangxin Laboratory Water Quality Platform</p>
          </div>
        </button>

        <button type="button" class="top-level-entry" @click="goRoute('/dashboard')">
          <span class="top-level-entry__label">一级菜单</span>
          <strong>水质管理</strong>
        </button>
      </div>

      <div class="topbar-right">
        <el-input
          v-model="menuKeyword"
          class="topbar-search"
          placeholder="请输入菜单名称或关键字"
          clearable
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
          <div class="user-avatar">{{ userInitial }}</div>
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
        <div class="sidebar-head">
          <span class="sidebar-head__caption">功能导航</span>
          <strong>水质管理</strong>
          <p>左侧按业务模块展开原一级菜单与二级菜单。</p>
        </div>

        <div class="sidebar-body">
          <el-menu
            ref="menuRef"
            :default-active="currentRoutePath"
            :default-openeds="defaultOpenMenuIds"
            router
            unique-opened
            class="menu-panel"
          >
            <el-sub-menu
              v-for="group in primaryMenus"
              :key="group.id"
              :index="group.id"
            >
              <template #title>
                <el-icon>
                  <component :is="iconMap[group.iconKey]" />
                </el-icon>
                <span>{{ group.title }}</span>
              </template>

              <el-menu-item
                v-for="item in group.children"
                :key="item.path"
                :index="item.path"
              >
                <span>{{ item.title }}</span>
              </el-menu-item>
            </el-sub-menu>
          </el-menu>
        </div>

        <div class="sidebar-footer">
          <span>当前模块</span>
          <strong>{{ currentPrimaryMenu.title }}</strong>
          <p>{{ currentSecondaryMenu.title }}</p>
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
              <span>水质管理</span>
              <el-icon class="breadcrumb-separator"><ArrowRight /></el-icon>
              <span>{{ currentPrimaryMenu.title }}</span>
              <el-icon class="breadcrumb-separator"><ArrowRight /></el-icon>
              <span class="is-current">{{ currentSecondaryMenu.title }}</span>
            </div>

            <div class="workspace-meta">
              <span>{{ todayText }}</span>
              <span class="workspace-meta__dot"></span>
              <span>{{ currentSubtitle }}</span>
            </div>
          </div>

          <div class="route-tabs">
            <button
              v-for="item in currentSecondaryMenus"
              :key="`tab-${item.path}`"
              type="button"
              :class="['route-tab', { 'is-active': currentRoutePath === item.path }]"
              @click="goRoute(item.path)"
            >
              {{ item.shortTitle || item.title }}
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
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElIcon } from 'element-plus/es/components/icon/index.mjs'
import { ElInput } from 'element-plus/es/components/input/index.mjs'
import { ElMenu, ElMenuItem, ElSubMenu } from 'element-plus/es/components/menu/index.mjs'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElPopover } from 'element-plus/es/components/popover/index.mjs'
import {
  ArrowRight,
  Bell,
  Cpu,
  DataAnalysis,
  DataLine,
  Document,
  DocumentChecked,
  Files,
  FullScreen,
  House,
  List,
  LocationFilled,
  PieChart,
  Search,
  Setting,
  SetUp,
  Tickets
} from '@element-plus/icons-vue'
import { clearToken, getUser } from '../../utils/auth'
import { labMenuGroups } from '../../router/menuConfig'

const THEME_STORAGE_KEY = 'yx-lab-theme'

const iconMap = {
  DataAnalysis,
  LocationFilled,
  Tickets,
  List,
  DataLine,
  DocumentChecked,
  Document,
  Cpu,
  Files,
  PieChart,
  Setting
}

const themeOptions = [
  { id: '科技蓝', name: '科技蓝', primary: '#1677FF', secondary: '#20BEF5', accent: '#8CC8FF' },
  { id: '湖湾青', name: '湖湾青', primary: '#0F9B8E', secondary: '#36CFC9', accent: '#A0E7E0' },
  { id: '政务绿', name: '政务绿', primary: '#18A058', secondary: '#52C41A', accent: '#B7EB8F' },
  { id: '暖阳橙', name: '暖阳橙', primary: '#F08C2E', secondary: '#FAAD14', accent: '#FFD591' },
  { id: '星夜紫', name: '星夜紫', primary: '#6F62FF', secondary: '#B37FEB', accent: '#D3ADF7' }
]

const router = useRouter()
const route = useRoute()
const menuRef = ref()
const menuKeyword = ref('')
const currentThemeId = ref(themeOptions[0].id)

const primaryMenus = labMenuGroups
const allSearchItems = labMenuGroups.flatMap((group) => ([
  {
    keywordText: [group.title, group.shortTitle].join('|'),
    path: group.defaultPath
  },
  ...group.children.map((item) => ({
    keywordText: [group.title, group.shortTitle, item.title, item.shortTitle, item.subtitle].filter(Boolean).join('|'),
    path: item.path
  }))
]))

const user = computed(() => getUser() || {})
const userInitial = computed(() => (user.value.realName || user.value.username || '管').slice(0, 1))
const currentRoutePath = computed(() => route.path)

const currentPrimaryMenu = computed(() => (
  primaryMenus.find((item) => item.id === route.meta?.primaryId)
  || primaryMenus.find((item) => item.children.some((child) => child.path === route.path))
  || primaryMenus[0]
))

const currentSecondaryMenus = computed(() => currentPrimaryMenu.value?.children || [])
const defaultOpenMenuIds = computed(() => (
  currentPrimaryMenu.value?.id ? [currentPrimaryMenu.value.id] : []
))

const currentSecondaryMenu = computed(() => (
  currentSecondaryMenus.value.find((item) => item.path === route.path)
  || currentSecondaryMenus.value[0]
  || { title: route.meta?.title || '未命名页面', shortTitle: route.meta?.secondaryShortTitle || route.meta?.title || '页面', subtitle: route.meta?.subtitle || '' }
))

const currentSubtitle = computed(() => currentSecondaryMenu.value.subtitle || route.meta?.subtitle || '化验室业务闭环管理')
const todayText = computed(() => dayjs().format('YYYY年MM月DD日 dddd'))

function applyTheme(themeId) {
  const matchedTheme = themeOptions.find((item) => item.id === themeId) || themeOptions[0]
  currentThemeId.value = matchedTheme.id
  document.documentElement.setAttribute('data-theme', matchedTheme.id)
  localStorage.setItem(THEME_STORAGE_KEY, matchedTheme.id)
}

function changeTheme(themeId) {
  applyTheme(themeId)
  ElMessage.success(`已切换为${themeId}主题`)
}

function initTheme() {
  applyTheme(localStorage.getItem(THEME_STORAGE_KEY) || themeOptions[0].id)
}

function goRoute(path) {
  if (path && route.path !== path) {
    router.push(path)
  }
}

function handleGlobalSearch() {
  const keyword = menuKeyword.value.trim()
  if (!keyword) {
    return
  }

  const matchedItem = allSearchItems.find((item) => item.keywordText.includes(keyword))
  if (!matchedItem) {
    ElMessage.warning('未找到匹配的菜单，请重新输入关键字。')
    return
  }

  goRoute(matchedItem.path)
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

watch(
  () => currentPrimaryMenu.value?.id,
  async (menuId) => {
    if (!menuId) {
      return
    }
    await nextTick()
    menuRef.value?.open(menuId)
  },
  { immediate: true }
)

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

.top-level-entry {
  min-width: 150px;
  display: grid;
  gap: 4px;
  padding: 10px 16px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.12);
  color: #ffffff;
  text-align: left;
  cursor: pointer;
}

.top-level-entry__label {
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
}

.top-level-entry strong {
  font-size: 16px;
  line-height: 1.2;
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
  padding: 18px 0 12px;
  box-shadow: inset -1px 0 0 rgba(255, 255, 255, 0.04);
  overflow: hidden;
}

.sidebar-head {
  display: grid;
  gap: 6px;
  padding: 0 18px 14px;
}

.sidebar-head__caption,
.sidebar-footer span {
  color: #8b95af;
  font-size: 12px;
  letter-spacing: 1px;
}

.sidebar-head strong,
.sidebar-footer strong {
  color: #ffffff;
  font-size: 15px;
  font-weight: 600;
}

.sidebar-head p,
.sidebar-footer p {
  margin: 0;
  color: #8b95af;
  font-size: 12px;
  line-height: 1.7;
}

.sidebar-footer {
  margin-top: auto;
  padding: 16px 18px 6px;
  display: grid;
  gap: 4px;
  flex-shrink: 0;
}

.sidebar-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-gutter: stable;
  overscroll-behavior: contain;
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
  flex-wrap: wrap;
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
  width: 100%;
  border-right: none;
  background: transparent;
  --el-menu-bg-color: transparent;
  --el-menu-border-color: transparent;
  --el-menu-text-color: #d4dbeb;
  --el-menu-hover-bg-color: var(--bg-sidebar-hover);
  --el-menu-active-color: #ffffff;
}

:deep(.menu-panel .el-sub-menu__title) {
  height: 46px;
  margin: 0 0 6px;
  padding-left: 18px !important;
  color: #ffffff;
  font-weight: 600;
}

:deep(.menu-panel .el-sub-menu__title:hover) {
  background: var(--bg-sidebar-hover);
}

:deep(.menu-panel .el-menu-item) {
  height: 42px;
  margin: 0 0 4px;
  padding-left: 46px !important;
  border-left: 3px solid transparent;
  color: #d4dbeb;
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

.sidebar-body::-webkit-scrollbar {
  width: 8px;
}

.sidebar-body::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.24);
}

.sidebar-body::-webkit-scrollbar-track {
  background: transparent;
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

  .topbar-left {
    flex-direction: column;
    align-items: flex-start;
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

  .sidebar-body {
    overflow: visible;
    scrollbar-gutter: auto;
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

  .topbar-search {
    width: 100%;
  }

  .user-box {
    width: 100%;
    justify-content: space-between;
  }

  .topbar-action,
  .skin-trigger,
  .top-level-entry {
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
