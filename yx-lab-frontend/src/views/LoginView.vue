<template>
  <div class="login-shell">
    <div class="login-side">
      <div class="login-brand">
        <div class="login-brand__mark">YX</div>
        <div>
          <strong>阳新实验室水质管理平台</strong>
          <p>Laboratory Water Quality Management Platform</p>
        </div>
      </div>

      <div class="login-side__copy">
        <span class="hero-tag">智慧实验室业务中台</span>
        <h1>统一支撑采样、检测、审核、报告和留痕闭环</h1>
        <p>
          依据阳新实验室 PC 端设计规范，构建标准化、规范化、可追踪的实验室业务工作界面，
          让业务办理、台账管理和状态流转都保持一致的后台体验。
        </p>
      </div>

      <div class="login-side__footer">
        <div class="footer-card">
          <strong>统一业务入口</strong>
          <span>覆盖监测点位、样品采样、检测分析、结果审核、报告发布等核心模块。</span>
        </div>
        <div class="footer-card">
          <strong>规范视觉体系</strong>
          <span>使用统一色板、导航结构、表单布局、表格样式和状态色规范。</span>
        </div>
      </div>
    </div>

    <div class="login-main">
      <div class="login-card">
        <div class="login-card__head">
          <h2>系统登录</h2>
          <p>请输入账号密码进入平台</p>
        </div>

        <el-form :model="form" class="login-form" @submit.prevent="submit">
          <el-form-item label="用户名" label-position="top">
            <el-input v-model="form.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码" label-position="top">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
          </el-form-item>
          <el-button type="primary" class="submit-btn" @click="submit">登录系统</el-button>
        </el-form>

        <div class="tips">默认账号：admin / Admin@123</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElForm, ElFormItem } from 'element-plus/es/components/form/index.mjs'
import { ElInput } from 'element-plus/es/components/input/index.mjs'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { useRouter } from 'vue-router'
import { getMeApi, loginApi } from '../api/lab'
import { setToken, setUser } from '../utils/auth'

const router = useRouter()
const form = reactive({
  username: 'admin',
  password: 'Admin@123'
})

async function submit() {
  const loginResult = await loginApi(form)
  setToken(loginResult.token)
  const user = await getMeApi()
  setUser(user)
  ElMessage.success('登录成功')
  router.push('/dashboard')
}
</script>

<style scoped>
.login-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) 520px;
  background:
    radial-gradient(circle at right top, rgba(118, 175, 255, 0.28) 0, rgba(118, 175, 255, 0.28) 120px, transparent 121px),
    radial-gradient(circle at 90% 18%, rgba(34, 131, 255, 0.12) 0, rgba(34, 131, 255, 0.12) 240px, transparent 241px),
    linear-gradient(135deg, #1e63dc 0%, #278cff 52%, #2ec0ff 100%);
}

.login-side {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 40px 56px 48px;
  color: #ffffff;
  overflow: hidden;
}

.login-side::before,
.login-side::after {
  content: "";
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.login-side::before {
  width: 320px;
  height: 320px;
  right: -60px;
  top: -80px;
}

.login-side::after {
  width: 520px;
  height: 520px;
  right: -180px;
  top: -160px;
}

.login-brand,
.login-side__copy,
.login-side__footer {
  position: relative;
  z-index: 1;
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.login-brand__mark {
  width: 52px;
  height: 52px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.24);
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 1px;
}

.login-brand strong {
  display: block;
  font-size: 18px;
  font-weight: 600;
}

.login-brand p {
  margin: 4px 0 0;
  color: rgba(255, 255, 255, 0.76);
  font-size: 13px;
}

.hero-tag {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
}

.login-side__copy {
  max-width: 620px;
  margin-top: auto;
  margin-bottom: auto;
}

.login-side__copy h1 {
  margin: 22px 0 18px;
  font-size: 40px;
  line-height: 1.35;
  font-weight: 700;
}

.login-side__copy p {
  max-width: 520px;
  margin: 0;
  color: rgba(255, 255, 255, 0.82);
  font-size: 15px;
  line-height: 1.9;
}

.login-side__footer {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.footer-card {
  padding: 16px 18px;
  border-radius: 10px;
  background: rgba(7, 33, 89, 0.22);
  border: 1px solid rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(8px);
}

.footer-card strong {
  display: block;
  margin-bottom: 8px;
  font-size: 15px;
}

.footer-card span {
  color: rgba(255, 255, 255, 0.78);
  font-size: 13px;
  line-height: 1.7;
}

.login-main {
  display: grid;
  place-items: center;
  padding: 32px;
  background: linear-gradient(180deg, #f5f9ff 0%, #eef4fb 100%);
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 32px 32px 28px;
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid #d9e5f2;
  border-radius: 12px;
  box-shadow: 0 12px 30px rgba(31, 83, 170, 0.14);
}

.login-card__head h2 {
  margin: 0;
  color: #24384d;
  font-size: 28px;
  line-height: 1.3;
}

.login-card__head p {
  margin: 10px 0 0;
  color: #7587a0;
  font-size: 14px;
}

.login-form {
  margin-top: 24px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.login-form :deep(.el-form-item__label) {
  padding-bottom: 8px;
  color: #3a4a63;
  font-size: 14px;
  line-height: 1.3;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 42px;
}

.submit-btn {
  width: 100%;
  height: 42px;
  margin-top: 4px;
}

.tips {
  margin-top: 16px;
  color: #6f8199;
  font-size: 13px;
  text-align: center;
}

@media (max-width: 1200px) {
  .login-shell {
    grid-template-columns: 1fr 460px;
  }

  .login-side {
    padding: 32px 36px 40px;
  }

  .login-side__copy h1 {
    font-size: 34px;
  }
}

@media (max-width: 980px) {
  .login-shell {
    grid-template-columns: 1fr;
  }

  .login-side {
    min-height: 420px;
  }

  .login-main {
    padding: 20px;
  }
}

@media (max-width: 768px) {
  .login-side {
    padding: 24px 20px 28px;
  }

  .login-side__copy h1 {
    font-size: 28px;
  }

  .login-side__footer {
    grid-template-columns: 1fr;
  }

  .login-card {
    padding: 24px 20px;
  }
}
</style>
