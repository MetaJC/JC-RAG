import { request } from './http'

/**
 * 用户偏好 API (C 端, 齿轮 - 偏好设置)。
 * <p>第一阶段实现 3 项: memoryFilePath / defaultModelCode / chatStyle, 其余预留。
 *
 * @author JC
 */

/** 偏好项默认值 */

/**
 * 记忆文件默认路径。
 * <p>注意: 此处仅作为"显示默认值"使用, 不参与运行时路径解析; 真实的 `~` 展开由后端在落盘时执行 (L2 阶段实施, 本期仅展示)。
 * <p>说明:
 * <ul>
 *   <li>`~` = 当前操作系统用户主目录 (Linux/macOS: `$HOME`, Windows: `%USERPROFILE%`)</li>
 *   <li>`.jc` = 隐藏目录, 项目所有本地数据统一存放于此 (类似 `.config` / `.cache`)</li>
 *   <li>`jc-rag/memory/` = 记忆模块专属子目录</li>
 *   <li>`user_profile.md` = Markdown 格式, 可用任意编辑器查看/编辑</li>
 * </ul>
 */
export const DEFAULT_MEMORY_FILE_PATH = '~/.jc/jc-rag/memory/user_profile.md'

/**
 * 用户可见的 `~` 含义提示文案 (在前端偏好设置弹窗中展示给用户)。
 * <p>与 DEFAULT_MEMORY_FILE_PATH 解耦: 后端在未来实施"路径展开"时, 不影响此处 UI 文案。
 */
export const MEMORY_FILE_PATH_HINT =
  '~ 表示当前用户的主目录 (Windows: C:\\Users\\你的用户名; macOS/Linux: /home/你的用户名); .jc 是项目统一隐藏目录; 系统会自动创建中间目录。'

export const DEFAULT_CHAT_STYLE = 'concise'
/** 空字符串表示"跟随系统默认" (与后端约定一致) */
export const DEFAULT_MODEL_CODE = ''

/** 回答风格可选值 (前端硬编码, 与后端默认对齐) */
export const CHAT_STYLE_OPTIONS: { value: string; label: string }[] = [
  { value: 'concise', label: '简洁' },
  { value: 'detailed', label: '详细' },
  { value: 'friendly', label: '友好' },
  { value: 'professional', label: '专业' },
]

/** 用户偏好 VO (与服务端 UserPreferenceVO 对齐) */
export interface UserPreferenceVO {
  memoryFilePath?: string
  defaultModelCode?: string
  chatStyle?: string
  /** L2 长期记忆开关: 缺省视为 true (后端默认开启) */
  enableLongMemory?: boolean
  /** L3 跨会话画像开关: 缺省视为 true (后端默认开启) */
  enableMemoryProfile?: boolean
}

/** 更新参数 (字段可空, 只更新非空项; 显式传空字符串可清空) */
export interface UserPreferenceUpdateParam {
  memoryFilePath?: string
  defaultModelCode?: string
  chatStyle?: string
  enableLongMemory?: boolean
  enableMemoryProfile?: boolean
}

export async function fetchUserPreference(): Promise<UserPreferenceVO> {
  const { data } = await request.get('/user/preference')
  return (data || {}) as UserPreferenceVO
}

export async function updateUserPreference(payload: UserPreferenceUpdateParam): Promise<UserPreferenceVO> {
  const { data } = await request.put('/user/preference', payload)
  return (data || {}) as UserPreferenceVO
}

/* ----------- 记忆文件 (Markdown) 读写 ----------- */

/** 记忆文件 VO (与服务端 MemoryFileVO 对齐) */
export interface MemoryFileVO {
  /** 用户填的原始路径 (含 ~) */
  rawPath?: string
  /** 后端展开后的真实磁盘路径 (含平台分隔符) */
  resolvedPath?: string
  /** 父目录 (展开后) */
  parentDir?: string
  /** 文件内容 (Markdown 文本) */
  content?: string
  /** 文件是否真实存在磁盘上 */
  exists?: boolean
  /** 字节数 (UTF-8 编码) */
  bytes?: number
  /** 最后修改时间 (ISO 字符串) */
  lastModified?: string
  /** 是否为后端自动写入的默认模板 (用户尚未编辑过) */
  autoInitialized?: boolean
}

/**
 * 读取记忆文件 (Markdown).
 * <p>文件不存在时, 后端会自动初始化默认模板并返回 ({@code autoInitialized=true}).
 */
export async function fetchMemoryFile(): Promise<MemoryFileVO> {
  const { data } = await request.get('/user/preference/memory-file')
  return (data || {}) as MemoryFileVO
}

/**
 * 保存记忆文件 (Markdown).
 * <p>写入磁盘真实路径 (路径来自当前用户偏好). 后端会自动创建父目录.
 */
export async function saveMemoryFile(content: string): Promise<MemoryFileVO> {
  const { data } = await request.put('/user/preference/memory-file', { content })
  return (data || {}) as MemoryFileVO
}

/* ----------- 被遗忘权 (GDPR / 个保法) ----------- */

/**
 * 一键忘记我: 物理删除本用户全部对话数据 (L0+L1+L2+L3+偏好+记忆文件).
 * <p>账号本身保留, 历史数据全清, 不可逆.
 * <p>响应字段: totalDeleted / chatMessageDeleted / chatSessionDeleted / chatSessionSummaryDeleted / userMemoryProfileDeleted / esMemoryDeleted / userPrefDeleted / memoryFileDeleted.
 */
export interface ForgetMeResult {
  userId?: number
  executedAt?: string
  sessionsFound?: number
  chatMessageDeleted?: number
  chatSessionDeleted?: number
  chatSessionSummaryDeleted?: number
  userMemoryProfileDeleted?: number
  esMemoryDeleted?: number
  userPrefDeleted?: number
  memoryFileDeleted?: number
  totalDeleted?: number
}

export async function forgetMe(): Promise<ForgetMeResult> {
  const { data } = await request.post('/user/preference/forget-me', {})
  return (data || {}) as ForgetMeResult
}
