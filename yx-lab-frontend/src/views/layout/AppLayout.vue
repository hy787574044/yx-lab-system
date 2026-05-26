<template>
  <div class="layout-root">
    <header class="layout-topbar">
      <div class="topbar-left">
        <button type="button" class="brand-panel" @click="goRoute('/dashboard')">
          <div class="brand-mark">YX</div>
          <div class="brand-copy">
            <strong>云河化验室水质管理平台</strong>
          </div>
        </button>

        <button type="button" class="top-level-entry" @click="goRoute('/dashboard')">
          <span class="top-level-entry__label">一级菜单</span>
          <strong>水质管理</strong>
        </button>
      </div>

      <div class="topbar-right">
        <el-popover placement="bottom-end" trigger="click" width="320" popper-class="skin-popover">
          <template #reference>
            <button type="button" class="topbar-action skin-trigger">
              <el-icon><SetUp /></el-icon>
              <span>换肤</span>
            </button>
          </template>

          <div class="skin-panel">
            <div class="skin-panel__head">
              <strong>系统换肤</strong>
              <span>按规范色板切换主题</span>
            </div>
            <button
              v-for="theme in themeOptions"
              :key="theme.id"
              type="button"
              :class="['skin-option', { 'is-active': currentThemeId === theme.id }]"
              @click="changeTheme(theme.id)"
            >
              <span class="skin-option__palette">
                <i :style="{ background: theme.primary }"></i>
                <i :style="{ background: theme.secondary }"></i>
                <i :style="{ background: theme.accent }"></i>
              </span>
              <span class="skin-option__meta">
                <strong>{{ theme.name }}</strong>
                <small>{{ theme.primary }} / {{ theme.secondary }}</small>
              </span>
            </button>
          </div>
        </el-popover>

        <button type="button" class="topbar-action" @click="showMessageTip">
          <el-icon><Bell /></el-icon>
          <span>消息</span>
          <em>99+</em>
        </button>

        <button
          type="button"
          :class="['topbar-action', 'icon-only', 'topbar-action--fullscreen', { 'is-active': isFullscreen }]"
          :title="isFullscreen ? '退出全屏' : '进入全屏'"
          @click="toggleFullscreen"
        >
          <el-icon><component :is="isFullscreen ? Fold : FullScreen" /></el-icon>
        </button>

        <el-dropdown trigger="click" popper-class="user-dropdown-popper" @command="handleUserCommand">
          <button type="button" class="user-box">
            <div class="user-avatar">
              <img v-if="userAvatarSrc" :src="userAvatarSrc" alt="用户头像" />
              <span v-else>{{ userInitial }}</span>
            </div>
            <div class="user-copy">
              <strong>{{ user.realName || user.username || '管理员' }}</strong>
              <p>{{ user.roleCode || 'ADMIN' }}</p>
            </div>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">修改资料</el-dropdown-item>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div class="layout-main">
      <aside class="sidebar">
        <div class="sidebar-head">
          <span class="sidebar-head__caption">功能导航</span>
          <strong>水质管理</strong>
          <p>左侧按业务模块展开原一级菜单与二级菜单。</p>
        </div>

        <div class="sidebar-body">
          <el-menu
            ref="menuRef"
            :default-active="currentRoutePath"
            :default-openeds="defaultOpenMenuIds"
            router
            unique-opened
            :collapse-transition="false"
            :active-text-color="'#4080ff'"
            class="menu-panel"
          >
            <el-sub-menu v-for="group in primaryMenus" :key="group.id" :index="group.id">
              <template #title>
                <el-icon :key="group.id + '-icon'">
                  <component :is="iconMap[group.iconKey]" :key="group.iconKey" />
                </el-icon>
                <span>{{ group.title }}</span>
              </template>

              <el-menu-item v-for="item in group.children" :key="item.path" :index="item.path">
                <span>{{ item.title }}</span>
              </el-menu-item>
            </el-sub-menu>
          </el-menu>
        </div>

        <div class="sidebar-footer">
          <span>当前模块</span>
          <strong>{{ currentPrimaryMenu?.title || '-' }}</strong>
          <p>{{ currentSecondaryMenu?.title || '-' }}</p>
        </div>
      </aside>

      <main class="workspace">
        <section class="view-body">
          <router-view />
        </section>
      </main>
    </div>

    <el-dialog v-model="profileDialogVisible" title="修改资料" width="520px" destroy-on-close @closed="resetProfileForm">
      <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="90px">
        <el-form-item label="头像">
          <div class="profile-avatar-editor">
            <div class="profile-avatar-preview">
              <img v-if="profileAvatarSrc" :src="profileAvatarSrc" alt="头像预览" />
              <span v-else>{{ userInitial }}</span>
            </div>
            <div class="profile-avatar-actions">
              <el-upload
                :auto-upload="false"
                :show-file-list="false"
                accept="image/png,image/jpeg,image/jpg,image/webp"
                :on-change="handleAvatarChange"
              >
                <el-button>选择头像</el-button>
              </el-upload>
              <el-button v-if="profileForm.avatarUrl || avatarPreviewUrl" text @click="clearAvatar">移除头像</el-button>
              <p>支持 JPG、PNG、WEBP，建议使用正方形图片。</p>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input :model-value="user.username || '-'" disabled />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="profileForm.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingProfile" @click="submitProfileForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="520px" destroy-on-close @closed="resetPasswordForm">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingPassword" @click="submitPasswordForm">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElDialog } from 'element-plus/es/components/dialog/index.mjs'
import { ElDropdown, ElDropdownItem, ElDropdownMenu } from 'element-plus/es/components/dropdown/index.mjs'
import { ElForm, ElFormItem } from 'element-plus/es/components/form/index.mjs'
import { ElIcon } from 'element-plus/es/components/icon/index.mjs'
import { ElInput } from 'element-plus/es/components/input/index.mjs'
import { ElMenu, ElMenuItem, ElSubMenu } from 'element-plus/es/components/menu/index.mjs'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElPopover } from 'element-plus/es/components/popover/index.mjs'
import { ElUpload } from 'element-plus/es/components/upload/index.mjs'
import {
  Bell,
  Cpu,
  DataAnalysis,
  DataLine,
  Document,
  DocumentChecked,
  Files,
  FullScreen,
  Fold,
  List,
  LocationFilled,
  PieChart,
  Setting,
  SetUp,
  Tickets
} from '@element-plus/icons-vue'
import { changeMyPasswordApi, logoutApi, previewStorageFileApi, updateMyProfileApi, uploadStorageFileApi } from '../../api/lab'
import { getPublicFileUrl } from '../../config/appConfig'
import { clearToken, getToken, getUser, setUser } from '../../utils/auth'
import { labMenuGroups } from '../../router/menuConfig'
import { getMenuPermissionCode } from '../../utils/menuPermission'

const THEME_STORAGE_KEY = 'yx-lab-theme'

const iconMap = {
  DataAnalysis,
  LocationFilled,
  Tickets,
  List,
  DataLine,
  DocumentChecked,
  Document,
  Cpu,
  Files,
  PieChart,
  Setting
}

const themeOptions = [
  { id: '科技蓝', name: '科技蓝', primary: '#1677FF', secondary: '#20BEF5', accent: '#8CC8FF' },
  { id: '湖湾青', name: '湖湾青', primary: '#0F9B8E', secondary: '#36CFC9', accent: '#A0E7E0' },
  { id: '政务绿', name: '政务绿', primary: '#18A058', secondary: '#52C41A', accent: '#B7EB8F' },
  { id: '暖阳橙', name: '暖阳橙', primary: '#F08C2E', secondary: '#FAAD14', accent: '#FFD591' },
  { id: '星夜紫', name: '星夜紫', primary: '#6F62FF', secondary: '#B37FEB', accent: '#D3ADF7' }
]

const router = useRouter()
const route = useRoute()
const menuRef = ref()
const isFullscreen = ref(Boolean(document.fullscreenElement))
const currentThemeId = ref(themeOptions[0].id)
const user = ref(getUser() || {})
const profileDialogVisible = ref(false)
const passwordDialogVisible = ref(false)
const savingProfile = ref(false)
const savingPassword = ref(false)
const profileFormRef = ref()
const passwordFormRef = ref()
const selectedAvatarFile = ref(null)
const avatarPreviewUrl = ref('')
const userAvatarSrc = ref('')

const profileForm = reactive({
  realName: '',
  phone: '',
  avatarUrl: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const primaryMenus = computed(() => {
  const permissionCodes = Array.isArray(user.value.permissionCodes) ? user.value.permissionCodes : []
  const visible = (path) => {
    const code = getMenuPermissionCode(path)
    return !code || permissionCodes.includes('*') || permissionCodes.includes(code)
  }
  return labMenuGroups
    .map((group) => ({
      ...group,
      children: group.children.filter((item) => visible(item.path))
    }))
    .filter((group) => group.children.length > 0)
})

const userInitial = computed(() => (user.value.realName || user.value.username || '管').slice(0, 1))
const currentRoutePath = computed(() => route.path)
const profileAvatarSrc = computed(() => avatarPreviewUrl.value || (profileForm.avatarUrl ? userAvatarSrc.value : ''))

const profileRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ max: 32, message: '手机号长度不能超过32个字符', trigger: 'blur' }]
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

const currentPrimaryMenu = computed(() => (
  primaryMenus.value.find((item) => item.id === route.meta?.primaryId)
  || primaryMenus.value.find((item) => item.children.some((child) => child.path === route.path))
  || primaryMenus.value[0]
))
const currentSecondaryMenus = computed(() => currentPrimaryMenu.value?.children || [])
const defaultOpenMenuIds = computed(() => currentPrimaryMenu.value?.id ? [currentPrimaryMenu.value.id] : [])
const currentSecondaryMenu = computed(() => (
  currentSecondaryMenus.value.find((item) => item.path === route.path)
  || currentSecondaryMenus.value[0]
  || { title: route.meta?.title || '页面' }
))

function applyTheme(themeId) {
  const matchedTheme = themeOptions.find((item) => item.id === themeId) || themeOptions[0]
  currentThemeId.value = matchedTheme.id
  document.documentElement.setAttribute('data-theme', matchedTheme.id)
  localStorage.setItem(THEME_STORAGE_KEY, matchedTheme.id)
}

function changeTheme(themeId) {
  applyTheme(themeId)
  ElMessage.success(`已切换为${themeId}主题`)
}

function initTheme() {
  applyTheme(localStorage.getItem(THEME_STORAGE_KEY) || themeOptions[0].id)
}

function goRoute(path) {
  if (path && route.path !== path) {
    router.push(path)
  }
}

function showMessageTip() {
  ElMessage.info('当前为演示消息入口，后续可接入正式消息中心。')
}

function syncFullscreenState() {
  isFullscreen.value = Boolean(document.fullscreenElement)
}

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen?.()
    return
  }
  document.exitFullscreen?.()
}

function handleUserCommand(command) {
  if (command === 'profile') {
    openProfileDialog()
    return
  }
  if (command === 'password') {
    openPasswordDialog()
    return
  }
  if (command === 'logout') {
    logout()
  }
}

function openProfileDialog() {
  profileForm.realName = user.value.realName || ''
  profileForm.phone = user.value.phone || ''
  profileForm.avatarUrl = user.value.avatarUrl || ''
  selectedAvatarFile.value = null
  clearAvatarPreview()
  profileDialogVisible.value = true
}

function openPasswordDialog() {
  resetPasswordForm()
  passwordDialogVisible.value = true
}

function resetProfileForm() {
  profileForm.realName = ''
  profileForm.phone = ''
  profileForm.avatarUrl = ''
  selectedAvatarFile.value = null
  clearAvatarPreview()
  profileFormRef.value?.clearValidate?.()
}

function resetPasswordForm() {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.clearValidate?.()
}

async function submitProfileForm() {
  await profileFormRef.value.validate()
  savingProfile.value = true
  try {
    let avatarUrl = profileForm.avatarUrl
    if (selectedAvatarFile.value) {
      const uploadResult = await uploadStorageFileApi(selectedAvatarFile.value)
      avatarUrl = getPublicFileUrl(uploadResult?.fullUrl || uploadResult?.filePath || '')
    }
    const nextUser = await updateMyProfileApi({
      realName: String(profileForm.realName || '').trim(),
      phone: String(profileForm.phone || '').trim(),
      avatarUrl
    })
    setUser(nextUser)
    user.value = nextUser || {}
    profileDialogVisible.value = false
    ElMessage.success('资料修改成功')
  } finally {
    savingProfile.value = false
  }
}

function handleAvatarChange(file) {
  const rawFile = file.raw
  if (!rawFile) {
    return
  }
  if (!rawFile.type?.startsWith('image/')) {
    ElMessage.warning('请选择图片文件作为头像')
    return
  }
  if (rawFile.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像图片不能超过 2MB')
    return
  }
  selectedAvatarFile.value = rawFile
  clearAvatarPreview()
  avatarPreviewUrl.value = URL.createObjectURL(rawFile)
}

function clearAvatar() {
  profileForm.avatarUrl = ''
  selectedAvatarFile.value = null
  clearAvatarPreview()
}

function clearAvatarPreview() {
  if (avatarPreviewUrl.value) {
    URL.revokeObjectURL(avatarPreviewUrl.value)
    avatarPreviewUrl.value = ''
  }
}

function clearUserAvatarSrc() {
  if (userAvatarSrc.value) {
    URL.revokeObjectURL(userAvatarSrc.value)
    userAvatarSrc.value = ''
  }
}

async function loadUserAvatar() {
  clearUserAvatarSrc()
  if (!user.value.avatarUrl || !getToken()) {
    return
  }
  try {
    const response = await previewStorageFileApi(user.value.avatarUrl)
    userAvatarSrc.value = URL.createObjectURL(response.data)
  } catch {
    userAvatarSrc.value = ''
  }
}

async function submitPasswordForm() {
  await passwordFormRef.value.validate()
  savingPassword.value = true
  try {
    await changeMyPasswordApi({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    passwordDialogVisible.value = false
    ElMessage.success('密码修改成功，请牢记新密码')
  } finally {
    savingPassword.value = false
  }
}

async function logout() {
  try {
    await logoutApi()
  } catch {
    // 即使服务端令牌已经失效，也要清理本地登录状态。
  }
  clearToken()
  router.push('/login')
}

function syncUserFromStorage(event) {
  user.value = event?.detail || getUser() || {}
  loadUserAvatar()
}

watch(
  () => currentPrimaryMenu.value?.id,
  async (menuId) => {
    if (!menuId) {
      return
    }
    await nextTick()
    menuRef.value?.open(menuId)
  },
  { immediate: true }
)

onMounted(() => {
  document.addEventListener('fullscreenchange', syncFullscreenState)
  initTheme()
  window.addEventListener('yx-lab-user-updated', syncUserFromStorage)
  loadUserAvatar()
})

onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', syncFullscreenState)
  window.removeEventListener('yx-lab-user-updated', syncUserFromStorage)
  clearAvatarPreview()
  clearUserAvatarSrc()
})
</script>

<style scoped>
.layout-root {
  height: 100vh;
  min-height: 100vh;
  padding-top: var(--layout-topbar-height);
  background: var(--bg-page);
  overflow: hidden;
}

.layout-topbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  height: var(--layout-topbar-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 22px;
  background: linear-gradient(90deg, var(--brand-gradient-start) 0%, var(--brand-gradient-end) 100%);
  box-shadow: 0 4px 16px rgba(17, 61, 122, 0.18);
}

.topbar-left,
.topbar-right {
  display: flex;
  align-items: center;
  min-width: 0;
}

.topbar-left {
  flex: 1;
  gap: 20px;
}

.topbar-right {
  gap: 12px;
  flex-wrap: nowrap;
  justify-content: flex-end;
}

.brand-panel {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0;
  border: none;
  background: transparent;
  color: #ffffff;
  cursor: pointer;
}

.brand-mark {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.24);
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 1px;
}

.brand-copy strong,
.user-copy strong {
  display: block;
  line-height: 1.3;
}

.brand-copy strong {
  font-size: 16px;
  font-weight: 600;
}

.top-level-entry {
  min-width: 150px;
  display: grid;
  gap: 4px;
  padding: 10px 16px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.12);
  color: #ffffff;
  text-align: left;
  cursor: pointer;
}

.top-level-entry__label {
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
}

.top-level-entry strong {
  font-size: 16px;
  line-height: 1.2;
}

.topbar-action {
  height: 38px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 14px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 19px;
  background: rgba(255, 255, 255, 0.12);
  color: #ffffff;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease;
}

.topbar-action:hover {
  background: rgba(255, 255, 255, 0.18);
  border-color: rgba(255, 255, 255, 0.28);
}

.topbar-action em {
  font-style: normal;
  min-width: 24px;
  padding: 0 6px;
  border-radius: 999px;
  background: #ff6b6b;
  color: #ffffff;
  font-size: 12px;
  line-height: 18px;
}

.topbar-action.icon-only {
  width: 38px;
  justify-content: center;
  padding: 0;
}

.skin-trigger {
  min-width: 86px;
}

.skin-panel {
  display: grid;
  gap: 10px;
}

.skin-panel__head {
  display: grid;
  gap: 4px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--line-soft);
}

.skin-panel__head strong {
  color: var(--text-main);
  font-size: 15px;
}

.skin-panel__head span {
  color: var(--text-sub);
  font-size: 12px;
}

.skin-option {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #dce6f2;
  border-radius: 12px;
  background: #ffffff;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}

.skin-option:hover,
.skin-option.is-active {
  border-color: var(--brand);
  background: var(--brand-soft);
}

.skin-option__palette {
  display: inline-flex;
  gap: 6px;
}

.skin-option__palette i {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.06) inset;
}

.skin-option__meta {
  display: grid;
  gap: 2px;
}

.skin-option__meta strong {
  color: var(--text-main);
  font-size: 14px;
}

.skin-option__meta small {
  color: var(--text-sub);
  font-size: 12px;
}

.user-box {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0;
  border: none;
  background: transparent;
  color: #ffffff;
  cursor: pointer;
  text-align: left;
}

.user-avatar {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  border: 1px solid rgba(255, 255, 255, 0.28);
  font-weight: 700;
  overflow: hidden;
}

.user-avatar img,
.profile-avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-copy p {
  margin: 4px 0 0;
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
  line-height: 1.2;
}

.profile-avatar-editor {
  display: flex;
  align-items: center;
  gap: 16px;
}

.profile-avatar-preview {
  width: 72px;
  height: 72px;
  flex: none;
  display: grid;
  place-items: center;
  border-radius: 50%;
  overflow: hidden;
  color: #ffffff;
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--brand-gradient-start), var(--brand-gradient-end));
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.34);
}

.profile-avatar-actions {
  display: grid;
  gap: 8px;
}

.profile-avatar-actions p {
  margin: 0;
  color: var(--text-sub);
  font-size: 12px;
  line-height: 1.6;
}

.layout-main {
  height: calc(100vh - var(--layout-topbar-height));
  min-height: calc(100vh - var(--layout-topbar-height));
  overflow: hidden;
}

.sidebar {
  position: fixed;
  top: var(--layout-topbar-height);
  left: 0;
  bottom: 0;
  width: var(--layout-sidebar-width);
  background: var(--bg-sidebar);
  color: #d4dbeb;
  display: flex;
  flex-direction: column;
  padding: 18px 0 12px;
  box-shadow: inset -1px 0 0 rgba(255, 255, 255, 0.04);
  overflow: hidden;
}

.sidebar-head {
  display: grid;
  gap: 6px;
  padding: 0 18px 14px;
}

.sidebar-head__caption,
.sidebar-footer span {
  color: #8b95af;
  font-size: 12px;
  letter-spacing: 1px;
}

.sidebar-head strong,
.sidebar-footer strong {
  color: #ffffff;
  font-size: 15px;
  font-weight: 600;
}

.sidebar-head p,
.sidebar-footer p {
  margin: 0;
  color: #8b95af;
  font-size: 12px;
  line-height: 1.7;
}

.sidebar-footer {
  margin-top: auto;
  padding: 16px 18px 6px;
  display: grid;
  gap: 4px;
  flex-shrink: 0;
}

.sidebar-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-gutter: stable;
  overscroll-behavior: contain;
}

.workspace {
  display: flex;
  flex-direction: column;
  height: calc(100vh - var(--layout-topbar-height));
  min-height: 0;
  min-width: 0;
  margin-left: var(--layout-sidebar-width);
  overflow: hidden;
  padding: 10px 14px 14px;
}

.view-body {
  flex: 1;
  min-height: 0;
  min-width: 0;
  overflow: auto;
  padding-top: 0;
}

:deep(.skin-popover) {
  padding: 14px !important;
  border-radius: 16px !important;
}

:deep(.menu-panel) {
  width: 100%;
  border-right: none;
  background: transparent;
  --el-menu-bg-color: transparent;
  --el-menu-border-color: transparent;
  --el-menu-text-color: #d4dbeb;
  --el-menu-hover-bg-color: var(--bg-sidebar-hover);
  --el-menu-active-color: #ffffff;
}

:deep(.menu-panel .el-sub-menu__title) {
  height: 46px;
  margin: 0 0 6px;
  padding-left: 18px !important;
  color: #ffffff;
  font-weight: 600;
}

:deep(.menu-panel .el-menu-item) {
  height: 42px;
  margin: 0 0 4px;
  padding-left: 46px !important;
  border-left: 3px solid transparent;
  color: #d4dbeb;
}

:deep(.menu-panel .el-menu-item:hover) {
  background: var(--bg-sidebar-hover);
  color: #ffffff;
}

:deep(.menu-panel .el-menu-item.is-active) {
  background: var(--bg-sidebar-active);
  border-left-color: var(--brand);
  color: #ffffff;
}

.sidebar-body::-webkit-scrollbar {
  width: 8px;
}

.sidebar-body::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.24);
}

.sidebar-body::-webkit-scrollbar-track {
  background: transparent;
}
</style>
