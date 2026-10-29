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


export interface MenuNode {
  id: number
  parentId?: number | null
  code?: string
  title: string
  path?: string
  icon?: string
  sort?: number
  component?: string
  visible?: number
  status?: number
  children?: MenuNode[]
}

export interface AuthUser {
  userId: number
  username: string
  nickname: string
  userType: string
  /** 用户拥有的全部 permCode 集合（去重） */
  permCodes?: string[]
  /** 用户可见的菜单树（permType=MENU + status=1 + 角色绑定） */
  menus?: MenuNode[]
}

export interface LoginRequest {
  username: string
  password: string
  /** 滑动验证码 token (后端一次性消费) */
  captchaToken?: string
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}
