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


/**
 * 商业版可通过构建别名替换本模块，向模型服务配置页注入额外服务商。
 * 社区版保持空数组，不包含任何商业版服务商定义。
 */
export interface ModelProviderExtension {
  id: string
  name: string
  logo: string
  logoUrl?: string
  type: 'online' | 'offline'
  baseUrl: string
  websiteUrl?: string
  apiKeyUrl?: string
  models: string[]
}

export const modelProviderExtensions: ModelProviderExtension[] = []
