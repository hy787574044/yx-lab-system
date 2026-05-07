<template>
  <div class="login-shell">
    <div class="login-hero">
      <span class="hero-tag">Yangxin Lab OS</span>
      <h1>把样品、检测、审核和报告，收拢到一条清晰的业务线上。</h1>
      <p>面向县级水务化验室的轻量实验管理平台，突出简洁录入、标准检测和结果追溯。</p>
    </div>
    <div class="login-card glass-panel">
      <h2>系统登录</h2>
      <el-form :model="form" @submit.prevent="submit">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-button type="primary" class="submit-btn" @click="submit">进入系统</el-button>
      </el-form>
      <div class="tips">默认账号：admin / Admin@123</div>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
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
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 24px;
  padding: 24px;
  background: linear-gradient(180deg, #f3f7fb 0%, #eef4fb 100%);
}

.login-hero {
  align-self: center;
  padding: 0 6vw;
}

.hero-tag {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 14px;
  font-weight: 600;
}

.login-hero h1 {
  max-width: 640px;
  margin: 18px 0 14px;
  font-size: 36px;
  line-height: 1.35;
  color: var(--text-main);
}

.login-hero p {
  max-width: 560px;
  color: var(--text-sub);
  font-size: 14px;
  line-height: 1.8;
}

.login-card {
  align-self: center;
  padding: 28px 24px;
  background: var(--bg-panel);
  border-radius: 16px;
  box-shadow: var(--shadow-md);
}

.login-card h2 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 22px;
}

.submit-btn {
  width: 100%;
  height: 40px;
}

.tips {
  margin-top: 14px;
  color: var(--text-sub);
  font-size: 14px;
}

@media (max-width: 980px) {
  .login-shell {
    grid-template-columns: 1fr;
    padding: 16px;
  }

  .login-hero {
    padding: 0;
  }

  .login-hero h1 {
    font-size: 28px;
  }
}
</style>
