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
