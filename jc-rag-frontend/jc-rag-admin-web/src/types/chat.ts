export interface ChatSessionItem {
  id: number
  sessionTitle: string
  sessionMode: string
  modelMode: string
  modelName: string
  kbId: number
  kbName?: string
  createdAt: string
  updatedAt: string
}

export interface ChatMessageItem {
  id: number
  roleType: string
  messageContent: string
  messageIndex: number
  tokenCount: number
  latencyMs: number
  createdAt: string
}

export interface ChatAskResponse {
  userMessageId: number
  assistantMessageId: number
  answer: string
  references: Array<{
    documentId: string
    chunkId: string
    pageNo: number
    sectionTitle: string
    score: number
    quoteText: string
  }>
}
