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
              :disabled="!pendingLoggableTasks.length"
              @click="openLoginDialog()"
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

      <div v-if="baseScene.showPlanSection && activeMissingSamplerPlans.length" class="panel-note plan-panel-warning">
        {{ planDispatchNotice }}
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
          <el-table-column prop="taskNo" label="任务编号" min-width="150" />
          <el-table-column label="采样封签号" min-width="150">
            <template #default="{ row }">
              <span class="plan-sampler" :class="{ 'is-empty': !row.sealNo }">
                {{ row.sealNo || '待录入' }}
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
          <el-table-column prop="startedTime" label="开始时间" width="170" />
          <el-table-column prop="abandonReason" label="废弃原因" min-width="160" show-overflow-tooltip />
          <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
          <el-table-column
            v-if="baseScene.allowTaskActions"
            label="操作"
            min-width="380"
            fixed="right"
            class-name="cell-center"
          >
            <template #default="{ row }">
              <div class="action-row">
                <el-button size="small" @click="editTaskSealNo(row)">
                  {{ row.sealNo ? '修改封签' : '录入封签' }}
                </el-button>
                <el-button size="small" @click="startTask(row)" :disabled="row.taskStatus !== pendingTaskStatus">
                  开始
                </el-button>
                <el-button
                  size="small"
                  @click="abandonTask(row)"
                  :disabled="!abandonableTaskStatuses.includes(row.taskStatus)"
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
          <el-table-column label="操作" min-width="300" fixed="right" class-name="cell-center">
            <template #default="{ row }">
              <div class="action-row">
                <el-button
                  size="small"
                  @click="openPlanEditDialog(row)"
                  :disabled="!actionablePlanStatuses.includes(row.planStatus)"
                >
                  编辑
                </el-button>
                <el-button
                  size="small"
                  @click="openDispatchDialog(row)"
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

    <el-dialog
      v-model="planDialogVisible"
      :title="editingPlanId ? '编辑采样计划' : '新增采样计划'"
      width="760px"
      destroy-on-close
      @closed="resetPlanForm"
    >
      <el-form label-width="96px">
        <div class="plan-form-grid">
          <el-form-item label="计划名称">
            <el-input v-model="planForm.planName" placeholder="请输入采样计划名称" />
          </el-form-item>
          <el-form-item label="点位来源">
            <el-select v-model="planForm.pointSource" style="width: 100%" @change="handlePlanPointSourceChange">
              <el-option label="监测点位选择" value="EXISTING" />
              <el-option label="手工填写点位" value="CUSTOM" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="planForm.pointSource === 'EXISTING'" label="监测点位">
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
          <el-form-item label="点位名称">
            <el-input
              v-model="planForm.pointName"
              :readonly="planForm.pointSource === 'EXISTING'"
              placeholder="请输入采样点位名称"
            />
          </el-form-item>
          <el-form-item label="样品类型">
            <el-select v-model="planForm.sampleType" style="width: 100%">
              <el-option
                v-for="option in sampleTypeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="周期类型">
            <el-select v-model="planForm.cycleType" style="width: 100%">
              <el-option
                v-for="option in cycleTypeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="采样人员">
            <el-select
              v-model="planForm.samplerId"
              clearable
              filterable
              style="width: 100%"
              placeholder="请选择采样员，可不填"
              :loading="samplerLoading"
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
          <el-form-item label="开始时间">
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
        <el-button type="primary" :loading="submitting" @click="submitPlanForm">{{ editingPlanId ? '确认保存' : '确认创建' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="dispatchDialogVisible"
      title="派发采样计划"
      width="560px"
      destroy-on-close
      @closed="resetDispatchForm"
    >
      <el-form label-width="96px">
        <el-form-item label="采样人员">
          <el-select
            v-model="dispatchForm.samplerId"
            filterable
            style="width: 100%"
            placeholder="请选择采样员"
            :loading="samplerLoading"
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
        <el-button @click="dispatchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitDispatchForm">确认派发</el-button>
      </template>
    </el-dialog>

    <section class="scene-grid">
      <div class="glass-panel section-block">
        <div class="section-head">
          <h3 class="section-title">页面说明</h3>
        </div>
        <div class="scene-copy">
          <p>{{ baseScene.guide }}</p>
          <p>当前页面已按菜单语义拆分，避免“同一页面换标题”的空转体验。</p>
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

    <el-dialog
      v-model="loginDialogVisible"
      title="样品登录"
      width="1040px"
      destroy-on-close
      @closed="resetLoginForm"
    >
      <el-form label-width="96px">
        <div class="login-form-grid">
          <el-form-item label="待登录任务">
            <el-select
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
          <el-form-item label="OCR封签号">
            <el-input
              v-model="loginForm.sealNo"
              placeholder="可粘贴 OCR 识别结果，自动匹配采样任务"
              @change="handleLoginSealNoChange"
            />
          </el-form-item>
          <el-form-item label="点位名称">
            <el-input v-model="loginForm.pointName" placeholder="请输入点位名称" />
          </el-form-item>
          <el-form-item label="样品类型">
            <el-select v-model="loginForm.sampleType" style="width: 100%">
              <el-option
                v-for="option in sampleTypeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="采样人员">
            <el-input v-model="loginForm.samplerName" readonly />
          </el-form-item>
          <el-form-item class="login-form-span-2" label="检测套餐">
            <el-select
              v-model="loginForm.detectionTypeId"
              clearable
              filterable
              placeholder="请选择本次样品对应的检测套餐"
              style="width: 100%"
              @change="handleLoginDetectionTypeChange"
            >
              <el-option
                v-for="item in detectionProjectOptions"
                :key="item.id"
                :label="item.typeName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="loginForm.detectionTypeId" class="login-form-span-2" label="套餐参数">
            <div class="login-config-panel">
              <div class="login-config-panel__summary">
                <span class="binding-editor__chip">
                  已选参数<strong>{{ loginDetectionConfigRows.length }}</strong>
                </span>
                <span class="login-config-panel__note">
                  选择检测套餐后，系统会带出对应检测参数；你可以临时增删参数并调整方法，仅对本次样品登录生效，不会改动原检测套餐。
                </span>
                <el-button type="primary" plain size="small" @click="appendLoginConfigRow">新增参数</el-button>
              </div>
              <el-table
                class="login-config-table"
                :data="loginDetectionConfigRows"
                size="small"
                max-height="280"
                border
              >
                <el-table-column label="检测参数名称" min-width="180">
                  <template #default="{ row, $index }">
                    <el-select
                      v-model="row.parameterId"
                      placeholder="请选择检测参数"
                      style="width: 100%"
                      @change="(value) => handleLoginConfigParameterChange(row, value)"
                    >
                      <el-option
                        v-for="option in getLoginConfigParameterOptions($index)"
                        :key="option.id"
                        :label="option.parameterName"
                        :value="option.id"
                      />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column prop="unit" label="单位" width="100">
                  <template #default="{ row }">{{ row.unit || '-' }}</template>
                </el-table-column>
                <el-table-column label="标准范围" min-width="140">
                  <template #default="{ row }">
                    {{ formatStandardRange(row.standardMin, row.standardMax, row.unit) }}
                  </template>
                </el-table-column>
                <el-table-column prop="referenceStandard" label="参考标准" min-width="160" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.referenceStandard || '-' }}</template>
                </el-table-column>
                <el-table-column label="检测方法" min-width="220">
                  <template #default="{ row }">
                    <el-select
                      v-model="row.methodId"
                      placeholder="请选择检测方法"
                      style="width: 100%"
                      :disabled="!row.parameterId || !row.methodOptions.length"
                      @change="(value) => handleLoginConfigMethodChange(row, value)"
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
                <el-table-column label="操作" width="90" class-name="cell-center" header-cell-class-name="cell-center">
                  <template #default="{ $index }">
                    <el-button link type="danger" @click="removeLoginConfigRow($index)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div v-if="!loginDetectionConfigRows.length" class="empty-block">
                当前套餐参数已被临时清空，可点击“新增参数”按需补充本次样品的检测参数与检测方法。
              </div>
            </div>
          </el-form-item>
          <el-form-item label="采样时间">
            <el-date-picker
              v-model="loginForm.samplingTime"
              type="datetime"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="天气">
            <el-input v-model="loginForm.weather" placeholder="请输入采样时天气情况" />
          </el-form-item>
          <el-form-item label="保存条件">
            <el-input v-model="loginForm.storageCondition" placeholder="例如冷藏避光、常温送检" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="loginForm.remark" placeholder="可补充样品来源、容器信息等说明" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="loginDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitSampleLogin">确认登录</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElDatePicker } from 'element-plus/es/components/date-picker/index.mjs'
import { ElDialog } from 'element-plus/es/components/dialog/index.mjs'
import { ElForm, ElFormItem } from 'element-plus/es/components/form/index.mjs'
import { ElInput } from 'element-plus/es/components/input/index.mjs'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
import TablePagination from '../components/common/TablePagination.vue'
import {
  abandonSamplingTaskApi,
  completeSamplingTaskApi,
  createSamplingPlanApi,
  dispatchSamplingPlanApi,
  fetchDetectionMethodOptionsApi,
  fetchDetectionParametersApi,
  fetchMonitoringPointsApi,
  fetchDetectionTypesApi,
  fetchSamplesApi,
  fetchSamplingPlansApi,
  fetchSamplingTasksApi,
  fetchSystemUsersApi,
  loginSampleApi,
  pauseSamplingPlanApi,
  resumeSamplingPlanApi,
  resumeSamplingTaskApi,
  startSamplingTaskApi,
  updateSamplingTaskSealNoApi,
  updateSamplingPlanApi
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
  sampleRegisterStatusLabelMap,
  sampleStatusLabelMap,
  sampleTypeOptions,
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
const loginDialogVisible = ref(false)
const planDialogVisible = ref(false)
const dispatchDialogVisible = ref(false)
const editingPlanId = ref(null)
const submitting = ref(false)
const monitoringPointOptions = ref([])
const monitoringPointLoading = ref(false)
const samplerOptions = ref([])
const samplerLoading = ref(false)
const detectionProjectOptions = ref([])
const detectionParameterOptions = ref([])
const detectionMethodOptions = ref([])

const loginForm = reactive({
  taskId: null,
  sealNo: '',
  pointId: null,
  pointName: '',
  sampleType: '',
  detectionItems: '',
  detectionTypeId: null,
  detectionTypeName: '',
  detectionConfigItems: [],
  samplingTime: '',
  samplerId: null,
  samplerName: '',
  weather: '',
  storageCondition: '',
  remark: ''
})

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
  cycleType: dailyCycleType,
  remark: ''
})

const dispatchForm = reactive({
  planId: null,
  samplingTime: '',
  samplerId: null,
  samplerName: ''
})

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
    note: '任务分配页强调今天要做什么，开始采样前必须先录入封签号，上方统计卡可切换到进行中、已完成、已废弃视角。',
    guide: '如需安排周期计划，可直接在本页下方创建并派发；任务完成后再进入样品登录。',
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
    tableSubtitle: '本页聚焦已完成、已废弃任务，操作区切换为只读查询视角。',
    note: '历史任务页用于追溯与复盘，不再承载现场执行按钮，避免误操作。',
    guide: '如历史任务已形成样品，可直接跳转到样品台账继续核验封签与留痕。',
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
      { path: '/detection-analysis', label: '检测分析', desc: '继续进入化验室检测流程' }
    ]
  },
  '/sample-login': {
    key: 'sample-login',
    title: '样品登录',
    subtitle: '将已完成采样任务转成正式样品，生成样品编号并承接后续检测、审核、报告链路。',
    tableTitle: '已登记样品',
    tableSubtitle: '默认展示已登记样品，并通过统计卡切换到待审核、退回重检、闭环完成等状态。',
    note: '样品登录页优先解决未登记任务，支持封签号识别后自动带出任务，且不再自动生成封签号。',
    guide: '如本页没有可登录样品，请先回到任务分配完成采样任务；若样品已登记，可继续前往检测分析。',
    mode: 'sample',
    defaultStatKey: 'samples:logged',
    allowTaskActions: false,
    showPlanSection: false,
    emptyText: '暂无样品登录数据',
    taskFilter: () => true,
    sampleFilter: () => true,
    quickLinks: [
      { path: '/task-assign', label: '任务分配', desc: '先完成现场采样任务，再进行样品登录' },
      { path: '/detection-analysis', label: '检测分析', desc: '样品登录完成后进入化验室检测流程' },
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
    guide: '如需新增样品，请回到样品登录；如需继续推进流程，可跳转到检测分析或结果审查。',
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

function isTaskRegistered(task) {
  if (!task) {
    return false
  }
  if (task.sampleRegisterStatus === 'REGISTERED' || task.sampleId) {
    return true
  }
  return samples.value.some((sample) => sample.taskId === task.id)
}

const pendingLoggableCount = computed(() =>
  tasks.value.filter((item) =>
    item.taskStatus === completedTaskStatus && !isTaskRegistered(item)
  ).length
)

const firstCompletableTask = computed(() =>
  taskSceneRecords.value.find((item) => completableTaskStatuses.includes(item.taskStatus))
)

const firstLoggableTask = computed(() =>
  tasks.value.find((item) =>
    item.taskStatus === completedTaskStatus && !isTaskRegistered(item)
  )
)

const pendingLoggableTasks = computed(() =>
  tasks.value.filter((item) =>
    item.taskStatus === completedTaskStatus && !isTaskRegistered(item)
  )
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
      desc: '被审核退回，等待重新检测的样品'
    },
    {
      key: 'samples:completed',
      label: '闭环完成',
      value: sampleSceneRecords.value.filter((item) => item.sampleStatus === completedSampleStatus).length,
      desc: '检测、审核已完成闭环的样品'
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
    label: '待补采样员',
    value: activeMissingSamplerPlans.value.length,
    desc: '已启用但未指定采样员，自动派发会跳过'
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
  return isTaskRegistered(tasks.value.find((item) => item.id === taskId))
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
      status: 1
    })
    const records = result.records || []
    samplerOptions.value = records.filter((item) => String(item.roleCode || '').trim().toUpperCase() === 'SAMPLER')
  } finally {
    samplerLoading.value = false
  }
}

function handlePlanSamplerChange(userId) {
  const user = samplerOptions.value.find((item) => item.id === userId)
  planForm.samplerId = user?.id || null
  planForm.samplerName = user?.realName || user?.username || ''
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
  planForm.cycleType = dailyCycleType
  planForm.remark = ''
}

async function openPlanDialog() {
  resetPlanForm()
  planForm.planName = `采样计划-${dayjs().format('MMDD-HHmm')}`
  planForm.startTime = dayjs().format('YYYY-MM-DD HH:mm:ss')
  planForm.endTime = dayjs().add(7, 'day').format('YYYY-MM-DD HH:mm:ss')
  await Promise.all([loadMonitoringPoints(), loadSamplers()])
  if (monitoringPointOptions.value.length) {
    handlePlanPointChange(monitoringPointOptions.value[0].id)
  } else {
    planForm.pointSource = 'CUSTOM'
  }
  planDialogVisible.value = true
}

async function openPlanEditDialog(row) {
  await Promise.all([loadMonitoringPoints(), loadSamplers()])
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
    await loadPlans()
  } finally {
    submitting.value = false
  }
}

async function legacyCreatePlanDoNotUse() {
  return null
}

async function openDispatchDialog(row) {
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
  if (!dispatchForm.planId || !dispatchForm.samplerId || !dispatchForm.samplerName) {
    ElMessage.warning('派发任务前必须指定采样员')
    return
  }
  submitting.value = true
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
    await Promise.all([loadPlans(), loadTasks()])
  } finally {
    submitting.value = false
  }
}

async function legacyDispatchDoNotUse(row) {
  return row || null
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

async function promptTaskSealNo(row, options = {}) {
  const {
    title = row?.sealNo ? '修改采样封签号' : '录入采样封签号',
    confirmButtonText = '确认保存'
  } = options
  try {
    const { value } = await ElMessageBox.prompt(
      '请输入采样封签号，支持手工录入或粘贴 OCR 识别结果。',
      title,
      {
        confirmButtonText,
        cancelButtonText: '取消',
        inputValue: row?.sealNo || '',
        inputPlaceholder: '请输入采样封签号',
        inputValidator: (inputValue) => String(inputValue || '').trim() ? true : '封签号不能为空'
      }
    )
    return String(value || '').trim()
  } catch {
    return null
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
          methodName: item.methodName || `检测方法-${item.id}`
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
        referenceStandard: parameter.referenceStandard || '',
        methodId: currentMethod?.id || '',
        methodName: currentMethod?.methodName || '',
        methodOptions
      }
    })
    .filter(Boolean)
}

const loginDetectionConfigRows = computed(() => loginForm.detectionConfigItems)
const enabledLoginParameterOptions = computed(() => (
  detectionParameterOptions.value
    .filter((item) => item.enabled === 1)
    .sort((left, right) => String(left.parameterName || '').localeCompare(String(right.parameterName || ''), 'zh-CN'))
))

function createEmptyLoginConfigRow() {
  return {
    parameterId: '',
    parameterName: '',
    unit: '',
    standardMin: null,
    standardMax: null,
    referenceStandard: '',
    methodId: '',
    methodName: '',
    methodOptions: []
  }
}

function getLoginMethodOptionsByParameter(parameterId) {
  if (!parameterId) {
    return []
  }
  return detectionMethodOptions.value
    .filter((item) => String(item.parameterId) === String(parameterId) && item.enabled === 1)
    .map((item) => ({
      id: String(item.id),
      methodName: item.methodName || `检测方法-${item.id}`
    }))
}

function getLoginConfigParameterOptions(currentIndex) {
  const selectedParameterIds = new Set(
    loginDetectionConfigRows.value
      .map((item, index) => index === currentIndex ? '' : String(item.parameterId || '').trim())
      .filter(Boolean)
  )
  return enabledLoginParameterOptions.value.filter((item) => !selectedParameterIds.has(String(item.id)))
}

function appendLoginConfigRow() {
  loginForm.detectionConfigItems.push(createEmptyLoginConfigRow())
}

function removeLoginConfigRow(index) {
  loginForm.detectionConfigItems.splice(index, 1)
}

function handleLoginConfigParameterChange(row, parameterId) {
  const parameter = detectionParameterOptions.value.find((item) => String(item.id) === String(parameterId || ''))
  row.parameterId = String(parameterId || '')
  row.parameterName = parameter?.parameterName || ''
  row.unit = parameter?.unit || ''
  row.standardMin = parameter?.standardMin ?? null
  row.standardMax = parameter?.standardMax ?? null
  row.referenceStandard = parameter?.referenceStandard || ''
  row.methodOptions = getLoginMethodOptionsByParameter(parameterId)
  const nextMethod = row.methodOptions.find((item) => item.id === row.methodId) || row.methodOptions[0] || null
  row.methodId = nextMethod?.id || ''
  row.methodName = nextMethod?.methodName || ''
}

async function editTaskSealNo(row) {
  const sealNo = await promptTaskSealNo(row)
  if (!sealNo) {
    return
  }
  await updateSamplingTaskSealNoApi(row.id, { sealNo })
  ElMessage.success('采样封签号已保存。')
  await loadTasks()
}

async function startTask(row) {
  const sealNo = row?.sealNo?.trim() || await promptTaskSealNo(row, {
    title: '开始任务前请先录入封签号',
    confirmButtonText: '录入并开始'
  })
  if (!sealNo) {
    return
  }
  await startSamplingTaskApi(row.id, {
    sealNo,
    remark: '采样任务开始执行'
  })
  ElMessage.success('采样任务已开始执行。')
  await Promise.all([loadTasks(), loadPlans()])
}

async function abandonTask(row) {
  await abandonSamplingTaskApi(row.id, {
    reason: '现场条件暂不满足采样要求',
    remark: '请确认现场情况后重新安排采样任务'
  })
  ElMessage.success('采样任务已废弃。')
  await Promise.all([loadTasks(), loadPlans()])
}

async function resumeTask(row) {
  await resumeSamplingTaskApi(row.id, { remark: '采样任务恢复为待处理状态' })
  ElMessage.success('采样任务已恢复。')
  await Promise.all([loadTasks(), loadPlans()])
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

async function legacyLoginSampleDoNotUse() {
  return null
}

function applyTaskToLoginForm(task) {
  if (!task) {
    return
  }
  loginForm.taskId = task.id
  loginForm.sealNo = task.sealNo || ''
  loginForm.pointId = task.pointId || null
  loginForm.pointName = task.pointName || ''
  loginForm.sampleType = task.sampleType || ''
  loginForm.detectionItems = parseDetectionItemsText(task.detectionItems)
  loginForm.detectionTypeId = null
  loginForm.detectionTypeName = ''
  loginForm.detectionConfigItems = []
  loginForm.samplingTime = task.samplingTime || dayjs().format('YYYY-MM-DD HH:mm:ss')
  loginForm.samplerId = task.samplerId || null
  loginForm.samplerName = task.samplerName || ''
  loginForm.weather = ''
  loginForm.storageCondition = ''
  loginForm.remark = task.remark || ''

  const preferredType = detectionProjectOptions.value.find((item) => item.typeName === loginForm.detectionItems)
    || detectionProjectOptions.value[0]
  if (preferredType) {
    handleLoginDetectionTypeChange(preferredType.id)
  }
}

function resetLoginForm() {
  loginForm.taskId = null
  loginForm.sealNo = ''
  loginForm.pointId = null
  loginForm.pointName = ''
  loginForm.sampleType = ''
  loginForm.detectionItems = ''
  loginForm.detectionTypeId = null
  loginForm.detectionTypeName = ''
  loginForm.detectionConfigItems = []
  loginForm.samplingTime = ''
  loginForm.samplerId = null
  loginForm.samplerName = ''
  loginForm.weather = ''
  loginForm.storageCondition = ''
  loginForm.remark = ''
}

async function openLoginDialog(task = firstLoggableTask.value) {
  if (!pendingLoggableTasks.value.length) {
    ElMessage.warning('当前没有待登录的已完成采样任务')
    return
  }
  await loadDetectionProjects()
  resetLoginForm()
  applyTaskToLoginForm(task || pendingLoggableTasks.value[0])
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

function handleLoginConfigMethodChange(row, methodId) {
  const method = (row.methodOptions || []).find((item) => item.id === String(methodId || ''))
  row.methodId = String(methodId || '')
  row.methodName = method?.methodName || ''
}

function handleLoginSealNoChange(value) {
  const sealNo = String(value || loginForm.sealNo || '').trim()
  loginForm.sealNo = sealNo
  if (!sealNo) {
    return
  }
  const task = pendingLoggableTasks.value.find((item) => item.sealNo === sealNo)
  if (!task) {
    ElMessage.warning('未找到与该封签号匹配的待登录采样任务')
    return
  }
  applyTaskToLoginForm(task)
}

function formatPendingTaskLabel(task) {
  const pointName = task?.pointName || '未命名点位'
  const samplerName = task?.samplerName || '未指定采样员'
  const taskNo = task?.taskNo || '未生成任务编号'
  const sealNo = task?.sealNo || '未录入封签号'
  return `${taskNo} / ${sealNo} / ${pointName} / ${samplerName}`
}

async function submitSampleLogin() {
  if (!loginForm.taskId || !loginForm.sealNo || !loginForm.pointId || !loginForm.pointName || !loginForm.sampleType || !loginForm.detectionTypeId || !loginForm.detectionItems || !loginForm.samplingTime) {
    ElMessage.warning('请完整填写样品登录信息')
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
        unit: item.unit,
        standardMin: item.standardMin,
        standardMax: item.standardMax,
        referenceStandard: item.referenceStandard,
        methodId: item.methodId,
        methodName: item.methodName
      }))
    })
    loginDialogVisible.value = false
    ElMessage.success(`样品登录完成，封签编号：${sample?.sealNo || '-'}`)
    sampleQuery.pageNum = 1
    await Promise.all([loadTasks(), loadSamples()])
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  syncRouteState()
  await Promise.all([loadPlans(), loadTasks(), loadSamples(), loadDetectionProjects()])
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

.plan-sampler {
  color: var(--text-main);
}

.plan-sampler.is-empty {
  color: #d14343;
  font-weight: 600;
}

.plan-stats {
  margin-bottom: 12px;
}

.plan-panel-warning {
  margin-bottom: 12px;
  color: #9a6700;
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

.login-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.login-form-span-2 {
  grid-column: 1 / -1;
}

.login-config-panel {
  display: grid;
  gap: 12px;
  width: 100%;
  padding: 16px;
  border: 1px solid var(--line-soft);
  border-radius: 18px;
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

.empty-block {
  padding: 18px 16px;
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

  .login-form-grid {
    grid-template-columns: 1fr;
  }

  .plan-form-grid {
    grid-template-columns: 1fr;
  }
}
</style>








