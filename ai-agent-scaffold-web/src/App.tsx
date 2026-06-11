import { ChangeEvent, FormEvent, KeyboardEvent, PointerEvent, useEffect, useRef, useState } from 'react';
import { DrawIoEmbed, type DrawIoEmbedRef, type UrlParameters } from 'react-drawio';
import {
  ChevronLeft,
  ChevronRight,
  LoaderCircle,
  MousePointer2,
  Paperclip,
  X,
  Send
} from 'lucide-react';

const STORAGE_KEY = 'drawio-agent-current-xml';
const DEFAULT_AGENT_ID = 300000;
const VISION_AGENT_ID = 300001;
const DEFAULT_USER_ID = 'drawio-web-user';
const SHOW_API_DEBUG_PANEL = false;
const CHAT_PANEL_DEFAULT_WIDTH = 400;
const CHAT_PANEL_MIN_WIDTH = 360;
const CHAT_PANEL_MAX_WIDTH = 560;
const CHAT_PANEL_CLOSED_WIDTH = 46;
const DRAWIO_SUCCESS_MESSAGE = '\u56fe\u8868\u5df2\u6210\u529f\u751f\u6210\uff0c\u5e76\u540c\u6b65\u5230\u5f53\u524d\u753b\u5e03\u3002';
const IMAGE_PROMPT_TEXT = '\u8bf7\u6839\u636e\u8fd9\u5f20\u56fe\u7247\u751f\u6210 draw.io \u56fe';
const PENDING_STEPS = [
  '\u6b63\u5728\u7406\u89e3\u4f60\u7684\u9700\u6c42...',
  '\u6b63\u5728\u8ba9 Agent \u89c4\u5212\u8f93\u51fa...',
  '\u6b63\u5728\u7b49\u5f85\u6a21\u578b\u8fd4\u56de...',
  '\u5982\u679c\u751f\u6210\u56fe\u8868\uff0c\u5c06\u81ea\u52a8\u540c\u6b65\u5230\u5f53\u524d\u753b\u5e03...'
];
const LONG_PENDING_STEP = '\u590d\u6742\u56fe\u8868\u53ef\u80fd\u9700\u8981\u66f4\u4e45\uff0c\u4ecd\u5728\u5904\u7406\u4e2d...';

type ChatRole = 'user' | 'agent';

type ChatMessage = {
  id: string;
  role: ChatRole;
  content: string;
  type?: string;
  drawioSummary?: DrawioSummary;
};

type DrawioSummary = {
  vertexCount: number;
  edgeCount: number;
};

type XmlUndoState = {
  before: string;
  after: string;
  undone: boolean;
};

type ApiAgentReply = {
  type?: string;
  content?: unknown;
  sessionId?: string;
};

type ChatApiResponse = {
  code?: string;
  info?: string;
  data?: ApiAgentReply;
};

type AgentReply = {
  type?: string;
  content?: string;
  sessionId?: string;
};

type DrawioUrlParameters = UrlParameters & {
  format?: 0 | 1;
};

type ApiDebugEntry = {
  endpoint: string;
  status: number;
  ok: boolean;
  rawText: string;
  parsed?: unknown;
  error?: string;
  time: string;
};

const starterXml = `<mxfile host="embed.diagrams.net" type="embed">
  <diagram id="agent-flow" name="Agent Flow">
    <mxGraphModel dx="1100" dy="700" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="1169" pageHeight="827" math="0" shadow="0">
      <root>
        <mxCell id="0" />
        <mxCell id="1" parent="0" />
        <mxCell id="prompt" value="用户需求" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#e8f3ff;strokeColor=#2d6cdf;fontColor=#1f2a44;" vertex="1" parent="1">
          <mxGeometry x="120" y="170" width="150" height="64" as="geometry" />
        </mxCell>
        <mxCell id="agent" value="DrawIO Agent" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#fff7df;strokeColor=#c58a00;fontColor=#31270b;" vertex="1" parent="1">
          <mxGeometry x="360" y="170" width="160" height="64" as="geometry" />
        </mxCell>
        <mxCell id="diagram" value="图表 XML" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#eaf7ef;strokeColor=#2d8f53;fontColor=#16351f;" vertex="1" parent="1">
          <mxGeometry x="610" y="170" width="150" height="64" as="geometry" />
        </mxCell>
        <mxCell id="drawio" value="draw.io 画布" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#f3edff;strokeColor=#7a56c2;fontColor=#271b43;" vertex="1" parent="1">
          <mxGeometry x="850" y="170" width="160" height="64" as="geometry" />
        </mxCell>
        <mxCell id="edge-1" style="edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#667085;" edge="1" parent="1" source="prompt" target="agent">
          <mxGeometry relative="1" as="geometry" />
        </mxCell>
        <mxCell id="edge-2" style="edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#667085;" edge="1" parent="1" source="agent" target="diagram">
          <mxGeometry relative="1" as="geometry" />
        </mxCell>
        <mxCell id="edge-3" style="edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#667085;" edge="1" parent="1" source="diagram" target="drawio">
          <mxGeometry relative="1" as="geometry" />
        </mxCell>
      </root>
    </mxGraphModel>
  </diagram>
</mxfile>`;

function getInitialXml() {
  return localStorage.getItem(STORAGE_KEY) || starterXml;
}

function createMessage(role: ChatRole, content: string, type?: string, drawioSummary?: DrawioSummary): ChatMessage {
  return {
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    role,
    content,
    type,
    drawioSummary
  };
}

function parseDrawioSummary(content: string): DrawioSummary | undefined {
  try {
    const document = new DOMParser().parseFromString(content, 'application/xml');

    if (document.querySelector('parsererror')) {
      return undefined;
    }

    const cells = Array.from(document.querySelectorAll('mxCell'));

    if (cells.length === 0) {
      return undefined;
    }

    return {
      vertexCount: cells.filter((cell) => cell.getAttribute('vertex') === '1').length,
      edgeCount: cells.filter((cell) => cell.getAttribute('edge') === '1').length
    };
  } catch {
    return undefined;
  }
}

function fileToDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();

    reader.onload = () => resolve(String(reader.result));
    reader.onerror = reject;

    reader.readAsDataURL(file);
  });
}

async function readStreamText(response: Response) {
  if (!response.body) {
    throw new Error('浏览器不支持流式响应');
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let rawText = '';

  while (true) {
    const { value, done } = await reader.read();

    if (done) {
      break;
    }

    rawText += decoder.decode(value, { stream: true });
  }

  rawText += decoder.decode();
  return rawText;
}

function extractSseData(rawText: string) {
  const events = rawText.replace(/\r\n/g, '\n').split(/\n\s*\n/);
  const eventParts: string[] = [];

  for (const eventText of events) {
    const dataLines: string[] = [];

    for (const line of eventText.split('\n')) {
      if (line.startsWith('data:')) {
        dataLines.push(line.slice(5));
      } else if (
        line.trim() &&
        !line.startsWith('event:') &&
        !line.startsWith('id:') &&
        !line.startsWith('retry:')
      ) {
        dataLines.push(line);
      }
    }

    if (dataLines.length > 0) {
      eventParts.push(dataLines.join('\n'));
    }
  }

  return eventParts.join('').trim();
}

function extractDrawioXml(text: string) {
  const withoutFence = text
    .replace(/^```json\s*/i, '')
    .replace(/^```xml\s*/i, '')
    .replace(/^```\s*/i, '')
    .replace(/```\s*$/i, '')
    .trim();

  const start = withoutFence.indexOf('<mxfile');
  const end = withoutFence.indexOf('</mxfile>', start);

  if (start >= 0 && end > start) {
    return cleanDrawioXml(withoutFence.slice(start, end + '</mxfile>'.length));
  }

  return '';
}

function cleanDrawioXml(xml: string) {
  const cleaned = xml
    .replace(/\\"/g, '"')
    .replace(/\\n/g, '\n')
    .replace(/\\r/g, '\r')
    .replace(/\\t/g, '\t')
    .replace(/\\\//g, '/')
    .trim();

  return repairDrawioXml(cleaned);
}

function repairDrawioXml(xml: string) {
  let repaired = xml;

  if (repaired.startsWith('<mxfile>')) {
    repaired = repaired.replace('<mxfile>', '<mxfile host="embed.diagrams.net" type="embed">');
  }

  if (/<diagram\s*>/.test(repaired)) {
    repaired = repaired.replace(/<diagram\s*>/, '<diagram id="page-1" name="Page-1">');
  }

  if (/<mxGraphModel\s*>/.test(repaired)) {
    repaired = repaired.replace(
      /<mxGraphModel\s*>/,
      '<mxGraphModel dx="1100" dy="700" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="1169" pageHeight="827" math="0" shadow="0">'
    );
  }

  return repaired
    .replace(/<diagram\b([^>]*)>/, (_match, attrs: string) => {
      let nextAttrs = attrs;

      if (!/\bid=/.test(nextAttrs)) {
        nextAttrs = ` id="page-1"${nextAttrs}`;
      }

      if (!/\bname=/.test(nextAttrs)) {
        nextAttrs = `${nextAttrs} name="Page-1"`;
      }

      return `<diagram${nextAttrs}>`;
    })
    .trim();
}

function normalizeContent(content: unknown) {
  if (typeof content === 'string') {
    return content;
  }

  if (content == null) {
    return undefined;
  }

  try {
    return JSON.stringify(content);
  } catch {
    return String(content);
  }
}

function formatDebugValue(value: unknown) {
  if (typeof value === 'string') {
    return value;
  }

  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return String(value);
  }
}

function clampChatPanelWidth(width: number) {
  return Math.min(CHAT_PANEL_MAX_WIDTH, Math.max(CHAT_PANEL_MIN_WIDTH, width));
}

function normalizeAgentReply(reply: ApiAgentReply): AgentReply {
  const content = normalizeContent(reply.content);

  if (!content) {
    return {
      type: reply.type,
      sessionId: reply.sessionId
    };
  }

  const xml = extractDrawioXml(content);
  if (xml) {
    return {
      ...reply,
      type: 'drawio',
      content: xml
    };
  }

  return {
    type: reply.type,
    content,
    sessionId: reply.sessionId
  };
}

function parseAgentReply(streamText: string): AgentReply {
  const payload = extractSseData(streamText);

  if (!payload) {
    return { type: 'user', content: 'Agent 没有返回内容' };
  }

  const directXml = extractDrawioXml(payload);
  if (directXml) {
    return { type: 'drawio', content: directXml };
  }

  try {
    const parsed = JSON.parse(payload) as ChatApiResponse & AgentReply;
    if (parsed.data) {
      return normalizeAgentReply(parsed.data || { type: 'user', content: 'Agent 没有返回内容' });
    }
    return normalizeAgentReply({
      type: parsed.type,
      content: parsed.content,
      sessionId: parsed.sessionId
    });
  } catch {
    const fallbackXml = extractDrawioXml(payload);
    if (fallbackXml) {
      return { type: 'drawio', content: fallbackXml };
    }

    const start = payload.indexOf('{');
    const end = payload.lastIndexOf('}');

    if (start >= 0 && end > start) {
      const jsonText = payload.slice(start, end + 1);
      const parsed = JSON.parse(jsonText) as ChatApiResponse & AgentReply;
      if (parsed.data) {
        return normalizeAgentReply(parsed.data || { type: 'user', content: 'Agent 没有返回内容' });
      }
      const contentXml = parsed.content ? extractDrawioXml(parsed.content) : '';
      if (contentXml) {
        return { type: 'drawio', content: contentXml, sessionId: parsed.sessionId };
      }
      return normalizeAgentReply({
        type: parsed.type,
        content: parsed.content,
        sessionId: parsed.sessionId
      });
    }

    return { type: 'user', content: payload };
  }
}

function parseChatApiResponse(parsed: ChatApiResponse & ApiAgentReply): AgentReply {
  if (parsed.data) {
    return normalizeAgentReply(parsed.data);
  }

  return normalizeAgentReply(parsed);
}
export default function App() {
  const drawioRef = useRef<DrawIoEmbedRef>(null);
  const imageInputRef = useRef<HTMLInputElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const messageListRef = useRef<HTMLDivElement>(null);
  const xmlRef = useRef(getInitialXml());
  const [xml, setXml] = useState(getInitialXml);
  const [isChatOpen, setIsChatOpen] = useState(true);
  const [isSending, setIsSending] = useState(false);
  const [pendingStepIndex, setPendingStepIndex] = useState(0);
  const [isLongPending, setIsLongPending] = useState(false);
  const [inputValue, setInputValue] = useState('');
  const [selectedImageFile, setSelectedImageFile] = useState<File | null>(null);
  const [selectedImageName, setSelectedImageName] = useState('');
  const [sessionId, setSessionId] = useState<string>('');
  const [activeAgentId, setActiveAgentId] = useState(DEFAULT_AGENT_ID);
  const [chatPanelWidth, setChatPanelWidth] = useState(CHAT_PANEL_DEFAULT_WIDTH);
  const [xmlUndoState, setXmlUndoState] = useState<XmlUndoState | null>(null);
  const [lastApiResponse, setLastApiResponse] = useState<ApiDebugEntry | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([
    createMessage('agent', '你好，我是 DrawIO Agent。你可以告诉我想画什么图，例如“画一个订单支付流程图”。')
  ]);

  const resizeTextarea = (textarea: HTMLTextAreaElement) => {
    textarea.style.height = 'auto';
    textarea.style.height = `${Math.min(textarea.scrollHeight, 180)}px`;
  };

  const handleInputChange = (event: ChangeEvent<HTMLTextAreaElement>) => {
    setInputValue(event.target.value);
    resizeTextarea(event.target);
  };

  useEffect(() => {
    xmlRef.current = xml;
  }, [xml]);

  useEffect(() => {
    const container = messageListRef.current;

    if (!container) {
      return;
    }

    requestAnimationFrame(() => {
      container.scrollTop = container.scrollHeight;
    });
  }, [messages, isSending]);

  useEffect(() => {
    if (!isSending) {
      setPendingStepIndex(0);
      setIsLongPending(false);
      return;
    }

    const stepTimer = window.setInterval(() => {
      setPendingStepIndex((current) => Math.min(current + 1, PENDING_STEPS.length - 1));
    }, 7000);
    const longTimer = window.setTimeout(() => {
      setIsLongPending(true);
    }, 45000);

    return () => {
      window.clearInterval(stepTimer);
      window.clearTimeout(longTimer);
    };
  }, [isSending]);

  const syncXmlToCanvas = (nextXml: string) => {
    setXmlUndoState({
      before: xmlRef.current,
      after: nextXml,
      undone: false
    });
    xmlRef.current = nextXml;
    setXml(nextXml);
    localStorage.setItem(STORAGE_KEY, nextXml);
  };

  const undoLastXmlSync = () => {
    if (!xmlUndoState) {
      return;
    }

    const nextXml = xmlUndoState.undone ? xmlUndoState.after : xmlUndoState.before;
    xmlRef.current = nextXml;
    setXml(nextXml);
    localStorage.setItem(STORAGE_KEY, nextXml);
    setXmlUndoState({
      ...xmlUndoState,
      undone: !xmlUndoState.undone
    });
  };

  const sendMessage = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const text = inputValue.trim();

    if ((!text && !selectedImageFile) || isSending) {
      return;
    }

    const imageFile = selectedImageFile;
    const userMessage = imageFile
      ? `${text || '请根据这张图片生成 draw.io 图'}\n[图片：${imageFile.name}]`
      : text;

    setInputValue('');
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }
    setSelectedImageFile(null);
    setSelectedImageName('');
    if (imageInputRef.current) {
      imageInputRef.current.value = '';
    }
    setIsSending(true);
    setMessages((current) => [...current, createMessage('user', userMessage)]);

    try {
      const data = imageFile
        ? await sendImageAnalyzeRequest(text, imageFile)
        : await sendChatRequest(text);

      if (imageFile) {
        setActiveAgentId(VISION_AGENT_ID);
        setSessionId('');
      }

      const replyType = data?.type || 'user';
      const replyContent = data?.content || 'Agent 没有返回内容';

      if (data?.sessionId) {
        setSessionId(data.sessionId);
      }

      if (replyType === 'drawio') {
        const drawioSummary = parseDrawioSummary(replyContent);
        syncXmlToCanvas(replyContent);
        setMessages((current) => [
          ...current,
          createMessage('agent', DRAWIO_SUCCESS_MESSAGE, replyType, drawioSummary)
        ]);
      } else {
        setMessages((current) => [...current, createMessage('agent', replyContent, replyType)]);
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : '未知错误';
      setMessages((current) => [
        ...current,
        createMessage('agent', `请求失败：${message}。请确认后端服务已启动在 8091 端口。`, 'error')
      ]);
    } finally {
      setIsSending(false);
    }
  };

  const startChatPanelResize = (event: PointerEvent<HTMLDivElement>) => {
    if (!isChatOpen) {
      return;
    }

    event.preventDefault();
    const startX = event.clientX;
    const startWidth = chatPanelWidth;

    const handlePointerMove = (moveEvent: globalThis.PointerEvent) => {
      const nextWidth = startWidth + startX - moveEvent.clientX;
      setChatPanelWidth(clampChatPanelWidth(nextWidth));
    };

    const stopResize = () => {
      document.removeEventListener('pointermove', handlePointerMove);
      document.removeEventListener('pointerup', stopResize);
      document.body.classList.remove('is-resizing-chat-panel');
    };

    document.body.classList.add('is-resizing-chat-panel');
    document.addEventListener('pointermove', handlePointerMove);
    document.addEventListener('pointerup', stopResize);
  };

  const handleChatResizeKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') {
      return;
    }

    event.preventDefault();
    const delta = event.key === 'ArrowLeft' ? 20 : -20;
    setChatPanelWidth((current) => clampChatPanelWidth(current + delta));
  };

  const requestJson = async (endpoint: string, body: Record<string, unknown>) => {
    const response = await fetch(endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(body)
    });
    const rawText = await response.text();
    let parsed: unknown;
    let parseError = '';

    try {
      parsed = rawText ? JSON.parse(rawText) : {};
    } catch (error) {
      parseError = error instanceof Error ? error.message : 'Invalid JSON';
    }

    setLastApiResponse({
      endpoint,
      status: response.status,
      ok: response.ok,
      rawText,
      parsed,
      error: parseError || undefined,
      time: new Date().toLocaleTimeString()
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}${rawText ? `: ${rawText}` : ''}`);
    }

    if (parseError) {
      throw new Error(`响应不是 JSON: ${parseError}`);
    }

    return parsed as ChatApiResponse & ApiAgentReply;
  };

  const sendChatRequest = async (text: string) => {
    const parsed = await requestJson('/api/v1/chat', {
      agentId: activeAgentId,
      userId: DEFAULT_USER_ID,
      sessionId,
      message: text
    });
    return parseChatApiResponse(parsed);
  };

  const sendImageAnalyzeRequest = async (text: string, imageFile: File) => {
    const imageDataUrl = await fileToDataUrl(imageFile);
    const parsed = await requestJson('/api/v1/analyze_diagram_image', {
      agentId: VISION_AGENT_ID,
      userId: DEFAULT_USER_ID,
      sessionId: '',
      message: text || '请模拟这张图片，生成一张 draw.io 图。',
      imageDataUrl
    });
    return parseChatApiResponse(parsed);
  };

  return (
    <main
      className={`app-shell ${isChatOpen ? 'chat-open' : 'chat-closed'}`}
      style={{
        gridTemplateColumns: isChatOpen
          ? `minmax(0, 1fr) ${chatPanelWidth}px`
          : `minmax(0, 1fr) ${CHAT_PANEL_CLOSED_WIDTH}px`
      }}
    >
      <section className="editor-area">
        <DrawIoEmbed
          ref={drawioRef}
          autosave
          xml={xml}
          exportFormat="xmlsvg"
          urlParameters={({
            ui: 'simple',
            spin: true,
            libraries: false,
            saveAndExit: true,
            format: 0,
            lang: 'zh'
          } as DrawioUrlParameters)}
          configuration={{
            version: 'drawio-agent-clean-workspace-v1',
            override: true,
            defaultFonts: ['Inter', 'Arial', 'Microsoft YaHei'],
            sidebarWidth: 0,
            formatWidth: 0,
            customColorSchemes: [[null, { fill: '#e8f3ff', stroke: '#2d6cdf' }]],
            css: `
            `
          }}
          onLoad={(data) => {
            setXml(data.xml || xml);
          }}
          onAutoSave={(data) => {
            setXml(data.xml);
          }}
          onSave={(data) => {
            setXml(data.xml);
            localStorage.setItem(STORAGE_KEY, data.xml);
          }}
          onExport={(data) => {
            setXml(data.xml || data.data || xml);
          }}
        />
      </section>

      <aside className="chat-panel" aria-label="Agent 对话面板">
        {isChatOpen && (
          <div
            className="chat-resize-handle"
            role="separator"
            aria-label="调整对话面板宽度"
            aria-orientation="vertical"
            tabIndex={0}
            onPointerDown={startChatPanelResize}
            onKeyDown={handleChatResizeKeyDown}
            title="拖动调整对话面板宽度"
          />
        )}
        {!isChatOpen && (
        <button
          className="chat-toggle"
          onClick={() => setIsChatOpen((current) => !current)}
          title={isChatOpen ? '收起对话' : '展开对话'}
        >
          <ChevronLeft size={18} />
        </button>
        )}

        {isChatOpen && (
          <div className="chat-content">
            <header className="chat-header">
              <div className="chat-title-block">
                <h2>✨ Draw.io Agent</h2>
                <p>通过对话创建和修改图表</p>
                <div className="chat-connection-status">
                  <span aria-hidden="true" />
                  已连接当前画布
                </div>
              </div>
              <button
                type="button"
                className="chat-header-toggle"
                onClick={() => setIsChatOpen(false)}
                title="收起侧栏"
              >
                <ChevronRight size={18} />
              </button>
            </header>

            <div className="message-list" ref={messageListRef}>
              {messages.map((message) => (
                <div key={message.id} className={`message-row ${message.role}`}>
                  <div className="message-meta">
                    {message.role === 'user' ? '你' : '✨ Draw.io Agent'}
                  </div>
                  {message.type === 'drawio' ? (
                    <div className="message-bubble drawio-result-card">
                      {message.drawioSummary ? (
                        <>
                          <div>已生成 {message.drawioSummary.vertexCount} 个节点</div>
                          <div>已创建 {message.drawioSummary.edgeCount} 条连接线</div>
                          <div>已同步到当前画布</div>
                        </>
                      ) : (
                        <div>{message.content}</div>
                      )}                      <button
                        type="button"
                        className="undo-sync-button"
                        onClick={undoLastXmlSync}
                        disabled={!xmlUndoState}
                      >
                        {xmlUndoState?.undone ? '\u53d6\u6d88\u64a4\u9500' : '\u64a4\u9500\u672c\u6b21\u4fee\u6539'}
                      </button>
                    </div>
                  ) : (
                    <div className={`message-bubble ${message.type === 'error' ? 'error' : ''}`}>
                      {message.content}
                    </div>
                  )}
                </div>
              ))}              {isSending && (
                <div className="message-row agent">
                  <div className="message-meta">✨ Draw.io Agent</div>
                  <div className="message-bubble pending-card">
                    <div className="pending-card-title">
                      <LoaderCircle className="spin" size={16} />
                      <span>正在处理请求</span>
                    </div>
                    <div className="pending-card-step">
                      {isLongPending ? LONG_PENDING_STEP : PENDING_STEPS[pendingStepIndex]}
                    </div>
                    <div className="pending-card-note">
                      复杂图表通常需要 20～60 秒，完成后会自动更新结果。
                    </div>
                  </div>
                </div>
              )}            </div>

            {SHOW_API_DEBUG_PANEL && lastApiResponse && (
              <details className="api-debug-panel" open>
                <summary>
                  <span>接口响应</span>
                  <strong className={lastApiResponse.ok ? 'ok' : 'error'}>
                    {lastApiResponse.status}
                  </strong>
                </summary>
                <div className="api-debug-meta">
                  <span>{lastApiResponse.endpoint}</span>
                  <span>{lastApiResponse.time}</span>
                </div>
                {lastApiResponse.error && (
                  <div className="api-debug-error">{lastApiResponse.error}</div>
                )}
                <label>Parsed</label>
                <pre>{formatDebugValue(lastApiResponse.parsed)}</pre>
                <label>Raw</label>
                <pre>{lastApiResponse.rawText || '(empty)'}</pre>
              </details>
            )}

            <form className="chat-input-bar" onSubmit={sendMessage}>
              <div className="chat-composer">
                <textarea
                  ref={textareaRef}
                  value={inputValue}
                  onChange={handleInputChange}
                  placeholder="描述你想创建或修改的图表..."
                  rows={2}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' && !event.shiftKey) {
                      event.preventDefault();
                      event.currentTarget.form?.requestSubmit();
                    }
                  }}
                />
                {selectedImageName && (
                  <div className="image-attachment">
                    <span>{selectedImageName}</span>
                    <button
                      type="button"
                      className="image-remove-button"
                      onClick={() => {
                        setSelectedImageFile(null);
                        setSelectedImageName('');
                        if (imageInputRef.current) {
                          imageInputRef.current.value = '';
                        }
                      }}
                      title="移除图片"
                    >
                      <X size={14} />
                    </button>
                  </div>
                )}
                <div className="chat-composer-toolbar">
                  <div className="chat-composer-tools">
                    <button
                      type="button"
                      className="composer-tool-button"
                      onClick={() => imageInputRef.current?.click()}
                      title="上传图片"
                    >
                      <Paperclip size={16} />
                      <span>上传图片</span>
                    </button>
                    <button
                      type="button"
                      className="composer-tool-button"
                      disabled
                      title="暂未接入当前选中元素"
                    >
                      <MousePointer2 size={16} />
                      <span>选择元素</span>
                    </button>
                    <input
                      ref={imageInputRef}
                      className="image-file-input"
                      type="file"
                      accept="image/png,image/jpeg,image/webp"
                      onChange={(event) => {
                        const file = event.target.files?.[0];
                        setSelectedImageFile(file || null);
                        setSelectedImageName(file?.name || '');
                        if (file && !inputValue.trim()) {
                          setInputValue(IMAGE_PROMPT_TEXT);
                          requestAnimationFrame(() => {
                            if (textareaRef.current) {
                              resizeTextarea(textareaRef.current);
                            }
                          });
                        }
                      }}
                    />
                  </div>
                  <button
                    type="submit"
                    className="composer-send-button"
                    disabled={(!inputValue.trim() && !selectedImageFile) || isSending}
                    title="发送"
                  >
                    {isSending ? <LoaderCircle className="spin" size={18} /> : <Send size={18} />}
                  </button>
                </div>
              </div>
            </form>
          </div>
        )}
      </aside>
    </main>
  );
}
