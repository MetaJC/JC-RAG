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


export interface KnowledgeBaseItem {
  id: number
  kbCode: string
  kbName: string
  kbDesc?: string
  visibilityScope: string
  /** 公开 KB 是否允许客户端用户自行选择 (0 否 / 1 是) */
  clientSelectable?: number
  /** 所有者用户 ID */
  ownerUserId?: number
  /** 当前用户对该 KB 的最大权限级别 (0 NONE / 1 VIEW / 2 EDIT / 3 MANAGE) */
  myPermLevel?: number
  /** 是否为系统级默认 KB (服务端标记) */
  isDefault?: boolean
  status: number
  createdAt: string
  updatedAt: string
}

export interface KnowledgeBaseCreateRequest {
  kbCode?: string
  kbName: string
  kbDesc: string
}

export interface KnowledgeBaseUpdateRequest {
  kbName: string
  kbDesc?: string
  visibilityScope?: string
  clientSelectable?: number
}

/** 知识库授权详情 */
export interface KbAuthRoleItem {
  roleId: number
  roleCode: string
  roleName: string
  /** 1 VIEW / 2 EDIT / 3 MANAGE / 0 未授权 */
  permLevel: number
}

export interface KbAuthUserItem {
  userId: number
  username: string
  nickname: string
  roleIds: number[]
  roleNames: string[]
  permLevel: number
}

export interface KbAuthDetail {
  kbId: number
  kbName: string
  visibilityScope: string
  clientSelectable?: number
  roleAuths: KbAuthRoleItem[]
  userAuths: KbAuthUserItem[]
}

export interface KbAuthSaveItem {
  roleId?: number
  userId?: number
  /** 1 VIEW / 2 EDIT / 3 MANAGE / 0 取消授权 */
  permLevel: number
}

export interface KbAuthSaveRequest {
  roleAuths: KbAuthSaveItem[]
  userAuths: KbAuthSaveItem[]
}

export interface KnowledgeDocumentItem {
  id: number
  fileName: string
  fileExt: string
  fileSize: number
  contentType?: string
  versionNo: number
  docType: string
  securityLevel: string
  parseStatus: string
  parseMessage?: string
  pageCount: number
  chunkCount: number
  uploadedAt: string
  updatedAt: string
}

export interface KnowledgeDocumentUpdateRequest {
  fileName: string
}

export interface KnowledgeUploadPolicy {
  maxFileSizeBytes: number
  maxRequestSizeBytes: number
  maxFileSizeLabel: string
  maxRequestSizeLabel: string
  allowedExtensions: string[]
  accept: string
  parseTimeoutSeconds: number
}
