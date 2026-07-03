﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿<template>
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
                  <el-select v-model="taskQuery.sampleRegisterStatus" clearable placeholder="请选择任务状态">
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
                      :label="getSamplerDisplayName(item)"
                      :value="getSamplerOptionId(item)"
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
                <label class="toolbar-field">
                  <span>样品来源</span>
                  <el-select v-model="sampleQuery.sampleSourceMethod" clearable placeholder="请选择样品来源">
                    <el-option
                      v-for="option in sampleSourceMethodOptions"
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
          <el-table-column prop="finishedTime" label="采样时间" width="170" />
          <el-table-column prop="weather" label="天气" width="110" />
          <el-table-column prop="temperature" label="温度" width="110" />
          <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
          <el-table-column
            label="操作"
            :width="baseScene.allowTaskActions ? 220 : 140"
            fixed="right"
            header-cell-class-name="cell-center"
            class-name="cell-center task-action-cell"
          >
            <template #default="{ row }">
              <div class="action-row task-action-row">
                <el-button
                  v-if="baseScene.allowTaskActions"
                  type="primary"
                  size="small"
                  :disabled="isTaskRegistered(row)"
                  @click="openLoginDialog(row)"
                >
                  样品登录
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
          <el-table-column label="样品来源" width="120" header-cell-class-name="cell-center" class-name="cell-center">
            <template #default="{ row }">
              <span>{{ getEnumLabel(sampleSourceMethodLabelMap, row.sampleSourceMethod) }}</span>
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
                      :label="getSamplerDisplayName(item)"
                      :value="getSamplerOptionId(item)"
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
            <el-table-column prop="samplerName" label="采样人员" min-width="140" show-overflow-tooltip />
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
          <el-form-item label="所属机构" required>
            <el-select
              v-model="planForm.orgId"
              filterable
              style="width: 100%"
              placeholder="请选择所属机构"
              @change="handlePlanOrgChange"
            >
              <el-option
                v-for="option in orgOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="planForm.pointSource === 'EXISTING'" label="监测点位" required>
            <el-select
              v-model="planForm.pointId"
              style="width: 100%"
              :placeholder="planForm.orgId ? '请选择已创建的监测点位' : '请先选择所属机构'"
              :loading="monitoringPointLoading"
              :disabled="!planForm.orgId"
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
              @input="handlePlanPointNameChange"
            />
          </el-form-item>
          <el-form-item label="点位坐标" required>
            <div class="location-picker">
              <el-input :model-value="formatCoordinateText(planForm)" readonly placeholder="请从地图选择点位" />
              <el-button v-if="planForm.pointSource === 'CUSTOM'" @click="openPlanMapSelector">
                {{ planForm.latitude && planForm.longitude ? '重新选点' : '地图选点' }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item class="plan-form-span-2" label="地图位置">
            <el-input v-model="planForm.address" readonly placeholder="地图选点后自动回填" />
          </el-form-item>
          <el-form-item label="样品类型" required>
            <el-select v-model="planForm.sampleType" style="width: 100%" :disabled="planForm.pointSource === 'EXISTING'">
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
              v-model="planForm.samplerIds"
              multiple
              collapse-tags
              collapse-tags-tooltip
              clearable
              filterable
              style="width: 100%"
              placeholder="请选择采样员，可多选"
              :loading="samplerLoading"
              @visible-change="handleSamplerDropdownVisible"
              @change="handlePlanSamplerChange"
            >
              <el-option
                v-for="item in samplerOptions"
                :key="item.id"
                :label="getSamplerDisplayName(item)"
                :value="getSamplerOptionId(item)"
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
          <el-form-item label="开始时间" required>
            <div class="plan-hour-picker" data-plan-hour-picker>
              <button
                ref="startPlanHourTriggerRef"
                type="button"
                :class="['plan-hour-picker__trigger', { 'is-placeholder': !getPlanDateTimeText('start') }]"
                @click="togglePlanHourPanel('start')"
              >
                {{ getPlanDateTimeText('start') || '请选择开始时间' }}
              </button>
            </div>
          </el-form-item>
          <el-form-item label="截止时间">
            <div class="plan-hour-picker" data-plan-hour-picker>
              <button
                ref="endPlanHourTriggerRef"
                type="button"
                :class="['plan-hour-picker__trigger', { 'is-placeholder': !getPlanDateTimeText('end') }]"
                @click="togglePlanHourPanel('end')"
              >
                {{ getPlanDateTimeText('end') || '请选择截止时间' }}
              </button>
            </div>
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

    <Teleport to="body">
      <div
        v-if="activePlanHourPanel"
        class="plan-hour-picker__panel plan-hour-picker__panel--floating"
        data-plan-hour-picker
        :style="planHourPanelStyle"
      >
        <div class="plan-hour-picker__body">
          <el-date-picker-panel
            :model-value="getPlanDatePart(activePlanHourPanel)"
            type="date"
            value-format="YYYY-MM-DD"
            :show-footer="false"
            @update:model-value="handlePlanDateChange(activePlanHourPanel, $event)"
          />
          <div class="plan-hour-picker__hours">
            <span>选择小时</span>
            <button
              v-for="item in hourOptions"
              :key="`${activePlanHourPanel}-hour-${item}`"
              type="button"
              :class="['plan-hour-option', { 'is-active': getPlanHourPart(activePlanHourPanel) === item }]"
              @click="handlePlanHourChange(activePlanHourPanel, item)"
            >
              {{ item }}:00
            </button>
          </div>
        </div>
      </div>
    </Teleport>

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
          <el-form-item class="plan-form-span-2" label="现场位置">
            <div class="location-picker">
              <el-input :model-value="formatLocationText(taskCompleteForm)" readonly placeholder="可回显计划坐标，也可重新选择现场位置" />
              <el-button @click="openMapSelector">
                {{ taskCompleteForm.latitude && taskCompleteForm.longitude ? '调整位置' : '选择位置' }}
              </el-button>
            </div>
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
            v-model="dispatchForm.samplerIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            clearable
            filterable
            style="width: 100%"
            placeholder="请选择采样员，可多选"
            :loading="samplerLoading"
            @visible-change="handleSamplerDropdownVisible"
            @change="handleDispatchSamplerChange"
          >
            <el-option
              v-for="item in samplerOptions"
              :key="item.id"
              :label="getSamplerDisplayName(item)"
              :value="getSamplerOptionId(item)"
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
              placeholder="请选择未采样且未登录的任务"
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
          <el-form-item label="样品来源" :required="!isLoginReadonly">
            <div class="sample-source-field">
              <el-input
                v-if="isLoginReadonly"
                :model-value="getEnumLabel(sampleSourceMethodLabelMap, loginForm.sampleSourceMethod)"
                readonly
              />
              <el-select
                v-else
                v-model="loginForm.sampleSourceMethod"
                placeholder="请选择样品来源"
                style="width: 100%"
              >
                <el-option
                  v-for="option in sampleSourceMethodOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </div>
          </el-form-item>
          <el-form-item label="采样人员" :required="!isLoginReadonly">
            <el-input v-model="loginForm.samplerName" readonly />
          </el-form-item>
          <el-form-item class="login-form-span-2 login-form-half-row" label="检测套餐" :required="!isLoginReadonly">
            <el-input
              v-if="isLoginReadonly"
              :model-value="loginForm.detectionTypeName || loginForm.detectionItems || '-'"
              readonly
            />
            <el-select
              v-else
              v-model="loginForm.detectionTypeId"
              filterable
              placeholder="请选择检测套餐"
              style="width: 100%"
              @change="handleLoginDetectionTypeChange"
            >
              <el-option
                v-for="item in loginDetectionProjectOptions"
                :key="item.id"
                :label="formatLoginDetectionTypeLabel(item)"
                :value="item.id"
              />
            </el-select>
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
                <el-button
                  v-if="!isLoginReadonly"
                  type="primary"
                  size="small"
                  @click="openAddParamDialog"
                >
                  添加参数
                </el-button>
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
                <el-table-column label="标准范围" min-width="120">
                  <template #default="{ row }">
                    {{ formatStandardRange(row.standardMin, row.standardMax, null, row.optionValues) }}
                  </template>
                </el-table-column>
                <el-table-column prop="unit" label="单位" width="72">
                  <template #default="{ row }">{{ row.unit || '-' }}</template>
                </el-table-column>
                <el-table-column prop="referenceStandard" label="检测标准" min-width="140" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.referenceStandard || '-' }}</template>
                </el-table-column>
                <el-table-column label="检测方法" min-width="160">
                  <template #default="{ row }">
                    <el-select
                      v-if="!isLoginReadonly && row.methodOptions && row.methodOptions.length"
                      v-model="row.methodId"
                      size="small"
                      style="width: 100%"
                      @change="(val) => handleLoginMethodChange(row, val)"
                    >
                      <el-option
                        v-for="opt in row.methodOptions"
                        :key="opt.id"
                        :label="opt.methodName"
                        :value="opt.id"
                      />
                    </el-select>
                    <span v-else>{{ row.methodName || '-' }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="sampleVolume" label="取样体积" min-width="90" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.sampleVolume || '-' }}</template>
                </el-table-column>
                <el-table-column v-if="!isLoginReadonly" label="操作" width="72" fixed="right">
                  <template #default="{ row, $index }">
                    <el-button
                      type="danger"
                      link
                      size="small"
                      @click="removeLoginConfigRow($index)"
                    >
                      删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div v-if="!loginDetectionConfigRows.length" class="empty-block">
                请选择检测套餐后确认检测参数与检测方法明细。
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
      v-model="addParamDialogVisible"
      title="添加检测参数"
      width="520px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div style="max-height:380px;overflow-y:auto;">
        <div v-if="!addParamDialogList.length" style="padding:30px;text-align:center;color:#999;">
          暂无可添加的检测参数
        </div>
        <label
          v-for="item in addParamDialogList"
          :key="item.id"
          :style="{ display:'flex',alignItems:'center',padding:'8px 0',cursor:item._existing?'not-allowed':'pointer',opacity:item._existing?0.55:1,gap:'8px' }"
        >
          <input
            type="checkbox"
            :checked="!!addParamDialogChecked[item.id]"
            :disabled="item._existing"
            style="width:15px;height:15px;cursor:inherit;"
            @change="addParamDialogChecked[item.id] = $event.target.checked"
          />
          <span>{{ item.parameterName }}</span>
          <span v-if="item.unit" style="color:#999;font-size:12px;">({{ item.unit }})</span>
          <el-tag v-if="item._existing" size="small" type="info">已添加</el-tag>
        </label>
      </div>
      <template #footer>
        <el-button @click="addParamDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!addParamDialogNewCount" @click="confirmAddParamDialog">
          确认 ({{ addParamDialogNewCount }})
        </el-button>
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
        <div><span>任务状态</span><strong>{{ getEnumLabel(taskStatusLabelMap, taskDetail?.taskStatus || taskDetail?.task_status) }}</strong></div>
        <div><span>完成时间</span><strong>{{ taskDetail?.finishedTime || taskDetail?.finished_time || '-' }}</strong></div>
        <div><span>天气</span><strong>{{ taskDetail?.weather || '-' }}</strong></div>
        <div><span>温度</span><strong>{{ taskDetail?.temperature || '-' }}</strong></div>
        <div><span>采样总容量</span><strong>{{ taskDetail?.sampleTotalVolume || taskDetail?.sample_total_volume || '-' }}</strong></div>
        <div><span>采样瓶数</span><strong>{{ taskDetail?.sampleBottleCount || taskDetail?.sample_bottle_count || '-' }}</strong></div>
        <div><span>地图位置</span><strong>{{ formatLocationText(taskDetail) }}</strong></div>
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
        <el-button v-if="hasCoordinates(taskDetail)" @click="openTaskDetailMap">查看地图</el-button>
        <el-button v-if="hasCoordinates(taskDetail)" type="primary" plain @click="openNavigation(taskDetail)">导航</el-button>
        <el-button @click="taskDetailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="planMapSelectorVisible"
      title="选择采样点位"
      width="960px"
      align-center
      append-to-body
      destroy-on-close
    >
      <TiandituPointSelector v-model="mapSelectorValue" />
      <template #footer>
        <el-button @click="planMapSelectorVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPlanMapSelection">确认选点</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="mapSelectorVisible"
      title="选择现场位置"
      width="960px"
      align-center
      append-to-body
      destroy-on-close
    >
      <TiandituPointSelector v-model="mapSelectorValue" />
      <template #footer>
        <el-button @click="mapSelectorVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmMapSelection">确认位置</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="taskLocationViewerVisible"
      title="任务地图位置"
      width="960px"
      align-center
      append-to-body
      destroy-on-close
    >
      <TiandituPointSelector v-model="taskLocationViewerValue" readonly />
      <template #footer>
        <el-button type="primary" plain @click="openNavigation(taskLocationViewerValue)">导航</el-button>
        <el-button @click="taskLocationViewerVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElDatePicker } from 'element-plus/es/components/date-picker/index.mjs'
import { ElDatePickerPanel } from 'element-plus/es/components/date-picker-panel/index.mjs'
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
import TiandituPointSelector from '../components/TiandituPointSelector.vue'
import {
  abandonSamplingTaskApi,
  completeSamplingTaskApi,
  createSamplingPlanApi,
  dispatchSamplingPlanApi,
  fetchDictItemsApi,
  exportSamplingPlansApi,
  exportSamplesApi,
  exportSamplingTasksApi,
  fetchDetectionMethodOptionsApi,
  fetchDetectionParametersApi,
  fetchMonitoringPointsApi,
  fetchMonitoringPointOrgOptionsApi,
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
  inProgressTaskStatus,
  loggedSampleStatus,
  pausedPlanStatus,
  pendingTaskStatus,
  planStatusLabelMap,
  retestSampleStatus,
  reviewingSampleStatus,
  routineSamplingType,
  samplingSampleSourceMethod,
  sampleRegisterStatusLabelMap,
  sampleSourceMethodLabelMap,
  sampleSourceMethodOptions,
  sampleStatusLabelMap,
  sampleTypeOptions,
  sampleTypeLabelMap,
  taskStatusLabelMap,
  translateWorkflowText
} from '../utils/labEnums'

const route = useRoute()
const FLOW_TYPE_REVIEW = 'REVIEW'
const YANZHEN_WATER_PLANT_NAME = '\u6cbf\u9547\u6c34\u5382'
const STAFF_ROLE_CODE = 'STAFF'
const DIRECTOR_ROLE_CODE = 'DIRECTOR'

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
  sampleRegisterStatus: '',
  samplerId: '',
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE
})
const sampleQuery = reactive({
  keyword: '',
  sampleStatus: '',
  sampleType: '',
  sampleSourceMethod: '',
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
const planMapSelectorVisible = ref(false)
const mapSelectorVisible = ref(false)
const mapSelectorValue = reactive({ pointName: '', address: '', latitude: '', longitude: '' })
const taskLocationViewerVisible = ref(false)
const taskLocationViewerValue = reactive({ pointName: '', address: '', latitude: '', longitude: '' })
const taskDetailDialogVisible = ref(false)
const addParamDialogVisible = ref(false)
const addParamDialogList = ref([])
const addParamDialogChecked = reactive({})

const addParamDialogNewCount = computed(() => {
  return addParamDialogList.value
    .filter((item) => addParamDialogChecked[item.id] && !item._existing)
    .length
})

function openAddParamDialog() {
  const existingIds = new Set(loginDetectionConfigRows.value.map((row) => String(row.parameterId)))
  // 清空旧的勾选
  Object.keys(addParamDialogChecked).forEach((key) => delete addParamDialogChecked[key])
  // 构建列表：已在套餐中的标记 _existing
  addParamDialogList.value = (detectionParameterOptions.value || []).map((item) => {
    const exists = existingIds.has(String(item.id))
    if (exists) {
      addParamDialogChecked[item.id] = true
    }
    return { ...item, _existing: exists }
  })
  addParamDialogVisible.value = true
}

function confirmAddParamDialog() {
  const selected = addParamDialogList.value.filter(
    (item) => addParamDialogChecked[item.id] && !item._existing
  )
  if (!selected.length) return

  const rows = selected.map((parameter) => {
    const parameterId = String(parameter.id)
    const methodOptions = getDetectionConfigMethodOptionsByParameter(parameterId)
    const currentMethod = methodOptions[0] || null
    return {
      parameterId,
      parameterName: parameter.parameterName || '',
      unit: parameter.unit || '',
      standardMin: parameter.standardMin,
      standardMax: parameter.standardMax,
      optionValues: parameter.optionValues || '',
      referenceStandard: parameter.referenceStandard || '',
      methodId: currentMethod?.id || '',
      methodName: currentMethod?.methodName || '',
      sampleVolume: currentMethod?.sampleVolume || '',
      methodOptions
    }
  })
  loginForm.detectionConfigItems = [...loginForm.detectionConfigItems, ...rows]
  addParamDialogVisible.value = false
  ElMessage.success(`成功添加 ${rows.length} 个检测参数`)
}
const weatherOptions = ref([])
const storageConditionOptions = ref([])
const orgOptions = ref([])
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
const loginPreviewTaskLabel = ref('')
const taskCompletePreview = ref(null)
const taskCompletePhotoList = ref([])
const taskDetailPhotoList = ref([])
const taskDetail = ref(null)
const activePlanHourPanel = ref('')
const startPlanHourTriggerRef = ref(null)
const endPlanHourTriggerRef = ref(null)
const planHourPanelStyle = ref({})
const hourOptions = Array.from({ length: 24 }, (_, index) => String(index).padStart(2, '0'))

const loginForm = reactive({
  taskId: null,
  sampleNo: '',
  pointId: null,
  pointName: '',
  sampleType: '',
  sampleSourceMethod: samplingSampleSourceMethod,
  detectionItems: '',
  detectionTypeId: null,
  detectionTypeName: '',
  detectionConfigItems: [],
  reviewFlowId: null,
  reviewFlowName: '',
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
const isLoginReadonly = computed(() => loginDialogMode.value === 'view')
const loginDialogTitle = computed(() => isLoginReadonly.value ? '样品登记明细' : '样品登录')
const loginConfigPanelNote = computed(() => (
  isLoginReadonly.value
    ? '当前仅展示该样品登记时保存的检测参数与检测方法明细，不可在此窗口中修改。'
    : '检测套餐在样品登录时选择，保存后后续检测分析沿用这份参数与方法明细。'
))

const loginDetectionProjectOptions = computed(() => {
  const sampleType = String(loginForm.sampleType || '').trim()
  if (!sampleType) {
    return detectionProjectOptions.value
  }
  return detectionProjectOptions.value
    .filter((item) => !item.sampleType || String(item.sampleType) === sampleType)
    .sort((left, right) => {
      const leftExact = String(left.sampleType || '') === sampleType ? 1 : 0
      const rightExact = String(right.sampleType || '') === sampleType ? 1 : 0
      if (leftExact !== rightExact) {
        return rightExact - leftExact
      }
      return String(left.typeName || '').localeCompare(String(right.typeName || ''), 'zh-CN')
    })
})

const planForm = reactive({
  planName: '',
  pointSource: 'CUSTOM',
  orgId: '',
  pointId: null,
  pointName: '',
  address: '',
  latitude: '',
  longitude: '',
  startTime: '',
  endTime: '',
  samplerIds: [],
  samplerId: null,
  samplerName: '',
  samplingType: routineSamplingType,
  sampleType: '',
  cycleType: dailyCycleType,
  remark: ''
})

const dispatchForm = reactive({
  planId: null,
  samplingTime: '',
  samplerIds: [],
  samplerId: null,
  samplerName: ''
})

const taskCompleteForm = reactive({
  taskId: null,
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
    subtitle: '展示未采样任务，任务无需单独完成，样品登录后自动变为已采样。',
    tableTitle: '采样任务',
    tableSubtitle: '默认聚焦未采样任务，可直接进入样品登录。',
    note: '采样任务页只展示任务和样品编号，状态按未采样、已采样两种展示。',
    guide: '如需安排周期计划，请进入采样计划；未采样任务可直接在样品登录中选择并登记。',
    mode: 'task',
    defaultStatKey: 'tasks:unsampled',
    allowTaskActions: true,
    showPlanSection: false,
    emptyText: '暂无未采样任务数据',
    taskFilter: () => true,
    sampleFilter: () => true,
    quickLinks: [
      { path: '/sampling-plan', label: '采样计划', desc: '独立维护周期计划并执行手动派发' },
      { path: '/sample-login', label: '样品登录', desc: '选择未采样任务并登记为正式样品' },
      { path: '/task-history', label: '历史任务', desc: '查看已采样或已废弃的采样记录' },
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
      { path: '/sample-login', label: '样品登录', desc: '选择未采样任务并登记样品' },
      { path: '/task-ledger', label: '任务台账', desc: '追踪计划转任务后的全量执行记录' }
    ]
  },
  '/task-history': {
    key: 'task-history',
    title: '历史任务',
    subtitle: '回看已经采样或已废弃的采样任务，方便核对现场执行情况与补录链路。',
    tableTitle: '历史采样任务',
    tableSubtitle: '本页聚焦已采样、已废弃任务，操作区切换为只读查询视角。',
    note: '历史任务页用于追溯与复盘，不再承载现场执行按钮，避免误操作。',
    guide: '如历史任务已形成样品，可直接跳转到样品台账继续核验编号与留痕。',
    mode: 'task',
    defaultStatKey: 'tasks:sampled',
    allowTaskActions: false,
    showPlanSection: false,
    emptyText: '暂无历史采样任务数据',
    taskFilter: (item) => isTaskRegistered(item) || item.taskStatus === abandonedTaskStatus,
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
      { path: '/sample-login', label: '样品登录', desc: '承接未采样任务并延续已有样品编号' },
      { path: '/task-history', label: '历史任务', desc: '只看已采样与已废弃任务' },
      { path: '/detection-analysis', label: '检测分析', desc: '继续进入化验室检测流程' }
    ]
  },
  '/sample-login': {
    key: 'sample-login',
    title: '样品登录',
    subtitle: '选择未采样任务直接登记为正式样品，沿用任务已生成的样品编号并承接后续检测、审核、报告链路。',
    tableTitle: '已登记样品',
    tableSubtitle: '默认展示已登记样品，并通过统计卡切换到待审核、退回重检、闭环完成等状态。',
    note: '样品登录页优先解决未采样任务，选择任务后回显任务已生成的样品编号。',
    guide: '如本页没有可登录任务，请先确认采样计划是否已生成任务；若样品已登记，可继续前往检测分析。',
    mode: 'sample',
    defaultStatKey: 'samples:logged',
    allowTaskActions: false,
    showPlanSection: false,
    emptyText: '暂无样品登录数据',
    taskFilter: () => true,
    sampleFilter: () => true,
    quickLinks: [
      { path: '/task-assign', label: '采样任务', desc: '查看未采样任务清单' },
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
const taskStatusOptions = computed(() => [
  { value: 'UNREGISTERED', label: '未采样' },
  { value: 'REGISTERED', label: '已采样' }
])
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
  return task && !isTaskRegistered(task) && task.taskStatus !== abandonedTaskStatus
}

const pendingLoggableCount = computed(() =>
  loggableTasks.value.length || getCount(taskStatCounts.value, 'UNLOGGED')
)

const firstCompletableTask = computed(() =>
  taskSceneRecords.value.find((item) => isTaskCompleteEntryEnabled(item))
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
          label: '未采样',
          value: getCount(taskStatCounts.value, 'UNSAMPLED') || getCount(taskStatCounts.value, 'UNLOGGED'),
          type: 'warning'
        },
        {
          label: '已采样',
          value: getCount(taskStatCounts.value, 'SAMPLED') || getCount(taskStatCounts.value, completedTaskStatus),
          type: 'success'
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
      const totalCount = getCount(taskStatCounts.value, 'ALL')
      const unsampledCount = getCount(taskStatCounts.value, 'UNSAMPLED') || getCount(taskStatCounts.value, 'UNLOGGED')
      const sampledCount = getCount(taskStatCounts.value, 'SAMPLED') || getCount(taskStatCounts.value, completedTaskStatus)
      return [
        {
          key: 'tasks:all',
          label: '总采样任务',
          value: totalCount,
          desc: '采样任务总量'
        },
        {
          key: 'tasks:sampled',
          label: '已采样任务',
          value: sampledCount,
          desc: '已经完成样品登录的任务'
        },
        {
          key: 'tasks:unsampled',
          label: '未采样任务',
          value: unsampledCount,
          desc: '可直接在样品登录中选择登记'
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
        key: 'tasks:unsampled',
        label: '未采样',
        value: getCount(taskStatCounts.value, 'UNSAMPLED') || getCount(taskStatCounts.value, 'UNLOGGED'),
        desc: '尚未登记为样品的采样任务'
      },
      {
        key: 'tasks:sampled',
        label: '已采样',
        value: getCount(taskStatCounts.value, 'SAMPLED') || getCount(taskStatCounts.value, completedTaskStatus),
        desc: '已经完成样品登录的任务'
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
        desc: '未采样且尚未生成样品的任务'
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
      value: getCount(taskStatCounts.value, 'UNSAMPLED') || getCount(taskStatCounts.value, 'UNLOGGED'),
      desc: '未采样且尚未登记为样品的任务数量'
    }
  ]
})

const visibleTasks = computed(() => {
  const records = taskSceneRecords.value
  if (activeStatKey.value === 'tasks:unsampled') {
    return records.filter((item) => !isTaskRegistered(item) && item.taskStatus !== abandonedTaskStatus)
  }
  if (activeStatKey.value === 'tasks:sampled') {
    return records.filter((item) => isTaskRegistered(item))
  }
  if (activeStatKey.value === 'tasks:abandoned') {
    return records.filter((item) => item.taskStatus === abandonedTaskStatus)
  }
  if (activeStatKey.value === 'tasks:unlogged') {
    return records.filter((item) =>
      !isTaskRegistered(item) && item.taskStatus !== abandonedTaskStatus
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
  taskQuery.sampleRegisterStatus = ''
  sampleQuery.sampleStatus = ''
  if (isPlanScene.value) {
    planQuery.pageNum = 1
    return
  }
  if (isTaskScene.value) {
    taskQuery.taskStatus = typeof route.query.taskStatus === 'string' ? route.query.taskStatus : ''
    taskQuery.sampleRegisterStatus = typeof route.query.sampleRegisterStatus === 'string' ? route.query.sampleRegisterStatus : ''
    taskQuery.pageNum = 1
    syncActiveStatByCurrentQuery()
    return
  }
  taskQuery.taskStatus = typeof route.query.taskStatus === 'string' ? route.query.taskStatus : ''
  taskQuery.sampleRegisterStatus = typeof route.query.sampleRegisterStatus === 'string' ? route.query.sampleRegisterStatus : ''
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
    taskQuery.sampleRegisterStatus = getTaskRegisterStatusByStatKey(key) || ''
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
    'tasks:abandoned': abandonedTaskStatus,
    'tasks:unlogged': ''
  }
  return statusMap[key]
}

function getTaskRegisterStatusByStatKey(key) {
  const statusMap = {
    'tasks:unsampled': 'UNREGISTERED',
    'tasks:unlogged': 'UNREGISTERED',
    'tasks:sampled': 'REGISTERED'
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
    if (taskQuery.sampleRegisterStatus === 'UNREGISTERED') {
      activeStatKey.value = 'tasks:unsampled'
    } else if (taskQuery.sampleRegisterStatus === 'REGISTERED') {
      activeStatKey.value = 'tasks:sampled'
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
  return {
    pageNum: 1,
    pageSize: 1,
    scope: isTaskTodoScene.value ? 'todo' : ''
  }
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
    sampleRegisterStatus: 'UNREGISTERED'
  })
  const records = Array.isArray(result.records) ? result.records : []
  loggableTasks.value = records.filter((item) => !isTaskRegistered(item) && item.taskStatus !== abandonedTaskStatus)
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

async function handleRouteAutoOpen() {
  if (route.query.autoOpen !== '1') {
    return
  }
  if (baseScene.value.key === 'task-assign') {
    const task = firstCompletableTask.value
    if (!task) {
      ElMessage.warning('当前没有可进行采样录入的任务')
      return
    }
    await openTaskCompleteDialog(task)
    return
  }
  if (baseScene.value.key === 'sample-login') {
    await openLoginDialog()
    return
  }
  if (baseScene.value.key === 'sampling-plan') {
    await createPlan()
  }
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
    if (taskQuery.sampleRegisterStatus) {
      taskQuery.taskStatus = ''
    }
    taskQuery.pageNum = 1
    syncActiveStatByCurrentQuery()
    loadTasks()
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
    taskQuery.sampleRegisterStatus = ''
    taskQuery.samplerId = ''
    taskQuery.pageNum = 1
    activeStatKey.value = 'tasks:all'
    loadTasks()
    return
  }
  sampleQuery.keyword = ''
  sampleQuery.sampleStatus = ''
  sampleQuery.sampleType = ''
  sampleQuery.sampleSourceMethod = ''
  sampleQuery.pageNum = 1
  activeStatKey.value = 'samples:all'
  loadSamples()
}

async function loadMonitoringPoints(orgId = planForm.orgId) {
  const normalizedOrgId = String(orgId || '').trim()
  if (!normalizedOrgId) {
    monitoringPointOptions.value = []
    return
  }
  monitoringPointLoading.value = true
  try {
    const result = await fetchMonitoringPointsApi({
      pageNum: 1,
      pageSize: 500,
      orgId: normalizedOrgId,
      pointStatus: enabledPointStatus
    })
    monitoringPointOptions.value = result.records || []
  } finally {
    monitoringPointLoading.value = false
  }
}

async function loadSamplers(force = false) {
  if (!force && samplerOptions.value.length) {
    return
  }
  samplerLoading.value = true
  try {
    const result = await fetchSystemUsersApi({
      pageNum: 1,
      pageSize: 500,
      roleCode: STAFF_ROLE_CODE,
      status: 1
    })
    const records = Array.isArray(result.records) ? result.records : []
    samplerOptions.value = dedupeSamplerOptions(records.map(normalizeSamplerOption).filter((item) => item.id))
  } finally {
    samplerLoading.value = false
  }
}

function handleSamplerDropdownVisible(visible) {
  if (visible) {
    // 如果已选择机构，按机构加载人员；否则加载所有人员
    if (planForm.orgId) {
      loadSamplersByOrg(planForm.orgId, false)
    } else {
      loadSamplers(true)
    }
  }
}

async function loadSamplersByOrg(orgId, applyToPlanForm = true) {
  const normalizedOrgId = String(orgId || '').trim()
  if (!normalizedOrgId) {
    samplerOptions.value = []
    return
  }
  samplerLoading.value = true
  try {
    const staffParams = {
      pageNum: 1,
      pageSize: 500,
      orgId: normalizedOrgId,
      roleCode: STAFF_ROLE_CODE,
      status: 1
    }
    const requests = [fetchSystemUsersApi(staffParams)]
    if (isYanzhenOrgId(normalizedOrgId)) {
      requests.push(fetchSystemUsersApi({
        pageNum: 1,
        pageSize: 500,
        roleCode: DIRECTOR_ROLE_CODE,
        status: 1
      }))
    }
    const results = await Promise.all(requests)
    const records = results.flatMap((result) => Array.isArray(result.records) ? result.records : [])
    samplerOptions.value = dedupeSamplerOptions(
      filterSamplerCandidates(records, normalizedOrgId).map(normalizeSamplerOption).filter((item) => item.id)
    )
    // 自动选中所有人员（如果只有一个则选中，多个则全部选中）
    if (applyToPlanForm && samplerOptions.value.length > 0) {
      const samplerIds = samplerOptions.value.map((item) => getSamplerOptionId(item))
      const samplerNames = samplerOptions.value.map((item) => getSamplerDisplayName(item)).filter(Boolean)
      planForm.samplerIds = samplerIds
      planForm.samplerId = samplerIds[0] || null
      planForm.samplerName = samplerNames.join('、')
    }
  } finally {
    samplerLoading.value = false
  }
}

async function loadSamplingDictOptions() {
  const [weatherItems, storageItems, orgItems] = await Promise.all([
    fetchDictItemsApi('weather_condition'),
    fetchDictItemsApi('storage_condition'),
    fetchMonitoringPointOrgOptionsApi()
  ])
  weatherOptions.value = normalizeDictOptions(weatherItems)
  storageConditionOptions.value = normalizeDictOptions(storageItems)
  orgOptions.value = Array.isArray(orgItems)
    ? orgItems.map((item) => ({ label: item.orgName, value: String(item.id) }))
    : []
}

function normalizeSamplerIdList(value) {
  const rawItems = Array.isArray(value)
    ? value
    : String(value || '')
      .split(',')
      .map((item) => item.trim())
  return rawItems
    .filter((item) => item && item !== '0' && item !== 'null' && item !== 'undefined')
    .filter((item, index, source) => source.indexOf(item) === index)
}

function getSamplerOptionId(item) {
  const rawId = item?.id ?? item?.userId ?? item?.user_id
  // 保持字符串类型，避免大数字精度丢失
  if (rawId !== null && rawId !== undefined && rawId !== '') {
    return String(rawId)
  }
  return rawId
}

function getSamplerDisplayName(item) {
  return String(
    item?.realName
    || item?.real_name
    || item?.name
    || item?.nickName
    || item?.nickname
    || item?.displayName
    || item?.employeeName
    || item?.label
    || item?.username
    || ''
  ).trim()
}

function splitSamplerNames(value) {
  return String(value || '')
    .split(/[、,，]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function normalizeSamplerOption(item) {
  const id = getSamplerOptionId(item)
  return {
    ...item,
    id,
    label: getSamplerDisplayName(item) || String(id || '')
  }
}

function dedupeSamplerOptions(options) {
  const idSet = new Set()
  const nameSet = new Set()
  const result = []
  ;(options || []).forEach((item) => {
    const id = getSamplerOptionId(item)
    const name = getSamplerDisplayName(item)
    const idKey = id ? String(id) : ''
    const nameKey = name || ''
    if ((idKey && idSet.has(idKey)) || (nameKey && nameSet.has(nameKey))) {
      return
    }
    if (idKey) {
      idSet.add(idKey)
    }
    if (nameKey) {
      nameSet.add(nameKey)
    }
    result.push(item)
  })
  return result
}

function getRoleCode(item) {
  return String(item?.roleCode || item?.role_code || '').trim().toUpperCase()
}

function isYanzhenOrgId(orgId) {
  const normalizedOrgId = String(orgId || '').trim()
  if (!normalizedOrgId) {
    return false
  }
  const option = orgOptions.value.find((item) => String(item.value) === normalizedOrgId)
  return String(option?.label || '').trim() === YANZHEN_WATER_PLANT_NAME
}

function isSamplerCandidateForOrg(item, orgId) {
  const normalizedOrgId = String(orgId || '').trim()
  const roleCode = getRoleCode(item)
  if (roleCode === STAFF_ROLE_CODE) {
    if (!normalizedOrgId) {
      return true
    }
    return String(item?.orgId || item?.org_id || '').trim() === normalizedOrgId
  }
  return isYanzhenOrgId(orgId) && roleCode === DIRECTOR_ROLE_CODE
}

function filterSamplerCandidates(records, orgId) {
  return (records || []).filter((item) => isSamplerCandidateForOrg(item, orgId))
}

function getRequiredSamplerIdsForOrg(orgId) {
  if (!isYanzhenOrgId(orgId)) {
    return []
  }
  return samplerOptions.value
    .filter((item) => getRoleCode(item) === DIRECTOR_ROLE_CODE)
    .map((item) => getSamplerOptionId(item))
    .filter(Boolean)
}

function includeRequiredSamplerIds(ids, orgId) {
  return normalizeSamplerIdList([
    ...normalizeSamplerIdList(ids),
    ...getRequiredSamplerIdsForOrg(orgId)
  ])
}

function ensureSelectedSamplerOptions(ids, samplerNameText) {
  const selectedIds = normalizeSamplerIdList(ids)
  if (!selectedIds.length) {
    return
  }
  const names = splitSamplerNames(samplerNameText)
  const existingIds = new Set(samplerOptions.value.map((item) => String(getSamplerOptionId(item))))
  const existingNames = new Set(samplerOptions.value.map((item) => getSamplerDisplayName(item)).filter(Boolean))
  const additions = selectedIds
    .filter((id, index) => !existingIds.has(String(id)) && !existingNames.has(names[index]))
    .map((id, index) => ({
      id,
      realName: names[index] || `采样员${id}`,
      username: String(id),
      label: names[index] || `采样员${id}`
    }))
  if (additions.length) {
    samplerOptions.value = dedupeSamplerOptions([...samplerOptions.value, ...additions])
  }
}

function resolveSamplerNames(ids) {
  const selectedIds = normalizeSamplerIdList(ids)
  return selectedIds
    .map((id) => samplerOptions.value.find((item) => String(getSamplerOptionId(item)) === String(id)))
    .filter(Boolean)
    .map((item) => getSamplerDisplayName(item))
    .filter(Boolean)
    .join('、')
}

function getRowSamplerIds(row) {
  const ids = normalizeSamplerIdList(row?.samplerIds || row?.sampler_ids)
  if (ids.length) {
    return ids
  }
  return normalizeSamplerIdList(row?.samplerId || row?.sampler_id)
}

function resolveRowSamplerOptionIds(row) {
  const names = splitSamplerNames(row?.samplerName || row?.sampler_name)
  const ids = getRowSamplerIds(row)
  const resolved = ids.map((id, index) => {
    const idMatched = samplerOptions.value.find((item) => String(getSamplerOptionId(item)) === String(id))
    if (idMatched) {
      return getSamplerOptionId(idMatched)
    }
    const name = names[index]
    if (!name) {
      return id
    }
    const nameMatched = samplerOptions.value.find((item) => getSamplerDisplayName(item) === name)
    return nameMatched ? getSamplerOptionId(nameMatched) : id
  })
  return normalizeSamplerIdList(resolved)
}

function getDefaultSamplerIds() {
  return samplerOptions.value.map((item) => getSamplerOptionId(item))
}

function applyDefaultPlanSampler() {
  const ids = getDefaultSamplerIds()
  planForm.samplerIds = ids
  handlePlanSamplerChange(ids)
}

function handlePlanSamplerChange(userIds) {
  const ids = normalizeSamplerIdList(userIds)
  planForm.samplerIds = ids
  planForm.samplerId = ids[0] || null
  planForm.samplerName = resolveSamplerNames(ids)
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

function handleTaskCompleteSampleTotalVolumeInput(value) {
  taskCompleteForm.sampleTotalVolume = sanitizeSampleVolumeNumber(value)
}

function resetDispatchForm() {
  dispatchForm.planId = null
  dispatchForm.samplingTime = ''
  dispatchForm.samplerIds = []
  dispatchForm.samplerId = null
  dispatchForm.samplerName = ''
}

function handleDispatchSamplerChange(userIds) {
  const ids = normalizeSamplerIdList(userIds)
  dispatchForm.samplerIds = ids
  dispatchForm.samplerId = ids[0] || null
  dispatchForm.samplerName = resolveSamplerNames(ids)
}

function getDispatchPlanOrgId() {
  const planId = String(dispatchForm.planId || '')
  const plan = plans.value.find((item) => String(item.id) === planId)
  return plan?.orgId || plan?.org_id || ''
}

function getPlanDateTime(prefix) {
  const value = planForm[`${prefix}Time`]
  const parsed = value ? dayjs(value) : null
  return parsed?.isValid() ? parsed : null
}

function getPlanDatePart(prefix) {
  return getPlanDateTime(prefix)?.format('YYYY-MM-DD') || ''
}

function getPlanDateTimeText(prefix) {
  return getPlanDateTime(prefix)?.format('YYYY-MM-DD HH:mm:ss') || ''
}

function getPlanHourPart(prefix) {
  return getPlanDateTime(prefix)?.format('HH') || '00'
}

function buildPlanHourDateTimeText(date, hour) {
  return date ? `${date} ${hour || '00'}:00:00` : ''
}

function handlePlanDateChange(prefix, value) {
  planForm[`${prefix}Time`] = buildPlanHourDateTimeText(value, getPlanHourPart(prefix))
  // 如果修改的是开始时间
  if (prefix === 'start') {
    // 已有点位名称，自动更新计划名称
    if (planForm.pointName && planForm.startTime) {
      planForm.planName = planForm.pointName
    }
    // 自动设置截止时间为24小时后
    if (planForm.startTime) {
      planForm.endTime = dayjs(planForm.startTime).add(24, 'hour').format('YYYY-MM-DD HH:mm:ss')
    }
  }
}

function handlePlanHourChange(prefix, value) {
  const date = getPlanDatePart(prefix) || dayjs().format('YYYY-MM-DD')
  planForm[`${prefix}Time`] = buildPlanHourDateTimeText(date, value)
  activePlanHourPanel.value = ''
  // 如果修改的是开始时间
  if (prefix === 'start') {
    // 已有点位名称，自动更新计划名称
    if (planForm.pointName && planForm.startTime) {
      planForm.planName = planForm.pointName
    }
    // 自动设置截止时间为24小时后
    if (planForm.startTime) {
      planForm.endTime = dayjs(planForm.startTime).add(24, 'hour').format('YYYY-MM-DD HH:mm:ss')
    }
  }
}

function updatePlanHourPanelPosition(prefix = activePlanHourPanel.value) {
  if (!prefix) {
    return
  }
  const trigger = prefix === 'start' ? startPlanHourTriggerRef.value : endPlanHourTriggerRef.value
  if (!trigger) {
    return
  }
  const rect = trigger.getBoundingClientRect()
  const preferredWidth = 700
  const viewportGap = 12
  const panelWidth = Math.min(preferredWidth, window.innerWidth - viewportGap * 2)
  const panelHeight = 430
  const bottomTop = rect.bottom + 6
  const topTop = rect.top - panelHeight - 6
  const top = bottomTop + panelHeight + viewportGap > window.innerHeight
    ? Math.max(viewportGap, topTop)
    : bottomTop
  const left = Math.min(Math.max(rect.left, viewportGap), window.innerWidth - panelWidth - viewportGap)
  planHourPanelStyle.value = {
    top: `${top}px`,
    left: `${left}px`,
    width: `${panelWidth}px`
  }
}

async function togglePlanHourPanel(prefix) {
  const nextPanel = activePlanHourPanel.value === prefix ? '' : prefix
  activePlanHourPanel.value = nextPanel
  if (!planForm[`${prefix}Time`]) {
    planForm[`${prefix}Time`] = buildPlanHourDateTimeText(dayjs().format('YYYY-MM-DD'), '00')
  }
  if (nextPanel) {
    await nextTick()
    updatePlanHourPanelPosition(nextPanel)
  }
}

function handlePlanHourOutsideClick(event) {
  if (!event.target?.closest?.('[data-plan-hour-picker]')) {
    activePlanHourPanel.value = ''
  }
}

function handlePlanHourViewportChange() {
  updatePlanHourPanelPosition()
}

function resetPlanForm() {
  editingPlanId.value = null
  activePlanHourPanel.value = ''
  planForm.planName = ''
  planForm.pointSource = 'CUSTOM'
  planForm.orgId = ''
  planForm.pointId = null
  planForm.pointName = ''
  planForm.address = ''
  planForm.latitude = ''
  planForm.longitude = ''
  planForm.startTime = ''
  planForm.endTime = ''
  planForm.samplerIds = []
  planForm.samplerId = null
  planForm.samplerName = ''
  planForm.samplingType = routineSamplingType
  planForm.sampleType = ''
  planForm.cycleType = dailyCycleType
  planForm.remark = ''
}

async function openPlanDialog() {
  resetPlanForm()
  planForm.planName = `采样计划`
  monitoringPointOptions.value = []
  samplerOptions.value = []
  planDialogVisible.value = true
}

async function openPlanEditDialog(row) {
  editingPlanId.value = row.id
  planForm.planName = row.planName || ''
  planForm.pointSource = row.pointId ? 'EXISTING' : 'CUSTOM'
  planForm.orgId = row.orgId ? String(row.orgId) : ''
  planForm.pointId = row.pointId || null
  planForm.pointName = row.pointName || ''
  planForm.address = row.address || ''
  planForm.latitude = row.latitude || ''
  planForm.longitude = row.longitude || ''
  planForm.startTime = row.startTime || ''
  planForm.endTime = row.endTime || ''
  planForm.samplerIds = resolveRowSamplerOptionIds(row)
  planForm.samplerId = planForm.samplerIds[0] || null
  planForm.samplerName = row.samplerName || row.sampler_name || ''
  planForm.samplingType = row.samplingType || routineSamplingType
  planForm.sampleType = row.sampleType || ''
  planForm.cycleType = row.cycleType || dailyCycleType
  planForm.remark = row.remark || ''
  if (planForm.pointSource === 'EXISTING' && planForm.pointId) {
    await loadMonitoringPointForEdit(row)
    const savedPlanName = planForm.planName
    handlePlanPointChange(planForm.pointId)
    planForm.planName = savedPlanName || planForm.planName
    // 根据机构加载人员
    if (planForm.orgId) {
      await loadSamplersByOrg(planForm.orgId, false)
      ensureSelectedSamplerOptions(planForm.samplerIds, planForm.samplerName)
    }
  } else if (planForm.orgId) {
    // 手工填写点位但有机构时，也需要加载该机构下的人员
    await loadSamplersByOrg(planForm.orgId, false)
    ensureSelectedSamplerOptions(planForm.samplerIds, planForm.samplerName)
  }
  planDialogVisible.value = true
}

function handlePlanPointSourceChange(value) {
  if (value === 'CUSTOM') {
    planForm.orgId = ''
    planForm.pointId = null
    planForm.pointName = ''
    planForm.address = ''
    planForm.latitude = ''
    planForm.longitude = ''
    planForm.sampleType = ''
    monitoringPointOptions.value = []
    return
  }
  planForm.pointId = null
  planForm.pointName = ''
  planForm.address = ''
  planForm.latitude = ''
  planForm.longitude = ''
  planForm.sampleType = ''
  monitoringPointOptions.value = []
  if (planForm.orgId) {
    loadMonitoringPoints(planForm.orgId)
  }
}

async function handlePlanOrgChange(orgId) {
  planForm.pointId = null
  planForm.pointName = ''
  planForm.address = ''
  planForm.latitude = ''
  planForm.longitude = ''
  planForm.sampleType = ''
  planForm.samplerIds = []
  planForm.samplerId = null
  planForm.samplerName = ''
  await loadMonitoringPoints(orgId)
  // 加载该机构下的人员并自动选中
  await loadSamplersByOrg(orgId)
}

function handlePlanPointNameChange(pointName) {
  // 点位名称变化时，自动更新计划名称
  if (pointName && planForm.startTime) {
    planForm.planName = pointName
  }
}

function handlePlanPointChange(pointId) {
  const point = monitoringPointOptions.value.find((item) => item.id === pointId)
  planForm.pointId = point?.id || null
  planForm.pointName = point?.pointName || ''
  planForm.address = point?.address || point?.pointName || ''
  planForm.latitude = point?.latitude || ''
  planForm.longitude = point?.longitude || ''
  planForm.sampleType = point?.pointType || ''
  if (point?.orgId) {
    planForm.orgId = String(point.orgId)
  }
  // 自动生成计划名称：点位名称
  if (point?.pointName && planForm.startTime) {
    planForm.planName = point.pointName
  }
}

async function loadMonitoringPointForEdit(row) {
  const rowPointId = row?.pointId
  if (!rowPointId) {
    monitoringPointOptions.value = []
    return
  }
  // 先加载监测点位详情，获取所属机构
  monitoringPointLoading.value = true
  try {
    const result = await fetchMonitoringPointsApi({
      pageNum: 1,
      pageSize: 500,
      pointStatus: enabledPointStatus
    })
    const records = result.records || []
    const point = records.find((item) => item.id === rowPointId)
    if (point?.orgId) {
      planForm.orgId = String(point.orgId)
      monitoringPointOptions.value = records.filter((item) => String(item.orgId) === planForm.orgId)
    } else {
      monitoringPointOptions.value = records
    }
  } finally {
    monitoringPointLoading.value = false
  }
}

function buildPlanPayload() {
  const samplerIds = includeRequiredSamplerIds(planForm.samplerIds, planForm.orgId)
  const samplerName = resolveSamplerNames(samplerIds)
  return {
    planName: planForm.planName?.trim() || '',
    orgId: planForm.orgId || null,
    pointId: planForm.pointSource === 'EXISTING' ? planForm.pointId : null,
    pointName: planForm.pointName?.trim() || '',
    address: planForm.address?.trim() || '',
    latitude: planForm.latitude?.trim() || '',
    longitude: planForm.longitude?.trim() || '',
    startTime: planForm.startTime || '',
    endTime: planForm.endTime || '',
    samplerIds,
    samplerId: samplerIds[0] || planForm.samplerId,
    samplerName: samplerName || planForm.samplerName?.trim() || '',
    samplingType: planForm.samplingType || routineSamplingType,
    sampleType: planForm.sampleType || '',
    cycleType: planForm.cycleType || dailyCycleType,
    remark: planForm.remark?.trim() || ''
  }
}

async function createPlan() {
  await openPlanDialog()
}

async function submitPlanForm() {
  const payload = buildPlanPayload()
  if (!payload.planName || !payload.orgId || !payload.pointName || !payload.startTime || !payload.sampleType || !payload.cycleType) {
    ElMessage.warning('请完整填写采样计划信息')
    return
  }
  if (!payload.samplerIds.length || !payload.samplerId || !payload.samplerName) {
    ElMessage.warning('请选择采样人员')
    return
  }
  if (planForm.pointSource === 'EXISTING') {
    if (!payload.pointId) {
      ElMessage.warning('请选择监测点位')
      return
    }
  }
  if (!payload.latitude || !payload.longitude) {
    ElMessage.warning('请从地图选择采样点位坐标')
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
  const rowOrgId = row?.orgId || row?.org_id || ''
  if (rowOrgId) {
    await loadSamplersByOrg(rowOrgId, false)
  } else {
    await loadSamplers(true)
  }
  resetDispatchForm()
  dispatchForm.planId = row.id
  dispatchForm.samplingTime = row.startTime || dayjs().format('YYYY-MM-DD HH:mm:ss')
  const samplerIds = includeRequiredSamplerIds(resolveRowSamplerOptionIds(row), rowOrgId)
  ensureSelectedSamplerOptions(samplerIds, row.samplerName || row.sampler_name)
  handleDispatchSamplerChange(samplerIds)
  dispatchDialogVisible.value = true
}

async function submitDispatchForm() {
  if (dispatchSubmitting.value) {
    return
  }
  const samplerIds = includeRequiredSamplerIds(dispatchForm.samplerIds, getDispatchPlanOrgId())
  const samplerName = resolveSamplerNames(samplerIds)
  if (!dispatchForm.planId || !samplerIds.length || !samplerIds[0] || !samplerName) {
    ElMessage.warning('派发任务前必须指定采样员')
    return
  }
  dispatchSubmitting.value = true
  try {
    await dispatchSamplingPlanApi({
      planId: dispatchForm.planId,
      samplingTime: dispatchForm.samplingTime || dayjs().format('YYYY-MM-DD HH:mm:ss'),
      samplerIds,
      samplerId: samplerIds[0],
      samplerName
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
  try {
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
  } catch (error) {
    console.error('加载检测配置选项失败:', error)
  }
}

async function loadFlowOptions() {
  if (reviewFlowOptions.value.length) {
    return
  }
  const reviewOptions = await fetchFlowConfigOptionsApi({ flowType: FLOW_TYPE_REVIEW })
  reviewFlowOptions.value = Array.isArray(reviewOptions) ? reviewOptions : []
}

function findFlowOption(options, flowId) {
  return options.find((item) => String(item.id) === String(flowId))
}

function getDefaultFlowOption(options) {
  return options.find((item) => item.defaultFlag) || options[0] || null
}

function applyDefaultFlowSelections() {
  const reviewFlow = getDefaultFlowOption(reviewFlowOptions.value)
  loginForm.reviewFlowId = reviewFlow?.id || null
  loginForm.reviewFlowName = reviewFlow?.flowName || ''
}

function handleReviewFlowChange(flowId) {
  const flow = findFlowOption(reviewFlowOptions.value, flowId)
  loginForm.reviewFlowName = flow?.flowName || ''
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

function parseOptionValuesArray(json) {
  if (!json) return []
  try {
    const arr = JSON.parse(json)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

function formatStandardRange(min, max, unit, optionValues) {
  if (optionValues) {
    const options = parseOptionValuesArray(optionValues)
    if (options.length) return options.join(' / ')
  }
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

function getDetectionTypeById(typeId) {
  return detectionProjectOptions.value.find((item) => String(item.id) === String(typeId || '')) || null
}

function formatLoginDetectionTypeLabel(item) {
  if (!item) {
    return '-'
  }
  const sampleTypeLabel = item.sampleType ? getEnumLabel(sampleTypeLabelMap, item.sampleType) : '未绑定样品类型'
  return `${item.typeName || '-'} / ${sampleTypeLabel}`
}

function autoSelectLoginDetectionType() {
  if (isLoginReadonly.value) {
    return
  }
  const options = loginDetectionProjectOptions.value
  if (!options.length) {
    loginForm.detectionTypeId = null
    loginForm.detectionTypeName = ''
    loginForm.detectionItems = ''
    loginForm.detectionConfigItems = []
    return
  }
  const current = options.find((item) => String(item.id) === String(loginForm.detectionTypeId || ''))
  const selected = current || options[0]
  handleLoginDetectionTypeChange(selected.id)
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
        unit: parameter.unit || '',
        standardMin: parameter.standardMin,
        standardMax: parameter.standardMax,
        optionValues: parameter.optionValues || '',
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
    unit: item?.unit || parameter?.unit || '',
    standardMin: item?.standardMin ?? parameter?.standardMin ?? null,
    standardMax: item?.standardMax ?? parameter?.standardMax ?? null,
    optionValues: item?.optionValues || parameter?.optionValues || '',
    referenceStandard: item?.referenceStandard || parameter?.referenceStandard || '',
    methodId,
    methodName: item?.methodName || methodOptions.find((option) => option.id === methodId)?.methodName || '',
    sampleVolume: item?.sampleVolume || item?.sample_volume || methodOptions.find((option) => option.id === methodId)?.sampleVolume || '',
    methodOptions
  }
}

function parseSampleDetectionConfigSnapshot(snapshot) {
  return parseBindingJson(snapshot)
    .map((item) => buildLoginConfigRowFromSnapshot(item))
    .filter((item) => item.parameterId || item.parameterName || item.methodId || item.methodName)
}

const loginDetectionConfigRows = computed(() => loginForm.detectionConfigItems)

const loginConfigParameterIds = computed(() =>
  loginDetectionConfigRows.value.map((row) => String(row.parameterId))
)

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

function removeLoginConfigRow(index) {
  loginForm.detectionConfigItems.splice(index, 1)
}

function handleLoginMethodChange(row, methodId) {
  const method = row.methodOptions?.find((opt) => opt.id === methodId)
  if (method) {
    row.methodName = method.methodName
    row.sampleVolume = method.sampleVolume || ''
  }
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

function syncMapSelectorValue(source) {
  mapSelectorValue.pointName = source?.pointName || ''
  mapSelectorValue.address = source?.address || source?.pointName || ''
  mapSelectorValue.latitude = source?.latitude || source?.x_coordinate || ''
  mapSelectorValue.longitude = source?.longitude || source?.y_coordinate || ''
}

function openPlanMapSelector() {
  syncMapSelectorValue(planForm)
  planMapSelectorVisible.value = true
}

function confirmPlanMapSelection() {
  if (!mapSelectorValue.latitude || !mapSelectorValue.longitude) {
    ElMessage.warning('请先在地图上选择点位')
    return
  }
  planForm.address = mapSelectorValue.address
  planForm.latitude = mapSelectorValue.latitude
  planForm.longitude = mapSelectorValue.longitude
  if (planForm.pointSource === 'CUSTOM') {
    planForm.pointName = mapSelectorValue.pointName || mapSelectorValue.address || planForm.pointName
    // 自动生成计划名称：点位名称
    if (planForm.pointName && planForm.startTime) {
      planForm.planName = planForm.pointName
    }
  }
  planMapSelectorVisible.value = false
}

function openMapSelector() {
  syncMapSelectorValue({
    pointName: taskCompletePreview.value?.pointName,
    address: taskCompleteForm.address,
    latitude: taskCompleteForm.latitude,
    longitude: taskCompleteForm.longitude
  })
  mapSelectorVisible.value = true
}

function confirmMapSelection() {
  if (!mapSelectorValue.latitude || !mapSelectorValue.longitude) {
    ElMessage.warning('请先在地图上选择位置')
    return
  }
  taskCompleteForm.address = mapSelectorValue.address
  taskCompleteForm.latitude = mapSelectorValue.latitude
  taskCompleteForm.longitude = mapSelectorValue.longitude
  mapSelectorVisible.value = false
}

function openTaskDetailMap() {
  syncTaskLocationViewerValue(taskDetail.value)
  taskLocationViewerVisible.value = true
}

function syncTaskLocationViewerValue(source) {
  taskLocationViewerValue.pointName = source?.pointName || source?.point_name || ''
  taskLocationViewerValue.address = source?.address || source?.pointName || source?.point_name || ''
  taskLocationViewerValue.latitude = source?.latitude || source?.x_coordinate || ''
  taskLocationViewerValue.longitude = source?.longitude || source?.y_coordinate || ''
}

function hasCoordinates(source) {
  return Boolean(source && (source.latitude || source.x_coordinate) && (source.longitude || source.y_coordinate))
}

function formatCoordinateText(source) {
  if (!hasCoordinates(source)) {
    return ''
  }
  const longitude = source.longitude || source.y_coordinate
  const latitude = source.latitude || source.x_coordinate
  return `${longitude}, ${latitude}`
}

function formatLocationText(source) {
  if (!source) {
    return '-'
  }
  const address = source.address || source.pointName || source.point_name || ''
  const coordinateText = formatCoordinateText(source)
  if (address && coordinateText) {
    return `${address}（${coordinateText}）`
  }
  return address || coordinateText || '-'
}

function openNavigation(source) {
  if (!hasCoordinates(source)) {
    ElMessage.warning('当前任务没有可导航坐标')
    return
  }
  const longitude = source.longitude || source.y_coordinate
  const latitude = source.latitude || source.x_coordinate
  const label = encodeURIComponent(source.address || source.pointName || source.point_name || '采样点位')
  const url = `https://map.tianditu.gov.cn/?lnglat=${encodeURIComponent(`${longitude},${latitude}`)}&level=16&name=${label}`
  window.open(url, '_blank', 'noopener,noreferrer')
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
    const detail = await fetchSamplingTaskDetailApi(row.id)
    const task = detail || row
    taskCompletePreview.value = task
    clearTaskCompletePhotoPreviewUrls()
    taskCompletePhotoList.value = extractTaskPhotoUrls(task).map(buildPhotoEntry)
    taskCompleteForm.taskId = task.id
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
      weather: taskCompleteForm.weather,
      temperature: taskCompleteForm.temperature,
      sampleTotalVolume: buildSampleVolumePayload(taskCompleteForm.sampleTotalVolume),
      sampleBottleCount: taskCompleteForm.sampleBottleCount == null ? '' : String(taskCompleteForm.sampleBottleCount),
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
  loginForm.sampleSourceMethod = samplingSampleSourceMethod
  loginForm.detectionItems = ''
  loginForm.detectionTypeId = null
  loginForm.detectionTypeName = ''
  loginForm.detectionConfigItems = []
  loginForm.samplingTime = task.samplingTime || dayjs().format('YYYY-MM-DD HH:mm:ss')
  loginForm.samplerId = task.samplerId || null
  loginForm.samplerName = task.samplerName || ''
  loginForm.weather = ''
  loginForm.storageCondition = ''
  loginForm.remark = task.remark || ''
  autoSelectLoginDetectionType()
}

function resetLoginForm() {
  loginDialogMode.value = 'create'
  loginPreviewTaskLabel.value = ''
  loginForm.taskId = null
  loginForm.sampleNo = ''
  loginForm.pointId = null
  loginForm.pointName = ''
  loginForm.sampleType = ''
  loginForm.sampleSourceMethod = samplingSampleSourceMethod
  loginForm.detectionItems = ''
  loginForm.detectionTypeId = null
  loginForm.detectionTypeName = ''
  loginForm.detectionConfigItems = []
  loginForm.reviewFlowId = null
  loginForm.reviewFlowName = ''
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
    ElMessage.warning('没有可登录的任务，请先确认采样计划是否已生成任务')
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
  loginForm.sampleSourceMethod = sample.sampleSourceMethod || samplingSampleSourceMethod
  loginForm.detectionItems = parseDetectionItemsText(sample.detectionItems)
  loginForm.detectionTypeId = sample.detectionTypeId || null
  loginForm.detectionTypeName = sample.detectionTypeName || sample.detectionItems || ''
  loginForm.detectionConfigItems = parseSampleDetectionConfigSnapshot(sample.detectionConfigSnapshot)
  if (!loginForm.detectionConfigItems.length && loginForm.detectionTypeId) {
    loginForm.detectionConfigItems = buildLoginDetectionConfigItems(getDetectionTypeById(loginForm.detectionTypeId))
  }
  loginForm.reviewFlowId = sample.reviewFlowId || null
  loginForm.reviewFlowName = sample.reviewFlowName || ''
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

function handleLoginDetectionTypeChange(typeId) {
  const detectionType = getDetectionTypeById(typeId)
  loginForm.detectionTypeId = detectionType?.id || null
  loginForm.detectionTypeName = detectionType?.typeName || ''
  loginForm.detectionItems = detectionType?.typeName || ''
  loginForm.detectionConfigItems = buildLoginDetectionConfigItems(detectionType)
}

function formatPendingTaskLabel(task) {
  const planName = task?.planName || task?.pointName || '未命名任务'
  if (task?.samplingTime) {
    const date = new Date(task.samplingTime)
    const yyyy = date.getFullYear()
    const mm = String(date.getMonth() + 1).padStart(2, '0')
    const dd = String(date.getDate()).padStart(2, '0')
    const hh = String(date.getHours()).padStart(2, '0')
    return `${planName}/${yyyy}${mm}${dd}-${hh}`
  }
  return planName
}

async function submitSampleLogin() {
  if (!loginForm.taskId) {
    ElMessage.warning('没有可登录的任务，请先确认采样计划是否已生成任务')
    return
  }
  const detectionItems = parseDetectionItemsText(loginForm.detectionItems || loginForm.detectionTypeName)
  if (!loginForm.pointName || !loginForm.sampleType || !loginForm.sampleSourceMethod || !loginForm.detectionTypeId || !detectionItems || !loginForm.samplingTime || !loginForm.samplerName) {
    ElMessage.warning('请完整填写样品登录信息')
    return
  }
  if (!loginForm.reviewFlowId) {
    ElMessage.warning('请选择审核流程')
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
      detectionItems,
      detectionTypeId: loginForm.detectionTypeId,
      detectionTypeName: loginForm.detectionTypeName,
      detectionConfigItems: loginDetectionConfigRows.value.map((item) => ({
        parameterId: item.parameterId,
        parameterName: item.parameterName,
        unit: item.unit,
        standardMin: item.standardMin,
        standardMax: item.standardMax,
        optionValues: item.optionValues || '',
        referenceStandard: item.referenceStandard,
        methodId: item.methodId,
        methodName: item.methodName,
        sampleVolume: item.sampleVolume,
        methodBasis: item.methodBasis
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
  document.addEventListener('click', handlePlanHourOutsideClick)
  window.addEventListener('resize', handlePlanHourViewportChange)
  window.addEventListener('scroll', handlePlanHourViewportChange, true)
  syncRouteState()
  await Promise.all([loadCurrentSceneData(), loadSamplingDictOptions()])
  await handleRouteAutoOpen()
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handlePlanHourOutsideClick)
  window.removeEventListener('resize', handlePlanHourViewportChange)
  window.removeEventListener('scroll', handlePlanHourViewportChange, true)
})

watch(() => route.fullPath, async () => {
  syncRouteState()
  await loadCurrentSceneData()
  await handleRouteAutoOpen()
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

.login-config-panel__summary .el-button {
  margin-left: auto;
}

.login-config-panel__note {
  color: var(--text-light);
  font-size: 12px;
  line-height: 1.6;
}

.add-param-quick-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  padding: 8px 0;
}

.add-param-quick-label {
  font-size: 12px;
  color: var(--text-light);
  margin-right: 4px;
}

.add-param-popover-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  cursor: pointer;
  border-radius: 4px;
}

.add-param-popover-row:hover {
  background: var(--fill-color-light);
}

.add-param-popover-row.is-existing {
  opacity: 0.55;
  cursor: not-allowed;
}

.add-param-popover-row input[type="checkbox"] {
  width: 15px;
  height: 15px;
  cursor: pointer;
  accent-color: var(--el-color-primary);
  flex-shrink: 0;
}

.add-param-popover-row.is-existing input[type="checkbox"] {
  cursor: not-allowed;
}

.add-param-unit {
  color: var(--text-light);
  font-size: 12px;
}

.add-parameter-dialog-content {
  min-height: 200px;
  max-height: 500px;
  overflow-y: auto;
}

.add-parameter-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.add-parameter-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-lighter);
  transition: background-color 0.2s;
  cursor: pointer;
}

.add-parameter-item:last-child {
  border-bottom: none;
}

.add-parameter-item:hover {
  background-color: var(--fill-color-light);
}

.add-parameter-item.is-disabled {
  opacity: 0.6;
  background-color: var(--fill-color-lighter);
  cursor: not-allowed;
}

.add-parameter-item :deep(.el-checkbox) {
  margin-right: 0;
}

.add-parameter-checkbox {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: var(--el-color-primary);
  flex-shrink: 0;
}

.add-parameter-item.is-disabled .add-parameter-checkbox {
  cursor: not-allowed;
}

.add-parameter-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 4px;
}

.add-parameter-name {
  font-size: 14px;
  color: var(--text-regular);
}

.add-parameter-unit {
  font-size: 12px;
  color: var(--text-secondary);
}

.sample-source-field {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.sample-source-field :deep(.el-input),
.sample-source-field :deep(.el-select) {
  flex: 1 1 auto;
  min-width: 0;
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

.plan-hour-picker {
  position: relative;
  width: 100%;
}

.plan-hour-picker__trigger {
  width: 100%;
  height: 34px;
  padding: 0 12px;
  border: 1px solid #cfd9e6;
  border-radius: 6px;
  background: #ffffff;
  color: var(--text-main);
  font: inherit;
  line-height: 32px;
  text-align: left;
  cursor: pointer;
}

.plan-hour-picker__trigger:hover,
.plan-hour-picker__trigger:focus {
  border-color: var(--brand);
  outline: none;
}

.plan-hour-picker__trigger.is-placeholder {
  color: #c0c4cc;
}

.plan-hour-picker__panel {
  z-index: 4000;
  border: 1px solid #d9e3ef;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 10px 28px rgba(31, 45, 61, 0.18);
  overflow: hidden;
}

.plan-hour-picker__panel--floating {
  position: fixed;
}

.plan-hour-picker__body {
  display: grid;
  grid-template-columns: minmax(320px, 1.2fr) minmax(300px, 1fr);
  align-items: stretch;
}

.plan-hour-picker__body :deep(.el-date-picker) {
  width: 100% !important;
  box-shadow: none !important;
}

.plan-hour-picker__body :deep(.el-picker-panel__body-wrapper),
.plan-hour-picker__body :deep(.el-picker-panel__body) {
  min-width: 0;
}

.plan-hour-picker__hours {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  align-content: start;
  gap: 12px;
  padding: 18px;
  border-left: 1px solid #eef2f6;
}

.plan-hour-picker__hours > span {
  grid-column: 1 / -1;
  color: var(--text-light);
  font-size: 15px;
  line-height: 24px;
}

.plan-hour-option {
  height: 38px;
  border: 0;
  border-radius: 5px;
  background: #ffffff;
  color: var(--text-sub);
  font-size: 16px;
  line-height: 38px;
  cursor: pointer;
}

.plan-hour-option:hover {
  background: var(--brand-soft);
  color: var(--brand);
}

.plan-hour-option.is-active {
  background: color-mix(in srgb, var(--brand) 12%, #ffffff 88%);
  color: var(--brand);
  font-weight: 700;
}

@media (max-width: 760px) {
  .plan-hour-picker__body {
    grid-template-columns: 1fr;
  }

  .plan-hour-picker__hours {
    border-top: 1px solid #eef2f6;
    border-left: 0;
  }
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

.location-picker {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  width: 100%;
}

.location-picker > :only-child {
  grid-column: 1 / -1;
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

</style>
