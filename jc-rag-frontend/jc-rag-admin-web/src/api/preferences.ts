import { request } from './http'

/**
 * 用户偏好 API (齿轮 - 偏好设置)。
 * <p>第一阶段仅实现: memoryFilePath
 *
 * @author JC
 */

/** 偏好项默认值 */
export const DEFAULT_MEMORY_FILE_PATH = '~/.jc/jc-rag/memory/user_profile.md'

/** 用户偏好 VO (与服务端 UserPreferenceVO 对齐) */
export interface UserPreferenceVO {
  memoryFilePath?: string
}

/** 更新参数 (字段可空, 只更新非空项) */
export interface UserPreferenceUpdateParam {
  memoryFilePath?: string
}

export async function fetchUserPreference(): Promise<UserPreferenceVO> {
  const { data } = await request.get('/sys/user-preference')
  return (data || {}) as UserPreferenceVO
}

export async function updateUserPreference(payload: UserPreferenceUpdateParam): Promise<UserPreferenceVO> {
  const { data } = await request.put('/sys/user-preference', payload)
  return (data || {}) as UserPreferenceVO
}
