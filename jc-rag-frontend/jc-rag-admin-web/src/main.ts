/*
 * Copyright 2024-2026 OpenJC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 本文件来源于 JC RAG Community Edition
 * 开源地址: https://gitee.com/Open-JC/jc-rag | https://github.com/MetaJC/JC-RAG
 * 二次开发请保留此出处声明。
 */


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
