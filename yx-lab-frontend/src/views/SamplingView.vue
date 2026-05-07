<template>
  <div class="content-grid">
    <section class="stats-grid">
      <article class="metric-card" v-for="item in currentStats" :key="item.label">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <p>{{ item.desc }}</p>
      </article>
    </section>

    <section class="glass-panel section-block">
      <el-tabs v-model="active">
        <el-tab-pane label="采样计划" name="plans">
          <div class="section-head">
            <div>
              <h3 class="section-title">采样计划</h3>
              <p class="page-subtitle">维护采样计划信息，并将计划派发到具体采样任务中。</p>
            </div>
          </div>
          <div class="toolbar">
            <el-button type="primary" @click="loadPlans">刷新</el-button>
            <el-button type="primary" plain @click="createPlan">新增计划</el-button>
          </div>
          <div class="table-card">
            <el-table class="list-table" :data="plans" stripe max-height="420" empty-text="暂无采样计划数据">
              <el-table-column prop="planName" label="计划名称" min-width="180" />
              <el-table-column prop="pointName" label="采样点位" min-width="160" />
              <el-table-column prop="samplerName" label="采样人" width="120" />
              <el-table-column prop="startTime" label="开始时间" width="170" />
              <el-table-column prop="endTime" label="结束时间" width="170" />
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <span class="status-chip" :class="getStatusClass('planStatus', row.planStatus)">
                    {{ getEnumLabel(planStatusLabelMap, row.planStatus) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120">
                <template #default="{ row }">
                  <el-button size="small" @click="dispatch(row)">派发</el-button>
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
              <p class="page-subtitle">查看计划派发后的采样任务执行情况，并可快速完成演示任务。</p>
            </div>
          </div>
          <div class="toolbar">
            <el-button type="primary" @click="loadTasks">刷新</el-button>
            <el-button @click="completeFirstTask" :disabled="!tasks.length">完成首条任务</el-button>
          </div>
          <div class="table-card">
            <el-table class="list-table" :data="tasks" stripe max-height="420" empty-text="暂无采样任务数据">
              <el-table-column prop="pointName" label="点位名称" min-width="180" />
              <el-table-column prop="samplerName" label="采样人" width="120" />
              <el-table-column label="样品类型" width="120">
                <template #default="{ row }">
                  {{ getEnumLabel(sampleTypeLabelMap, row.sampleType) }}
                </template>
              </el-table-column>
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <span class="status-chip" :class="getStatusClass('taskStatus', row.taskStatus)">
                    {{ getEnumLabel(taskStatusLabelMap, row.taskStatus) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="samplingTime" label="采样时间" width="170" />
              <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
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
              <p class="page-subtitle">展示样品登录结果及当前样品状态，便于后续检测流转。</p>
            </div>
          </div>
          <div class="toolbar">
            <el-button type="primary" @click="loadSamples">刷新</el-button>
            <el-button @click="loginSample">样品登录</el-button>
          </div>
          <div class="table-card">
            <el-table class="list-table" :data="samples" stripe max-height="420" empty-text="暂无样品台账数据">
              <el-table-column prop="sampleNo" label="样品编号" min-width="180" />
              <el-table-column prop="pointName" label="点位名称" min-width="160" />
              <el-table-column label="样品类型" width="120">
                <template #default="{ row }">
                  {{ getEnumLabel(sampleTypeLabelMap, row.sampleType) }}
                </template>
              </el-table-column>
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <span class="status-chip" :class="getStatusClass('sampleStatus', row.sampleStatus)">
                    {{ getEnumLabel(sampleStatusLabelMap, row.sampleStatus) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="samplingTime" label="采样时间" width="170" />
              <el-table-column prop="resultSummary" label="检测结果" min-width="180" show-overflow-tooltip />
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
  completeSamplingTaskApi,
  createSamplingPlanApi,
  dispatchSamplingPlanApi,
  fetchSamplesApi,
  fetchSamplingPlansApi,
  fetchSamplingTasksApi,
  loginSampleApi
} from '../api/lab'
import {
  getEnumLabel,
  getStatusClass,
  planStatusLabelMap,
  sampleStatusLabelMap,
  sampleTypeLabelMap,
  taskStatusLabelMap
} from '../utils/labEnums'

const active = ref('plans')
const planQuery = reactive({ pageNum: 1, pageSize: 30 })
const taskQuery = reactive({ pageNum: 1, pageSize: 30 })
const sampleQuery = reactive({ pageNum: 1, pageSize: 30 })

const plans = ref([])
const tasks = ref([])
const samples = ref([])
const planTotal = ref(0)
const taskTotal = ref(0)
const sampleTotal = ref(0)

const currentStats = computed(() => {
  if (active.value === 'plans') {
    return [
      { label: '计划总数', value: planTotal.value, desc: '采样计划记录总量' },
      { label: '本页记录', value: plans.value.length, desc: '当前分页已加载的采样计划' },
      { label: '已派发', value: plans.value.filter((item) => ['DISPATCHED', 'PUBLISHED'].includes(item.planStatus)).length, desc: '当前页已派发的采样计划' },
      { label: '待执行', value: plans.value.filter((item) => ['PENDING', 'UNPUBLISHED'].includes(item.planStatus)).length, desc: '当前页待执行的采样计划' }
    ]
  }
  if (active.value === 'tasks') {
    return [
      { label: '任务总数', value: taskTotal.value, desc: '采样任务记录总量' },
      { label: '本页记录', value: tasks.value.length, desc: '当前分页已加载的采样任务' },
      { label: '待处理', value: tasks.value.filter((item) => item.taskStatus === 'PENDING').length, desc: '当前页待处理任务' },
      { label: '已完成', value: tasks.value.filter((item) => item.taskStatus === 'COMPLETED').length, desc: '当前页已完成任务' }
    ]
  }
  return [
    { label: '样品总数', value: sampleTotal.value, desc: '样品台账记录总量' },
    { label: '本页记录', value: samples.value.length, desc: '当前分页已加载的样品记录' },
    { label: '已登记', value: samples.value.filter((item) => ['LOGGED', 'SUBMITTED'].includes(item.sampleStatus)).length, desc: '当前页已登记样品' },
    { label: '流转中', value: samples.value.filter((item) => item.sampleStatus === 'IN_TEST').length, desc: '当前页检测中的样品' }
  ]
})

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
    planName: `常规采样-${dayjs().format('MMDD-HHmm')}`,
    pointId: 2001,
    pointName: '城东水厂出厂水',
    startTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    endTime: dayjs().add(2, 'hour').format('YYYY-MM-DD HH:mm:ss'),
    samplerId: 1002,
    samplerName: '采样员',
    samplingType: 'ROUTINE',
    sampleType: 'FACTORY'
  })
  ElMessage.success('采样计划已创建')
  planQuery.pageNum = 1
  loadPlans()
}

async function dispatch(row) {
  await dispatchSamplingPlanApi({
    planId: row.id,
    samplingTime: row.startTime
  })
  ElMessage.success('计划已派发')
  planQuery.pageNum = 1
  taskQuery.pageNum = 1
  loadPlans()
  loadTasks()
}

async function completeFirstTask() {
  await completeSamplingTaskApi({
    taskId: tasks.value[0].id,
    onsiteMetrics: 'pH=7.2;余氯=0.4;浊度=0.5',
    remark: '现场采样完成'
  })
  ElMessage.success('任务已完成')
  taskQuery.pageNum = 1
  loadTasks()
}

async function loginSample() {
  if (!tasks.value.length) {
    ElMessage.warning('请先完成采样任务派发')
    return
  }
  await loginSampleApi({
    taskId: tasks.value[0].id,
    pointId: 2001,
    pointName: '城东水厂出厂水',
    sampleType: 'FACTORY',
    detectionItems: 'pH,浊度,余氯,氨氮',
    samplingTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    samplerId: 1002,
    samplerName: '采样员',
    weather: '晴',
    storageCondition: '冷藏'
  })
  ElMessage.success('样品登录完成')
  sampleQuery.pageNum = 1
  loadSamples()
}

onMounted(() => {
  loadPlans()
  loadTasks()
  loadSamples()
})
</script>

<style scoped>
.section-head {
  margin-bottom: 16px;
}

.section-title {
  margin: 0;
  font-size: 18px;
  line-height: 1.4;
}

.metric-card p {
  margin: 10px 0 0;
  color: var(--text-sub);
  font-size: 14px;
  line-height: 1.6;
}
</style>
