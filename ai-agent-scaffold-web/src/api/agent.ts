export interface AgentConfig {
  agentId: string
  agentName: string
  agentDesc: string
}

export interface ApiResponse<T> {
  code: string
  info: string
  data?: T
}

export interface CreateSessionResult {
  sessionId: string
}

export interface ChatRequest {
  agentId: string
  userId: string
  sessionId: string
  message: string
}

export async function queryAgents() {
  const response = await fetch('/api/v1/query_ai_agent_config_list')
  const result = await response.json() as ApiResponse<AgentConfig[]>
  return result.data ?? []
}

export async function createSession(agentId: string, userId: string) {
  const response = await fetch('/api/v1/create_session', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ agentId, userId })
  })
  const result = await response.json() as ApiResponse<CreateSessionResult>
  if (!result.data?.sessionId) {
    throw new Error(result.info || '创建会话失败')
  }
  return result.data.sessionId
}

export async function sendChat(request: ChatRequest) {
  const response = await fetch('/api/v1/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(request)
  })
  const result = await response.json() as ApiResponse<{ content: string }>
  return result.data?.content ?? ''
}

export async function streamChat(request: ChatRequest, onChunk: (chunk: string) => void) {
  const response = await fetch('/api/v1/chat_stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream'
    },
    body: JSON.stringify(request)
  })

  if (!response.ok || !response.body) {
    throw new Error(`流式请求失败：${response.status}`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  const readEventData = (eventText: string) => {
    return eventText
      .split('\n')
      .filter((line) => line.startsWith('data:'))
      .map((line) => line.replace(/^data:\s?/, ''))
      .join('\n')
  }

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')
    const events = buffer.split('\n\n')
    buffer = events.pop() ?? ''

    for (const event of events) {
      const data = readEventData(event)
      if (data) {
        onChunk(data)
      }
    }
  }

  const rest = buffer.trim()
  if (rest.startsWith('data:')) {
    const data = readEventData(rest)
    if (data) {
      onChunk(data)
    }
  }
}
