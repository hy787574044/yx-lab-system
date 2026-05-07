<template>
  <div class="page-shell layout-root">
    <aside class="sidebar glass-panel">
      <div class="brand-box">
        <div class="brand-mark">LAB</div>
        <div class="brand-copy">
          <strong>阳新化验室</strong>
          <p>实验室检测业务平台</p>
        </div>
      </div>

      <el-menu :default-active="$route.path" router class="menu-panel">
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>运行总览</span>
        </el-menu-item>
        <el-menu-item index="/monitoring">
          <el-icon><LocationFilled /></el-icon>
          <span>监测点位</span>
        </el-menu-item>
        <el-menu-item index="/samples">
          <el-icon><Tickets /></el-icon>
          <span>样品采样</span>
        </el-menu-item>
        <el-menu-item index="/detections">
          <el-icon><List /></el-icon>
          <span>检测分析</span>
        </el-menu-item>
        <el-menu-item index="/reviews">
          <el-icon><DocumentChecked /></el-icon>
          <span>结果审核</span>
        </el-menu-item>
        <el-menu-item index="/reports">
          <el-icon><Document /></el-icon>
          <span>报告台账</span>
        </el-menu-item>
        <el-menu-item index="/assets">
          <el-icon><Files /></el-icon>
          <span>仪器文档</span>
        </el-menu-item>
        <el-menu-item index="/statistics">
          <el-icon><PieChart /></el-icon>
          <span>分析统计</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <main class="main-panel">
      <header class="topbar glass-panel">
        <div class="title-block">
          <h1 class="page-title">{{ currentTitle }}</h1>
          <div class="page-subtitle">{{ currentSubtitle }}</div>
        </div>

        <div class="user-box">
          <div class="user-avatar">
            {{ (user.realName || user.username || 'A').slice(0, 1) }}
          </div>
          <div>
            <strong>{{ user.realName || user.username }}</strong>
            <p>{{ user.roleCode || 'ADMIN' }}</p>
          </div>
          <el-button text @click="logout">退出</el-button>
        </div>
      </header>

      <section class="view-body">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  DataAnalysis,
  Document,
  DocumentChecked,
  Files,
  List,
  LocationFilled,
  PieChart,
  Tickets
} from '@element-plus/icons-vue'
import { clearToken, getUser } from '../../utils/auth'

const router = useRouter()
const route = useRoute()
const user = computed(() => getUser())
const currentTitle = computed(() => route.meta?.title || '阳新化验室管理系统')
const currentSubtitle = computed(() => route.meta?.subtitle || '围绕采样、检测、审核与报告的轻量化实验室业务闭环')

function logout() {
  clearToken()
  router.push('/login')
}
</script>

<style scoped>
.layout-root {
  width: 100%;
  min-height: 100vh;
  overflow-x: hidden;
  background: var(--bg-page);
}

.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  width: 190px;
  height: 100vh;
  padding: 18px 12px;
  background: var(--bg-sidebar);
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
  border-left: none;
  box-shadow: none;
}

.brand-box {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 20px;
  padding: 0 6px;
}

.brand-copy strong {
  display: block;
  font-size: 18px;
  line-height: 1.3;
}

.brand-copy p,
.user-box p {
  margin: 4px 0 0;
  color: var(--text-sub);
  font-size: 14px;
  line-height: 1.5;
}

.brand-mark {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: var(--brand);
  color: #fff;
  font-weight: 700;
}

.menu-panel {
  border-right: none;
  background: transparent;
}

.main-panel {
  min-width: 0;
  width: calc(100% - 190px);
  max-width: calc(100vw - 190px);
  min-height: 100vh;
  margin-left: 190px;
  padding: 0 16px 16px;
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 10;
  min-height: 82px;
  padding: 14px 24px;
  margin-bottom: 16px;
  border-top-left-radius: 0;
  border-top-right-radius: 0;
}

.title-block {
  min-width: 0;
}

.user-box {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-left: 16px;
}

.user-avatar {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: linear-gradient(180deg, #f6fbff 0%, #e8f1ff 100%);
  color: var(--brand);
  font-size: 16px;
  font-weight: 700;
  border: 1px solid #d8e7ff;
}

.view-body {
  min-width: 0;
  padding-bottom: 8px;
}

:deep(.el-menu) {
  --el-menu-bg-color: transparent;
  --el-menu-border-color: transparent;
  --el-menu-text-color: var(--text-main);
  --el-menu-active-color: var(--brand);
}

:deep(.el-menu-item) {
  height: 44px;
  margin-bottom: 8px;
  border-radius: 10px;
  font-size: 14px;
  gap: 10px;
}

:deep(.el-menu-item .el-icon) {
  font-size: 18px;
}

:deep(.el-menu-item:hover) {
  background: #edf5ff;
}

:deep(.el-menu-item.is-active) {
  background: var(--brand-soft);
  font-weight: 700;
}

@media (max-width: 1100px) {
  .sidebar {
    position: static;
    width: auto;
    height: auto;
    margin: 12px;
    border-radius: 16px;
    border-left: 1px solid var(--line-soft);
  }

  .main-panel {
    width: auto;
    max-width: none;
    margin-left: 0;
    padding: 0 12px 12px;
  }
}

@media (max-width: 768px) {
  .topbar {
    align-items: flex-start;
    gap: 12px;
    flex-direction: column;
    min-height: auto;
  }

  .user-box {
    width: 100%;
    justify-content: space-between;
    padding-left: 0;
  }
}
</style>
