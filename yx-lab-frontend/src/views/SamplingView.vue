<template>
  <div class="content-grid sampling-page">
    <section class="glass-panel section-block page-hero">
      <div>
        <h2 class="page-title">{{ currentScene.title }}</h2>
        <p class="page-subtitle">{{ currentScene.subtitle }}</p>
      </div>
      <div class="hero-tags">
        <span
          v-for="tag in currentScene.tags"
          :key="tag.label"
          :class="['status-chip', tag.type]"
        >
          {{ tag.label }} {{ tag.value }}
        </span>
      </div>
    </section>

    <section class="stats-grid">
      <button
        v-for="item in currentStats"
        :key="item.key"
        type="button"
        :class="['metric-card', 'metric-card--action', { 'is-active': activeStatKey === item.key }]"
        @click="handleStatClick(item.key)"
      >
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <p>{{ item.desc }}</p>
      </button>
    </section>

    <section class="glass-panel section-block">
      <div class="section-head">
        <div>
          <h3 class="section-title">{{ currentScene.tableTitle }}</h3>
          <p class="page-subtitle">{{ currentScene.tableSubtitle }}</p>
        </div>
      </div>

      <div class="toolbar-panel">
        <div class="toolbar-row">
          <div class="panel-note">{{ currentScene.note }}</div>
          <div class="toolbar-actions">
            <el-button @click="refreshCurrentScene">刷新</el-button>
            <el-button
              v-if="baseScene.key === 'task-assign'"
              type="primary"
              plain
              :disabled="!firstCompletableTask"
              @click="completeFirstPendingTask"
            >
              完成首条任务
            </el-button>
            <el-button
              v-if="baseScene.key === 'sample-login'"
              type="primary"
              plain
              :disabled="!firstLoggableTask"
              @click="loginSample"
            >
              样品登录
            </el-button>
            <el-button
              v-if="baseScene.key === 'task-history'"
              type="primary"
              plain
              @click="goRoute('/sample-ledger')"
            >
              查看样品台账
            </el-button>
            <el-button
              v-if="baseScene.key === 'task-ledger'"
              type="primary"
              plain
              @click="goRoute('/sample-login')"
            >
              前往样品登录
            </el-button>
            <el-button
              v-if="baseScene.key === 'sample-ledger'"
              type="primary"
              plain
              @click="goRoute('/review-result')"
            >
              前往结果审查
            </el-button>
          </div>
        </div>
      </div>

      <div class="table-card">
        <el-table
          v-if="isTaskScene"
          class="list-table"
          :data="visibleTasks"
          stripe
          max-height="460"
          :empty-text="baseScene.emptyText"
        >
          <el-table-column prop="pointName" label="点位名称" min-width="180" />
          <el-table-column prop="samplerName" label="采样人员" width="120" />
          <el-table-column label="样品类型" width="120" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">
              {{ getEnumLabel(sampleTypeLabelMap, row.sampleType) }}
            </template>
          </el-table-column>
          <el-table-column label="任务状态" width="120" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">
              <span class="status-chip" :class="getStatusClass('taskStatus', row.taskStatus)">
                {{ getEnumLabel(taskStatusLabelMap, row.taskStatus) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="样品登记" width="120" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">
              <span :class="['status-chip', isTaskLogged(row.id) ? 'success' : 'warning']">
                {{ isTaskLogged(row.id) ? '已登记' : '待登记' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="samplingTime" label="计划采样时间" width="170" />
          <el-table-column prop="startedTime" label="开始时间" width="170" />
          <el-table-column prop="abandonReason" label="废弃原因" min-width="160" show-overflow-tooltip />
          <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
          <el-table-column
            v-if="baseScene.allowTaskActions"
            label="操作"
            min-width="290"
            fixed="right"
            class-name="cell-center"
          >
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

        <el-table
          v-else
          class="list-table"
          :data="visibleSamples"
          stripe
          max-height="460"
          :empty-text="baseScene.emptyText"
        >
          <el-table-column prop="sampleNo" label="样品编号" min-width="180" />
          <el-table-column prop="sealNo" label="封签编号" min-width="180" />
          <el-table-column prop="pointName" label="点位名称" min-width="160" />
          <el-table-column label="样品类型" width="120" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">
              {{ getEnumLabel(sampleTypeLabelMap, row.sampleType) }}
            </template>
          </el-table-column>
          <el-table-column label="样品状态" width="120" header-cell-class-name="cell-center" class-name="cell-center">
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
          v-if="isTaskScene"
          v-model:current-page="taskQuery.pageNum"
          v-model:page-size="taskQuery.pageSize"
          :total="taskTotal"
          @change="loadTasks"
        />
        <TablePagination
          v-else
          v-model:current-page="sampleQuery.pageNum"
          v-model:page-size="sampleQuery.pageSize"
          :total="sampleTotal"
          @change="loadSamples"
        />
      </div>
    </section>

    <section v-if="baseScene.showPlanSection" class="glass-panel section-block">
      <div class="section-head">
        <div>
          <h3 class="section-title">周期采样计划</h3>
          <p class="page-subtitle">任务分配页保留周期计划入口，支持自动派发前的创建、暂停与恢复。</p>
        </div>
      </div>

      <section class="stats-grid plan-stats">
        <div v-for="item in planStats" :key="item.label" class="metric-card metric-card--static">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <p>{{ item.desc }}</p>
        </div>
      </section>

      <div class="toolbar-panel">
        <div class="toolbar-row">
          <div class="panel-note">这里用于承接周期计划与自动任务能力，当前与任务分配场景联动展示。</div>
          <div class="toolbar-actions">
            <el-button @click="loadPlans">刷新计划</el-button>
            <el-button type="primary" plain @click="createPlan">新增计划</el-button>
          </div>
        </div>
      </div>

      <div class="table-card">
        <el-table class="list-table" :data="plans" stripe max-height="380" empty-text="暂无采样计划数据">
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
          <el-table-column label="计划状态" width="120" header-cell-class-name="cell-center" class-name="cell-center">
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
    </section>

    <section class="scene-grid">
      <div class="glass-panel section-block">
        <div class="section-head">
          <h3 class="section-title">页面说明</h3>
        </div>
        <div class="scene-copy">
          <p>{{ baseScene.guide }}</p>
          <p>当前页已按菜单语义拆分，避免“同一页面换标题”的空转体验。</p>
        </div>
      </div>

      <div class="glass-panel section-block">
        <div class="section-head">
          <h3 class="section-title">关联入口</h3>
        </div>
        <div class="quick-links">
          <button
            v-for="item in baseScene.quickLinks"
            :key="item.path"
            type="button"
            class="quick-link"
            @click="goRoute(item.path)"
          >
            <strong>{{ item.label }}</strong>
            <span>{{ item.desc }}</span>
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
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
  routineSamplingType,
  sampleStatusLabelMap,
  sampleTypeLabelMap,
  taskStatusLabelMap
} from '../utils/labEnums'

const route = useRoute()
const router = useRouter()

const planQuery = reactive({ pageNum: 1, pageSize: DEFAULT_PAGE_SIZE })
const taskQuery = reactive({ pageNum: 1, pageSize: DEFAULT_PAGE_SIZE })
const sampleQuery = reactive({ pageNum: 1, pageSize: DEFAULT_PAGE_SIZE })

const plans = ref([])
const tasks = ref([])
const samples = ref([])
const planTotal = ref(0)
const taskTotal = ref(0)
const sampleTotal = ref(0)
const activeStatKey = ref('tasks:pending')

function toSafeNumber(value) {
  const num = typeof value === 'number' ? value : Number.parseFloat(String(value ?? '').replace(/,/g, '').trim())
  return Number.isFinite(num) ? num : 0
}

const sceneMap = {
  '/task-assign': {
    key: 'task-assign',
    title: '任务分配',
    subtitle: '聚焦待处理采样任务与周期计划派发，适合班组长或调度人员快速推进采样执行。',
    tableTitle: '待办采样任务',
    tableSubtitle: '默认聚焦待处理任务，并保留开始、废弃、恢复、完成等现场执行动作。',
    note: '任务分配页强调“今天要做什么”，上方统计卡可切换到进行中、已完成、已废弃视角。',
    guide: '如果需要安排周期计划，可直接在本页下方创建并派发；任务完成后再进入样品登录。',
    mode: 'task',
    defaultStatKey: 'tasks:pending',
    allowTaskActions: true,
    showPlanSection: true,
    emptyText: '暂无待处理采样任务数据',
    taskFilter: () => true,
    sampleFilter: () => true,
    quickLinks: [
      { path: '/sample-login', label: '样品登录', desc: '将已完成采样任务登记为正式样品' },
      { path: '/task-history', label: '历史任务', desc: '查看已完成或已废弃的采样执行记录' },
      { path: '/sample-ledger', label: '样品台账', desc: '查看样品封签、状态与流程留痕' }
    ]
  },
  '/task-history': {
    key: 'task-history',
    title: '历史任务',
    subtitle: '回看已经完成或已废弃的采样任务，方便核对现场执行情况与补录链路。',
    tableTitle: '历史采样任务',
    tableSubtitle: '本页自动聚焦已完成、已废弃任务，操作区切换为只读查询视角。',
    note: '历史任务页强调追溯与复盘，不再承载现场执行按钮，避免误操作。',
    guide: '如果历史任务已经形成样品，可通过关联入口直接跳到样品台账继续核验封签与留痕。',
    mode: 'task',
    defaultStatKey: 'tasks:completed',
    allowTaskActions: false,
    showPlanSection: false,
    emptyText: '暂无历史采样任务数据',
    taskFilter: (item) => [completedTaskStatus, abandonedTaskStatus].includes(item.taskStatus),
    sampleFilter: () => true,
    quickLinks: [
      { path: '/sample-ledger', label: '样品台账', desc: '查看历史任务生成的样品与流程留痕' },
      { path: '/review-history', label: '历史审查', desc: '继续追踪后续检测审核结果' },
      { path: '/report-ledger', label: '报告台账', desc: '核对最终报告是否已生成与发布' }
    ]
  },
  '/task-ledger': {
    key: 'task-ledger',
    title: '任务台账',
    subtitle: '集中查看全部采样任务记录，适合做全量核对、跨状态追踪与台账盘点。',
    tableTitle: '采样任务台账',
    tableSubtitle: '保留全量任务清单与样品登记联动状态，便于纵向串联任务与样品闭环。',
    note: '任务台账页用于全量盘点，支持按任务状态切换，但默认保持全量视图。',
    guide: '如需补录样品，可直接跳往样品登录；如需回看执行结果，可切换到历史任务页。',
    mode: 'task',
    defaultStatKey: 'tasks:all',
    allowTaskActions: false,
    showPlanSection: false,
    emptyText: '暂无采样任务台账数据',
    taskFilter: () => true,
    sampleFilter: () => true,
    quickLinks: [
      { path: '/sample-login', label: '样品登录', desc: '承接已完成任务并生成样品封签' },
      { path: '/task-history', label: '历史任务', desc: '只看已完成与已废弃任务' },
      { path: '/detection-analysis', label: '检测分析', desc: '继续进入实验室检测流程' }
    ]
  },
  '/sample-login': {
    key: 'sample-login',
    title: '样品登录',
    subtitle: '将已完成采样任务转成正式样品，生成封签编号并留存后续检测、审核、报告链路。',
    tableTitle: '已登记样品',
    tableSubtitle: '默认展示已登记样品，并通过统计卡切换待审核、退回重检、闭环完成等状态。',
    note: '样品登录页优先解决“哪些任务还没登记样品”，点击登录后即可生成封签并进入后续闭环。',
    guide: '如果本页没有可登记样品，请先返回任务分配完成采样任务；若样品已登记，可继续前往检测分析。',
    mode: 'sample',
    defaultStatKey: 'samples:logged',
    allowTaskActions: false,
    showPlanSection: false,
    emptyText: '暂无样品登录数据',
    taskFilter: () => true,
    sampleFilter: () => true,
    quickLinks: [
      { path: '/task-assign', label: '任务分配', desc: '先完成现场采样任务，再进行样品登录' },
      { path: '/detection-analysis', label: '检测分析', desc: '样品登记完成后进入实验室检测流程' },
      { path: '/sample-ledger', label: '样品台账', desc: '查看全量样品、封签与流程留痕' }
    ]
  },
  '/sample-ledger': {
    key: 'sample-ledger',
    title: '样品台账',
    subtitle: '集中查看全部样品、封签编号、保存条件、结果摘要和全流程留痕信息。',
    tableTitle: '样品全量台账',
    tableSubtitle: '本页不再强调登录动作，转为全量追踪样品状态与闭环结果。',
    note: '样品台账页适合盘点封签与状态流转，可通过统计卡快速定位待审核、退回重检、已完成样品。',
    guide: '如需新增样品，请返回样品登录；如需继续推进流程，可跳转到检测分析或结果审查。',
    mode: 'sample',
    defaultStatKey: 'samples:all',
    allowTaskActions: false,
    showPlanSection: false,
    emptyText: '暂无样品台账数据',
    taskFilter: () => true,
    sampleFilter: () => true,
    quickLinks: [
      { path: '/sample-login', label: '样品登录', desc: '继续补录新样品并生成封签' },
      { path: '/detection-analysis', label: '检测分析', desc: '查看样品是否已进入检测流程' },
      { path: '/review-result', label: '结果审查', desc: '跟踪样品检测后的审核流转' }
    ]
  }
}

const baseScene = computed(() => sceneMap[route.path] || sceneMap['/task-assign'])
const isTaskScene = computed(() => baseScene.value.mode === 'task')
const taskSceneRecords = computed(() => tasks.value.filter((item) => baseScene.value.taskFilter(item)))
const sampleSceneRecords = computed(() => samples.value.filter((item) => baseScene.value.sampleFilter(item)))

const pendingLoggableCount = computed(() =>
  tasks.value.filter((item) =>
    item.taskStatus === completedTaskStatus && !samples.value.some((sample) => sample.taskId === item.id)
  ).length
)

const firstCompletableTask = computed(() =>
  taskSceneRecords.value.find((item) => completableTaskStatuses.includes(item.taskStatus))
)

const firstLoggableTask = computed(() =>
  tasks.value.find((item) =>
    item.taskStatus === completedTaskStatus && !samples.value.some((sample) => sample.taskId === item.id)
  )
)

const currentScene = computed(() => ({
  ...baseScene.value,
  tags: isTaskScene.value
    ? [
        {
          label: '待处理',
          value: taskSceneRecords.value.filter((item) => item.taskStatus === pendingTaskStatus).length,
          type: 'warning'
        },
        {
          label: '进行中',
          value: taskSceneRecords.value.filter((item) => item.taskStatus === inProgressTaskStatus).length,
          type: 'info'
        },
        {
          label: '待样品登录',
          value: pendingLoggableCount.value,
          type: pendingLoggableCount.value ? 'warning' : 'success'
        }
      ]
    : [
        {
          label: '已登记',
          value: sampleSceneRecords.value.filter((item) => item.sampleStatus === loggedSampleStatus).length,
          type: 'info'
        },
        {
          label: '退回重检',
          value: sampleSceneRecords.value.filter((item) => item.sampleStatus === retestSampleStatus).length,
          type: sampleSceneRecords.value.some((item) => item.sampleStatus === retestSampleStatus) ? 'danger' : 'success'
        },
        {
          label: '已闭环',
          value: sampleSceneRecords.value.filter((item) => item.sampleStatus === completedSampleStatus).length,
          type: 'success'
        }
      ]
}))

const currentStats = computed(() => {
  if (isTaskScene.value) {
    return [
      {
        key: 'tasks:all',
        label: baseScene.value.key === 'task-history' ? '历史任务' : '任务总览',
        value: baseScene.value.key === 'task-ledger' ? toSafeNumber(taskTotal.value) : taskSceneRecords.value.length,
        desc: baseScene.value.key === 'task-ledger' ? '采样任务台账总量' : '当前场景下已加载的任务记录'
      },
      {
        key: 'tasks:pending',
        label: '待处理',
        value: taskSceneRecords.value.filter((item) => item.taskStatus === pendingTaskStatus).length,
        desc: '尚未开始执行的采样任务'
      },
      {
        key: 'tasks:progress',
        label: '进行中',
        value: taskSceneRecords.value.filter((item) => item.taskStatus === inProgressTaskStatus).length,
        desc: '正在现场执行的采样任务'
      },
      {
        key: 'tasks:completed',
        label: '已完成',
        value: taskSceneRecords.value.filter((item) => item.taskStatus === completedTaskStatus).length,
        desc: '已经完成采样并可进入样品登录的任务'
      },
      {
        key: 'tasks:abandoned',
        label: '已废弃',
        value: taskSceneRecords.value.filter((item) => item.taskStatus === abandonedTaskStatus).length,
        desc: '因现场条件等原因废弃的任务'
      },
      {
        key: 'tasks:unlogged',
        label: '待样品登录',
        value: pendingLoggableCount.value,
        desc: '已完成采样但尚未生成样品的任务'
      }
    ]
  }

  return [
    {
      key: 'samples:all',
      label: baseScene.value.key === 'sample-ledger' ? '样品总量' : '样品总览',
      value: baseScene.value.key === 'sample-ledger' ? toSafeNumber(sampleTotal.value) : sampleSceneRecords.value.length,
      desc: baseScene.value.key === 'sample-ledger' ? '样品台账总量' : '当前场景下已加载的样品记录'
    },
    {
      key: 'samples:logged',
      label: baseScene.value.key === 'sample-login' ? '已登记样品' : '登记完成',
      value: sampleSceneRecords.value.filter((item) => item.sampleStatus === loggedSampleStatus).length,
      desc: '已经生成封签并等待后续检测的样品'
    },
    {
      key: 'samples:reviewing',
      label: '待审核',
      value: sampleSceneRecords.value.filter((item) => item.sampleStatus === reviewingSampleStatus).length,
      desc: '检测完成后进入审核流程的样品'
    },
    {
      key: 'samples:retest',
      label: '退回重检',
      value: sampleSceneRecords.value.filter((item) => item.sampleStatus === retestSampleStatus).length,
      desc: '被审核退回，等待重新检测处理的样品'
    },
    {
      key: 'samples:completed',
      label: '闭环完成',
      value: sampleSceneRecords.value.filter((item) => item.sampleStatus === completedSampleStatus).length,
      desc: '检测、审核已经完成闭环的样品'
    },
    {
      key: 'samples:todo-login',
      label: '待登录任务',
      value: pendingLoggableCount.value,
      desc: '已完成采样但尚未登记为样品的任务数量'
    }
  ]
})

const visibleTasks = computed(() => {
  const records = taskSceneRecords.value
  if (activeStatKey.value === 'tasks:pending') {
    return records.filter((item) => item.taskStatus === pendingTaskStatus)
  }
  if (activeStatKey.value === 'tasks:progress') {
    return records.filter((item) => item.taskStatus === inProgressTaskStatus)
  }
  if (activeStatKey.value === 'tasks:completed') {
    return records.filter((item) => item.taskStatus === completedTaskStatus)
  }
  if (activeStatKey.value === 'tasks:abandoned') {
    return records.filter((item) => item.taskStatus === abandonedTaskStatus)
  }
  if (activeStatKey.value === 'tasks:unlogged') {
    return records.filter((item) =>
      item.taskStatus === completedTaskStatus && !samples.value.some((sample) => sample.taskId === item.id)
    )
  }
  return records
})

const visibleSamples = computed(() => {
  const records = sampleSceneRecords.value
  if (activeStatKey.value === 'samples:logged') {
    return records.filter((item) => item.sampleStatus === loggedSampleStatus)
  }
  if (activeStatKey.value === 'samples:reviewing') {
    return records.filter((item) => item.sampleStatus === reviewingSampleStatus)
  }
  if (activeStatKey.value === 'samples:retest') {
    return records.filter((item) => item.sampleStatus === retestSampleStatus)
  }
  if (activeStatKey.value === 'samples:completed') {
    return records.filter((item) => item.sampleStatus === completedSampleStatus)
  }
  return records
})

const planStats = computed(() => [
  {
    label: '计划总量',
    value: toSafeNumber(planTotal.value),
    desc: '周期采样计划台账总量'
  },
  {
    label: '启用中',
    value: plans.value.filter((item) => item.planStatus === activePlanStatus).length,
    desc: '当前处于启用状态的采样计划'
  },
  {
    label: '已暂停',
    value: plans.value.filter((item) => item.planStatus === pausedPlanStatus).length,
    desc: '临时暂停执行的采样计划'
  },
  {
    label: '已派发',
    value: plans.value.filter((item) => dispatchedPlanStatuses.includes(item.planStatus)).length,
    desc: '已经生成采样任务的采样计划'
  },
  {
    label: '已完成',
    value: plans.value.filter((item) => item.planStatus === completedPlanStatus).length,
    desc: '已完成闭环的周期计划'
  }
])

function syncRouteState() {
  activeStatKey.value = baseScene.value.defaultStatKey
}

function handleStatClick(key) {
  activeStatKey.value = activeStatKey.value === key ? baseScene.value.defaultStatKey : key
}

function isTaskLogged(taskId) {
  return samples.value.some((item) => item.taskId === taskId)
}

function goRoute(path) {
  if (!path) {
    return
  }
  router.push(path)
}

async function loadPlans() {
  const result = await fetchSamplingPlansApi(planQuery)
  plans.value = result.records || []
  planTotal.value = toSafeNumber(result.total)
}

async function loadTasks() {
  const result = await fetchSamplingTasksApi(taskQuery)
  tasks.value = result.records || []
  taskTotal.value = toSafeNumber(result.total)
}

async function loadSamples() {
  const result = await fetchSamplesApi(sampleQuery)
  samples.value = result.records || []
  sampleTotal.value = toSafeNumber(result.total)
}

async function refreshCurrentScene() {
  const requests = [loadTasks(), loadSamples()]
  if (baseScene.value.showPlanSection) {
    requests.push(loadPlans())
  }
  await Promise.all(requests)
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
    ElMessage.warning('当前没有待登记的已完成采样任务。')
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

onMounted(async () => {
  syncRouteState()
  await Promise.all([loadPlans(), loadTasks(), loadSamples()])
})

watch(() => route.fullPath, () => {
  syncRouteState()
})
</script>

<style scoped>
.sampling-page {
  gap: 16px;
}

.page-hero,
.scene-grid {
  display: grid;
  gap: 16px;
}

.page-hero {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: flex-end;
}

.metric-card--action,
.quick-link {
  width: 100%;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.metric-card--action:hover,
.metric-card--action:focus-visible,
.metric-card--action.is-active,
.quick-link:hover,
.quick-link:focus-visible {
  border-color: color-mix(in srgb, var(--brand) 48%, #ffffff 52%);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
  outline: none;
}

.metric-card--static p,
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

.plan-stats {
  margin-bottom: 12px;
}

.scene-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.scene-copy {
  display: grid;
  gap: 10px;
  color: var(--text-sub);
  font-size: 14px;
  line-height: 1.7;
}

.scene-copy p {
  margin: 0;
}

.quick-links {
  display: grid;
  gap: 12px;
}

.quick-link {
  display: grid;
  gap: 4px;
  padding: 14px;
  border: 1px solid var(--line-soft);
  border-radius: 12px;
  background: var(--bg-panel-soft);
}

.quick-link strong {
  color: var(--text-main);
  font-size: 14px;
  line-height: 1.5;
}

.quick-link span {
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 900px) {
  .page-hero,
  .scene-grid {
    grid-template-columns: 1fr;
  }

  .hero-tags {
    justify-content: flex-start;
  }
}
</style>
