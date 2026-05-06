<template>
  <div class="page-shell layout-root">
    <aside class="sidebar glass-panel">
      <div class="brand-box">
        <div class="brand-mark">LAB</div>
        <div>
          <strong>阳新化验室</strong>
          <p>实验检测业务平台</p>
        </div>
      </div>
      <el-menu :default-active="$route.path" router class="menu-panel">
        <el-menu-item index="/dashboard">运行总览</el-menu-item>
        <el-menu-item index="/monitoring">监测点位</el-menu-item>
        <el-menu-item index="/samples">样品采样</el-menu-item>
        <el-menu-item index="/detections">检测分析</el-menu-item>
        <el-menu-item index="/reviews">结果审核</el-menu-item>
        <el-menu-item index="/reports">报告台账</el-menu-item>
        <el-menu-item index="/assets">仪器文档</el-menu-item>
        <el-menu-item index="/statistics">分析统计</el-menu-item>
      </el-menu>
    </aside>
    <main class="main-panel">
      <header class="topbar glass-panel">
        <div>
          <h1 class="page-title">阳新化验室管理系统</h1>
          <div class="page-subtitle">围绕采样、检测、审核与报告的轻量化实验室业务闭环</div>
        </div>
        <div class="user-box">
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
import { useRouter } from 'vue-router'
import { clearToken, getUser } from '../../utils/auth'

const router = useRouter()
const user = computed(() => getUser())

function logout() {
  clearToken()
  router.push('/login')
}
</script>

<style scoped>
.layout-root {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 18px;
  padding: 18px;
}

.sidebar {
  padding: 22px 16px;
}

.brand-box {
  display: flex;
  gap: 14px;
  align-items: center;
  margin-bottom: 26px;
}

.brand-box strong {
  font-size: 20px;
}

.brand-box p,
.user-box p {
  margin: 6px 0 0;
  color: var(--text-sub);
  font-size: 12px;
}

.brand-mark {
  width: 54px;
  height: 54px;
  display: grid;
  place-items: center;
  border-radius: 16px;
  background: linear-gradient(145deg, var(--brand), #0d9eb3);
  color: #fff;
  font-weight: 700;
}

.menu-panel {
  border-right: none;
  background: transparent;
}

.main-panel {
  min-width: 0;
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 22px 26px;
}

.user-box {
  display: flex;
  align-items: center;
  gap: 16px;
}

.view-body {
  padding-top: 18px;
}

@media (max-width: 1100px) {
  .layout-root {
    grid-template-columns: 1fr;
  }
}
</style>
