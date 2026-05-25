<template>
  <div :class="['content-grid', 'dashboard-page', { 'dashboard-page--detector': viewMode === 'detector' }]" v-loading="loading">
    <section v-if="viewMode === 'leader'" class="glass-panel leader-hero">
      <div class="leader-hero__main">
        <h3>化验室运行总览</h3>
        <p>聚焦当前预警、质量趋势与流程节点进度，快速把握全局运行态势。</p>
        <div class="leader-hero__meta">
          <span>预警总量：{{ warningTotal }}</span>
          <span>近况合格率：{{ latestPassRate }}%</span>
          <span>当前待审任务：{{ topMetrics[0]?.todayValue ?? 0 }}</span>
        </div>
      </div>
      <div class="leader-hero__flow">
        <div class="leader-hero__flow-label">流程概览</div>
        <div class="leader-hero__flow-bar">
          <span
            v-for="item in processNodes.slice(0, 5)"
            :key="item.label"
            class="leader-hero__flow-item"
            @click="goRoute(item.path)"
          >
            {{ item.label }}
          </span>
        </div>
      </div>
      <div class="hero-actions">
        <button type="button" @click="goRoute('/review-result')">
          <strong>{{ warningTotal }}</strong>
          <span>待处理预警</span>
        </button>
        <button type="button" @click="goRoute('/statistics-quality')">
          <strong>{{ latestPassRate }}%</strong>
          <span>近况合格率</span>
        </button>
      </div>
    </section>

    <section v-if="viewMode === 'leader'" class="top-metric-grid">
      <button
        v-for="item in topMetrics"
        :key="item.label"
        type="button"
        :class="['top-metric-card', item.tone]"
        @click="goRoute(item.path)"
      >
        <div class="metric-title">
          <span>{{ item.label }}</span>
          <i></i>
        </div>
        <template v-if="item.realtimeValue !== null && item.realtimeValue !== undefined">
          <strong>{{ item.realtimeValue }}</strong>
          <p>{{ item.description }}</p>
        </template>
        <template v-else>
          <div class="metric-periods">
            <span>今日 <strong>{{ item.todayValue }}</strong></span>
            <span>本月 <strong>{{ item.monthValue }}</strong></span>
            <span>今年 <strong>{{ item.yearValue }}</strong></span>
          </div>
          <p>{{ item.description }}</p>
        </template>
      </button>
    </section>

    <section v-if="viewMode === 'leader'" class="glass-panel section-block process-panel">
      <div class="panel-head">
        <div>
          <h3 class="section-title">样品检测全流程节点看板</h3>
          <p>点击任意节点进入对应业务页面，后续可继续叠加超时筛选条件。</p>
        </div>
      </div>
      <div class="process-track">
        <button
          v-for="item in processNodes"
          :key="item.label"
          type="button"
          :class="['process-node', { 'is-warning': item.warning }]"
          @click="goRoute(item.path)"
        >
          <span class="node-index">{{ item.index }}</span>
          <strong>{{ item.label }}</strong>
          <em>{{ item.value }}</em>
          <small>{{ item.statusText }}</small>
        </button>
      </div>
    </section>

    <section v-if="viewMode === 'leader'" class="dashboard-lower">
      <section class="glass-panel section-block chart-panel">
        <div class="panel-head">
          <div>
            <h3 class="section-title">水质合格率趋势</h3>
            <p>最近 7 天合格率，目标线为 95%。</p>
          </div>
        </div>
        <MiniCharts type="line" :items="passRateTrendItems" :colors="['#1677ff']" />
        <div class="target-line">目标合格率：95%</div>
      </section>

      <section class="glass-panel section-block chart-panel">
        <div class="panel-head">
          <div>
            <h3 class="section-title">检测员工作量排行</h3>
            <p>按本月完成检测项次数排序。</p>
          </div>
        </div>
        <MiniCharts type="bar" :items="workloadItems" :colors="['#0f766e', '#1677ff', '#d97706', '#16a34a']" />
      </section>

      <section class="glass-panel section-block warning-panel">
        <div class="panel-head">
          <div>
            <h3 class="section-title">待处理预警</h3>
            <p>优先暴露超时、超标、仪器和质控风险。</p>
          </div>
          <el-button link type="primary" @click="goRoute('/review-result')">查看更多</el-button>
        </div>
        <div class="warning-list">
          <button
            v-for="item in warnings"
            :key="item.type"
            type="button"
            :class="['warning-item', item.priority === '高' ? 'is-high' : 'is-medium']"
            @click="goRoute(item.path)"
          >
            <div>
              <span>{{ item.type }}</span>
              <strong>{{ item.title }}</strong>
              <p>{{ item.content }}</p>
            </div>
            <em>{{ item.count }}</em>
          </button>
        </div>
      </section>
    </section>

    <section v-else class="dashboard-detector">
      <section class="glass-panel section-block detector-hero">
        <div>
          <h3 class="section-title">检测员工作台</h3>
        </div>
        <div class="detector-hero-actions">
          <button type="button" @click="goRoute('/detection-analysis')">
            <strong>{{ detectorStats.pendingCount }}</strong>
            <span>待检测</span>
          </button>
          <button type="button" @click="goRoute('/detection-analysis')">
            <strong>{{ detectorStats.todayFinishCount }}</strong>
            <span>今日完成</span>
          </button>
          <button type="button" @click="goRoute('/detection-ledger')">
            <strong>{{ detectorStats.monthFinishCount }}</strong>
            <span>本月完成</span>
          </button>
          <button type="button" @click="goRoute('/detection-history')">
            <strong>{{ detectorStats.averageMinutes }}</strong>
            <span>平均耗时(min)</span>
          </button>
        </div>
      </section>

      <section class="dashboard-detector-body">
        <section class="glass-panel section-block detector-list-panel">
          <div class="panel-head">
            <div>
              <h3 class="section-title">待检测任务</h3>
            </div>
            <el-button link type="primary" @click="goRoute('/detection-analysis')">进入处理</el-button>
          </div>
          <div class="detector-card-list">
            <template v-if="detectorTodoItems.length">
              <div
                v-for="item in detectorTodoItems"
                :key="item.id"
                class="detector-card"
                role="button"
                tabindex="0"
                @click="goRoute('/detection-analysis')"
                @keyup.enter="goRoute('/detection-analysis')"
              >
                <div class="detector-card__main">
                  <strong>{{ item.parameterName || item.title }}</strong>
                  <div class="detector-field-grid">
                    <p><span>样品编号：</span>{{ item.sampleNo || '-' }}</p>
                    <p><span>检测方法：</span>{{ item.methodName || '-' }}</p>
                  </div>
                </div>
                <div class="detector-card__meta">
                  <p><span>封签编号：</span>{{ item.sealNo || '-' }}</p>
                  <em>任务状态：{{ item.statusText || '-' }}</em>
                </div>
                <button type="button" class="detector-card__action" @click.stop="goRoute('/detection-analysis')">进入处理</button>
              </div>
            </template>
            <el-empty v-else description="暂无待检测任务" :image-size="86" />
          </div>
        </section>

        <section class="glass-panel section-block detector-list-panel">
          <div class="panel-head">
            <div>
              <h3 class="section-title">最近记录</h3>
            </div>
          </div>
          <div class="detector-record-list detector-record-scroll">
            <div v-for="item in detectorRecentRecords" :key="item.id" class="detector-record-item">
              <div class="detector-record-item__main">
                <strong>样品编号：{{ item.sampleNo || '-' }}</strong>
                <div class="detector-field-grid">
                  <p><span>检测参数：</span>{{ item.parameterText || '-' }}</p>
                  <p><span>检测结果：</span>{{ formatResultValue(item) }}</p>
                </div>
              </div>
              <div class="detector-record-item__meta">
                <p><span>判定结果：</span>{{ item.resultText || '-' }}</p>
                <p><span>更新时间：</span>{{ item.updatedTime || '-' }}</p>
                <em>审核状态：{{ item.statusText || '-' }}</em>
              </div>
            </div>
            <el-empty v-if="!detectorRecentRecords.length" description="暂无最近记录" :image-size="86" />
          </div>
        </section>
      </section>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElEmpty } from 'element-plus/es/components/empty/index.mjs'
import { ElLoadingDirective } from 'element-plus/es/components/loading/index.mjs'
import { dashboardApi, detectorDashboardApi, leaderDashboardApi } from '../api/lab'
import { getUser } from '../utils/auth'
import MiniCharts from '../components/common/MiniCharts.vue'

const router = useRouter()
const loading = ref(false)
const dashboard = ref({})
const currentUser = ref(getUser())
const vLoading = ElLoadingDirective

function toSafeNumber(value) {
  const num = typeof value === 'number' ? value : Number.parseFloat(String(value ?? '').replace(/,/g, '').trim())
  return Number.isFinite(num) ? num : 0
}

function goRoute(path) {
  if (!path) {
    return
  }
  router.push(path)
}

function formatResultValue(item) {
  if (!item || item.resultValue === null || item.resultValue === undefined || item.resultValue === '') {
    return '-'
  }
  return `${item.resultValue}${item.unit ? ` ${item.unit}` : ''}`
}

const topMetrics = computed(() => dashboard.value.topMetrics || [])
const processNodes = computed(() => dashboard.value.processNodes || [])
const warnings = computed(() => dashboard.value.warnings || [])
const passRateTrendItems = computed(() => (dashboard.value.passRateTrend || []).map((item) => ({
  label: item.label,
  value: toSafeNumber(item.value),
  normalCount: toSafeNumber(item.normalCount),
  abnormalCount: toSafeNumber(item.abnormalCount)
})))
const workloadItems = computed(() => (dashboard.value.detectorWorkloadRanking || []).map((item) => ({
  label: item.name,
  value: toSafeNumber(item.value)
})))
const warningTotal = computed(() => warnings.value.reduce((sum, item) => sum + toSafeNumber(item.count), 0))
const latestPassRate = computed(() => {
  const items = passRateTrendItems.value
  if (!items.length) {
    return 0
  }
  return items[items.length - 1].value
})
const viewMode = computed(() => {
  const roleCode = String(currentUser.value?.roleCode || '').toUpperCase()
  if (roleCode === 'DETECTOR') {
    return 'detector'
  }
  return 'leader'
})
const detectorStats = computed(() => dashboard.value.stats || {
  pendingCount: 0,
  todayFinishCount: 0,
  monthFinishCount: 0,
  averageMinutes: 0
})
const detectorTodoItems = computed(() => dashboard.value.todoItems || [])
const detectorRecentRecords = computed(() => dashboard.value.recentRecords || [])

onMounted(async () => {
  loading.value = true
  try {
    const roleCode = String(currentUser.value?.roleCode || '').toUpperCase()
    if (roleCode === 'DETECTOR') {
      dashboard.value = await detectorDashboardApi() || {}
    } else if (roleCode) {
      dashboard.value = await leaderDashboardApi() || {}
    } else {
      dashboard.value = await dashboardApi() || {}
    }
  } catch (error) {
    dashboard.value = {}
    console.warn('运行总览数据加载失败，已使用空数据兜底。', error)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.dashboard-page {
  min-height: 100%;
  gap: 16px;
}

.dashboard-page--detector {
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.leader-hero {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 18px;
  padding: 22px 24px;
  overflow: hidden;
  border: 1px solid rgba(47, 111, 159, 0.18);
  background:
    radial-gradient(circle at 5% 0%, rgba(47, 111, 159, 0.16), transparent 34%),
    linear-gradient(135deg, #ffffff 0%, #f6fbff 55%, #edf6ff 100%);
}

.leader-hero__main {
  display: flex;
  flex: 1 1 0;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}

.leader-hero h3 {
  margin: 0;
  color: var(--text-main);
  font-size: 28px;
  line-height: 1.2;
}

.leader-hero__main p {
  max-width: 520px;
  margin: 10px 0 0;
  color: var(--text-sub);
  font-size: 14px;
  line-height: 1.7;
}

.leader-hero__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.leader-hero__meta span {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.76);
  color: var(--text-main);
  font-size: 13px;
  border: 1px solid rgba(214, 225, 241, 0.88);
}

.leader-hero__flow {
  display: flex;
  flex: 0 0 320px;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
}

.leader-hero__flow-label {
  color: var(--text-sub);
  font-size: 13px;
}

.leader-hero__flow-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.leader-hero__flow-item {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  color: var(--text-main);
  font-size: 13px;
  border: 1px solid rgba(214, 225, 241, 0.88);
  cursor: pointer;
}

.hero-actions {
  display: grid;
  grid-template-columns: repeat(2, 128px);
  gap: 12px;
  align-items: stretch;
}

.hero-actions button,
.top-metric-card,
.process-node,
.warning-item {
  border: 0;
  font: inherit;
  cursor: pointer;
}

.hero-actions button {
  padding: 16px;
  border: 1px solid rgba(214, 225, 241, 0.88);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  text-align: center;
}

.hero-actions strong {
  display: block;
  color: var(--brand);
  font-size: 30px;
  line-height: 1;
}

.hero-actions span {
  display: block;
  margin-top: 10px;
  color: var(--text-sub);
  font-size: 13px;
}

.dashboard-detector {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.detector-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  min-height: 96px;
  padding: 16px 18px;
  border-color: rgba(47, 111, 159, 0.14);
  background:
    radial-gradient(circle at 0% 0%, rgba(22, 119, 255, 0.1), transparent 30%),
    linear-gradient(135deg, #ffffff 0%, #f8fbff 100%);
}

.detector-hero p {
  margin: 4px 0 0;
  color: var(--text-sub);
}

.detector-hero-actions {
  display: grid;
  grid-template-columns: repeat(4, minmax(110px, 1fr));
  gap: 10px;
}

.detector-hero-actions button {
  min-width: 112px;
  min-height: 74px;
  padding: 10px 14px;
  border: 1px solid rgba(214, 225, 241, 0.88);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.86);
  text-align: center;
  cursor: pointer;
  box-shadow: 0 8px 18px rgba(17, 54, 99, 0.05);
}

.detector-hero-actions strong {
  display: block;
  color: var(--brand);
  font-size: 28px;
  line-height: 1;
}

.detector-hero-actions span {
  display: block;
  margin-top: 8px;
  color: var(--text-sub);
  font-size: 13px;
}

.dashboard-detector-body {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(360px, 0.85fr);
  gap: 16px;
  flex: 1;
  min-height: 0;
}

.detector-card-list,
.detector-record-list {
  display: grid;
  gap: 10px;
  align-content: start;
}

.detector-record-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 2px 4px 2px 0;
  scrollbar-width: thin;
  scrollbar-color: rgba(99, 116, 139, 0.36) transparent;
}

.detector-record-scroll::-webkit-scrollbar,
.detector-card-list::-webkit-scrollbar {
  width: 6px;
}

.detector-record-scroll::-webkit-scrollbar-thumb,
.detector-card-list::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(99, 116, 139, 0.32);
}

.detector-record-scroll :deep(.el-empty),
.detector-card-list :deep(.el-empty) {
  width: 100%;
  padding: 18px 0;
}

.detector-list-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  padding: 14px;
  border-color: rgba(205, 221, 239, 0.92);
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
}

.detector-list-panel .panel-head {
  flex-shrink: 0;
  padding-bottom: 10px;
  border-bottom: 1px solid #edf2f7;
}

.detector-card-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 2px 2px 2px 0;
}

.detector-record-list {
  flex: 1;
  min-height: 0;
  align-content: start;
}

.detector-card-list :deep(.el-empty),
.detector-record-scroll :deep(.el-empty) {
  margin: auto 0;
}

.detector-card,
.detector-record-item {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 184px;
  gap: 14px 18px;
  padding: 14px 16px 14px 18px;
  border: 1px solid rgba(214, 225, 241, 0.9);
  border-radius: 15px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  text-align: left;
  box-shadow: 0 8px 18px rgba(17, 54, 99, 0.04);
  align-items: start;
}

.detector-card::before,
.detector-record-item::before {
  content: "";
  position: absolute;
  left: 0;
  top: 14px;
  bottom: 14px;
  width: 3px;
  border-radius: 0 999px 999px 0;
  background: var(--brand);
  opacity: 0.8;
}

.detector-card strong,
.detector-record-item strong {
  color: #10233f;
  font-size: 15px;
  line-height: 1.4;
}

.detector-card__main,
.detector-record-item__main {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.detector-card__meta,
.detector-record-item__meta {
  display: grid;
  gap: 8px;
  align-content: start;
  min-width: 0;
  padding-top: 2px;
}

.detector-card__meta {
  justify-self: end;
}

.detector-card p,
.detector-record-item p {
  margin: 0;
  color: #4d6280;
  font-size: 13px;
  line-height: 1.55;
}

.detector-card em,
.detector-record-item em {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 3px 10px;
  border-radius: 999px;
  color: var(--brand);
  background: var(--brand-soft);
  font-size: 12px;
  line-height: 1;
  font-style: normal;
  width: fit-content;
}

.detector-card__action {
  justify-self: end;
  min-width: 88px;
  height: 32px;
  padding: 0 14px;
  border: 1px solid rgba(22, 119, 255, 0.18);
  border-radius: 16px;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  cursor: pointer;
}

.detector-field-grid {
  display: grid;
  gap: 4px;
}

.detector-field-grid span {
  color: #2a3850;
  font-weight: 600;
}

.detector-card {
  min-height: 112px;
  grid-template-columns: minmax(0, 1fr) 180px auto;
  column-gap: 14px;
  align-items: center;
  cursor: pointer;
}

.detector-record-item {
  min-height: auto;
  padding: 16px 16px 18px 18px;
}

.detector-card__meta p,
.detector-record-item__meta p {
  font-size: 12px;
  line-height: 1.45;
}

@media (max-width: 1280px) {
  .leader-hero {
    flex-direction: column;
  }

  .leader-hero__flow {
    flex-basis: auto;
  }

  .detector-card,
  .detector-record-item {
    grid-template-columns: 1fr;
  }

  .detector-card__action {
    justify-self: start;
  }
}

.top-metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.top-metric-card {
  min-height: 154px;
  padding: 18px;
  border: 1px solid rgba(214, 225, 241, 0.9);
  border-radius: 18px;
  background: #ffffff;
  text-align: left;
  box-shadow: var(--shadow-sm);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.top-metric-card:hover,
.process-node:hover,
.warning-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.metric-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--text-main);
  font-size: 16px;
  font-weight: 700;
}

.metric-title i {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: var(--brand);
}

.top-metric-card.success .metric-title i {
  background: var(--success);
}

.top-metric-card.warning .metric-title i {
  background: var(--warning);
}

.top-metric-card.danger .metric-title i {
  background: var(--danger);
}

.top-metric-card > strong {
  display: block;
  margin-top: 22px;
  color: var(--text-main);
  font-size: 36px;
  line-height: 1;
}

.metric-periods {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 20px;
}

.metric-periods span {
  display: grid;
  gap: 8px;
  padding: 10px;
  border-radius: 12px;
  color: var(--text-sub);
  background: #f6f9fc;
  font-size: 13px;
}

.metric-periods strong {
  color: var(--text-main);
  font-size: 20px;
  line-height: 1;
}

.top-metric-card p {
  margin: 14px 0 0;
  color: var(--text-sub);
  font-size: 13px;
}

.section-title {
  margin: 0;
  font-size: 18px;
  line-height: 1.4;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.panel-head p {
  margin: 4px 0 0;
  color: var(--text-sub);
  font-size: 13px;
}

.process-panel {
  overflow-x: auto;
}

.process-track {
  min-width: 980px;
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 14px;
  position: relative;
}

.process-track::before {
  content: "";
  position: absolute;
  left: 7%;
  right: 7%;
  top: 42px;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(47, 111, 159, 0.36), transparent);
}

.process-node {
  position: relative;
  z-index: 1;
  display: grid;
  justify-items: center;
  gap: 8px;
  min-height: 138px;
  padding: 14px 10px;
  border: 1px solid rgba(214, 225, 241, 0.92);
  border-radius: 18px;
  background: #ffffff;
  text-align: center;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.node-index {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #ffffff;
  background: var(--brand);
  font-weight: 800;
}

.process-node strong {
  color: var(--text-main);
  font-size: 15px;
}

.process-node em {
  min-width: 56px;
  padding: 8px 10px;
  border-radius: 12px;
  color: var(--brand);
  background: var(--brand-soft);
  font-size: 24px;
  font-style: normal;
  font-weight: 800;
  line-height: 1;
}

.process-node small {
  color: var(--text-sub);
  font-size: 12px;
}

.process-node.is-warning {
  border-color: rgba(217, 119, 6, 0.34);
}

.process-node.is-warning em {
  color: var(--warning);
  background: rgba(217, 119, 6, 0.12);
}

.dashboard-lower {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.target-line {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  color: var(--brand);
  background: var(--brand-soft);
  font-size: 13px;
  font-weight: 700;
}

.warning-list {
  display: grid;
  gap: 10px;
}

.warning-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 48px;
  gap: 12px;
  align-items: center;
  padding: 12px;
  border: 1px solid rgba(214, 225, 241, 0.9);
  border-radius: 14px;
  background: #ffffff;
  text-align: left;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.warning-item span {
  color: var(--text-sub);
  font-size: 12px;
}

.warning-item strong {
  display: block;
  margin-top: 4px;
  color: var(--text-main);
  font-size: 14px;
}

.warning-item p {
  margin: 5px 0 0;
  color: var(--text-sub);
  font-size: 12px;
  line-height: 1.45;
}

.warning-item em {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  font-style: normal;
  font-weight: 800;
}

.warning-item.is-high em {
  color: var(--danger);
  background: rgba(220, 38, 38, 0.1);
}

.warning-item.is-medium em {
  color: var(--warning);
  background: rgba(217, 119, 6, 0.12);
}

@media (max-width: 1200px) {
  .leader-hero,
  .top-metric-grid,
  .dashboard-lower {
    grid-template-columns: 1fr;
  }

  .leader-hero {
    display: grid;
  }

  .hero-actions {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
