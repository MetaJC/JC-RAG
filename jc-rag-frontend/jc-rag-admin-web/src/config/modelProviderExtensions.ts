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
