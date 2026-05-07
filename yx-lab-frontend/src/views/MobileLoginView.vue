<template>
  <div class="mobile-login-shell">
    <div class="mobile-login-card">
      <div class="mobile-login-head">
        <span class="mobile-login-tag">移动闭环</span>
        <h1>阳新化验室移动工作台</h1>
        <p>用手机完成采样、样品登录、检测提交、审核处理和报告回看。</p>
      </div>

      <el-form :model="form" @submit.prevent="submit">
        <el-form-item>
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password size="large" />
        </el-form-item>
        <el-button type="primary" class="submit-btn" size="large" @click="submit">进入移动端</el-button>
      </el-form>

      <div class="mobile-login-foot">
        <span>默认账号：admin / Admin@123</span>
        <el-button link type="primary" @click="goDesktop">切换桌面端</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { getMeApi, mobileLoginApi } from '../api/lab'
import { setToken, setUser } from '../utils/auth'

const router = useRouter()
const form = reactive({
  username: 'admin',
  password: 'Admin@123'
})

async function submit() {
  const loginResult = await mobileLoginApi(form)
  setToken(loginResult.token)
  const user = await getMeApi()
  setUser(user)
  ElMessage.success('移动端登录成功')
  router.push('/mobile')
}

function goDesktop() {
  router.push('/login')
}
</script>

<style scoped>
.mobile-login-shell {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 20px;
  background:
    radial-gradient(circle at top, rgba(22, 119, 255, 0.18), transparent 38%),
    linear-gradient(180deg, #f2f8ff 0%, #eaf3ff 46%, #f9fbff 100%);
}

.mobile-login-card {
  width: min(100%, 420px);
  padding: 28px 22px 24px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(179, 209, 255, 0.85);
  box-shadow: 0 24px 52px rgba(31, 70, 116, 0.14);
}

.mobile-login-head {
  margin-bottom: 22px;
}

.mobile-login-tag {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: #e8f1ff;
  color: #1677ff;
  font-size: 13px;
  font-weight: 700;
}

.mobile-login-head h1 {
  margin: 14px 0 10px;
  font-size: 28px;
  line-height: 1.28;
  color: #1b3656;
}

.mobile-login-head p {
  margin: 0;
  color: #5d748f;
  line-height: 1.7;
}

.submit-btn {
  width: 100%;
  margin-top: 6px;
}

.mobile-login-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  color: #6a8098;
  font-size: 13px;
}

@media (max-width: 480px) {
  .mobile-login-shell {
    padding: 14px;
  }

  .mobile-login-card {
    padding: 24px 18px 20px;
  }

  .mobile-login-head h1 {
    font-size: 24px;
  }

  .mobile-login-foot {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
