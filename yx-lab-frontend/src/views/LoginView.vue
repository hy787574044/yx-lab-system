<template>
  <div class="login-shell">
    <div class="login-side">
      <div class="login-brand">
        <div class="login-brand__mark">YX</div>
        <div>
          <strong>阳新化验室水质管理平台</strong>
        </div>
      </div>

      <div class="login-side__copy">
        <span class="hero-tag">智慧化验室业务中台</span>
        <h1>统一支撑采样、检测、审核、报告和留痕闭环</h1>
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
          <el-form-item label="验证码" label-position="top">
            <div class="captcha-row">
              <el-input
                v-model="form.captchaCode"
                maxlength="4"
                placeholder="请输入验证码"
                @keyup.enter="submit"
              />
              <button class="captcha-image" type="button" title="点击刷新验证码" @click="loadCaptcha">
                <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
                <span v-else>刷新</span>
              </button>
            </div>
          </el-form-item>
          <el-button type="primary" class="submit-btn" :loading="submitting" @click="submit">登录</el-button>
        </el-form>

        <div class="tips">默认账号：admin / Admin@123</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElForm, ElFormItem } from 'element-plus/es/components/form/index.mjs'
import { ElInput } from 'element-plus/es/components/input/index.mjs'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { useRouter } from 'vue-router'
import { fetchCaptchaApi, getMeApi, loginApi } from '../api/lab'
import { setToken, setUser } from '../utils/auth'

const router = useRouter()
const submitting = ref(false)
const captchaImage = ref('')
const form = reactive({
  username: 'admin',
  password: 'Admin@123',
  captchaId: '',
  captchaCode: ''
})

async function submit() {
  if (!form.username || !form.password || !form.captchaCode) {
    ElMessage.warning('请填写用户名、密码和验证码')
    return
  }
  submitting.value = true
  try {
    const loginResult = await loginApi({
      username: form.username,
      password: form.password,
      captchaId: form.captchaId,
      captchaCode: form.captchaCode
    })
    setToken(loginResult.token)
    const user = await getMeApi()
    setUser(user)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (error) {
    form.captchaCode = ''
    await loadCaptcha()
  } finally {
    submitting.value = false
  }
}

async function loadCaptcha() {
  try {
    const result = await fetchCaptchaApi()
    form.captchaId = result.captchaId || ''
    form.captchaCode = ''
    captchaImage.value = result.imageBase64 || ''
  } catch (error) {
    form.captchaId = ''
    form.captchaCode = ''
    captchaImage.value = ''
    console.warn('验证码加载失败，请确认后端服务是否启动。', error)
  }
}

onMounted(loadCaptcha)
</script>

<style scoped>
.login-shell {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background:
    linear-gradient(90deg, rgba(0, 15, 38, 0.48) 0%, rgba(0, 38, 82, 0.12) 52%, rgba(0, 12, 32, 0.36) 100%),
    url("/assets/shouye_Image.png") center center / cover no-repeat,
    linear-gradient(135deg, #0b2f59 0%, #064b87 58%, #041b38 100%);
}

.login-shell::before,
.login-shell::after {
  content: "";
  position: absolute;
  pointer-events: none;
}

.login-shell::before {
  inset: 0;
  background:
    linear-gradient(rgba(255, 255, 255, 0.032) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.032) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: linear-gradient(90deg, rgba(0, 0, 0, 0.86), rgba(0, 0, 0, 0.28), rgba(0, 0, 0, 0.66));
}

.login-shell::after {
  width: 760px;
  height: 760px;
  right: -300px;
  top: -220px;
  border-radius: 50%;
  border: 1px solid rgba(125, 211, 252, 0.16);
  box-shadow:
    inset 0 0 90px rgba(14, 165, 233, 0.08),
    0 0 120px rgba(14, 165, 233, 0.10);
}

.login-side {
  position: relative;
  z-index: 2;
  width: min(62vw, 980px);
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 40px 56px 48px;
  color: #f5fbff;
  overflow: hidden;
}

.login-side::before,
.login-side::after {
  display: none;
}

.login-side::before {
  inset: 0;
  background:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.035) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: linear-gradient(90deg, rgba(0, 0, 0, 0.84), rgba(0, 0, 0, 0.18));
}

.login-side::after {
  width: 720px;
  height: 720px;
  right: -260px;
  top: -210px;
  border-radius: 50%;
  border: 1px solid rgba(125, 211, 252, 0.16);
  box-shadow:
    inset 0 0 80px rgba(14, 165, 233, 0.08),
    0 0 100px rgba(14, 165, 233, 0.10);
}

.login-brand,
.login-side__copy {
  position: relative;
  z-index: 2;
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
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(222, 249, 255, 0.45);
  box-shadow: 0 10px 28px rgba(12, 39, 58, 0.20);
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 1px;
}

.login-brand strong {
  display: block;
  font-size: 18px;
  font-weight: 600;
}

.hero-tag {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(9, 49, 72, 0.46);
  border: 1px solid rgba(173, 237, 255, 0.30);
  color: #dffbff;
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
  color: #f3fbff;
  text-shadow: 0 12px 30px rgba(7, 28, 43, 0.36);
}

.login-main {
  position: absolute;
  z-index: 4;
  top: 0;
  right: clamp(36px, 5vw, 72px);
  bottom: 0;
  width: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  background: transparent;
  border: 0;
  box-shadow: none;
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 34px 32px 30px;
  background: rgba(248, 252, 255, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.82);
  border-radius: 18px;
  box-shadow:
    0 24px 64px rgba(15, 95, 131, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(20px);
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

.captcha-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 132px;
  gap: 10px;
  align-items: center;
}

.captcha-image {
  width: 132px;
  height: 42px;
  padding: 0;
  overflow: hidden;
  border: 1px solid rgba(70, 119, 161, 0.22);
  border-radius: 10px;
  background: #eef7ff;
  cursor: pointer;
}

.captcha-image img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-image span {
  color: #2f6f9f;
  font-size: 13px;
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
  .login-side {
    width: calc(100% - 500px);
    padding: 32px 36px 40px;
  }

  .login-main {
    right: 36px;
  }

  .login-side__copy h1 {
    font-size: 34px;
  }

}

@media (max-width: 980px) {
  .login-side {
    width: 100%;
    min-height: 420px;
  }

  .login-main {
    position: relative;
    inset: auto;
    width: auto;
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

  .login-card {
    padding: 24px 20px;
  }
}
</style>