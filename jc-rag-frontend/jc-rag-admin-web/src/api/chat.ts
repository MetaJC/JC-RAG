import axios from 'axios'

import type { ChatAskResponse, ChatMessageItem, ChatSessionItem } from '../types/chat'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api'

const request = axios.create({
  baseURL: apiBaseUrl,
  timeout: 60000,
  withCredentials: true,
})

export async function fetchChatSessions(params?: { page?: number; size?: number; keyword?: string }) {
  const { data } = await request.get('/chat/sessions', { params })
  return data
}

export async function createChatSession(sessionTitle: string, kbId: number) {
  const { data } = await request.post<ChatSessionItem>('/chat/sessions', {
    sessionTitle,
    kbId,
  })
  return data
}

export async function fetchChatMessages(sessionId: number) {
  const { data } = await request.get<ChatMessageItem[]>(`/chat/sessions/${sessionId}/messages`)
  return data
}

export async function askChatQuestion(sessionId: number, question: string) {
  const { data } = await request.post<ChatAskResponse>(`/chat/sessions/${sessionId}/ask`, {
    question,
  })
  return data
}

export async function askChatQuestionStream(
  sessionId: number,
  question: string,
  handlers: {
    onMeta?: (data: any) => void
    onDelta?: (delta: string) => void
    onDone?: (data?: any) => void
  },
) {
  const response = await fetch(`${apiBaseUrl}/chat/sessions/${sessionId}/ask/stream`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
    credentials: 'include',
    body: JSON.stringify({ question }),
  })
  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || `HTTP ${response.status}`)
  }
  if (!response.body) {
    throw new Error('stream not supported')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let doneReceived = false

  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })

    while (true) {
      const lfSep = buffer.indexOf('\n\n')
      const crlfSep = buffer.indexOf('\r\n\r\n')
      let sepIndex = -1
      let sepLen = 0
      if (crlfSep >= 0 && (lfSep < 0 || crlfSep < lfSep)) {
        sepIndex = crlfSep
        sepLen = 4
      } else if (lfSep >= 0) {
        sepIndex = lfSep
        sepLen = 2
      }
      if (sepIndex < 0) break
      const block = buffer.slice(0, sepIndex)
      buffer = buffer.slice(sepIndex + sepLen)

      let eventName = ''
      let dataPayload = ''
      for (const rawLine of block.split(/\r?\n/)) {
        const line = rawLine.trim()
        if (!line) continue
        if (line.startsWith('event:')) {
          eventName = line.substring('event:'.length).trim()
          continue
        }
        if (line.startsWith('data:')) {
          dataPayload += line.substring('data:'.length).trim()
        }
      }
      if (!dataPayload) continue

      let data: any = null
      try {
        data = JSON.parse(dataPayload)
      } catch {
        data = dataPayload
      }

      if (eventName === 'meta') {
        handlers.onMeta?.(data)
        continue
      }
      if (eventName === 'delta') {
        handlers.onDelta?.(typeof data?.delta === 'string' ? data.delta : String(data))
        continue
      }
      if (eventName === 'done') {
        handlers.onDone?.(data)
        doneReceived = true
        return
      }
    }
  }

  if (!doneReceived) {
    handlers.onDone?.({ done: true, abnormalEnd: true })
  }
}
