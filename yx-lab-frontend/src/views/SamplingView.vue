<template>
  <div class="content-grid sampling-page">
    <section class="stats-grid">
      <button
        v-for="item in currentStats"
        :key="item.label"
        type="button"
        :class="['metric-card', 'metric-card--action', { 'is-active': activeStatKey === item.key }]"
        @click="handleStatClick(item)"
      >
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <p>{{ item.desc }}</p>
      </button>
    </section>

    <section class="glass-panel section-block">
      <el-tabs v-model="active">
        <el-tab-pane label="采样计划" name="plans">
          <div class="section-head">
            <div>
              <h3 class="section-title">采样计划</h3>
              <p class="page-subtitle">
                统一维护周期采样计划，支持自动生成任务，也保留人工派发、暂停和恢复能力。
              </p>
            </div>
          </div>

          <div class="toolbar-panel">
            <div class="toolbar-row">
              <div class="panel-note">点击上方统计卡片可切换当前页签，并按计划状态筛选当前页数据。</div>
              <div class="toolbar-actions">
                <el-button @click="loadPlans">刷新</el-button>
                <el-button type="primary" plain @click="createPlan">新增计划</el-button>
              </div>
            </div>
          </div>

          <div class="table-card">
            <el-table class="list-table" :data="visiblePlans" stripe max-height="420" empty-text="暂无采样计划数据">
              <el-table-column prop="planName" label="计划名称" min-width="180" />
              <el-table-column prop="pointName" label="采样点位" min-width="160" />
              <el-table-column prop="samplerName" label="采样人员" width="120" />
              <el-table-column label="周期类型" width="120" header-cell-class-name="cell-center" class-name="cell-center">
                <template #default="{ row }">
                  {{ getEnumLabel(cycleTypeLabelMap, row.cycleType) }}
                </template>
              </el-table-column>
              <el-table-column prop="startTime" label="开始时间" width="170" />
              <el-table-column prop="endTime" label="结束时间" width="170" />
              <el-table-column label="状态" width="120" header-cell-class-name="cell-center" class-name="cell-center">
                <template #default="{ row }">
                  <span class="status-chip" :class="getStatusClass('planStatus', row.planStatus)">
                    {{ getEnumLabel(planStatusLabelMap, row.planStatus) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="操作" min-width="240" fixed="right" class-name="cell-center">
                <template #default="{ row }">
                  <div class="action-row">
                    <el-button
                      size="small"
                      @click="dispatch(row)"
                      :disabled="!actionablePlanStatuses.includes(row.planStatus)"
                    >
                      派发
                    </el-button>
                    <el-button
                      size="small"
                      @click="pausePlan(row)"
                      :disabled="!actionablePlanStatuses.includes(row.planStatus)"
                    >
                      暂停
                    </el-button>
                    <el-button
                      size="small"
                      @click="resumePlan(row)"
                      :disabled="row.planStatus !== pausedPlanStatus"
                    >
                      恢复
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>

            <TablePagination
              v-model:current-page="planQuery.pageNum"
              v-model:page-size="planQuery.pageSize"
              :total="planTotal"
              @change="loadPlans"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="采样任务" name="tasks">
          <div class="section-head">
            <div>
              <h3 class="section-title">采样任务</h3>
              <p class="page-subtitle">
                跟踪任务执行过程，支持开始、废弃、恢复和完成，确保现场采样过程可追溯。
              </p>
            </div>
          </div>

          <div class="toolbar-panel">
            <div class="toolbar-row">
              <div class="panel-note">点击上方统计卡片可切换当前页签，并按任务状态筛选当前页数据。</div>
              <div class="toolbar-actions">
                <el-button @click="loadTasks">刷新</el-button>
                <el-button type="primary" plain @click="completeFirstPendingTask" :disabled="!firstCompletableTask">
                  完成首条任务
                </el-button>
              </div>
            </div>
          </div>

          <div class="table-card">
            <el-table class="list-table" :data="visibleTasks" stripe max-height="420" empty-text="暂无采样任务数据">
              <el-table-column prop="pointName" label="点位名称" min-width="180" />
              <el-table-column prop="samplerName" label="采样人员" width="120" />
              <el-table-column label="样品类型" width="120" header-cell-class-name="cell-center" class-name="cell-center">
                <template #default="{ row }">
                  {{ getEnumLabel(sampleTypeLabelMap, row.sampleType) }}
                </template>
              </el-table-column>
              <el-table-column label="状态" width="120" header-cell-class-name="cell-center" class-name="cell-center">
                <template #default="{ row }">
                  <span class="status-chip" :class="getStatusClass('taskStatus', row.taskStatus)">
                    {{ getEnumLabel(taskStatusLabelMap, row.taskStatus) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="samplingTime" label="采样时间" width="170" />
              <el-table-column prop="startedTime" label="开始时间" width="170" />
              <el-table-column prop="abandonReason" label="废弃原因" min-width="160" show-overflow-tooltip />
              <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
              <el-table-column label="操作" min-width="290" fixed="right" class-name="cell-center">
                <template #default="{ row }">
                  <div class="action-row">
                    <el-button size="small" @click="startTask(row)" :disabled="row.taskStatus !== pendingTaskStatus">
                      开始
                    </el-button>
                    <el-button
                      size="small"
                      @click="abandonTask(row)"
                      :disabled="!completableTaskStatuses.includes(row.taskStatus)"
                    >
                      废弃
                    </el-button>
                    <el-button size="small" @click="resumeTask(row)" :disabled="row.taskStatus !== abandonedTaskStatus">
                      恢复
                    </el-button>
                    <el-button
                      size="small"
                      type="primary"
                      plain
                      @click="completeTask(row)"
                      :disabled="!completableTaskStatuses.includes(row.taskStatus)"
                    >
                      完成
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>

            <TablePagination
              v-model:current-page="taskQuery.pageNum"
              v-model:page-size="taskQuery.pageSize"
              :total="taskTotal"
              @change="loadTasks"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="样品台账" name="samples">
          <div class="section-head">
            <div>
              <h3 class="section-title">样品台账</h3>
              <p class="page-subtitle">
                查看样品登录结果、封签信息和流程留痕，便于后续检测、审核和报告出具。
              </p>
            </div>
          </div>

          <div class="toolbar-panel">
            <div class="toolbar-row">
              <div class="panel-note">点击上方统计卡片可切换当前页签，并按样品状态筛选当前页数据。</div>
              <div class="toolbar-actions">
                <el-button @click="loadSamples">刷新</el-button>
                <el-button type="primary" plain @click="loginSample" :disabled="!firstLoggableTask">
                  样品登录
                </el-button>
              </div>
            </div>
          </div>

          <div class="table-card">
            <el-table class="list-table" :data="visibleSamples" stripe max-height="420" empty-text="暂无样品台账数据">
              <el-table-column prop="sampleNo" label="样品编号" min-width="180" />
              <el-table-column prop="sealNo" label="封签编号" min-width="180" />
              <el-table-column prop="pointName" label="点位名称" min-width="160" />
              <el-table-column label="样品类型" width="120" header-cell-class-name="cell-center" class-name="cell-center">
                <template #default="{ row }">
                  {{ getEnumLabel(sampleTypeLabelMap, row.sampleType) }}
                </template>
              </el-table-column>
              <el-table-column label="状态" width="120" header-cell-class-name="cell-center" class-name="cell-center">
                <template #default="{ row }">
                  <span class="status-chip" :class="getStatusClass('sampleStatus', row.sampleStatus)">
                    {{ getEnumLabel(sampleStatusLabelMap, row.sampleStatus) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="samplingTime" label="采样时间" width="170" />
              <el-table-column prop="storageCondition" label="保存条件" width="140" />
              <el-table-column prop="resultSummary" label="结果摘要" min-width="180" show-overflow-tooltip />
              <el-table-column prop="traceLog" label="流程留痕" min-width="260" show-overflow-tooltip />
            </el-table>

            <TablePagination
              v-model:current-page="sampleQuery.pageNum"
              v-model:page-size="sampleQuery.pageSize"
              :total="sampleTotal"
              @change="loadSamples"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import TablePagination from '../components/common/TablePagination.vue'
import {
  abandonSamplingTaskApi,
  completeSamplingTaskApi,
  createSamplingPlanApi,
  dispatchSamplingPlanApi,
  fetchSamplesApi,
  fetchSamplingPlansApi,
  fetchSamplingTasksApi,
  loginSampleApi,
  pauseSamplingPlanApi,
  resumeSamplingPlanApi,
  resumeSamplingTaskApi,
  startSamplingTaskApi
} from '../api/lab'
import {
  activePlanStatus,
  abandonedTaskStatus,
  actionablePlanStatuses,
  completedPlanStatus,
  completedSampleStatus,
  completedTaskStatus,
  completableTaskStatuses,
  cycleTypeLabelMap,
  dailyCycleType,
  DEFAULT_PAGE_SIZE,
  dispatchedPlanStatuses,
  factorySampleType,
  getEnumLabel,
  getStatusClass,
  inProgressTaskStatus,
  loggedSampleStatus,
  pausedPlanStatus,
  pendingTaskStatus,
  planStatusLabelMap,
  retestSampleStatus,
  reviewingSampleStatus,
  sampleStatusLabelMap,
  sampleTypeLabelMap,
  taskStatusLabelMap,
  routineSamplingType
} from '../utils/labEnums'

const active = ref('plans')
const activeStatKey = ref('plans:all')
const planQuery = reactive({ pageNum: 1, pageSize: DEFAULT_PAGE_SIZE })
const taskQuery = reactive({ pageNum: 1, pageSize: DEFAULT_PAGE_SIZE })
const sampleQuery = reactive({ pageNum: 1, pageSize: DEFAULT_PAGE_SIZE })

const plans = ref([])
const tasks = ref([])
const samples = ref([])
const planTotal = ref(0)
const taskTotal = ref(0)
const sampleTotal = ref(0)

const firstCompletableTask = computed(() =>
  tasks.value.find((item) => completableTaskStatuses.includes(item.taskStatus))
)

const firstLoggableTask = computed(() =>
  tasks.value.find((item) => item.taskStatus === completedTaskStatus
    && !samples.value.some((sample) => sample.taskId === item.id))
)

const visiblePlans = computed(() => {
  if (activeStatKey.value === 'plans:active') {
    return plans.value.filter((item) => item.planStatus === activePlanStatus)
  }
  if (activeStatKey.value === 'plans:paused') {
    return plans.value.filter((item) => item.planStatus === pausedPlanStatus)
  }
  if (activeStatKey.value === 'plans:dispatched') {
    return plans.value.filter((item) => dispatchedPlanStatuses.includes(item.planStatus))
  }
  if (activeStatKey.value === 'plans:completed') {
    return plans.value.filter((item) => item.planStatus === completedPlanStatus)
  }
  return plans.value
})

const visibleTasks = computed(() => {
  if (activeStatKey.value === 'tasks:pending') {
    return tasks.value.filter((item) => item.taskStatus === pendingTaskStatus)
  }
  if (activeStatKey.value === 'tasks:progress') {
    return tasks.value.filter((item) => item.taskStatus === inProgressTaskStatus)
  }
  if (activeStatKey.value === 'tasks:abandoned') {
    return tasks.value.filter((item) => item.taskStatus === abandonedTaskStatus)
  }
  if (activeStatKey.value === 'tasks:completed') {
    return tasks.value.filter((item) => item.taskStatus === completedTaskStatus)
  }
  return tasks.value
})

const visibleSamples = computed(() => {
  if (activeStatKey.value === 'samples:logged') {
    return samples.value.filter((item) => item.sampleStatus === loggedSampleStatus)
  }
  if (activeStatKey.value === 'samples:reviewing') {
    return samples.value.filter((item) => item.sampleStatus === reviewingSampleStatus)
  }
  if (activeStatKey.value === 'samples:retest') {
    return samples.value.filter((item) => item.sampleStatus === retestSampleStatus)
  }
  if (activeStatKey.value === 'samples:completed') {
    return samples.value.filter((item) => item.sampleStatus === completedSampleStatus)
  }
  return samples.value
})

const currentStats = computed(() => {
  if (active.value === 'plans') {
    return [
      { key: 'plans:all', tab: 'plans', label: '计划总数', value: planTotal.value, desc: '采样计划台账记录总量' },
      { key: 'plans:page', tab: 'plans', label: '本页记录', value: plans.value.length, desc: '当前分页加载的计划条数' },
      {
        key: 'plans:active',
        tab: 'plans',
        label: '启用中',
        value: plans.value.filter((item) => item.planStatus === activePlanStatus).length,
        desc: '当前页处于启用状态的采样计划'
      },
      {
        key: 'plans:paused',
        tab: 'plans',
        label: '已暂停',
        value: plans.value.filter((item) => item.planStatus === pausedPlanStatus).length,
        desc: '当前页已暂停的采样计划'
      },
      {
        key: 'plans:dispatched',
        tab: 'plans',
        label: '已派发',
        value: plans.value.filter((item) => dispatchedPlanStatuses.includes(item.planStatus)).length,
        desc: '当前页已生成采样任务的采样计划'
      },
      {
        key: 'plans:completed',
        tab: 'plans',
        label: '已完成',
        value: plans.value.filter((item) => item.planStatus === completedPlanStatus).length,
        desc: '当前页已完成闭环的采样计划'
      }
    ]
  }

  if (active.value === 'tasks') {
    return [
      { key: 'tasks:all', tab: 'tasks', label: '任务总数', value: taskTotal.value, desc: '采样任务台账记录总量' },
      { key: 'tasks:page', tab: 'tasks', label: '本页记录', value: tasks.value.length, desc: '当前分页加载的任务条数' },
      {
        key: 'tasks:pending',
        tab: 'tasks',
        label: '待处理',
        value: tasks.value.filter((item) => item.taskStatus === pendingTaskStatus).length,
        desc: '尚未开始处理的采样任务'
      },
      {
        key: 'tasks:progress',
        tab: 'tasks',
        label: '进行中',
        value: tasks.value.filter((item) => item.taskStatus === inProgressTaskStatus).length,
        desc: '正在执行中的采样任务'
      },
      {
        key: 'tasks:abandoned',
        tab: 'tasks',
        label: '已废弃',
        value: tasks.value.filter((item) => item.taskStatus === abandonedTaskStatus).length,
        desc: '因现场异常被废弃的任务'
      },
      {
        key: 'tasks:completed',
        tab: 'tasks',
        label: '已完成',
        value: tasks.value.filter((item) => item.taskStatus === completedTaskStatus).length,
        desc: '已完成采样并提交现场数据的任务'
      }
    ]
  }

  return [
    { key: 'samples:all', tab: 'samples', label: '样品总数', value: sampleTotal.value, desc: '样品台账记录总量' },
    { key: 'samples:page', tab: 'samples', label: '本页记录', value: samples.value.length, desc: '当前分页加载的样品条数' },
    {
      key: 'samples:logged',
      tab: 'samples',
      label: '已登录',
      value: samples.value.filter((item) => item.sampleStatus === loggedSampleStatus).length,
      desc: '已完成样品登录的样品'
    },
    {
      key: 'samples:reviewing',
      tab: 'samples',
      label: '审核中',
      value: samples.value.filter((item) => item.sampleStatus === reviewingSampleStatus).length,
      desc: '检测完成后进入审核流程的样品'
    },
    {
      key: 'samples:retest',
      tab: 'samples',
      label: '待重检',
      value: samples.value.filter((item) => item.sampleStatus === retestSampleStatus).length,
      desc: '因退回重检而待重新处理的样品'
    },
    {
      key: 'samples:completed',
      tab: 'samples',
      label: '已完成',
      value: samples.value.filter((item) => item.sampleStatus === completedSampleStatus).length,
      desc: '已完成检测与审核闭环的样品'
    }
  ]
})

function handleStatClick(item) {
  active.value = item.tab
  activeStatKey.value = activeStatKey.value === item.key ? `${item.tab}:all` : item.key
}

async function loadPlans() {
  const result = await fetchSamplingPlansApi(planQuery)
  plans.value = result.records || []
  planTotal.value = result.total || 0
}

async function loadTasks() {
  const result = await fetchSamplingTasksApi(taskQuery)
  tasks.value = result.records || []
  taskTotal.value = result.total || 0
}

async function loadSamples() {
  const result = await fetchSamplesApi(sampleQuery)
  samples.value = result.records || []
  sampleTotal.value = result.total || 0
}

async function createPlan() {
  await createSamplingPlanApi({
    planName: `常规采样计划-${dayjs().format('MMDD-HHmm')}`,
    pointId: 2001,
    pointName: '城东水厂出厂水',
    startTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    endTime: dayjs().add(7, 'day').format('YYYY-MM-DD HH:mm:ss'),
    samplerId: 1002,
    samplerName: '采样员',
    samplingType: routineSamplingType,
    sampleType: factorySampleType,
    cycleType: dailyCycleType
  })
  ElMessage.success('周期采样计划已创建，系统将按计划时间自动生成采样任务。')
  planQuery.pageNum = 1
  await loadPlans()
}

async function dispatch(row) {
  await dispatchSamplingPlanApi({
    planId: row.id,
    samplingTime: row.startTime || dayjs().format('YYYY-MM-DD HH:mm:ss')
  })
  ElMessage.success('采样计划已派发，并已同步生成采样任务。')
  planQuery.pageNum = 1
  taskQuery.pageNum = 1
  await Promise.all([loadPlans(), loadTasks()])
}

async function pausePlan(row) {
  await pauseSamplingPlanApi(row.id)
  ElMessage.success('采样计划已暂停。')
  await loadPlans()
}

async function resumePlan(row) {
  await resumeSamplingPlanApi(row.id)
  ElMessage.success('采样计划已恢复。')
  await loadPlans()
}

async function startTask(row) {
  await startSamplingTaskApi(row.id, { remark: '采样任务开始执行' })
  ElMessage.success('采样任务已开始执行。')
  await loadTasks()
}

async function abandonTask(row) {
  await abandonSamplingTaskApi(row.id, {
    reason: '现场条件暂不满足采样要求',
    remark: '请确认现场情况后重新安排采样任务'
  })
  ElMessage.success('采样任务已废弃。')
  await loadTasks()
}

async function resumeTask(row) {
  await resumeSamplingTaskApi(row.id, { remark: '采样任务恢复为待处理状态' })
  ElMessage.success('采样任务已恢复。')
  await loadTasks()
}

async function completeTask(row) {
  await completeSamplingTaskApi({
    taskId: row.id,
    onsiteMetrics: 'pH=7.2；余氯=0.4；浊度=0.5',
    remark: '现场采样完成'
  })
  ElMessage.success('采样任务已完成。')
  taskQuery.pageNum = 1
  await Promise.all([loadTasks(), loadPlans()])
}

async function completeFirstPendingTask() {
  if (!firstCompletableTask.value) {
    ElMessage.warning('当前没有可完成的采样任务。')
    return
  }
  await completeTask(firstCompletableTask.value)
}

async function loginSample() {
  const completedTask = firstLoggableTask.value
  if (!completedTask) {
    ElMessage.warning('当前没有待登录的已完成采样任务。')
    return
  }

  const sample = await loginSampleApi({
    taskId: completedTask.id,
    pointId: completedTask.pointId || 2001,
    pointName: completedTask.pointName || '城东水厂出厂水',
    sampleType: completedTask.sampleType || factorySampleType,
    detectionItems: 'pH、浊度、余氯、氨氮',
    samplingTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    samplerId: completedTask.samplerId || 1002,
    samplerName: completedTask.samplerName || '采样员',
    weather: '晴',
    storageCondition: '冷藏'
  })
  ElMessage.success(`样品登录完成，封签编号：${sample?.sealNo || '待系统生成'}`)
  sampleQuery.pageNum = 1
  await loadSamples()
}

onMounted(() => {
  loadPlans()
  loadTasks()
  loadSamples()
})
</script>

<style scoped>
.sampling-page {
  gap: 16px;
}

.metric-card--action {
  width: 100%;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.metric-card--action:hover,
.metric-card--action:focus-visible,
.metric-card--action.is-active {
  border-color: color-mix(in srgb, var(--brand) 48%, #ffffff 52%);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
  outline: none;
}

.metric-card p {
  margin: 10px 0 0;
  color: var(--text-sub);
  font-size: 14px;
  line-height: 1.6;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}
</style>
