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
