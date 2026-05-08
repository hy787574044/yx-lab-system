<template>
  <div class="content-grid system-page" v-loading="loading">
    <section class="glass-panel section-block page-hero">
      <div>
        <h2 class="page-title">{{ currentScene.title }}</h2>
        <p class="page-subtitle">{{ currentScene.subtitle }}</p>
      </div>
      <div class="hero-tags">
        <span
          v-for="tag in currentTags"
          :key="tag.label"
          :class="['status-chip', tag.type || 'info']"
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

      <template v-if="isUserScene">
        <div class="toolbar-panel">
          <div class="toolbar-row">
            <div class="toolbar-fields">
              <label class="toolbar-field toolbar-field--medium">
                <span>关键字</span>
                <el-input
                  v-model="userQuery.keyword"
                  clearable
                  placeholder="请输入用户名、姓名、手机号或角色编码"
                  @keyup.enter="handleUserSearch"
                />
              </label>
              <label class="toolbar-field">
                <span>状态</span>
                <el-select v-model="userQuery.status" clearable placeholder="请选择状态">
                  <el-option
                    v-for="item in userStatusOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </label>
            </div>

            <div class="toolbar-actions">
              <el-button type="primary" @click="handleUserSearch">查询</el-button>
              <el-button @click="resetUserQuery">重置</el-button>
              <el-button @click="reloadData">刷新</el-button>
              <el-button type="primary" plain @click="openUserDialog()">新增用户</el-button>
            </div>
          </div>
          <div class="panel-note">
            当前已接入真实用户管理接口，支持分页查询、新增、编辑、删除；新增密码必填，编辑留空不改密码，且不能删除当前登录用户。
          </div>
        </div>

        <div class="table-card">
          <el-table
            class="list-table"
            :data="visibleUserRows"
            stripe
            max-height="460"
            empty-text="暂无用户管理数据"
          >
            <el-table-column prop="username" label="用户名" min-width="140" />
            <el-table-column prop="realName" label="姓名" min-width="120" />
            <el-table-column prop="roleCode" label="角色编码" min-width="140" />
            <el-table-column prop="phone" label="手机号" min-width="140">
              <template #default="{ row }">
                {{ row.phone || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="110" header-cell-class-name="cell-center" class-name="cell-center">
              <template #default="{ row }">
                <span :class="['status-chip', getUserStatusClass(row.status)]">
                  {{ getUserStatusLabel(row.status) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="createdName" label="创建人" min-width="120">
              <template #default="{ row }">
                {{ row.createdName || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="createdTime" label="创建时间" min-width="170">
              <template #default="{ row }">
                {{ row.createdTime || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="updatedTime" label="更新时间" min-width="170">
              <template #default="{ row }">
                {{ row.updatedTime || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right" class-name="cell-center">
              <template #default="{ row }">
                <el-button link type="primary" @click="openUserDialog(row)">编辑</el-button>
                <el-button
                  link
                  type="danger"
                  :disabled="String(row.id) === String(currentLoginUser.userId || '')"
                  @click="removeUser(row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <TablePagination
            v-model:current-page="userQuery.pageNum"
            v-model:page-size="userQuery.pageSize"
            :total="userTotal"
            @change="loadUsers"
          />
        </div>
      </template>

      <template v-else>
        <div class="toolbar-panel">
          <div class="toolbar-row">
            <div class="toolbar-fields">
              <label class="toolbar-field toolbar-field--medium">
                <span>关键字</span>
                <el-input
                  v-model="sceneKeyword"
                  clearable
                  :placeholder="currentScene.keywordPlaceholder"
                />
              </label>
            </div>

            <div class="toolbar-actions">
              <el-button type="primary" @click="reloadData">刷新</el-button>
              <el-button
                v-for="item in currentScene.actions"
                :key="item.label"
                :type="item.type || 'default'"
                :plain="item.plain !== false"
                @click="goRoute(item.path)"
              >
                {{ item.label }}
              </el-button>
            </div>
          </div>
          <div class="panel-note">{{ currentScene.note }}</div>
        </div>

        <div class="table-card">
          <el-table
            class="list-table"
            :data="visibleSceneRows"
            stripe
            max-height="460"
            :empty-text="currentScene.emptyText"
          >
            <el-table-column
              v-for="column in currentScene.columns"
              :key="column.key"
              :prop="column.key"
              :label="column.label"
              :min-width="column.minWidth"
              :width="column.width"
              :show-overflow-tooltip="column.overflow !== false"
              :header-cell-class-name="column.align === 'center' ? 'cell-center' : ''"
              :class-name="column.align === 'center' ? 'cell-center' : ''"
            >
              <template #default="{ row }">
                <span v-if="column.type === 'status'" :class="['status-chip', resolveSceneTone(column, row)]">
                  {{ resolveSceneValue(column, row) }}
                </span>
                <span v-else>{{ resolveSceneValue(column, row) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>
    </section>

    <section class="scene-grid">
      <div class="glass-panel section-block">
        <div class="section-head">
          <h3 class="section-title">页面说明</h3>
        </div>
        <div class="scene-copy">
          <p>{{ currentScene.guide }}</p>
          <p>{{ currentScene.constraint }}</p>
        </div>
      </div>

      <div class="glass-panel section-block">
        <div class="section-head">
          <h3 class="section-title">关联入口</h3>
        </div>
        <div class="quick-links">
          <button
            v-for="item in currentScene.quickLinks"
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
      v-model="dialogVisible"
      :title="userForm.id ? '编辑用户' : '新增用户'"
      width="760px"
      destroy-on-close
      @closed="resetUserForm"
    >
      <el-form ref="formRef" :model="userForm" :rules="userRules" label-width="100px">
        <div class="form-grid">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="userForm.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="姓名" prop="realName">
            <el-input v-model="userForm.realName" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="角色编码" prop="roleCode">
            <el-select
              v-model="userForm.roleCode"
              filterable
              allow-create
              default-first-option
              placeholder="请选择或直接输入角色编码"
              style="width: 100%"
            >
              <el-option
                v-for="item in roleCodeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="userForm.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="userForm.status">
              <el-radio-button :label="1">启用</el-radio-button>
              <el-radio-button :label="0">停用</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="登录密码" prop="password">
            <el-input
              v-model="userForm.password"
              type="password"
              show-password
              :placeholder="userForm.id ? '留空表示不修改密码' : '请输入登录密码'"
            />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitUserForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import TablePagination from '../components/common/TablePagination.vue'
import {
  createSystemUserApi,
  deleteSystemUserApi,
  fetchSystemUsersApi,
  updateSystemUserApi
} from '../api/lab'
import { getUser } from '../utils/auth'
import { labMenuGroups } from '../router/menuConfig'
import {
  DEFAULT_PAGE_SIZE,
  cycleTypeLabelMap,
  detectionStatusLabelMap,
  instrumentStatusLabelMap,
  planStatusLabelMap,
  pointStatusLabelMap,
  reportStatusLabelMap,
  sampleStatusLabelMap,
  taskStatusLabelMap
} from '../utils/labEnums'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const activeStatKey = ref('all')
const sceneKeyword = ref('')
const currentLoginUser = reactive(getUser() || {})

const isUserScene = computed(() => route.path === '/system-users')

const userQuery = reactive({
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE,
  keyword: '',
  status: ''
})

const userRows = ref([])
const userTotal = ref(0)
const userSummaryRows = ref([])
const userSummaryTotal = ref(0)

const userStatusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]

const roleCodeOptions = [
  { label: '系统管理员 ADMIN', value: 'ADMIN' },
  { label: '采样员 SAMPLER', value: 'SAMPLER' },
  { label: '检测员 DETECTOR', value: 'DETECTOR' },
  { label: '审核员 REVIEWER', value: 'REVIEWER' },
  { label: '报告员 REPORTER', value: 'REPORTER' }
]

const userForm = reactive(createDefaultUserForm())

const userRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  password: [{
    validator: (_, value, callback) => {
      if (!userForm.id && !String(value || '').trim()) {
        callback(new Error('新增用户时请输入登录密码'))
        return
      }
      callback()
    },
    trigger: 'blur'
  }]
}

const menuRows = computed(() => {
  const rows = []
  for (const group of labMenuGroups) {
    rows.push({
      id: `${group.id}-group`,
      menuName: group.title,
      level: '二级目录',
      parentName: '水质管理',
      routePath: group.defaultPath,
      componentName: '目录容器',
      status: '已启用',
      remark: `包含 ${group.children.length} 个业务入口`
    })

    for (const item of group.children) {
      rows.push({
        id: item.path,
        menuName: item.title,
        level: '三级页面',
        parentName: group.title,
        routePath: item.path,
        componentName: item.componentKey,
        status: '已启用',
        remark: item.subtitle
      })
    }
  }
  return rows
})

const dictRows = computed(() => [
  ['instrument_status', '设备状态字典', '仪器管理', instrumentStatusLabelMap],
  ['point_status', '点位状态字典', '监测点位', pointStatusLabelMap],
  ['plan_status', '计划状态字典', '采样计划', planStatusLabelMap],
  ['task_status', '任务状态字典', '采样任务', taskStatusLabelMap],
  ['sample_status', '样品状态字典', '样品管理', sampleStatusLabelMap],
  ['detection_status', '检测状态字典', '检测管理', detectionStatusLabelMap],
  ['report_status', '报告状态字典', '报告管理', reportStatusLabelMap],
  ['cycle_type', '周期类型字典', '基础配置', cycleTypeLabelMap]
].map(([dictCode, dictName, moduleName, labelMap]) => ({
  id: dictCode,
  dictCode,
  dictName,
  moduleName,
  itemCount: Object.keys(labelMap || {}).length,
  sampleText: Object.values(labelMap || {}).slice(0, 4).join('、'),
  status: '已生效'
})))

const sceneRowsMap = computed(() => ({
  '/system-orgs': [
    {
      id: 'local-domain',
      orgName: '本地实验室账户域',
      orgType: '本地账号体系',
      memberCount: userSummaryTotal.value || userSummaryRows.value.length,
      source: 'lab_user',
      status: '已接入',
      remark: '当前登录、认证与用户管理均已接入本地用户表'
    },
    {
      id: 'business-domain',
      orgName: '业务共享对象域',
      orgType: '业务复用',
      memberCount: 7,
      source: '现有业务菜单',
      status: '已复用',
      remark: '文档、报告、移动闭环等业务已复用当前用户域'
    },
    {
      id: 'unified-domain',
      orgName: '统一平台组织域',
      orgType: '平台集成',
      memberCount: 0,
      source: '/api/unified',
      status: '待接入',
      remark: '如需正式组织树，可在后续统一平台集成后升级'
    }
  ],
  '/system-roles': [
    buildRoleRow('ADMIN', '系统管理员', '全系统', '负责系统配置、账号维护与基础资料管理'),
    buildRoleRow('SAMPLER', '采样员', '采样闭环', '负责采样任务执行、样品登录与现场填报'),
    buildRoleRow('DETECTOR', '检测员', '检测闭环', '负责检测分析、结果录入与重检提交'),
    buildRoleRow('REVIEWER', '审核员', '审核闭环', '负责审核通过、驳回与重检门禁控制'),
    buildRoleRow('REPORTER', '报告员', '报告闭环', '负责正式报告生成、发布与推送')
  ],
  '/system-menus': menuRows.value,
  '/system-logs': [
    {
      id: 'auth',
      sourceName: '登录认证日志',
      eventCount: 2,
      retentionRule: '登录有效期内可追溯',
      status: '已生效',
      remark: '已记录登录态与用户上下文'
    },
    {
      id: 'workflow',
      sourceName: '业务流程日志',
      eventCount: 8,
      retentionRule: '按业务表长期保留',
      status: '已生效',
      remark: '采样、检测、审核、报告均已形成业务留痕'
    },
    {
      id: 'sample-seal',
      sourceName: '样品封签留痕',
      eventCount: 4,
      retentionRule: '随样品生命周期保留',
      status: '已生效',
      remark: '封签编号、确认人、确认时间均已留痕'
    },
    {
      id: 'push',
      sourceName: '报告推送日志',
      eventCount: 3,
      retentionRule: '按报告生命周期保留',
      status: '已生效',
      remark: '报告正式产物与推送结果已记录'
    },
    {
      id: 'audit-center',
      sourceName: '统一审计中心',
      eventCount: 0,
      retentionRule: '待规划',
      status: '待接入',
      remark: '如果后续确实需要，再做统一日志中心即可'
    }
  ],
  '/system-dicts': dictRows.value,
  '/system-forms': [
    {
      id: 'login-form',
      formName: '登录表单',
      moduleName: '系统认证',
      pagePath: '/login',
      fieldCount: 2,
      formType: '页面表单',
      status: '已配置',
      remark: '用户名与密码统一风格'
    },
    {
      id: 'mobile-login-form',
      formName: '移动登录表单',
      moduleName: '移动闭环',
      pagePath: '/mobile/login',
      fieldCount: 2,
      formType: '页面表单',
      status: '已配置',
      remark: '移动端统一登录入口'
    },
    {
      id: 'user-form',
      formName: '用户管理表单',
      moduleName: '系统管理',
      pagePath: '/system-users',
      fieldCount: 6,
      formType: '弹窗表单',
      status: '已配置',
      remark: '已接入真实新增、编辑能力'
    },
    {
      id: 'instrument-form',
      formName: '设备台账表单',
      moduleName: '仪器管理',
      pagePath: '/instrument-ledger',
      fieldCount: 11,
      formType: '弹窗表单',
      status: '已配置',
      remark: '设备台账正式表单'
    },
    {
      id: 'maintenance-form',
      formName: '设备维修表单',
      moduleName: '设备维修',
      pagePath: '/instrument-maintenance',
      fieldCount: 8,
      formType: '弹窗表单',
      status: '已配置',
      remark: '维修原因、结果、费用与留痕已落地'
    },
    {
      id: 'mobile-review-form',
      formName: '移动审核表单',
      moduleName: '移动闭环',
      pagePath: '/mobile',
      fieldCount: 3,
      formType: '移动弹窗',
      status: '已配置',
      remark: '支持移动审核通过与驳回'
    }
  ]
}))

const sceneConfigMap = {
  '/system-users': {
    title: '用户管理',
    subtitle: '对系统使用用户进行集中维护，统一管理登录账号、角色编码、手机号与启停状态。',
    tableTitle: '用户台账',
    tableSubtitle: '当前页面已接入真实用户管理接口，可直接完成正式业务维护。',
    guide: '用户管理已经从演示型静态页面升级为真实业务页，支持分页、查询、新增、编辑、删除，并保留统一样式。',
    constraint: '后端已限制新增时密码必填、编辑时留空不改密码、用户名唯一、不能删除当前登录用户。',
    quickLinks: [
      { label: '机构管理', path: '/system-orgs', desc: '查看当前用户域承接方案与后续组织扩展建议' },
      { label: '角色管理', path: '/system-roles', desc: '查看现有角色编码与职责划分' },
      { label: '日志管理', path: '/system-logs', desc: '查看当前留痕来源与后续统一审计建议' }
    ]
  },
  '/system-orgs': {
    title: '机构管理',
    subtitle: '查看当前系统用户域、业务复用域与后续统一平台组织树接入策略。',
    tableTitle: '组织承接清单',
    tableSubtitle: '当前阶段先保证业务可用，再逐步升级为正式组织树模型。',
    keywordPlaceholder: '请输入组织名称、组织类型、数据来源或说明',
    note: '当前系统已能支撑正式业务使用，但真正的组织树、部门层级和岗位模型仍可在后续系统治理阶段补齐。',
    emptyText: '暂无机构管理清单',
    guide: '本页用于说明当前账号体系如何承接正式业务流程，避免后续扩展时再次拆改用户域。',
    constraint: '如果未来需要按部门、厂站、班组做强权限控制，建议新增正式组织表，而不是继续复用静态承接信息。',
    actions: [
      { label: '查看用户管理', path: '/system-users', type: 'primary', plain: false },
      { label: '查看角色管理', path: '/system-roles' }
    ],
    quickLinks: [
      { label: '用户管理', path: '/system-users', desc: '维护账号信息并同步查看启停状态' },
      { label: '角色管理', path: '/system-roles', desc: '查看角色编码与业务职责分工' },
      { label: '系统首页', path: '/dashboard', desc: '回到统一业务工作台继续流转业务' }
    ],
    columns: [
      { key: 'orgName', label: '组织名称', minWidth: 180 },
      { key: 'orgType', label: '组织类型', minWidth: 140 },
      { key: 'memberCount', label: '成员数', width: 110, align: 'center' },
      { key: 'source', label: '数据来源', minWidth: 140 },
      { key: 'status', label: '接入状态', width: 110, align: 'center', type: 'status', tone: (row) => row.status === '待接入' ? 'warning' : 'success' },
      { key: 'remark', label: '说明', minWidth: 260 }
    ]
  },
  '/system-roles': {
    title: '角色管理',
    subtitle: '梳理当前系统角色编码、职责边界与实际账号分配情况。',
    tableTitle: '角色台账',
    tableSubtitle: '角色编码可直接用于登录后权限识别和业务职责展示。',
    keywordPlaceholder: '请输入角色编码、角色名称、适用范围或说明',
    note: '当前先采用角色编码直连业务场景的方式，已经足以承接实验室正式流程；后续如需按钮级权限，可继续细分权限点。',
    emptyText: '暂无角色管理清单',
    guide: '本页用于对齐系统中真实在用的角色集合，避免用户管理、流程门禁和移动闭环中的角色编码不一致。',
    constraint: '如果后续增加角色授权矩阵，建议继续复用当前角色编码，避免前后端枚举与数据表再次改名。',
    actions: [
      { label: '查看用户管理', path: '/system-users', type: 'primary', plain: false },
      { label: '查看菜单管理', path: '/system-menus' }
    ],
    quickLinks: [
      { label: '用户管理', path: '/system-users', desc: '直接维护角色编码对应的账号' },
      { label: '菜单管理', path: '/system-menus', desc: '查看角色能力承接到哪些业务页面' },
      { label: '移动工作台', path: '/mobile', desc: '核对移动端角色看到的业务闭环' }
    ],
    columns: [
      { key: 'roleCode', label: '角色编码', minWidth: 140 },
      { key: 'roleName', label: '角色名称', minWidth: 140 },
      { key: 'userCount', label: '分配人数', width: 110, align: 'center' },
      { key: 'scope', label: '适用范围', minWidth: 130 },
      { key: 'status', label: '分配状态', width: 110, align: 'center', type: 'status', tone: (row) => row.status === '已分配' ? 'success' : 'warning' },
      { key: 'remark', label: '说明', minWidth: 240 }
    ]
  },
  '/system-menus': {
    title: '菜单管理',
    subtitle: '展示当前“顶部一级固定 + 左侧二级三级展开”的正式菜单结构承接结果。',
    tableTitle: '菜单结构清单',
    tableSubtitle: '一级固定为“水质管理”，原有菜单已整体下沉到左侧。',
    keywordPlaceholder: '请输入菜单名称、层级、路由或组件名',
    note: '菜单结构已经完成重构，这里主要作为台账页展示当前实际启用的菜单节点，便于后续与功能清单逐项对照。',
    emptyText: '暂无菜单结构清单',
    guide: '本页沉淀菜单结构现状，方便继续补空白页、对齐工单清单，并确认一级二级三级菜单布局是否完整。',
    constraint: '菜单配置已稳定，后续只需继续补齐菜单对应的真实业务页，不需要再折返改层级。',
    actions: [
      { label: '查看系统首页', path: '/dashboard', type: 'primary', plain: false },
      { label: '查看表单管理', path: '/system-forms' }
    ],
    quickLinks: [
      { label: '历史任务', path: '/task-history', desc: '验证任务菜单已落到正式业务页' },
      { label: '历史检测', path: '/detection-history', desc: '验证检测菜单已落到正式业务页' },
      { label: '历史审查', path: '/review-history', desc: '验证审查菜单已落到正式业务页' }
    ],
    columns: [
      { key: 'menuName', label: '菜单名称', minWidth: 180 },
      { key: 'level', label: '层级', width: 110, align: 'center' },
      { key: 'parentName', label: '上级节点', minWidth: 140 },
      { key: 'routePath', label: '路由路径', minWidth: 160 },
      { key: 'componentName', label: '组件名', minWidth: 160 },
      { key: 'status', label: '状态', width: 100, align: 'center', type: 'status', tone: () => 'success' },
      { key: 'remark', label: '说明', minWidth: 220 }
    ]
  },
  '/system-logs': {
    title: '日志管理',
    subtitle: '汇总当前正式业务流中的留痕来源，核对哪些日志已落地、哪些仍待补齐。',
    tableTitle: '日志留痕清单',
    tableSubtitle: '业务留痕已经形成闭环，后续如需统一审计中心可再升级。',
    keywordPlaceholder: '请输入日志来源、状态、保留规则或说明',
    note: '当前业务日志主要沉淀在业务表与推送记录中，已经能支撑追踪；如果后续要做统一审计台账，再补独立日志中心即可。',
    emptyText: '暂无日志留痕清单',
    guide: '本页用于确认样品封签留痕、报告推送留痕、流程状态留痕等关键节点是否已经有正式数据承接。',
    constraint: '统一日志中心不是当前最紧急项，现阶段优先保证业务表留痕可追溯即可。',
    actions: [
      { label: '查看报告台账', path: '/report-ledger', type: 'primary', plain: false },
      { label: '查看设备维修', path: '/instrument-maintenance' }
    ],
    quickLinks: [
      { label: '报告台账', path: '/report-ledger', desc: '查看正式报告产物与推送状态' },
      { label: '设备维修', path: '/instrument-maintenance', desc: '查看维修记录与留痕说明' },
      { label: '移动工作台', path: '/mobile', desc: '查看移动闭环中的业务留痕节点' }
    ],
    columns: [
      { key: 'sourceName', label: '日志来源', minWidth: 180 },
      { key: 'eventCount', label: '事件数', width: 100, align: 'center' },
      { key: 'retentionRule', label: '保留规则', minWidth: 160 },
      { key: 'status', label: '状态', width: 100, align: 'center', type: 'status', tone: (row) => row.status === '待接入' ? 'warning' : 'success' },
      { key: 'remark', label: '说明', minWidth: 260 }
    ]
  },
  '/system-dicts': {
    title: '数据字典管理',
    subtitle: '集中展示当前系统已经正式落地的业务状态字典、周期字典与配置字典。',
    tableTitle: '字典清单',
    tableSubtitle: '当前枚举已支撑采样、检测、审核、报告、设备等主流程。',
    keywordPlaceholder: '请输入字典编码、字典名称、所属模块或字典项',
    note: '字典当前以前端枚举与后端常量协同承接；如后续需要在线维护，可再升级为正式字典表。',
    emptyText: '暂无数据字典清单',
    guide: '本页用于确认关键状态字典是否已经完整覆盖正式业务流程，避免前后端出现状态含义不一致。',
    constraint: '如果后续需要让业务人员自行配置字典值，必须补齐后端字典表和管理接口，不能只改前端枚举。',
    actions: [
      { label: '查看检测配置', path: '/detection-analysis', type: 'primary', plain: false },
      { label: '查看表单管理', path: '/system-forms' }
    ],
    quickLinks: [
      { label: '样品台账', path: '/sample-ledger', desc: '核对样品状态字典是否与业务展示一致' },
      { label: '检测台账', path: '/detection-ledger', desc: '核对检测状态字典是否与业务展示一致' },
      { label: '报告台账', path: '/report-ledger', desc: '核对报告状态字典是否与业务展示一致' }
    ],
    columns: [
      { key: 'dictCode', label: '字典编码', minWidth: 180 },
      { key: 'dictName', label: '字典名称', minWidth: 180 },
      { key: 'moduleName', label: '所属模块', minWidth: 130 },
      { key: 'itemCount', label: '字典项数', width: 110, align: 'center' },
      { key: 'sampleText', label: '示例字典项', minWidth: 220 },
      { key: 'status', label: '状态', width: 100, align: 'center', type: 'status', tone: () => 'success' }
    ]
  },
  '/system-forms': {
    title: '表单管理',
    subtitle: '汇总当前系统已经正式落地的页面表单、弹窗表单与移动端表单。',
    tableTitle: '表单清单',
    tableSubtitle: '用于跟踪哪些表单已经从演示升级为正式业务输入入口。',
    keywordPlaceholder: '请输入表单名称、所属模块、页面路径或说明',
    note: '采样、检测、审核、设备、维修、用户等关键表单已经转为正式业务承接；其余仍可按优先级继续补强。',
    emptyText: '暂无表单清单',
    guide: '本页帮助我们按表单粒度核查哪些页面只是能看，哪些页面已经真正能录入、编辑、流转。',
    constraint: '后续继续推进系统管理其余能力时，建议优先改有真实写入动作的表单页，收益最高。',
    actions: [
      { label: '查看用户管理', path: '/system-users', type: 'primary', plain: false },
      { label: '查看设备台账', path: '/instrument-ledger' }
    ],
    quickLinks: [
      { label: '用户管理', path: '/system-users', desc: '查看真实 CRUD 表单页效果' },
      { label: '设备维修', path: '/instrument-maintenance', desc: '查看维修记录表单与留痕' },
      { label: '移动工作台', path: '/mobile', desc: '查看移动端表单闭环效果' }
    ],
    columns: [
      { key: 'formName', label: '表单名称', minWidth: 180 },
      { key: 'moduleName', label: '所属模块', minWidth: 120 },
      { key: 'pagePath', label: '页面路径', minWidth: 150 },
      { key: 'fieldCount', label: '字段数', width: 100, align: 'center' },
      { key: 'formType', label: '表单类型', minWidth: 120 },
      { key: 'status', label: '状态', width: 100, align: 'center', type: 'status', tone: () => 'success' },
      { key: 'remark', label: '说明', minWidth: 240 }
    ]
  }
}

const currentScene = computed(() => sceneConfigMap[route.path] || sceneConfigMap['/system-users'])
const currentSceneRows = computed(() => sceneRowsMap.value[route.path] || [])

const visibleUserRows = computed(() => {
  if (activeStatKey.value === 'role') {
    return userRows.value.filter((item) => normalizeText(item.roleCode) !== '-')
  }
  return userRows.value
})

const visibleSceneRows = computed(() => {
  let rows = currentSceneRows.value
  const keyword = normalizeKeyword(sceneKeyword.value)
  if (keyword) {
    rows = rows.filter((item) => matchesKeyword(item, keyword))
  }

  switch (route.path) {
    case '/system-orgs':
      if (activeStatKey.value === 'connected') {
        return rows.filter((item) => item.status !== '待接入')
      }
      if (activeStatKey.value === 'pending') {
        return rows.filter((item) => item.status === '待接入')
      }
      return rows
    case '/system-roles':
      if (activeStatKey.value === 'assigned') {
        return rows.filter((item) => item.status === '已分配')
      }
      if (activeStatKey.value === 'unassigned') {
        return rows.filter((item) => item.status === '未分配')
      }
      return rows
    case '/system-menus':
      if (activeStatKey.value === 'group') {
        return rows.filter((item) => item.level === '二级目录')
      }
      if (activeStatKey.value === 'page') {
        return rows.filter((item) => item.level === '三级页面')
      }
      return rows
    case '/system-logs':
      if (activeStatKey.value === 'effective') {
        return rows.filter((item) => item.status === '已生效')
      }
      if (activeStatKey.value === 'pending') {
        return rows.filter((item) => item.status === '待接入')
      }
      return rows
    case '/system-dicts':
      if (activeStatKey.value === 'status') {
        return rows.filter((item) => String(item.dictCode).includes('status'))
      }
      if (activeStatKey.value === 'business') {
        return rows.filter((item) => item.moduleName !== '基础配置')
      }
      return rows
    case '/system-forms':
      if (activeStatKey.value === 'dialog') {
        return rows.filter((item) => String(item.formType).includes('弹窗'))
      }
      if (activeStatKey.value === 'mobile') {
        return rows.filter((item) => String(item.formType).includes('移动'))
      }
      return rows
    default:
      return rows
  }
})

const currentTags = computed(() => {
  if (isUserScene.value) {
    return [
      { label: '当前页', value: userRows.value.length, type: 'info' },
      { label: '分页总数', value: userTotal.value, type: 'success' },
      { label: '当前登录', value: normalizeText(currentLoginUser.realName || currentLoginUser.username), type: 'warning' }
    ]
  }

  const rows = currentSceneRows.value
  if (route.path === '/system-orgs') {
    return [
      { label: '组织域', value: rows.length, type: 'info' },
      { label: '已接入', value: rows.filter((item) => item.status !== '待接入').length, type: 'success' },
      { label: '待接入', value: rows.filter((item) => item.status === '待接入').length, type: 'warning' }
    ]
  }
  if (route.path === '/system-roles') {
    return [
      { label: '角色数', value: rows.length, type: 'info' },
      { label: '已分配', value: rows.filter((item) => item.status === '已分配').length, type: 'success' },
      { label: '未分配', value: rows.filter((item) => item.status === '未分配').length, type: 'warning' }
    ]
  }
  if (route.path === '/system-menus') {
    return [
      { label: '菜单节点', value: rows.length, type: 'info' },
      { label: '目录', value: rows.filter((item) => item.level === '二级目录').length, type: 'success' },
      { label: '页面', value: rows.filter((item) => item.level === '三级页面').length, type: 'warning' }
    ]
  }
  if (route.path === '/system-dicts') {
    return [
      { label: '字典数', value: rows.length, type: 'info' },
      { label: '状态类', value: rows.filter((item) => String(item.dictCode).includes('status')).length, type: 'success' },
      { label: '业务类', value: rows.filter((item) => item.moduleName !== '基础配置').length, type: 'warning' }
    ]
  }
  return [
    { label: '当前项', value: rows.length, type: 'info' },
    { label: '已生效', value: rows.filter((item) => String(item.status || '').includes('已')).length, type: 'success' },
    { label: '待接入', value: rows.filter((item) => String(item.status || '').includes('待')).length, type: 'warning' }
  ]
})

const currentStats = computed(() => {
  if (isUserScene.value) {
    return [
      { key: 'all', label: '全部用户', value: userTotal.value, desc: '当前用户分页总记录数' },
      { key: 'enabled', label: '启用用户', value: userRows.value.filter((item) => Number(item.status) === 1).length, desc: '当前页启用状态账号数' },
      { key: 'disabled', label: '停用用户', value: userRows.value.filter((item) => Number(item.status) === 0).length, desc: '当前页停用状态账号数' },
      { key: 'role', label: '已配角色', value: userRows.value.filter((item) => normalizeText(item.roleCode) !== '-').length, desc: '当前页已填写角色编码的账号数' }
    ]
  }

  const rows = currentSceneRows.value
  switch (route.path) {
    case '/system-orgs':
      return [
        { key: 'all', label: '全部组织域', value: rows.length, desc: '当前组织承接方案总项数' },
        { key: 'connected', label: '已接入', value: rows.filter((item) => item.status !== '待接入').length, desc: '已承接真实业务的组织域' },
        { key: 'pending', label: '待接入', value: rows.filter((item) => item.status === '待接入').length, desc: '后续可升级的正式组织域' }
      ]
    case '/system-roles':
      return [
        { key: 'all', label: '全部角色', value: rows.length, desc: '当前角色编码总数' },
        { key: 'assigned', label: '已分配', value: rows.filter((item) => item.status === '已分配').length, desc: '已有真实账号分配的角色' },
        { key: 'unassigned', label: '未分配', value: rows.filter((item) => item.status === '未分配').length, desc: '仍待后续账号启用的角色' }
      ]
    case '/system-menus':
      return [
        { key: 'all', label: '全部节点', value: rows.length, desc: '当前正式菜单结构总节点数' },
        { key: 'group', label: '目录节点', value: rows.filter((item) => item.level === '二级目录').length, desc: '左侧菜单目录节点' },
        { key: 'page', label: '页面节点', value: rows.filter((item) => item.level === '三级页面').length, desc: '落到业务页面的菜单节点' }
      ]
    case '/system-logs':
      return [
        { key: 'all', label: '全部来源', value: rows.length, desc: '当前已梳理的留痕来源数' },
        { key: 'effective', label: '已生效', value: rows.filter((item) => item.status === '已生效').length, desc: '当前已可追溯的留痕来源' },
        { key: 'pending', label: '待接入', value: rows.filter((item) => item.status === '待接入').length, desc: '后续可补统一审计的来源' }
      ]
    case '/system-dicts':
      return [
        { key: 'all', label: '全部字典', value: rows.length, desc: '当前在用字典总数' },
        { key: 'status', label: '状态字典', value: rows.filter((item) => String(item.dictCode).includes('status')).length, desc: '覆盖业务状态流转的字典' },
        { key: 'business', label: '业务字典', value: rows.filter((item) => item.moduleName !== '基础配置').length, desc: '直接服务业务流程的字典' }
      ]
    case '/system-forms':
      return [
        { key: 'all', label: '全部表单', value: rows.length, desc: '当前已梳理表单总数' },
        { key: 'dialog', label: '弹窗表单', value: rows.filter((item) => String(item.formType).includes('弹窗')).length, desc: '桌面端弹窗编辑表单数' },
        { key: 'mobile', label: '移动表单', value: rows.filter((item) => String(item.formType).includes('移动')).length, desc: '移动端闭环相关表单数' }
      ]
    default:
      return [{ key: 'all', label: '全部', value: rows.length, desc: '当前页面数据总数' }]
  }
})

watch(
  () => route.path,
  async () => {
    activeStatKey.value = 'all'
    sceneKeyword.value = ''
    resetUserQueryState()
    await loadUserSummary()
    await loadUsers()
  }
)

onMounted(async () => {
  await loadUserSummary()
  await loadUsers()
})

function createDefaultUserForm() {
  return {
    id: '',
    username: '',
    password: '',
    realName: '',
    roleCode: '',
    phone: '',
    status: 1
  }
}

function buildRoleRow(roleCode, roleName, scope, remark) {
  const userCount = countUsersByRole(roleCode)
  return {
    id: roleCode,
    roleCode,
    roleName,
    userCount,
    scope,
    status: userCount > 0 ? '已分配' : '未分配',
    remark
  }
}

function normalizeText(value) {
  const text = String(value ?? '').trim()
  return text || '-'
}

function normalizeKeyword(value) {
  return String(value ?? '').trim().toLowerCase()
}

function matchesKeyword(row, keyword) {
  return Object.values(row || {}).some((value) => String(value ?? '').toLowerCase().includes(keyword))
}

function countUsersByRole(roleCode) {
  return userSummaryRows.value.filter((item) => item.roleCode === roleCode).length
}

function getUserStatusLabel(status) {
  return Number(status) === 1 ? '启用' : '停用'
}

function getUserStatusClass(status) {
  return Number(status) === 1 ? 'success' : 'warning'
}

function resolveSceneValue(column, row) {
  if (typeof column.formatter === 'function') {
    return column.formatter(row)
  }
  return normalizeText(row[column.key])
}

function resolveSceneTone(column, row) {
  if (typeof column.tone === 'function') {
    return column.tone(row)
  }
  return column.tone || 'info'
}

function resetUserQueryState() {
  userQuery.pageNum = 1
  userQuery.pageSize = DEFAULT_PAGE_SIZE
  userQuery.keyword = ''
  userQuery.status = ''
}

function handleStatClick(key) {
  activeStatKey.value = key
  if (!isUserScene.value) {
    return
  }

  if (key === 'enabled') {
    userQuery.status = 1
    userQuery.pageNum = 1
    loadUsers()
    return
  }
  if (key === 'disabled') {
    userQuery.status = 0
    userQuery.pageNum = 1
    loadUsers()
    return
  }
  if (key === 'all' || key === 'role') {
    userQuery.status = ''
    userQuery.pageNum = 1
    loadUsers()
  }
}

function goRoute(path) {
  if (!path) {
    return
  }
  router.push(path)
}

async function loadUsers() {
  if (!isUserScene.value) {
    userRows.value = []
    userTotal.value = 0
    return
  }

  loading.value = true
  try {
    const result = await fetchSystemUsersApi({
      pageNum: userQuery.pageNum,
      pageSize: userQuery.pageSize,
      keyword: String(userQuery.keyword || '').trim() || undefined,
      status: userQuery.status === '' ? undefined : userQuery.status
    })
    userRows.value = Array.isArray(result.records) ? result.records : []
    userTotal.value = Number(result.total || 0)
  } finally {
    loading.value = false
  }
}

async function loadUserSummary() {
  try {
    const result = await fetchSystemUsersApi({
      pageNum: 1,
      pageSize: 200
    })
    userSummaryRows.value = Array.isArray(result.records) ? result.records : []
    userSummaryTotal.value = Number(result.total || 0)
  } catch (error) {
    userSummaryRows.value = []
    userSummaryTotal.value = 0
  }
}

function handleUserSearch() {
  userQuery.pageNum = 1
  loadUsers()
}

function resetUserQuery() {
  activeStatKey.value = 'all'
  resetUserQueryState()
  loadUsers()
}

function openUserDialog(row) {
  resetUserForm()
  if (row) {
    userForm.id = row.id
    userForm.username = row.username || ''
    userForm.realName = row.realName || ''
    userForm.roleCode = row.roleCode || ''
    userForm.phone = row.phone || ''
    userForm.status = Number(row.status) === 0 ? 0 : 1
  }
  dialogVisible.value = true
}

function resetUserForm() {
  Object.assign(userForm, createDefaultUserForm())
  formRef.value?.clearValidate?.()
}

async function submitUserForm() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      username: String(userForm.username || '').trim(),
      password: String(userForm.password || '').trim(),
      realName: String(userForm.realName || '').trim(),
      roleCode: String(userForm.roleCode || '').trim(),
      phone: String(userForm.phone || '').trim(),
      status: Number(userForm.status) === 0 ? 0 : 1
    }

    if (userForm.id) {
      await updateSystemUserApi(userForm.id, payload)
      ElMessage.success('用户更新成功')
    } else {
      await createSystemUserApi(payload)
      ElMessage.success('用户新增成功')
    }

    dialogVisible.value = false
    await loadUsers()
    await loadUserSummary()
  } finally {
    saving.value = false
  }
}

async function removeUser(row) {
  await ElMessageBox.confirm(`确定删除用户“${row.realName || row.username}”吗？`, '删除确认', {
    type: 'warning'
  })
  await deleteSystemUserApi(row.id)
  ElMessage.success('用户删除成功')
  if (userRows.value.length === 1 && userQuery.pageNum > 1) {
    userQuery.pageNum -= 1
  }
  await loadUsers()
  await loadUserSummary()
}

function reloadData() {
  if (isUserScene.value) {
    loadUsers()
    loadUserSummary()
    return
  }
  activeStatKey.value = 'all'
}
</script>

<style scoped>
.page-hero,
.scene-grid {
  background:
    radial-gradient(circle at top right, rgba(41, 129, 255, 0.16), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(245, 249, 255, 0.98));
}

.page-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.metric-card--action {
  width: 100%;
  border: none;
  cursor: pointer;
  text-align: left;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.metric-card--action:hover,
.metric-card--action:focus-visible,
.metric-card--action.is-active {
  transform: translateY(-2px);
  box-shadow: 0 18px 32px rgba(23, 67, 122, 0.12);
}

.metric-card p {
  margin: 8px 0 0;
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.7;
}

.scene-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.scene-copy p {
  margin: 0;
  color: var(--text-sub);
  line-height: 1.9;
}

.scene-copy p + p {
  margin-top: 10px;
}

.quick-links {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.quick-link {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px 18px;
  border: 1px solid var(--line-soft);
  border-radius: 14px;
  background: linear-gradient(180deg, #ffffff, #f7fbff);
  cursor: pointer;
  text-align: left;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.quick-link:hover {
  transform: translateY(-2px);
  border-color: rgba(38, 126, 255, 0.26);
  box-shadow: 0 16px 28px rgba(17, 60, 112, 0.1);
}

.quick-link strong {
  color: var(--text-main);
  font-size: 15px;
}

.quick-link span {
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.7;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 18px;
}

.system-page :deep(.el-form-item) {
  margin-bottom: 18px;
}

.system-page :deep(.el-radio-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.system-page :deep(.el-select),
.system-page :deep(.el-input),
.system-page :deep(.el-input-number) {
  width: 100%;
}

.system-page :deep(.el-dialog__body) {
  padding-top: 12px;
}

@media (max-width: 1200px) {
  .quick-links {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .page-hero,
  .scene-grid {
    grid-template-columns: 1fr;
  }

  .page-hero {
    flex-direction: column;
  }

  .hero-tags {
    justify-content: flex-start;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
