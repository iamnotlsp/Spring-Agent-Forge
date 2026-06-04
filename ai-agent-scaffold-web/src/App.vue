<template>
  <main class="shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="mark">AS</div>
        <div>
          <h1>AI Agent Scaffold</h1>
          <p>智能体对话台</p>
        </div>
      </div>

      <section class="panel">
        <div class="panel-title">
          <span>智能体</span>
          <button class="ghost-button" type="button" @click="loadAgents" :disabled="loadingAgents">
            刷新
          </button>
        </div>

        <div class="agent-list">
          <button
            v-for="agent in agents"
            :key="agent.agentId"
            class="agent-item"
            :class="{ active: selectedAgent?.agentId === agent.agentId }"
            type="button"
            @click="selectAgent(agent)"
          >
            <strong>{{ agent.agentName || agent.agentId }}</strong>
            <span>{{ agent.agentDesc || '暂无描述' }}</span>
            <small>ID {{ agent.agentId }}</small>
          </button>
        </div>

        <p v-if="!agents.length && !loadingAgents" class="empty-text">未读取到智能体配置</p>
      </section>

      <section class="panel">
        <label class="field-label" for="userId">用户 ID</label>
        <input id="userId" v-model.trim="userId" class="input" />
        <p class="hint-text">发送消息时会自动创建会话，并使用流式响应。</p>
      </section>
    </aside>

    <section class="chat">
      <header class="chat-header">
        <div>
          <p class="eyebrow">当前智能体</p>
          <h2>{{ selectedAgent?.agentName || '请选择一个智能体' }}</h2>
        </div>
        <div class="stream-badge">流式输出</div>
      </header>

      <div class="messages">
        <article v-for="message in messages" :key="message.id" class="message" :class="message.role">
          <div class="message-meta">{{ message.role === 'user' ? '你' : selectedAgent?.agentName || 'Agent' }}</div>
          <pre>{{ message.content }}</pre>
        </article>

        <div v-if="!messages.length" class="welcome">
          <h3>开始一次智能体对话</h3>
          <p>选择智能体后直接输入消息，系统会自动创建会话并流式返回结果。</p>
        </div>
      </div>

      <form class="composer" @submit.prevent="submitMessage">
        <textarea
          v-model.trim="draft"
          placeholder="输入消息，例如：你是什么东西"
          rows="3"
          @keydown.enter.exact.prevent="submitMessage"
        />
        <button class="primary-button" type="submit" :disabled="!canSend || sending">
          {{ sending ? '发送中' : '发送' }}
        </button>
      </form>

      <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { createSession, queryAgents, streamChat, type AgentConfig } from './api/agent'

type Message = {
  id: number
  role: 'user' | 'agent'
  content: string
}

const agents = ref<AgentConfig[]>([])
const selectedAgent = ref<AgentConfig | null>(null)
const userId = ref('lsp')
const draft = ref('')
const messages = ref<Message[]>([])
const loadingAgents = ref(false)
const sending = ref(false)
const errorMessage = ref('')
let messageId = 1

const canSend = computed(() => {
  return Boolean(selectedAgent.value && userId.value && draft.value)
})

const sleep = (ms: number) => new Promise((resolve) => window.setTimeout(resolve, ms))

async function loadAgents() {
  loadingAgents.value = true
  errorMessage.value = ''
  try {
    agents.value = await queryAgents()
    if (!selectedAgent.value && agents.value.length) {
      selectedAgent.value = agents.value[0]
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '智能体列表加载失败'
  } finally {
    loadingAgents.value = false
  }
}

function selectAgent(agent: AgentConfig) {
  selectedAgent.value = agent
  messages.value = []
}

async function submitMessage() {
  if (!canSend.value || !selectedAgent.value) return

  const content = draft.value
  draft.value = ''
  errorMessage.value = ''
  sending.value = true

  messages.value.push({
    id: messageId++,
    role: 'user',
    content
  })

  const agentMessage: Message = {
    id: messageId++,
    role: 'agent',
    content: ''
  }
  messages.value.push(agentMessage)
  const agentMessageIndex = messages.value.length - 1

  const updateAgentMessage = (content: string) => {
    const current = messages.value[agentMessageIndex]
    if (!current) return

    messages.value[agentMessageIndex] = {
      ...current,
      content
    }
  }

  const appendAgentMessage = (chunk: string) => {
    const current = messages.value[agentMessageIndex]
    if (!current) return

    messages.value[agentMessageIndex] = {
      ...current,
      content: current.content + chunk
    }
  }

  try {
    const sessionId = await createSession(selectedAgent.value.agentId, userId.value)
    const queue: string[] = []
    let typing = false

    const drainQueue = async () => {
      if (typing) return

      typing = true
      while (queue.length) {
        appendAgentMessage(queue.shift() ?? '')
        await sleep(18)
      }
      typing = false
    }

    await streamChat(
      {
        agentId: selectedAgent.value.agentId,
        userId: userId.value,
        sessionId,
        message: content
      },
      (chunk) => {
        queue.push(...Array.from(chunk))
        void drainQueue()
      }
    )

    while (typing || queue.length) {
      await sleep(18)
    }
  } catch (error) {
    updateAgentMessage('')
    errorMessage.value = error instanceof Error ? error.message : '发送失败'
  } finally {
    sending.value = false
  }
}

onMounted(loadAgents)
</script>
