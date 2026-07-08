import { ChangeEvent, FormEvent, KeyboardEvent, PointerEvent, useEffect, useRef, useState } from 'react';
import { DrawIoEmbed, type DrawIoEmbedRef, type UrlParameters } from 'react-drawio';
import {
  ChevronLeft,
  ChevronRight,
  Coins,
  LoaderCircle,
  LogIn,
  MousePointer2,
  Paperclip,
  Play,
  Receipt,
  RefreshCw,
  X,
  Send,
  UserPlus
} from 'lucide-react';

const STORAGE_KEY = 'drawio-agent-current-xml';
const CHAT_MESSAGES_STORAGE_KEY = 'drawio-agent-chat-messages';
const CHAT_SESSION_STORAGE_KEY = 'drawio-agent-chat-session';
const XML_UNDO_STORAGE_KEY = 'drawio-agent-xml-undo';
const DEMO_SESSION_KEY = 'drawio-agent-demo-session';
const CREDIT_STORAGE_KEY = 'drawio-agent-demo-credits';
const USER_ID_STORAGE_KEY = 'drawio-agent-demo-user-id';
const TEST_GROUP_STORAGE_KEY = 'drawio-agent-demo-test-group';
const DEFAULT_CREDITS = 300;
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
const CREDIT_PACKAGES = [
  { credits: 100, price: '￥9.9', originalPrice: '￥12.9', saving: '￥3', desc: '轻量体验' },
  { credits: 500, price: '￥39.9', originalPrice: '￥59.9', saving: '￥20', desc: '常用推荐', badge: '推荐' },
  { credits: 1200, price: '￥89.9', originalPrice: '￥140.75', saving: '￥50.85', desc: '单价更低', badge: '最划算' }
];
const CREDIT_GOODS_IDS = ['9890002', '9890003', '9890004'];
const CREDIT_SOURCE = 's01';
const CREDIT_CHANNEL = 'c01';
const ORDER_PAGE_SIZE = 5;
const MAX_CHAT_MESSAGES = 100;
const WELCOME_MESSAGE = '你好，我是 DrawIO Agent。你可以告诉我想画什么图，例如“画一个订单支付流程图”。';

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
  requestId?: string;
  durationMs?: number;
  costCredits?: number;
  remainingCredits?: number;
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
  requestId?: string;
  durationMs?: number;
  costCredits?: number;
  remainingCredits?: number;
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

type CreditPackage = {
  credits: number;
  price: string;
  originalPrice: string;
  saving: string;
  desc: string;
  badge?: string;
  goodsId?: string;
  goodsName?: string;
  activityId?: number;
  teamList?: GroupBuyTeam[];
  teamStatistic?: GroupBuyTeamStatistic;
};

type GroupBuyTeam = {
  userId?: string;
  teamId?: string;
  activityId?: number;
  targetCount?: number;
  completeCount?: number;
  lockCount?: number;
  validTimeCountdown?: string;
};

type GroupBuyTeamStatistic = {
  allTeamCount?: number;
  allTeamCompleteCount?: number;
  allTeamUserCount?: number;
};

type GroupBuyMarketConfigResponse = {
  code?: string;
  info?: string;
  data?: {
    activityId?: number;
    goods?: {
      goodsId?: string;
      goodsName?: string;
      originalPrice?: number;
      deductionPrice?: number;
      discountPrice?: number;
      payPrice?: number;
    };
    teamList?: GroupBuyTeam[];
    teamStatistic?: GroupBuyTeamStatistic;
  };
};

type LockedPayOrder = {
  orderId?: string;
  originalPrice?: number;
  deductionPrice?: number;
  payPrice?: number;
  tradeOrderStatus?: number;
  goodsId?: string;
  goodsName?: string;
  activityId?: number;
  teamId?: string;
  targetCount?: number;
  joinedCount?: number;
  purchaseType?: 'group_buy' | 'direct';
  outTradeNo: string;
  credits: number;
};

type GroupBuyOrder = LockedPayOrder & {
  paymentMethod?: '微信支付' | '支付宝支付';
  purchaseType?: 'group_buy' | 'direct';
  paidAt?: string;
  grantTime?: string;
  createTime?: string;
  updateTime?: string;
  status: 'waiting' | 'formed' | 'granted' | 'failed';
  statusCode?: string;
  statusTitle: string;
  statusText: string;
};

type CurrentTestGroup = {
  teamId: string;
  goodsId: string;
  activityId?: number;
  targetCount: number;
  joinedCount: number;
};

type LockMarketPayOrderResponse = {
  code?: string;
  info?: string;
  data?: Omit<LockedPayOrder, 'outTradeNo' | 'credits'>;
};

type SettlementMarketPayOrderResponse = {
  code?: string;
  info?: string;
  data?: {
    userId?: string;
    teamId?: string;
    activityId?: number;
    outTradeNo?: string;
  };
};

type CreateCreditOrderResponse = {
  code?: string;
  info?: string;
  data?: boolean;
};

type PurchaseCreditOrderResponse = {
  code?: string;
  info?: string;
  data?: CreditOrderResponse | null;
};

type CreditOrderResponse = {
  userId?: string;
  teamId?: string;
  orderId?: string;
  outTradeNo?: string;
  goodsId?: string;
  goodsName?: string;
  credits?: number | string;
  payPrice?: number | string;
  status?: string;
  statusInfo?: string;
  paidTime?: string;
  grantTime?: string;
  createTime?: string;
  updateTime?: string;
  targetCount?: number | string;
  joinedCount?: number | string;
  teamStatus?: number | string;
};

type QueryCreditOrderListResponse = {
  code?: string;
  info?: string;
  data?: CreditOrderResponse[];
};

type QueryCreditOrderResponse = {
  code?: string;
  info?: string;
  data?: CreditOrderResponse | null;
};

type CreditAccountResponse = {
  userId?: string;
  availableCredits?: number | string;
  frozenCredits?: number | string;
  totalGrantedCredits?: number | string;
  totalUsedCredits?: number | string;
  status?: string;
  createTime?: string;
  updateTime?: string;
};

type QueryCreditAccountResponse = {
  code?: string;
  info?: string;
  data?: CreditAccountResponse | null;
};

function formatCurrency(value?: number) {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    return '￥0';
  }

  return `￥${value.toFixed(2).replace(/\.00$/, '').replace(/(\.\d)0$/, '$1')}`;
}

function parseCurrencyAmount(value?: string) {
  if (!value) {
    return 0;
  }

  const parsed = Number(value.replace(/[^\d.]/g, ''));
  return Number.isFinite(parsed) ? parsed : 0;
}

function formatOrderTime(value?: string) {
  if (!value) {
    return '';
  }

  const normalizedValue = value.includes('T') ? value : value.replace(' ', 'T');
  const date = new Date(normalizedValue);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  const pad = (nextValue: number) => String(nextValue).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function getGroupJoinedCount(team?: GroupBuyTeam) {
  if (!team) {
    return 0;
  }

  return team.completeCount || 0;
}

function getGroupOccupiedCount(team?: GroupBuyTeam) {
  if (!team) {
    return 0;
  }

  return Math.max(team.lockCount || 0, team.completeCount || 0);
}

function getGroupTargetCount(team?: GroupBuyTeam, fallback = 3) {
  return Math.max(1, team?.targetCount || fallback);
}

function toNumber(value: number | string | undefined, fallback = 0) {
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : fallback;
  }

  if (typeof value === 'string') {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : fallback;
  }

  return fallback;
}

function resolveCreditOrderStatus(status?: string, statusInfo?: string) {
  const statusTextMap: Record<string, { status: GroupBuyOrder['status']; title: string; text: string }> = {
    WAIT_GROUP: {
      status: 'waiting',
      title: '待成团',
      text: '已支付，等待拼团成团'
    },
    GROUP_SUCCESS: {
      status: 'formed',
      title: '已成团',
      text: '拼团已满，等待后端发放额度'
    },
    CREDIT_GRANTED: {
      status: 'granted',
      title: '已到账',
      text: '额度已发放'
    },
    GRANT_FAILED: {
      status: 'failed',
      title: '发放失败',
      text: '额度发放失败，等待重试'
    }
  };

  return statusTextMap[status || ''] || {
    status: 'waiting' as const,
    title: '处理中',
    text: statusInfo || '订单处理中'
  };
}

function getOrderStatusDescription(order: GroupBuyOrder) {
  if (order.status === 'granted') {
    return '拼团已完成，额度已到账。';
  }

  if (order.status === 'failed') {
    return '拼团已成，但额度发放失败，需要等待 MQ 重试或人工补偿。';
  }

  if (order.status === 'formed') {
    return '拼团人数已满，后端会通过 MQ 消息触发额度发放。';
  }

  return '支付已完成，当前订单正在等待其他成员参团。拼团满员前不会发放额度。';
}

function isGroupTeamOpen(team?: GroupBuyTeam) {
  if (!team?.teamId) {
    return false;
  }

  return getGroupOccupiedCount(team) < getGroupTargetCount(team);
}

function isJoinableGroupTeam(team: GroupBuyTeam | undefined, userId: string) {
  if (!isGroupTeamOpen(team)) {
    return false;
  }

  return !team?.userId || team.userId !== userId;
}

function getJoinableGroupTeams(teamList: GroupBuyTeam[] | undefined, userId: string) {
  const teamMap = new Map<string, GroupBuyTeam>();

  (teamList || []).forEach((team) => {
    if (!team.teamId) {
      return;
    }

    const currentTeam = teamMap.get(team.teamId);

    if (!currentTeam) {
      teamMap.set(team.teamId, team);
      return;
    }

    teamMap.set(team.teamId, {
      ...currentTeam,
      ...team,
      userId: currentTeam.userId || team.userId,
      targetCount: Math.max(currentTeam.targetCount || 0, team.targetCount || 0) || currentTeam.targetCount || team.targetCount,
      completeCount: Math.max(currentTeam.completeCount || 0, team.completeCount || 0),
      lockCount: Math.max(currentTeam.lockCount || 0, team.lockCount || 0)
    });
  });

  return Array.from(teamMap.values()).filter((team) => isJoinableGroupTeam(team, userId));
}

function getInitialTestGroup(): CurrentTestGroup | null {
  const stored = localStorage.getItem(TEST_GROUP_STORAGE_KEY);

  if (!stored) {
    return null;
  }

  try {
    const parsed = JSON.parse(stored) as CurrentTestGroup;

    if (!parsed.teamId || !parsed.goodsId || parsed.joinedCount >= parsed.targetCount) {
      return null;
    }

    return parsed;
  } catch {
    return null;
  }
}

async function parseJsonResponse<T>(response: Response) {
  const rawText = await response.text();

  if (!rawText.trim()) {
    throw new Error(`接口无响应内容（HTTP ${response.status}）`);
  }

  try {
    return JSON.parse(rawText) as T;
  } catch {
    throw new Error(`接口返回不是合法 JSON（HTTP ${response.status}）`);
  }
}

function resolveCreditPackageMeta(goodsId?: string) {
  if (goodsId === '9890003') {
    return { credits: 500, desc: '常用推荐', badge: '推荐' };
  }

  if (goodsId === '9890004') {
    return { credits: 1000, desc: '单价更低', badge: '最划算' };
  }

  return { credits: 100, desc: '轻量体验' };
}

function buildCreditPackageFromConfig(data: NonNullable<GroupBuyMarketConfigResponse['data']>): CreditPackage {
  const goods = data.goods || {};
  const originalPrice = goods.originalPrice ?? 0;
  const payPrice = goods.payPrice ?? originalPrice;
  const discountPrice = goods.deductionPrice ?? goods.discountPrice ?? Math.max(0, originalPrice - payPrice);
  const meta = resolveCreditPackageMeta(goods.goodsId);

  return {
    credits: meta.credits,
    price: formatCurrency(payPrice),
    originalPrice: formatCurrency(originalPrice),
    saving: formatCurrency(discountPrice),
    desc: meta.desc,
    badge: meta.badge,
    goodsId: goods.goodsId,
    goodsName: goods.goodsName,
    activityId: data.activityId,
    teamList: data.teamList || [],
    teamStatistic: data.teamStatistic || {}
  };
}

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
  const userId = localStorage.getItem(USER_ID_STORAGE_KEY) || DEFAULT_USER_ID;
  return localStorage.getItem(getUserXmlStorageKey(userId)) || localStorage.getItem(STORAGE_KEY) || starterXml;
}

function getUserXmlStorageKey(userId: string) {
  return `${STORAGE_KEY}:${userId || DEFAULT_USER_ID}`;
}

function getUserMessagesStorageKey(userId: string) {
  return `${CHAT_MESSAGES_STORAGE_KEY}:${userId || DEFAULT_USER_ID}`;
}

function getUserSessionStorageKey(userId: string) {
  return `${CHAT_SESSION_STORAGE_KEY}:${userId || DEFAULT_USER_ID}`;
}

function getUserXmlUndoStorageKey(userId: string) {
  return `${XML_UNDO_STORAGE_KEY}:${userId || DEFAULT_USER_ID}`;
}

function getInitialCredits() {
  const stored = Number(localStorage.getItem(CREDIT_STORAGE_KEY));
  return Number.isFinite(stored) && stored >= 0 ? stored : DEFAULT_CREDITS;
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

function createRequestId() {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID().replace(/-/g, '');
  }

  return `${Date.now()}${Math.random().toString(16).slice(2)}`;
}

function createWelcomeMessages() {
  return [createMessage('agent', WELCOME_MESSAGE)];
}

function limitChatMessages(nextMessages: ChatMessage[]) {
  return nextMessages.slice(-MAX_CHAT_MESSAGES);
}

function normalizeStoredMessages(value: unknown): ChatMessage[] | null {
  if (!Array.isArray(value)) {
    return null;
  }

  const messages = value.filter((item): item is ChatMessage => (
    Boolean(item)
      && typeof item === 'object'
      && typeof (item as ChatMessage).id === 'string'
      && ((item as ChatMessage).role === 'user' || (item as ChatMessage).role === 'agent')
      && typeof (item as ChatMessage).content === 'string'
  ));

  return messages.length > 0 ? limitChatMessages(messages) : null;
}

function getStoredMessagesForUser(userId: string) {
  const stored = localStorage.getItem(getUserMessagesStorageKey(userId));

  if (!stored) {
    return createWelcomeMessages();
  }

  try {
    return normalizeStoredMessages(JSON.parse(stored)) || createWelcomeMessages();
  } catch {
    return createWelcomeMessages();
  }
}

function saveStoredMessagesForUser(userId: string, nextMessages: ChatMessage[]) {
  localStorage.setItem(getUserMessagesStorageKey(userId), JSON.stringify(limitChatMessages(nextMessages)));
}

function getInitialMessages() {
  const userId = localStorage.getItem(USER_ID_STORAGE_KEY) || DEFAULT_USER_ID;
  return getStoredMessagesForUser(userId);
}

function getStoredSessionForUser(userId: string) {
  return localStorage.getItem(getUserSessionStorageKey(userId)) || '';
}

function getInitialSessionId() {
  const userId = localStorage.getItem(USER_ID_STORAGE_KEY) || DEFAULT_USER_ID;
  return getStoredSessionForUser(userId);
}

function getStoredXmlUndoStateForUser(userId: string): XmlUndoState | null {
  const stored = localStorage.getItem(getUserXmlUndoStorageKey(userId));

  if (!stored) {
    return null;
  }

  try {
    const parsed = JSON.parse(stored) as XmlUndoState;
    if (
      typeof parsed?.before === 'string'
      && typeof parsed?.after === 'string'
      && typeof parsed?.undone === 'boolean'
    ) {
      return parsed;
    }
  } catch {
    return null;
  }

  return null;
}

function getInitialXmlUndoState() {
  const userId = localStorage.getItem(USER_ID_STORAGE_KEY) || DEFAULT_USER_ID;
  return getStoredXmlUndoStateForUser(userId);
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
      sessionId: reply.sessionId,
      requestId: reply.requestId,
      durationMs: reply.durationMs,
      costCredits: reply.costCredits,
      remainingCredits: reply.remainingCredits
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
    sessionId: reply.sessionId,
    requestId: reply.requestId,
    durationMs: reply.durationMs,
    costCredits: reply.costCredits,
    remainingCredits: reply.remainingCredits
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
  const currentUserIdRef = useRef(localStorage.getItem(USER_ID_STORAGE_KEY) || DEFAULT_USER_ID);
  const creditsRef = useRef(getInitialCredits());
  const [xml, setXml] = useState(getInitialXml);
  const [isChatOpen, setIsChatOpen] = useState(true);
  const [isSending, setIsSending] = useState(false);
  const [sendingUserId, setSendingUserId] = useState('');
  const [pendingStepIndex, setPendingStepIndex] = useState(0);
  const [isLongPending, setIsLongPending] = useState(false);
  const [inputValue, setInputValue] = useState('');
  const [selectedImageFile, setSelectedImageFile] = useState<File | null>(null);
  const [selectedImageName, setSelectedImageName] = useState('');
  const [sessionId, setSessionId] = useState<string>(getInitialSessionId);
  const [activeAgentId, setActiveAgentId] = useState(DEFAULT_AGENT_ID);
  const [authMode, setAuthMode] = useState<'login' | 'register'>('login');
  const [isDemoAuthed, setIsDemoAuthed] = useState(() => sessionStorage.getItem(DEMO_SESSION_KEY) === '1');
  const [saveStatus, setSaveStatus] = useState('');
  const [credits, setCredits] = useState(getInitialCredits);
  const [currentUserId, setCurrentUserId] = useState(() => localStorage.getItem(USER_ID_STORAGE_KEY) || DEFAULT_USER_ID);
  const [lastCreditCost, setLastCreditCost] = useState(0);
  const [activePanelView, setActivePanelView] = useState<'chat' | 'orders'>('chat');
  const [isCreditPanelOpen, setIsCreditPanelOpen] = useState(false);
  const [creditPackages, setCreditPackages] = useState<CreditPackage[]>(CREDIT_PACKAGES);
  const [selectedCreditPackage, setSelectedCreditPackage] = useState<CreditPackage>(CREDIT_PACKAGES[1]);
  const [isCreditGoodsLoading, setIsCreditGoodsLoading] = useState(false);
  const [creditGoodsError, setCreditGoodsError] = useState('');
  const [isLockingOrder, setIsLockingOrder] = useState(false);
  const [lockOrderError, setLockOrderError] = useState('');
  const [lockedPayOrder, setLockedPayOrder] = useState<LockedPayOrder | null>(null);
  const [selectedGroupTeamId, setSelectedGroupTeamId] = useState('');
  const [isSettlingOrder, setIsSettlingOrder] = useState(false);
  const [settlementError, setSettlementError] = useState('');
  const [groupBuyOrders, setGroupBuyOrders] = useState<GroupBuyOrder[]>([]);
  const [orderPage, setOrderPage] = useState(1);
  const [isCreditDataLoading, setIsCreditDataLoading] = useState(false);
  const [creditDataError, setCreditDataError] = useState('');
  const [currentTestGroup, setCurrentTestGroup] = useState<CurrentTestGroup | null>(getInitialTestGroup);
  const [refreshingOrderNo, setRefreshingOrderNo] = useState('');
  const [orderStatusError, setOrderStatusError] = useState<{ outTradeNo: string; message: string } | null>(null);
  const [chatPanelWidth, setChatPanelWidth] = useState(CHAT_PANEL_DEFAULT_WIDTH);
  const [xmlUndoState, setXmlUndoState] = useState<XmlUndoState | null>(getInitialXmlUndoState);
  const [lastApiResponse, setLastApiResponse] = useState<ApiDebugEntry | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>(getInitialMessages);
  const openGroupTeams = getJoinableGroupTeams(selectedCreditPackage.teamList, currentUserId);
  const selectedGroupTeam = openGroupTeams.find((team) => team.teamId === selectedGroupTeamId);
  const activeTeam = selectedGroupTeam || openGroupTeams[0];
  const teamTargetCount = getGroupTargetCount(activeTeam, selectedCreditPackage.teamList?.[0]?.targetCount || 3);
  const teamJoinedCount = activeTeam
    ? Math.min(teamTargetCount, getGroupJoinedCount(activeTeam))
    : 0;
  const hasActiveGroupInfo = Boolean(activeTeam);
  const activeTeamId = activeTeam?.teamId;
  const teamStatistic = selectedCreditPackage.teamStatistic || {};
  const joinableTeamCount = openGroupTeams.length;
  const completeTeamCount = teamStatistic.allTeamCompleteCount || 0;
  const joinedUserCount = teamStatistic.allTeamUserCount || 0;
  const orderTotalPages = Math.max(1, Math.ceil(groupBuyOrders.length / ORDER_PAGE_SIZE));
  const normalizedOrderPage = Math.min(orderPage, orderTotalPages);
  const pagedGroupBuyOrders = groupBuyOrders.slice(
    (normalizedOrderPage - 1) * ORDER_PAGE_SIZE,
    normalizedOrderPage * ORDER_PAGE_SIZE
  );
  const isCurrentUserSending = isSending && sendingUserId === currentUserId;

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
  }, [messages, isCurrentUserSending]);

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

  useEffect(() => {
    if (!saveStatus) {
      return;
    }

    const timer = window.setTimeout(() => setSaveStatus(''), 2200);
    return () => window.clearTimeout(timer);
  }, [saveStatus]);

  useEffect(() => {
    currentUserIdRef.current = currentUserId || DEFAULT_USER_ID;
    localStorage.setItem(USER_ID_STORAGE_KEY, currentUserId || DEFAULT_USER_ID);
  }, [currentUserId]);

  useEffect(() => {
    creditsRef.current = credits;
  }, [credits]);

  useEffect(() => {
    const limitedMessages = limitChatMessages(messages);
    localStorage.setItem(getUserMessagesStorageKey(currentUserId), JSON.stringify(limitedMessages));

    if (limitedMessages.length !== messages.length) {
      setMessages(limitedMessages);
    }
  }, [currentUserId, messages]);

  useEffect(() => {
    const sessionStorageKey = getUserSessionStorageKey(currentUserId);

    if (sessionId) {
      localStorage.setItem(sessionStorageKey, sessionId);
      return;
    }

    localStorage.removeItem(sessionStorageKey);
  }, [currentUserId, sessionId]);

  useEffect(() => {
    const undoStorageKey = getUserXmlUndoStorageKey(currentUserId);

    if (xmlUndoState) {
      localStorage.setItem(undoStorageKey, JSON.stringify(xmlUndoState));
      return;
    }

    localStorage.removeItem(undoStorageKey);
  }, [currentUserId, xmlUndoState]);

  useEffect(() => {
    if (!currentTestGroup || currentTestGroup.joinedCount >= currentTestGroup.targetCount) {
      localStorage.removeItem(TEST_GROUP_STORAGE_KEY);
      return;
    }

    localStorage.setItem(TEST_GROUP_STORAGE_KEY, JSON.stringify(currentTestGroup));
  }, [currentTestGroup]);

  useEffect(() => {
    if (isDemoAuthed) {
      loadCreditData();
    }
  }, [isDemoAuthed, currentUserId]);

  useEffect(() => {
    if (isCreditPanelOpen) {
      loadCreditGoods();
    }
  }, [isCreditPanelOpen, currentUserId]);

  useEffect(() => {
    const nextOpenGroupTeams = getJoinableGroupTeams(selectedCreditPackage.teamList, currentUserId);
    const firstOpenTeamId = nextOpenGroupTeams[0]?.teamId || '';
    const selectedTeamStillOpen = nextOpenGroupTeams.some((team) => team.teamId === selectedGroupTeamId);

    if (!selectedTeamStillOpen) {
      setSelectedGroupTeamId(firstOpenTeamId);
    }
  }, [currentUserId, selectedCreditPackage, selectedGroupTeamId]);

  useEffect(() => {
    if (orderPage > orderTotalPages) {
      setOrderPage(orderTotalPages);
    }
  }, [orderPage, orderTotalPages]);

  const syncXmlToCanvas = (nextXml: string) => {
    const nextUndoState = {
      before: xmlRef.current,
      after: nextXml,
      undone: false
    };
    setXmlUndoState(nextUndoState);
    xmlRef.current = nextXml;
    setXml(nextXml);
    localStorage.setItem(getUserXmlStorageKey(currentUserId), nextXml);
    localStorage.setItem(getUserXmlUndoStorageKey(currentUserId), JSON.stringify(nextUndoState));
  };

  const updateMessagesForUser = (
    userId: string,
    updater: (current: ChatMessage[]) => ChatMessage[]
  ) => {
    if (currentUserIdRef.current === userId) {
      setMessages((current) => limitChatMessages(updater(current)));
      return;
    }

    const nextMessages = limitChatMessages(updater(getStoredMessagesForUser(userId)));
    saveStoredMessagesForUser(userId, nextMessages);
  };

  const updateSessionForUser = (userId: string, nextSessionId: string) => {
    if (currentUserIdRef.current === userId) {
      setSessionId(nextSessionId);
      return;
    }

    if (nextSessionId) {
      localStorage.setItem(getUserSessionStorageKey(userId), nextSessionId);
    } else {
      localStorage.removeItem(getUserSessionStorageKey(userId));
    }
  };

  const syncXmlForUser = (userId: string, nextXml: string) => {
    const previousXml = localStorage.getItem(getUserXmlStorageKey(userId)) || starterXml;
    const nextUndoState = {
      before: previousXml,
      after: nextXml,
      undone: false
    };

    localStorage.setItem(getUserXmlStorageKey(userId), nextXml);
    localStorage.setItem(getUserXmlUndoStorageKey(userId), JSON.stringify(nextUndoState));

    if (currentUserIdRef.current === userId) {
      xmlRef.current = nextXml;
      setXml(nextXml);
      setXmlUndoState(nextUndoState);
    }
  };

  const undoLastXmlSync = () => {
    if (!xmlUndoState) {
      return;
    }

    const nextXml = xmlUndoState.undone ? xmlUndoState.after : xmlUndoState.before;
    const nextUndoState = {
      ...xmlUndoState,
      undone: !xmlUndoState.undone
    };
    xmlRef.current = nextXml;
    setXml(nextXml);
    localStorage.setItem(getUserXmlStorageKey(currentUserId), nextXml);
    localStorage.setItem(getUserXmlUndoStorageKey(currentUserId), JSON.stringify(nextUndoState));
    setXmlUndoState(nextUndoState);
  };

  const sendMessage = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const text = inputValue.trim();
    const requestUserId = currentUserId;
    const requestSessionId = sessionId;
    const requestAgentId = activeAgentId;

    if ((!text && !selectedImageFile) || isSending) {
      return;
    }

    if (credits <= 0) {
      setIsCreditPanelOpen(true);
      updateMessagesForUser(requestUserId, (current) => [
        ...current,
        createMessage('agent', '额度不足，请购买额度后继续体验。', 'error')
      ]);
      return;
    }

    const imageFile = selectedImageFile;
    const requestId = createRequestId();
    const userMessage = imageFile
      ? `${text || '请根据这张图片生成 draw.io 图'}\n[图片：${imageFile.name}]`
      : text;
    const creditMessage = createMessage('agent', '正在处理，本次对话额度将在完成后按耗时结算。');

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
    setSendingUserId(requestUserId);
    updateMessagesForUser(requestUserId, (current) => [
      ...current,
      createMessage('user', userMessage),
      creditMessage
    ]);

    try {
      const data = imageFile
        ? await sendImageAnalyzeRequest(text, imageFile, requestUserId, requestId)
        : await sendChatRequest(text, requestUserId, requestSessionId, requestAgentId, requestId);
      const durationMs = data?.durationMs || 0;
      const creditCost = data?.costCredits || 0;
      const durationSeconds = Math.max(1, Math.round(durationMs / 1000));

      if (typeof data?.remainingCredits === 'number' && currentUserIdRef.current === requestUserId) {
        updateCredits(data.remainingCredits);
      }
      if (currentUserIdRef.current === requestUserId) {
        setLastCreditCost(creditCost);
      }

      updateMessagesForUser(requestUserId, (current) => current.map((message) => (
        message.id === creditMessage.id
          ? {
              ...message,
              content: `本次对话耗时 ${durationSeconds} 秒，消耗 ${creditCost} 点额度。`
            }
          : message
      )));

      if (imageFile) {
        if (currentUserIdRef.current === requestUserId) {
          setActiveAgentId(VISION_AGENT_ID);
        }
        updateSessionForUser(requestUserId, '');
      }

      const replyType = data?.type || 'user';
      const replyContent = data?.content || 'Agent 没有返回内容';

      if (data?.sessionId) {
        updateSessionForUser(requestUserId, data.sessionId);
      }

      if (replyType === 'drawio') {
        const drawioSummary = parseDrawioSummary(replyContent);
        syncXmlForUser(requestUserId, replyContent);
        updateMessagesForUser(requestUserId, (current) => [
          ...current,
          createMessage('agent', DRAWIO_SUCCESS_MESSAGE, replyType, drawioSummary)
        ]);
      } else {
        updateMessagesForUser(requestUserId, (current) => [...current, createMessage('agent', replyContent, replyType)]);
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : '未知错误';
      updateMessagesForUser(requestUserId, (current) => current.map((nextMessage) => (
        nextMessage.id === creditMessage.id
          ? {
              ...nextMessage,
              content: '本次对话未完成，未扣除额度。'
            }
          : nextMessage
      )));
      updateMessagesForUser(requestUserId, (current) => [
        ...current,
        createMessage('agent', `请求失败：${message}。请确认后端服务已启动在 8091 端口。`, 'error')
      ]);
    } finally {
      setIsSending(false);
      setSendingUserId((current) => (current === requestUserId ? '' : current));
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

  const switchDemoUser = (nextUserId: string) => {
    const normalizedUserId = nextUserId || DEFAULT_USER_ID;
    localStorage.setItem(getUserXmlStorageKey(currentUserId), xmlRef.current);
    saveStoredMessagesForUser(currentUserId, messages);
    if (sessionId) {
      localStorage.setItem(getUserSessionStorageKey(currentUserId), sessionId);
    } else {
      localStorage.removeItem(getUserSessionStorageKey(currentUserId));
    }
    if (xmlUndoState) {
      localStorage.setItem(getUserXmlUndoStorageKey(currentUserId), JSON.stringify(xmlUndoState));
    } else {
      localStorage.removeItem(getUserXmlUndoStorageKey(currentUserId));
    }
    localStorage.setItem(USER_ID_STORAGE_KEY, normalizedUserId);

    const nextXml = localStorage.getItem(getUserXmlStorageKey(normalizedUserId)) || starterXml;
    xmlRef.current = nextXml;
    setXml(nextXml);
    setCurrentUserId(normalizedUserId);
    setMessages(getStoredMessagesForUser(normalizedUserId));
    setSessionId(getStoredSessionForUser(normalizedUserId));
    setSelectedImageFile(null);
    setSelectedImageName('');
    setLastApiResponse(null);
    setXmlUndoState(getStoredXmlUndoStateForUser(normalizedUserId));
    setActivePanelView('chat');
    setOrderPage(1);
    setGroupBuyOrders([]);
    setCreditDataError('');
    setCreditGoodsError('');
    setLockOrderError('');
    setSettlementError('');
    setLockedPayOrder(null);
    setCurrentTestGroup(null);
    if (imageInputRef.current) {
      imageInputRef.current.value = '';
    }
  };

  const enterDemo = () => {
    sessionStorage.setItem(DEMO_SESSION_KEY, '1');
    setIsDemoAuthed(true);
  };

  const saveCurrentXml = (nextXml: string, status = '已保存到本地') => {
    xmlRef.current = nextXml;
    setXml(nextXml);
    localStorage.setItem(getUserXmlStorageKey(currentUserId), nextXml);
    setSaveStatus(status);
  };

  const updateCredits = (nextCredits: number) => {
    const normalizedCredits = Math.max(0, nextCredits);
    localStorage.setItem(CREDIT_STORAGE_KEY, String(normalizedCredits));
    setCredits(normalizedCredits);
  };

  const calculateCreditCostByDuration = (durationMs: number) => {
    return Math.max(1, Math.ceil(durationMs / 10000));
  };

  const consumeCreditsByDuration = (durationMs: number, userId = currentUserId) => {
    const cost = calculateCreditCostByDuration(durationMs);
    const currentCredits = currentUserIdRef.current === userId ? creditsRef.current : getInitialCredits();
    const actualCost = Math.min(cost, currentCredits);

    if (currentUserIdRef.current === userId) {
      updateCredits(currentCredits - actualCost);
      setLastCreditCost(actualCost);
    }

    return actualCost;
  };

  const purchaseCredits = (creditPackage: CreditPackage) => {
    setLockOrderError('');
    setSettlementError('');

    const outTradeNo = createOutTradeNo();
    const payPrice = parseCurrencyAmount(creditPackage.originalPrice) || parseCurrencyAmount(creditPackage.price);
    setLockedPayOrder({
      orderId: outTradeNo,
      outTradeNo,
      goodsId: creditPackage.goodsId || CREDIT_GOODS_IDS[0],
      goodsName: creditPackage.goodsName || `${creditPackage.credits} 点额度普通包`,
      credits: creditPackage.credits,
      payPrice,
      purchaseType: 'direct'
    });
  };

  const createOutTradeNo = () => {
    return `${Date.now()}`.slice(-8) + `${Math.floor(Math.random() * 10000)}`.padStart(4, '0');
  };

  const lockGroupBuyOrder = async () => {
    setIsLockingOrder(true);
    setLockOrderError('');

    try {
      const outTradeNo = createOutTradeNo();
      const goodsId = selectedCreditPackage.goodsId || CREDIT_GOODS_IDS[0];
      const configResponse = await fetch('/api/v1/gbm/index/query_group_buy_market_config', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          userId: currentUserId,
          source: CREDIT_SOURCE,
          channel: CREDIT_CHANNEL,
          goodsId
        })
      });
      const configParsed = await parseJsonResponse<GroupBuyMarketConfigResponse>(configResponse);

      if (!configResponse.ok || configParsed.code !== '0000' || !configParsed.data?.goods) {
        throw new Error(configParsed.info || `HTTP ${configResponse.status}`);
      }

      const latestCreditPackage = buildCreditPackageFromConfig(configParsed.data);
      const latestOpenTeams = getJoinableGroupTeams(latestCreditPackage.teamList, currentUserId);
      const latestSelectedTeam = latestOpenTeams.find((team) => team.teamId === selectedGroupTeamId);
      const latestActiveTeam = latestSelectedTeam || latestOpenTeams[0];
      const latestTargetCount = getGroupTargetCount(latestActiveTeam, latestCreditPackage.teamList?.[0]?.targetCount || 3);
      const latestJoinedCount = latestActiveTeam
        ? Math.min(latestTargetCount, getGroupJoinedCount(latestActiveTeam))
        : 0;

      setSelectedCreditPackage(latestCreditPackage);
      setSelectedGroupTeamId(latestActiveTeam?.teamId || '');

      const response = await fetch('/api/v1/gbm/trade/lock_market_pay_order', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          userId: currentUserId,
          source: CREDIT_SOURCE,
          channel: CREDIT_CHANNEL,
          goodsId,
          activityId: latestCreditPackage.activityId,
          teamId: latestActiveTeam?.teamId,
          outTradeNo,
          notifyConfigVO: {
            notifyType: 'MQ'
          }
        })
      });
      const parsed = await parseJsonResponse<LockMarketPayOrderResponse>(response);

      if (!response.ok || parsed.code !== '0000' || !parsed.data) {
        throw new Error(parsed.info || `HTTP ${response.status}`);
      }

      const nextLockedPayOrder: LockedPayOrder = {
        ...parsed.data,
        goodsId,
        goodsName: latestCreditPackage.goodsName,
        activityId: latestCreditPackage.activityId,
        teamId: parsed.data.teamId || latestActiveTeam?.teamId,
        targetCount: latestTargetCount,
        joinedCount: latestJoinedCount,
        purchaseType: 'group_buy',
        outTradeNo,
        credits: latestCreditPackage.credits
      };

      setLockedPayOrder(nextLockedPayOrder);
      setSettlementError('');
    } catch (error) {
      const message = error instanceof Error ? error.message : '锁单失败';
      setLockOrderError(message);
    } finally {
      setIsLockingOrder(false);
    }
  };

  const cancelLockedOrder = async () => {
    if (!lockedPayOrder) {
      setLockedPayOrder(null);
      return;
    }
    if (isSettlingOrder) {
      return;
    }

    const orderToCancel = lockedPayOrder;
    if (orderToCancel.purchaseType === 'direct') {
      setLockedPayOrder(null);
      setSettlementError('');
      return;
    }

    setSettlementError('');

    try {
      const response = await fetch('/api/v1/gbm/trade/cancel_market_pay_order', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          source: CREDIT_SOURCE,
          channel: CREDIT_CHANNEL,
          userId: currentUserId,
          outTradeNo: orderToCancel.outTradeNo
        })
      });
      const parsed = await parseJsonResponse<{ code?: string; info?: string }>(response);

      if (!response.ok || parsed.code !== '0000') {
        throw new Error(parsed.info || `HTTP ${response.status}`);
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : '取消锁单失败';
      setSettlementError(message);
    } finally {
      setLockedPayOrder((current) => (
        current?.outTradeNo === orderToCancel.outTradeNo ? null : current
      ));
    }
  };

  const closeCreditPanel = async () => {
    if (isSettlingOrder) {
      return;
    }
    if (lockedPayOrder) {
      await cancelLockedOrder();
    }
    setIsCreditPanelOpen(false);
  };

  const openOrdersPanel = () => {
    setOrderPage(1);
    setActivePanelView('orders');
    void loadCreditData();
  };

  const upsertGroupBuyOrder = (nextOrder: GroupBuyOrder) => {
    setGroupBuyOrders((current) => {
      const existingIndex = current.findIndex((item) => item.outTradeNo === nextOrder.outTradeNo);

      if (existingIndex < 0) {
        return [nextOrder, ...current];
      }

      return current.map((item) => (
        item.outTradeNo === nextOrder.outTradeNo ? nextOrder : item
      ));
    });
  };

  const buildGroupBuyOrderFromCreditOrder = (creditOrder: CreditOrderResponse): GroupBuyOrder | null => {
    if (!creditOrder.outTradeNo) {
      return null;
    }

    const creditsAmount = toNumber(creditOrder.credits);
    const payPrice = toNumber(creditOrder.payPrice);
    const purchaseType = creditOrder.teamId ? 'group_buy' : 'direct';
    const matchedTestGroup = currentTestGroup?.teamId === creditOrder.teamId ? currentTestGroup : null;
    const targetCount = purchaseType === 'direct'
      ? 1
      : Math.max(1, toNumber(creditOrder.targetCount, matchedTestGroup?.targetCount || 3));
    const baseResolvedStatus = resolveCreditOrderStatus(creditOrder.status, creditOrder.statusInfo);
    const realtimeJoinedCount = toNumber(creditOrder.joinedCount);
    const preliminaryJoinedCount = purchaseType === 'direct'
      ? 1
      : realtimeJoinedCount > 0
        ? Math.min(targetCount, realtimeJoinedCount)
        : matchedTestGroup
        ? Math.min(targetCount, matchedTestGroup.joinedCount)
        : 1;
    const isRealtimeFormed = purchaseType === 'group_buy' && preliminaryJoinedCount >= targetCount;
    const resolvedStatus = purchaseType === 'direct' && baseResolvedStatus.status === 'waiting'
      ? {
          status: 'waiting' as const,
          title: '待到账',
          text: '已支付，等待额度发放'
        }
      : isRealtimeFormed && baseResolvedStatus.status === 'waiting'
      ? {
          status: 'formed' as const,
          title: '已成团',
          text: '拼团已满，等待后端发放额度'
        }
      : baseResolvedStatus;
    const displayStatus = purchaseType === 'group_buy' && resolvedStatus.status === 'formed' && !isRealtimeFormed
      ? {
          status: 'waiting' as const,
          title: '待成团',
          text: '已支付，等待拼团成团'
        }
      : resolvedStatus;
    const isFormed = displayStatus.status !== 'waiting';
    const joinedCount = purchaseType === 'direct'
      ? 1
      : isFormed
        ? targetCount
        : preliminaryJoinedCount;

    return {
      orderId: creditOrder.orderId,
      outTradeNo: creditOrder.outTradeNo,
      goodsId: creditOrder.goodsId,
      goodsName: creditOrder.goodsName,
      teamId: creditOrder.teamId,
      purchaseType,
      credits: creditsAmount,
      payPrice,
      targetCount,
      joinedCount,
      paidAt: creditOrder.paidTime,
      grantTime: creditOrder.grantTime,
      createTime: creditOrder.createTime,
      updateTime: creditOrder.updateTime,
      status: displayStatus.status,
      statusCode: creditOrder.status,
      statusTitle: displayStatus.title,
      statusText: displayStatus.text
    };
  };

  const loadCreditData = async () => {
    setIsCreditDataLoading(true);
    setCreditDataError('');

    try {
      const [accountResponse, orderListResponse] = await Promise.all([
        fetch(`/api/v1/credit/query_credit_account/${encodeURIComponent(currentUserId)}`),
        fetch(`/api/v1/credit/query_credit_order_list/${encodeURIComponent(currentUserId)}`)
      ]);
      const accountParsed = await parseJsonResponse<QueryCreditAccountResponse>(accountResponse);
      const orderListParsed = await parseJsonResponse<QueryCreditOrderListResponse>(orderListResponse);

      if (!accountResponse.ok || accountParsed.code !== '0000') {
        throw new Error(accountParsed.info || `账户接口 HTTP ${accountResponse.status}`);
      }
      if (!orderListResponse.ok || orderListParsed.code !== '0000') {
        throw new Error(orderListParsed.info || `订单接口 HTTP ${orderListResponse.status}`);
      }

      setCredits(toNumber(accountParsed.data?.availableCredits));
      setGroupBuyOrders((orderListParsed.data || [])
        .map(buildGroupBuyOrderFromCreditOrder)
        .filter((order): order is GroupBuyOrder => Boolean(order)));
    } catch (error) {
      const message = error instanceof Error ? error.message : '额度数据加载失败';
      setCreditDataError(message);
    } finally {
      setIsCreditDataLoading(false);
    }
  };

  const refreshGroupOrderStatus = async (order: GroupBuyOrder) => {
    setRefreshingOrderNo(order.outTradeNo);
    setOrderStatusError(null);

    try {
      const response = await fetch(`/api/v1/credit/query_credit_order/${encodeURIComponent(order.outTradeNo)}`);
      const parsed = await parseJsonResponse<QueryCreditOrderResponse>(response);

      if (!response.ok || parsed.code !== '0000') {
        throw new Error(parsed.info || `HTTP ${response.status}`);
      }

      if (!parsed.data) {
        throw new Error('订单不存在');
      }

      const latestOrder = buildGroupBuyOrderFromCreditOrder(parsed.data);
      if (!latestOrder) {
        throw new Error('订单数据无效');
      }

      upsertGroupBuyOrder({
        ...latestOrder,
        paymentMethod: order.paymentMethod
      });

      if (latestOrder.teamId && currentTestGroup?.teamId === latestOrder.teamId) {
        setCurrentTestGroup(latestOrder.status === 'waiting' ? {
          teamId: latestOrder.teamId,
          goodsId: latestOrder.goodsId || currentTestGroup.goodsId,
          activityId: latestOrder.activityId,
          targetCount: latestOrder.targetCount || currentTestGroup.targetCount,
          joinedCount: latestOrder.joinedCount || currentTestGroup.joinedCount
        } : null);
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : '订单状态刷新失败';
      setOrderStatusError({ outTradeNo: order.outTradeNo, message });
    } finally {
      setRefreshingOrderNo((current) => (current === order.outTradeNo ? '' : current));
    }
  };

  const confirmDemoPayment = async (paymentMethod: '微信支付' | '支付宝支付') => {
    if (!lockedPayOrder) {
      return;
    }

    const orderToPay = lockedPayOrder;
    setIsSettlingOrder(true);
    setSettlementError('');

    try {
      const paidAt = new Date().toISOString();
      if (orderToPay.purchaseType === 'direct') {
        const response = await fetch('/api/v1/credit/purchase_credit_order', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            userId: currentUserId,
            teamId: null,
            orderId: orderToPay.orderId || orderToPay.outTradeNo,
            outTradeNo: orderToPay.outTradeNo,
            goodsId: orderToPay.goodsId,
            goodsName: orderToPay.goodsName,
            credits: orderToPay.credits,
            payPrice: orderToPay.payPrice,
            paidTime: paidAt
          })
        });
        const parsed = await parseJsonResponse<PurchaseCreditOrderResponse>(response);

        if (!response.ok || parsed.code !== '0000' || !parsed.data) {
          throw new Error(parsed.info || `HTTP ${response.status}`);
        }

        const nextOrder = buildGroupBuyOrderFromCreditOrder(parsed.data);
        if (nextOrder) {
          upsertGroupBuyOrder({
            ...nextOrder,
            paymentMethod
          });
        }
        setIsCreditPanelOpen(false);
        setLockedPayOrder(null);
        setOrderPage(1);
        setActivePanelView('orders');
        await loadCreditData();
        return;
      }

      const settlementResponse = await fetch('/api/v1/gbm/trade/settlement_market_pay_order', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          source: CREDIT_SOURCE,
          channel: CREDIT_CHANNEL,
          userId: currentUserId,
          outTradeNo: orderToPay.outTradeNo,
          outTradeTime: paidAt
        })
      });
      const settlementParsed = await parseJsonResponse<SettlementMarketPayOrderResponse>(settlementResponse);

      if (!settlementResponse.ok || settlementParsed.code !== '0000' || !settlementParsed.data) {
        throw new Error(settlementParsed.info || `HTTP ${settlementResponse.status}`);
      }

      const settledTeamId = settlementParsed.data.teamId || orderToPay.teamId;
      const targetCount = orderToPay.targetCount || teamTargetCount;
      const matchedTestGroup = currentTestGroup?.teamId === settledTeamId ? currentTestGroup : null;
      const previousJoinedCount = matchedTestGroup
        ? matchedTestGroup.joinedCount
        : (orderToPay.joinedCount || teamJoinedCount);
      const joinedCount = Math.min(targetCount, Math.max(previousJoinedCount + 1, 1));
      const isFormed = false;
      const settledStatus = resolveCreditOrderStatus('WAIT_GROUP');
      const nextOrder: GroupBuyOrder = {
        ...orderToPay,
        teamId: settledTeamId,
        activityId: settlementParsed.data.activityId || orderToPay.activityId,
        paymentMethod,
        paidAt,
        targetCount,
        joinedCount,
        status: settledStatus.status,
        statusCode: 'WAIT_GROUP',
        statusTitle: settledStatus.title,
        statusText: settledStatus.text
      };

      if (settledTeamId) {
        setCurrentTestGroup(isFormed ? null : {
          teamId: settledTeamId,
          goodsId: orderToPay.goodsId || CREDIT_GOODS_IDS[0],
          activityId: nextOrder.activityId,
          targetCount,
          joinedCount
        });
      }

      const creditOrderResponse = await fetch('/api/v1/credit/create_credit_order', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          userId: currentUserId,
          teamId: nextOrder.teamId,
          orderId: nextOrder.orderId,
          outTradeNo: nextOrder.outTradeNo,
          goodsId: nextOrder.goodsId,
          goodsName: nextOrder.goodsName,
          credits: nextOrder.credits,
          payPrice: nextOrder.payPrice,
          status: nextOrder.statusCode,
          paidTime: paidAt
        })
      });
      const creditOrderParsed = await parseJsonResponse<CreateCreditOrderResponse>(creditOrderResponse);

      if (!creditOrderResponse.ok || creditOrderParsed.code !== '0000') {
        throw new Error(creditOrderParsed.info || `订单同步 HTTP ${creditOrderResponse.status}`);
      }

      upsertGroupBuyOrder(nextOrder);
      setIsCreditPanelOpen(false);
      setLockedPayOrder(null);
      setOrderPage(1);
      setActivePanelView('orders');
      window.setTimeout(() => {
        void refreshGroupOrderStatus(nextOrder);
        void loadCreditData();
      }, 0);
    } catch (error) {
      const message = error instanceof Error ? error.message : '支付确认失败';
      setSettlementError(message);
    } finally {
      setIsSettlingOrder(false);
    }
  };

  const loadCreditGoods = async () => {
    setIsCreditGoodsLoading(true);
    setCreditGoodsError('');

    try {
      const fetchGoods = async (goodsId: string) => {
        const response = await fetch('/api/v1/gbm/index/query_group_buy_market_config', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            userId: currentUserId,
            source: CREDIT_SOURCE,
            channel: CREDIT_CHANNEL,
            goodsId
          })
        });
        const parsed = await parseJsonResponse<GroupBuyMarketConfigResponse>(response);

        if (!response.ok || parsed.code !== '0000' || !parsed.data?.goods) {
          throw new Error(parsed.info || `HTTP ${response.status}`);
        }

        return buildCreditPackageFromConfig(parsed.data);
      };

      const nextPackages = await Promise.all(CREDIT_GOODS_IDS.map(fetchGoods));
      const preferredPackage = currentTestGroup
        ? nextPackages.find((item) => item.goodsId === currentTestGroup.goodsId)
        : null;
      setCreditPackages(nextPackages);
      setSelectedCreditPackage(preferredPackage || nextPackages[1] || nextPackages[0]);
    } catch (error) {
      const message = error instanceof Error ? error.message : '额度商品加载失败';
      setCreditGoodsError(message);
      setCreditPackages(CREDIT_PACKAGES);
      setSelectedCreditPackage(CREDIT_PACKAGES[1]);
    } finally {
      setIsCreditGoodsLoading(false);
    }
  };

  const exitDemo = () => {
    sessionStorage.removeItem(DEMO_SESSION_KEY);
    setIsDemoAuthed(false);
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

    const apiResponse = parsed as ChatApiResponse;
    if (apiResponse.code && apiResponse.code !== '0000') {
      throw new Error(apiResponse.info || `API ${apiResponse.code}`);
    }

    return parsed as ChatApiResponse & ApiAgentReply;
  };

  const sendChatRequest = async (
    text: string,
    userId = currentUserId,
    nextSessionId = sessionId,
    agentId = activeAgentId,
    requestId = createRequestId()
  ) => {
    const parsed = await requestJson('/api/v1/chat', {
      agentId,
      userId,
      sessionId: nextSessionId,
      requestId,
      message: text
    });
    return parseChatApiResponse(parsed);
  };

  const sendImageAnalyzeRequest = async (text: string, imageFile: File, userId = currentUserId, requestId = createRequestId()) => {
    const imageDataUrl = await fileToDataUrl(imageFile);
    const parsed = await requestJson('/api/v1/analyze_diagram_image', {
      agentId: VISION_AGENT_ID,
      userId,
      sessionId: '',
      requestId,
      message: text || '请模拟这张图片，生成一张 draw.io 图。',
      imageDataUrl
    });
    return parseChatApiResponse(parsed);
  };

  if (!isDemoAuthed) {
    return (
      <main className="auth-shell">
        <section className="auth-preview" aria-hidden="true">
          <div className="auth-preview-window">
            <div className="auth-preview-toolbar">
              <span />
              <span />
              <span />
            </div>
            <div className="auth-preview-canvas">
              <div className="auth-node primary">用户需求</div>
              <div className="auth-line" />
              <div className="auth-node accent">DrawIO Agent</div>
              <div className="auth-line" />
              <div className="auth-node success">图表生成</div>
            </div>
          </div>
        </section>

        <section className="auth-panel" aria-label="登录注册入口">
          <div className="auth-brand">
            <span>DrawIO Agent</span>
            <strong>用对话生成和修改流程图</strong>
          </div>

          <div className="auth-tabs" role="tablist" aria-label="登录注册切换">
            <button
              type="button"
              className={authMode === 'login' ? 'active' : ''}
              onClick={() => setAuthMode('login')}
            >
              <LogIn size={16} />
              登录
            </button>
            <button
              type="button"
              className={authMode === 'register' ? 'active' : ''}
              onClick={() => setAuthMode('register')}
            >
              <UserPlus size={16} />
              注册
            </button>
          </div>

          <form className="auth-form" onSubmit={(event) => {
            event.preventDefault();
            enterDemo();
          }}>
            <label>
              <span>用户名</span>
              <input
                key={`username-${authMode}`}
                name="username"
                autoComplete="username"
                defaultValue={authMode === 'login' ? 'admin' : ''}
                placeholder={authMode === 'login' ? 'admin' : '请输入用户名'}
              />
            </label>
            <label>
              <span>密码</span>
              <input
                key={`password-${authMode}`}
                name="password"
                type="password"
                autoComplete={authMode === 'login' ? 'current-password' : 'new-password'}
                defaultValue={authMode === 'login' ? '123456' : ''}
                placeholder={authMode === 'login' ? '123456' : '任意输入'}
              />
            </label>
            {authMode === 'register' && (
              <label>
                <span>昵称</span>
                <input name="nickname" autoComplete="nickname" placeholder="DrawIO User" />
              </label>
            )}
            <button type="submit" className="auth-submit-button">
              {authMode === 'login' ? '登录' : '注册并进入'}
            </button>
          </form>

          <button type="button" className="demo-entry-button" onClick={enterDemo}>
            <Play size={17} />
            演示体验
          </button>
        </section>
      </main>
    );
  }

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
          key={currentUserId}
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
            saveCurrentXml(data.xml || xml, '已连接当前画布');
          }}
          onAutoSave={(data) => {
            saveCurrentXml(data.xml, '已自动保存');
          }}
          onSave={(data) => {
            saveCurrentXml(data.xml);
          }}
          onClose={exitDemo}
          onExport={(data) => {
            saveCurrentXml(data.xml || data.data || xml);
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
                <div className="chat-status-row">
                <div className="chat-connection-status">
                  <span aria-hidden="true" />
                  {saveStatus || '已连接当前画布'}
                </div>
                <label className="demo-user-switch">
                  <span>测试用户</span>
                  <select
                    value={currentUserId}
                    onChange={(event) => switchDemoUser(event.target.value)}
                    title="切换测试用户"
                  >
                    <option value="drawio-web-user">用户 1</option>
                    <option value="drawio-web-user-2">用户 2</option>
                    <option value="drawio-web-user-3">用户 3</option>
                    <option value="drawio-web-user-4">用户 4</option>
                  </select>
                </label>
                <div className="credit-summary">
                  <span>
                    剩余额度：
                    <strong>{isCreditDataLoading ? '...' : credits}</strong>
                    点
                  </span>
                  <button
                    type="button"
                    onClick={() => {
                      setActivePanelView('chat');
                      setIsCreditPanelOpen((current) => !current);
                    }}
                    title="购买额度"
                  >
                    <Coins size={13} />
                    购买额度
                  </button>
                  <button
                    type="button"
                    className={activePanelView === 'orders' ? 'active' : ''}
                    onClick={openOrdersPanel}
                    title="查看订单"
                  >
                    <Receipt size={13} />
                    我的订单
                  </button>
                </div>
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

            {activePanelView === 'chat' ? (
            <>
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
              ))}              {isCurrentUserSending && (
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
                    {isCurrentUserSending ? <LoaderCircle className="spin" size={18} /> : <Send size={18} />}
                  </button>
                </div>
              </div>
            </form>
            </>
            ) : (
            <section className="order-center" aria-label="订单中心">
              <div className="order-center-header">
                <div>
                  <span>订单中心</span>
                  <strong>额度订单</strong>
                </div>
                <div className="order-header-actions">
                  <button
                    type="button"
                    onClick={loadCreditData}
                    disabled={isCreditDataLoading}
                    title="刷新订单"
                  >
                    <RefreshCw className={isCreditDataLoading ? 'spin' : ''} size={15} />
                    {isCreditDataLoading ? '刷新中' : '刷新订单'}
                  </button>
                  <button
                    type="button"
                    onClick={() => setActivePanelView('chat')}
                    title="返回对话"
                  >
                    返回对话
                  </button>
                </div>
              </div>
              {creditDataError && (
                <div className="payment-error">额度数据加载失败：{creditDataError}</div>
              )}

              {groupBuyOrders.length > 0 ? (
                <>
                <div className="order-list">
                  {pagedGroupBuyOrders.map((groupBuyOrder) => {
                    const isDirectOrder = groupBuyOrder.purchaseType === 'direct' || !groupBuyOrder.teamId;
                    const orderTargetCount = groupBuyOrder.targetCount || teamTargetCount;
                    const orderJoinedCount = Math.min(orderTargetCount, groupBuyOrder.joinedCount || 0);
                    const orderMissingCount = Math.max(0, orderTargetCount - orderJoinedCount);
                    const isRefreshingCurrentOrder = refreshingOrderNo === groupBuyOrder.outTradeNo;
                    const currentOrderError = orderStatusError?.outTradeNo === groupBuyOrder.outTradeNo
                      ? orderStatusError.message
                      : '';

                    return (
                      <div className="order-card" key={groupBuyOrder.outTradeNo}>
                        <div className="order-card-status">
                          <span className={`order-status-label ${groupBuyOrder.status}`}>
                            {groupBuyOrder.statusTitle}
                          </span>
                          <em>{groupBuyOrder.statusText}</em>
                        </div>

                        <div className="order-card-title">
                          <strong>{groupBuyOrder.goodsName || `${groupBuyOrder.credits} 点额度${isDirectOrder ? '普通包' : '拼团包'}`}</strong>
                          <span>订单号：{groupBuyOrder.orderId || groupBuyOrder.outTradeNo}</span>
                          <span>{isDirectOrder ? '购买方式：原价购买' : `拼团队伍：${groupBuyOrder.teamId || '待生成'}`}</span>
                          <span className="order-time-line">支付时间：{formatOrderTime(groupBuyOrder.paidAt || groupBuyOrder.createTime) || '待记录'}</span>
                          {groupBuyOrder.grantTime && (
                            <span className="order-time-line">到账时间：{formatOrderTime(groupBuyOrder.grantTime)}</span>
                          )}
                        </div>

                        <div className="order-summary-row">
                          <div>
                            <span>应付金额</span>
                            <strong>{formatCurrency(groupBuyOrder.payPrice)}</strong>
                          </div>
                          <div>
                            <span>到账额度</span>
                            <strong>{groupBuyOrder.credits} 点</strong>
                          </div>
                          <div>
                            <span>支付状态</span>
                            <strong>{groupBuyOrder.paymentMethod || '已支付'}</strong>
                          </div>
                        </div>

                        <div className="order-bottom-row">
                          {isDirectOrder ? (
                            <div className="order-group-status">
                              <div className="order-progress-copy">
                                <span>普通购买</span>
                                <strong>{groupBuyOrder.status === 'granted' ? '额度已到账' : '额度发放中'}</strong>
                              </div>
                              <p>{groupBuyOrder.status === 'granted' ? '原价购买已完成，额度已到账。' : '原价购买已支付，正在等待额度发放。'}</p>
                            </div>
                          ) : (
                            <div className="order-group-status">
                              <div className="order-progress-copy">
                                <span>拼团进度 {orderJoinedCount}/{orderTargetCount}</span>
                                <strong>{orderMissingCount > 0 ? `还差 ${orderMissingCount} 人成团` : '拼团已完成'}</strong>
                              </div>
                              <div className="order-node-progress" aria-label={`拼团进度 ${orderJoinedCount}/${orderTargetCount}`}>
                                {Array.from({ length: orderTargetCount }).map((_, index) => (
                                  <span
                                    key={`${groupBuyOrder.outTradeNo}-node-${index}`}
                                    className={index < orderJoinedCount ? 'completed' : ''}
                                  />
                                ))}
                              </div>
                              <p>{getOrderStatusDescription(groupBuyOrder)}</p>
                            </div>
                          )}

                          <div className="order-actions">
                            <button
                              type="button"
                              onClick={() => refreshGroupOrderStatus(groupBuyOrder)}
                              disabled={Boolean(refreshingOrderNo)}
                            >
                              <RefreshCw className={isRefreshingCurrentOrder ? 'spin' : ''} size={14} />
                              {isRefreshingCurrentOrder ? '刷新中' : '刷新订单状态'}
                            </button>
                          </div>
                        </div>
                        {currentOrderError && (
                          <div className="payment-error">订单刷新失败：{currentOrderError}</div>
                        )}
                      </div>
                    );
                  })}
                </div>
                <div className="order-pagination">
                  <span>
                    第 {normalizedOrderPage} / {orderTotalPages} 页 · 共 {groupBuyOrders.length} 条订单
                  </span>
                  <div>
                    <button
                      type="button"
                      onClick={() => setOrderPage((current) => Math.max(1, current - 1))}
                      disabled={normalizedOrderPage <= 1}
                    >
                      上一页
                    </button>
                    <button
                      type="button"
                      onClick={() => setOrderPage((current) => Math.min(orderTotalPages, current + 1))}
                      disabled={normalizedOrderPage >= orderTotalPages}
                    >
                      下一页
                    </button>
                  </div>
                </div>
                </>
              ) : (
                <div className="order-empty">
                  <Receipt size={34} />
                  <strong>暂无额度订单</strong>
                  <span>购买额度并完成支付后，会自动进入这里查看到账状态。</span>
                  <button
                    type="button"
                    onClick={() => {
                      setActivePanelView('chat');
                      setIsCreditPanelOpen(true);
                    }}
                  >
                    去购买额度
                  </button>
                </div>
              )}
            </section>
            )}
          </div>
        )}
      </aside>

      {isCreditPanelOpen && (
        <div className="credit-modal-backdrop" role="presentation" onMouseDown={closeCreditPanel}>
          <section
            className="credit-modal"
            role="dialog"
            aria-modal="true"
            aria-label="额度购买"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <div className="credit-modal-header">
              <div>
                <strong>购买对话额度</strong>
              </div>
              <button
                type="button"
                onClick={closeCreditPanel}
                title="关闭"
              >
                <X size={17} />
              </button>
            </div>

            <div className="credit-modal-balance">
              <span>当前余额</span>
              <strong>{isCreditDataLoading ? '加载中...' : `${credits} 点`}</strong>
            </div>

            <section className="credit-promo-card" aria-label="当前拼团">
              <div className="credit-promo-main">
                <div>
                  <div className="credit-promo-heading">
                    <strong>当前拼团</strong>
                    <span>{joinableTeamCount > 0 ? `${joinableTeamCount} 个可参与团` : '暂无可参与团'}</span>
                  </div>
                  <h3>{teamTargetCount} 人团 · {hasActiveGroupInfo ? '选择一个团加入' : '系统自动开团'}</h3>
                  <p>
                    {hasActiveGroupInfo
                      ? `已有 ${joinedUserCount || teamJoinedCount} 人参与，${completeTeamCount} 个团已成团，加入下方队伍后即可享受拼团价。`
                      : `当前暂无可加入队伍，锁单时将按 ${teamTargetCount} 人团为你创建优惠名额。`}
                  </p>
                </div>
                <div className="credit-promo-status">
                  <strong>拼团价生效中</strong>
                  <span>当前可省 {selectedCreditPackage.saving}</span>
                </div>
              </div>
              {openGroupTeams.length > 0 ? (
                <div className="credit-team-list-wrap">
                  <div className="credit-team-list-title">
                    <strong>可参与队伍</strong>
                    <span>请选择一个团参与</span>
                  </div>
                  <div className="credit-team-list" aria-label="选择拼团队伍">
                    {openGroupTeams.map((team) => {
                      const targetCount = getGroupTargetCount(team, teamTargetCount);
                      const joinedCount = Math.min(targetCount, getGroupJoinedCount(team));
                      const isSelected = activeTeamId === team.teamId;
                      const previewJoinedCount = Math.min(targetCount, joinedCount + (isSelected ? 1 : 0));
                      const missingCount = Math.max(0, targetCount - previewJoinedCount);

                      return (
                        <button
                          key={team.teamId}
                          type="button"
                          className={`credit-team-option ${isSelected ? 'selected' : ''}`}
                          onClick={() => setSelectedGroupTeamId(team.teamId || '')}
                        >
                          <span className="credit-team-leader">
                            <small>团长</small>
                            <strong>{team.userId || team.teamId || '待生成'}</strong>
                          </span>
                          <span className="credit-seat-row" aria-label={`成团进度 ${previewJoinedCount}/${targetCount}`}>
                            {Array.from({ length: targetCount }).map((_, index) => {
                              const isFilled = index < joinedCount;
                              const isYourSeat = isSelected && index === joinedCount;

                              return (
                                <span
                                  key={`${team.teamId}-seat-${index}`}
                                  className={`credit-seat-node ${isFilled ? 'filled' : ''} ${isYourSeat ? 'your-seat' : ''}`}
                                >
                                  {isYourSeat ? '你' : isFilled ? (index === 0 ? '团长' : '已占') : '待加入'}
                                </span>
                              );
                            })}
                          </span>
                          <span className="credit-team-state">{isSelected ? '选中' : '可加入'} · 还差 {missingCount} 人</span>
                        </button>
                      );
                    })}
                  </div>
                </div>
              ) : (
                <div className="credit-team-empty">暂无可加入队伍，提交后自动开团。</div>
              )}
              <div className="credit-group-progress">
                <span>成团进度 {teamJoinedCount}/{teamTargetCount} · 还差 {Math.max(0, teamTargetCount - teamJoinedCount)} 人成团</span>
                <strong>拼团价已生效</strong>
              </div>
            </section>

            <div className="credit-section-title">选择额度套餐</div>
            {isCreditGoodsLoading && (
              <div className="credit-loading">正在加载真实额度商品...</div>
            )}
            {creditGoodsError && (
              <div className="credit-load-error">额度商品加载失败，已展示演示套餐：{creditGoodsError}</div>
            )}
            <div className="credit-package-list">
              {creditPackages.map((item) => (
                <button
                  key={item.goodsId || item.credits}
                  type="button"
                  className={`credit-package-button ${selectedCreditPackage.credits === item.credits ? 'selected' : ''}`}
                  onClick={() => setSelectedCreditPackage(item)}
                >
                  <span className="credit-package-title">
                    <strong>{item.credits} 点</strong>
                    {item.badge && <em>{item.badge}</em>}
                  </span>
                  <strong className="credit-package-price">{item.price}</strong>
                  <del>原价 {item.originalPrice}</del>
                  <i>立省 {item.saving}</i>
                  <small>{item.desc}</small>
                </button>
              ))}
            </div>

            <div className="credit-modal-footer">
              <div className="credit-footer-summary">
                <strong>应付：{selectedCreditPackage.price}</strong>
                <span>已选：{selectedCreditPackage.credits} 点 · 拼团优惠：已省 {selectedCreditPackage.saving}</span>
              </div>
              <div className="credit-footer-actions">
                <button
                  type="button"
                  className="credit-original-price-button"
                  onClick={() => purchaseCredits(selectedCreditPackage)}
                >
                  原价购买
                </button>
                <button
                  type="button"
                  className="credit-group-buy-button"
                  onClick={lockGroupBuyOrder}
                  disabled={isLockingOrder}
                >
                  {isLockingOrder ? '正在锁单...' : '立即享拼团价'}
                </button>
              </div>
            </div>
            {lockOrderError && (
              <div className="payment-error">锁单失败：{lockOrderError}</div>
            )}
          </section>
          {lockedPayOrder && (
            <section
              className="payment-method-modal"
              role="dialog"
              aria-modal="true"
              aria-label="选择支付方式"
              onMouseDown={(event) => event.stopPropagation()}
            >
              <div className="payment-method-header">
                <div>
                  <span>{lockedPayOrder.purchaseType === 'direct' ? '订单待支付' : '订单已锁定'}</span>
                  <strong>选择支付方式</strong>
                </div>
                <button
                  type="button"
                  onClick={cancelLockedOrder}
                  title="关闭"
                >
                  <X size={17} />
                </button>
              </div>
              <div className="payment-order-summary">
                <span>订单号：{lockedPayOrder.orderId || lockedPayOrder.outTradeNo}</span>
                <strong>应付：{formatCurrency(lockedPayOrder.payPrice)}</strong>
              </div>
              <div className="payment-method-list">
                <button type="button" onClick={() => confirmDemoPayment('微信支付')} disabled={isSettlingOrder}>
                  <span className="payment-icon wechat">微</span>
                  <div>
                    <strong>微信支付</strong>
                    <span>{isSettlingOrder ? '正在确认支付...' : '使用微信完成支付，进入订单页'}</span>
                  </div>
                </button>
                <button type="button" onClick={() => confirmDemoPayment('支付宝支付')} disabled={isSettlingOrder}>
                  <span className="payment-icon alipay">支</span>
                  <div>
                    <strong>支付宝支付</strong>
                    <span>{isSettlingOrder ? '正在确认支付...' : '使用支付宝完成支付，进入订单页'}</span>
                  </div>
                </button>
              </div>
              {settlementError && (
                <div className="payment-error">支付失败：{settlementError}</div>
              )}
            </section>
          )}
        </div>
      )}
    </main>
  );
}

