import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import 'element-plus/dist/index.css'
import * as ElIcons from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import { pinia } from './stores'
import permissionDirective from './directives/permission'
import './style.css'

const app = createApp(App)

app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.directive('permission', permissionDirective)

/** 全局注册所有 Element Plus 图标
 * <p>侧边栏 / 菜单管理树里通过 DB 存的图标名字符串渲染: <component :is="iconName" />
 * <p>必须先 app.component(name, comp) 才能让 Vue 解析字符串
 * <p>Element Plus 默认不全局注册, 这里一次性注册
 */
Object.entries(ElIcons).forEach(([k, v]) => {
  if (k === 'default') return
  if (!/^[A-Z]/.test(k)) return
  app.component(k, v as any)
})

app.mount('#app')
