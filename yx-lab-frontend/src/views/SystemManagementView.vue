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
                  placeholder="请输入用户名、姓名、机构、手机号或角色编码"
                  @keyup.enter="handleUserSearch"
                />
              </label>
              <label class="toolbar-field">
                <span>状态</span>
                <el-select v-model="userQuery.status" clearable placeholder="请选择状态">
                  <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
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
            用户管理已接入真实接口，新增和编辑时必须绑定有效机构与启用角色，且不允许删除当前登录用户。
          </div>
        </div>

        <div class="table-card">
          <el-table class="list-table" :data="visibleUserRows" stripe max-height="460" empty-text="暂无用户数据">
            <el-table-column prop="username" label="用户名" min-width="140" />
            <el-table-column prop="realName" label="姓名" min-width="120" />
            <el-table-column prop="orgName" label="所属机构" min-width="160">
              <template #default="{ row }">{{ row.orgName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="roleCode" label="角色编码" min-width="140" />
            <el-table-column prop="phone" label="手机号" min-width="140">
              <template #default="{ row }">{{ row.phone || '-' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="110" header-cell-class-name="cell-center" class-name="cell-center">
              <template #default="{ row }">
                <span :class="['status-chip', getStatusClassByValue(row.status)]">{{ getStatusLabelByValue(row.status) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="createdName" label="创建人" min-width="120">
              <template #default="{ row }">{{ row.createdName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="createdTime" label="创建时间" min-width="170">
              <template #default="{ row }">{{ row.createdTime || '-' }}</template>
            </el-table-column>
            <el-table-column prop="updatedTime" label="更新时间" min-width="170">
              <template #default="{ row }">{{ row.updatedTime || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right" class-name="cell-center">
              <template #default="{ row }">
                <el-button link type="primary" @click="openUserDialog(row)">编辑</el-button>
                <el-button link type="danger" :disabled="String(row.id) === String(currentLoginUser.userId || '')" @click="removeUser(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <TablePagination v-model:current-page="userQuery.pageNum" v-model:page-size="userQuery.pageSize" :total="userTotal" @change="loadUsers" />
        </div>
      </template>

      <template v-else-if="isOrgScene">
        <div class="toolbar-panel">
          <div class="toolbar-row">
            <div class="toolbar-fields">
              <label class="toolbar-field toolbar-field--medium">
                <span>关键字</span>
                <el-input
                  v-model="orgQuery.keyword"
                  clearable
                  placeholder="请输入机构编码、机构名称、上级机构、机构类型或备注"
                  @keyup.enter="handleOrgSearch"
                />
              </label>
              <label class="toolbar-field">
                <span>状态</span>
                <el-select v-model="orgQuery.status" clearable placeholder="请选择状态">
                  <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </label>
            </div>

            <div class="toolbar-actions">
              <el-button type="primary" @click="handleOrgSearch">查询</el-button>
              <el-button @click="resetOrgQuery">重置</el-button>
              <el-button @click="reloadData">刷新</el-button>
              <el-button type="primary" plain @click="openOrgDialog()">新增机构</el-button>
            </div>
          </div>
          <div class="panel-note">机构管理已升级为正式台账。删除机构时会校验下级机构和已绑定用户，用户保存时也会校验机构是否存在且启用。</div>
        </div>

        <div class="table-card">
          <el-table class="list-table" :data="visibleOrgRows" stripe max-height="460" empty-text="暂无机构数据">
            <el-table-column prop="orgCode" label="机构编码" min-width="140" />
            <el-table-column prop="orgName" label="机构名称" min-width="150" />
            <el-table-column prop="parentName" label="上级机构" min-width="150"><template #default="{ row }">{{ row.parentName || '-' }}</template></el-table-column>
            <el-table-column prop="orgType" label="机构类型" min-width="140"><template #default="{ row }">{{ row.orgType || '-' }}</template></el-table-column>
            <el-table-column prop="memberCount" label="成员数" width="110" class-name="cell-center" header-cell-class-name="cell-center" />
            <el-table-column label="状态" width="110" header-cell-class-name="cell-center" class-name="cell-center"><template #default="{ row }"><span :class="['status-chip', getStatusClassByValue(row.status)]">{{ getStatusLabelByValue(row.status) }}</span></template></el-table-column>
            <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip><template #default="{ row }">{{ row.remark || '-' }}</template></el-table-column>
            <el-table-column prop="updatedTime" label="更新时间" min-width="170"><template #default="{ row }">{{ row.updatedTime || '-' }}</template></el-table-column>
            <el-table-column label="操作" width="170" fixed="right" class-name="cell-center"><template #default="{ row }"><el-button link type="primary" @click="openOrgDialog(row)">编辑</el-button><el-button link type="danger" @click="removeOrg(row)">删除</el-button></template></el-table-column>
          </el-table>
          <TablePagination v-model:current-page="orgQuery.pageNum" v-model:page-size="orgQuery.pageSize" :total="orgTotal" @change="loadOrgs" />
        </div>
      </template>

      <template v-else-if="isDictScene">
        <div class="toolbar-panel">
          <div class="toolbar-row">
            <div class="toolbar-fields">
              <label class="toolbar-field toolbar-field--medium">
                <span>关键字</span>
                <el-input
                  v-model="dictQuery.keyword"
                  clearable
                  placeholder="请输入字典编码、字典名称、所属模块、字典项或备注"
                  @keyup.enter="handleDictSearch"
                />
              </label>
              <label class="toolbar-field">
                <span>状态</span>
                <el-select v-model="dictQuery.status" clearable placeholder="请选择状态">
                  <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </label>
            </div>
            <div class="toolbar-actions">
              <el-button type="primary" @click="handleDictSearch">查询</el-button>
              <el-button @click="resetDictQuery">重置</el-button>
              <el-button @click="reloadData">刷新</el-button>
              <el-button type="primary" plain @click="openDictDialog()">新增字典</el-button>
            </div>
          </div>
          <div class="panel-note">数据字典管理已接入真实接口，支持正式维护字典编码、所属模块、字典项文本与启停状态，可作为统一状态口径基础台账。</div>
        </div>

        <div class="table-card">
          <el-table class="list-table" :data="visibleDictRows" stripe max-height="460" empty-text="暂无数据字典">
            <el-table-column prop="dictCode" label="字典编码" min-width="160" />
            <el-table-column prop="dictName" label="字典名称" min-width="160" />
            <el-table-column prop="moduleName" label="所属模块" min-width="140" />
            <el-table-column prop="itemCount" label="字典项数" width="100" class-name="cell-center" header-cell-class-name="cell-center" />
            <el-table-column prop="itemText" label="字典项内容" min-width="240" show-overflow-tooltip><template #default="{ row }">{{ formatDictPreview(row.itemText) }}</template></el-table-column>
            <el-table-column label="状态" width="110" header-cell-class-name="cell-center" class-name="cell-center"><template #default="{ row }"><span :class="['status-chip', getStatusClassByValue(row.status)]">{{ getStatusLabelByValue(row.status) }}</span></template></el-table-column>
            <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip><template #default="{ row }">{{ row.remark || '-' }}</template></el-table-column>
            <el-table-column prop="updatedTime" label="更新时间" min-width="170"><template #default="{ row }">{{ row.updatedTime || '-' }}</template></el-table-column>
            <el-table-column label="操作" width="170" fixed="right" class-name="cell-center"><template #default="{ row }"><el-button link type="primary" @click="openDictDialog(row)">编辑</el-button><el-button link type="danger" @click="removeDict(row)">删除</el-button></template></el-table-column>
          </el-table>
          <TablePagination v-model:current-page="dictQuery.pageNum" v-model:page-size="dictQuery.pageSize" :total="dictTotal" @change="loadDicts" />
        </div>
      </template>

      <template v-else-if="isRoleScene">
        <div class="toolbar-panel">
          <div class="toolbar-row">
            <div class="toolbar-fields">
              <label class="toolbar-field toolbar-field--medium">
                <span>关键字</span>
                <el-input
                  v-model="roleQuery.keyword"
                  clearable
                  placeholder="请输入角色编码、角色名称、适用范围或备注"
                  @keyup.enter="handleRoleSearch"
                />
              </label>
              <label class="toolbar-field">
                <span>状态</span>
                <el-select v-model="roleQuery.status" clearable placeholder="请选择状态">
                  <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </label>
            </div>
            <div class="toolbar-actions">
              <el-button type="primary" @click="handleRoleSearch">查询</el-button>
              <el-button @click="resetRoleQuery">重置</el-button>
              <el-button @click="reloadData">刷新</el-button>
              <el-button type="primary" plain @click="openRoleDialog()">新增角色</el-button>
            </div>
          </div>
          <div class="panel-note">角色管理已接入真实接口，支持角色编码唯一、角色名称唯一，以及“角色被用户引用时不可删除”的业务约束。</div>
        </div>

        <div class="table-card">
          <el-table class="list-table" :data="visibleRoleRows" stripe max-height="460" empty-text="暂无角色数据">
            <el-table-column prop="roleCode" label="角色编码" min-width="140" />
            <el-table-column prop="roleName" label="角色名称" min-width="140" />
            <el-table-column prop="roleScope" label="适用范围" min-width="150"><template #default="{ row }">{{ row.roleScope || '-' }}</template></el-table-column>
            <el-table-column prop="userCount" label="关联用户数" width="120" class-name="cell-center" header-cell-class-name="cell-center" />
            <el-table-column label="状态" width="110" header-cell-class-name="cell-center" class-name="cell-center"><template #default="{ row }"><span :class="['status-chip', getStatusClassByValue(row.status)]">{{ getStatusLabelByValue(row.status) }}</span></template></el-table-column>
            <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip><template #default="{ row }">{{ row.remark || '-' }}</template></el-table-column>
            <el-table-column prop="updatedTime" label="更新时间" min-width="170"><template #default="{ row }">{{ row.updatedTime || '-' }}</template></el-table-column>
            <el-table-column label="操作" width="170" fixed="right" class-name="cell-center"><template #default="{ row }"><el-button link type="primary" @click="openRoleDialog(row)">编辑</el-button><el-button link type="danger" @click="removeRole(row)">删除</el-button></template></el-table-column>
          </el-table>
          <TablePagination v-model:current-page="roleQuery.pageNum" v-model:page-size="roleQuery.pageSize" :total="roleTotal" @change="loadRoles" />
        </div>
      </template>

      <template v-else-if="isLogScene">
        <div class="toolbar-panel">
          <div class="toolbar-row">
            <div class="toolbar-fields">
              <label class="toolbar-field toolbar-field--medium">
                <span>关键字</span>
                <el-input v-model="logQuery.keyword" clearable placeholder="请输入业务编号、操作人、日志内容或状态" @keyup.enter="handleLogSearch" />
              </label>
              <label class="toolbar-field">
                <span>日志来源</span>
                <el-select v-model="logQuery.sourceType" clearable placeholder="请选择日志来源" @change="handleLogSourceChange">
                  <el-option v-for="item in logSourceOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </label>
            </div>
            <div class="toolbar-actions">
              <el-button type="primary" @click="handleLogSearch">查询</el-button>
              <el-button @click="resetLogQuery">重置</el-button>
              <el-button @click="reloadData">刷新</el-button>
            </div>
          </div>
          <div class="panel-note">日志管理已切换为真实业务留痕查询，统一汇总登录认证、样品留痕、检测记录、审核记录与报告推送日志。</div>
        </div>

        <div class="table-card">
          <el-table class="list-table" :data="logPageRows" stripe max-height="460" empty-text="暂无系统日志">
            <el-table-column prop="occurTime" label="发生时间" min-width="170"><template #default="{ row }">{{ row.occurTime || '-' }}</template></el-table-column>
            <el-table-column prop="sourceName" label="日志来源" min-width="120" />
            <el-table-column prop="businessNo" label="业务编号" min-width="170" show-overflow-tooltip><template #default="{ row }">{{ row.businessNo || '-' }}</template></el-table-column>
            <el-table-column prop="operatorName" label="操作人" min-width="120"><template #default="{ row }">{{ row.operatorName || '-' }}</template></el-table-column>
            <el-table-column label="状态" width="120" header-cell-class-name="cell-center" class-name="cell-center"><template #default="{ row }"><span :class="['status-chip', getLogStatusClass(row.status)]">{{ row.status || '-' }}</span></template></el-table-column>
            <el-table-column prop="content" label="日志内容" min-width="320" show-overflow-tooltip><template #default="{ row }">{{ row.content || '-' }}</template></el-table-column>
          </el-table>
          <TablePagination v-model:current-page="logQuery.pageNum" v-model:page-size="logQuery.pageSize" :total="logPageTotal" @change="loadLogs" />
        </div>
      </template>

      <template v-else>
        <div class="toolbar-panel">
          <div class="toolbar-row">
            <div class="toolbar-fields">
              <label class="toolbar-field toolbar-field--medium">
                <span>关键字</span>
                <el-input v-model="sceneKeyword" clearable :placeholder="currentScene.keywordPlaceholder" />
              </label>
            </div>
            <div class="toolbar-actions">
              <el-button type="primary" @click="reloadData">刷新</el-button>
              <el-button v-for="item in currentScene.actions" :key="item.label" :type="item.type || 'default'" :plain="item.plain !== false" @click="goRoute(item.path)">
                {{ item.label }}
              </el-button>
            </div>
          </div>
          <div class="panel-note">{{ currentScene.note }}</div>
        </div>

        <div class="table-card">
          <el-table class="list-table" :data="visibleSceneRows" stripe max-height="460" :empty-text="currentScene.emptyText">
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
                <span v-if="column.type === 'status'" :class="['status-chip', resolveSceneTone(column, row)]">{{ resolveSceneValue(column, row) }}</span>
                <span v-else>{{ resolveSceneValue(column, row) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>
    </section>

    <section class="scene-grid">
      <div class="glass-panel section-block">
        <div class="section-head"><h3 class="section-title">页面说明</h3></div>
        <div class="scene-copy">
          <p>{{ currentScene.guide }}</p>
          <p>{{ currentScene.constraint }}</p>
        </div>
      </div>

      <div class="glass-panel section-block">
        <div class="section-head"><h3 class="section-title">关联入口</h3></div>
        <div class="quick-links">
          <button v-for="item in currentScene.quickLinks" :key="item.path" type="button" class="quick-link" @click="goRoute(item.path)">
            <strong>{{ item.label }}</strong>
            <span>{{ item.desc }}</span>
          </button>
        </div>
      </div>
    </section>

    <el-dialog v-model="dictDialogVisible" :title="dictForm.id ? '编辑字典' : '新增字典'" width="760px" destroy-on-close @closed="resetDictForm">
      <el-form ref="dictFormRef" :model="dictForm" :rules="dictRules" label-width="100px">
        <div class="form-grid">
          <el-form-item label="字典编码" prop="dictCode"><el-input v-model="dictForm.dictCode" placeholder="请输入字典编码" /></el-form-item>
          <el-form-item label="字典名称" prop="dictName"><el-input v-model="dictForm.dictName" placeholder="请输入字典名称" /></el-form-item>
          <el-form-item label="所属模块" prop="moduleName"><el-input v-model="dictForm.moduleName" placeholder="请输入所属模块" /></el-form-item>
          <el-form-item label="状态" prop="status"><el-radio-group v-model="dictForm.status"><el-radio-button :label="1">启用</el-radio-button><el-radio-button :label="0">停用</el-radio-button></el-radio-group></el-form-item>
          <el-form-item class="form-span-2" label="字典项" prop="itemText"><el-input v-model="dictForm.itemText" type="textarea" :rows="6" placeholder="请按每行一个字典项填写，例如：&#10;待执行&#10;执行中&#10;已完成" /></el-form-item>
          <el-form-item class="form-span-2" label="备注" prop="remark"><el-input v-model="dictForm.remark" type="textarea" :rows="3" placeholder="请输入字典用途说明或维护备注" /></el-form-item>
        </div>
      </el-form>
      <template #footer><el-button @click="dictDialogVisible = false">取消</el-button><el-button type="primary" :loading="savingDict" @click="submitDictForm">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="userDialogVisible" :title="userForm.id ? '编辑用户' : '新增用户'" width="760px" destroy-on-close @closed="resetUserForm">
      <el-form ref="userFormRef" :model="userForm" :rules="userRules" label-width="100px">
        <div class="form-grid">
          <el-form-item label="用户名" prop="username"><el-input v-model="userForm.username" placeholder="请输入用户名" /></el-form-item>
          <el-form-item label="姓名" prop="realName"><el-input v-model="userForm.realName" placeholder="请输入姓名" /></el-form-item>
          <el-form-item label="所属机构" prop="orgId"><el-select v-model="userForm.orgId" filterable placeholder="请选择所属机构" style="width: 100%"><el-option v-for="item in orgOptionsForUser" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
          <el-form-item label="角色编码" prop="roleCode"><el-select v-model="userForm.roleCode" filterable placeholder="请选择角色编码" style="width: 100%"><el-option v-for="item in roleOptionsForUser" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
          <el-form-item label="手机号" prop="phone"><el-input v-model="userForm.phone" placeholder="请输入手机号" /></el-form-item>
          <el-form-item label="状态" prop="status"><el-radio-group v-model="userForm.status"><el-radio-button :label="1">启用</el-radio-button><el-radio-button :label="0">停用</el-radio-button></el-radio-group></el-form-item>
          <el-form-item label="登录密码" prop="password"><el-input v-model="userForm.password" type="password" show-password :placeholder="userForm.id ? '留空表示不修改密码' : '请输入登录密码'" /></el-form-item>
        </div>
      </el-form>
      <template #footer><el-button @click="userDialogVisible = false">取消</el-button><el-button type="primary" :loading="savingUser" @click="submitUserForm">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="orgDialogVisible" :title="orgForm.id ? '编辑机构' : '新增机构'" width="760px" destroy-on-close @closed="resetOrgForm">
      <el-form ref="orgFormRef" :model="orgForm" :rules="orgRules" label-width="100px">
        <div class="form-grid">
          <el-form-item label="机构编码" prop="orgCode"><el-input v-model="orgForm.orgCode" placeholder="请输入机构编码" /></el-form-item>
          <el-form-item label="机构名称" prop="orgName"><el-input v-model="orgForm.orgName" placeholder="请输入机构名称" /></el-form-item>
          <el-form-item label="上级机构" prop="parentId"><el-select v-model="orgForm.parentId" clearable filterable placeholder="请选择上级机构" style="width: 100%"><el-option v-for="item in parentOrgOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
          <el-form-item label="机构类型" prop="orgType"><el-input v-model="orgForm.orgType" placeholder="请输入机构类型" /></el-form-item>
          <el-form-item label="状态" prop="status"><el-radio-group v-model="orgForm.status"><el-radio-button :label="1">启用</el-radio-button><el-radio-button :label="0">停用</el-radio-button></el-radio-group></el-form-item>
          <el-form-item class="form-span-2" label="备注" prop="remark"><el-input v-model="orgForm.remark" type="textarea" :rows="3" placeholder="请输入机构说明或使用备注" /></el-form-item>
        </div>
      </el-form>
      <template #footer><el-button @click="orgDialogVisible = false">取消</el-button><el-button type="primary" :loading="savingOrg" @click="submitOrgForm">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="roleDialogVisible" :title="roleForm.id ? '编辑角色' : '新增角色'" width="760px" destroy-on-close @closed="resetRoleForm">
      <el-form ref="roleFormRef" :model="roleForm" :rules="roleRules" label-width="100px">
        <div class="form-grid">
          <el-form-item label="角色编码" prop="roleCode"><el-input v-model="roleForm.roleCode" placeholder="请输入角色编码" /></el-form-item>
          <el-form-item label="角色名称" prop="roleName"><el-input v-model="roleForm.roleName" placeholder="请输入角色名称" /></el-form-item>
          <el-form-item label="适用范围" prop="roleScope"><el-input v-model="roleForm.roleScope" placeholder="请输入适用范围" /></el-form-item>
          <el-form-item label="状态" prop="status"><el-radio-group v-model="roleForm.status"><el-radio-button :label="1">启用</el-radio-button><el-radio-button :label="0">停用</el-radio-button></el-radio-group></el-form-item>
          <el-form-item class="form-span-2" label="备注" prop="remark"><el-input v-model="roleForm.remark" type="textarea" :rows="3" placeholder="请输入角色说明、职责边界或使用备注" /></el-form-item>
        </div>
      </el-form>
      <template #footer><el-button @click="roleDialogVisible = false">取消</el-button><el-button type="primary" :loading="savingRole" @click="submitRoleForm">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import TablePagination from '../components/common/TablePagination.vue'
import {
  createSystemDictApi,
  createSystemOrgApi,
  createSystemRoleApi,
  createSystemUserApi,
  deleteSystemDictApi,
  deleteSystemOrgApi,
  deleteSystemRoleApi,
  deleteSystemUserApi,
  fetchSystemDictsApi,
  fetchSystemLogsApi,
  fetchSystemOrgOptionsApi,
  fetchSystemOrgsApi,
  fetchSystemRoleOptionsApi,
  fetchSystemRolesApi,
  fetchSystemUsersApi,
  updateSystemDictApi,
  updateSystemOrgApi,
  updateSystemRoleApi,
  updateSystemUserApi
} from '../api/lab'
import { getUser } from '../utils/auth'
import { labMenuGroups } from '../router/menuConfig'
import { DEFAULT_PAGE_SIZE } from '../utils/labEnums'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const savingDict = ref(false)
const savingOrg = ref(false)
const savingUser = ref(false)
const savingRole = ref(false)
const activeStatKey = ref('all')
const sceneKeyword = ref('')
const currentLoginUser = reactive(getUser() || {})

const dictDialogVisible = ref(false)
const orgDialogVisible = ref(false)
const userDialogVisible = ref(false)
const roleDialogVisible = ref(false)
const dictFormRef = ref()
const orgFormRef = ref()
const userFormRef = ref()
const roleFormRef = ref()

const isDictScene = computed(() => route.path === '/system-dicts')
const isOrgScene = computed(() => route.path === '/system-orgs')
const isUserScene = computed(() => route.path === '/system-users')
const isRoleScene = computed(() => route.path === '/system-roles')
const isLogScene = computed(() => route.path === '/system-logs')

const statusOptions = [
  { label: '鍚敤', value: 1 },
  { label: '鍋滅敤', value: 0 }
]

const dictQuery = reactive({
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE,
  keyword: '',
  status: ''
})

const orgQuery = reactive({
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE,
  keyword: '',
  status: ''
})

const userQuery = reactive({
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE,
  keyword: '',
  status: ''
})

const roleQuery = reactive({
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE,
  keyword: '',
  status: ''
})

const logQuery = reactive({
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE,
  keyword: '',
  sourceType: ''
})

const dictRows = ref([])
const dictTotal = ref(0)
const orgRows = ref([])
const orgTotal = ref(0)
const userRows = ref([])
const userTotal = ref(0)
const roleRows = ref([])
const roleTotal = ref(0)
const logPageRows = ref([])
const logPageTotal = ref(0)
const userSummaryRows = ref([])
const userSummaryTotal = ref(0)
const orgOptions = ref([])
const roleOptions = ref([])
const logSummary = reactive({
  totalCount: 0,
  loginCount: 0,
  processCount: 0,
  pushCount: 0
})

const logSourceOptions = [
  { label: '登录认证', value: 'LOGIN' },
  { label: '流程日志', value: 'PROCESS' },
  { label: '样品留痕', value: 'SAMPLE' },
  { label: '检测记录', value: 'DETECTION' },
  { label: '审核记录', value: 'REVIEW' },
  { label: '报告推送', value: 'PUSH' }
]

const dictForm = reactive(createDefaultDictForm())
const orgForm = reactive(createDefaultOrgForm())
const userForm = reactive(createDefaultUserForm())
const roleForm = reactive(createDefaultRoleForm())

const dictRules = {
  dictCode: [{ required: true, message: '请输入字典编码', trigger: 'blur' }],
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  moduleName: [{ required: true, message: '请输入所属模块', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const orgRules = {
  orgCode: [{ required: true, message: '请输入机构编码', trigger: 'blur' }],
  orgName: [{ required: true, message: '请输入机构名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const userRules = {
  username: [{ required: true, message: '璇疯緭鍏ョ敤鎴峰悕', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  orgId: [{ required: true, message: '请选择所属机构', trigger: 'change' }],
  roleCode: [{ required: true, message: '璇烽€夋嫨瑙掕壊缂栫爜', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  password: [{
    validator: (_, value, callback) => {
      if (!userForm.id && !String(value || '').trim()) {
        callback(new Error('鏂板鐢ㄦ埛鏃惰杈撳叆鐧诲綍瀵嗙爜'))
        return
      }
      callback()
    },
    trigger: 'blur'
  }]
}

const roleRules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const orgOptionsForUser = computed(() => {
  const options = [...orgOptions.value]
  const currentValue = Number(userForm.orgId || 0)
  if (currentValue && !options.some((item) => Number(item.value) === currentValue)) {
    options.unshift({
      label: `当前机构 ${currentValue}`,
      value: currentValue
    })
  }
  return options
})

const parentOrgOptions = computed(() => {
  const currentId = Number(orgForm.id || 0)
  return orgOptions.value.filter((item) => Number(item.value) !== currentId)
})

const roleOptionsForUser = computed(() => {
  const options = [...roleOptions.value]
  const currentValue = String(userForm.roleCode || '').trim()
  if (currentValue && !options.some((item) => item.value === currentValue)) {
    options.unshift({
      label: `${currentValue}（当前值）`,
      value: currentValue
    })
  }
  return options
})

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

const formRows = [
  {
    id: 'user-form',
    formName: '用户管理表单',
    moduleName: '系统管理',
    pagePath: '/system-users',
    fieldCount: 7,
    formType: '弹窗表单',
    status: '已配置',
    remark: '支持机构、角色、状态和密码维护'
  },
  {
    id: 'org-form',
    formName: '机构管理表单',
    moduleName: '系统管理',
    pagePath: '/system-orgs',
    fieldCount: 6,
    formType: '弹窗表单',
    status: '已配置',
    remark: '支持机构树基础维护与状态控制'
  },
  {
    id: 'dict-form',
    formName: '数据字典表单',
    moduleName: '系统管理',
    pagePath: '/system-dicts',
    fieldCount: 6,
    formType: '弹窗表单',
    status: '已配置',
    remark: '支持字典编码、模块、字典项文本和状态维护'
  },
  {
    id: 'role-form',
    formName: '角色管理表单',
    moduleName: '系统管理',
    pagePath: '/system-roles',
    fieldCount: 5,
    formType: '弹窗表单',
    status: '已配置',
    remark: '支持角色编码、名称、范围和状态维护'
  }
]

const sceneConfigMap = {
  '/system-users': {
    title: '????',
    subtitle: '??????????????????????????????????????????',
    tableTitle: '????',
    tableSubtitle: '????????????????????????????',
    guide: '???????????????????????????????????????????????',
    constraint: '?????????????????????????????????????????????????',
    quickLinks: [
      { label: '????', path: '/system-orgs', desc: '????????????????' },
      { label: '????', path: '/system-dicts', desc: '???????????????' },
      { label: '????', path: '/system-roles', desc: '????????????????' }
    ]
  },
  '/system-orgs': {
    title: '????',
    subtitle: '???????????????????????????????????',
    tableTitle: '????',
    tableSubtitle: '??????????????????????????????????',
    guide: '?????????????????????????????????????????',
    constraint: '?????????????????????????????????????????',
    quickLinks: [
      { label: '????', path: '/system-users', desc: '?????????????' },
      { label: '????', path: '/system-dicts', desc: '?????????????' },
      { label: '????', path: '/system-roles', desc: '?????????????' }
    ]
  },
  '/system-dicts': {
    title: '??????',
    subtitle: '??????????????????????????????',
    tableTitle: '??????',
    tableSubtitle: '???????????????????????????????????',
    guide: '???????????????????????????????????????',
    constraint: '?????????????????????????????????????????',
    quickLinks: [
      { label: '????', path: '/system-users', desc: '????????????????' },
      { label: '????', path: '/system-orgs', desc: '???????????????' },
      { label: '????', path: '/system-roles', desc: '???????????' }
    ]
  },
  '/system-roles': {
    title: '????',
    subtitle: '?????????????????????????????',
    tableTitle: '????',
    tableSubtitle: '???????????????????????????',
    guide: '?????????????????????????????????????',
    constraint: '?????????????????????????????????????????????',
    quickLinks: [
      { label: '????', path: '/system-users', desc: '???????????' },
      { label: '????', path: '/system-orgs', desc: '???????????' },
      { label: '????', path: '/system-dicts', desc: '??????????????' }
    ]
  },
  '/system-menus': {
    title: '????',
    subtitle: '????????????? + ?????????????????',
    tableTitle: '??????',
    tableSubtitle: '???????????????????????????',
    keywordPlaceholder: '?????????????????',
    note: '????????????????????????????????????????',
    emptyText: '????????',
    guide: '????????????????????????????????????',
    constraint: '?????????????????????????????????????',
    actions: [
      { label: '??????', path: '/dashboard', type: 'primary', plain: false },
      { label: '??????', path: '/system-forms' }
    ],
    quickLinks: [
      { label: '????', path: '/task-history', desc: '????????????????' },
      { label: '????', path: '/detection-history', desc: '????????????????' },
      { label: '????', path: '/review-history', desc: '????????????????' }
    ],
    columns: [
      { key: 'menuName', label: '????', minWidth: 180 },
      { key: 'level', label: '??', width: 110, align: 'center' },
      { key: 'parentName', label: '????', minWidth: 140 },
      { key: 'routePath', label: '????', minWidth: 160 },
      { key: 'componentName', label: '???', minWidth: 160 },
      { key: 'status', label: '??', width: 100, align: 'center', type: 'status', tone: () => 'success' },
      { key: 'remark', label: '??', minWidth: 220 }
    ]
  },
  '/system-logs': {
    title: '????',
    subtitle: '????????????????????????????',
    tableTitle: '??????',
    tableSubtitle: '???????????????????????????',
    keywordPlaceholder: '?????????????',
    note: '??????????????????????????????????????',
    emptyText: '????????',
    guide: '?????????????????????????????????',
    constraint: '?????????????????????????????????',
    actions: [
      { label: '??????', path: '/sample-login', type: 'primary', plain: false },
      { label: '??????', path: '/report-ledger' }
    ],
    quickLinks: [
      { label: '????', path: '/sample-ledger', desc: '?????????????' },
      { label: '????', path: '/detection-ledger', desc: '???????????' },
      { label: '????', path: '/report-ledger', desc: '???????????' }
    ],
    columns: [
      { key: 'sourceName', label: '????', minWidth: 180 },
      { key: 'eventCount', label: '???', width: 100, align: 'center' },
      { key: 'retentionRule', label: '????', minWidth: 180 },
      { key: 'status', label: '??', width: 100, align: 'center', type: 'status', tone: (row) => row.status === '???' ? 'warning' : 'success' },
      { key: 'remark', label: '??', minWidth: 260 }
    ]
  },
  '/system-forms': {
    title: '????',
    subtitle: '??????????????????????????????????',
    tableTitle: '????',
    tableSubtitle: '?????????????? CRUD ???',
    keywordPlaceholder: '??????????????????',
    note: '??????????????????????????????????????????????',
    emptyText: '??????',
    guide: '???????????????????????????????????????',
    constraint: '???????????????????????????????????????',
    actions: [
      { label: '??????', path: '/system-users', type: 'primary', plain: false },
      { label: '??????', path: '/system-dicts' }
    ],
    quickLinks: [
      { label: '????', path: '/system-users', desc: '?????? CRUD ????' },
      { label: '????', path: '/system-orgs', desc: '?????? CRUD ????' },
      { label: '????', path: '/system-dicts', desc: '???????? CRUD ????' }
    ],
    columns: [
      { key: 'formName', label: '????', minWidth: 180 },
      { key: 'moduleName', label: '????', minWidth: 120 },
      { key: 'pagePath', label: '????', minWidth: 150 },
      { key: 'fieldCount', label: '???', width: 100, align: 'center' },
      { key: 'formType', label: '????', minWidth: 120 },
      { key: 'status', label: '??', width: 100, align: 'center', type: 'status', tone: () => 'success' },
      { key: 'remark', label: '??', minWidth: 240 }
    ]
  }
}

const sceneRowsMap = computed(() => ({
  '/system-menus': menuRows.value,
  '/system-forms': formRows
}))

const currentScene = computed(() => sceneConfigMap[route.path] || sceneConfigMap['/system-users'])
const currentSceneRows = computed(() => sceneRowsMap.value[route.path] || [])

const visibleUserRows = computed(() => {
  if (activeStatKey.value === 'role') {
    return userRows.value.filter((item) => normalizeText(item.roleCode) !== '-')
  }
  return userRows.value
})

const visibleOrgRows = computed(() => {
  if (activeStatKey.value === 'inuse') {
    return orgRows.value.filter((item) => Number(item.memberCount || 0) > 0)
  }
  return orgRows.value
})

const visibleDictRows = computed(() => {
  if (activeStatKey.value === 'status') {
    return dictRows.value.filter((item) => String(item.dictCode || '').includes('status'))
  }
  if (activeStatKey.value === 'business') {
    return dictRows.value.filter((item) => String(item.moduleName || '') !== '鍩虹閰嶇疆')
  }
  return dictRows.value
})

const visibleRoleRows = computed(() => {
  if (activeStatKey.value === 'inuse') {
    return roleRows.value.filter((item) => Number(item.userCount || 0) > 0)
  }
  return roleRows.value
})

const visibleSceneRows = computed(() => {
  let rows = currentSceneRows.value
  const keyword = normalizeKeyword(sceneKeyword.value)
  if (keyword) {
    rows = rows.filter((item) => matchesKeyword(item, keyword))
  }

  switch (route.path) {
    case '/system-menus':
      if (activeStatKey.value === 'group') {
        return rows.filter((item) => item.level === '浜岀骇鐩綍')
      }
      if (activeStatKey.value === 'page') {
        return rows.filter((item) => item.level === '涓夌骇椤甸潰')
      }
      return rows
    case '/system-forms':
      if (activeStatKey.value === 'dialog') {
        return rows.filter((item) => String(item.formType).includes('寮圭獥'))
      }
      if (activeStatKey.value === 'page') {
        return rows.filter((item) => String(item.formType).includes('椤甸潰'))
      }
      return rows
    default:
      return rows
  }
})

const currentTags = computed(() => {
  if (isUserScene.value) {
    return [
      { label: '???', value: userRows.value.length, type: 'info' },
      { label: '????', value: userTotal.value, type: 'success' },
      { label: '????', value: normalizeText(currentLoginUser.realName || currentLoginUser.username), type: 'warning' }
    ]
  }

  if (isOrgScene.value) {
    return [
      { label: '???', value: orgRows.value.length, type: 'info' },
      { label: '????', value: orgTotal.value, type: 'success' },
      { label: '???????', value: orgRows.value.filter((item) => Number(item.memberCount || 0) > 0).length, type: 'warning' }
    ]
  }

  if (isDictScene.value) {
    return [
      { label: '???', value: dictRows.value.length, type: 'info' },
      { label: '????', value: dictTotal.value, type: 'success' },
      { label: '?????', value: dictRows.value.filter((item) => String(item.dictCode || '').includes('status')).length, type: 'warning' }
    ]
  }

  if (isRoleScene.value) {
    return [
      { label: '???', value: roleRows.value.length, type: 'info' },
      { label: '????', value: roleTotal.value, type: 'success' },
      { label: '??????', value: roleRows.value.filter((item) => Number(item.userCount || 0) > 0).length, type: 'warning' }
    ]
  }

  if (isLogScene.value) {
    return [
      { label: '???', value: logPageRows.value.length, type: 'info' },
      { label: '????', value: logPageTotal.value, type: 'success' },
      { label: '????', value: getLogSourceFilterLabel(logQuery.sourceType), type: 'warning' }
    ]
  }

  const rows = currentSceneRows.value
  if (route.path === '/system-menus') {
    return [
      { label: '????', value: rows.length, type: 'info' },
      { label: '??', value: rows.filter((item) => item.level === '????').length, type: 'success' },
      { label: '??', value: rows.filter((item) => item.level === '????').length, type: 'warning' }
    ]
  }

  return [
    { label: '???', value: rows.length, type: 'info' },
    { label: '???', value: rows.filter((item) => String(item.status || '').includes('?')).length, type: 'success' },
    { label: '???', value: rows.filter((item) => String(item.status || '').includes('?')).length, type: 'warning' }
  ]
})

const currentStats = computed(() => {
  if (isUserScene.value) {
    return [
      { key: 'all', label: '????', value: userTotal.value, desc: '??????????' },
      { key: 'enabled', label: '????', value: userRows.value.filter((item) => Number(item.status) === 1).length, desc: '??????????' },
      { key: 'disabled', label: '????', value: userRows.value.filter((item) => Number(item.status) === 0).length, desc: '??????????' },
      { key: 'role', label: '????', value: userRows.value.filter((item) => normalizeText(item.roleCode) !== '-').length, desc: '??????????????' }
    ]
  }

  if (isOrgScene.value) {
    return [
      { key: 'all', label: '????', value: orgTotal.value, desc: '??????????' },
      { key: 'enabled', label: '????', value: orgRows.value.filter((item) => Number(item.status) === 1).length, desc: '??????????' },
      { key: 'disabled', label: '????', value: orgRows.value.filter((item) => Number(item.status) === 0).length, desc: '??????????' },
      { key: 'inuse', label: '????', value: orgRows.value.filter((item) => Number(item.memberCount || 0) > 0).length, desc: '????????????' }
    ]
  }

  if (isDictScene.value) {
    return [
      { key: 'all', label: '????', value: dictTotal.value, desc: '????????????' },
      { key: 'enabled', label: '????', value: dictRows.value.filter((item) => Number(item.status) === 1).length, desc: '??????????' },
      { key: 'status', label: '????', value: dictRows.value.filter((item) => String(item.dictCode || '').includes('status')).length, desc: '???????????????' },
      { key: 'business', label: '????', value: dictRows.value.filter((item) => String(item.moduleName || '') !== '????').length, desc: '???????????????' }
    ]
  }

  if (isRoleScene.value) {
    return [
      { key: 'all', label: '????', value: roleTotal.value, desc: '??????????' },
      { key: 'enabled', label: '????', value: roleRows.value.filter((item) => Number(item.status) === 1).length, desc: '??????????' },
      { key: 'disabled', label: '????', value: roleRows.value.filter((item) => Number(item.status) === 0).length, desc: '??????????' },
      { key: 'inuse', label: '????', value: roleRows.value.filter((item) => Number(item.userCount || 0) > 0).length, desc: '????????????' }
    ]
  }

  if (isLogScene.value) {
    return [
      { key: 'all', label: '????', value: logSummary.totalCount, desc: '????????????' },
      { key: 'login', label: '????', value: logSummary.loginCount, desc: '?????????' },
      { key: 'process', label: '????', value: logSummary.processCount, desc: '???????????????' },
      { key: 'push', label: '????', value: logSummary.pushCount, desc: '?????????' }
    ]
  }

  const rows = currentSceneRows.value
  switch (route.path) {
    case '/system-menus':
      return [
        { key: 'all', label: '????', value: rows.length, desc: '????????????' },
        { key: 'group', label: '????', value: rows.filter((item) => item.level === '????').length, desc: '???????' },
        { key: 'page', label: '????', value: rows.filter((item) => item.level === '????').length, desc: '???????' }
      ]
    case '/system-forms':
      return [
        { key: 'all', label: '????', value: rows.length, desc: '?????????' },
        { key: 'dialog', label: '????', value: rows.filter((item) => String(item.formType).includes('??')).length, desc: '????????' },
        { key: 'page', label: '????', value: rows.filter((item) => String(item.formType).includes('??')).length, desc: '??????' }
      ]
    default:
      return [{ key: 'all', label: '??', value: rows.length, desc: '????????' }]
  }
})

watch(
  () => route.path,
  async () => {
    activeStatKey.value = 'all'
    sceneKeyword.value = ''
    resetDictQueryState()
    resetOrgQueryState()
    resetUserQueryState()
    resetRoleQueryState()
    resetLogQueryState()
    await loadPageData()
  }
)

onMounted(async () => {
  await loadPageData()
})

function createDefaultDictForm() {
  return {
    id: '',
    dictCode: '',
    dictName: '',
    moduleName: '',
    itemText: '',
    status: 1,
    remark: ''
  }
}

function createDefaultOrgForm() {
  return {
    id: '',
    orgCode: '',
    orgName: '',
    parentId: '',
    orgType: '',
    status: 1,
    remark: ''
  }
}

function createDefaultUserForm() {
  return {
    id: '',
    username: '',
    password: '',
    realName: '',
    orgId: '',
    roleCode: '',
    phone: '',
    status: 1
  }
}

function createDefaultRoleForm() {
  return {
    id: '',
    roleCode: '',
    roleName: '',
    roleScope: '',
    status: 1,
    remark: ''
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

function formatDictPreview(value) {
  return String(value || '')
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean)
    .join('、') || '-'
}

function getStatusLabelByValue(status) {
  return Number(status) === 1 ? '启用' : '停用'
}

function getStatusClassByValue(status) {
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

function resetDictQueryState() {
  dictQuery.pageNum = 1
  dictQuery.pageSize = DEFAULT_PAGE_SIZE
  dictQuery.keyword = ''
  dictQuery.status = ''
}

function resetOrgQueryState() {
  orgQuery.pageNum = 1
  orgQuery.pageSize = DEFAULT_PAGE_SIZE
  orgQuery.keyword = ''
  orgQuery.status = ''
}

function resetUserQueryState() {
  userQuery.pageNum = 1
  userQuery.pageSize = DEFAULT_PAGE_SIZE
  userQuery.keyword = ''
  userQuery.status = ''
}

function resetRoleQueryState() {
  roleQuery.pageNum = 1
  roleQuery.pageSize = DEFAULT_PAGE_SIZE
  roleQuery.keyword = ''
  roleQuery.status = ''
}

function resetLogQueryState() {
  logQuery.pageNum = 1
  logQuery.pageSize = DEFAULT_PAGE_SIZE
  logQuery.keyword = ''
  logQuery.sourceType = ''
}

function handleStatClick(key) {
  activeStatKey.value = key

  if (isUserScene.value) {
    if (key === 'enabled') {
      userQuery.status = 1
    } else if (key === 'disabled') {
      userQuery.status = 0
    } else {
      userQuery.status = ''
    }
    userQuery.pageNum = 1
    loadUsers()
    return
  }

  if (isOrgScene.value) {
    if (key === 'enabled') {
      orgQuery.status = 1
    } else if (key === 'disabled') {
      orgQuery.status = 0
    } else {
      orgQuery.status = ''
    }
    orgQuery.pageNum = 1
    loadOrgs()
    return
  }

  if (isDictScene.value) {
    if (key === 'enabled') {
      dictQuery.status = 1
    } else {
      dictQuery.status = ''
    }
    dictQuery.pageNum = 1
    loadDicts()
    return
  }

  if (isRoleScene.value) {
    if (key === 'enabled') {
      roleQuery.status = 1
    } else if (key === 'disabled') {
      roleQuery.status = 0
    } else {
      roleQuery.status = ''
    }
    roleQuery.pageNum = 1
    loadRoles()
    return
  }

  if (isLogScene.value) {
    if (key === 'login') {
      logQuery.sourceType = 'LOGIN'
    } else if (key === 'process') {
      logQuery.sourceType = 'PROCESS'
    } else if (key === 'push') {
      logQuery.sourceType = 'PUSH'
    } else {
      logQuery.sourceType = ''
    }
    logQuery.pageNum = 1
    loadLogs()
  }
}

function goRoute(path) {
  if (!path) {
    return
  }
  router.push(path)
}

async function loadPageData() {
  await Promise.all([
    loadUserSummary(),
    loadOrgOptions(),
    loadRoleOptions(),
    loadLogs(),
    loadDicts(),
    loadOrgs(),
    loadUsers(),
    loadRoles()
  ])
}

async function loadLogs() {
  if (!isLogScene.value) {
    logPageRows.value = []
    logPageTotal.value = 0
    logSummary.totalCount = 0
    logSummary.loginCount = 0
    logSummary.processCount = 0
    logSummary.pushCount = 0
    return
  }

  loading.value = true
  try {
    const result = await fetchSystemLogsApi({
      pageNum: logQuery.pageNum,
      pageSize: logQuery.pageSize,
      keyword: String(logQuery.keyword || '').trim() || undefined,
      sourceType: String(logQuery.sourceType || '').trim() || undefined
    })
    logPageRows.value = Array.isArray(result.records) ? result.records : []
    logPageTotal.value = Number(result.total || 0)
    logSummary.totalCount = Number(result.totalCount || 0)
    logSummary.loginCount = Number(result.loginCount || 0)
    logSummary.processCount = Number(result.processCount || 0)
    logSummary.pushCount = Number(result.pushCount || 0)
    syncLogActiveStatKey()
  } finally {
    loading.value = false
  }
}

async function loadDicts() {
  if (!isDictScene.value) {
    dictRows.value = []
    dictTotal.value = 0
    return
  }

  loading.value = true
  try {
    const result = await fetchSystemDictsApi({
      pageNum: dictQuery.pageNum,
      pageSize: dictQuery.pageSize,
      keyword: String(dictQuery.keyword || '').trim() || undefined,
      status: dictQuery.status === '' ? undefined : dictQuery.status
    })
    dictRows.value = Array.isArray(result.records) ? result.records : []
    dictTotal.value = Number(result.total || 0)
  } finally {
    loading.value = false
  }
}

async function loadOrgs() {
  if (!isOrgScene.value) {
    orgRows.value = []
    orgTotal.value = 0
    return
  }

  loading.value = true
  try {
    const result = await fetchSystemOrgsApi({
      pageNum: orgQuery.pageNum,
      pageSize: orgQuery.pageSize,
      keyword: String(orgQuery.keyword || '').trim() || undefined,
      status: orgQuery.status === '' ? undefined : orgQuery.status
    })
    orgRows.value = Array.isArray(result.records) ? result.records : []
    orgTotal.value = Number(result.total || 0)
  } finally {
    loading.value = false
  }
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

async function loadRoles() {
  if (!isRoleScene.value) {
    roleRows.value = []
    roleTotal.value = 0
    return
  }

  loading.value = true
  try {
    const result = await fetchSystemRolesApi({
      pageNum: roleQuery.pageNum,
      pageSize: roleQuery.pageSize,
      keyword: String(roleQuery.keyword || '').trim() || undefined,
      status: roleQuery.status === '' ? undefined : roleQuery.status
    })
    roleRows.value = Array.isArray(result.records) ? result.records : []
    roleTotal.value = Number(result.total || 0)
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

async function loadOrgOptions() {
  try {
    const result = await fetchSystemOrgOptionsApi()
    orgOptions.value = Array.isArray(result)
      ? result.map((item) => ({
          label: `${item.orgName} ${item.orgCode}`,
          value: Number(item.id)
        }))
      : []
  } catch (error) {
    orgOptions.value = []
  }
}

async function loadRoleOptions() {
  try {
    const result = await fetchSystemRoleOptionsApi()
    roleOptions.value = Array.isArray(result)
      ? result.map((item) => ({
          label: `${item.roleName} ${item.roleCode}`,
          value: item.roleCode
        }))
      : []
  } catch (error) {
    roleOptions.value = []
  }
}

function handleDictSearch() {
  dictQuery.pageNum = 1
  loadDicts()
}

function handleOrgSearch() {
  orgQuery.pageNum = 1
  loadOrgs()
}

function handleUserSearch() {
  userQuery.pageNum = 1
  loadUsers()
}

function handleRoleSearch() {
  roleQuery.pageNum = 1
  loadRoles()
}

function handleLogSearch() {
  logQuery.pageNum = 1
  loadLogs()
}

function handleLogSourceChange() {
  logQuery.pageNum = 1
  syncLogActiveStatKey()
  loadLogs()
}

function resetDictQuery() {
  activeStatKey.value = 'all'
  resetDictQueryState()
  loadDicts()
}

function resetOrgQuery() {
  activeStatKey.value = 'all'
  resetOrgQueryState()
  loadOrgs()
}

function resetUserQuery() {
  activeStatKey.value = 'all'
  resetUserQueryState()
  loadUsers()
}

function resetRoleQuery() {
  activeStatKey.value = 'all'
  resetRoleQueryState()
  loadRoles()
}

function resetLogQuery() {
  activeStatKey.value = 'all'
  resetLogQueryState()
  loadLogs()
}

function openDictDialog(row) {
  resetDictForm()
  if (row) {
    dictForm.id = row.id
    dictForm.dictCode = row.dictCode || ''
    dictForm.dictName = row.dictName || ''
    dictForm.moduleName = row.moduleName || ''
    dictForm.itemText = row.itemText || ''
    dictForm.status = Number(row.status) === 0 ? 0 : 1
    dictForm.remark = row.remark || ''
  }
  dictDialogVisible.value = true
}

function openOrgDialog(row) {
  resetOrgForm()
  if (row) {
    orgForm.id = row.id
    orgForm.orgCode = row.orgCode || ''
    orgForm.orgName = row.orgName || ''
    orgForm.parentId = row.parentId || ''
    orgForm.orgType = row.orgType || ''
    orgForm.status = Number(row.status) === 0 ? 0 : 1
    orgForm.remark = row.remark || ''
  }
  orgDialogVisible.value = true
}

function openUserDialog(row) {
  resetUserForm()
  if (row) {
    userForm.id = row.id
    userForm.username = row.username || ''
    userForm.realName = row.realName || ''
    userForm.orgId = row.orgId || ''
    userForm.roleCode = row.roleCode || ''
    userForm.phone = row.phone || ''
    userForm.status = Number(row.status) === 0 ? 0 : 1
  }
  userDialogVisible.value = true
}

function openRoleDialog(row) {
  resetRoleForm()
  if (row) {
    roleForm.id = row.id
    roleForm.roleCode = row.roleCode || ''
    roleForm.roleName = row.roleName || ''
    roleForm.roleScope = row.roleScope || ''
    roleForm.status = Number(row.status) === 0 ? 0 : 1
    roleForm.remark = row.remark || ''
  }
  roleDialogVisible.value = true
}

function resetDictForm() {
  Object.assign(dictForm, createDefaultDictForm())
  dictFormRef.value?.clearValidate?.()
}

function resetOrgForm() {
  Object.assign(orgForm, createDefaultOrgForm())
  orgFormRef.value?.clearValidate?.()
}

function resetUserForm() {
  Object.assign(userForm, createDefaultUserForm())
  userFormRef.value?.clearValidate?.()
}

function resetRoleForm() {
  Object.assign(roleForm, createDefaultRoleForm())
  roleFormRef.value?.clearValidate?.()
}

async function submitDictForm() {
  await dictFormRef.value.validate()
  savingDict.value = true
  try {
    const payload = {
      dictCode: String(dictForm.dictCode || '').trim(),
      dictName: String(dictForm.dictName || '').trim(),
      moduleName: String(dictForm.moduleName || '').trim(),
      itemText: String(dictForm.itemText || '').trim(),
      status: Number(dictForm.status) === 0 ? 0 : 1,
      remark: String(dictForm.remark || '').trim()
    }

    if (dictForm.id) {
      await updateSystemDictApi(dictForm.id, payload)
      ElMessage.success('鏁版嵁瀛楀吀鏇存柊鎴愬姛')
    } else {
      await createSystemDictApi(payload)
      ElMessage.success('鏁版嵁瀛楀吀鏂板鎴愬姛')
    }

    dictDialogVisible.value = false
    await loadDicts()
  } finally {
    savingDict.value = false
  }
}

async function submitOrgForm() {
  await orgFormRef.value.validate()
  savingOrg.value = true
  try {
    const payload = {
      orgCode: String(orgForm.orgCode || '').trim(),
      orgName: String(orgForm.orgName || '').trim(),
      parentId: orgForm.parentId ? Number(orgForm.parentId) : null,
      orgType: String(orgForm.orgType || '').trim(),
      status: Number(orgForm.status) === 0 ? 0 : 1,
      remark: String(orgForm.remark || '').trim()
    }

    if (orgForm.id) {
      await updateSystemOrgApi(orgForm.id, payload)
      ElMessage.success('鏈烘瀯鏇存柊鎴愬姛')
    } else {
      await createSystemOrgApi(payload)
      ElMessage.success('鏈烘瀯鏂板鎴愬姛')
    }

    orgDialogVisible.value = false
    await loadOrgOptions()
    await loadOrgs()
  } finally {
    savingOrg.value = false
  }
}

async function submitUserForm() {
  await userFormRef.value.validate()
  savingUser.value = true
  try {
    const payload = {
      username: String(userForm.username || '').trim(),
      password: String(userForm.password || '').trim(),
      realName: String(userForm.realName || '').trim(),
      orgId: Number(userForm.orgId),
      roleCode: String(userForm.roleCode || '').trim(),
      phone: String(userForm.phone || '').trim(),
      status: Number(userForm.status) === 0 ? 0 : 1
    }

    if (userForm.id) {
      await updateSystemUserApi(userForm.id, payload)
      ElMessage.success('鐢ㄦ埛鏇存柊鎴愬姛')
    } else {
      await createSystemUserApi(payload)
      ElMessage.success('鐢ㄦ埛鏂板鎴愬姛')
    }

    userDialogVisible.value = false
    await loadUsers()
    await loadUserSummary()
  } finally {
    savingUser.value = false
  }
}

async function submitRoleForm() {
  await roleFormRef.value.validate()
  savingRole.value = true
  try {
    const payload = {
      roleCode: String(roleForm.roleCode || '').trim(),
      roleName: String(roleForm.roleName || '').trim(),
      roleScope: String(roleForm.roleScope || '').trim(),
      status: Number(roleForm.status) === 0 ? 0 : 1,
      remark: String(roleForm.remark || '').trim()
    }

    if (roleForm.id) {
      await updateSystemRoleApi(roleForm.id, payload)
      ElMessage.success('瑙掕壊鏇存柊鎴愬姛')
    } else {
      await createSystemRoleApi(payload)
      ElMessage.success('瑙掕壊鏂板鎴愬姛')
    }

    roleDialogVisible.value = false
    await loadRoles()
    await loadRoleOptions()
  } finally {
    savingRole.value = false
  }
}

async function removeDict(row) {
  await ElMessageBox.confirm(`确定删除数据字典“${row.dictName || row.dictCode}”吗？`, '删除确认', {
    type: 'warning'
  })
  await deleteSystemDictApi(row.id)
  ElMessage.success('数据字典删除成功')
  if (dictRows.value.length === 1 && dictQuery.pageNum > 1) {
    dictQuery.pageNum -= 1
  }
  await loadDicts()
}

async function removeOrg(row) {
  await ElMessageBox.confirm(`确定删除机构“${row.orgName || row.orgCode}”吗？`, '删除确认', {
    type: 'warning'
  })
  await deleteSystemOrgApi(row.id)
  ElMessage.success('机构删除成功')
  if (orgRows.value.length === 1 && orgQuery.pageNum > 1) {
    orgQuery.pageNum -= 1
  }
  await loadOrgOptions()
  await loadOrgs()
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

async function removeRole(row) {
  await ElMessageBox.confirm(`确定删除角色“${row.roleName || row.roleCode}”吗？`, '删除确认', {
    type: 'warning'
  })
  await deleteSystemRoleApi(row.id)
  ElMessage.success('角色删除成功')
  if (roleRows.value.length === 1 && roleQuery.pageNum > 1) {
    roleQuery.pageNum -= 1
  }
  await loadRoles()
  await loadRoleOptions()
}

function reloadData() {
  activeStatKey.value = 'all'
  if (isLogScene.value) {
    logQuery.sourceType = ''
    loadLogs()
    return
  }
  if (isDictScene.value) {
    dictQuery.status = ''
    loadDicts()
    return
  }
  if (isOrgScene.value) {
    orgQuery.status = ''
    loadOrgOptions()
    loadOrgs()
    return
  }
  if (isUserScene.value) {
    userQuery.status = ''
    loadUsers()
    loadUserSummary()
    return
  }
  if (isRoleScene.value) {
    roleQuery.status = ''
    loadRoles()
    loadRoleOptions()
  }
}

function syncLogActiveStatKey() {
  if (String(logQuery.sourceType || '') === 'LOGIN') {
    activeStatKey.value = 'login'
    return
  }
  if (String(logQuery.sourceType || '') === 'PROCESS') {
    activeStatKey.value = 'process'
    return
  }
  if (String(logQuery.sourceType || '') === 'PUSH') {
    activeStatKey.value = 'push'
    return
  }
  activeStatKey.value = 'all'
}

function getLogStatusClass(status) {
  const text = String(status || '')
  if (!text) {
    return 'info'
  }
  if (text.includes('失败') || text.includes('驳回') || text.includes('取消')) {
    return 'danger'
  }
  if (text.includes('待')) {
    return 'warning'
  }
  if (text.includes('成功') || text.includes('通过') || text.includes('留痕')) {
    return 'success'
  }
  return 'info'
}

function getLogSourceFilterLabel(sourceType) {
  const value = String(sourceType || '')
  if (!value) {
    return '鍏ㄩ儴'
  }
  const match = logSourceOptions.find((item) => item.value === value)
  return match?.label || value
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

.form-span-2 {
  grid-column: span 2;
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
.system-page :deep(.el-input) {
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

  .form-span-2 {
    grid-column: span 1;
  }
}
</style>
