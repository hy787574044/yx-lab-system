﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿<template>
  <div :class="['content-grid', 'sampling-page', 'fixed-table-page', { 'sampling-page--plan': isPlanScene }]">
    <section v-if="!isPlanScene" class="glass-panel section-block fixed-table-section">
      <div class="section-head">
        <div>
          <h3 class="section-title">{{ currentScene.tableTitle }}</h3>
        </div>
      </div>

      <section class="stats-grid section-stats">
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

      <div class="toolbar-panel">
        <div class="toolbar-row">
          <div class="toolbar-main">
              <el-button
                v-permission="'sample:write'"
                v-if="baseScene.key === 'sample-login'"
                type="primary"
              class="toolbar-primary-button"
              @click="openLoginDialog()"
            >
              样品登录
            </el-button>
            <div class="toolbar-fields">
              <template v-if="isTaskScene">
                <label class="toolbar-field toolbar-field--medium">
                  <span>关键字</span>
                  <el-input
                    v-model="taskQuery.keyword"
                    clearable
                    placeholder="请输入任务编号、点位名称或样品编号"
                    @keyup.enter="handleCurrentSceneSearch"
                  />
                </label>
                <label class="toolbar-field">
                  <span>任务状态</span>
                  <el-select v-model="taskQuery.taskStatus" clearable placeholder="请选择任务状态">
                    <el-option
                      v-for="option in taskStatusOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                </label>
                <label class="toolbar-field">
                  <span>采样人员</span>
                  <el-select
                    v-model="taskQuery.samplerId"
                    clearable
                    filterable
                    placeholder="请选择采样人员"
                    :loading="samplerLoading"
                    @visible-change="handleSamplerDropdownVisible"
                  >
                    <el-option
                      v-for="item in samplerOptions"
                      :key="item.id"
                      :label="item.realName || item.username"
                      :value="item.id"
                    />
                  </el-select>
                </label>
              </template>
              <template v-else>
                <label class="toolbar-field toolbar-field--medium">
                  <span>关键字</span>
                  <el-input
                    v-model="sampleQuery.keyword"
                    clearable
                    placeholder="请输入样品编号或点位名称"
                    @keyup.enter="handleCurrentSceneSearch"
                  />
                </label>
                <label class="toolbar-field">
                  <span>样品状态</span>
                  <el-select v-model="sampleQuery.sampleStatus" clearable placeholder="请选择样品状态">
                    <el-option
                      v-for="option in sampleStatusOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                </label>
                <label class="toolbar-field">
                  <span>样品类型</span>
                  <el-select v-model="sampleQuery.sampleType" clearable placeholder="请选择样品类型">
                    <el-option
                      v-for="option in sampleTypeOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                </label>
              </template>
            </div>
          </div>
          <div class="toolbar-actions">
            <el-button type="primary" @click="handleCurrentSceneSearch">查询</el-button>
            <el-button @click="resetCurrentSceneQuery">重置</el-button>
            <el-button @click="handleExportCurrentScene">导出</el-button>
          </div>
        </div>
      </div>

      <div class="table-card table-card--fixed-scroll">
        <div class="table-card__body">
        <el-table
          v-if="isTaskScene"
          class="list-table"
          :data="visibleTasks"
          stripe
          height="100%"
          :empty-text="baseScene.emptyText"
        >
          <el-table-column prop="taskNo" label="任务编号" min-width="150" />
          <el-table-column label="样品编号" min-width="170">
            <template #default="{ row }">
              <span class="plan-sampler" :class="{ 'is-empty': !row.sampleNo }">
                {{ row.sampleNo || '任务生成时自动生成' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="pointName" label="点位名称" min-width="180" />
          <el-table-column label="采样人员" width="140">
            <template #default="{ row }">
              <span
                class="plan-sampler"
                :class="{ 'is-empty': !row.samplerId || !row.samplerName }"
              >
                {{ row.samplerName || '未指定采样员' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="样品类型" width="120" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">
              {{ getEnumLabel(sampleTypeLabelMap, row.sampleType) }}
            </template>
          </el-table-column>
          <el-table-column prop="detectionTypeName" label="检测套餐" min-width="160" show-overflow-tooltip />
          <el-table-column label="检测参数" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              {{ formatTaskDetectionParameterSummary(row) }}
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
                {{ isTaskLogged(row.id) ? '已登记' : '未登记' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="samplingTime" label="计划采样时间" width="170" />
          <el-table-column prop="startedTime" label="开始时间" width="170" />
          <el-table-column prop="finishedTime" label="完成时间" width="170" />
          <el-table-column prop="weather" label="天气" width="110" />
          <el-table-column prop="temperature" label="温度" width="110" />
          <el-table-column prop="abandonReason" label="废弃原因" min-width="160" show-overflow-tooltip />
          <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
          <el-table-column
            label="操作"
            :width="baseScene.allowTaskActions ? 370 : 140"
            fixed="right"
            header-cell-class-name="cell-center"
            class-name="cell-center task-action-cell"
          >
            <template #default="{ row }">
              <div class="action-row task-action-row">
                <el-button
                  v-if="baseScene.allowTaskActions"
                  v-permission="'samplingTask:write'"
                  type="primary"
                  size="small"
                  :loading="isRowActionLoading('task', 'complete', row.id)"
                  :disabled="!isTaskCompleteEntryEnabled(row)"
                  @click="openTaskCompleteDialog(row)"
                >
                  采样录入
                </el-button>
                <el-button
                  v-if="baseScene.allowTaskActions"
                  v-permission="'samplingTask:write'"
                  size="small"
                  :loading="isRowActionLoading('task', 'abandon', row.id)"
                  @click="abandonTask(row)"
                  :disabled="isRowActionLoading('task', 'resume', row.id) || !abandonableTaskStatuses.includes(row.taskStatus)"
                >
                  废弃
                </el-button>
                <el-button
                  v-if="baseScene.allowTaskActions"
                  v-permission="'samplingTask:write'"
                  size="small"
                  :loading="isRowActionLoading('task', 'resume', row.id)"
                  @click="resumeTask(row)"
                  :disabled="isRowActionLoading('task', 'abandon', row.id) || row.taskStatus !== abandonedTaskStatus"
                >
                  恢复
                </el-button>
                <el-button
                  size="small"
                  @click="openTaskDetailDialog(row)"
                >
                  查看详情
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
          height="100%"
          :empty-text="baseScene.emptyText"
        >
          <el-table-column prop="sampleNo" label="样品编号" min-width="180" />
          <el-table-column prop="pointName" label="点位名称" min-width="160" />
          <el-table-column label="样品类型" width="120" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">
              {{ getEnumLabel(sampleTypeLabelMap, row.sampleType) }}
            </template>
          </el-table-column>
          <el-table-column label="质控类型" width="120" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">
              {{ getEnumLabel(qualityControlTypeLabelMap, row.qualityControlType) || '-' }}
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
          <el-table-column label="结果摘要" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              {{ translateWorkflowText(row.resultSummary) || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="流程留痕" min-width="260" show-overflow-tooltip>
            <template #default="{ row }">
              {{ translateWorkflowText(row.traceLog) || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right" class-name="cell-center" header-cell-class-name="cell-center">
            <template #default="{ row }">
              <div class="table-action-row">
                <el-button link type="primary" @click="openSampleDetailDialog(row)">查看登记明细</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        </div>

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

    <section
      v-if="baseScene.showPlanSection || isPlanScene"
      :class="['glass-panel', 'section-block', { 'sampling-plan-section': isPlanScene }]"
    >
      <div class="section-head">
        <div>
          <h3 class="section-title">周期采样计划</h3>
        </div>
      </div>

      <section class="stats-grid plan-stats">
        <button
          v-for="item in planStats"
          :key="item.key"
          type="button"
          :class="['metric-card', 'metric-card--action', { 'is-active': activeStatKey === item.key }]"
          @click="handlePlanStatClick(item.key)"
        >
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <p>{{ item.desc }}</p>
        </button>
      </section>

      <div class="toolbar-panel">
        <div class="toolbar-row">
          <div class="toolbar-main">
            <el-button v-permission="'samplingPlan:write'" type="primary" class="toolbar-primary-button" @click="createPlan">新增计划</el-button>
            <div class="toolbar-fields">
              <label class="toolbar-field toolbar-field--medium">
                <span>关键字</span>
                <el-input
                  v-model="planQuery.keyword"
                  clearable
                  placeholder="请输入计划名称或点位名称"
                  @keyup.enter="handlePlanSearch"
                />
              </label>
              <label class="toolbar-field">
                <span>计划状态</span>
                <el-select v-model="planQuery.planStatus" clearable placeholder="请选择计划状态">
                  <el-option
                    v-for="option in planStatusOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
              </label>
              <label class="toolbar-field">
                <span>采样人员</span>
                <el-select
                  v-model="planQuery.samplerId"
                  clearable
                  filterable
                  placeholder="请选择采样人员"
                  :loading="samplerLoading"
                  @visible-change="handleSamplerDropdownVisible"
                >
                  <el-option
                    v-for="item in samplerOptions"
                    :key="item.id"
                    :label="item.realName || item.username"
                    :value="item.id"
                  />
                </el-select>
              </label>
            </div>
          </div>
          <div class="toolbar-actions">
            <el-button type="primary" @click="handlePlanSearch">查询</el-button>
            <el-button @click="resetPlanQuery">重置</el-button>
            <el-button @click="handleExportPlans">导出</el-button>
          </div>
        </div>
      </div>

      <div v-if="activeMissingSamplerPlans.length" class="panel-note plan-panel-warning">
        {{ planDispatchNotice }}
      </div>

      <div :class="['table-card', { 'table-card--fixed-scroll': isPlanScene }, 'plan-table-card', { 'plan-table-card--fullscreen': isPlanScene }]">
        <div class="plan-table-card__body">
          <el-table class="list-table" :data="visiblePlans" stripe :height="isPlanScene ? '100%' : undefined" empty-text="暂无采样计划数据">
            <el-table-column prop="planName" label="计划名称" min-width="180" />
            <el-table-column prop="pointName" label="采样点位" min-width="160" />
            <el-table-column prop="samplerName" label="采样人员" width="120" />
            <el-table-column prop="detectionTypeName" label="检测套餐" min-width="160" show-overflow-tooltip />
            <el-table-column prop="samplingBasis" label="采样依据" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">
                {{ formatSamplingBasisText(row.samplingBasis) || '-' }}
              </template>
            </el-table-column>
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
            <el-table-column label="操作" min-width="300" fixed="right" header-cell-class-name="cell-center" class-name="cell-center">
              <template #default="{ row }">
                <div class="action-row">
                  <el-button
                    v-permission="'samplingPlan:write'"
                    size="small"
                    @click="openPlanEditDialog(row)"
                    :disabled="isRowActionLoading('plan', 'pause', row.id) || isRowActionLoading('plan', 'resume', row.id) || !actionablePlanStatuses.includes(row.planStatus)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    v-permission="'samplingPlan:write'"
                    size="small"
                    @click="openDispatchDialog(row)"
                    :disabled="dispatchSubmitting || !actionablePlanStatuses.includes(row.planStatus)"
                  >
                    派发
                  </el-button>
                  <el-button
                    v-permission="'samplingPlan:write'"
                    size="small"
                    :loading="isRowActionLoading('plan', 'pause', row.id)"
                    @click="pausePlan(row)"
                    :disabled="isRowActionLoading('plan', 'resume', row.id) || !actionablePlanStatuses.includes(row.planStatus)"
                  >
                    暂停
                  </el-button>
                  <el-button
                    v-permission="'samplingPlan:write'"
                    size="small"
                    :loading="isRowActionLoading('plan', 'resume', row.id)"
                    @click="resumePlan(row)"
                    :disabled="isRowActionLoading('plan', 'pause', row.id) || row.planStatus !== pausedPlanStatus"
                  >
                    恢复
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <TablePagination
          v-model:current-page="planQuery.pageNum"
          v-model:page-size="planQuery.pageSize"
          :total="planTotal"
          @change="loadPlans"
        />
      </div>
    </section>

    <el-dialog
      v-model="planDialogVisible"
      class="sampling-form-dialog sampling-plan-form-dialog"
      :title="editingPlanId ? '编辑采样计划' : '新增采样计划'"
      width="1080px"
      align-center
      destroy-on-close
      @closed="resetPlanForm"
    >
      <el-form label-width="96px">
        <div class="plan-form-grid">
          <el-form-item label="计划名称" required>
            <el-input v-model="planForm.planName" placeholder="请输入采样计划名称" />
          </el-form-item>
          <el-form-item label="点位来源">
            <el-select v-model="planForm.pointSource" style="width: 100%" @change="handlePlanPointSourceChange">
              <el-option label="监测点位选择" value="EXISTING" />
              <el-option label="手工填写点位" value="CUSTOM" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="planForm.pointSource === 'EXISTING'" label="监测点位" required>
            <el-select
              v-model="planForm.pointId"
              style="width: 100%"
              placeholder="请选择已创建的监测点位"
              :loading="monitoringPointLoading"
              @change="handlePlanPointChange"
            >
              <el-option
                v-for="point in monitoringPointOptions"
                :key="point.id"
                :label="point.pointName"
                :value="point.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="false" label="自填点位">
            <el-input v-model="planForm.pointName" placeholder="请输入采样点位名称" />
          </el-form-item>
          <el-form-item label="点位名称" required>
            <el-input
              v-model="planForm.pointName"
              :readonly="planForm.pointSource === 'EXISTING'"
              placeholder="请输入采样点位名称"
            />
          </el-form-item>
          <el-form-item label="样品类型" required>
            <el-select v-model="planForm.sampleType" style="width: 100%">
              <el-option
                v-for="option in sampleTypeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="采样人员" required>
            <el-select
              v-model="planForm.samplerId"
              filterable
              style="width: 100%"
              placeholder="请选择采样员"
              :loading="samplerLoading"
              @visible-change="handleSamplerDropdownVisible"
              @change="handlePlanSamplerChange"
            >
              <el-option
                v-for="item in samplerOptions"
                :key="item.id"
                :label="item.realName || item.username"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item class="plan-form-new-row" label="检测套餐" required>
            <el-select
              v-model="planForm.detectionTypeId"
              filterable
              style="width: 100%"
              placeholder="请选择检测套餐"
              @change="handlePlanDetectionTypeChange"
            >
              <el-option
                v-for="item in detectionProjectOptions"
                :key="item.id"
                :label="item.typeName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="周期类型" required>
            <el-select v-model="planForm.cycleType" style="width: 100%">
              <el-option
                v-for="option in cycleTypeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="采样依据" required>
            <el-select
              v-model="planForm.samplingBasisList"
              multiple
              filterable
              clearable
              collapse-tags
              collapse-tags-tooltip
              style="width: 100%"
              placeholder="请选择采样依据，可多选"
            >
              <el-option
                v-for="option in samplingBasisOptions"
                :key="option"
                :label="option"
                :value="option"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="planForm.detectionTypeId" class="plan-form-span-2" label="套餐参数" required>
            <div class="login-config-panel">
              <div class="login-config-panel__summary">
                <span class="binding-editor__chip">
                  已选参数<strong>{{ planDetectionConfigRows.length }}</strong>
                </span>
                <span class="login-config-panel__note">
                  检测套餐参数在采样计划中确认，派发任务和样品登录都会沿用这份明细。
                </span>
                <el-button v-permission="'samplingPlan:write'" type="primary" plain size="small" @click="appendPlanConfigRow">新增参数</el-button>
              </div>
              <el-table
                class="login-config-table"
                :data="planDetectionConfigRows"
                size="small"
                max-height="360"
                border
              >
                <el-table-column label="检测参数名称" min-width="140">
                  <template #default="{ row, $index }">
                    <el-select
                      v-model="row.parameterId"
                      placeholder="请选择检测参数"
                      style="width: 100%"
                      @change="(value) => handlePlanConfigParameterChange(row, value)"
                    >
                      <el-option
                        v-for="option in getPlanConfigParameterOptions($index)"
                        :key="option.id"
                        :label="option.parameterName"
                        :value="option.id"
                      />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column label="标准范围" min-width="120">
                  <template #default="{ row }">
                    {{ formatStandardRange(row.standardMin, row.standardMax) }}
                  </template>
                </el-table-column>
                <el-table-column prop="unit" label="单位" width="72">
                  <template #default="{ row }">{{ row.unit || '-' }}</template>
                </el-table-column>
                <el-table-column prop="referenceStandard" label="检测标准" min-width="150" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.referenceStandard || '-' }}</template>
                </el-table-column>
                <el-table-column label="检测方法" min-width="170">
                  <template #default="{ row }">
                    <el-select
                      v-model="row.methodId"
                      placeholder="请选择检测方法"
                      style="width: 100%"
                      :disabled="!row.parameterId || !row.methodOptions.length"
                      @change="(value) => handlePlanConfigMethodChange(row, value)"
                    >
                      <el-option
                        v-for="method in row.methodOptions"
                        :key="method.id"
                        :label="method.methodName"
                        :value="method.id"
                      />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column prop="sampleVolume" label="取样体积" min-width="90" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.sampleVolume || '-' }}</template>
                </el-table-column>
                <el-table-column label="操作" width="90" class-name="cell-center" header-cell-class-name="cell-center">
                  <template #default="{ $index }">
                    <div class="table-action-row">
                      <el-button v-permission="'samplingPlan:write'" link type="danger" @click="removePlanConfigRow($index)">删除</el-button>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
              <div v-if="!planDetectionConfigRows.length" class="empty-block">
                当前套餐参数已清空，可点击“新增参数”补充本计划需要检测的参数与方法。
              </div>
            </div>
          </el-form-item>
          <el-form-item label="开始时间" required>
            <el-date-picker
              v-model="planForm.startTime"
              type="datetime"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="截止时间">
            <el-date-picker
              v-model="planForm.endTime"
              type="datetime"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
              placeholder="单次计划可不填，周期计划建议填写"
            />
          </el-form-item>
          <el-form-item class="plan-form-span-2" label="备注">
            <el-input v-model="planForm.remark" type="textarea" :rows="3" placeholder="可补充客户要求、执行说明等信息" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="planDialogVisible = false">取消</el-button>
        <el-button v-permission="'samplingPlan:write'" type="primary" :loading="submitting" @click="submitPlanForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="taskCompleteDialogVisible"
      class="sampling-form-dialog"
      title="采样录入"
      width="960px"
      align-center
      destroy-on-close
      @closed="resetTaskCompleteDialog"
    >
      <el-form label-width="96px">
        <div class="plan-form-grid">
          <el-form-item label="任务编号">
            <el-input :model-value="taskCompletePreview?.taskNo || '-'" readonly />
          </el-form-item>
          <el-form-item label="样品编号">
            <el-input :model-value="taskCompletePreview?.sampleNo || '-'" readonly />
          </el-form-item>
          <el-form-item label="点位名称">
            <el-input :model-value="taskCompletePreview?.pointName || '-'" readonly />
          </el-form-item>
          <el-form-item label="采样人员">
            <el-input :model-value="taskCompletePreview?.samplerName || '-'" readonly />
          </el-form-item>
          <el-form-item label="样品类型">
            <el-input :model-value="getEnumLabel(sampleTypeLabelMap, taskCompletePreview?.sampleType) || '-'" readonly />
          </el-form-item>
          <el-form-item label="检测套餐">
            <el-input
              :model-value="taskCompletePreview?.detectionTypeName || taskCompletePreview?.detectionItems || '-'"
              readonly
            />
          </el-form-item>
          <el-form-item label="采样依据">
            <el-input
              :model-value="formatSamplingBasisText(taskCompletePreview?.samplingBasis || taskCompletePreview?.sampling_basis) || '-'"
              readonly
            />
          </el-form-item>
          <el-form-item class="plan-form-span-2" label="检测参数">
            <div class="login-config-panel">
              <div class="login-config-panel__summary">
                <span class="binding-editor__chip">
                  参数数量<strong>{{ taskCompleteDetectionConfigRows.length }}</strong>
                </span>
                <span class="login-config-panel__note">
                  原位检测、现场测定参数可在采样录入阶段直接填写检测值，实验室测定参数后续由检测人员录入。
                </span>
              </div>
              <el-table
                v-if="taskCompleteDetectionConfigRows.length"
                class="login-config-table task-detail-config-table"
                :data="taskCompleteDetectionConfigRows"
                size="small"
                max-height="320"
                border
              >
                <el-table-column prop="parameterName" label="检测参数" min-width="130" show-overflow-tooltip />
                <el-table-column label="参数类别" width="100" header-cell-class-name="cell-center" class-name="cell-center">
                  <template #default="{ row }">{{ formatParameterCategory(row) }}</template>
                </el-table-column>
                <el-table-column label="标准范围" min-width="120">
                  <template #default="{ row }">
                    {{ formatStandardRange(row.standardMin, row.standardMax) }}
                  </template>
                </el-table-column>
                <el-table-column prop="unit" label="单位" width="72">
                  <template #default="{ row }">{{ row.unit || '-' }}</template>
                </el-table-column>
                <el-table-column label="检测值" width="150">
                  <template #default="{ row }">
                    <el-input-number
                      v-if="isSamplingDetectionResultEditable(row)"
                      v-model="row.resultValue"
                      :precision="2"
                      :step="0.1"
                      controls-position="right"
                      style="width: 128px"
                    />
                    <span v-else>{{ row.resultValue ?? '-' }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="referenceStandard" label="检测标准" min-width="150" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.referenceStandard || '-' }}</template>
                </el-table-column>
                <el-table-column prop="methodName" label="检测方法" min-width="160" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.methodName || '-' }}</template>
                </el-table-column>
                <el-table-column prop="instrumentDisplayNames" label="采样设备" min-width="220" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.instrumentDisplayNames || '-' }}</template>
                </el-table-column>
                <el-table-column prop="sampleVolume" label="取样体积" min-width="90" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.sampleVolume || '-' }}</template>
                </el-table-column>
              </el-table>
              <div v-else class="empty-block">暂无检测参数明细</div>
            </div>
          </el-form-item>
          <el-form-item label="天气">
            <el-select
              v-model="taskCompleteForm.weather"
              allow-create
              clearable
              default-first-option
              filterable
              placeholder="请选择或输入天气"
              style="width: 100%"
            >
              <el-option
                v-for="option in weatherOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="温度">
            <el-input v-model="taskCompleteForm.temperature" placeholder="例如：26℃" />
          </el-form-item>
          <el-form-item label="应采总容量">
            <el-input :model-value="expectedTaskCompleteSampleTotalVolume" readonly>
              <template #append>mL</template>
            </el-input>
          </el-form-item>
          <el-form-item label="采样总容量">
            <el-input
              :model-value="taskCompleteForm.sampleTotalVolume"
              placeholder="请输入采样总容量"
              @input="handleTaskCompleteSampleTotalVolumeInput"
            >
              <template #append>mL</template>
            </el-input>
          </el-form-item>
          <el-form-item label="采样瓶数">
            <el-input-number v-model="taskCompleteForm.sampleBottleCount" :min="0" :precision="0" style="width: 100%" />
          </el-form-item>
          <el-form-item class="plan-form-span-2" label="现场照片">
            <div class="sampling-photo-uploader">
              <div v-if="taskCompletePhotoList.length" class="sampling-photo-list">
                <div
                  v-for="(photo, index) in taskCompletePhotoList"
                  :key="`${photo.path}-${index}`"
                  class="sampling-photo-card"
                >
                  <el-image
                    v-if="photo.previewUrl"
                    class="sampling-photo-card__image"
                    :src="photo.previewUrl"
                    :preview-src-list="taskCompletePreviewPhotoUrls"
                    :initial-index="index"
                    fit="cover"
                    preview-teleported
                  />
                  <div v-else class="sampling-photo-card__placeholder">预览失败</div>
                  <el-button text type="danger" size="small" @click="removeTaskCompletePhoto(index)">移除</el-button>
                </div>
              </div>
              <el-upload
                :auto-upload="false"
                :show-file-list="false"
                accept="image/png,image/jpeg,image/jpg,image/webp"
                :on-change="handleTaskCompletePhotoChange"
              >
                <el-button :loading="taskCompletePhotoUploading">上传照片</el-button>
              </el-upload>
              <p class="sampling-photo-uploader__tip">支持 JPG、PNG、WEBP，多张照片会随采样完成信息一起保存。</p>
            </div>
          </el-form-item>
          <el-form-item class="plan-form-span-2" label="现场指标">
            <el-input
              v-model="taskCompleteForm.onsiteMetrics"
              type="textarea"
              :rows="3"
              placeholder="请输入现场检测指标、仪器读数或现场情况说明"
            />
          </el-form-item>
          <el-form-item class="plan-form-span-2" label="备注">
            <el-input v-model="taskCompleteForm.remark" type="textarea" :rows="3" placeholder="可补充采样过程、异常情况等说明" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button :disabled="taskCompleteSubmitting" @click="taskCompleteDialogVisible = false">取消</el-button>
        <el-button
          v-permission="'samplingTask:write'"
          type="primary"
          :loading="taskCompleteSubmitting"
          @click="submitTaskCompleteForm"
        >
          保存
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="dispatchDialogVisible"
      class="dispatch-plan-dialog"
      title="派发采样计划"
      width="560px"
      align-center
      destroy-on-close
      @closed="resetDispatchForm"
    >
      <el-form label-width="96px">
        <el-form-item label="采样人员" required>
          <el-select
            v-model="dispatchForm.samplerId"
            filterable
            style="width: 100%"
            placeholder="请选择采样员"
            :loading="samplerLoading"
            @visible-change="handleSamplerDropdownVisible"
            @change="handleDispatchSamplerChange"
          >
            <el-option
              v-for="item in samplerOptions"
              :key="item.id"
              :label="item.realName || item.username"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="执行时间">
          <el-date-picker
            v-model="dispatchForm.samplingTime"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="dispatchSubmitting" @click="dispatchDialogVisible = false">取消</el-button>
        <el-button v-permission="'samplingPlan:write'" type="primary" :loading="dispatchSubmitting" :disabled="dispatchSubmitting" @click="submitDispatchForm">确认派发</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="loginDialogVisible"
      class="sample-login-dialog sampling-form-dialog"
      :title="loginDialogTitle"
      width="1180px"
      align-center
      destroy-on-close
      @closed="resetLoginForm"
    >
      <el-form label-width="96px">
        <div class="login-form-grid">
          <el-form-item :label="isLoginReadonly ? '关联任务' : '待登录任务'" :required="!isLoginReadonly">
            <el-input
              v-if="isLoginReadonly"
              :model-value="loginPreviewTaskLabel"
              readonly
              placeholder="该样品为无任务直登"
            />
            <el-select
              v-else
              v-model="loginForm.taskId"
              placeholder="请选择已完成采样且未登录的任务"
              style="width: 100%"
              @change="handleLoginTaskChange"
            >
              <el-option
                v-for="task in pendingLoggableTasks"
                :key="task.id"
                :label="formatPendingTaskLabel(task)"
                :value="task.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="样品编号">
            <el-input
              :model-value="loginForm.sampleNo || '-'"
              readonly
              placeholder="任务生成时由服务器自动生成"
            />
          </el-form-item>
          <el-form-item label="点位名称" :required="!isLoginReadonly">
            <el-input
              :model-value="loginForm.pointName || '-'"
              readonly
              placeholder="选择待登录任务后自动带出"
            />
          </el-form-item>
          <el-form-item label="样品类型" :required="!isLoginReadonly">
            <el-input
              :model-value="getEnumLabel(sampleTypeLabelMap, loginForm.sampleType) || loginForm.sampleType || '-'"
              readonly
            />
          </el-form-item>
          <el-form-item label="质控类型">
            <el-input
              v-if="isLoginReadonly"
              :model-value="getEnumLabel(qualityControlTypeLabelMap, loginForm.qualityControlType) || '-'"
              readonly
            />
            <el-select
              v-else
              v-model="loginForm.qualityControlType"
              clearable
              placeholder="请选择质控类型"
              style="width: 100%"
            >
              <el-option
                v-for="option in qualityControlTypeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="采样人员" :required="!isLoginReadonly">
            <el-input v-model="loginForm.samplerName" readonly />
          </el-form-item>
          <el-form-item class="login-form-span-2 login-form-half-row" label="检测套餐" :required="!isLoginReadonly">
            <el-input
              :model-value="loginForm.detectionTypeName || loginForm.detectionItems || '-'"
              readonly
            />
          </el-form-item>
          <el-form-item v-if="loginForm.detectionTypeId" class="login-form-span-2" label="检测参数明细">
            <div class="login-config-panel">
              <div class="login-config-panel__summary">
                <span class="binding-editor__chip">
                  参数数量<strong>{{ loginDetectionConfigRows.length }}</strong>
                </span>
                <span class="login-config-panel__note">
                  {{ loginConfigPanelNote }}
                </span>
              </div>
              <el-table
                class="login-config-table"
                :data="loginDetectionConfigRows"
                size="small"
                max-height="460"
                border
              >
                <el-table-column label="检测参数名称" min-width="130">
                  <template #default="{ row }">
                    <span>{{ row.parameterName || '-' }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="参数类别" width="100" header-cell-class-name="cell-center" class-name="cell-center">
                  <template #default="{ row }">{{ formatParameterCategory(row) }}</template>
                </el-table-column>
                <el-table-column label="标准范围" min-width="120">
                  <template #default="{ row }">
                    {{ formatStandardRange(row.standardMin, row.standardMax) }}
                  </template>
                </el-table-column>
                <el-table-column prop="unit" label="单位" width="72">
                  <template #default="{ row }">{{ row.unit || '-' }}</template>
                </el-table-column>
                <el-table-column label="检测值" width="110">
                  <template #default="{ row }">{{ row.resultValue ?? '-' }}</template>
                </el-table-column>
                <el-table-column prop="referenceStandard" label="检测标准" min-width="140" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.referenceStandard || '-' }}</template>
                </el-table-column>
                <el-table-column label="检测方法" min-width="160">
                  <template #default="{ row }">
                    <span>{{ row.methodName || '-' }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="sampleVolume" label="取样体积" min-width="90" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.sampleVolume || '-' }}</template>
                </el-table-column>
              </el-table>
              <div v-if="!loginDetectionConfigRows.length" class="empty-block">
                当前任务未携带套餐参数明细，请检查采样计划中的检测套餐配置。
              </div>
            </div>
          </el-form-item>
          <el-form-item label="审核流程" :required="!isLoginReadonly">
            <el-input
              v-if="isLoginReadonly"
              :model-value="loginForm.reviewFlowName || '-'"
              readonly
            />
            <el-select
              v-else
              v-model="loginForm.reviewFlowId"
              filterable
              placeholder="请选择审核流程"
              style="width: 100%"
              @change="handleReviewFlowChange"
            >
              <el-option
                v-for="item in reviewFlowOptions"
                :key="item.id"
                :label="item.flowName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="发布流程" :required="!isLoginReadonly">
            <el-input
              v-if="isLoginReadonly"
              :model-value="loginForm.publishFlowName || '-'"
              readonly
            />
            <el-select
              v-else
              v-model="loginForm.publishFlowId"
              filterable
              placeholder="请选择发布流程"
              style="width: 100%"
              @change="handlePublishFlowChange"
            >
              <el-option
                v-for="item in publishFlowOptions"
                :key="item.id"
                :label="item.flowName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="采样时间" :required="!isLoginReadonly">
            <el-input v-if="isLoginReadonly" :model-value="loginForm.samplingTime || '-'" readonly />
            <el-date-picker
              v-else
              v-model="loginForm.samplingTime"
              type="datetime"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="天气">
            <el-input v-if="isLoginReadonly" :model-value="loginForm.weather || '-'" readonly />
            <el-select
              v-else
              v-model="loginForm.weather"
              allow-create
              clearable
              default-first-option
              filterable
              placeholder="请选择或输入天气"
              style="width: 100%"
            >
              <el-option
                v-for="option in weatherOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="保存条件">
            <el-input v-if="isLoginReadonly" :model-value="loginForm.storageCondition || '-'" readonly />
            <el-select
              v-else
              v-model="loginForm.storageCondition"
              allow-create
              clearable
              default-first-option
              filterable
              placeholder="请选择或输入保存条件"
              style="width: 100%"
            >
              <el-option
                v-for="option in storageConditionOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="loginForm.remark" :readonly="isLoginReadonly" placeholder="可补充样品来源、容器信息等说明" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="loginDialogVisible = false">{{ isLoginReadonly ? '关闭' : '取消' }}</el-button>
        <el-button v-if="!isLoginReadonly" v-permission="'sample:write'" type="primary" :loading="submitting" @click="submitSampleLogin">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="taskDetailDialogVisible"
      align-center
      title="采样任务详情"
      width="960px"
      top="5vh"
      :lock-scroll="true"
      destroy-on-close
      :modal-append-to-body="false"
      :append-to-body="true"
      class="task-detail-dialog"
      @closed="resetTaskDetailDialog"
    >
      <div class="task-detail-grid">
        <div><span>任务编号</span><strong>{{ taskDetail?.taskNo || taskDetail?.task_no || '-' }}</strong></div>
        <div><span>样品编号</span><strong>{{ taskDetail?.sampleNo || '-' }}</strong></div>
        <div><span>点位名称</span><strong>{{ taskDetail?.pointName || taskDetail?.point_name || '-' }}</strong></div>
        <div><span>采样人员</span><strong>{{ taskDetail?.samplerName || taskDetail?.sampler_name || '-' }}</strong></div>
        <div><span>检测套餐</span><strong>{{ taskDetail?.detectionTypeName || taskDetail?.detection_type_name || taskDetail?.detectionItems || '-' }}</strong></div>
        <div><span>采样依据</span><strong>{{ formatSamplingBasisText(taskDetail?.samplingBasis || taskDetail?.sampling_basis) || '-' }}</strong></div>
        <div><span>任务状态</span><strong>{{ getEnumLabel(taskStatusLabelMap, taskDetail?.taskStatus || taskDetail?.task_status) }}</strong></div>
        <div><span>完成时间</span><strong>{{ taskDetail?.finishedTime || taskDetail?.finished_time || '-' }}</strong></div>
        <div><span>天气</span><strong>{{ taskDetail?.weather || '-' }}</strong></div>
        <div><span>温度</span><strong>{{ taskDetail?.temperature || '-' }}</strong></div>
        <div><span>采样总容量</span><strong>{{ taskDetail?.sampleTotalVolume || taskDetail?.sample_total_volume || '-' }}</strong></div>
        <div><span>采样瓶数</span><strong>{{ taskDetail?.sampleBottleCount || taskDetail?.sample_bottle_count || '-' }}</strong></div>
      </div>
      <div class="task-detail-block">
        <span>检测参数明细</span>
        <el-table
          v-if="taskDetailDetectionConfigRows.length"
          class="login-config-table task-detail-config-table"
          :data="taskDetailDetectionConfigRows"
          size="small"
          border
        >
          <el-table-column prop="parameterName" label="检测参数" min-width="130" show-overflow-tooltip />
          <el-table-column label="参数类别" width="100" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">{{ formatParameterCategory(row) }}</template>
          </el-table-column>
          <el-table-column label="标准范围" min-width="120">
            <template #default="{ row }">
              {{ formatStandardRange(row.standardMin, row.standardMax) }}
            </template>
          </el-table-column>
          <el-table-column prop="unit" label="单位" width="72">
            <template #default="{ row }">{{ row.unit || '-' }}</template>
          </el-table-column>
          <el-table-column label="检测值" width="110">
            <template #default="{ row }">{{ row.resultValue ?? '-' }}</template>
          </el-table-column>
          <el-table-column prop="referenceStandard" label="检测标准" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ row.referenceStandard || '-' }}</template>
          </el-table-column>
          <el-table-column prop="methodName" label="检测方法" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ row.methodName || '-' }}</template>
          </el-table-column>
          <el-table-column prop="instrumentDisplayNames" label="采样设备" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">{{ row.instrumentDisplayNames || '-' }}</template>
          </el-table-column>
          <el-table-column prop="sampleVolume" label="取样体积" min-width="90" show-overflow-tooltip>
            <template #default="{ row }">{{ row.sampleVolume || '-' }}</template>
          </el-table-column>
        </el-table>
        <p v-else>暂无检测参数明细</p>
      </div>
      <div class="task-detail-block">
        <span>现场指标</span>
        <p>{{ taskDetail?.onsiteMetrics || '-' }}</p>
      </div>
      <div class="task-detail-block">
        <span>现场照片</span>
        <div v-if="taskDetailPhotos.length" class="task-detail-photo-list">
          <template v-for="(photo, index) in taskDetailPhotos" :key="`${photo.path}-${index}`">
            <el-image
              v-if="photo.previewUrl"
              class="task-detail-photo"
              :src="photo.previewUrl"
              :preview-src-list="taskDetailPhotoPreviewUrls"
              :initial-index="index"
              fit="cover"
              preview-teleported
            />
            <div v-else class="task-detail-photo task-detail-photo--failed">
              预览失败
            </div>
          </template>
        </div>
        <p v-else>暂无照片</p>
      </div>
      <div class="task-detail-block">
        <span>备注</span>
        <p>{{ taskDetail?.remark || '-' }}</p>
      </div>
      <template #footer>
        <el-button @click="taskDetailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElDatePicker } from 'element-plus/es/components/date-picker/index.mjs'
import { ElDialog } from 'element-plus/es/components/dialog/index.mjs'
import { ElForm, ElFormItem } from 'element-plus/es/components/form/index.mjs'
import { ElInput } from 'element-plus/es/components/input/index.mjs'
import { ElInputNumber } from 'element-plus/es/components/input-number/index.mjs'
import { ElImage } from 'element-plus/es/components/image/index.mjs'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
import { ElUpload } from 'element-plus/es/components/upload/index.mjs'
import TablePagination from '../components/common/TablePagination.vue'
import {
  abandonSamplingTaskApi,
  completeSamplingTaskApi,
  createSamplingPlanApi,
  dispatchSamplingPlanApi,
  fetchDictItemsApi,
  exportSamplingPlansApi,
  exportSamplesApi,
  exportSamplingTasksApi,
  fetchDetectionMethodInstrumentModelBindingsApi,
  fetchDetectionMethodOptionsApi,
  fetchDetectionParametersApi,
  fetchMonitoringPointsApi,
  fetchDetectionTypesApi,
  fetchFlowConfigOptionsApi,
  fetchSamplesApi,
  fetchSampleStatsApi,
  fetchSamplingTaskDetailApi,
  fetchSamplingPlansApi,
  fetchSamplingPlanStatsApi,
  fetchSamplingTasksApi,
  fetchSamplingTaskStatsApi,
  fetchSystemUsersApi,
  loginSampleApi,
  pauseSamplingPlanApi,
  resumeSamplingPlanApi,
  resumeSamplingTaskApi,
  startSamplingTaskApi,
  updateSamplingPlanApi,
  uploadStorageFileApi
} from '../api/lab'
import {
  activePlanStatus,
  abandonedTaskStatus,
  abandonableTaskStatuses,
  actionablePlanStatuses,
  completedPlanStatus,
  completedSampleStatus,
  completedTaskStatus,
  completableTaskStatuses,
  cycleTypeOptions,
  cycleTypeLabelMap,
  dailyCycleType,
  DEFAULT_PAGE_SIZE,
  dispatchedPlanStatuses,
  enabledPointStatus,
  factorySampleType,
  getEnumLabel,
  getStatusClass,
  fieldParameterCategory,
  inSituParameterCategory,
  inProgressTaskStatus,
  loggedSampleStatus,
  pausedPlanStatus,
  pendingTaskStatus,
  planStatusLabelMap,
  parameterCategoryLabelMap,
  qualityControlTypeLabelMap,
  qualityControlTypeOptions,
  retestSampleStatus,
  reviewingSampleStatus,
  routineSamplingType,
  sampleRegisterStatusLabelMap,
  sampleStatusLabelMap,
  sampleTypeOptions,
  sampleTypeLabelMap,
  taskStatusLabelMap,
  translateWorkflowText
} from '../utils/labEnums'

const route = useRoute()
const FLOW_TYPE_REVIEW = 'REVIEW'
const FLOW_TYPE_PUBLISH = 'PUBLISH'
const samplingBasisOptions = [
  'GB 5749-2022',
  'GB/T 5750.2-2023',
  'GB 3838-2002',
  'HJ 91.2-2022',
  'GB/T 14848-2017',
  'HJ 164-2020',
  'HJ 91.1-2019',
  'HJ 493-2009',
  'HJ 494-2009',
  'GB/T 14581-1993',
  'GB/T 13580.2-1992'
]

const planQuery = reactive({
  keyword: '',
  planStatus: '',
  samplerId: '',
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE
})
const taskQuery = reactive({
  keyword: '',
  taskStatus: '',
  samplerId: '',
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE
})
const sampleQuery = reactive({
  keyword: '',
  sampleStatus: '',
  sampleType: '',
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE
})

const plans = ref([])
const tasks = ref([])
const loggableTasks = ref([])
const samples = ref([])
const planTotal = ref(0)
const taskTotal = ref(0)
const sampleTotal = ref(0)
const planStatCounts = ref({})
const taskStatCounts = ref({})
const sampleStatCounts = ref({})
const activeStatKey = ref('tasks:all')
const loginDialogVisible = ref(false)
const loginDialogMode = ref('create')
const planDialogVisible = ref(false)
const taskCompleteDialogVisible = ref(false)
const dispatchDialogVisible = ref(false)
const mapSelectorVisible = ref(false)
const mapSelectorValue = reactive({ address: '', latitude: '', longitude: '' })
const taskDetailDialogVisible = ref(false)
const weatherOptions = ref([])
const storageConditionOptions = ref([])
const editingPlanId = ref(null)
const submitting = ref(false)
const taskCompleteSubmitting = ref(false)
const taskCompletePhotoUploading = ref(false)
const dispatchSubmitting = ref(false)
const rowActionLoading = reactive({})
const monitoringPointOptions = ref([])
const monitoringPointLoading = ref(false)
const samplerOptions = ref([])
const samplerLoading = ref(false)
const detectionProjectOptions = ref([])
const detectionParameterOptions = ref([])
const detectionMethodOptions = ref([])
const reviewFlowOptions = ref([])
const publishFlowOptions = ref([])
const loginPreviewTaskLabel = ref('')
const taskCompletePreview = ref(null)
const taskCompletePhotoList = ref([])
const taskDetailPhotoList = ref([])
const taskDetail = ref(null)

const loginForm = reactive({
  taskId: null,
  sampleNo: '',
  pointId: null,
  pointName: '',
  sampleType: '',
  qualityControlType: '',
  detectionItems: '',
  detectionTypeId: null,
  detectionTypeName: '',
  detectionConfigItems: [],
  reviewFlowId: null,
  reviewFlowName: '',
  publishFlowId: null,
  publishFlowName: '',
  samplingTime: '',
  samplerId: null,
  samplerName: '',
  weather: '',
  storageCondition: '',
  remark: ''
})

const taskCompletePreviewPhotoUrls = computed(() =>
  taskCompletePhotoList.value.map((item) => item.previewUrl).filter(Boolean)
)
const taskDetailPhotos = computed(() => taskDetailPhotoList.value)
const taskDetailPhotoPreviewUrls = computed(() =>
  taskDetailPhotoList.value.map((item) => item.previewUrl).filter(Boolean)
)
const detectionMethodInstrumentModelNameMap = ref({})
const detectionMethodInstrumentModelLoaded = ref(false)
const taskCompleteDetectionConfigRows = computed(() => (
  parseTaskDetectionConfigSnapshot(taskCompletePreview.value?.detectionConfigSnapshot).map((item) => ({
    ...item,
    instrumentDisplayNames: resolveMethodInstrumentDisplayNames(item?.methodId)
  }))
))
const expectedTaskCompleteSampleTotalVolume = computed(() => {
  const total = taskCompleteDetectionConfigRows.value.reduce((sum, item) => (
    sum + toSafeNumber(extractSampleVolumeNumber(item?.sampleVolume))
  ), 0)
  if (!Number.isFinite(total) || total <= 0) {
    return ''
  }
  return Number.isInteger(total) ? String(total) : String(Number(total.toFixed(2)))
})
const taskDetailDetectionConfigRows = computed(() => (
  parseTaskDetectionConfigSnapshot(taskDetail.value?.detectionConfigSnapshot).map((item) => ({
    ...item,
    instrumentDisplayNames: item.instrumentDisplayNames || resolveMethodInstrumentDisplayNames(item?.methodId)
  }))
))

const isLoginReadonly = computed(() => loginDialogMode.value === 'view')
const loginDialogTitle = computed(() => isLoginReadonly.value ? '样品登记明细' : '样品登录')
const loginConfigPanelNote = computed(() => (
  isLoginReadonly.value
    ? '当前仅展示该样品登记时保存的检测参数与检测方法明细，不可在此窗口中修改。'
    : '检测套餐由采样计划确定，样品登录只读展示任务携带的检测参数与检测方法明细。'
))

const planForm = reactive({
  planName: '',
  pointSource: 'EXISTING',
  pointId: null,
  pointName: '',
  startTime: '',
  endTime: '',
  samplerId: null,
  samplerName: '',
  samplingType: routineSamplingType,
  sampleType: '',
  detectionTypeId: null,
  detectionTypeName: '',
  detectionConfigItems: [],
  samplingBasisList: [],
  cycleType: dailyCycleType,
  remark: ''
})

const dispatchForm = reactive({
  planId: null,
  samplingTime: '',
  samplerId: null,
  samplerName: ''
})

const taskCompleteForm = reactive({
  taskId: null,
  onsiteMetrics: '',
  weather: '',
  temperature: '',
  sampleTotalVolume: '',
  sampleBottleCount: null,
  photoUrls: '',
  remark: '',
  address: '',
  latitude: '',
  longitude: ''
})

function toSafeNumber(value) {
  const num = typeof value === 'number' ? value : Number.parseFloat(String(value ?? '').replace(/,/g, '').trim())
  return Number.isFinite(num) ? num : 0
}

function buildCountMap(items) {
  return (items || []).reduce((result, item) => {
    result[String(item.status || '')] = toSafeNumber(item.count)
    return result
  }, {})
}

function getCount(counts, key) {
  return toSafeNumber(counts?.[key])
}

function normalizeDictOptions(items) {
  return (items || [])
    .map((item) => {
      const label = String(item.label || item.value || '').trim()
      const value = String(item.value || item.label || '').trim()
      return label && value ? { label, value } : null
    })
    .filter(Boolean)
}

function formatParameterCategory(value) {
  const rawValue = typeof value === 'object'
    ? (value?.parameterCategoryDesc || value?.parameter_category_desc || value?.parameterCategory || value?.parameter_category || '')
    : value
  return getEnumLabel(parameterCategoryLabelMap, String(rawValue || '').trim())
}

function normalizeParameterCategoryCode(value) {
  const rawValue = String(value || '').trim()
  if (rawValue === '原位检测') return 'IN_SITU'
  if (rawValue === '现场测定') return 'FIELD'
  if (rawValue === '实验室测定') return 'LABORATORY'
  return rawValue
}

function buildRowActionKey(scope, action, id) {
  return `${scope}:${action}:${id ?? 'unknown'}`
}

function isRowActionLoading(scope, action, id) {
  return Boolean(rowActionLoading[buildRowActionKey(scope, action, id)])
}

function beginRowAction(scope, action, id) {
  const key = buildRowActionKey(scope, action, id)
  if (rowActionLoading[key]) {
    return false
  }
  rowActionLoading[key] = true
  return true
}

function endRowAction(scope, action, id) {
  delete rowActionLoading[buildRowActionKey(scope, action, id)]
}

const sceneMap = {
  '/task-assign': {
    key: 'task-assign',
    title: '采样任务',
    subtitle: '聚焦待处理采样任务，适合班组长或调度人员快速推进采样执行。',
    tableTitle: '待办采样任务',
    tableSubtitle: '默认聚焦待处理任务，并保留开始、废弃、恢复、完成等现场执行动作。',
    note: '采样任务页强调今天要做什么，样品编号会在任务生成时按规则自动生成，上方统计卡可切换到进行中、已完成、已废弃视角。',
    guide: '如需安排周期计划，请进入独立的采样计划页面；任务完成后再进入样品登录。',
    mode: 'task',
    defaultStatKey: 'tasks:pending',
    allowTaskActions: true,
    showPlanSection: false,
    emptyText: '暂无待处理采样任务数据',
    taskFilter: () => true,
    sampleFilter: () => true,
    quickLinks: [
      { path: '/sampling-plan', label: '采样计划', desc: '独立维护周期计划并执行手动派发' },
      { path: '/sample-login', label: '样品登录', desc: '将已完成采样任务登记为正式样品' },
      { path: '/task-history', label: '历史任务', desc: '查看已完成或已废弃的采样执行记录' },
      { path: '/sample-ledger', label: '样品台账', desc: '查看样品编号、状态与流程留痕' }
    ]
  },
  '/sampling-plan': {
    key: 'sampling-plan',
    title: '采样计划',
    subtitle: '独立维护周期采样计划，支持新增、编辑、暂停、恢复与手动派发，便于和采样任务分离管理。',
    tableTitle: '周期采样计划',
    tableSubtitle: '延续原采样任务页中的计划能力，保留计划台账、派发动作与计划维护表单。',
    note: '采样计划页只处理计划本身，不再混合展示采样任务，便于单独维护周期配置和派发节奏。',
    guide: '计划派发生成任务后，再回到采样任务或样品登录继续推进现场与样品闭环。',
    mode: 'plan',
    defaultStatKey: 'plans:all',
    allowTaskActions: false,
    showPlanSection: true,
    emptyText: '暂无采样计划数据',
    taskFilter: () => true,
    sampleFilter: () => true,
    quickLinks: [
      { path: '/task-assign', label: '采样任务', desc: '查看计划派发后生成的待执行采样任务' },
      { path: '/sample-login', label: '样品登录', desc: '承接完成采样后的样品登记流程' },
      { path: '/task-ledger', label: '任务台账', desc: '追踪计划转任务后的全量执行记录' }
    ]
  },
  '/task-history': {
    key: 'task-history',
    title: '历史任务',
    subtitle: '回看已经完成或已废弃的采样任务，方便核对现场执行情况与补录链路。',
    tableTitle: '历史采样任务',
    tableSubtitle: '本页聚焦已完成、已废弃任务，操作区切换为只读查询视角。',
    note: '历史任务页用于追溯与复盘，不再承载现场执行按钮，避免误操作。',
    guide: '如历史任务已形成样品，可直接跳转到样品台账继续核验编号与留痕。',
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
      { path: '/sample-login', label: '样品登录', desc: '承接已完成任务并延续已有样品编号' },
      { path: '/task-history', label: '历史任务', desc: '只看已完成与已废弃任务' },
      { path: '/detection-analysis', label: '检测分析', desc: '继续进入化验室检测流程' }
    ]
  },
  '/sample-login': {
    key: 'sample-login',
    title: '样品登录',
    subtitle: '将已完成采样任务转成正式样品，沿用任务已生成的样品编号并承接后续检测、审核、报告链路。',
    tableTitle: '已登记样品',
    tableSubtitle: '默认展示已登记样品，并通过统计卡切换到待审核、退回重检、闭环完成等状态。',
    note: '样品登录页优先解决未登记任务，直接选择已完成采样的任务后回显任务已生成的样品编号。',
    guide: '如本页没有可登录样品，请先回到采样任务完成采样任务；若样品已登记，可继续前往检测分析。',
    mode: 'sample',
    defaultStatKey: 'samples:logged',
    allowTaskActions: false,
    showPlanSection: false,
    emptyText: '暂无样品登录数据',
    taskFilter: () => true,
    sampleFilter: () => true,
    quickLinks: [
      { path: '/task-assign', label: '采样任务', desc: '先完成现场采样任务，再进行样品登录' },
      { path: '/detection-analysis', label: '检测分析', desc: '样品登录完成后进入化验室检测流程' },
      { path: '/sample-ledger', label: '样品台账', desc: '查看全量样品编号与流程留痕' }
    ]
  },
  '/sample-ledger': {
    key: 'sample-ledger',
    title: '样品台账',
    subtitle: '集中查看全部样品编号、保存条件、结果摘要和全流程留痕信息。',
    tableTitle: '样品全量台账',
    tableSubtitle: '本页不再强调登录动作，转为全量追踪样品状态与闭环结果。',
    note: '样品台账页适合盘点样品编号与状态流转，可通过统计卡快速定位待审核、退回重检、已完成样品。',
    guide: '如需新增样品，请回到样品登录；如需继续推进流程，可跳转到检测分析或结果审查。',
    mode: 'sample',
    defaultStatKey: 'samples:all',
    allowTaskActions: false,
    showPlanSection: false,
    emptyText: '暂无样品台账数据',
    taskFilter: () => true,
    sampleFilter: () => true,
    quickLinks: [
      { path: '/sample-login', label: '样品登录', desc: '继续补录新样品并承接编号' },
      { path: '/detection-analysis', label: '检测分析', desc: '查看样品是否已进入检测流程' },
      { path: '/review-result', label: '结果审查', desc: '跟踪样品检测后的审核流转' }
    ]
  }
}

const baseScene = computed(() => sceneMap[route.path] || sceneMap['/task-assign'])
const isTaskScene = computed(() => baseScene.value.mode === 'task')
const isPlanScene = computed(() => baseScene.value.mode === 'plan')
const isTaskTodoScene = computed(() => baseScene.value.key === 'task-assign')
const taskSceneRecords = computed(() => tasks.value.filter((item) => baseScene.value.taskFilter(item)))
const sampleSceneRecords = computed(() => samples.value.filter((item) => baseScene.value.sampleFilter(item)))
const planStatusOptions = computed(() => Object.entries(planStatusLabelMap).map(([value, label]) => ({ value, label })))
const taskStatusOptions = computed(() => Object.entries(taskStatusLabelMap).map(([value, label]) => ({ value, label })))
const sampleStatusOptions = computed(() => Object.entries(sampleStatusLabelMap).map(([value, label]) => ({ value, label })))

function isTaskRegistered(task) {
  if (!task) {
    return false
  }
  if (task.sampleRegisterStatus === 'REGISTERED' || task.sampleId) {
    return true
  }
  return samples.value.some((sample) => sample.taskId === task.id)
}

function isTaskCompleteEntryEnabled(task) {
  return [pendingTaskStatus, inProgressTaskStatus].includes(task?.taskStatus)
}

const pendingLoggableCount = computed(() =>
  loggableTasks.value.length || getCount(taskStatCounts.value, 'UNLOGGED')
)

const firstCompletableTask = computed(() =>
  taskSceneRecords.value.find((item) => completableTaskStatuses.includes(item.taskStatus))
)

const firstLoggableTask = computed(() =>
  loggableTasks.value[0] || null
)

const pendingLoggableTasks = computed(() =>
  loggableTasks.value
)

const activeMissingSamplerPlans = computed(() =>
  plans.value.filter((item) =>
    item.planStatus === activePlanStatus && (!item.samplerId || !item.samplerName)
  )
)

const planDispatchNotice = computed(() => {
  if (!activeMissingSamplerPlans.value.length) {
    return '当前启用计划均已指定采样员，满足自动派发前提。'
  }
  return `当前有 ${activeMissingSamplerPlans.value.length} 个启用计划未指定采样员，自动派发会跳过这些计划，请先补充人员。`
})

const currentScene = computed(() => ({
  ...baseScene.value,
  tags: isPlanScene.value
    ? [
        {
          label: '启用中',
          value: getCount(planStatCounts.value, activePlanStatus),
          type: 'info'
        },
        {
          label: '待补采样员',
          value: getCount(planStatCounts.value, 'MISSING_SAMPLER'),
          type: getCount(planStatCounts.value, 'MISSING_SAMPLER') ? 'warning' : 'success'
        },
        {
          label: '已派发',
          value: dispatchedPlanStatuses.reduce((sum, status) => sum + getCount(planStatCounts.value, status), 0),
          type: 'success'
        }
      ]
    : isTaskScene.value
    ? [
        {
          label: '待处理',
          value: getCount(taskStatCounts.value, pendingTaskStatus),
          type: 'warning'
        },
        {
          label: '进行中',
          value: getCount(taskStatCounts.value, inProgressTaskStatus),
          type: 'info'
        },
        {
          label: '待样品登录',
          value: getCount(taskStatCounts.value, 'UNLOGGED'),
          type: getCount(taskStatCounts.value, 'UNLOGGED') ? 'warning' : 'success'
        }
      ]
    : [
        {
          label: '已登记',
          value: getCount(sampleStatCounts.value, loggedSampleStatus),
          type: 'info'
        },
        {
          label: '退回重检',
          value: getCount(sampleStatCounts.value, retestSampleStatus),
          type: getCount(sampleStatCounts.value, retestSampleStatus) ? 'danger' : 'success'
        },
        {
          label: '已闭环',
          value: getCount(sampleStatCounts.value, completedSampleStatus),
          type: 'success'
        }
      ]
}))

const currentStats = computed(() => {
  if (isPlanScene.value) {
    return []
  }

  if (isTaskScene.value) {
    if (isTaskTodoScene.value) {
      const todoTotal = getCount(taskStatCounts.value, 'TODO')
      const unloggedCount = getCount(taskStatCounts.value, 'UNLOGGED')
      return [
        {
          key: 'tasks:all',
          label: '采样待办',
          value: todoTotal,
          desc: '待执行和待样品登录任务'
        },
        {
          key: 'tasks:pending',
          label: '待处理',
          value: getCount(taskStatCounts.value, pendingTaskStatus),
          desc: '尚未开始执行的采样任务'
        },
        {
          key: 'tasks:progress',
          label: '进行中',
          value: getCount(taskStatCounts.value, inProgressTaskStatus),
          desc: '正在现场执行的采样任务'
        },
        {
          key: 'tasks:unlogged',
          label: '待样品登录',
          value: unloggedCount,
          desc: '已完成采样但尚未登记为样品的任务'
        }
      ]
    }
    return [
      {
        key: 'tasks:all',
        label: baseScene.value.key === 'task-history' ? '历史任务' : '任务总览',
        value: getCount(taskStatCounts.value, 'ALL'),
        desc: '采样任务台账总量'
      },
      {
        key: 'tasks:pending',
        label: '待处理',
        value: getCount(taskStatCounts.value, pendingTaskStatus),
        desc: '尚未开始执行的采样任务'
      },
      {
        key: 'tasks:progress',
        label: '进行中',
        value: getCount(taskStatCounts.value, inProgressTaskStatus),
        desc: '正在现场执行的采样任务'
      },
      {
        key: 'tasks:completed',
        label: '已完成',
        value: getCount(taskStatCounts.value, completedTaskStatus),
        desc: '已经完成采样并可进入样品登录的任务'
      },
      {
        key: 'tasks:abandoned',
        label: '已废弃',
        value: getCount(taskStatCounts.value, abandonedTaskStatus),
        desc: '因现场条件等原因废弃的任务'
      },
      {
        key: 'tasks:unlogged',
        label: '待样品登录',
        value: getCount(taskStatCounts.value, 'UNLOGGED'),
        desc: '已完成采样但尚未生成样品的任务'
      }
    ]
  }

  return [
    {
      key: 'samples:all',
      label: baseScene.value.key === 'sample-ledger' ? '样品总量' : '样品总览',
      value: getCount(sampleStatCounts.value, 'ALL'),
      desc: '样品台账总量'
    },
    {
      key: 'samples:logged',
      label: baseScene.value.key === 'sample-login' ? '已登记样品' : '登记完成',
      value: getCount(sampleStatCounts.value, loggedSampleStatus),
      desc: '已经生成样品编号并等待后续检测的样品'
    },
    {
      key: 'samples:reviewing',
      label: '待审核',
      value: getCount(sampleStatCounts.value, reviewingSampleStatus),
      desc: '检测完成后进入审核流程的样品'
    },
    {
      key: 'samples:retest',
      label: '退回重检',
      value: getCount(sampleStatCounts.value, retestSampleStatus),
      desc: '被审核退回，等待重新检测的样品'
    },
    {
      key: 'samples:completed',
      label: '闭环完成',
      value: getCount(sampleStatCounts.value, completedSampleStatus),
      desc: '检测、审核已完成闭环的样品'
    },
    {
      key: 'samples:todo-login',
      label: '待登录任务',
      value: getCount(taskStatCounts.value, 'UNLOGGED'),
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
      item.taskStatus === completedTaskStatus && !isTaskRegistered(item)
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

const visiblePlans = computed(() => {
  if (activeStatKey.value === 'plans:active') {
    return plans.value.filter((item) => item.planStatus === activePlanStatus)
  }
  if (activeStatKey.value === 'plans:missing-sampler') {
    return activeMissingSamplerPlans.value
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

const planStats = computed(() => [
  {
    key: 'plans:all',
    label: '计划总量',
    value: getCount(planStatCounts.value, 'ALL'),
    desc: '周期采样计划台账总量'
  },
  {
    key: 'plans:active',
    label: '启用中',
    value: getCount(planStatCounts.value, activePlanStatus),
    desc: '当前处于启用状态的采样计划'
  },
  {
    key: 'plans:missing-sampler',
    label: '待补采样员',
    value: getCount(planStatCounts.value, 'MISSING_SAMPLER'),
    desc: '已启用但未指定采样员，自动派发会跳过'
  },
  {
    key: 'plans:paused',
    label: '已暂停',
    value: getCount(planStatCounts.value, pausedPlanStatus),
    desc: '临时暂停执行的采样计划'
  },
  {
    key: 'plans:dispatched',
    label: '已派发',
    value: dispatchedPlanStatuses.reduce((sum, status) => sum + getCount(planStatCounts.value, status), 0),
    desc: '已经生成采样任务的采样计划'
  },
  {
    key: 'plans:completed',
    label: '已完成',
    value: getCount(planStatCounts.value, completedPlanStatus),
    desc: '已完成闭环的周期计划'
  }
])

function syncRouteState() {
  activeStatKey.value = getAllStatKeyForCurrentScene()
  planQuery.planStatus = ''
  taskQuery.taskStatus = ''
  sampleQuery.sampleStatus = ''
  if (isPlanScene.value) {
    planQuery.pageNum = 1
    return
  }
  if (isTaskScene.value) {
    taskQuery.pageNum = 1
    return
  }
  sampleQuery.pageNum = 1
}

function handleStatClick(key) {
  const nextKey = activeStatKey.value === key ? getAllStatKeyForCurrentScene() : key
  activeStatKey.value = nextKey
  applyStatToCurrentSceneQuery(nextKey)
  loadCurrentSceneData()
}

function handlePlanStatClick(key) {
  const nextKey = activeStatKey.value === key ? 'plans:all' : key
  activeStatKey.value = nextKey
  applyStatToCurrentSceneQuery(nextKey)
  loadPlans()
}

function applyStatToCurrentSceneQuery(key) {
  if (isPlanScene.value) {
    planQuery.planStatus = getPlanStatusByStatKey(key) || ''
    planQuery.pageNum = 1
    return
  }
  if (isTaskScene.value) {
    taskQuery.taskStatus = getTaskStatusByStatKey(key) || ''
    taskQuery.pageNum = 1
    return
  }
  sampleQuery.sampleStatus = getSampleStatusByStatKey(key) || ''
  sampleQuery.pageNum = 1
}

function getAllStatKeyForCurrentScene() {
  if (isPlanScene.value) {
    return 'plans:all'
  }
  if (isTaskScene.value) {
    return 'tasks:all'
  }
  return 'samples:all'
}

function getPlanStatusByStatKey(key) {
  const statusMap = {
    'plans:active': activePlanStatus,
    'plans:missing-sampler': activePlanStatus,
    'plans:paused': pausedPlanStatus,
    'plans:dispatched': dispatchedPlanStatuses[0],
    'plans:completed': completedPlanStatus
  }
  return statusMap[key]
}

function getTaskStatusByStatKey(key) {
  const statusMap = {
    'tasks:pending': pendingTaskStatus,
    'tasks:progress': inProgressTaskStatus,
    'tasks:completed': completedTaskStatus,
    'tasks:abandoned': abandonedTaskStatus,
    'tasks:unlogged': completedTaskStatus
  }
  return statusMap[key]
}

function getSampleStatusByStatKey(key) {
  const statusMap = {
    'samples:logged': loggedSampleStatus,
    'samples:reviewing': reviewingSampleStatus,
    'samples:retest': retestSampleStatus,
    'samples:completed': completedSampleStatus
  }
  return statusMap[key]
}

function syncActiveStatByCurrentQuery() {
  if (isPlanScene.value) {
    if (planQuery.planStatus === activePlanStatus) {
      activeStatKey.value = 'plans:active'
    } else if (planQuery.planStatus === pausedPlanStatus) {
      activeStatKey.value = 'plans:paused'
    } else if (dispatchedPlanStatuses.includes(planQuery.planStatus)) {
      activeStatKey.value = 'plans:dispatched'
    } else if (planQuery.planStatus === completedPlanStatus) {
      activeStatKey.value = 'plans:completed'
    } else {
      activeStatKey.value = 'plans:all'
    }
    return
  }
  if (isTaskScene.value) {
    if (taskQuery.taskStatus === pendingTaskStatus) {
      activeStatKey.value = 'tasks:pending'
    } else if (taskQuery.taskStatus === inProgressTaskStatus) {
      activeStatKey.value = 'tasks:progress'
    } else if (taskQuery.taskStatus === completedTaskStatus) {
      activeStatKey.value = 'tasks:completed'
    } else if (taskQuery.taskStatus === abandonedTaskStatus) {
      activeStatKey.value = 'tasks:abandoned'
    } else {
      activeStatKey.value = 'tasks:all'
    }
    return
  }
  if (sampleQuery.sampleStatus === loggedSampleStatus) {
    activeStatKey.value = 'samples:logged'
  } else if (sampleQuery.sampleStatus === reviewingSampleStatus) {
    activeStatKey.value = 'samples:reviewing'
  } else if (sampleQuery.sampleStatus === retestSampleStatus) {
    activeStatKey.value = 'samples:retest'
  } else if (sampleQuery.sampleStatus === completedSampleStatus) {
    activeStatKey.value = 'samples:completed'
  } else {
    activeStatKey.value = 'samples:all'
  }
}

function isTaskLogged(taskId) {
  return isTaskRegistered(tasks.value.find((item) => item.id === taskId))
}

async function loadPlans() {
  const result = await fetchSamplingPlansApi(planQuery)
  plans.value = result.records || []
  planTotal.value = toSafeNumber(result.total)
}

async function loadPlanStats() {
  planStatCounts.value = buildCountMap(await fetchSamplingPlanStatsApi())
}

function buildTaskQueryPayload(extra = {}) {
  return {
    ...taskQuery,
    scope: isTaskTodoScene.value ? 'todo' : '',
    ...extra
  }
}

function buildTaskStatsQueryPayload() {
  return buildTaskQueryPayload({ taskStatus: '' })
}

async function loadTasks() {
  const result = await fetchSamplingTasksApi(buildTaskQueryPayload())
  tasks.value = result.records || []
  taskTotal.value = toSafeNumber(result.total)
}

async function loadTaskStats() {
  taskStatCounts.value = buildCountMap(await fetchSamplingTaskStatsApi(buildTaskStatsQueryPayload()))
}

async function loadLoggableTasks() {
  const result = await fetchSamplingTasksApi({
    pageNum: 1,
    pageSize: 500,
    taskStatus: completedTaskStatus
  })
  const records = Array.isArray(result.records) ? result.records : []
  loggableTasks.value = records.filter((item) => !isTaskRegistered(item))
}

async function loadSamples() {
  const result = await fetchSamplesApi(sampleQuery)
  samples.value = result.records || []
  sampleTotal.value = toSafeNumber(result.total)
}

async function loadSampleStats() {
  sampleStatCounts.value = buildCountMap(await fetchSampleStatsApi())
}

async function loadCurrentSceneData() {
  if (isPlanScene.value) {
    await Promise.all([loadPlans(), loadPlanStats()])
    return
  }
  if (isTaskScene.value) {
    await Promise.all([loadTasks(), loadTaskStats()])
    return
  }
  if (baseScene.value.key === 'sample-login') {
    await Promise.all([loadSamples(), loadTaskStats(), loadSampleStats()])
    await loadLoggableTasks()
    return
  }
  await Promise.all([loadSamples(), loadSampleStats(), loadTaskStats()])
  await loadLoggableTasks()
}

async function handleExportCurrentScene() {
  try {
    if (isPlanScene.value) {
      await exportSamplingPlansApi({ ...planQuery })
      ElMessage.success('采样计划导出成功')
      return
    }
    if (isTaskScene.value) {
      await exportSamplingTasksApi(buildTaskQueryPayload())
      ElMessage.success('采样任务导出成功')
      return
    }
    await exportSamplesApi({ ...sampleQuery })
    ElMessage.success('样品台账导出成功')
  } catch (error) {
    ElMessage.error(error.message || '导出失败')
  }
}

async function handleExportPlans() {
  try {
    await exportSamplingPlansApi({ ...planQuery })
    ElMessage.success('采样计划导出成功')
  } catch (error) {
    ElMessage.error(error.message || '采样计划导出失败')
  }
}

function handlePlanSearch() {
  planQuery.pageNum = 1
  syncActiveStatByCurrentQuery()
  loadPlans()
}

function resetPlanQuery() {
  planQuery.keyword = ''
  planQuery.planStatus = ''
  planQuery.samplerId = ''
  planQuery.pageNum = 1
  activeStatKey.value = 'plans:all'
  loadPlans()
}

function handleCurrentSceneSearch() {
  if (isTaskScene.value) {
    taskQuery.pageNum = 1
    syncActiveStatByCurrentQuery()
    Promise.all([loadTasks(), loadTaskStats()])
    return
  }
  sampleQuery.pageNum = 1
  syncActiveStatByCurrentQuery()
  loadSamples()
}

function resetCurrentSceneQuery() {
  if (isTaskScene.value) {
    taskQuery.keyword = ''
    taskQuery.taskStatus = ''
    taskQuery.samplerId = ''
    taskQuery.pageNum = 1
    activeStatKey.value = 'tasks:all'
    Promise.all([loadTasks(), loadTaskStats()])
    return
  }
  sampleQuery.keyword = ''
  sampleQuery.sampleStatus = ''
  sampleQuery.sampleType = ''
  sampleQuery.pageNum = 1
  activeStatKey.value = 'samples:all'
  loadSamples()
}

async function loadMonitoringPoints() {
  if (monitoringPointOptions.value.length) {
    return
  }
  monitoringPointLoading.value = true
  try {
    const result = await fetchMonitoringPointsApi({
      pageNum: 1,
      pageSize: 500,
      pointStatus: enabledPointStatus
    })
    monitoringPointOptions.value = result.records || []
  } finally {
    monitoringPointLoading.value = false
  }
}

async function loadSamplers() {
  if (samplerOptions.value.length) {
    return
  }
  samplerLoading.value = true
  try {
    const result = await fetchSystemUsersApi({
      pageNum: 1,
      pageSize: 500,
      roleCode: 'SAMPLER',
      status: 1
    })
    const records = Array.isArray(result.records) ? result.records : []
    samplerOptions.value = records
  } finally {
    samplerLoading.value = false
  }
}

function handleSamplerDropdownVisible(visible) {
  if (visible) {
    loadSamplers()
  }
}

async function loadSamplingDictOptions() {
  const [weatherItems, storageItems] = await Promise.all([
    fetchDictItemsApi('weather_condition'),
    fetchDictItemsApi('storage_condition')
  ])
  weatherOptions.value = normalizeDictOptions(weatherItems)
  storageConditionOptions.value = normalizeDictOptions(storageItems)
}

function handlePlanSamplerChange(userId) {
  const user = samplerOptions.value.find((item) => item.id === userId)
  planForm.samplerId = user?.id || null
  planForm.samplerName = user?.realName || user?.username || ''
}

function parseSamplingBasisList(value) {
  if (Array.isArray(value)) {
    return value.map((item) => String(item || '').trim()).filter(Boolean)
  }
  return String(value || '')
    .split('、')
    .map((item) => item.trim())
    .filter(Boolean)
}

function formatSamplingBasisText(value) {
  return parseSamplingBasisList(value).join('、')
}

function extractSampleVolumeNumber(value) {
  const text = String(value || '').trim().replace(/\s+/g, '')
  return text.replace(/(?:ml|毫升)$/i, '')
}

function sanitizeSampleVolumeNumber(value) {
  const text = extractSampleVolumeNumber(value).replace(/[^\d.]/g, '')
  const parts = text.split('.')
  if (parts.length <= 1) {
    return parts[0]
  }
  return `${parts[0]}.${parts.slice(1).join('')}`
}

function buildSampleVolumePayload(value) {
  const numberText = sanitizeSampleVolumeNumber(value)
  return numberText ? `${numberText}mL` : ''
}

function isSamplingDetectionResultEditable(row) {
  if (!row) {
    return false
  }
  const category = String(row.parameterCategory || row.parameter_category || row.parameterCategoryDesc || row.parameter_category_desc || '').trim()
  return [inSituParameterCategory, fieldParameterCategory, '原位检测', '现场测定'].includes(category)
}

function handleTaskCompleteSampleTotalVolumeInput(value) {
  taskCompleteForm.sampleTotalVolume = sanitizeSampleVolumeNumber(value)
}

function resetDispatchForm() {
  dispatchForm.planId = null
  dispatchForm.samplingTime = ''
  dispatchForm.samplerId = null
  dispatchForm.samplerName = ''
}

function handleDispatchSamplerChange(userId) {
  const user = samplerOptions.value.find((item) => item.id === userId)
  dispatchForm.samplerId = user?.id || null
  dispatchForm.samplerName = user?.realName || user?.username || ''
}

function resetPlanForm() {
  editingPlanId.value = null
  planForm.planName = ''
  planForm.pointSource = 'EXISTING'
  planForm.pointId = null
  planForm.pointName = ''
  planForm.startTime = ''
  planForm.endTime = ''
  planForm.samplerId = null
  planForm.samplerName = ''
  planForm.samplingType = routineSamplingType
  planForm.sampleType = ''
  planForm.detectionTypeId = null
  planForm.detectionTypeName = ''
  planForm.detectionConfigItems = []
  planForm.samplingBasisList = []
  planForm.cycleType = dailyCycleType
  planForm.remark = ''
}

async function openPlanDialog() {
  resetPlanForm()
  planForm.planName = `采样计划-${dayjs().format('MMDD-HHmm')}`
  planForm.startTime = dayjs().format('YYYY-MM-DD HH:mm:ss')
  planForm.endTime = dayjs().add(7, 'day').format('YYYY-MM-DD HH:mm:ss')
  await Promise.all([loadMonitoringPoints(), loadSamplers(), loadDetectionProjects()])
  if (monitoringPointOptions.value.length) {
    handlePlanPointChange(monitoringPointOptions.value[0].id)
  } else {
    planForm.pointSource = 'CUSTOM'
  }
  planDialogVisible.value = true
}

async function openPlanEditDialog(row) {
  await Promise.all([loadMonitoringPoints(), loadSamplers(), loadDetectionProjects()])
  editingPlanId.value = row.id
  planForm.planName = row.planName || ''
  planForm.pointSource = row.pointId ? 'EXISTING' : 'CUSTOM'
  planForm.pointId = row.pointId || null
  planForm.pointName = row.pointName || ''
  planForm.startTime = row.startTime || ''
  planForm.endTime = row.endTime || ''
  planForm.samplerId = row.samplerId || null
  planForm.samplerName = row.samplerName || ''
  planForm.samplingType = row.samplingType || routineSamplingType
  planForm.sampleType = row.sampleType || ''
  planForm.detectionTypeId = row.detectionTypeId || null
  planForm.detectionTypeName = row.detectionTypeName || ''
  planForm.detectionConfigItems = parseSampleDetectionConfigSnapshot(row.detectionConfigSnapshot)
  planForm.samplingBasisList = parseSamplingBasisList(row.samplingBasis || row.sampling_basis)
  if (!planForm.detectionConfigItems.length && planForm.detectionTypeId) {
    planForm.detectionConfigItems = buildLoginDetectionConfigItems(getDetectionTypeById(planForm.detectionTypeId))
  }
  planForm.cycleType = row.cycleType || dailyCycleType
  planForm.remark = row.remark || ''
  if (planForm.pointSource === 'EXISTING' && planForm.pointId) {
    handlePlanPointChange(planForm.pointId)
  }
  planDialogVisible.value = true
}

function handlePlanPointSourceChange(value) {
  if (value === 'CUSTOM') {
    planForm.pointId = null
    planForm.pointName = ''
    return
  }
  if (monitoringPointOptions.value.length) {
    handlePlanPointChange(planForm.pointId || monitoringPointOptions.value[0].id)
  }
}

function handlePlanPointChange(pointId) {
  const point = monitoringPointOptions.value.find((item) => item.id === pointId)
  planForm.pointId = point?.id || null
  planForm.pointName = point?.pointName || ''
}

function handlePlanDetectionTypeChange(typeId) {
  const detectionType = getDetectionTypeById(typeId)
  planForm.detectionTypeId = detectionType?.id || null
  planForm.detectionTypeName = detectionType?.typeName || ''
  planForm.detectionConfigItems = buildLoginDetectionConfigItems(detectionType)
}

function buildPlanPayload() {
  return {
    planName: planForm.planName?.trim() || '',
    pointId: planForm.pointSource === 'EXISTING' ? planForm.pointId : null,
    pointName: planForm.pointName?.trim() || '',
    startTime: planForm.startTime || '',
    endTime: planForm.endTime || '',
    samplerId: planForm.samplerId,
    samplerName: planForm.samplerName?.trim() || '',
    samplingType: planForm.samplingType || routineSamplingType,
    sampleType: planForm.sampleType || '',
    detectionTypeId: planForm.detectionTypeId,
    detectionTypeName: planForm.detectionTypeName?.trim() || '',
    detectionConfigItems: planDetectionConfigRows.value.map((item) => ({
      parameterId: item.parameterId,
      parameterName: item.parameterName,
      parameterCategory: item.parameterCategory,
      parameterCategoryDesc: item.parameterCategoryDesc,
      unit: item.unit,
      standardMin: item.standardMin,
      standardMax: item.standardMax,
      referenceStandard: item.referenceStandard,
      methodId: item.methodId,
      methodName: item.methodName,
      sampleVolume: item.sampleVolume
    })),
    samplingBasisList: parseSamplingBasisList(planForm.samplingBasisList),
    cycleType: planForm.cycleType || dailyCycleType,
    remark: planForm.remark?.trim() || ''
  }
}

async function createPlan() {
  await openPlanDialog()
}

async function submitPlanForm() {
  const payload = buildPlanPayload()
  if (!payload.planName || !payload.pointName || !payload.startTime || !payload.sampleType || !payload.cycleType) {
    ElMessage.warning('请完整填写采样计划信息')
    return
  }
  if (!payload.samplerId || !payload.samplerName) {
    ElMessage.warning('请选择采样人员')
    return
  }
  if (!payload.detectionTypeId || !payload.detectionTypeName) {
    ElMessage.warning('请选择检测套餐')
    return
  }
  if (!payload.samplingBasisList.length) {
    ElMessage.warning('请选择采样依据')
    return
  }
  if (!payload.detectionConfigItems.length || payload.detectionConfigItems.some((item) => !item.parameterId || !item.methodId)) {
    ElMessage.warning('请选择检测套餐对应的检测参数与检测方法')
    return
  }
  if (planForm.pointSource === 'EXISTING' && !payload.pointId) {
    ElMessage.warning('请选择监测点位')
    return
  }
  if (payload.cycleType !== 'ONCE' && !payload.endTime) {
    ElMessage.warning('周期计划请填写截止时间')
    return
  }

  submitting.value = true
  try {
    if (editingPlanId.value) {
      await updateSamplingPlanApi(editingPlanId.value, payload)
    } else {
      await createSamplingPlanApi(payload)
    }
    planDialogVisible.value = false
    ElMessage.success(editingPlanId.value ? '采样计划已更新' : '采样计划已创建')
    planQuery.pageNum = 1
    await Promise.all([loadPlans(), loadPlanStats()])
  } finally {
    submitting.value = false
  }
}

async function legacyCreatePlanDoNotUse() {
  return null
}

async function openDispatchDialog(row) {
  if (dispatchSubmitting.value) {
    return
  }
  await loadSamplers()
  resetDispatchForm()
  dispatchForm.planId = row.id
  dispatchForm.samplingTime = row.startTime || dayjs().format('YYYY-MM-DD HH:mm:ss')
  if (row.samplerId) {
    handleDispatchSamplerChange(row.samplerId)
  }
  dispatchDialogVisible.value = true
}

async function submitDispatchForm() {
  if (dispatchSubmitting.value) {
    return
  }
  if (!dispatchForm.planId || !dispatchForm.samplerId || !dispatchForm.samplerName) {
    ElMessage.warning('派发任务前必须指定采样员')
    return
  }
  dispatchSubmitting.value = true
  try {
    await dispatchSamplingPlanApi({
      planId: dispatchForm.planId,
      samplingTime: dispatchForm.samplingTime || dayjs().format('YYYY-MM-DD HH:mm:ss'),
      samplerId: dispatchForm.samplerId,
      samplerName: dispatchForm.samplerName
    })
    dispatchDialogVisible.value = false
    ElMessage.success('采样计划已派发，并已同步生成采样任务。')
    planQuery.pageNum = 1
    taskQuery.pageNum = 1
    await Promise.all([loadPlans(), loadTasks(), loadPlanStats(), loadTaskStats()])
  } finally {
    dispatchSubmitting.value = false
  }
}

async function legacyDispatchDoNotUse(row) {
  return row || null
}

async function pausePlan(row) {
  if (!beginRowAction('plan', 'pause', row?.id)) {
    return
  }
  try {
    await pauseSamplingPlanApi(row.id)
    ElMessage.success('采样计划已暂停。')
    await Promise.all([loadPlans(), loadPlanStats()])
  } finally {
    endRowAction('plan', 'pause', row?.id)
  }
}

async function resumePlan(row) {
  if (!beginRowAction('plan', 'resume', row?.id)) {
    return
  }
  try {
    await resumeSamplingPlanApi(row.id)
    ElMessage.success('采样计划已恢复。')
    await Promise.all([loadPlans(), loadPlanStats()])
  } finally {
    endRowAction('plan', 'resume', row?.id)
  }
}

async function loadDetectionProjects() {
  if (detectionProjectOptions.value.length && detectionParameterOptions.value.length && detectionMethodOptions.value.length) {
    return
  }
  const [typeResult, parameterResult, methodResult] = await Promise.all([
    fetchDetectionTypesApi({
      pageNum: 1,
      pageSize: 500,
      enabled: 1
    }),
    fetchDetectionParametersApi({
      pageNum: 1,
      pageSize: 500
    }),
    fetchDetectionMethodOptionsApi()
  ])
  detectionProjectOptions.value = typeResult.records || []
  detectionParameterOptions.value = parameterResult.records || []
  detectionMethodOptions.value = Array.isArray(methodResult) ? methodResult : []
}

async function loadFlowOptions() {
  if (reviewFlowOptions.value.length && publishFlowOptions.value.length) {
    return
  }
  const [reviewOptions, publishOptions] = await Promise.all([
    fetchFlowConfigOptionsApi({ flowType: FLOW_TYPE_REVIEW }),
    fetchFlowConfigOptionsApi({ flowType: FLOW_TYPE_PUBLISH })
  ])
  reviewFlowOptions.value = Array.isArray(reviewOptions) ? reviewOptions : []
  publishFlowOptions.value = Array.isArray(publishOptions) ? publishOptions : []
}

function findFlowOption(options, flowId) {
  return options.find((item) => String(item.id) === String(flowId))
}

function getDefaultFlowOption(options) {
  return options.find((item) => item.defaultFlag) || options[0] || null
}

function applyDefaultFlowSelections() {
  const reviewFlow = getDefaultFlowOption(reviewFlowOptions.value)
  const publishFlow = getDefaultFlowOption(publishFlowOptions.value)
  loginForm.reviewFlowId = reviewFlow?.id || null
  loginForm.reviewFlowName = reviewFlow?.flowName || ''
  loginForm.publishFlowId = publishFlow?.id || null
  loginForm.publishFlowName = publishFlow?.flowName || ''
}

function handleReviewFlowChange(flowId) {
  const flow = findFlowOption(reviewFlowOptions.value, flowId)
  loginForm.reviewFlowName = flow?.flowName || ''
}

function handlePublishFlowChange(flowId) {
  const flow = findFlowOption(publishFlowOptions.value, flowId)
  loginForm.publishFlowName = flow?.flowName || ''
}

function parseDetectionItemsText(value) {
  return String(value || '').trim()
}

function parseIdList(value) {
  return String(value || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function parseBindingJson(value) {
  const text = String(value || '').trim()
  if (!text) {
    return []
  }
  try {
    const list = JSON.parse(text)
    return Array.isArray(list) ? list : []
  } catch {
    return []
  }
}

function parsePhotoUrls(value) {
  if (Array.isArray(value)) {
    return value
      .map((item) => {
        if (typeof item === 'string') {
          return item.trim()
        }
        if (item && typeof item === 'object') {
          return String(item.path || item.filePath || item.url || '').trim()
        }
        return String(item || '').trim()
      })
      .filter(Boolean)
  }
  const text = String(value || '').trim()
  if (!text) {
    return []
  }
  if (text.startsWith('[')) {
    try {
      const list = JSON.parse(text)
      if (Array.isArray(list)) {
        return list.map((item) => String(item || '').trim()).filter(Boolean)
      }
    } catch {
      // Ignore malformed JSON and continue with separator parsing.
    }
  }
  return String(value || '')
    .split(/[,，\n\r;；]+/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function buildPhotoEntry(path) {
  return {
    path,
    localPreview: false,
    previewUrl: isDirectImageUrl(path) ? buildStoragePreviewUrl(path) : ''
  }
}

function buildLocalPhotoEntry(path, file) {
  return {
    path,
    localPreview: true,
    previewUrl: URL.createObjectURL(file)
  }
}

function buildStoragePreviewUrl(path) {
  const value = String(path || '').trim()
  if (!value) {
    return ''
  }
  if (
    /^https?:\/\//i.test(value)
    || value.startsWith('blob:')
    || value.startsWith('data:')
    || value.startsWith('/api/storage/file?path=')
    || value.startsWith('api/storage/file?path=')
  ) {
    return value
  }
  return `/api/storage/file?path=${encodeURIComponent(value)}`
}

function isDirectImageUrl(path) {
  const value = String(path || '').trim()
  return /^https?:\/\//i.test(value)
    || value.startsWith('blob:')
    || value.startsWith('data:')
    || value.startsWith('/api/storage/file?path=')
    || value.startsWith('api/storage/file?path=')
}

function formatStandardRange(min, max, unit) {
  const suffix = unit ? ` ${unit}` : ''
  if (min != null && max != null) {
    return `${min} ~ ${max}${suffix}`
  }
  if (min != null) {
    return `>= ${min}${suffix}`
  }
  if (max != null) {
    return `<= ${max}${suffix}`
  }
  return '-'
}

function formatTaskDetectionParameterSummary(task) {
  const rows = parseTaskDetectionConfigSnapshot(task?.detectionConfigSnapshot)
  if (!rows.length) {
    return task?.detectionTypeName || task?.detectionItems || '-'
  }
  return rows.map((item) => item.parameterName).filter(Boolean).join('、')
}

function getDetectionTypeById(typeId) {
  return detectionProjectOptions.value.find((item) => String(item.id) === String(typeId || '')) || null
}

function buildLoginDetectionConfigItems(detectionType) {
  if (!detectionType) {
    return []
  }
  const parameterIds = parseIdList(detectionType.parameterIds)
  const parameterMap = new Map(detectionParameterOptions.value.map((item) => [String(item.id), item]))
  const bindingMap = new Map()
  parseBindingJson(detectionType.parameterMethodBindings).forEach((item) => {
    const parameterId = String(item?.parameterId || '').trim()
    if (!parameterId) {
      return
    }
    const methodId = Array.isArray(item?.methodIds) ? item.methodIds[0] : null
    bindingMap.set(parameterId, methodId == null ? '' : String(methodId))
  })

  return parameterIds
    .map((parameterId) => {
      const parameter = parameterMap.get(String(parameterId))
      if (!parameter) {
        return null
      }
      const methodOptions = detectionMethodOptions.value
        .filter((item) => String(item.parameterId) === String(parameterId) && item.enabled === 1)
        .map((item) => ({
          id: String(item.id),
          methodName: item.methodName || `检测方法-${item.id}`,
          sampleVolume: item.sampleVolume || item.sample_volume || ''
        }))
      const defaultMethodId = bindingMap.get(parameterId) && methodOptions.some((item) => item.id === bindingMap.get(parameterId))
        ? bindingMap.get(parameterId)
        : (methodOptions[0]?.id || '')
      const currentMethod = methodOptions.find((item) => item.id === defaultMethodId) || methodOptions[0] || null
      return {
        parameterId: String(parameter.id),
        parameterName: parameter.parameterName || '',
        parameterCategory: normalizeParameterCategoryCode(parameter.parameterCategory || ''),
        parameterCategoryDesc: formatParameterCategory(parameter),
        unit: parameter.unit || '',
        standardMin: parameter.standardMin,
        standardMax: parameter.standardMax,
        referenceStandard: parameter.referenceStandard || '',
        methodId: currentMethod?.id || '',
        methodName: currentMethod?.methodName || '',
        sampleVolume: currentMethod?.sampleVolume || '',
        methodOptions
      }
    })
    .filter(Boolean)
}

function buildLoginConfigRowFromSnapshot(item) {
  const parameterId = String(item?.parameterId || '').trim()
  const parameter = detectionParameterOptions.value.find((option) => String(option.id) === parameterId)
  const methodId = String(item?.methodId || '').trim()
  let methodOptions = getDetectionConfigMethodOptionsByParameter(parameterId)
  if (methodId && !methodOptions.some((option) => option.id === methodId)) {
    methodOptions = [
      ...methodOptions,
      {
        id: methodId,
        methodName: item?.methodName || `检测方法-${methodId}`,
        sampleVolume: item?.sampleVolume || item?.sample_volume || ''
      }
    ]
  }
  return {
    parameterId,
    parameterName: item?.parameterName || parameter?.parameterName || '',
    parameterCategory: normalizeParameterCategoryCode(item?.parameterCategory || item?.parameter_category || parameter?.parameterCategory || item?.parameterCategoryDesc || item?.parameter_category_desc || ''),
    parameterCategoryDesc: formatParameterCategory(item?.parameterCategory || item?.parameter_category || parameter?.parameterCategory || item?.parameterCategoryDesc || item?.parameter_category_desc || ''),
    unit: item?.unit || parameter?.unit || '',
    standardMin: item?.standardMin ?? parameter?.standardMin ?? null,
    standardMax: item?.standardMax ?? parameter?.standardMax ?? null,
    referenceStandard: item?.referenceStandard || parameter?.referenceStandard || '',
    methodId,
    methodName: item?.methodName || methodOptions.find((option) => option.id === methodId)?.methodName || '',
    sampleVolume: item?.sampleVolume || item?.sample_volume || methodOptions.find((option) => option.id === methodId)?.sampleVolume || '',
    resultValue: item?.resultValue ?? item?.result_value ?? null,
    methodOptions
  }
}

function parseSampleDetectionConfigSnapshot(snapshot) {
  return parseBindingJson(snapshot)
    .map((item) => buildLoginConfigRowFromSnapshot(item))
    .filter((item) => item.parameterId || item.parameterName || item.methodId || item.methodName)
}

function parseTaskDetectionConfigSnapshot(snapshot) {
  return parseBindingJson(snapshot)
    .map((item) => buildLoginConfigRowFromSnapshot(item))
    .filter((item) => item.parameterId || item.parameterName || item.methodId || item.methodName)
}

function buildInstrumentModelLabel(binding) {
  const displayNames = String(binding?.instrumentDisplayNames || binding?.instrumentDisplayName || binding?.label || '').trim()
  if (displayNames) {
    return displayNames
  }
  const instrumentName = String(binding?.instrumentName || '').trim()
  const instrumentModel = String(binding?.instrumentModel || '').trim()
  const manufacturer = String(binding?.manufacturer || '').trim()
  if (!instrumentModel) {
    return ''
  }
  if (instrumentName) {
    return `${instrumentName}/${instrumentModel}`
  }
  return manufacturer ? `${instrumentModel} / ${manufacturer}` : instrumentModel
}

function resolveMethodInstrumentDisplayNames(methodId) {
  const key = String(methodId || '').trim()
  if (!key) {
    return ''
  }
  return detectionMethodInstrumentModelNameMap.value[key] || ''
}

async function ensureDetectionMethodInstrumentModelMap() {
  if (detectionMethodInstrumentModelLoaded.value) {
    return
  }
  const result = await fetchDetectionMethodInstrumentModelBindingsApi({
    pageNum: 1,
    pageSize: 1000
  })
  const records = Array.isArray(result?.records) ? result.records : []
  const nextMap = {}
  records.forEach((item) => {
    const methodId = String(item?.id || item?.methodId || '').trim()
    if (!methodId) {
      return
    }
    const instrumentDisplayNames = String(item?.instrumentDisplayNames || '').trim()
    if (instrumentDisplayNames) {
      nextMap[methodId] = instrumentDisplayNames
      return
    }
    const instrumentModelNames = String(item?.instrumentModelNames || '').trim()
    if (instrumentModelNames) {
      nextMap[methodId] = instrumentModelNames
      return
    }
    const bindings = Array.isArray(item?.instrumentModelBindings) ? item.instrumentModelBindings : []
    nextMap[methodId] = bindings
      .map((binding) => buildInstrumentModelLabel(binding))
      .filter(Boolean)
      .join('、')
  })
  detectionMethodInstrumentModelNameMap.value = nextMap
  detectionMethodInstrumentModelLoaded.value = true
}

const loginDetectionConfigRows = computed(() => loginForm.detectionConfigItems)
const planDetectionConfigRows = computed(() => planForm.detectionConfigItems)
const enabledDetectionParameterOptions = computed(() => (
  detectionParameterOptions.value
    .filter((item) => item.enabled === 1)
    .sort((left, right) => String(left.parameterName || '').localeCompare(String(right.parameterName || ''), 'zh-CN'))
))

function createEmptyDetectionConfigRow() {
  return {
    parameterId: '',
    parameterName: '',
    parameterCategory: '',
    parameterCategoryDesc: '',
    unit: '',
    standardMin: null,
    standardMax: null,
    referenceStandard: '',
    methodId: '',
    methodName: '',
    sampleVolume: '',
    methodOptions: []
  }
}

function getDetectionConfigMethodOptionsByParameter(parameterId) {
  if (!parameterId) {
    return []
  }
  return detectionMethodOptions.value
    .filter((item) => String(item.parameterId) === String(parameterId) && item.enabled === 1)
    .map((item) => ({
      id: String(item.id),
      methodName: item.methodName || `检测方法-${item.id}`,
      sampleVolume: item.sampleVolume || item.sample_volume || ''
    }))
}

function getPlanConfigParameterOptions(currentIndex) {
  const selectedParameterIds = new Set(
    planDetectionConfigRows.value
      .map((item, index) => index === currentIndex ? '' : String(item.parameterId || '').trim())
      .filter(Boolean)
  )
  return enabledDetectionParameterOptions.value.filter((item) => !selectedParameterIds.has(String(item.id)))
}

function appendPlanConfigRow() {
  planForm.detectionConfigItems.push(createEmptyDetectionConfigRow())
}

function removePlanConfigRow(index) {
  planForm.detectionConfigItems.splice(index, 1)
}

function handlePlanConfigParameterChange(row, parameterId) {
  applyDetectionConfigParameterChange(row, parameterId)
}

function handlePlanConfigMethodChange(row, methodId) {
  applyDetectionConfigMethodChange(row, methodId)
}

function applyDetectionConfigParameterChange(row, parameterId) {
  const parameter = detectionParameterOptions.value.find((item) => String(item.id) === String(parameterId || ''))
  row.parameterId = String(parameterId || '')
  row.parameterName = parameter?.parameterName || ''
  row.parameterCategory = normalizeParameterCategoryCode(parameter?.parameterCategory || '')
  row.parameterCategoryDesc = formatParameterCategory(parameter)
  row.unit = parameter?.unit || ''
  row.standardMin = parameter?.standardMin ?? null
  row.standardMax = parameter?.standardMax ?? null
  row.referenceStandard = parameter?.referenceStandard || ''
  row.methodOptions = getDetectionConfigMethodOptionsByParameter(parameterId)
  const nextMethod = row.methodOptions.find((item) => item.id === row.methodId) || row.methodOptions[0] || null
  row.methodId = nextMethod?.id || ''
  row.methodName = nextMethod?.methodName || ''
  row.sampleVolume = nextMethod?.sampleVolume || ''
}

async function startTask(row) {
  if (!beginRowAction('task', 'start', row?.id)) {
    return
  }
  try {
    await startSamplingTaskApi(row.id, {
      remark: '采样任务开始执行'
    })
    ElMessage.success('采样任务已开始执行。')
    await Promise.all([loadTasks(), loadPlans(), loadTaskStats(), loadPlanStats()])
  } finally {
    endRowAction('task', 'start', row?.id)
  }
}

async function abandonTask(row) {
  if (!beginRowAction('task', 'abandon', row?.id)) {
    return
  }
  try {
    await abandonSamplingTaskApi(row.id, {
      reason: '现场条件暂不满足采样要求',
      remark: '请确认现场情况后重新安排采样任务'
    })
    ElMessage.success('采样任务已废弃。')
    await Promise.all([loadTasks(), loadPlans(), loadTaskStats(), loadPlanStats()])
  } finally {
    endRowAction('task', 'abandon', row?.id)
  }
}

async function resumeTask(row) {
  if (!beginRowAction('task', 'resume', row?.id)) {
    return
  }
  try {
    await resumeSamplingTaskApi(row.id, { remark: '采样任务恢复为待处理状态' })
    ElMessage.success('采样任务已恢复。')
    await Promise.all([loadTasks(), loadPlans(), loadTaskStats(), loadPlanStats()])
  } finally {
    endRowAction('task', 'resume', row?.id)
  }
}

async function openTaskDetailDialog(row) {
  if (!row?.id) {
    return
  }
  await ensureDetectionMethodInstrumentModelMap()
  const detail = await fetchSamplingTaskDetailApi(row.id)
  taskDetail.value = detail || row
  await resolveTaskDetailPhotos(extractTaskPhotoUrls(taskDetail.value))
  taskDetailDialogVisible.value = true
}

function extractTaskPhotoUrls(task) {
  return parsePhotoUrls(
    task?.photoUrls
    || task?.photo_urls
    || task?.photoUrl
    || task?.photo_url
    || ''
  )
}

function resetTaskCompleteForm() {
  clearTaskCompletePhotoPreviewUrls()
  taskCompletePreview.value = null
  taskCompletePhotoList.value = []
  taskCompleteForm.taskId = null
  taskCompleteForm.onsiteMetrics = ''
  taskCompleteForm.weather = ''
  taskCompleteForm.temperature = ''
  taskCompleteForm.sampleTotalVolume = ''
  taskCompleteForm.sampleBottleCount = null
  taskCompleteForm.photoUrls = ''
  taskCompleteForm.remark = ''
  taskCompleteForm.address = ''
  taskCompleteForm.latitude = ''
  taskCompleteForm.longitude = ''
}

function resetTaskCompleteDialog() {
  resetTaskCompleteForm()
}

function openMapSelector() {
  mapSelectorValue.address = taskCompleteForm.address
  mapSelectorValue.latitude = taskCompleteForm.latitude
  mapSelectorValue.longitude = taskCompleteForm.longitude
  mapSelectorVisible.value = true
  initMap()
}

function confirmMapSelection() {
  taskCompleteForm.address = mapSelectorValue.address
  taskCompleteForm.latitude = mapSelectorValue.latitude
  taskCompleteForm.longitude = mapSelectorValue.longitude
  mapSelectorVisible.value = false
}

let map = null
let mapMarker = null

function initMap() {
  if (map) {
    map.destroy()
    map = null
    mapMarker = null
  }
  if (!window.AMap) {
    loadAMapScript()
  } else {
    createMap()
  }
}

function loadAMapScript() {
  const script = document.createElement('script')
  script.src = 'https://webapi.amap.com/maps?v=2.0&key=da985cf3edbb156ab16aa5de2e8e954a&plugin=AMap.Geocoder'
  script.onload = createMap
  document.head.appendChild(script)
}

function createMap() {
  const mapContainer = document.querySelector('.map-selector__container')
  if (!mapContainer) return

  map = new window.AMap.Map(mapContainer, {
    center: mapSelectorValue.longitude && mapSelectorValue.latitude
      ? [Number(mapSelectorValue.longitude), Number(mapSelectorValue.latitude)]
      : [116.397428, 39.90923],
    zoom: 15
  })

  map.on('click', async (e) => {
    const lng = e.lnglat.getLng()
    const lat = e.lnglat.getLat()

    const geocoder = new window.AMap.Geocoder()
    geocoder.getAddress([lng, lat], (status, res) => {
      mapSelectorValue.address = res.regeocode?.formattedAddress || '未知地址'
      mapSelectorValue.latitude = String(lat)
      mapSelectorValue.longitude = String(lng)

      if (mapMarker) {
        mapMarker.setPosition([lng, lat])
      } else {
        mapMarker = new window.AMap.Marker({
          position: [lng, lat],
          map: map
        })
      }
    })
  })

  if (mapSelectorValue.longitude && mapSelectorValue.latitude) {
    mapMarker = new window.AMap.Marker({
      position: [Number(mapSelectorValue.longitude), Number(mapSelectorValue.latitude)],
      map: map
    })
  }
}

function clearTaskCompletePhotoPreviewUrls() {
  taskCompletePhotoList.value.forEach((item) => {
    if (item.previewUrl?.startsWith('blob:')) {
      URL.revokeObjectURL(item.previewUrl)
    }
  })
}

function resetTaskDetailDialog() {
  taskDetail.value = null
  taskDetailPhotoList.value.forEach((item) => {
    if (item.previewUrl?.startsWith('blob:')) {
      URL.revokeObjectURL(item.previewUrl)
    }
  })
  taskDetailPhotoList.value = []
}

async function resolveTaskDetailPhotos(photoPaths) {
  taskDetailPhotoList.value.forEach((item) => {
    if (item.previewUrl?.startsWith('blob:')) {
      URL.revokeObjectURL(item.previewUrl)
    }
  })
  taskDetailPhotoList.value = photoPaths.map(buildPhotoEntry)
}

async function handleTaskCompletePhotoChange(file) {
  const rawFile = file.raw
  if (!rawFile) {
    return
  }
  if (!rawFile.type?.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (rawFile.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 5MB')
    return
  }
  taskCompletePhotoUploading.value = true
  try {
    const result = await uploadStorageFileApi(rawFile)
    const publicFileUrl = String(
      result?.fullUrl
      || result?.data?.fullUrl
      || result?.filePath
      || result?.data?.filePath
      || ''
    ).trim()
    if (!publicFileUrl) {
      ElMessage.error('图片上传失败')
      return
    }
    taskCompleteForm.photoUrls = [...parsePhotoUrls(taskCompleteForm.photoUrls), publicFileUrl].join(',')
    taskCompletePhotoList.value = [
      ...taskCompletePhotoList.value,
      buildLocalPhotoEntry(publicFileUrl, rawFile)
    ]
  } finally {
    taskCompletePhotoUploading.value = false
  }
}

function removeTaskCompletePhoto(index) {
  const nextPhotoUrls = parsePhotoUrls(taskCompleteForm.photoUrls)
  nextPhotoUrls.splice(index, 1)
  taskCompleteForm.photoUrls = nextPhotoUrls.join(',')
  const removed = taskCompletePhotoList.value.splice(index, 1)[0]
  if (removed?.previewUrl?.startsWith('blob:')) {
    URL.revokeObjectURL(removed.previewUrl)
  }
}

async function openTaskCompleteDialog(row) {
  if (!row?.id || !isTaskCompleteEntryEnabled(row)) {
    return
  }
  if (!beginRowAction('task', 'complete', row.id)) {
    return
  }
  try {
    await ensureDetectionMethodInstrumentModelMap()
    const detail = await fetchSamplingTaskDetailApi(row.id)
    const task = detail || row
    taskCompletePreview.value = task
    clearTaskCompletePhotoPreviewUrls()
    taskCompletePhotoList.value = extractTaskPhotoUrls(task).map(buildPhotoEntry)
    taskCompleteForm.taskId = task.id
    taskCompleteForm.onsiteMetrics = task.onsiteMetrics || task.onsite_metrics || ''
    taskCompleteForm.weather = task.weather || ''
    taskCompleteForm.temperature = task.temperature || ''
    taskCompleteForm.sampleTotalVolume = extractSampleVolumeNumber(task.sampleTotalVolume || task.sample_total_volume || '')
    taskCompleteForm.sampleBottleCount = task.sampleBottleCount ?? task.sample_bottle_count ?? null
    taskCompleteForm.photoUrls = extractTaskPhotoUrls(task).join(',')
    taskCompleteForm.remark = task.remark || ''
    taskCompleteForm.address = task.address || ''
    taskCompleteForm.latitude = task.latitude || task.x_coordinate || ''
    taskCompleteForm.longitude = task.longitude || task.y_coordinate || ''
    taskCompleteDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error?.message || '获取采样录入信息失败')
  } finally {
    endRowAction('task', 'complete', row.id)
  }
}

async function submitTaskCompleteForm() {
  if (!taskCompleteForm.taskId) {
    ElMessage.warning('请选择要录入的采样任务')
    return
  }
  taskCompleteSubmitting.value = true
  try {
    const task = taskCompletePreview.value
    if (task?.taskStatus === pendingTaskStatus) {
      await startSamplingTaskApi(task.id, {
        remark: 'PC端采样录入时自动开始任务'
      })
    }
    await completeSamplingTaskApi({
      taskId: taskCompleteForm.taskId,
      onsiteMetrics: taskCompleteForm.onsiteMetrics,
      weather: taskCompleteForm.weather,
      temperature: taskCompleteForm.temperature,
      sampleTotalVolume: buildSampleVolumePayload(taskCompleteForm.sampleTotalVolume),
      sampleBottleCount: taskCompleteForm.sampleBottleCount == null ? '' : String(taskCompleteForm.sampleBottleCount),
      detectionConfigItems: taskCompleteDetectionConfigRows.value.map((item) => ({
        parameterId: item.parameterId,
        parameterName: item.parameterName,
        parameterCategory: item.parameterCategory,
        parameterCategoryDesc: item.parameterCategoryDesc,
        unit: item.unit,
        standardMin: item.standardMin,
        standardMax: item.standardMax,
        referenceStandard: item.referenceStandard,
        methodId: item.methodId,
        methodName: item.methodName,
        sampleVolume: item.sampleVolume,
        resultValue: isSamplingDetectionResultEditable(item) ? item.resultValue : null
      })),
      photoUrls: taskCompleteForm.photoUrls,
      remark: taskCompleteForm.remark,
      address: taskCompleteForm.address,
      latitude: taskCompleteForm.latitude,
      longitude: taskCompleteForm.longitude
    })
    taskCompleteDialogVisible.value = false
    ElMessage.success('采样录入已保存。')
    await Promise.all([loadTasks(), loadPlans(), loadTaskStats(), loadPlanStats()])
  } finally {
    taskCompleteSubmitting.value = false
  }
}

async function legacyLoginSampleDoNotUse() {
  return null
}

function applyTaskToLoginForm(task) {
  if (!task) {
    return
  }
  loginForm.taskId = task.id
  loginForm.sampleNo = task.sampleNo || ''
  loginForm.pointId = task.pointId || null
  loginForm.pointName = task.pointName || ''
  loginForm.sampleType = task.sampleType || ''
  loginForm.qualityControlType = ''
  loginForm.detectionItems = parseDetectionItemsText(task.detectionTypeName || task.detectionItems)
  loginForm.detectionTypeId = task.detectionTypeId || null
  loginForm.detectionTypeName = task.detectionTypeName || task.detectionItems || ''
  loginForm.detectionConfigItems = parseSampleDetectionConfigSnapshot(task.detectionConfigSnapshot)
  if (!loginForm.detectionConfigItems.length && loginForm.detectionTypeId) {
    loginForm.detectionConfigItems = buildLoginDetectionConfigItems(getDetectionTypeById(loginForm.detectionTypeId))
  }
  loginForm.samplingTime = task.samplingTime || dayjs().format('YYYY-MM-DD HH:mm:ss')
  loginForm.samplerId = task.samplerId || null
  loginForm.samplerName = task.samplerName || ''
  loginForm.weather = ''
  loginForm.storageCondition = ''
  loginForm.remark = task.remark || ''
}

function resetLoginForm() {
  loginDialogMode.value = 'create'
  loginPreviewTaskLabel.value = ''
  loginForm.taskId = null
  loginForm.sampleNo = ''
  loginForm.pointId = null
  loginForm.pointName = ''
  loginForm.sampleType = ''
  loginForm.qualityControlType = ''
  loginForm.detectionItems = ''
  loginForm.detectionTypeId = null
  loginForm.detectionTypeName = ''
  loginForm.detectionConfigItems = []
  loginForm.reviewFlowId = null
  loginForm.reviewFlowName = ''
  loginForm.publishFlowId = null
  loginForm.publishFlowName = ''
  loginForm.samplingTime = ''
  loginForm.samplerId = null
  loginForm.samplerName = ''
  loginForm.weather = ''
  loginForm.storageCondition = ''
  loginForm.remark = ''
}

async function openLoginDialog(task = null) {
  await Promise.all([loadSamples(), loadLoggableTasks(), loadTaskStats()])
  const loggableTask = task || firstLoggableTask.value || pendingLoggableTasks.value[0]
  if (!loggableTask) {
    ElMessage.warning('没有可登录的任务，请先进行采样任务完成录入')
    return
  }
  await Promise.all([loadDetectionProjects(), loadFlowOptions()])
  resetLoginForm()
  applyDefaultFlowSelections()
  loginDialogMode.value = 'create'
  applyTaskToLoginForm(loggableTask)
  loginDialogVisible.value = true
}

function buildSampleTaskPreviewLabel(sample) {
  const sampleNo = sample?.sampleNo || '未生成样品编号'
  const pointName = sample?.pointName || '未命名点位'
  const samplerName = sample?.samplerName || '未指定采样员'
  if (sample?.taskId) {
    return `任务ID-${sample.taskId} / ${sampleNo} / ${pointName} / ${samplerName}`
  }
  return `无任务直登 / ${sampleNo} / ${pointName} / ${samplerName}`
}

function applySampleToLoginForm(sample) {
  if (!sample) {
    return
  }
  loginForm.taskId = sample.taskId || null
  loginPreviewTaskLabel.value = buildSampleTaskPreviewLabel(sample)
  loginForm.sampleNo = sample.sampleNo || ''
  loginForm.pointId = sample.pointId || null
  loginForm.pointName = sample.pointName || ''
  loginForm.sampleType = sample.sampleType || ''
  loginForm.qualityControlType = sample.qualityControlType || ''
  loginForm.detectionItems = parseDetectionItemsText(sample.detectionItems)
  loginForm.detectionTypeId = sample.detectionTypeId || null
  loginForm.detectionTypeName = sample.detectionTypeName || sample.detectionItems || ''
  loginForm.detectionConfigItems = parseSampleDetectionConfigSnapshot(sample.detectionConfigSnapshot)
  if (!loginForm.detectionConfigItems.length && loginForm.detectionTypeId) {
    loginForm.detectionConfigItems = buildLoginDetectionConfigItems(getDetectionTypeById(loginForm.detectionTypeId))
  }
  loginForm.reviewFlowId = sample.reviewFlowId || null
  loginForm.reviewFlowName = sample.reviewFlowName || ''
  loginForm.publishFlowId = sample.publishFlowId || null
  loginForm.publishFlowName = sample.publishFlowName || ''
  loginForm.samplingTime = sample.samplingTime || ''
  loginForm.samplerId = sample.samplerId || null
  loginForm.samplerName = sample.samplerName || ''
  loginForm.weather = sample.weather || ''
  loginForm.storageCondition = sample.storageCondition || ''
  loginForm.remark = sample.remark || ''
}

async function openSampleDetailDialog(sample) {
  await Promise.all([loadDetectionProjects(), loadFlowOptions()])
  resetLoginForm()
  loginDialogMode.value = 'view'
  applySampleToLoginForm(sample)
  loginDialogVisible.value = true
}

function handleLoginTaskChange(taskId) {
  const task = pendingLoggableTasks.value.find((item) => item.id === taskId)
  applyTaskToLoginForm(task)
}

function applyDetectionConfigMethodChange(row, methodId) {
  const method = (row.methodOptions || []).find((item) => item.id === String(methodId || ''))
  row.methodId = String(methodId || '')
  row.methodName = method?.methodName || ''
  row.sampleVolume = method?.sampleVolume || ''
}

function formatPendingTaskLabel(task) {
  const pointName = task?.pointName || '未命名点位'
  const samplerName = task?.samplerName || '未指定采样员'
  const taskNo = task?.taskNo || '未生成任务编号'
  const sampleNo = task?.sampleNo || '任务生成时自动生成样品编号'
  return `${taskNo} / ${sampleNo} / ${pointName} / ${samplerName}`
}

async function submitSampleLogin() {
  if (!loginForm.taskId) {
    ElMessage.warning('没有可登录的任务，请先进行采样任务完成录入')
    return
  }
  if (!loginForm.pointId || !loginForm.pointName || !loginForm.sampleType || !loginForm.detectionTypeId || !loginForm.detectionItems || !loginForm.samplingTime) {
    ElMessage.warning('请完整填写样品登录信息')
    return
  }
  if (!loginForm.reviewFlowId || !loginForm.publishFlowId) {
    ElMessage.warning('请选择审核流程和发布流程')
    return
  }
  if (!loginDetectionConfigRows.value.length || loginDetectionConfigRows.value.some((item) => !item.methodId)) {
    ElMessage.warning('请选择检测套餐对应的检测参数与检测方法')
    return
  }

  submitting.value = true
  try {
    const sample = await loginSampleApi({
      ...loginForm,
      detectionItems: parseDetectionItemsText(loginForm.detectionItems),
      detectionTypeId: loginForm.detectionTypeId,
      detectionTypeName: loginForm.detectionTypeName,
      detectionConfigItems: loginDetectionConfigRows.value.map((item) => ({
        parameterId: item.parameterId,
        parameterName: item.parameterName,
        parameterCategory: item.parameterCategory,
        parameterCategoryDesc: item.parameterCategoryDesc,
        unit: item.unit,
        standardMin: item.standardMin,
        standardMax: item.standardMax,
        referenceStandard: item.referenceStandard,
        methodId: item.methodId,
        methodName: item.methodName,
        sampleVolume: item.sampleVolume,
        resultValue: item.resultValue
      }))
    })
    loginDialogVisible.value = false
    ElMessage.success(`样品登录完成，样品编号：${sample?.sampleNo || '-'}`)
    sampleQuery.pageNum = 1
    await Promise.all([loadTasks(), loadSamples(), loadTaskStats(), loadSampleStats()])
    await loadLoggableTasks()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  syncRouteState()
  await Promise.all([loadCurrentSceneData(), loadSamplingDictOptions()])
})

watch(() => route.fullPath, () => {
  syncRouteState()
  loadCurrentSceneData()
})
</script>

<style scoped>
.sampling-page {
  gap: 12px;
}

.sampling-page--plan {
  height: 100%;
  min-height: 0;
}

.page-hero,
.scene-grid {
  display: grid;
  gap: 12px;
}

.page-hero {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
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
  margin: 8px 0 0;
  color: var(--text-sub);
  font-size: 14px;
  line-height: 1.6;
}

.action-row {
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
  justify-content: center;
  white-space: nowrap;
}

:deep(.el-table td.task-action-cell .cell) {
  padding-left: 8px;
  padding-right: 8px;
}

.task-action-row {
  gap: 6px;
}

.task-action-row :deep(.el-button) {
  min-width: 60px;
  padding: 0 12px;
}

.plan-sampler {
  color: var(--text-main);
}

.plan-sampler.is-empty {
  color: #d14343;
  font-weight: 600;
}

.task-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
  margin-bottom: 16px;
}

.task-detail-grid div,
.task-detail-block {
  border: 1px solid var(--line-soft);
  border-radius: 12px;
  padding: 12px 14px;
  background: var(--bg-panel-soft);
}

.task-detail-grid span,
.task-detail-block span {
  display: block;
  margin-bottom: 6px;
  color: var(--text-sub);
  font-size: 12px;
}

.task-detail-grid strong,
.task-detail-block p {
  margin: 0;
  color: var(--text-main);
  line-height: 1.6;
}

.task-detail-block + .task-detail-block {
  margin-top: 12px;
}

.task-detail-photo-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 10px;
}

.task-detail-photo {
  width: 110px;
  height: 110px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--line-soft);
  background: var(--bg-panel-soft);
  cursor: pointer;
}

.sampling-photo-uploader {
  display: grid;
  gap: 10px;
}

.sampling-photo-uploader__tip {
  margin: 0;
  color: var(--text-light);
  font-size: 12px;
}

.sampling-photo-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
}

.sampling-photo-card {
  display: grid;
  gap: 6px;
  justify-items: center;
}

.sampling-photo-card__image,
.sampling-photo-card__placeholder {
  display: block;
  width: 120px;
  height: 120px;
  border-radius: 12px;
  border: 1px solid var(--line-soft);
  background: var(--bg-panel-soft);
  overflow: hidden;
}

.sampling-photo-card__image {
  cursor: pointer;
}

.sampling-photo-card__image :deep(img),
.task-detail-photo :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.sampling-photo-card__image :deep(.el-image__inner),
.task-detail-photo :deep(.el-image__inner) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.sampling-photo-card__placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-light);
  font-size: 12px;
}

.plan-stats {
  margin-bottom: 10px;
}

.plan-panel-warning {
  margin-bottom: 10px;
  color: #9a6700;
}

.sampling-plan-section {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.plan-table-card {
  min-width: 0;
}

.plan-table-card--fullscreen {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.plan-table-card__body {
  min-width: 0;
}

.plan-table-card--fullscreen .plan-table-card__body {
  flex: 1 1 auto;
  min-height: 0;
}

.plan-table-card--fullscreen .plan-table-card__body :deep(.el-table) {
  height: 100%;
}

.scene-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.scene-copy {
  display: grid;
  gap: 8px;
  color: var(--text-sub);
  font-size: 14px;
  line-height: 1.7;
}

:deep(.dispatch-plan-dialog .el-dialog__body) {
  padding-bottom: 0;
}

:deep(.dispatch-plan-dialog .el-dialog__footer) {
  padding-top: 8px;
  padding-bottom: 16px;
}

:deep(.el-dialog.dispatch-plan-dialog),
:deep(.dispatch-plan-dialog) {
  min-height: auto;
}

:deep(.sampling-form-dialog .el-dialog__body) {
  max-height: calc(100vh - 160px);
  overflow-y: auto;
}

:deep(.sample-login-dialog) {
  width: min(1180px, calc(100vw - 40px));
}

:deep(.sampling-plan-form-dialog) {
  width: min(1080px, calc(100vw - 40px));
}

:deep(.sample-login-dialog .el-dialog__body) {
  max-height: calc(100vh - 150px);
  overflow-y: auto;
  padding-top: 18px;
}

.login-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.login-form-span-2 {
  grid-column: 1 / -1;
}

.login-form-half-row {
  width: calc(50% - 8px);
}

.login-config-panel {
  display: grid;
  gap: 10px;
  width: 100%;
  padding: 14px;
  border: 1px solid var(--line-soft);
  border-radius: 16px;
  background: linear-gradient(180deg, color-mix(in srgb, var(--brand) 4%, #ffffff 96%) 0%, #ffffff 100%);
}

.login-config-panel__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.login-config-panel__note {
  color: var(--text-light);
  font-size: 12px;
  line-height: 1.6;
}

.binding-editor__chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--brand) 16%, #ffffff 84%);
  background: color-mix(in srgb, var(--brand) 7%, #ffffff 93%);
  color: var(--text-sub);
  font-size: 13px;
}

.binding-editor__chip strong {
  color: var(--brand);
  font-size: 15px;
}

.login-config-table {
  width: 100%;
}

:deep(.sample-login-dialog .login-config-table .el-table__inner-wrapper) {
  min-height: 0;
}

.empty-block {
  padding: 16px 14px;
  border: 1px dashed var(--line-strong);
  border-radius: 14px;
  background: color-mix(in srgb, var(--brand) 3%, #ffffff 97%);
  color: var(--text-light);
  font-size: 13px;
  text-align: center;
}

.plan-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.plan-form-span-2 {
  grid-column: 1 / -1;
}

.plan-form-new-row {
  grid-column: 1 / span 1;
}

.scene-copy p {
  margin: 0;
}

.quick-links {
  display: grid;
  gap: 10px;
}

.quick-link {
  display: grid;
  gap: 4px;
  padding: 12px;
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
  :deep(.sample-login-dialog) {
    width: calc(100vw - 20px);
  }

  :deep(.sample-login-dialog .el-dialog__body) {
    max-height: calc(100vh - 120px);
  }

  .page-hero,
  .scene-grid {
    grid-template-columns: 1fr;
  }

  .hero-tags {
    justify-content: flex-start;
  }

  .login-form-grid {
    grid-template-columns: 1fr;
  }

  .login-form-half-row {
    width: 100%;
  }

  .plan-form-grid {
    grid-template-columns: 1fr;
  }
}

.coordinate-selector {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.coordinate-fields {
  display: flex;
  gap: 12px;
}

.coordinate-field {
  flex: 1;
}

:deep(.task-detail-dialog .el-dialog__body) {
  max-height: 70vh;
  overflow-y: auto;
}

:deep(.task-detail-dialog) {
  width: min(960px, calc(100vw - 40px));
}

.task-detail-config-table {
  margin-top: 8px;
}

.map-selector {
  display: flex;
  flex-direction: column;
  height: 400px;
}

.map-selector__container {
  flex: 1;
  width: 100%;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
}

.map-selector__info {
  padding: 12px;
  background: #f5f5f5;
  border-top: 1px solid #e0e0e0;
  font-size: 14px;
}
</style>
