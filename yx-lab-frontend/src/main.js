import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import App from './App.vue'
import router from './router'
import './styles/theme.css'

dayjs.locale('zh-cn')

const savedTheme = localStorage.getItem('yx-lab-theme')
if (savedTheme) {
  document.documentElement.setAttribute('data-theme', savedTheme)
}

createApp(App)
  .use(createPinia())
  .use(router)
  .use(ElementPlus, { locale: zhCn })
  .mount('#app')
