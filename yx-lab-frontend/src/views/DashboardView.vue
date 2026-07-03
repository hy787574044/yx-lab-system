<template>
  <div class="content-grid dashboard-page" v-loading="loading">
    <section class="glass-panel workbench-hero">
      <div class="workbench-hero__title">
        <h3>化验室工作台</h3>
      </div>
      <div class="workbench-hero__flow">
        <span>流程概览</span>
        <div class="workbench-hero__flow-chips">
          <button
            v-for="item in workflowOverviewItems"
            :key="item.key"
            type="button"
            @click="openWorkbenchAction(item.action)"
          >
            {{ item.label }}
          </button>
        </div>
      </div>
      <div class="workbench-hero__stats">
        <span>
          <strong>{{ warningTotal }}</strong>
          <em>待处理预警</em>
        </span>
        <span>
          <strong>{{ latestPassRate }}%</strong>
          <em>近况合格率</em>
        </span>
      </div>
    </section>

    <section class="todo-detail-grid">
      <article
        v-for="section in workbenchSections"
        :key="section.key"
        class="glass-panel todo-detail-panel"
      >
        <div class="todo-detail-panel__head">
          <div>
            <h3>{{ section.title }}</h3>
          </div>
          <strong>{{ section.total ?? section.rows.length }}</strong>
        </div>
        <div v-if="section.rows.length" class="todo-detail-list">
          <div
            v-for="row in section.rows"
            :key="`${section.key}-${getActionRowKey(row)}`"
            role="button"
            tabindex="0"
            :class="['todo-detail-card', `todo-detail-card--${section.key}`]"
            @click="handlePreviewCardClick(section.key, row)"
            @keydown.enter.prevent="handlePreviewCardClick(section.key, row)"
            @keydown.space.prevent="handlePreviewCardClick(section.key, row)"
          >
            <div class="todo-detail-card__main">
              <span>{{ getPreviewRowTitle(section.key, row) }}</span>
              <p v-if="getPreviewDisplayRowMeta(section.key, row)">{{ getPreviewDisplayRowMeta(section.key, row) }}</p>
            </div>
            <div class="todo-detail-card__fields">
              <span v-for="field in getVisiblePreviewFields(section.key, row)" :key="field.label">
                <em>{{ field.label }}</em>
                <strong
                  v-if="field.statusType || field.statusClass"
                  class="status-chip"
                  :class="field.statusClass || getStatusClass(field.statusType, field.statusValue)"
                >
                  {{ field.value || '-' }}
                </strong>
                <strong v-else>{{ field.value || '-' }}</strong>
              </span>
            </div>
            <el-button
              v-if="section.key === 'samplingPlan'"
              v-permission="'samplingPlan:write'"
              class="todo-detail-card__action"
              type="primary"
              size="small"
              :loading="dispatchingPlanId === row.id"
              :disabled="dispatchSubmitting || !actionablePlanStatuses.includes(row.planStatus)"
              @click.stop="dispatchSamplingPlanFromWorkbench(row)"
              @keydown.stop
            >
              派发
            </el-button>
            <el-button
              v-if="section.key === 'sampling'"
              v-permission="'sample:write'"
              class="todo-detail-card__action todo-detail-card__action--center"
              type="primary"
              size="small"
              @click.stop="openWorkbenchAction('sampleLogin', row)"
              @keydown.stop
            >
              样品登录
            </el-button>
          </div>
        </div>
        <div v-else class="todo-detail-empty">暂无待处理数据</div>
      </article>
    </section>

    <el-dialog
      v-model="samplingPlanDetailVisible"
      class="sampling-plan-detail-dialog"
      title="采样计划详情"
      width="760px"
      align-center
    >
      <div v-if="selectedSamplingPlan" class="plan-detail">
        <div class="plan-detail__title">
          <strong>{{ selectedSamplingPlan.planName || '-' }}</strong>
          <span
            class="status-chip"
            :class="getStatusClass('planStatus', selectedSamplingPlan.planStatus)"
          >
            {{ getEnumLabel(planStatusLabelMap, selectedSamplingPlan.planStatus) }}
          </span>
        </div>
        <div class="plan-detail__grid">
          <div v-for="field in samplingPlanDetailFields" :key="field.label">
            <span>{{ field.label }}</span>
            <strong>{{ field.value || '-' }}</strong>
          </div>
        </div>
        <div class="plan-detail__remark">
          <span>备注</span>
          <p>{{ selectedSamplingPlan.remark || '-' }}</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="samplingPlanDetailVisible = false">关闭</el-button>
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
        <el-button
          v-permission="'samplingPlan:write'"
          type="primary"
          :loading="dispatchSubmitting"
          :disabled="dispatchSubmitting"
          @click="submitDispatchForm"
        >
          确认派发
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="planCreateDialogVisible"
      class="sampling-form-dialog sampling-plan-form-dialog"
      title="新增采样计划"
      width="1080px"
      append-to-body
      align-center
      destroy-on-close
      @closed="resetPlanCreateForm"
    >
      <el-form label-width="96px">
        <div class="plan-form-grid">
          <el-form-item label="计划名称" required>
            <el-input v-model="planCreateForm.planName" placeholder="请输入采样计划名称" />
          </el-form-item>
          <el-form-item label="点位来源">
            <el-select v-model="planCreateForm.pointSource" style="width: 100%" @change="handlePlanCreatePointSourceChange">
              <el-option label="监测点位选择" value="EXISTING" />
              <el-option label="手工填写点位" value="CUSTOM" />
            </el-select>
          </el-form-item>
          <el-form-item label="所属机构" required>
            <el-select
              v-model="planCreateForm.orgId"
              filterable
              style="width: 100%"
              placeholder="请选择所属机构"
              @change="handlePlanCreateOrgChange"
            >
              <el-option
                v-for="option in planOrgOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="planCreateForm.pointSource === 'EXISTING'" label="监测点位" required>
            <el-select
              v-model="planCreateForm.pointId"
              style="width: 100%"
              :placeholder="planCreateForm.orgId ? '请选择已创建的监测点位' : '请先选择所属机构'"
              :loading="monitoringPointLoading"
              :disabled="!planCreateForm.orgId"
              @change="handlePlanCreatePointChange"
            >
              <el-option
                v-for="point in monitoringPointOptions"
                :key="point.id"
                :label="point.pointName"
                :value="point.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="点位名称" required>
            <el-input
              v-model="planCreateForm.pointName"
              :readonly="planCreateForm.pointSource === 'EXISTING'"
              placeholder="请输入采样点位名称"
            />
          </el-form-item>
          <el-form-item label="点位坐标" required>
            <div class="location-picker">
              <el-input :model-value="formatPlanCreateCoordinateText()" readonly placeholder="请从地图选择点位" />
              <el-button v-if="planCreateForm.pointSource === 'CUSTOM'" @click="openPlanCreateMapDialog">
                {{ planCreateForm.latitude && planCreateForm.longitude ? '重新选点' : '地图选点' }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item class="plan-form-span-2" label="地图位置">
            <el-input v-model="planCreateForm.address" readonly placeholder="地图选点后自动回填" />
          </el-form-item>
          <el-form-item label="样品类型" required>
            <el-select v-model="planCreateForm.sampleType" style="width: 100%" :disabled="planCreateForm.pointSource === 'EXISTING'">
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
              v-model="planCreateForm.samplerIds"
              multiple
              collapse-tags
              collapse-tags-tooltip
              clearable
              filterable
              style="width: 100%"
              placeholder="请选择采样员，可多选"
              :loading="samplerLoading"
              @visible-change="handleSamplerDropdownVisible"
              @change="handlePlanCreateSamplerChange"
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
            <el-select v-model="planCreateForm.cycleType" style="width: 100%">
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
                :class="['plan-hour-picker__trigger', { 'is-placeholder': !getPlanCreateDateTimeText('start') }]"
                @click="togglePlanCreateHourPanel('start')"
              >
                {{ getPlanCreateDateTimeText('start') || '请选择开始时间' }}
              </button>
            </div>
          </el-form-item>
          <el-form-item label="截止时间">
            <div class="plan-hour-picker" data-plan-hour-picker>
              <button
                ref="endPlanHourTriggerRef"
                type="button"
                :class="['plan-hour-picker__trigger', { 'is-placeholder': !getPlanCreateDateTimeText('end') }]"
                @click="togglePlanCreateHourPanel('end')"
              >
                {{ getPlanCreateDateTimeText('end') || '请选择截止时间' }}
              </button>
            </div>
          </el-form-item>
          <el-form-item class="plan-form-span-2" label="备注">
            <el-input v-model="planCreateForm.remark" type="textarea" :rows="3" placeholder="可补充客户要求、执行说明等信息" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="planCreateDialogVisible = false">取消</el-button>
        <el-button v-permission="'samplingPlan:write'" type="primary" :loading="planCreateSubmitting" @click="submitPlanCreateForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="planCreateMapSelectorVisible"
      title="选择采样点位"
      width="960px"
      align-center
      append-to-body
      destroy-on-close
    >
      <TiandituPointSelector
        :model-value="planCreateMapSelectorValue"
        @update:model-value="syncPlanCreateMapSelectorValue"
      />
      <template #footer>
        <el-button @click="planCreateMapSelectorVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPlanCreateMapSelection">确认选点</el-button>
      </template>
    </el-dialog>

    <Teleport to="body">
      <div
        v-if="activePlanCreateHourPanel"
        class="plan-hour-picker__panel plan-hour-picker__panel--floating"
        data-plan-hour-picker
        :style="planCreateHourPanelStyle"
      >
        <div class="plan-hour-picker__body">
          <el-date-picker-panel
            :model-value="getPlanCreateDatePart(activePlanCreateHourPanel)"
            type="date"
            value-format="YYYY-MM-DD"
            :show-footer="false"
            @update:model-value="handlePlanCreateDateChange(activePlanCreateHourPanel, $event)"
          />
          <div class="plan-hour-picker__hours">
            <span>选择小时</span>
            <button
              v-for="item in hourOptions"
              :key="`${activePlanCreateHourPanel}-hour-${item}`"
              type="button"
              :class="['plan-hour-option', { 'is-active': getPlanCreateHourPart(activePlanCreateHourPanel) === item }]"
              @click="handlePlanCreateHourChange(activePlanCreateHourPanel, item)"
            >
              {{ item }}:00
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <el-dialog
      v-model="workbenchDialogVisible"
      class="workbench-action-dialog"
      :title="activeActionTitle"
      :width="activeAction === 'sampling' ? '1140px' : (activeAction === 'detection' || activeAction === 'detectionSplit') ? '1080px' : activeAction === 'samplingPlan' ? '900px' : '1180px'"
      align-center
      destroy-on-close
      @closed="resetWorkbenchDialog"
    >
      <div
        :class="[
          'workbench-dialog',
          {
            'workbench-dialog--sampling': activeAction === 'sampling',
            'workbench-dialog--sampling-plan': activeAction === 'samplingPlan',
            'workbench-dialog--sample-login': activeAction === 'sampleLogin',
            'workbench-dialog--detection': activeAction === 'detection',
            'workbench-dialog--detection-split': activeAction === 'detectionSplit',
            'workbench-dialog--review': activeAction === 'review',
            'workbench-dialog--report': activeAction === 'report',
            'workbench-dialog--report-half-month': activeAction === 'report' && previewData?.previewMode === 'HALF_MONTHLY_TERMINAL_TEMPLATE'
          }
        ]"
        v-loading="actionLoading"
      >
        <aside class="workbench-dialog__side">
          <div v-if="activeAction !== 'detectionSplit'" class="workbench-dialog__side-head">
            <span>{{ activeActionSideTitle }}</span>
            <strong>{{ actionSideCountLabel }}</strong>
          </div>
          <div v-if="activeAction === 'detectionSplit'" class="detection-split-tabs">
            <button
              v-for="tab in detectionSplitTabs"
              :key="tab.key"
              type="button"
              :class="['detection-split-tabs__item', { 'is-active': detectionSplitActiveTab === tab.key }]"
              @click="switchDetectionSplitTab(tab.key)"
            >
              <span>{{ tab.label }}</span>
              <strong>{{ tab.count }}</strong>
            </button>
          </div>
          <div v-if="activeAction === 'detectionSplit'" class="workbench-dialog__side-filter">
            <el-input
              v-model="detectionSplitPlanKeyword"
              clearable
              placeholder="请输入计划名称"
            />
          </div>
          <div v-if="filteredActionRows.length" class="workbench-row-list">
            <button
              v-for="row in filteredActionRows"
              :key="getActionRowKey(row)"
              type="button"
              :class="[
                'workbench-row-card',
                {
                  'is-active': isActionRowActive(row),
                  'is-selected': activeAction === 'detectionSplit' && isDetectionSplitRowSelected(row)
                }
              ]"
              @click="selectActionRow(row)"
            >
              <el-checkbox
                v-if="activeAction === 'detectionSplit'"
                class="workbench-row-card__check"
                :model-value="isDetectionSplitRowSelected(row)"
                @click.stop
                @change="(checked) => toggleDetectionSplitRow(row, checked)"
              />
              <div class="workbench-row-card__content">
                <strong>{{ getActionRowTitle(row) }}</strong>
                <span>{{ getActionRowMeta(row) }}</span>
              </div>
            </button>
          </div>
          <div v-else class="empty-block">{{ actionRows.length ? '没有匹配的计划' : '当前没有可处理数据' }}</div>
        </aside>

        <main class="workbench-dialog__main">
          <template v-if="activeAction === 'samplingPlan'">
            <div v-if="activeRow" class="workbench-detail">
              <div class="workbench-detail__title">
                <div>
                  <span>采样计划</span>
                  <strong>{{ activeRow.planName || activeRow.pointName || '-' }}</strong>
                </div>
                <span
                  class="status-chip"
                  :class="getStatusClass('planStatus', activeRow.planStatus)"
                >
                  {{ getEnumLabel(planStatusLabelMap, activeRow.planStatus) }}
                </span>
              </div>
              <div class="workbench-detail__grid">
                <div v-for="field in activeSamplingPlanDetailFields" :key="field.label">
                  <span>{{ field.label }}</span>
                  <strong>{{ field.value || '-' }}</strong>
                </div>
              </div>
              <div class="workbench-detail__remark">
                <span>备注</span>
                <p>{{ activeRow.remark || '-' }}</p>
              </div>
            </div>
            <div v-else class="empty-block">请选择左侧采样计划查看详情</div>
          </template>

          <template v-else-if="activeAction === 'sampling'">
            <el-form label-width="96px">
              <div class="form-grid sampling-entry-grid">
                <el-form-item label="任务编号">
                  <el-input :model-value="samplingTask?.taskNo || '-'" readonly />
                </el-form-item>
                <el-form-item label="样品编号">
                  <el-input :model-value="samplingTask?.sampleNo || '-'" readonly />
                </el-form-item>
                <el-form-item label="点位名称">
                  <el-input :model-value="samplingTask?.pointName || '-'" readonly />
                </el-form-item>
                <el-form-item label="采样人员">
                  <el-input :model-value="samplingTask?.samplerName || '-'" readonly />
                </el-form-item>
                <el-form-item label="样品类型">
                  <el-input :model-value="getEnumLabel(sampleTypeLabelMap, samplingTask?.sampleType) || '-'" readonly />
                </el-form-item>
                <el-form-item label="任务状态">
                  <el-input :model-value="getEnumLabel(taskStatusLabelMap, samplingTask?.taskStatus) || '-'" readonly />
                </el-form-item>
                <el-form-item label="计划时间">
                  <el-input :model-value="formatDate(samplingTask?.samplingTime) || '-'" readonly />
                </el-form-item>
                <el-form-item label="登记状态">
                  <el-input :model-value="getEnumLabel(sampleRegisterStatusLabelMap, samplingTask?.sampleRegisterStatus || unregisteredSampleRegisterStatus)" readonly />
                </el-form-item>
                <el-form-item class="form-span-2" label="备注">
                  <el-input :model-value="samplingTask?.remark || '-'" type="textarea" :rows="3" readonly />
                </el-form-item>
              </div>
            </el-form>
          </template>

          <template v-else-if="activeAction === 'sampleLogin'">
            <el-form label-width="96px">
              <div class="form-grid sample-login-grid">
                <el-form-item label="待登录任务">
                  <el-input :model-value="formatTaskLabel(loginTask)" readonly />
                </el-form-item>
                <el-form-item label="样品编号">
                  <el-input :model-value="loginForm.sampleNo || '-'" readonly />
                </el-form-item>
                <el-form-item label="点位名称">
                  <el-input :model-value="loginForm.pointName || '-'" readonly />
                </el-form-item>
                <el-form-item label="样品类型">
                  <el-input :model-value="getEnumLabel(sampleTypeLabelMap, loginForm.sampleType)" readonly />
                </el-form-item>
                <el-form-item label="样品来源">
                  <el-select v-model="loginForm.sampleSourceMethod" style="width: 100%">
                    <el-option v-for="option in sampleSourceMethodOptions" :key="option.value" :label="option.label" :value="option.value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="采样人员">
                  <el-input v-model="loginForm.samplerName" readonly />
                </el-form-item>
                <el-form-item class="form-span-2" label="检测套餐" required>
                  <el-select v-model="loginForm.detectionTypeId" filterable style="width: 100%" @change="handleLoginDetectionTypeChange">
                    <el-option
                      v-for="item in loginDetectionTypeOptions"
                      :key="item.id"
                      :label="formatDetectionTypeLabel(item)"
                      :value="item.id"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="审核流程" required>
                  <el-select v-model="loginForm.reviewFlowId" filterable style="width: 100%" @change="handleReviewFlowChange">
                    <el-option v-for="item in reviewFlowOptions" :key="item.id" :label="item.flowName" :value="item.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="采样时间" required>
                  <el-date-picker
                    v-model="loginForm.samplingTime"
                    type="datetime"
                    format="YYYY-MM-DD HH:mm:ss"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    style="width: 100%"
                  />
                </el-form-item>
                <el-form-item label="天气">
                  <el-select v-model="loginForm.weather" allow-create clearable filterable default-first-option style="width: 100%">
                    <el-option v-for="option in weatherOptions" :key="option.value" :label="option.label" :value="option.value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="保存条件">
                  <el-select v-model="loginForm.storageCondition" allow-create clearable filterable default-first-option style="width: 100%">
                    <el-option v-for="option in storageConditionOptions" :key="option.value" :label="option.label" :value="option.value" />
                  </el-select>
                </el-form-item>
              </div>
            </el-form>
            <div class="sample-login-config-header">
              <span class="sample-login-config-title">检测参数明细</span>
              <el-button type="primary" size="small" @click="openWorkbenchAddParamDialog">添加参数</el-button>
            </div>
            <el-table
              v-if="loginForm.detectionConfigItems.length"
              class="compact-table sample-login-config-table"
              :data="loginForm.detectionConfigItems"
              size="small"
              border
              max-height="190"
            >
              <el-table-column prop="parameterName" label="检测参数" min-width="140" />
              <el-table-column label="标准范围" min-width="120">
                <template #default="{ row }">{{ formatStandardRange(row.standardMin, row.standardMax, row.unit, row.optionValues) }}</template>
              </el-table-column>
              <el-table-column prop="methodName" label="检测方法" min-width="160" />
              <el-table-column prop="sampleVolume" label="取样体积" width="100" />
            </el-table>
            <div v-else class="sample-login-config-empty">请选择检测套餐后确认检测参数与检测方法明细。</div>
          </template>

          <template v-else-if="activeAction === 'detectionSplit'">
            <div v-if="activeRow" class="workbench-detail">
              <div class="workbench-detail__title">
                <div>
                  <span>检测分样</span>
                  <strong>{{ resultForm.sampleNo || '-' }}</strong>
                </div>
                <span
                  class="status-chip"
                  :class="getStatusClass('detectionStatus', resultForm.itemStatus)"
                >
                  {{ getEnumLabel(detectionStatusLabelMap, resultForm.itemStatus) }}
                </span>
              </div>
              <div class="summary-chips">
                <span>检测参数<strong>{{ resultForm.parameterName || '-' }}</strong></span>
                <span>检测人员<strong class="detector-name-text">{{ resultForm.detectorName || '-' }}</strong></span>
                <span>检测套餐<strong>{{ resultForm.detectionTypeName || '-' }}</strong></span>
              </div>
              <div class="workbench-detail__grid">
                <div><span>计划名称</span><strong>{{ resultForm.planName || '-' }}</strong></div>
                <div><span>点位名称</span><strong>{{ resultForm.pointName || '-' }}</strong></div>
                <div><span>样品类型</span><strong>{{ getEnumLabel(sampleTypeLabelMap, resultForm.sampleType) || resultForm.sampleType || '-' }}</strong></div>
                <div><span>检测方法</span><strong>{{ resultForm.methodName || '-' }}</strong></div>
                <div><span>检测标准</span><strong>{{ resultForm.referenceStandard || '-' }}</strong></div>
                <div><span>标准范围</span><strong>{{ formatStandardRange(resultForm.standardMin, resultForm.standardMax, resultForm.unit, resultForm.optionValues) }}</strong></div>
                <div><span>单位</span><strong>{{ resultForm.unit || '-' }}</strong></div>
                <div><span>检测记录</span><strong>{{ resultForm.recordId || '-' }}</strong></div>
              </div>
              <div class="workbench-detail__remark">
                <span>检测步骤</span>
                <p class="preserve-line-breaks">{{ resultForm.methodBasis || '-' }}</p>
              </div>
              <div class="workbench-detail__remark">
                <span>备注</span>
                <p>{{ resultForm.remark || '-' }}</p>
              </div>
            </div>
            <div v-else class="empty-block">请选择左侧检测分样查看详情</div>
          </template>

          <template v-else-if="activeAction === 'detection'">
            <div class="summary-chips">
              <span>样品编号<strong>{{ resultForm.sampleNo || '-' }}</strong></span>
              <span>检测参数<strong>{{ resultForm.parameterName || '-' }}</strong></span>
              <span>检测人员<strong class="detector-name-text">{{ resultForm.detectorName || '-' }}</strong></span>
            </div>
            <div class="meta-grid">
              <div><span>检测方法</span><strong>{{ resultForm.methodName || '-' }}</strong></div>
              <div><span>检测标准</span><strong>{{ resultForm.referenceStandard || '-' }}</strong></div>
              <div><span>标准范围</span><strong>{{ formatStandardRange(resultForm.standardMin, resultForm.standardMax, resultForm.unit, resultForm.optionValues) }}</strong></div>
              <div><span>单位</span><strong>{{ resultForm.unit || '-' }}</strong></div>
            </div>
            <div class="detection-step-row">
              <span>检测步骤</span>
              <p class="preserve-line-breaks">{{ resultForm.methodBasis || '-' }}</p>
            </div>
            <el-form label-position="top">
              <el-form-item label="检测结果" required :class="{ 'is-abnormal': isResultValueAbnormal(resultForm) }">
                <div class="result-value-field">
                  <el-button
                    class="ocr-trigger-btn"
                    @click="handleOcrTrigger"
                  >
                    OCR识别
                  </el-button>
                  <el-select
                    v-if="resultForm.optionValues"
                    v-model="resultForm.resultValue"
                    :class="['result-value-input', 'result-value-select', { 'is-abnormal': isResultValueAbnormal(resultForm) }]"
                    placeholder="请选择"
                  >
                    <el-option
                      v-for="(label, index) in parseOptionValuesArray(resultForm.optionValues)"
                      :key="index"
                      :label="label"
                      :value="String(index)"
                    />
                  </el-select>
                  <el-input
                    v-else
                    v-model="resultForm.resultValue"
                    inputmode="decimal"
                    :class="['result-value-input', { 'is-abnormal': isResultValueAbnormal(resultForm) }]"
                    @input="handleDetectionResultInput"
                  />
                  <span v-if="resultForm.unit" class="result-value-unit">{{ resultForm.unit }}</span>
                </div>
              </el-form-item>
              <el-form-item class="detection-textarea-item" label="异常说明">
                <el-input v-model="resultForm.abnormalRemark" type="textarea" :rows="3" />
              </el-form-item>
              <el-form-item class="detection-textarea-item" label="备注">
                <el-input v-model="resultForm.remark" type="textarea" :rows="3" />
              </el-form-item>
            </el-form>
          </template>

          <template v-else-if="activeAction === 'review'">
            <div class="summary-chips">
              <span>样品编号<strong>{{ reviewForm.sampleNo || '-' }}</strong></span>
              <span>检测套餐<strong>{{ reviewForm.detectionTypeName || '-' }}</strong></span>
              <span>待审核<strong>{{ pendingReviewItemCount }}</strong></span>
            </div>
            <div class="review-toolbar">
              <el-button type="primary" @click="setAllReviewResult(approvedReviewResult)">一键通过</el-button>
              <el-button type="danger" plain @click="rejectAllReviewItems">一键驳回</el-button>
            </div>
            <el-table class="compact-table" :data="reviewForm.items" size="small" border max-height="430">
              <el-table-column prop="parameterName" label="检测参数" min-width="130" />
              <el-table-column prop="methodName" label="检测方法" min-width="150" />
              <el-table-column label="标准范围" min-width="130">
                <template #default="{ row }">{{ formatStandardRange(row.standardMin, row.standardMax, row.unit, row.optionValues) }}</template>
              </el-table-column>
              <el-table-column prop="unit" label="单位" width="90" />
              <el-table-column label="检测值" width="100">
                <template #default="{ row }">
                  <span v-if="row.optionValues">{{ (parseOptionValuesArray(row.optionValues)[row.resultValue] || row.resultValue) ?? '-' }}</span>
                  <span v-else>{{ row.resultValue }}</span>
                </template>
              </el-table-column>
              <el-table-column label="审核状态" width="100">
                <template #default="{ row }">
                  <span class="review-state-chip" :class="getReviewDraftStatusClass(row)">
                    {{ getReviewDraftStatusLabel(row) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="审核操作" min-width="180">
                <template #default="{ row }">
                  <el-button size="small" :type="row.reviewResultDraft === approvedReviewResult ? 'primary' : 'default'" @click="approveReviewItem(row)">通过</el-button>
                  <el-button size="small" :type="row.reviewResultDraft === rejectedReviewResult ? 'danger' : 'default'" @click="rejectReviewItem(row)">驳回</el-button>
                </template>
              </el-table-column>
              <el-table-column label="驳回原因" min-width="220" class-name="review-reject-reason-cell">
                <template #default="{ row }">
                  <span class="review-reject-reason">{{ row.reviewResultDraft === rejectedReviewResult ? (row.rejectReasonDraft || '-') : '-' }}</span>
                </template>
              </el-table-column>
            </el-table>
            <el-form label-position="top" class="review-remark-form">
              <el-form-item label="整单审核意见">
                <el-input v-model="reviewForm.reviewRemark" type="textarea" :rows="3" />
              </el-form-item>
            </el-form>
          </template>

          <template v-else-if="activeAction === 'report'">
            <div v-if="previewError" class="empty-block">{{ previewError }}</div>
            <div v-else-if="previewData && previewComponent" class="workbench-report-preview">
              <div class="workbench-report-preview__scale">
                <component
                  :is="previewComponent"
                  ref="workbenchReportPrintRef"
                  :preview-data="previewData"
                />
              </div>
            </div>
            <div v-else-if="previewData" class="empty-block">暂无可预览模板</div>
            <div v-else class="empty-block">请选择左侧报告进行预览</div>
          </template>
        </main>
      </div>

      <template #footer>
        <div class="workbench-dialog__footer">
          <div class="workbench-dialog__footer-left">
            <el-button v-if="activeAction === 'samplingPlan'" v-permission="'samplingPlan:write'" type="primary" plain @click="openPlanCreateDialog">新增计划</el-button>
          </div>
          <div class="workbench-dialog__footer-right">
            <el-button v-if="activeAction === 'sampling'" type="primary" @click="openWorkbenchAction('sampleLogin', activeRow)">样品登录</el-button>
            <el-button
              v-if="activeAction === 'detectionSplit'"
              v-permission="'detection:assign'"
              type="primary"
              :disabled="!detectorAssignmentTargetRows.length"
              @click="openDetectorAssignDialog"
            >
              {{ selectedDetectionSplitRows.length ? `分配检测人员 (${selectedDetectionSplitRows.length})` : '分配检测人员' }}
            </el-button>
            <el-button v-if="activeAction === 'report'" type="primary" plain :disabled="!previewData || !previewComponent" @click="printWorkbenchReport">打印</el-button>
            <el-button @click="workbenchDialogVisible = false">关闭</el-button>
            <el-button v-if="activeAction === 'sampleLogin'" type="primary" :loading="submitting" @click="submitSampleLogin">登录</el-button>
            <el-button v-if="activeAction === 'detection'" type="primary" :loading="submitting" @click="submitDetectionResult">提交</el-button>
            <el-button v-if="activeAction === 'review'" type="primary" :loading="submitting" @click="submitReviewDecision">提交</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="detectorAssignDialogVisible"
      class="detector-assign-dialog"
      title="分配检测人员"
      width="680px"
      align-center
      append-to-body
      destroy-on-close
      @closed="resetDetectorAssignDialog"
    >
      <div class="detector-assign-form">
        <div class="detector-assign-form__summary">
          <span>分配数量<strong>{{ detectorAssignmentTargetRows.length }}</strong></span>
          <span>当前点位<strong>{{ getDetectorAssignSummaryTitle() }}</strong></span>
          <span>当前人员<strong>{{ getDetectorAssignSummaryDetector() }}</strong></span>
        </div>
        <el-form class="detector-assign-form__selector" label-width="96px">
          <el-form-item label="所属机构">
            <el-select
              v-model="detectorAssignForm.orgId"
              filterable
              clearable
              style="width: 100%"
              placeholder="请选择所属机构"
              @change="handleDetectorAssignOrgChange"
            >
              <el-option v-for="option in planOrgOptions" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="检测人员" required>
            <el-select
              v-model="detectorAssignForm.detectorId"
              filterable
              clearable
              :loading="detectorOptionsLoading"
              style="width: 100%"
              placeholder="请选择检测人员"
            >
              <el-option
                v-for="item in detectorOptions"
                :key="item.id"
                :label="getDetectorOptionLabel(item)"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button :disabled="detectorAssignSubmitting" @click="detectorAssignDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="detectorAssignSubmitting"
          :disabled="detectorAssignSubmitting"
          @click="submitDetectorAssignment"
        >
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 添加检测参数对话框 -->
    <el-dialog
      v-model="workbenchAddParamDialogVisible"
      title="添加检测参数"
      width="600px"
      append-to-body
      destroy-on-close
      @open="handleWorkbenchAddParamDialogOpen"
    >
      <div v-loading="workbenchAddParamLoading" class="add-parameter-dialog-content">
        <div v-if="workbenchAddParamOptions.length" class="add-parameter-list">
          <div
            v-for="item in workbenchAddParamOptions"
            :key="item.id"
            :class="['add-parameter-item', { 'is-disabled': isWorkbenchParamInCurrentList(item.id) }]"
          >
            <el-checkbox
              :model-value="workbenchAddParamSelectedIds.includes(String(item.id))"
              :disabled="isWorkbenchParamInCurrentList(item.id)"
              @change="(val) => handleWorkbenchParamCheckChange(String(item.id), val)"
            />
            <div class="add-parameter-info">
              <span class="add-parameter-name">{{ item.parameterName }}</span>
              <span v-if="item.unit" class="add-parameter-unit">({{ item.unit }})</span>
            </div>
            <el-tag v-if="isWorkbenchParamInCurrentList(item.id)" size="small" type="info">已在列表中</el-tag>
          </div>
        </div>
        <el-empty v-else description="暂无可添加的检测参数" />
      </div>
      <template #footer>
        <el-button @click="workbenchAddParamDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!workbenchNewSelectedParamCount" @click="confirmWorkbenchAddParamDialog">
          确认 ({{ workbenchNewSelectedParamCount }})
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElCheckbox } from 'element-plus/es/components/checkbox/index.mjs'
import { ElDatePicker } from 'element-plus/es/components/date-picker/index.mjs'
import { ElDatePickerPanel } from 'element-plus/es/components/date-picker-panel/index.mjs'
import { ElDialog } from 'element-plus/es/components/dialog/index.mjs'
import { ElEmpty } from 'element-plus/es/components/empty/index.mjs'
import { ElForm, ElFormItem } from 'element-plus/es/components/form/index.mjs'
import { ElInput } from 'element-plus/es/components/input/index.mjs'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
import { ElTag } from 'element-plus/es/components/tag/index.mjs'
import { ElLoadingDirective } from 'element-plus/es/components/loading/index.mjs'
import {
  assignDetectionDetectorsApi,
  createSamplingPlanApi,
  dispatchSamplingPlanApi,
  fetchDetectionDetailApi,
  fetchDetectionDetectorsApi,
  fetchDetectionItemsApi,
  fetchDetectionMethodOptionsApi,
  fetchDetectionParametersApi,
  fetchDetectionsApi,
  fetchDetectionTypesApi,
  fetchDictItemsApi,
  fetchFlowConfigOptionsApi,
  fetchMonitoringPointOrgOptionsApi,
  fetchMonitoringPointsApi,
  fetchSamplingPlansApi,
  fetchSamplingTaskDetailApi,
  fetchSamplingTasksApi,
  fetchSummaryReportsApi,
  fetchSystemUsersApi,
  leaderDashboardApi,
  loginSampleApi,
  previewSummaryReportApi,
  submitDetectionApi,
  submitReviewApi
} from '../api/lab'
import DailyExternalSummaryTemplate from '../components/report/DailyExternalSummaryTemplate.vue'
import DailyInternalSummaryTemplate from '../components/report/DailyInternalSummaryTemplate.vue'
import HalfMonthlyTerminalSummaryTemplate from '../components/report/HalfMonthlyTerminalSummaryTemplate.vue'
import TiandituPointSelector from '../components/TiandituPointSelector.vue'
import WeeklyFactorySummaryTemplate from '../components/report/WeeklyFactorySummaryTemplate.vue'
import {
  approvedDetectionStatus,
  approvedReviewResult,
  activePlanStatus,
  actionablePlanStatuses,
  cycleTypeLabelMap,
  cycleTypeOptions,
  detectionStatusLabelMap,
  dailyCycleType,
  getEnumLabel,
  getStatusClass,
  planStatusLabelMap,
  rejectedDetectionStatus,
  rejectedReviewResult,
  reviewPendingDetectionStatus,
  sampleRegisterStatusLabelMap,
  sampleTypeOptions,
  sampleSourceMethodOptions,
  sampleTypeLabelMap,
  samplingTypeLabelMap,
  samplingSampleSourceMethod,
  routineSamplingType,
  taskStatusLabelMap,
  unregisteredSampleRegisterStatus,
  waitAssignDetectionStatus,
  waitDetectDetectionStatus
} from '../utils/labEnums'
import { getUser } from '../utils/auth'
import { hasPermission } from '../utils/permission'
import { isStaffRole } from '../utils/menuPermission'

const FLOW_TYPE_REVIEW = 'REVIEW'
const SUMMARY_TYPE_DAILY = 'DAILY'
const SUMMARY_TYPE_WEEKLY = 'WEEKLY'
const SUMMARY_TYPE_HALF_MONTHLY = 'HALF_MONTHLY'
const YANZHEN_WATER_PLANT_NAME = '\u6cbf\u9547\u6c34\u5382'
const STAFF_ROLE_CODE = 'STAFF'
const DIRECTOR_ROLE_CODE = 'DIRECTOR'
const router = useRouter()
const vLoading = ElLoadingDirective
const loading = ref(false)
const actionLoading = ref(false)
const submitting = ref(false)
const dispatchSubmitting = ref(false)
const dispatchingPlanId = ref(null)
const planCreateSubmitting = ref(false)
const activePlanCreateHourPanel = ref('')
const startPlanHourTriggerRef = ref(null)
const endPlanHourTriggerRef = ref(null)
const planCreateHourPanelStyle = ref({})
const hourOptions = Array.from({ length: 24 }, (_, index) => String(index).padStart(2, '0'))
const currentUser = ref(getUser() || {})
const dispatchDialogVisible = ref(false)
const planCreateDialogVisible = ref(false)
const planCreateMapSelectorVisible = ref(false)
const dashboard = ref({})
const activeAction = ref('')
const workbenchDialogVisible = ref(false)
const samplingPlanDetailVisible = ref(false)
const actionRows = ref([])
const activeRow = ref(null)
const detectionSplitPlanKeyword = ref('')
const detectionSplitActiveTab = ref('waitAssign')
const selectedDetectionSplitRowKeys = ref([])
const selectedSamplingPlan = ref(null)
const planOrgOptions = ref([])
const monitoringPointOptions = ref([])
const monitoringPointLoading = ref(false)
const samplerOptions = ref([])
const samplerOptionsOrgId = ref('')
const samplerLoading = ref(false)
const weatherOptions = ref([])
const storageConditionOptions = ref([])
const detectionTypes = ref([])
const detectionParameters = ref([])
const detectionMethods = ref([])
const detectorOptions = ref([])
const detectorOptionsLoaded = ref(false)
const detectorOptionsOrgId = ref('')
const detectorOptionsLoading = ref(false)
const reviewFlowOptions = ref([])
const workbenchAddParamDialogVisible = ref(false)
const workbenchAddParamLoading = ref(false)
const workbenchAddParamOptions = ref([])
const workbenchAddParamSelectedIds = ref([])
const detectorAssignDialogVisible = ref(false)
const detectorAssignSubmitting = ref(false)
const previewData = ref(null)
const previewError = ref('')
const workbenchReportPrintRef = ref(null)
const previewRows = reactive({
  samplingPlan: [],
  sampling: [],
  sampleLogin: [],
  detection: [],
  review: [],
  report: []
})
const previewTotals = reactive({
  samplingPlan: 0,
  sampling: 0,
  sampleLogin: 0,
  detection: 0,
  review: 0,
  report: 0
})
const detectionSplitCounts = reactive({
  waitAssign: 0,
  waitDetect: 0
})

const planCreateMapSelectorValue = reactive({
  pointName: '',
  address: '',
  latitude: '',
  longitude: ''
})

const samplingTask = ref(null)
const loginTask = ref(null)

const dispatchForm = reactive({
  planId: null,
  orgId: '',
  samplingTime: '',
  samplerIds: [],
  samplerId: null,
  samplerName: ''
})

const planCreateForm = reactive({
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

const resultForm = reactive({
  id: null,
  recordId: null,
  sampleId: null,
  sampleNo: '',
  planName: '',
  pointName: '',
  sampleType: '',
  detectionTypeId: null,
  detectionTypeName: '',
  parameterId: null,
  parameterName: '',
  methodName: '',
  methodBasis: '',
  standardMin: null,
  standardMax: null,
  unit: '',
  referenceStandard: '',
  detectorName: '',
  detectorId: null,
  resultValue: null,
  abnormalRemark: '',
  remark: '',
  itemStatus: '',
  optionValues: ''
})

const detectorAssignForm = reactive({
  orgId: '',
  detectorId: null
})

const detectionSplitTabs = computed(() => [
  { key: 'waitAssign', label: '待处理列表', count: detectionSplitCounts.waitAssign },
  { key: 'waitDetect', label: '待检测列表', count: detectionSplitCounts.waitDetect }
])

async function switchDetectionSplitTab(tabKey) {
  if (detectionSplitActiveTab.value === tabKey) {
    return
  }
  detectionSplitActiveTab.value = tabKey
  detectionSplitPlanKeyword.value = ''
  selectedDetectionSplitRowKeys.value = []
  actionLoading.value = true
  try {
    await loadActionRows('detectionSplit')
    if (actionRows.value.length) {
      await selectActionRow(actionRows.value[0])
    } else {
      activeRow.value = null
    }
  } finally {
    actionLoading.value = false
  }
}

async function loadDetectionSplitCounts() {
  const [waitAssignResult, waitDetectResult] = await Promise.all([
    fetchDetectionItemsApi({ pageNum: 1, pageSize: 1, itemStatus: waitAssignDetectionStatus }),
    fetchDetectionItemsApi({ pageNum: 1, pageSize: 1, itemStatus: waitDetectDetectionStatus })
  ])
  detectionSplitCounts.waitAssign = Number(waitAssignResult.total || 0)
  detectionSplitCounts.waitDetect = Number(waitDetectResult.total || 0)
}

const filteredActionRows = computed(() => {
  if (activeAction.value !== 'detectionSplit') {
    return actionRows.value
  }
  const keyword = detectionSplitPlanKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return actionRows.value
  }
  return actionRows.value.filter((row) => getDetectionSplitPlanSearchText(row).includes(keyword))
})

const actionSideCountLabel = computed(() => {
  if (activeAction.value === 'detectionSplit' && detectionSplitPlanKeyword.value.trim()) {
    return `${filteredActionRows.value.length}/${actionRows.value.length}`
  }
  return actionRows.value.length
})

const selectedDetectionSplitRows = computed(() => {
  const selectedKeys = new Set(selectedDetectionSplitRowKeys.value.map((key) => String(key)))
  return actionRows.value.filter((row) => selectedKeys.has(String(getActionRowKey(row))))
})

const detectorAssignmentTargetRows = computed(() => {
  if (activeAction.value !== 'detectionSplit') {
    return []
  }
  if (selectedDetectionSplitRows.value.length) {
    return selectedDetectionSplitRows.value
  }
  return activeRow.value ? [activeRow.value] : []
})

const reviewForm = reactive({
  detectionRecordId: null,
  sampleNo: '',
  detectionTypeName: '',
  reviewRemark: '',
  items: []
})

const staffWorkbenchSectionKeys = new Set(['sampling', 'detection', 'report'])
const hiddenAdminWorkbenchSectionKeys = new Set(['samplingPlan', 'sampleLogin'])
const workbenchSections = computed(() => {
  const sections = [
    {
      key: 'samplingPlan',
      title: '采样计划',
      rows: previewRows.samplingPlan,
      total: previewTotals.samplingPlan
    },
    {
      key: 'sampling',
      title: '采样任务',
      rows: previewRows.sampling,
      total: previewTotals.sampling
    },
    {
      key: 'sampleLogin',
      title: '样品登录',
      rows: previewRows.sampleLogin,
      total: previewTotals.sampleLogin
    },
    {
      key: 'detection',
      title: '检测录入',
      rows: previewRows.detection,
      total: previewTotals.detection
    },
    {
      key: 'review',
      title: '结果审核',
      rows: previewRows.review,
      total: previewTotals.review
    },
    {
      key: 'report',
      title: '报告处理',
      rows: previewRows.report,
      total: previewTotals.report
    }
  ]
  if (isStaffRole(currentUser.value)) {
    return sections.filter((section) => staffWorkbenchSectionKeys.has(section.key))
  }
  return sections.filter((section) => !hiddenAdminWorkbenchSectionKeys.has(section.key))
})
const workflowOverviewItems = computed(() => {
  const defaultItems = [
    { key: 'samplingPlan', label: '采样计划', action: 'samplingPlan', permission: 'samplingPlan:view' },
    { key: 'sampling', label: '采样任务', action: 'sampling', permission: 'samplingTask:view' },
    { key: 'sampleLogin', label: '样品登录', action: 'sampleLogin', permission: 'sample:view' },
    { key: 'detectionSplit', label: '检测分样', action: 'detectionSplit', permission: 'detection:view' },
    { key: 'detection', label: '化验检测', action: 'detection', permission: 'detection:view' },
    { key: 'review', label: '结果审查', action: 'review', permission: 'review:view' },
    { key: 'report', label: '生成报告', action: 'report', permission: 'report:view' }
  ]
  // 根据权限过滤
  const filteredItems = defaultItems.filter((item) => hasPermission(item.permission))
  const processNodes = dashboard.value.processNodes || []
  if (!processNodes.length) {
    return filteredItems
  }
  return filteredItems.map((item) => {
    const matched = processNodes.find((node) => node.label === item.label)
    return matched ? { ...item, label: matched.label || item.label } : item
  })
})
const warningTotal = computed(() => (dashboard.value.warnings || []).reduce((sum, item) => sum + toSafeNumber(item.count), 0))
const latestPassRate = computed(() => {
  const trendItems = dashboard.value.passRateTrend || []
  const latest = trendItems[trendItems.length - 1]
  return toSafeNumber(latest?.value).toFixed(0)
})
const activeActionTitle = computed(() => ({
  samplingPlan: '采样计划',
  sampling: '采样任务',
  sampleLogin: '样品登录',
  detectionSplit: '检测分样',
  detection: '检测结果录入',
  review: '结果审核',
  report: '报告处理'
}[activeAction.value] || '快捷处理'))
const activeActionSideTitle = computed(() => {
  if (activeAction.value === 'samplingPlan') {
    return '计划列表'
  }
  if (activeAction.value === 'report') {
    return '报告列表'
  }
  return '待处理列表'
})
const loginDetectionTypeOptions = computed(() => {
  const sampleType = String(loginForm.sampleType || '').trim()
  if (!sampleType) {
    return detectionTypes.value
  }
  return detectionTypes.value.filter((item) => !item.sampleType || String(item.sampleType) === sampleType)
})
const pendingReviewItemCount = computed(() => reviewForm.items.filter((item) => item.itemStatus === reviewPendingDetectionStatus).length)
const previewComponent = computed(() => resolveSummaryPreviewComponent(previewData.value?.previewMode))
const samplingPlanDetailFields = computed(() => {
  const plan = selectedSamplingPlan.value || {}
  return buildSamplingPlanDetailFields(plan)
})
const activeSamplingPlanDetailFields = computed(() => buildSamplingPlanDetailFields(activeRow.value || {}))

function buildSamplingPlanDetailFields(plan) {
  return [
    { label: '点位名称', value: plan.pointName },
    { label: '所属地址', value: plan.address },
    { label: '采样人员', value: plan.samplerName },
    { label: '样品类型', value: getEnumLabel(sampleTypeLabelMap, plan.sampleType) },
    { label: '检测套餐', value: plan.detectionTypeName },
    { label: '采样周期', value: getEnumLabel(cycleTypeLabelMap, plan.cycleType) },
    { label: '开始时间', value: formatDate(plan.startTime) },
    { label: '结束时间', value: formatDate(plan.endTime) },
    { label: '坐标', value: formatPlanCoordinate(plan) },
    { label: '采样方式', value: getEnumLabel(samplingTypeLabelMap, plan.samplingType) }
  ]
}

function toSafeNumber(value) {
  const num = typeof value === 'number' ? value : Number.parseFloat(String(value ?? '').replace(/,/g, '').trim())
  return Number.isFinite(num) ? num : 0
}

function normalizeDictOptions(items) {
  return (items || []).map((item) => ({
    label: item.itemName || item.label || item.value || item.itemValue,
    value: item.itemValue || item.value || item.itemName || item.label
  })).filter((item) => item.value)
}

function normalizeOrgOption(item) {
  const value = item?.value ?? item?.id ?? item?.orgId
  return {
    label: item?.label || item?.orgName || item?.name || String(value || ''),
    value: value == null ? '' : String(value)
  }
}

function formatPlanCoordinate(plan) {
  if (!plan?.longitude && !plan?.latitude) {
    return ''
  }
  return `${plan.longitude || '-'}, ${plan.latitude || '-'}`
}

function toStableId(value) {
  if (value === null || value === undefined || value === '' || value === 'null' || value === 'undefined') {
    return ''
  }
  return String(value).trim()
}

function normalizeSamplerIdList(value) {
  const rawItems = Array.isArray(value)
    ? value
    : String(value || '')
      .split(',')
      .map((item) => item.trim())
  return rawItems
    .map(toStableId)
    .filter((item) => item && item !== '0')
    .filter((item, index, source) => source.indexOf(item) === index)
}

function getSamplerOptionId(item) {
  return toStableId(item?.id ?? item?.userId ?? item?.user_id)
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
  const normalizedOrgId = toStableId(orgId)
  if (!normalizedOrgId) {
    return false
  }
  const option = planOrgOptions.value.find((item) => String(item.value) === normalizedOrgId)
  return String(option?.label || '').trim() === YANZHEN_WATER_PLANT_NAME
}

function isSamplerCandidateForOrg(item, orgId) {
  const roleCode = getRoleCode(item)
  if (roleCode === STAFF_ROLE_CODE) {
    return true
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

function resetDispatchForm() {
  dispatchForm.planId = null
  dispatchForm.orgId = ''
  dispatchForm.samplingTime = ''
  dispatchForm.samplerIds = []
  dispatchForm.samplerId = null
  dispatchForm.samplerName = ''
}

function resetPlanCreateForm() {
  planCreateForm.planName = ''
  planCreateForm.pointSource = 'CUSTOM'
  planCreateForm.orgId = ''
  planCreateForm.pointId = null
  planCreateForm.pointName = ''
  planCreateForm.address = ''
  planCreateForm.latitude = ''
  planCreateForm.longitude = ''
  planCreateForm.startTime = ''
  planCreateForm.endTime = ''
  planCreateForm.samplerIds = []
  planCreateForm.samplerId = null
  planCreateForm.samplerName = ''
  planCreateForm.samplingType = routineSamplingType
  planCreateForm.sampleType = ''
  planCreateForm.cycleType = dailyCycleType
  planCreateForm.remark = ''
  planCreateMapSelectorVisible.value = false
  syncPlanCreateMapSelectorValue(null)
}

function handlePlanCreatePointSourceChange(value) {
  if (value === 'CUSTOM') {
    planCreateForm.orgId = ''
    planCreateForm.pointId = null
    planCreateForm.pointName = ''
    planCreateForm.address = ''
    planCreateForm.latitude = ''
    planCreateForm.longitude = ''
    planCreateForm.sampleType = ''
    monitoringPointOptions.value = []
    return
  }
  planCreateForm.pointId = null
  planCreateForm.pointName = ''
  planCreateForm.address = ''
  planCreateForm.latitude = ''
  planCreateForm.longitude = ''
  planCreateForm.sampleType = ''
  monitoringPointOptions.value = []
  if (planCreateForm.orgId) {
    loadMonitoringPoints(planCreateForm.orgId)
  }
}

function handlePlanCreatePointChange(pointId) {
  const point = monitoringPointOptions.value.find((item) => item.id === pointId)
  planCreateForm.pointId = point?.id || null
  planCreateForm.pointName = point?.pointName || ''
  planCreateForm.address = point?.address || point?.pointName || ''
  planCreateForm.latitude = point?.latitude || ''
  planCreateForm.longitude = point?.longitude || ''
  planCreateForm.sampleType = point?.pointType || ''
}

function formatPlanCreateCoordinateText() {
  if (planCreateForm.latitude && planCreateForm.longitude) {
    return `${planCreateForm.latitude}, ${planCreateForm.longitude}`
  }
  return ''
}

function syncPlanCreateMapSelectorValue(source) {
  planCreateMapSelectorValue.pointName = source?.pointName || ''
  planCreateMapSelectorValue.address = source?.address || source?.pointName || ''
  planCreateMapSelectorValue.latitude = source?.latitude || source?.x_coordinate || ''
  planCreateMapSelectorValue.longitude = source?.longitude || source?.y_coordinate || ''
}

function openPlanCreateMapDialog() {
  syncPlanCreateMapSelectorValue(planCreateForm)
  planCreateMapSelectorVisible.value = true
}

function confirmPlanCreateMapSelection() {
  if (!planCreateMapSelectorValue.latitude || !planCreateMapSelectorValue.longitude) {
    ElMessage.warning('请先在地图上选择点位')
    return
  }
  planCreateForm.address = planCreateMapSelectorValue.address || planCreateMapSelectorValue.pointName || ''
  planCreateForm.latitude = planCreateMapSelectorValue.latitude
  planCreateForm.longitude = planCreateMapSelectorValue.longitude
  if (planCreateForm.pointSource === 'CUSTOM') {
    const pointName = planCreateMapSelectorValue.pointName || planCreateMapSelectorValue.address || planCreateForm.pointName
    planCreateForm.pointName = pointName
    if (pointName && (!String(planCreateForm.planName || '').trim() || planCreateForm.planName === '采样计划')) {
      planCreateForm.planName = pointName
    }
  }
  planCreateMapSelectorVisible.value = false
}

function handleDispatchSamplerChange(userIds) {
  const ids = normalizeSamplerIdList(userIds)
  dispatchForm.samplerIds = ids
  dispatchForm.samplerId = ids[0] || null
  dispatchForm.samplerName = resolveSamplerNames(ids)
}

function handlePlanCreateSamplerChange(userIds) {
  const ids = normalizeSamplerIdList(userIds)
  planCreateForm.samplerIds = ids
  planCreateForm.samplerId = ids[0] || null
  planCreateForm.samplerName = resolveSamplerNames(ids)
}

async function loadPlanOrgOptions() {
  if (planOrgOptions.value.length) {
    return
  }
  const result = await fetchMonitoringPointOrgOptionsApi()
  planOrgOptions.value = Array.isArray(result)
    ? result.map(normalizeOrgOption).filter((item) => item.value)
    : []
}

async function loadSamplers(force = false, orgId = '') {
  const normalizedOrgId = orgId == null ? '' : String(orgId)
  if (!force && samplerOptions.value.length && samplerOptionsOrgId.value === normalizedOrgId) {
    return
  }
  samplerLoading.value = true
  try {
    const params = {
      pageNum: 1,
      pageSize: 500,
      status: 1
    }
    if (normalizedOrgId) {
      params.orgId = normalizedOrgId
    }
    if (!normalizedOrgId || !isYanzhenOrgId(normalizedOrgId)) {
      params.roleCode = STAFF_ROLE_CODE
    }
    const result = await fetchSystemUsersApi(params)
    const records = Array.isArray(result.records) ? result.records : []
    samplerOptions.value = dedupeSamplerOptions(
      filterSamplerCandidates(records, normalizedOrgId).map(normalizeSamplerOption).filter((item) => item.id)
    )
    samplerOptionsOrgId.value = normalizedOrgId
  } finally {
    samplerLoading.value = false
  }
}

function handleSamplerDropdownVisible(visible) {
  if (visible) {
    loadSamplers(true, planCreateDialogVisible.value ? planCreateForm.orgId : '')
  }
}

async function loadMonitoringPoints(orgId) {
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
      pointStatus: 'ENABLED'
    })
    monitoringPointOptions.value = result.records || []
  } finally {
    monitoringPointLoading.value = false
  }
}

async function handlePlanCreateOrgChange(orgId) {
  planCreateForm.pointId = null
  planCreateForm.pointName = ''
  planCreateForm.address = ''
  planCreateForm.latitude = ''
  planCreateForm.longitude = ''
  planCreateForm.sampleType = ''
  planCreateForm.samplerIds = []
  planCreateForm.samplerId = null
  planCreateForm.samplerName = ''
  samplerOptions.value = []
  samplerOptionsOrgId.value = ''
  monitoringPointOptions.value = []
  if (orgId) {
    await loadMonitoringPoints(orgId)
    await loadSamplers(true, orgId)
    handlePlanCreateSamplerChange(samplerOptions.value.map((item) => getSamplerOptionId(item)))
  }
}

function nowDateTimeText() {
  return formatDateTimeText(new Date())
}

function formatDateTimeText(date) {
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

async function refreshDashboard() {
  dashboard.value = await leaderDashboardApi() || {}
}

async function loadWorkbenchPreviewRows() {
  const previewPageSize = 12
  const [planResult, samplingResult, loginResult, detectionResult, reviewResult, summaryReportRows] = await Promise.all([
    fetchSamplingPlansApi({ pageNum: 1, pageSize: previewPageSize, planStatus: activePlanStatus }),
    fetchSamplingTasksApi({ pageNum: 1, pageSize: previewPageSize, sampleRegisterStatus: 'UNREGISTERED' }),
    fetchSamplingTasksApi({ pageNum: 1, pageSize: previewPageSize, sampleRegisterStatus: 'UNREGISTERED' }),
    loadWorkbenchDetectionRows(previewPageSize),
    fetchDetectionsApi({ pageNum: 1, pageSize: previewPageSize, detectionStatus: reviewPendingDetectionStatus }),
    loadWorkbenchSummaryReportRows(previewPageSize)
  ])
  previewRows.samplingPlan = planResult.records || []
  previewRows.sampling = samplingResult.records || []
  previewRows.sampleLogin = loginResult.records || []
  previewRows.detection = detectionResult.records || []
  previewRows.review = reviewResult.records || []
  previewRows.report = summaryReportRows
  previewTotals.samplingPlan = Number(planResult.total) || previewRows.samplingPlan.length
  previewTotals.sampling = Number(samplingResult.total) || previewRows.sampling.length
  previewTotals.sampleLogin = Number(loginResult.total) || previewRows.sampleLogin.length
  previewTotals.detection = Number(detectionResult.total) || previewRows.detection.length
  previewTotals.review = Number(reviewResult.total) || previewRows.review.length
  previewTotals.report = summaryReportRows.length
}

function getWorkbenchSummaryTypes() {
  if (isStaffRole(currentUser.value)) {
    return [SUMMARY_TYPE_DAILY]
  }
  return [SUMMARY_TYPE_DAILY, SUMMARY_TYPE_WEEKLY, SUMMARY_TYPE_HALF_MONTHLY]
}

async function loadWorkbenchSummaryReportRows(pageSize) {
  const size = Number(pageSize || 12)
  const results = await Promise.all(
    getWorkbenchSummaryTypes().map((summaryType) => fetchSummaryReportsApi({
      pageNum: 1,
      pageSize: size,
      summaryType
    }))
  )
  return results
    .flatMap((result) => Array.isArray(result.records) ? result.records : [])
    .sort(compareSummaryReportRows)
    .slice(0, size)
}

async function loadWorkbenchDetectionRows(pageSize) {
  const size = Number(pageSize || 12)
  const [waitDetectResult, rejectedResult] = await Promise.all([
    fetchDetectionItemsApi({ pageNum: 1, pageSize: size, itemStatus: waitDetectDetectionStatus }),
    fetchDetectionItemsApi({ pageNum: 1, pageSize: size, itemStatus: rejectedDetectionStatus })
  ])
  const records = mergeActionRows([
    ...(waitDetectResult.records || []),
    ...(rejectedResult.records || [])
  ])
    .sort(compareWorkbenchDetectionRows)
    .slice(0, size)
  return {
    records,
    total: Number(waitDetectResult.total || 0) + Number(rejectedResult.total || 0)
  }
}

function compareWorkbenchDetectionRows(left, right) {
  const rightTime = getWorkbenchDetectionLatestTime(right)
  const leftTime = getWorkbenchDetectionLatestTime(left)
  if (rightTime !== leftTime) {
    return rightTime - leftTime
  }
  return String(right?.id || '').localeCompare(String(left?.id || ''))
}

function getWorkbenchDetectionLatestTime(row) {
  const rawTime = row?.updatedTime || row?.detectionTime || row?.createdTime || ''
  const timestamp = Date.parse(rawTime)
  return Number.isFinite(timestamp) ? timestamp : 0
}

function compareSummaryReportRows(left, right) {
  const safeRightTime = getSummaryReportLatestTime(right)
  const safeLeftTime = getSummaryReportLatestTime(left)
  if (safeRightTime !== safeLeftTime) {
    return safeRightTime - safeLeftTime
  }
  return String(left?.reportName || '').localeCompare(String(right?.reportName || ''))
}

function getSummaryReportLatestTime(row) {
  const rawTime = row?.updatedTime
    || row?.generatedTime
    || row?.createdTime
    || row?.createTime
    || row?.reportTime
    || row?.periodEnd
    || row?.periodStart
    || row?.latestSamplingTime
    || ''
  const timestamp = Date.parse(rawTime)
  return Number.isFinite(timestamp) ? timestamp : 0
}

async function loadDictOptions() {
  const [weatherItems, storageItems] = await Promise.all([
    fetchDictItemsApi('weather_condition'),
    fetchDictItemsApi('storage_condition')
  ])
  weatherOptions.value = normalizeDictOptions(weatherItems)
  storageConditionOptions.value = normalizeDictOptions(storageItems)
}

async function loadDetectionConfigOptions() {
  if (detectionTypes.value.length && detectionParameters.value.length && detectionMethods.value.length) {
    return
  }
  const [typeResult, parameterResult, methodResult] = await Promise.all([
    fetchDetectionTypesApi({ pageNum: 1, pageSize: 500, enabled: 1 }),
    fetchDetectionParametersApi({ pageNum: 1, pageSize: 500 }),
    fetchDetectionMethodOptionsApi()
  ])
  detectionTypes.value = typeResult.records || []
  detectionParameters.value = parameterResult.records || []
  detectionMethods.value = Array.isArray(methodResult) ? methodResult : []
}

async function loadFlowOptions() {
  if (reviewFlowOptions.value.length) {
    return
  }
  const result = await fetchFlowConfigOptionsApi({ flowType: FLOW_TYPE_REVIEW })
  reviewFlowOptions.value = Array.isArray(result) ? result : []
}

function handlePreviewCardClick(key, row) {
  if (key === 'samplingPlan') {
    selectedSamplingPlan.value = row
    samplingPlanDetailVisible.value = true
    return
  }
  openWorkbenchAction(key, row)
}

async function dispatchSamplingPlanFromWorkbench(row) {
  if (!row || dispatchSubmitting.value) {
    return
  }
  await loadPlanOrgOptions()
  const rowOrgId = row?.orgId || row?.org_id || ''
  await loadSamplers(true, rowOrgId)
  resetDispatchForm()
  const samplerIds = includeRequiredSamplerIds(resolveRowSamplerOptionIds(row), rowOrgId)
  dispatchForm.planId = row.id
  dispatchForm.orgId = rowOrgId
  dispatchForm.samplingTime = row.startTime || nowDateTimeText()
  ensureSelectedSamplerOptions(samplerIds, row.samplerName || row.sampler_name)
  handleDispatchSamplerChange(samplerIds)
  dispatchDialogVisible.value = true
}

async function openPlanCreateDialog() {
  resetPlanCreateForm()
  planCreateForm.planName = '采样计划'
  await loadPlanOrgOptions()
  planCreateDialogVisible.value = true
}

// 采样计划时间选择器相关函数
function getPlanCreateDateTime(prefix) {
  const value = planCreateForm[`${prefix}Time`]
  const parsed = value ? dayjs(value) : null
  return parsed?.isValid() ? parsed : null
}

function getPlanCreateDatePart(prefix) {
  return getPlanCreateDateTime(prefix)?.format('YYYY-MM-DD') || ''
}

function getPlanCreateDateTimeText(prefix) {
  return getPlanCreateDateTime(prefix)?.format('YYYY-MM-DD HH:mm:ss') || ''
}

function getPlanCreateHourPart(prefix) {
  return getPlanCreateDateTime(prefix)?.format('HH') || '00'
}

function handlePlanCreateDateChange(prefix, value) {
  const hour = getPlanCreateHourPart(prefix)
  planCreateForm[`${prefix}Time`] = value ? `${value} ${hour}:00:00` : ''
  if (prefix === 'start' && planCreateForm.startTime) {
    planCreateForm.endTime = dayjs(planCreateForm.startTime).add(24, 'hour').format('YYYY-MM-DD HH:mm:ss')
  }
}

function handlePlanCreateHourChange(prefix, value) {
  const date = getPlanCreateDatePart(prefix) || dayjs().format('YYYY-MM-DD')
  planCreateForm[`${prefix}Time`] = `${date} ${value}:00:00`
  activePlanCreateHourPanel.value = ''
  if (prefix === 'start' && planCreateForm.startTime) {
    planCreateForm.endTime = dayjs(planCreateForm.startTime).add(24, 'hour').format('YYYY-MM-DD HH:mm:ss')
  }
}

function updatePlanCreateHourPanelPosition(prefix = activePlanCreateHourPanel.value) {
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
  planCreateHourPanelStyle.value = {
    top: `${top}px`,
    left: `${left}px`,
    width: `${panelWidth}px`
  }
}

async function togglePlanCreateHourPanel(prefix) {
  const nextPanel = activePlanCreateHourPanel.value === prefix ? '' : prefix
  activePlanCreateHourPanel.value = nextPanel
  if (!planCreateForm[`${prefix}Time`]) {
    const now = new Date()
    const date = dayjs().format('YYYY-MM-DD')
    const hour = String(now.getHours()).padStart(2, '0')
    planCreateForm[`${prefix}Time`] = `${date} ${hour}:00:00`
  }
  if (nextPanel) {
    await nextTick()
    updatePlanCreateHourPanelPosition(nextPanel)
  }
}

function handlePlanCreateHourOutsideClick(event) {
  if (!event.target?.closest?.('[data-plan-hour-picker]')) {
    activePlanCreateHourPanel.value = ''
  }
}

function buildPlanCreatePayload() {
  const samplerIds = includeRequiredSamplerIds(planCreateForm.samplerIds, planCreateForm.orgId)
  const samplerName = resolveSamplerNames(samplerIds)
  return {
    planName: String(planCreateForm.planName || '').trim(),
    orgId: planCreateForm.orgId || null,
    pointId: planCreateForm.pointSource === 'EXISTING' ? planCreateForm.pointId : null,
    pointName: String(planCreateForm.pointName || '').trim(),
    address: String(planCreateForm.address || '').trim(),
    latitude: String(planCreateForm.latitude || '').trim(),
    longitude: String(planCreateForm.longitude || '').trim(),
    startTime: planCreateForm.startTime || '',
    endTime: planCreateForm.endTime || '',
    samplerIds,
    samplerId: samplerIds[0] || null,
    samplerName,
    samplingType: planCreateForm.samplingType || routineSamplingType,
    sampleType: planCreateForm.sampleType || '',
    cycleType: planCreateForm.cycleType || dailyCycleType,
    remark: String(planCreateForm.remark || '').trim()
  }
}

async function submitPlanCreateForm() {
  if (planCreateSubmitting.value) {
    return
  }
  const payload = buildPlanCreatePayload()
  if (!payload.planName || !payload.orgId || !payload.pointName || !payload.address || !payload.longitude || !payload.latitude || !payload.startTime || !payload.endTime || !payload.sampleType || !payload.cycleType || !payload.samplerIds.length) {
    ElMessage.warning('请完整填写采样计划信息')
    return
  }
  if (new Date(payload.endTime).getTime() < new Date(payload.startTime).getTime()) {
    ElMessage.warning('结束时间不能早于开始时间')
    return
  }
  planCreateSubmitting.value = true
  try {
    await createSamplingPlanApi(payload)
    ElMessage.success('采样计划已创建')
    planCreateDialogVisible.value = false
    await Promise.all([loadWorkbenchPreviewRows(), loadActionRows('samplingPlan')])
    if (activeAction.value === 'samplingPlan' && actionRows.value.length) {
      await selectActionRow(actionRows.value[0])
    }
  } finally {
    planCreateSubmitting.value = false
  }
}

async function submitDispatchForm() {
  if (dispatchSubmitting.value) {
    return
  }
  const samplerIds = includeRequiredSamplerIds(dispatchForm.samplerIds, dispatchForm.orgId)
  const samplerName = resolveSamplerNames(samplerIds)
  if (!dispatchForm.planId || !samplerIds.length || !samplerIds[0] || !samplerName) {
    ElMessage.warning('派发任务前必须指定采样员')
    return
  }
  dispatchSubmitting.value = true
  dispatchingPlanId.value = dispatchForm.planId
  try {
    await dispatchSamplingPlanApi({
      planId: dispatchForm.planId,
      samplingTime: dispatchForm.samplingTime || nowDateTimeText(),
      samplerIds,
      samplerId: samplerIds[0],
      samplerName
    })
    dispatchDialogVisible.value = false
    ElMessage.success('采样计划已派发，并已同步生成采样任务。')
    samplingPlanDetailVisible.value = false
    await Promise.all([refreshDashboard(), loadWorkbenchPreviewRows()])
  } finally {
    dispatchSubmitting.value = false
    dispatchingPlanId.value = null
  }
}

async function openWorkbenchAction(key, preferredRow = null) {
  activeAction.value = key
  if (key === 'detectionSplit') {
    detectionSplitPlanKeyword.value = ''
    detectionSplitActiveTab.value = 'waitAssign'
  }
  workbenchDialogVisible.value = true
  actionLoading.value = true
  try {
    await loadActionRows(key)
    const selected = preferredRow
      ? actionRows.value.find((row) => String(getActionRowKey(row)) === String(getActionRowKey(preferredRow))) || preferredRow
      : null
    if (selected) {
      await selectActionRow(selected)
    } else if (actionRows.value.length) {
      await selectActionRow(actionRows.value[0])
    } else {
      activeRow.value = null
    }
  } finally {
    actionLoading.value = false
  }
}

async function loadActionRows(key) {
  previewData.value = null
  previewError.value = ''
  if (key !== 'detectionSplit') {
    selectedDetectionSplitRowKeys.value = []
    detectionSplitPlanKeyword.value = ''
  }
  const actionPageSize = 200
  if (key === 'samplingPlan') {
    const result = await fetchSamplingPlansApi({ pageNum: 1, pageSize: actionPageSize, planStatus: 'ACTIVE' })
    actionRows.value = result.records || []
    return
  }
  if (key === 'sampling') {
    const result = await fetchSamplingTasksApi({ pageNum: 1, pageSize: actionPageSize, taskStatus: 'PENDING' })
    actionRows.value = result.records || []
    return
  }
  if (key === 'sampleLogin') {
    await Promise.all([loadDetectionConfigOptions(), loadFlowOptions()])
    const result = await fetchSamplingTasksApi({
      pageNum: 1,
      pageSize: actionPageSize,
      sampleRegisterStatus: 'UNREGISTERED'
    })
    actionRows.value = result.records || []
    return
  }
  if (key === 'detectionSplit') {
    const currentTab = detectionSplitActiveTab.value
    const itemStatus = currentTab === 'waitDetect' ? waitDetectDetectionStatus : waitAssignDetectionStatus
    const result = await fetchDetectionItemsApi({ pageNum: 1, pageSize: actionPageSize, itemStatus })
    actionRows.value = mergeActionRows(result.records || [])
    // 更新统计数量
    await loadDetectionSplitCounts()
    return
  }
  if (key === 'detection') {
    const result = await loadWorkbenchDetectionRows(actionPageSize)
    actionRows.value = result.records || []
    return
  }
  if (key === 'review') {
    const result = await fetchDetectionsApi({ pageNum: 1, pageSize: actionPageSize, detectionStatus: reviewPendingDetectionStatus })
    actionRows.value = result.records || []
    return
  }
  if (key === 'report') {
    actionRows.value = await loadWorkbenchSummaryReportRows(actionPageSize)
  }
}

async function selectActionRow(row) {
  activeRow.value = row
  if (activeAction.value === 'samplingPlan') {
    // 采样计划暂无详情表单
  } else if (activeAction.value === 'sampling') {
    await openSamplingForm(row)
  } else if (activeAction.value === 'sampleLogin') {
    await openSampleLoginForm(row)
  } else if (activeAction.value === 'detectionSplit' || activeAction.value === 'detection') {
    openResultForm(row)
  } else if (activeAction.value === 'review') {
    await openReviewForm(row)
  } else if (activeAction.value === 'report') {
    await openReportPreview(row)
  }
}

function getActionRowKey(row) {
  return row?.id || row?.reportKey || row?.taskId || row?.recordId || row?.sampleNo || JSON.stringify(row)
}

function mergeActionRows(rows) {
  const rowMap = new Map()
  ;(rows || []).forEach((row) => {
    const key = getActionRowKey(row)
    if (!rowMap.has(key)) {
      rowMap.set(key, row)
    }
  })
  return Array.from(rowMap.values())
}

function isActionRowActive(row) {
  return String(getActionRowKey(row)) === String(getActionRowKey(activeRow.value))
}

function isDetectionSplitRowSelected(row) {
  const rowKey = String(getActionRowKey(row))
  return selectedDetectionSplitRowKeys.value.some((key) => String(key) === rowKey)
}

function toggleDetectionSplitRow(row, checked) {
  const rowKey = String(getActionRowKey(row))
  if (checked) {
    if (!isDetectionSplitRowSelected(row)) {
      selectedDetectionSplitRowKeys.value = [...selectedDetectionSplitRowKeys.value, rowKey]
    }
    return
  }
  selectedDetectionSplitRowKeys.value = selectedDetectionSplitRowKeys.value.filter((key) => String(key) !== rowKey)
}

function getActionRowTitle(row) {
  if (activeAction.value === 'samplingPlan') {
    return formatPlanNameWithDate(row)
  }
  if (activeAction.value === 'sampling' || activeAction.value === 'sampleLogin') {
    return formatPlanNameWithDate(row)
  }
  if (activeAction.value === 'detectionSplit' || activeAction.value === 'detection' || activeAction.value === 'review') {
    return formatPlanNameWithDate(row)
  }
  if (activeAction.value === 'report') {
    return row?.reportName || row?.sampleNo || '-'
  }
  return '-'
}

function getActionRowMeta(row) {
  if (activeAction.value === 'samplingPlan') {
    return row?.samplerName || ''
  }
  if (activeAction.value === 'sampling' || activeAction.value === 'sampleLogin') {
    return row?.samplerName || ''
  }
  if (activeAction.value === 'detectionSplit' || activeAction.value === 'detection') {
    return `${row?.parameterName || '-'} / ${row?.methodName || '-'}`
  }
  if (activeAction.value === 'review') {
    return `${row?.detectionTypeName || '-'} / ${row?.detectorName || '-'}`
  }
  if (activeAction.value === 'report') {
    return `${row?.summaryTypeLabel || row?.summaryType || '-'} / ${row?.periodLabel || '-'}`
  }
  return ''
}

function getDetectionSplitPlanSearchText(row) {
  return [
    row?.planName,
    row?.pointName,
    row?.monitoringPointName,
    row?.address,
    getActionRowTitle(row)
  ]
    .map((value) => String(value ?? '').trim().toLowerCase())
    .filter(Boolean)
    .join(' ')
}

function getPreviewRowTitle(key, row) {
  if (key === 'samplingPlan') {
    return row?.planName || row?.pointName || '-'
  }
  if (key === 'sampleLogin') {
    return formatPlanNameWithDate(row)
  }
  if (key === 'sampling') {
    return formatPlanNameWithDate(row)
  }
  if (key === 'detection') {
    return buildSampleContextTitle(row, row?.parameterName)
  }
  if (key === 'review') {
    return buildSampleContextTitle(row, row?.detectionTypeName)
  }
  if (key === 'report') {
    return row?.reportName || row?.summaryTypeLabel || '-'
  }
  return '-'
}

function buildSampleContextTitle(row, suffix = '') {
  const planName = firstNonBlank(row?.planName, row?.pointName, row?.monitoringPointName, row?.sampleNo) || '-'
  const sampleType = firstNonBlank(row?.sampleTypeLabel, getEnumLabel(sampleTypeLabelMap, row?.sampleType), row?.sampleType)
  const parameterName = row?.parameterName || ''
  let title = `计划名称：${planName || '未填'}      样品类型：${sampleType || '未填'}`
  if (parameterName) {
    title += `      检测参数：${parameterName}`
  }
  return title
}

function firstNonBlank(...values) {
  return values
    .map((value) => String(value ?? '').trim())
    .find((value) => value && value !== '-') || ''
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return dateStr
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatSamplingTimeShort(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return ''
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hour}:${minute}`
}

function formatPlanNameWithDate(row) {
  const planName = firstNonBlank(row?.planName, row?.pointName, row?.monitoringPointName)
  const dateLabel = formatSamplingTimeShort(firstNonBlank(
    row?.samplingTime,
    row?.detectionTime,
    row?.startTime,
    row?.updatedTime,
    row?.createdTime
  ))
  return dateLabel ? `${planName}  ${dateLabel}` : planName
}

function getPreviewRowMeta(key, row) {
  if (key === 'samplingPlan') {
    return `${row?.pointName || '-'} / ${row?.samplerName || '-'}`
  }
  if (key === 'sampleLogin') {
    return row?.samplerName || ''
  }
  if (key === 'sampling') {
    return ''
  }
  if (key === 'detection') {
    return ''
  }
  if (key === 'review') {
    return firstNonBlank(row?.detectorName) ? `检测人员：${row.detectorName}` : ''
  }
  if (key === 'report') {
    return `${row?.summaryTypeLabel || row?.summaryType || '-'} / ${row?.periodLabel || '-'}`
  }
  return ''
}

function getPreviewFields(key, row) {
  if (key === 'samplingPlan') {
    return [
      { label: '计划状态', value: getEnumLabel(planStatusLabelMap, row?.planStatus) },
      { label: '周期类型', value: getEnumLabel(cycleTypeLabelMap, row?.cycleType) },
      { label: '开始时间', value: formatDate(row?.startTime) }
    ]
  }
  if (key === 'report') {
    return [
      { label: '报表类型', value: row?.summaryTypeLabel || row?.summaryType },
      { label: '周期范围', value: row?.periodLabel },
      { label: '报表状态', value: row?.reportStatusLabel || row?.reportStatus }
    ]
  }
  if (key === 'sampling') {
    return [
      { label: '任务状态', value: row?.taskStatus },
      { label: '计划时间', value: formatDate(row?.samplingTime) }
    ]
  }
  if (key === 'sampleLogin') {
    return [
      { label: '任务编号', value: row?.taskNo },
      { label: '计划时间', value: formatDate(row?.samplingTime) }
    ]
  }
  if (key === 'detection') {
    return [
      { label: '检测方法', value: row?.methodName },
      { label: '检测人员', value: row?.detectorName },
      { label: '更新时间', value: row?.updatedTime }
    ]
  }
  if (key === 'review') {
    return [
      { label: '检测套餐', value: row?.detectionTypeName },
      { label: '参数进度', value: `${row?.completedCount ?? 0}/${row?.parameterCount ?? 0}` },
      { label: '更新时间', value: row?.updatedTime }
    ]
  }
  if (key === 'report') {
    return [
      { label: '报告状态', value: row?.reportStatus },
      { label: '生成时间', value: row?.generatedTime }
    ]
  }
  return []
}

function getPreviewDisplayRowMeta(key, row) {
  return getPreviewRowMeta(key, row)
}

function getPreviewDisplayFields(key, row) {
  if (key === 'report') {
    return [
      {
        label: '报表状态',
        value: row?.reportStatusLabel || row?.reportStatus,
        statusClass: resolveSummaryReportStatusClass(row?.reportStatus)
      },
      { label: '报表类型', value: row?.summaryTypeLabel || row?.summaryType },
      { label: '周期范围', value: row?.periodLabel }
    ]
  }
  if (key === 'samplingPlan') {
    return [
      {
        label: '计划状态',
        value: getEnumLabel(planStatusLabelMap, row?.planStatus),
        statusType: 'planStatus',
        statusValue: row?.planStatus
      },
      { label: '周期', value: getEnumLabel(cycleTypeLabelMap, row?.cycleType) },
      { label: '开始时间', value: formatDate(row?.startTime) }
    ]
  }
  if (key === 'sampling') {
    return [
      {
        label: '任务状态',
        value: getEnumLabel(taskStatusLabelMap, row?.taskStatus),
        statusType: 'taskStatus',
        statusValue: row?.taskStatus
      },
      { label: '计划时间', value: formatDate(row?.samplingTime) },
      { label: '采样人员', value: row?.samplerName }
    ]
  }
  if (key === 'sampleLogin') {
    return [
      { label: '任务编号', value: row?.taskNo },
      {
        label: '登记状态',
        value: getEnumLabel(sampleRegisterStatusLabelMap, row?.sampleRegisterStatus || unregisteredSampleRegisterStatus),
        statusClass: row?.sampleRegisterStatus && row.sampleRegisterStatus !== unregisteredSampleRegisterStatus ? 'success' : 'warning'
      },
      { label: '计划时间', value: formatDate(row?.samplingTime) }
    ]
  }
  if (key === 'detection') {
    return [
      {
        label: '检测状态',
        value: getEnumLabel(detectionStatusLabelMap, row?.itemStatus),
        statusType: 'detectionStatus',
        statusValue: row?.itemStatus
      },
      { label: '检测方法', value: row?.methodName },
      { label: '检测人员', value: row?.detectorName }
    ]
  }
  if (key === 'review') {
    return [
      {
        label: '审核状态',
        value: getEnumLabel(detectionStatusLabelMap, reviewPendingDetectionStatus),
        statusType: 'detectionStatus',
        statusValue: reviewPendingDetectionStatus
      },
      { label: '参数进度', value: `${row?.completedCount ?? 0}/${row?.parameterCount ?? 0}` },
      { label: '更新时间', value: row?.updatedTime }
    ]
  }
  if (key === 'report') {
    return [
      {
        label: '报告状态',
        value: getEnumLabel(reportStatusLabelMap, row?.reportStatus),
        statusType: 'reportStatus',
        statusValue: row?.reportStatus
      },
      { label: '生成时间', value: row?.generatedTime }
    ]
  }
  return getPreviewFields(key, row)
}

function getVisiblePreviewFields(key, row) {
  return getPreviewDisplayFields(key, row).filter((field) => field?.statusType || field?.statusClass || firstNonBlank(field?.value))
}

async function openSamplingForm(row) {
  const detail = await fetchSamplingTaskDetailApi(row.id)
  const task = detail || row
  samplingTask.value = task
}

async function openSampleLoginForm(task) {
  loginTask.value = task
  loginForm.taskId = task.id
  loginForm.sampleNo = task.sampleNo || ''
  loginForm.pointId = task.pointId || null
  loginForm.pointName = task.pointName || ''
  loginForm.sampleType = task.sampleType || ''
  loginForm.sampleSourceMethod = samplingSampleSourceMethod
  loginForm.samplingTime = task.samplingTime || ''
  loginForm.samplerId = task.samplerId || null
  loginForm.samplerName = task.samplerName || ''
  loginForm.weather = task.weather || ''
  loginForm.storageCondition = ''
  loginForm.remark = task.remark || ''
  const flow = reviewFlowOptions.value.find((item) => item.defaultFlag) || reviewFlowOptions.value[0]
  loginForm.reviewFlowId = flow?.id || null
  loginForm.reviewFlowName = flow?.flowName || ''
  const type = loginDetectionTypeOptions.value[0]
  handleLoginDetectionTypeChange(type?.id || null)
}

function handleReviewFlowChange(flowId) {
  const flow = reviewFlowOptions.value.find((item) => String(item.id) === String(flowId))
  loginForm.reviewFlowName = flow?.flowName || ''
}

// 工作台添加参数相关函数
function isWorkbenchParamInCurrentList(parameterId) {
  const currentIds = loginForm.detectionConfigItems.map((row) => String(row.parameterId))
  return currentIds.includes(String(parameterId))
}

const workbenchNewSelectedParamCount = computed(() => {
  const currentIds = loginForm.detectionConfigItems.map((row) => String(row.parameterId))
  return workbenchAddParamSelectedIds.value.filter((id) => !currentIds.includes(id)).length
})

function openWorkbenchAddParamDialog() {
  workbenchAddParamSelectedIds.value = []
  workbenchAddParamDialogVisible.value = true
}

async function handleWorkbenchAddParamDialogOpen() {
  workbenchAddParamLoading.value = true
  workbenchAddParamSelectedIds.value = []
  try {
    if (!detectionParameters.value.length) {
      const result = await fetchDetectionParametersApi({ pageNum: 1, pageSize: 1000 })
      detectionParameters.value = result.records || []
    }
    workbenchAddParamOptions.value = detectionParameters.value
  } catch (error) {
    console.error('加载检测参数失败:', error)
    ElMessage.error('加载检测参数失败')
    workbenchAddParamOptions.value = []
  } finally {
    workbenchAddParamLoading.value = false
  }
}

function handleWorkbenchParamCheckChange(parameterId, checked) {
  const id = String(parameterId)
  if (checked) {
    if (!workbenchAddParamSelectedIds.value.includes(id)) {
      workbenchAddParamSelectedIds.value = [...workbenchAddParamSelectedIds.value, id]
    }
  } else {
    workbenchAddParamSelectedIds.value = workbenchAddParamSelectedIds.value.filter((item) => item !== id)
  }
}

function confirmWorkbenchAddParamDialog() {
  const currentIds = loginForm.detectionConfigItems.map((row) => String(row.parameterId))
  const selectedIds = workbenchAddParamSelectedIds.value.filter((id) => !currentIds.includes(id))
  if (!selectedIds.length) {
    ElMessage.warning('请选择要添加的参数')
    return
  }
  const newRows = selectedIds
    .map((parameterId) => {
      const parameter = workbenchAddParamOptions.value.find((item) => String(item.id) === parameterId)
      if (!parameter) return null
      const methodOptions = detectionMethods.value
        .filter((item) => String(item.parameterId) === String(parameterId) && item.enabled === 1)
        .map((item) => ({
          id: String(item.id),
          methodName: item.methodName || `检测方法-${item.id}`,
          sampleVolume: item.sampleVolume || item.sample_volume || ''
        }))
      const currentMethod = methodOptions[0] || null
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
  loginForm.detectionConfigItems = [...loginForm.detectionConfigItems, ...newRows]
  workbenchAddParamDialogVisible.value = false
  ElMessage.success(`成功添加 ${newRows.length} 个检测参数`)
}

function formatDetectionTypeLabel(item) {
  const sampleTypeLabel = item?.sampleType ? getEnumLabel(sampleTypeLabelMap, item.sampleType) : '未绑定样品类型'
  return `${item?.typeName || '-'} / ${sampleTypeLabel}`
}

function handleLoginDetectionTypeChange(typeId) {
  const type = detectionTypes.value.find((item) => String(item.id) === String(typeId || ''))
  loginForm.detectionTypeId = type?.id || null
  loginForm.detectionTypeName = type?.typeName || ''
  loginForm.detectionItems = type?.typeName || ''
  loginForm.detectionConfigItems = buildLoginDetectionConfigItems(type)
}

function parseIdList(value) {
  return String(value || '').split(',').map((item) => item.trim()).filter(Boolean)
}

function parseBindingJson(value) {
  try {
    const list = JSON.parse(String(value || '[]'))
    return Array.isArray(list) ? list : []
  } catch {
    return []
  }
}

function parseDetectionConfigRows(snapshot) {
  return parseBindingJson(snapshot).map((item) => ({
    parameterId: item.parameterId,
    parameterName: item.parameterName || '',
    standardMin: item.standardMin,
    standardMax: item.standardMax,
    unit: item.unit || '',
    optionValues: item.optionValues || '',
    referenceStandard: item.referenceStandard || '',
    methodId: item.methodId,
    methodName: item.methodName || '',
    sampleVolume: item.sampleVolume || ''
  }))
}

function buildLoginDetectionConfigItems(type) {
  if (!type) {
    return []
  }
  const parameterMap = new Map(detectionParameters.value.map((item) => [String(item.id), item]))
  const bindingMap = new Map()
  parseBindingJson(type.parameterMethodBindings).forEach((item) => {
    const parameterId = String(item?.parameterId || '')
    const methodId = Array.isArray(item?.methodIds) ? item.methodIds[0] : null
    if (parameterId) {
      bindingMap.set(parameterId, methodId == null ? '' : String(methodId))
    }
  })
  return parseIdList(type.parameterIds).map((parameterId) => {
    const parameter = parameterMap.get(String(parameterId))
    if (!parameter) {
      return null
    }
    const methodOptions = detectionMethods.value.filter((item) => String(item.parameterId) === String(parameterId) && item.enabled === 1)
    const defaultMethodId = bindingMap.get(parameterId) || methodOptions[0]?.id || ''
    const method = methodOptions.find((item) => String(item.id) === String(defaultMethodId)) || methodOptions[0]
    return {
      parameterId: String(parameter.id),
      parameterName: parameter.parameterName || '',
      unit: parameter.unit || '',
      standardMin: parameter.standardMin,
      standardMax: parameter.standardMax,
      referenceStandard: parameter.referenceStandard || '',
      methodId: method?.id ? String(method.id) : '',
      methodName: method?.methodName || '',
      sampleVolume: method?.sampleVolume || method?.sample_volume || ''
    }
  }).filter(Boolean)
}

async function submitSampleLogin() {
  if (!loginForm.taskId || !loginForm.pointName || !loginForm.sampleType || !loginForm.detectionTypeId || !loginForm.reviewFlowId || !loginForm.samplingTime) {
    ElMessage.warning('请完整填写样品登录信息')
    return
  }
  if (!loginForm.detectionConfigItems.length || loginForm.detectionConfigItems.some((item) => !item.methodId)) {
    ElMessage.warning('检测套餐缺少检测参数或检测方法')
    return
  }
  submitting.value = true
  try {
    // 验证任务状态，防止长时间未刷新导致重复登录
    const taskDetail = await fetchSamplingTaskDetailApi(loginForm.taskId)
    if (taskDetail.sampleRegisterStatus === 'REGISTERED') {
      ElMessage.warning('该任务已被其他人完成样品登录，请刷新页面')
      await reloadActiveAction()
      return
    }
    if (taskDetail.taskStatus === 'ABANDONED') {
      ElMessage.warning('该任务已被废弃，请刷新页面')
      await reloadActiveAction()
      return
    }
    const sample = await loginSampleApi({
      ...loginForm,
      detectionConfigItems: loginForm.detectionConfigItems
    })
    ElMessage.success(`样品登录完成：${sample?.sampleNo || loginForm.sampleNo || '-'}`)
    await reloadActiveAction()
  } finally {
    submitting.value = false
  }
}

function formatTaskLabel(task) {
  if (!task) {
    return '-'
  }
  return task.planName || task.pointName || '-'
}

function openResultForm(row) {
  resultForm.id = row.id
  resultForm.recordId = row.recordId
  resultForm.sampleId = row.sampleId
  resultForm.sampleNo = row.sampleNo || ''
  resultForm.planName = row.planName || ''
  resultForm.pointName = row.pointName || row.monitoringPointName || ''
  resultForm.sampleType = row.sampleType || ''
  resultForm.detectionTypeId = row.detectionTypeId
  resultForm.detectionTypeName = row.detectionTypeName || ''
  resultForm.parameterId = row.parameterId
  resultForm.parameterName = row.parameterName || ''
  resultForm.methodName = row.methodName || ''
  resultForm.methodBasis = row.methodBasis || ''
  resultForm.standardMin = row.standardMin
  resultForm.standardMax = row.standardMax
  resultForm.unit = row.unit || ''
  resultForm.referenceStandard = row.referenceStandard || ''
  resultForm.detectorId = toStableId(row.detectorId) || null
  resultForm.detectorName = row.detectorName || ''
  resultForm.resultValue = row.resultValue == null ? null : String(row.resultValue)
  resultForm.abnormalRemark = row.abnormalRemark || ''
  resultForm.remark = row.remark || ''
  resultForm.itemStatus = row.itemStatus || ''
  // 优先从参数配置中获取 optionValues，确保文本选项能正确显示
  const parameter = detectionParameters.value.find((p) => String(p.id) === String(row.parameterId))
  resultForm.optionValues = parameter?.optionValues || row.optionValues || ''
}

function getDetectorOptionLabel(option) {
  if (!option) {
    return '-'
  }
  return option.displayName || option.realName || option.username || option.nickname || '-'
}

function normalizeDetectorOption(item) {
  const id = toStableId(item?.userId ?? item?.id ?? item?.user_id)
  const username = item?.username || ''
  const realName = item?.realName || item?.real_name || item?.name || ''
  const displayName = item?.displayName || item?.label || realName || username || id
  return {
    ...item,
    id,
    userId: id,
    username,
    realName,
    displayName
  }
}

async function loadDetectorOptions(force = false, orgId = '') {
  const normalizedOrgId = toStableId(orgId)
  if (!force && detectorOptionsLoaded.value && detectorOptionsOrgId.value === normalizedOrgId) {
    return
  }
  detectorOptionsLoading.value = true
  try {
    const params = normalizedOrgId ? { orgId: normalizedOrgId } : undefined
    const result = await fetchDetectionDetectorsApi(params)
    const records = Array.isArray(result) ? result : []
    detectorOptions.value = records.map(normalizeDetectorOption).filter((item) => item.id)
    detectorOptionsOrgId.value = normalizedOrgId
    detectorOptionsLoaded.value = true
  } finally {
    detectorOptionsLoading.value = false
  }
}

function getDefaultDetectorIdForOrg(orgId) {
  if (!isYanzhenOrgId(orgId)) {
    return null
  }
  const director = detectorOptions.value.find((item) => getRoleCode(item) === DIRECTOR_ROLE_CODE)
  return director?.id || null
}

function ensureSelectedDetectorOptions(rows) {
  const existingIds = new Set(detectorOptions.value.map((item) => String(item.id)))
  const additions = []
  ;(rows || []).forEach((row) => {
    const detectorId = toStableId(row?.detectorId)
    if (!detectorId || existingIds.has(detectorId)) {
      return
    }
    const detectorName = firstNonBlank(row?.detectorName, `检测人员${detectorId}`)
    additions.push({
      id: detectorId,
      userId: detectorId,
      realName: detectorName,
      displayName: detectorName,
      username: detectorId
    })
    existingIds.add(detectorId)
  })
  if (additions.length) {
    detectorOptions.value = [...detectorOptions.value, ...additions]
  }
}

function getCommonDetectorId(rows) {
  const detectorIds = (rows || [])
    .map((row) => toStableId(row?.detectorId))
    .filter(Boolean)
  if (!detectorIds.length || detectorIds.length !== (rows || []).length) {
    return null
  }
  const firstDetectorId = detectorIds[0]
  return detectorIds.every((id) => id === firstDetectorId) ? firstDetectorId : null
}

function getCommonOrgId(rows) {
  const orgIds = (rows || [])
    .map((row) => toStableId(row?.orgId ?? row?.org_id))
    .filter(Boolean)
  if (!orgIds.length || orgIds.length !== (rows || []).length) {
    return ''
  }
  const firstOrgId = orgIds[0]
  return orgIds.every((id) => id === firstOrgId) ? firstOrgId : ''
}

function hasMixedDetectorOrgRows(rows) {
  const orgIds = Array.from(new Set((rows || [])
    .map((row) => toStableId(row?.orgId ?? row?.org_id))
    .filter(Boolean)))
  return orgIds.length > 1
}

async function openDetectorAssignDialog() {
  if (!detectorAssignmentTargetRows.value.length) {
    ElMessage.warning('请选择需要分配的检测分样')
    return
  }
  if (hasMixedDetectorOrgRows(detectorAssignmentTargetRows.value)) {
    ElMessage.warning('请选择同一所属机构下的检测分样后再统一分配')
    return
  }
  await loadPlanOrgOptions()
  detectorAssignForm.orgId = getCommonOrgId(detectorAssignmentTargetRows.value)
  await loadDetectorOptions(true, detectorAssignForm.orgId)
  ensureSelectedDetectorOptions(detectorAssignmentTargetRows.value)
  detectorAssignForm.detectorId = getCommonDetectorId(detectorAssignmentTargetRows.value) || getDefaultDetectorIdForOrg(detectorAssignForm.orgId)
  detectorAssignDialogVisible.value = true
}

function resetDetectorAssignDialog() {
  detectorAssignForm.orgId = ''
  detectorAssignForm.detectorId = null
}

async function handleDetectorAssignOrgChange(orgId) {
  await loadDetectorOptions(true, orgId)
  detectorAssignForm.detectorId = getDefaultDetectorIdForOrg(orgId)
}

function getDetectorAssignSummaryTitle() {
  const rows = detectorAssignmentTargetRows.value
  if (!rows.length) {
    return '-'
  }
  if (rows.length === 1) {
    return getActionRowTitle(rows[0])
  }
  const pointNames = Array.from(new Set(rows.map((row) => firstNonBlank(row?.planName, row?.pointName, row?.monitoringPointName)).filter(Boolean)))
  return pointNames.length === 1 ? pointNames[0] : `多个点位（${rows.length}项）`
}

function getDetectorAssignSummaryDetector() {
  const rows = detectorAssignmentTargetRows.value
  if (!rows.length) {
    return '-'
  }
  if (rows.length === 1) {
    return rows[0]?.detectorName || '待分配'
  }
  const detectorNames = Array.from(new Set(rows.map((row) => firstNonBlank(row?.detectorName)).filter(Boolean)))
  if (!detectorNames.length) {
    return '待分配'
  }
  return detectorNames.length === 1 ? detectorNames[0] : '多个人员'
}

async function submitDetectorAssignment() {
  const rows = detectorAssignmentTargetRows.value
  if (!rows.length) {
    ElMessage.warning('请选择需要分配的检测分样')
    return
  }
  const selectedDetectorId = toStableId(detectorAssignForm.detectorId)
  const selectedOrgId = toStableId(detectorAssignForm.orgId)
  if (selectedOrgId && detectorAssignmentTargetRows.value.some((row) => {
    const rowOrgId = toStableId(row?.orgId ?? row?.org_id)
    return rowOrgId && rowOrgId !== selectedOrgId
  })) {
    ElMessage.warning('检测人员所属机构与所选检测分样所属机构不一致')
    return
  }
  const hasSelectedDetector = Boolean(selectedDetectorId)
  if (!hasSelectedDetector && rows.some((row) => !toStableId(row?.detectorId))) {
    ElMessage.warning('请选择检测人员')
    return
  }
  detectorAssignSubmitting.value = true
  try {
    const recordGroups = rows.reduce((groups, row) => {
      if (!row?.recordId || !row?.id) {
        return groups
      }
      const key = String(row.recordId)
      const group = groups.get(key) || { recordId: row.recordId, items: [] }
      group.items.push({
        itemId: row.id,
        detectorId: hasSelectedDetector ? selectedDetectorId : toStableId(row.detectorId)
      })
      groups.set(key, group)
      return groups
    }, new Map())
    if (!recordGroups.size) {
      ElMessage.warning('所选检测分样缺少必要信息')
      return
    }
    await Promise.all(Array.from(recordGroups.values()).map((group) => assignDetectionDetectorsApi(group.recordId, {
      items: group.items
    })))
    ElMessage.success(`检测人员分配已保存：${rows.length}项`)
    selectedDetectionSplitRowKeys.value = []
    detectorAssignDialogVisible.value = false
    await reloadActiveAction()
  } finally {
    detectorAssignSubmitting.value = false
  }
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

function isResultValueAbnormal(item) {
  if (!item || item.resultValue == null || item.resultValue === '') {
    return false
  }
  if (item.optionValues) {
    return false
  }
  const value = Number(item.resultValue)
  if (!Number.isFinite(value)) {
    return false
  }
  if (item.standardMin != null && value < Number(item.standardMin)) {
    return true
  }
  return item.standardMax != null && value > Number(item.standardMax)
}

function handleOcrTrigger() {
  ElMessage.info('未检测到可适配的OCR设备')
}

function handleDetectionResultInput(value) {
  resultForm.resultValue = normalizeDetectionResultInput(value)
}

function normalizeDetectionResultInput(value) {
  const text = String(value ?? '').replace(/[^\d.]/g, '')
  const [integerPart, ...decimalParts] = text.split('.')
  if (!decimalParts.length) {
    return integerPart
  }
  return `${integerPart}.${decimalParts.join('')}`
}

async function submitDetectionResult() {
  if (!resultForm.sampleId || !resultForm.detectionTypeId || !resultForm.recordId || !resultForm.id) {
    ElMessage.warning('当前检测子流程缺少必要信息')
    return
  }
  if (resultForm.resultValue == null || resultForm.resultValue === '') {
    ElMessage.warning('请填写检测结果')
    return
  }
  if (isResultValueAbnormal(resultForm)) {
    ElMessage.warning(`检测结果超出标准范围：${formatStandardRange(resultForm.standardMin, resultForm.standardMax, resultForm.unit, resultForm.optionValues)}，请检查结果值`)
    return
  }
  submitting.value = true
  try {
    await submitDetectionApi({
      recordId: resultForm.recordId,
      itemId: resultForm.id,
      sampleId: resultForm.sampleId,
      detectionTypeId: resultForm.detectionTypeId,
      detectionTypeName: resultForm.detectionTypeName,
      abnormalRemark: resultForm.abnormalRemark,
      remark: resultForm.remark,
      items: [{
        parameterId: resultForm.parameterId,
        parameterName: resultForm.parameterName,
        standardMin: resultForm.standardMin,
        standardMax: resultForm.standardMax,
        resultValue: Number(resultForm.resultValue),
        unit: resultForm.unit,
        optionValues: resultForm.optionValues || ''
      }]
    })
    ElMessage.success('检测结果已提交')
    await reloadActiveAction()
  } finally {
    submitting.value = false
  }
}

async function openReviewForm(row) {
  const detail = await fetchDetectionDetailApi(row.id || row.detectionRecordId)
  const items = detail?.items || []
  reviewForm.detectionRecordId = row.id || row.detectionRecordId
  reviewForm.sampleNo = row.sampleNo || detail?.record?.sampleNo || ''
  reviewForm.detectionTypeName = row.detectionTypeName || detail?.record?.detectionTypeName || ''
  reviewForm.reviewRemark = ''
  reviewForm.items = items.map((item) => ({
    id: item.id,
    parameterName: item.parameterName || '',
    methodName: item.methodName || '',
    standardMin: item.standardMin,
    standardMax: item.standardMax,
    unit: item.unit || '',
    optionValues: item.optionValues || '',
    resultValue: item.resultValue == null ? null : Number(item.resultValue),
    itemStatus: item.itemStatus || '',
    reviewResultDraft: item.itemStatus === approvedDetectionStatus ? approvedReviewResult : '',
    rejectReasonDraft: '',
    reviewRemarkDraft: ''
  }))
}

function setAllReviewResult(result) {
  reviewForm.items.forEach((item) => {
    if (item.itemStatus === reviewPendingDetectionStatus) {
      item.reviewResultDraft = result
      if (result === approvedReviewResult) {
        item.rejectReasonDraft = ''
      }
    }
  })
}

function approveReviewItem(item) {
  item.reviewResultDraft = approvedReviewResult
  item.rejectReasonDraft = ''
}

function getReviewDraftStatusLabel(item) {
  if (item?.reviewResultDraft === approvedReviewResult) {
    return '通过'
  }
  if (item?.reviewResultDraft === rejectedReviewResult) {
    return '驳回'
  }
  return '待审核'
}

function getReviewDraftStatusClass(item) {
  if (item?.reviewResultDraft === approvedReviewResult) {
    return 'is-approved'
  }
  if (item?.reviewResultDraft === rejectedReviewResult) {
    return 'is-rejected'
  }
  return 'is-pending'
}

async function promptRejectReason(message, defaultValue = '') {
  try {
    const result = await ElMessageBox.prompt(message, '填写驳回原因', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputValue: defaultValue,
      inputPlaceholder: '请输入驳回原因',
      inputValidator: (value) => String(value || '').trim() ? true : '请填写驳回原因'
    })
    return String(result.value || '').trim()
  } catch {
    return ''
  }
}

async function rejectReviewItem(item) {
  const reason = await promptRejectReason(`请填写 ${item.parameterName || '检测项目'} 的驳回原因`, item.rejectReasonDraft)
  if (!reason) {
    return
  }
  item.reviewResultDraft = rejectedReviewResult
  item.rejectReasonDraft = reason
}

async function rejectAllReviewItems() {
  const pendingItems = reviewForm.items.filter((item) => item.itemStatus === reviewPendingDetectionStatus)
  if (!pendingItems.length) {
    ElMessage.warning('当前没有可驳回的检测项目')
    return
  }
  const reason = await promptRejectReason('请填写本次一键驳回的原因')
  if (!reason) {
    return
  }
  pendingItems.forEach((item) => {
    item.reviewResultDraft = rejectedReviewResult
    item.rejectReasonDraft = reason
  })
}

async function submitReviewDecision() {
  const pendingItems = reviewForm.items.filter((item) => item.itemStatus === reviewPendingDetectionStatus)
  if (!pendingItems.length) {
    ElMessage.warning('当前主流程下没有待审核子流程')
    return
  }
  const undecidedItem = pendingItems.find((item) => !item.reviewResultDraft)
  if (undecidedItem) {
    try {
      await ElMessageBox.confirm(
        '仍有检测项目未选择通过或驳回，是否将未选检测项目全部按通过提交？',
        '确认提交',
        {
          confirmButtonText: '全部通过并提交',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
      pendingItems.forEach((item) => {
        if (!item.reviewResultDraft) {
          item.reviewResultDraft = approvedReviewResult
          item.rejectReasonDraft = ''
        }
      })
    } catch {
      return
    }
  }
  const missingRejectReason = pendingItems.find((item) => item.reviewResultDraft === rejectedReviewResult && !String(item.rejectReasonDraft || '').trim())
  if (missingRejectReason) {
    await rejectReviewItem(missingRejectReason)
    if (!String(missingRejectReason.rejectReasonDraft || '').trim()) {
      ElMessage.warning(`请填写 ${missingRejectReason.parameterName || '检测参数'} 的驳回原因`)
      return
    }
  }
  const remainingUndecidedItem = pendingItems.find((item) => !item.reviewResultDraft)
  if (remainingUndecidedItem) {
    return
  }
  submitting.value = true
  try {
    const anyRejected = pendingItems.some((item) => item.reviewResultDraft === rejectedReviewResult)
    await submitReviewApi({
      detectionRecordId: reviewForm.detectionRecordId,
      reviewResult: anyRejected ? rejectedReviewResult : approvedReviewResult,
      reviewRemark: reviewForm.reviewRemark,
      items: pendingItems.map((item) => ({
        itemId: item.id,
        reviewResult: item.reviewResultDraft,
        rejectReason: item.reviewResultDraft === rejectedReviewResult ? item.rejectReasonDraft : '',
        reviewRemark: item.reviewRemarkDraft
      }))
    })
    ElMessage.success(anyRejected ? '审核已提交，主流程已退回' : '审核已提交，主流程全部通过')
    await reloadActiveAction()
  } finally {
    submitting.value = false
  }
}

async function openReportPreview(row) {
  previewData.value = null
  previewError.value = ''
  workbenchReportPrintRef.value = null
  try {
    previewData.value = await previewSummaryReportApi(buildSummaryPreviewParams(row))
  } catch (error) {
    previewError.value = error?.message || '报告预览失败'
  }
}

async function printWorkbenchReport() {
  if (!previewData.value || !previewComponent.value) {
    ElMessage.warning('请先选择可预览的报告')
    return
  }
  if (workbenchReportPrintRef.value?.printDocument) {
    await workbenchReportPrintRef.value.printDocument()
    return
  }
  document.body.classList.add('workbench-report-printing')

  let cleanupTimer = 0
  let cleaned = false
  const cleanupPrintMode = () => {
    if (cleaned) {
      return
    }
    cleaned = true
    window.removeEventListener('afterprint', cleanupPrintMode)
    if (cleanupTimer) {
      window.clearTimeout(cleanupTimer)
    }
    document.body.classList.remove('workbench-report-printing')
  }

  window.addEventListener('afterprint', cleanupPrintMode, { once: true })
  cleanupTimer = window.setTimeout(cleanupPrintMode, 30000)
  await nextTick()

  try {
    window.print()
  } catch (error) {
    cleanupPrintMode()
    throw error
  }
}

function buildSummaryPreviewParams(row) {
  const params = {
    summaryType: row?.summaryType || SUMMARY_TYPE_DAILY,
    regionName: row?.regionName || '',
    periodStart: row?.periodStart,
    periodEnd: row?.periodEnd
  }
  if (params.summaryType === SUMMARY_TYPE_DAILY) {
    params.dailyReportType = row?.dailyReportType || ''
  }
  return params
}

function resolveSummaryPreviewComponent(previewMode) {
  if (previewMode === 'DAILY_INTERNAL_TEMPLATE') {
    return DailyInternalSummaryTemplate
  }
  if (previewMode === 'DAILY_EXTERNAL_TEMPLATE') {
    return DailyExternalSummaryTemplate
  }
  if (previewMode === 'WEEKLY_FACTORY_TEMPLATE') {
    return WeeklyFactorySummaryTemplate
  }
  if (previewMode === 'HALF_MONTHLY_TERMINAL_TEMPLATE') {
    return HalfMonthlyTerminalSummaryTemplate
  }
  return null
}

function resolveSummaryReportStatusClass(status) {
  if (status === 'COMPLETE') {
    return 'success'
  }
  if (status === 'PARTIAL') {
    return 'warning'
  }
  return 'info'
}

async function reloadActiveAction() {
  await Promise.all([refreshDashboard(), loadWorkbenchPreviewRows(), loadActionRows(activeAction.value)])
  if (actionRows.value.length) {
    await selectActionRow(actionRows.value[0])
  } else {
    activeRow.value = null
  }
}

function resetWorkbenchDialog() {
  activeAction.value = ''
  actionRows.value = []
  activeRow.value = null
  detectionSplitPlanKeyword.value = ''
  selectedDetectionSplitRowKeys.value = []
  detectorAssignDialogVisible.value = false
  resetDetectorAssignDialog()
  previewData.value = null
  previewError.value = ''
}

function syncCurrentUser(event) {
  currentUser.value = event?.detail || getUser() || {}
}

onMounted(async () => {
  currentUser.value = getUser() || {}
  window.addEventListener('yx-lab-user-updated', syncCurrentUser)
  document.addEventListener('mousedown', handlePlanCreateHourOutsideClick)
  loading.value = true
  try {
    await Promise.all([refreshDashboard(), loadWorkbenchPreviewRows(), loadDictOptions()])
  } catch (error) {
    dashboard.value = {}
    console.warn('工作台数据加载失败，已使用空数据兜底。', error)
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  window.removeEventListener('yx-lab-user-updated', syncCurrentUser)
  document.removeEventListener('mousedown', handlePlanCreateHourOutsideClick)
})
</script>

<style scoped>
.dashboard-page {
  min-height: 100%;
  gap: 16px;
}

.workbench-hero {
  display: grid;
  grid-template-columns: minmax(180px, 260px) minmax(360px, 1fr) auto;
  gap: 22px;
  align-items: center;
  padding: 18px 22px;
  border: 1px solid rgba(47, 111, 159, 0.18);
  background:
    radial-gradient(circle at 5% 0%, rgba(47, 111, 159, 0.16), transparent 34%),
    linear-gradient(135deg, #ffffff 0%, #f6fbff 55%, #edf6ff 100%);
}

.workbench-hero h3 {
  margin: 0;
  color: var(--text-main);
  font-size: 28px;
}

.workbench-hero__flow {
  min-width: 0;
}

.workbench-hero__flow > span {
  display: block;
  margin-bottom: 10px;
  color: var(--text-sub);
  font-size: 14px;
}

.workbench-hero__flow-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.workbench-hero__flow-chips button {
  min-height: 34px;
  padding: 0 14px;
  border: 1px solid rgba(22, 119, 255, 0.18);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.74);
  color: #173b67;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: border-color 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
}

.workbench-hero__flow-chips button:hover {
  border-color: rgba(22, 119, 255, 0.34);
  color: var(--brand);
  box-shadow: 0 8px 18px rgba(17, 54, 99, 0.08);
}

.workbench-hero__flow-chips button:disabled {
  cursor: default;
  opacity: 1;
}

.workbench-hero__stats {
  display: grid;
  grid-template-columns: repeat(2, 128px);
  gap: 12px;
}

.workbench-hero__stats span {
  display: grid;
  gap: 8px;
  min-height: 92px;
  place-items: center;
  padding: 14px 12px;
  border: 1px solid rgba(214, 225, 241, 0.92);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 12px 24px rgba(17, 54, 99, 0.05);
}

.workbench-hero__stats strong {
  color: #1769ff;
  font-size: 30px;
  line-height: 1;
}

.workbench-hero__stats em {
  color: #3d5878;
  font-size: 13px;
  font-style: normal;
}

.workbench-guide button,
.workbench-row-card,
.todo-detail-card {
  border: 0;
  font: inherit;
  cursor: pointer;
}

.todo-preview-panel {
  padding: 18px;
}

.panel-head {
  margin-bottom: 14px;
}

.section-title {
  margin: 0;
  font-size: 18px;
}

.panel-head p {
  margin: 4px 0 0;
  color: var(--text-sub);
  font-size: 13px;
}

.workbench-guide {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.workbench-guide button {
  min-width: 128px;
  padding: 12px 18px;
  border: 1px solid rgba(22, 119, 255, 0.18);
  border-radius: 12px;
  background: var(--brand-soft);
  color: var(--brand);
  font-weight: 800;
}

.todo-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.todo-detail-panel {
  display: flex;
  flex-direction: column;
  height: 460px;
  min-height: 290px;
  overflow: hidden;
  padding: 16px;
  border: 1px solid rgba(214, 225, 241, 0.86);
}

.todo-detail-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 2px 12px;
  border-bottom: 1px solid #edf2f7;
}

.todo-detail-panel__head h3 {
  margin: 0;
  color: var(--text-main);
  font-size: 16px;
}

.todo-detail-panel__head > strong {
  min-width: 38px;
  padding: 6px 10px;
  border-radius: 999px;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 18px;
  line-height: 1;
  text-align: center;
}

.todo-detail-list {
  display: grid;
  align-content: start;
  grid-auto-rows: max-content;
  gap: 10px;
  flex: 1;
  min-height: 0;
  margin-top: 12px;
  overflow-y: auto;
  padding-right: 4px;
  scrollbar-width: thin;
  scrollbar-color: rgba(100, 116, 139, 0.32) transparent;
}

.todo-detail-list::-webkit-scrollbar {
  width: 6px;
}

.todo-detail-list::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(100, 116, 139, 0.28);
}

.todo-detail-list::-webkit-scrollbar-track {
  background: transparent;
}

.todo-detail-card {
  position: relative;
  display: grid;
  align-content: center;
  gap: 9px;
  height: 112px;
  padding: 13px 14px 13px 18px;
  border: 1px solid rgba(214, 225, 241, 0.92);
  border-radius: 12px;
  background: #ffffff;
  text-align: left;
  cursor: pointer;
  box-shadow: 0 8px 18px rgba(17, 54, 99, 0.035);
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.todo-detail-card--samplingPlan,
.todo-detail-card--sampling {
  padding-right: 88px;
}

.todo-detail-card::before {
  content: "";
  position: absolute;
  left: 0;
  top: 12px;
  bottom: 12px;
  width: 4px;
  border-radius: 0 999px 999px 0;
  background: var(--brand);
}

.todo-detail-card--sampleLogin::before {
  background: var(--success);
}

.todo-detail-card--samplingPlan::before {
  background: var(--brand);
}

.todo-detail-card--detection::before,
.todo-detail-card--review::before {
  background: var(--warning);
}

.todo-detail-card--report::before {
  background: #64748b;
}

.todo-detail-card:hover {
  border-color: rgba(22, 119, 255, 0.35);
  box-shadow: 0 12px 24px rgba(17, 54, 99, 0.08);
  transform: translateY(-1px);
}

.todo-detail-card:focus-visible {
  outline: 2px solid rgba(22, 119, 255, 0.38);
  outline-offset: 2px;
}

.todo-detail-card__action {
  position: absolute;
  right: 12px;
  bottom: 12px;
}

.todo-detail-card__action--center {
  top: 50%;
  bottom: auto;
  transform: translateY(-50%);
}

.todo-detail-card__main {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 4px;
}

.todo-detail-card__main span {
  overflow: hidden;
  color: var(--text-main);
  font-size: 15px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-detail-card__main p {
  overflow: hidden;
  margin: 0;
  color: var(--text-sub);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-detail-card__fields {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  min-width: 0;
}

.todo-detail-card__fields span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
  max-width: 100%;
  padding: 0;
}

.todo-detail-card--sampling .todo-detail-card__fields,
.todo-detail-card--detection .todo-detail-card__fields,
.todo-detail-card--review .todo-detail-card__fields {
  gap: 8px 20px;
}

.todo-detail-card--sampling .todo-detail-card__fields span,
.todo-detail-card--detection .todo-detail-card__fields span,
.todo-detail-card--review .todo-detail-card__fields span {
  gap: 6px;
}

.todo-detail-card__fields em {
  color: var(--text-sub);
  font-size: 12px;
  font-style: normal;
  white-space: nowrap;
}

.todo-detail-card__fields strong {
  overflow: hidden;
  color: var(--text-main);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-detail-card__fields .status-chip {
  min-width: 0;
  min-height: 22px;
  padding: 0 9px;
  font-size: 12px;
  line-height: 22px;
}

.todo-detail-empty {
  display: grid;
  flex: 1;
  min-height: 160px;
  place-items: center;
  color: var(--text-sub);
}

.plan-detail {
  display: grid;
  gap: 16px;
}

.plan-detail__title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid #edf2f7;
}

.plan-detail__title > strong {
  min-width: 0;
  overflow: hidden;
  color: var(--text-main);
  font-size: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plan-detail__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.plan-detail__grid > div,
.plan-detail__remark {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 12px;
  border: 1px solid #edf2f7;
  border-radius: 8px;
  background: #f8fafc;
}

.plan-detail__grid span,
.plan-detail__remark span {
  color: var(--text-sub);
  font-size: 12px;
}

.plan-detail__grid strong,
.plan-detail__remark p {
  min-width: 0;
  overflow-wrap: anywhere;
  margin: 0;
  color: var(--text-main);
  font-size: 14px;
  font-weight: 700;
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

.workbench-action-dialog :deep(.el-dialog__body) {
  padding-top: 10px;
}

:deep(.el-dialog.workbench-action-dialog) {
  max-width: calc(100vw - 32px);
}

.workbench-dialog__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.workbench-dialog__footer-left,
.workbench-dialog__footer-right {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.workbench-dialog__footer-right {
  justify-content: flex-end;
}

.detector-assign-form {
  display: grid;
  gap: 16px;
}

.detector-assign-form__selector {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
}

.detector-assign-form__selector :deep(.el-form-item) {
  margin-bottom: 0;
}

:deep(.detector-assign-dialog.el-dialog),
:global(.detector-assign-dialog.el-dialog) {
  min-height: 0;
}

:deep(.detector-assign-dialog .el-dialog__body),
:global(.detector-assign-dialog .el-dialog__body) {
  padding-bottom: 10px;
}

:deep(.detector-assign-dialog .el-dialog__footer),
:global(.detector-assign-dialog .el-dialog__footer) {
  padding-top: 12px;
}

.detector-assign-form__summary {
  display: grid;
  grid-template-columns: minmax(96px, 0.7fr) minmax(0, 1.45fr) minmax(0, 1.25fr);
  gap: 10px;
}

.detector-assign-form__summary span {
  display: grid;
  align-content: start;
  gap: 4px;
  min-height: 68px;
  padding: 10px 12px;
  border: 1px solid rgba(214, 225, 241, 0.9);
  border-radius: 10px;
  background: #f8fbff;
  color: var(--text-sub);
  font-size: 12px;
}

.detector-assign-form__summary strong {
  min-width: 0;
  color: var(--text-main);
  font-size: 14px;
  line-height: 1.45;
  overflow-wrap: anywhere;
  white-space: normal;
}

.workbench-dialog {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
  min-height: 520px;
}

.workbench-dialog--sampling {
  grid-template-columns: 322px minmax(0, 1fr);
  height: 430px;
  min-height: 0;
}

.workbench-dialog--detection {
  grid-template-columns: 320px minmax(0, 1fr);
  width: 100%;
  height: 650px;
  min-height: 0;
}

.workbench-dialog--detection-split {
  grid-template-columns: 320px minmax(0, 1fr);
  width: 100%;
  height: min(560px, calc(100vh - 170px));
  min-height: 420px;
  overflow: hidden;
}

.workbench-dialog--sample-login {
  grid-template-columns: 320px minmax(0, 1fr);
  width: 100%;
  height: 560px;
  min-height: 0;
}

.workbench-dialog--report {
  grid-template-columns: 320px minmax(0, 1fr);
  width: 100%;
  height: 650px;
  min-height: 0;
}

.workbench-dialog--sampling-plan {
  grid-template-columns: 320px minmax(0, 1fr);
  width: 100%;
  height: 560px;
  min-height: 0;
}

.workbench-dialog__side {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 12px;
  border: 1px solid rgba(214, 225, 241, 0.9);
  border-radius: 14px;
  background: #f8fbff;
}

.workbench-dialog__side-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  color: var(--text-main);
  font-weight: 800;
}

.workbench-dialog__side-filter {
  margin-bottom: 10px;
}

.workbench-dialog__side-filter :deep(.el-input__wrapper) {
  border-radius: 10px;
  background: #ffffff;
}

.workbench-row-list {
  display: grid;
  align-content: start;
  gap: 8px;
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(100, 116, 139, 0.32) transparent;
}

.workbench-row-list::-webkit-scrollbar {
  width: 6px;
}

.workbench-row-list::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(100, 116, 139, 0.28);
}

.workbench-row-list::-webkit-scrollbar-track {
  background: transparent;
}

.workbench-dialog--detection .workbench-row-list {
  max-height: 582px;
  padding-right: 4px;
}

.workbench-dialog--detection-split .workbench-row-list {
  max-height: none;
  padding-right: 4px;
}

.workbench-dialog--sample-login .workbench-row-list {
  max-height: 492px;
  padding-right: 4px;
}

.workbench-dialog--report .workbench-row-list {
  max-height: 582px;
  padding-right: 4px;
}

.workbench-dialog--sampling-plan .workbench-row-list {
  max-height: 492px;
  padding-right: 4px;
}

.workbench-dialog--detection .workbench-row-card,
.workbench-dialog--detection-split .workbench-row-card,
.workbench-dialog--review .workbench-row-card {
  min-height: 78px;
  padding: 10px 12px;
}

.workbench-dialog--detection-split .workbench-row-card {
  grid-template-columns: auto minmax(0, 1fr);
  align-items: start;
  column-gap: 8px;
}

.workbench-dialog--sample-login .workbench-row-card {
  min-height: 68px;
  padding: 10px 12px;
}

.workbench-dialog--report .workbench-row-card {
  min-height: 68px;
  padding: 10px 12px;
}

.workbench-dialog--sampling-plan .workbench-row-card {
  min-height: 68px;
  padding: 10px 12px;
}

.workbench-row-card {
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid rgba(214, 225, 241, 0.9);
  border-radius: 12px;
  background: #ffffff;
  text-align: left;
}

.workbench-row-card.is-active {
  border-color: rgba(22, 119, 255, 0.42);
  background: var(--brand-soft);
}

.workbench-row-card.is-selected {
  border-color: rgba(22, 119, 255, 0.55);
  background: #eaf3ff;
}

.workbench-row-card__check {
  margin-top: 2px;
}

.workbench-row-card__content {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.workbench-row-card strong {
  min-width: 0;
  color: var(--text-main);
  font-size: 14px;
  line-height: 1.45;
  overflow-wrap: anywhere;
  white-space: normal;
}

.workbench-dialog--detection .workbench-row-card strong,
.workbench-dialog--detection-split .workbench-row-card strong,
.workbench-dialog--review .workbench-row-card strong {
  display: block;
  min-height: 40px;
}

.workbench-row-card span {
  color: var(--text-sub);
  font-size: 12px;
  line-height: 1.5;
}

.workbench-dialog__main {
  min-width: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.workbench-dialog--sampling .workbench-dialog__main,
.workbench-dialog--detection .workbench-dialog__main {
  display: block;
  overflow: hidden;
}

.workbench-dialog--sample-login .workbench-dialog__main {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  padding-right: 0;
}

.workbench-dialog--detection-split .workbench-dialog__main {
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.workbench-dialog--report .workbench-dialog__main {
  min-height: 0;
  overflow: hidden;
  padding-right: 0;
}

.workbench-dialog--sampling-plan .workbench-dialog__main {
  min-height: 0;
}

.workbench-detail {
  display: grid;
  gap: 14px;
  min-width: 0;
}

.workbench-detail__title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
  border: 1px solid #edf2f7;
  border-radius: 8px;
  background: #f8fafc;
}

.workbench-detail__title > div {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.workbench-detail__title span,
.workbench-detail__section-head span,
.workbench-detail__grid span,
.workbench-detail__remark span {
  color: var(--text-sub);
  font-size: 12px;
}

.workbench-detail__title strong {
  min-width: 0;
  overflow: hidden;
  color: var(--text-main);
  font-size: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workbench-detail__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.workbench-detail__grid > div,
.workbench-detail__section,
.workbench-detail__remark {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 12px;
  border: 1px solid #edf2f7;
  border-radius: 8px;
  background: #f8fafc;
}

.workbench-detail__section {
  gap: 10px;
}

.workbench-detail__section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.workbench-detail__section-head strong,
.workbench-detail__grid strong,
.workbench-detail__remark p {
  min-width: 0;
  overflow-wrap: anywhere;
  margin: 0;
  color: var(--text-main);
  font-size: 14px;
  font-weight: 700;
}

.workbench-report-preview {
  width: 100%;
  height: 100%;
  min-width: 0;
  overflow: auto;
  padding: 0 4px 4px 0;
  scrollbar-width: thin;
  scrollbar-color: rgba(100, 116, 139, 0.32) transparent;
}

.workbench-report-preview::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.workbench-report-preview::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(100, 116, 139, 0.28);
}

.workbench-report-preview::-webkit-scrollbar-track {
  background: transparent;
}

.workbench-report-preview__scale {
  min-width: 820px;
}

.workbench-dialog--report-half-month .workbench-report-preview__scale {
  min-width: 1180px;
  zoom: 0.78;
}

@media print {
  body.workbench-report-printing * {
    visibility: hidden !important;
  }

  body.workbench-report-printing .workbench-dialog__side,
  body.workbench-report-printing .workbench-dialog__footer,
  body.workbench-report-printing .el-dialog__header {
    display: none !important;
  }

  body.workbench-report-printing .el-overlay,
  body.workbench-report-printing .el-overlay-dialog,
  body.workbench-report-printing .el-dialog,
  body.workbench-report-printing .el-dialog__body,
  body.workbench-report-printing .workbench-dialog,
  body.workbench-report-printing .workbench-dialog__main {
    position: static !important;
    display: block !important;
    width: auto !important;
    max-width: none !important;
    height: auto !important;
    max-height: none !important;
    margin: 0 !important;
    padding: 0 !important;
    overflow: visible !important;
    background: #ffffff !important;
    box-shadow: none !important;
    transform: none !important;
  }

  body.workbench-report-printing .workbench-report-preview,
  body.workbench-report-printing .workbench-report-preview * {
    visibility: visible !important;
  }

  body.workbench-report-printing .workbench-report-preview {
    position: fixed !important;
    inset: 0 !important;
    width: 100% !important;
    height: auto !important;
    overflow: visible !important;
    padding: 0 !important;
    background: #ffffff !important;
    box-shadow: none !important;
  }

  body.workbench-report-printing .workbench-report-preview__scale {
    min-width: 0 !important;
    zoom: 1 !important;
  }
}

.workbench-dialog--sampling :deep(.el-form) {
  height: 100%;
}

.workbench-dialog--detection .workbench-dialog__main {
  display: grid;
  align-content: start;
  gap: 14px;
  padding-right: 0;
}

.workbench-dialog--detection .summary-chips {
  margin-bottom: 0;
}

.workbench-dialog--detection .summary-chips span {
  min-height: 36px;
  padding: 8px 12px;
}

.workbench-dialog--detection .meta-grid {
  margin-bottom: 0;
}

.workbench-dialog--detection .meta-grid div {
  min-height: 64px;
  padding: 10px 12px;
}

.detection-step-row {
  display: grid;
  gap: 8px;
  min-height: 120px;
  padding: 12px 14px;
  border: 1px solid rgba(214, 225, 241, 0.9);
  border-radius: 12px;
  background: #ffffff;
}

.detection-step-row span {
  color: var(--text-sub);
  font-size: 12px;
}

.detection-step-row p {
  min-height: 68px;
  max-height: 92px;
  overflow-y: auto;
  margin: 0;
  color: var(--text-main);
  font-size: 13px;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.workbench-dialog--detection :deep(.el-form) {
  display: grid;
  grid-template-rows: 64px 112px 112px;
  gap: 14px;
}

.workbench-dialog--detection :deep(.el-form-item) {
  margin-bottom: 0;
}

.workbench-dialog--detection :deep(.el-form-item__label) {
  line-height: 26px;
  margin-bottom: 8px;
}

.workbench-dialog--detection :deep(.el-form-item__content) {
  min-height: 36px;
}

.workbench-dialog--detection .detection-textarea-item :deep(.el-textarea__inner) {
  height: 76px;
  min-height: 76px !important;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 14px;
}

.sample-login-grid {
  gap: 2px 14px;
}

.create-plan-grid {
  gap: 8px 14px;
}

.workbench-dialog--sample-login :deep(.el-form-item) {
  margin-bottom: 12px;
}

.sample-login-config-table {
  flex: 0 1 auto;
  min-height: 0;
}

.sampling-entry-grid {
  height: 100%;
  align-content: stretch;
  grid-template-rows: repeat(4, 48px) minmax(132px, 1fr);
  gap: 10px 22px;
  padding: 2px 0;
}

.sampling-entry-grid :deep(.el-form-item) {
  align-items: center;
  margin-bottom: 0;
  min-height: 48px;
}

.sampling-entry-grid :deep(.el-form-item__label) {
  line-height: 36px;
  padding-right: 10px;
  white-space: nowrap;
}

.sampling-entry-grid :deep(.el-form-item__content) {
  min-height: 36px;
  line-height: 36px;
  min-width: 0;
}

.sampling-entry-grid .form-span-2 {
  align-items: stretch;
  margin-top: 0;
  min-height: 132px;
}

.sampling-entry-grid .form-span-2 :deep(.el-form-item__label) {
  line-height: 36px;
  padding-top: 0;
}

.sampling-entry-grid .form-span-2 :deep(.el-form-item__content) {
  display: flex;
  min-height: 132px;
  line-height: 1.5;
}

.sampling-entry-grid .form-span-2 :deep(.el-textarea) {
  flex: 1;
}

.sampling-entry-grid .form-span-2 :deep(.el-textarea__inner) {
  height: 100%;
  min-height: 132px !important;
}

.form-span-2 {
  grid-column: span 2;
}

.summary-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.summary-chips span {
  padding: 8px 12px;
  border-radius: 999px;
  background: #f6f9fc;
  color: var(--text-sub);
}

.summary-chips strong {
  margin-left: 6px;
  color: var(--text-main);
}

.summary-chips strong.detector-name-text {
  color: #1d6ff2;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.meta-grid div {
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid rgba(214, 225, 241, 0.9);
  border-radius: 12px;
  background: #ffffff;
}

.meta-grid span {
  color: var(--text-sub);
  font-size: 12px;
}

.meta-grid strong {
  color: var(--text-main);
  font-size: 13px;
}

.result-value-field {
  display: flex;
  align-items: center;
  gap: 10px;
}

.result-value-input {
  width: 220px;
  max-width: 100%;
}

.result-value-input.result-value-select {
  width: 165px !important;
  min-width: 165px !important;
}

.result-value-unit {
  color: var(--text-sub);
}

.result-value-input.is-abnormal :deep(.el-input__wrapper),
.result-value-input.is-abnormal :deep(.el-input-number__wrapper),
.result-value-input.is-abnormal :deep(.el-select__wrapper) {
  border-color: #f56c6c !important;
  box-shadow: 0 0 0 1px #f56c6c inset !important;
}

:deep(.is-abnormal > .el-form-item__label) {
  color: #f56c6c;
}

.compact-table {
  margin-top: 12px;
}

.review-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.review-state-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 54px;
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.review-state-chip.is-pending {
  color: #64748b;
  background: #f1f5f9;
}

.review-state-chip.is-approved {
  color: #047857;
  background: #d1fae5;
}

.review-state-chip.is-rejected {
  color: #b91c1c;
  background: #fee2e2;
}

.review-reject-reason {
  display: flex;
  align-items: center;
  min-height: 24px;
  color: var(--text-main);
  font-size: 13px;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.compact-table :deep(.review-reject-reason-cell .cell) {
  display: flex;
  align-items: center;
  min-height: 32px;
}

.review-remark-form {
  margin-top: 12px;
}

.review-remark-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.review-remark-form :deep(.el-textarea__inner) {
  min-height: 116px !important;
}

.empty-block {
  display: grid;
  min-height: 160px;
  place-items: center;
  color: var(--text-sub);
}

@media (max-width: 1280px) {
  .workbench-hero {
    grid-template-columns: 1fr;
  }

  .workbench-hero__stats {
    grid-template-columns: repeat(2, minmax(120px, 1fr));
  }

  .todo-detail-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .workbench-dialog {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .workbench-hero__stats {
    grid-template-columns: 1fr;
  }

  .todo-detail-grid {
    grid-template-columns: 1fr;
  }

  .todo-detail-card {
    height: 132px;
  }
}

.sample-login-config-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.sample-login-config-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-regular);
}

.sample-login-config-empty {
  padding: 16px;
  text-align: center;
  color: var(--text-secondary);
  font-size: 13px;
  background-color: var(--fill-color-lighter);
  border-radius: 4px;
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

.detection-split-tabs {
  display: flex;
  gap: 10px;
  padding: 0 0 14px;
}

.detection-split-tabs__item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 12px 10px;
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 13px;
  color: #999;
}

.detection-split-tabs__item:first-child {
  border-color: #ffd666;
  background: linear-gradient(135deg, #fffbe6 0%, #fff7d6 100%);
  color: #d48806;
}

.detection-split-tabs__item:first-child:hover {
  border-color: #ffc53d;
  box-shadow: 0 2px 8px rgba(250, 173, 20, 0.2);
}

.detection-split-tabs__item:first-child.is-active {
  border-color: #faad14;
  background: linear-gradient(135deg, #fffbe6 0%, #ffe7ba 100%);
  color: #d48806;
  box-shadow: 0 3px 10px rgba(250, 173, 20, 0.25);
}

.detection-split-tabs__item:last-child {
  border-color: #91d5ff;
  background: linear-gradient(135deg, #e6f7ff 0%, #d6efff 100%);
  color: #1890ff;
}

.detection-split-tabs__item:last-child:hover {
  border-color: #69c0ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.2);
}

.detection-split-tabs__item:last-child.is-active {
  border-color: #1890ff;
  background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%);
  color: #096dd9;
  box-shadow: 0 3px 10px rgba(24, 144, 255, 0.25);
}

.detection-split-tabs__item span {
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.5px;
}

.detection-split-tabs__item.is-active span {
  font-weight: 600;
}

.detection-split-tabs__item strong {
  font-size: 22px;
  font-weight: 700;
  line-height: 1;
}

.detection-split-tabs__item:first-child strong {
  color: #faad14;
}

.detection-split-tabs__item:last-child strong {
  color: #1890ff;
}

.detection-split-tabs__item.is-active strong {
  font-size: 24px;
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

.plan-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.plan-form-span-2 {
  grid-column: 1 / -1;
}

.location-picker {
  display: flex;
  gap: 8px;
  width: 100%;
}

.location-picker .el-input {
  flex: 1;
}
</style>
