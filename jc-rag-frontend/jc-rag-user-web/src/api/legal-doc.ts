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


import axios from 'axios'

const request = axios.create({ baseURL: '/api', timeout: 30000 })

export type LegalDocType = 'USER_AGREEMENT' | 'PRIVACY_POLICY'

export interface LegalDocVO {
  id: number
  docType: LegalDocType
  title: string
  contentMd: string
  publishedAt: string | null
}

/** 用户端: 获取已发布的协议 (公开) */
export async function fetchPublishedLegalDoc(docType: LegalDocType): Promise<LegalDocVO | null> {
  const { data } = await request.get(`/legal-doc/${docType}/published`)
  if (!data || !data.success || !data.data) return null
  return data.data as LegalDocVO
}
