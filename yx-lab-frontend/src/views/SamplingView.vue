<template>
  <div class="content-grid">
    <div class="glass-panel section-block">
      <el-tabs v-model="active">
        <el-tab-pane label="采样计划" name="plans">
          <div class="toolbar">
            <el-button type="primary" @click="loadPlans">刷新</el-button>
            <el-button @click="createPlan">新增计划</el-button>
          </div>
          <el-table :data="plans" stripe>
            <el-table-column prop="planName" label="计划名称" />
            <el-table-column prop="pointName" label="采样点位" />
            <el-table-column prop="samplerName" label="采样人" />
            <el-table-column prop="planStatus" label="状态" />
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button size="small" @click="dispatch(row)">派发</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="采样任务" name="tasks">
          <div class="toolbar">
            <el-button type="primary" @click="loadTasks">刷新</el-button>
            <el-button @click="completeFirstTask" :disabled="!tasks.length">完成首条任务</el-button>
          </div>
          <el-table :data="tasks" stripe>
            <el-table-column prop="pointName" label="点位名称" />
            <el-table-column prop="samplerName" label="采样人" />
            <el-table-column prop="taskStatus" label="状态" />
            <el-table-column prop="samplingTime" label="采样时间" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="样品台账" name="samples">
          <div class="toolbar">
            <el-button type="primary" @click="loadSamples">刷新</el-button>
            <el-button @click="loginSample">样品登录</el-button>
          </div>
          <el-table :data="samples" stripe>
            <el-table-column prop="sampleNo" label="样品编号" />
            <el-table-column prop="pointName" label="点位名称" />
            <el-table-column prop="sampleType" label="样品类型" />
            <el-table-column prop="sampleStatus" label="状态" />
            <el-table-column prop="resultSummary" label="检测结果" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  completeSamplingTaskApi,
  createSamplingPlanApi,
  dispatchSamplingPlanApi,
  fetchSamplesApi,
  fetchSamplingPlansApi,
  fetchSamplingTasksApi,
  loginSampleApi
} from '../api/lab'

const active = ref('plans')
const plans = ref([])
const tasks = ref([])
const samples = ref([])

async function loadPlans() {
  plans.value = (await fetchSamplingPlansApi({ pageNum: 1, pageSize: 10 })).records || []
}

async function loadTasks() {
  tasks.value = (await fetchSamplingTasksApi({ pageNum: 1, pageSize: 10 })).records || []
}

async function loadSamples() {
  samples.value = (await fetchSamplesApi({ pageNum: 1, pageSize: 10 })).records || []
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
  loadPlans()
}

async function dispatch(row) {
  await dispatchSamplingPlanApi({
    planId: row.id,
    samplingTime: row.startTime
  })
  ElMessage.success('计划已派发')
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
  loadTasks()
}

async function loginSample() {
  await loginSampleApi({
    taskId: tasks.value[0]?.id,
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
  loadSamples()
}

onMounted(() => {
  loadPlans()
  loadTasks()
  loadSamples()
})
</script>
