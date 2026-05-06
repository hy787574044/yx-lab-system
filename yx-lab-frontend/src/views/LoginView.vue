<template>
  <div class="login-shell">
    <div class="login-hero">
      <span class="hero-tag">Yangxin Lab OS</span>
      <h1>把样品、检测、审核和报告，收拢到一条清晰的业务线里。</h1>
      <p>面向县级水厂化验室的轻量实验管理平台，突出简洁录入、标准检测和结果追溯。</p>
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
  grid-template-columns: 1.2fr 420px;
  gap: 36px;
  padding: 40px;
  background:
    radial-gradient(circle at top left, rgba(15, 109, 122, 0.18), transparent 32%),
    radial-gradient(circle at bottom right, rgba(240, 139, 87, 0.18), transparent 28%),
    var(--bg-page);
}

.login-hero {
  align-self: center;
  padding: 0 4vw;
}

.hero-tag {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(15, 109, 122, 0.1);
  color: var(--brand);
  font-size: 12px;
  letter-spacing: 1px;
}

.login-hero h1 {
  max-width: 580px;
  margin: 20px 0 16px;
  font-size: 52px;
  line-height: 1.15;
}

.login-hero p {
  max-width: 520px;
  color: var(--text-sub);
  font-size: 17px;
  line-height: 1.7;
}

.login-card {
  align-self: center;
  padding: 36px;
  background: var(--bg-panel-strong);
}

.login-card h2 {
  margin-top: 0;
  margin-bottom: 22px;
}

.submit-btn {
  width: 100%;
  height: 46px;
}

.tips {
  margin-top: 18px;
  color: var(--text-sub);
  font-size: 13px;
}

@media (max-width: 980px) {
  .login-shell {
    grid-template-columns: 1fr;
    padding: 24px;
  }

  .login-hero {
    padding: 0;
  }

  .login-hero h1 {
    font-size: 36px;
  }
}
</style>
