import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'

export type Locale = 'zh-CN' | 'zh-TW' | 'fr' | 'ja' | 'en'

export const LOCALES: { id: Locale; label: string }[] = [
  { id: 'zh-CN', label: '简体中文' },
  { id: 'zh-TW', label: '繁體中文' },
  { id: 'ja', label: '日本語' },
  { id: 'fr', label: 'Français' },
  { id: 'en', label: 'English' },
]

const STORAGE_KEY = 'amygo-locale'

/** Keys that may intentionally stay identical to English (brand, codes). */
const ALLOW_SAME_AS_EN = new Set(['brand'])

const en = {
  brand: 'AMYGO RaaS',
  opsConsole: 'Ops Console',
  overview: 'Overview',
  tasks: 'Tasks',
  robots: 'Robots',
  bindings: 'Bindings',
  audit: 'Audit',
  events: 'Events',
  createDelivery: 'Create delivery task',
  createCleaning: 'Create cleaning task',
  createHotel: 'Create hotel delivery',
  working: 'Working…',
  refresh: 'Refresh',
  tasksHint: 'Cancel / fail / restart · timeline',
  noTasks: 'No tasks yet. Create a delivery task.',
  unassigned: 'unassigned',
  attempt: 'attempt',
  timeline: 'Timeline',
  cancel: 'Cancel',
  fail: 'Fail',
  restart: 'Restart',
  robotsHint: 'Multi-dimensional operational state',
  bindingsTitle: 'Vendor bindings',
  bindingsHint: 'Opaque device refs · MOCK_BOUND until formal docs',
  auditHint: 'High-risk and lifecycle actions',
  taskTimeline: 'Task timeline',
  timelineHint: 'Events + audit for selected task',
  allEvents: 'All loaded events',
  apiError: 'API error: {msg}. Is control-plane on :8080?',
  navAria: 'Product navigation',
  crumbAria: 'Breadcrumb',
  railFoot: 'MVP · Simulator',
  themeDark: 'Dark',
  themeLight: 'Light',
  themeToDark: 'Switch to dark mode',
  themeToLight: 'Switch to light mode',
  language: 'Language',
  bat: 'bat',
  loc: 'loc',
  safety: 'safety',
  mnt: 'mnt',
  lease: 'lease',
  home: 'Home',
  homeHeroTitle: 'See every robot mission — live.',
  homeHeroLead:
    'Create delivery tasks, watch Simulator / PUDU / KEENON mocks, and open the Fleet Dashboard for live ops visualization.',
  homeCtaOps: 'Open Ops Console',
  homeCtaCommand: 'Fleet Dashboard',
  homeCtaDemoTask: 'Fire demo delivery',
  homeCtaCleanTask: 'Fire demo cleaning',
  homeCtaHotelTask: 'Fire demo hotel',
  homeBannerAria: 'Robot operations scenes',
  slidePrev: 'Previous scene',
  slideNext: 'Next scene',
  sceneSwitcher: 'Scenario',
  sceneRestaurant: 'Restaurant delivery',
  sceneRestaurantCap: 'Table-side service robots on a live dining floor.',
  sceneCleaning: 'Facility cleaning',
  sceneCleaningCap: 'Autonomous scrubbers covering lobbies and corridors.',
  sceneHotel: 'Hotel delivery',
  sceneHotelCap: 'Cabin robots moving amenities floor-to-door.',
  sceneShow: 'Venue show',
  sceneShowCap: 'Humanoid show agents under stage supervision.',
  homeOpsTitle: 'Built for site operators',
  homeOpsLead: 'What your ops team watches every shift — not an engineering checklist.',
  valueLiveTitle: 'Live fleet picture',
  valueLiveBody: 'Online robots, active tasks, and site floor status in one wall.',
  valueRecoverTitle: 'Recover without guesswork',
  valueRecoverBody: 'Disconnect → reconnect → restart with audit when a mission stalls.',
  valueMultiTitle: 'Four scenes, one console',
  valueMultiBody: 'Restaurant, cleaning, hotel, and show — switch context in one click.',
  valueAuditTitle: 'Evidence for every action',
  valueAuditBody: 'Who dispatched, canceled, or intervened — ready for review.',
  commandCenter: 'Fleet Dashboard',
  backHome: 'Home',
  mockNotice: 'Mock demo — not a production vendor link',
  kpiRobots: 'Robots online',
  kpiRunning: 'Tasks active',
  kpiSuccess: 'Succeeded',
  kpiUnknown: 'Unknown cmds',
  floorMap: 'Site floor (mock)',
  liveTasks: 'Live tasks',
  liveEvents: 'Event ticker',
  demoFire: 'Demo: create delivery',
  openFullscreen: 'Focus wall',
  planLink: 'Dev plan 1.1 progress',
  adaptersLive: 'Adapters',
  reconnect: 'Reconnect',
} as const

export type MessageKey = keyof typeof en
type Dict = Record<MessageKey, string>

/** Every locale must provide a full dictionary — TypeScript enforces key coverage. */
function defineLocale(id: Locale, messages: Dict): Dict {
  if (id === 'en') return messages
  const leaks: string[] = []
  for (const key of Object.keys(en) as MessageKey[]) {
    if (ALLOW_SAME_AS_EN.has(key)) continue
    if (messages[key] === en[key]) leaks.push(key)
  }
  if (leaks.length > 0) {
    throw new Error(`[i18n] ${id} still uses English for: ${leaks.join(', ')}`)
  }
  return messages
}

const zhCN = defineLocale('zh-CN', {
  brand: 'AMYGO RaaS',
  opsConsole: '运营控制台',
  overview: '总览',
  tasks: '任务',
  robots: '机器人',
  bindings: '设备绑定',
  audit: '审计',
  events: '事件',
  createDelivery: '创建配送任务',
  createCleaning: '创建清洁任务',
  createHotel: '创建酒店配送',
  working: '处理中…',
  refresh: '刷新',
  tasksHint: '取消 / 失败 / 重启 · 时间线',
  noTasks: '暂无任务。请创建配送任务。',
  unassigned: '未分配',
  attempt: '尝试',
  timeline: '时间线',
  cancel: '取消',
  fail: '失败',
  restart: '重启',
  robotsHint: '多维运行状态',
  bindingsTitle: '厂商设备绑定',
  bindingsHint: '不透明设备引用 · 正式文档前为 MOCK_BOUND',
  auditHint: '高风险与生命周期操作',
  taskTimeline: '任务时间线',
  timelineHint: '所选任务的事件与审计',
  allEvents: '全部已加载事件',
  apiError: 'API 错误：{msg}。控制面是否在 :8080？',
  navAria: '产品导航',
  crumbAria: '面包屑',
  railFoot: 'MVP · 模拟器',
  themeDark: '深色',
  themeLight: '浅色',
  themeToDark: '切换到深色模式',
  themeToLight: '切换到浅色模式',
  language: '语言',
  bat: '电量',
  loc: '定位',
  safety: '安全',
  mnt: '维护',
  lease: '租约',
  home: '首页',
  homeHeroTitle: '每一场任务，现场可见。',
  homeHeroLead:
    '一键创建配送任务，观察 Simulator / PUDU / KEENON Mock，并打开指挥中心查看运营可视化。',
  homeCtaOps: '进入运营控制台',
  homeCtaCommand: '指挥中心',
  homeCtaDemoTask: '触发演示配送',
  homeCtaCleanTask: '触发演示清洁',
  homeCtaHotelTask: '触发演示酒店配送',
  homeBannerAria: '机器人运营场景',
  slidePrev: '上一场景',
  slideNext: '下一场景',
  sceneSwitcher: '场景切换',
  sceneRestaurant: '餐厅配送',
  sceneRestaurantCap: '餐桌服务机器人在营业餐厅现场运行。',
  sceneCleaning: '场地清洁',
  sceneCleaningCap: '自主清洁机覆盖大堂与走廊。',
  sceneHotel: '酒店配送',
  sceneHotelCap: '舱体机器人楼层到客房配送。',
  sceneShow: '场馆表演',
  sceneShowCap: '人形表演机器人在值守下彩排。',
  homeOpsTitle: '为现场运营而设计',
  homeOpsLead: '值守班次真正关心的画面 — 不是工程进度清单。',
  valueLiveTitle: '实时机队态势',
  valueLiveBody: '在线机器人、进行中任务与场地平面，一块大屏看清。',
  valueRecoverTitle: '异常可恢复',
  valueRecoverBody: '断网 → 重连 → 重启，全程留审计，不靠猜。',
  valueMultiTitle: '四类场景一套台',
  valueMultiBody: '餐厅、清洁、酒店、表演 — 一键切换运营语境。',
  valueAuditTitle: '操作可追溯',
  valueAuditBody: '谁下发、谁取消、谁干预 — 复盘有据。',
  commandCenter: '指挥中心',
  backHome: '首页',
  mockNotice: 'Mock 演示 — 非生产厂商实连',
  kpiRobots: '在线机器人',
  kpiRunning: '进行中任务',
  kpiSuccess: '已成功',
  kpiUnknown: '未知命令',
  floorMap: '场地平面（模拟）',
  liveTasks: '实时任务',
  liveEvents: '事件滚动',
  demoFire: '演示：创建配送',
  openFullscreen: '沉浸大屏',
  planLink: '开发计划 1.1 进度',
  adaptersLive: '适配器',
  reconnect: '重新连接',
})

const zhTW = defineLocale('zh-TW', {
  brand: 'AMYGO RaaS',
  opsConsole: '營運控制台',
  overview: '總覽',
  tasks: '任務',
  robots: '機器人',
  bindings: '裝置綁定',
  audit: '稽核',
  events: '事件',
  createDelivery: '建立配送任務',
  createCleaning: '建立清潔任務',
  createHotel: '建立飯店配送',
  working: '處理中…',
  refresh: '重新整理',
  tasksHint: '取消 / 失敗 / 重啟 · 時間軸',
  noTasks: '尚無任務。請建立配送任務。',
  unassigned: '未指派',
  attempt: '嘗試',
  timeline: '時間軸',
  cancel: '取消',
  fail: '失敗',
  restart: '重啟',
  robotsHint: '多維運行狀態',
  bindingsTitle: '廠商裝置綁定',
  bindingsHint: '不透明裝置參照 · 正式文件前為 MOCK_BOUND',
  auditHint: '高風險與生命週期操作',
  taskTimeline: '任務時間軸',
  timelineHint: '所選任務的事件與稽核',
  allEvents: '全部已載入事件',
  apiError: 'API 錯誤：{msg}。控制平面是否在 :8080？',
  navAria: '產品導覽',
  crumbAria: '麵包屑',
  railFoot: 'MVP · 模擬器',
  themeDark: '深色',
  themeLight: '淺色',
  themeToDark: '切換到深色模式',
  themeToLight: '切換到淺色模式',
  language: '語言',
  bat: '電量',
  loc: '定位',
  safety: '安全',
  mnt: '維護',
  lease: '租約',
  home: '首頁',
  homeHeroTitle: '每一場任務，現場可見。',
  homeHeroLead:
    '一鍵建立配送任務，觀察 Simulator / PUDU / KEENON Mock，並打開指揮中心查看營運可視化。',
  homeCtaOps: '進入營運控制台',
  homeCtaCommand: '指揮中心',
  homeCtaDemoTask: '觸發示範配送',
  homeCtaCleanTask: '觸發示範清潔',
  homeCtaHotelTask: '觸發示範飯店配送',
  homeBannerAria: '機器人營運場景',
  slidePrev: '上一場景',
  slideNext: '下一場景',
  sceneSwitcher: '場景切換',
  sceneRestaurant: '餐廳配送',
  sceneRestaurantCap: '餐桌服務機器人在營業餐廳現場運行。',
  sceneCleaning: '場地清潔',
  sceneCleaningCap: '自主清潔機覆蓋大廳與走廊。',
  sceneHotel: '飯店配送',
  sceneHotelCap: '艙體機器人樓層到客房配送。',
  sceneShow: '場館表演',
  sceneShowCap: '人形表演機器人在值守下彩排。',
  homeOpsTitle: '為現場營運而設計',
  homeOpsLead: '值守班次真正關心的畫面 — 不是工程進度清單。',
  valueLiveTitle: '即時機隊態勢',
  valueLiveBody: '線上機器人、進行中任務與場地平面，一塊大屏看清。',
  valueRecoverTitle: '異常可恢復',
  valueRecoverBody: '斷網 → 重連 → 重啟，全程留稽核，不靠猜。',
  valueMultiTitle: '四類場景一套台',
  valueMultiBody: '餐廳、清潔、飯店、表演 — 一鍵切換營運語境。',
  valueAuditTitle: '操作可追溯',
  valueAuditBody: '誰下發、誰取消、誰干預 — 複盤有據。',
  commandCenter: '指揮中心',
  backHome: '首頁',
  mockNotice: 'Mock 示範 — 非生產廠商實連',
  kpiRobots: '線上機器人',
  kpiRunning: '進行中任務',
  kpiSuccess: '已成功',
  kpiUnknown: '未知命令',
  floorMap: '場地平面（模擬）',
  liveTasks: '即時任務',
  liveEvents: '事件捲動',
  demoFire: '示範：建立配送',
  openFullscreen: '沉浸大屏',
  planLink: '開發計畫 1.1 進度',
  adaptersLive: '適配器',
  reconnect: '重新連線',
})

const ja = defineLocale('ja', {
  brand: 'AMYGO RaaS',
  opsConsole: '運用コンソール',
  overview: '概要',
  tasks: 'タスク',
  robots: 'ロボット',
  bindings: 'バインディング',
  audit: '監査',
  events: 'イベント',
  createDelivery: '配送タスクを作成',
  createCleaning: '清掃タスクを作成',
  createHotel: 'ホテル配送を作成',
  working: '処理中…',
  refresh: '更新',
  tasksHint: 'キャンセル / 失敗 / 再起動 · タイムライン',
  noTasks: 'タスクがありません。配送タスクを作成してください。',
  unassigned: '未割当',
  attempt: '試行',
  timeline: 'タイムライン',
  cancel: 'キャンセル',
  fail: '失敗',
  restart: '再起動',
  robotsHint: '多次元の運用状態',
  bindingsTitle: 'ベンダー機器バインディング',
  bindingsHint: '不透明な機器参照 · 正式文書前は MOCK_BOUND',
  auditHint: '高リスクとライフサイクル操作',
  taskTimeline: 'タスクタイムライン',
  timelineHint: '選択タスクのイベントと監査',
  allEvents: '読み込み済みイベント一覧',
  apiError: 'API エラー: {msg}。control-plane は :8080 ですか？',
  navAria: 'プロダクトナビ',
  crumbAria: 'パンくず',
  railFoot: 'MVP · シミュレータ',
  themeDark: 'ダーク',
  themeLight: 'ライト',
  themeToDark: 'ダークモードに切替',
  themeToLight: 'ライトモードに切替',
  language: '言語',
  bat: '電池',
  loc: '測位',
  safety: '安全',
  mnt: '保全',
  lease: 'リース',
  home: 'ホーム',
  homeHeroTitle: 'すべてのミッションを、現場で見える化。',
  homeHeroLead:
    '配送タスクを作成し、Simulator / PUDU / KEENON のモックを監視。フリートダッシュボードで運用を可視化します。',
  homeCtaOps: '運用コンソールを開く',
  homeCtaCommand: '指揮センター',
  homeCtaDemoTask: 'デモ配送を実行',
  homeCtaCleanTask: 'デモ清掃を実行',
  homeCtaHotelTask: 'デモホテル配送を実行',
  homeBannerAria: 'ロボット運用シーン',
  slidePrev: '前のシーン',
  slideNext: '次のシーン',
  sceneSwitcher: 'シーン切替',
  sceneRestaurant: 'レストラン配送',
  sceneRestaurantCap: '営業中のダイニングで配膳ロボットが稼働。',
  sceneCleaning: '施設清掃',
  sceneCleaningCap: 'ロビーと廊下を自律清掃機がカバー。',
  sceneHotel: 'ホテル配送',
  sceneHotelCap: '客室階へのキャビン配送ロボット。',
  sceneShow: '会場ショー',
  sceneShowCap: '有人監視下のヒューマノイド演出。',
  homeOpsTitle: '現場オペレーターのために',
  homeOpsLead: 'シフトで本当に見る画面 — 開発チェックリストではありません。',
  valueLiveTitle: 'ライブ艦隊ビュー',
  valueLiveBody: 'オンライン機体、進行中タスク、フロア状況を一望。',
  valueRecoverTitle: '推測なしの復旧',
  valueRecoverBody: '切断 → 再接続 → 再開。監査付きで止めない。',
  valueMultiTitle: '4シーン・1コンソール',
  valueMultiBody: '飲食・清掃・ホテル・ショーをワンクリック切替。',
  valueAuditTitle: '操作の証跡',
  valueAuditBody: '誰が投入・取消・介入したかをレビュー可能。',
  commandCenter: '指揮センター',
  backHome: 'ホーム',
  mockNotice: 'Mock デモ — 本番ベンダー接続ではありません',
  kpiRobots: 'オンライン機体',
  kpiRunning: '進行中タスク',
  kpiSuccess: '成功',
  kpiUnknown: '不明コマンド',
  floorMap: 'サイト平面（モック）',
  liveTasks: 'ライブタスク',
  liveEvents: 'イベントティッカー',
  demoFire: 'デモ：配送を作成',
  openFullscreen: 'ウォール表示',
  planLink: '開発計画 1.1 進捗',
  adaptersLive: 'アダプタ',
  reconnect: '再接続',
})

const fr = defineLocale('fr', {
  brand: 'AMYGO RaaS',
  opsConsole: 'Console Ops',
  overview: 'Vue d’ensemble',
  tasks: 'Tâches',
  robots: 'Parc robots',
  bindings: 'Liaisons',
  audit: 'Journal d’audit',
  events: 'Événements',
  createDelivery: 'Créer une livraison',
  createCleaning: 'Créer un nettoyage',
  createHotel: 'Créer une livraison hôtel',
  working: 'En cours…',
  refresh: 'Actualiser',
  tasksHint: 'Annuler / échec / relancer · chronologie',
  noTasks: 'Aucune tâche. Créez une livraison.',
  unassigned: 'non assigné',
  attempt: 'tentative',
  timeline: 'Chronologie',
  cancel: 'Annuler',
  fail: 'Échec',
  restart: 'Relancer',
  robotsHint: 'État opérationnel multidimensionnel',
  bindingsTitle: 'Liaisons fabricant',
  bindingsHint: 'Réfs opaque · MOCK_BOUND sans docs formels',
  auditHint: 'Actions à risque et cycle de vie',
  taskTimeline: 'Chronologie de tâche',
  timelineHint: 'Événements + audit de la tâche',
  allEvents: 'Tous les événements chargés',
  apiError: 'Erreur API : {msg}. Le control-plane est-il sur :8080 ?',
  navAria: 'Navigation produit',
  crumbAria: 'Fil d’Ariane',
  railFoot: 'MVP · Simulateur',
  themeDark: 'Sombre',
  themeLight: 'Clair',
  themeToDark: 'Passer en mode sombre',
  themeToLight: 'Passer en mode clair',
  language: 'Langue',
  bat: 'batt.',
  loc: 'loc.',
  safety: 'sécu.',
  mnt: 'maint.',
  lease: 'bail',
  home: 'Accueil',
  homeHeroTitle: 'Chaque mission robot, en direct.',
  homeHeroLead:
    'Créez des livraisons, suivez les mocks Simulator / PUDU / KEENON, et ouvrez le tableau de bord flotte.',
  homeCtaOps: 'Ouvrir la console Ops',
  homeCtaCommand: 'Tableau de bord flotte',
  homeCtaDemoTask: 'Lancer une livraison démo',
  homeCtaCleanTask: 'Lancer un nettoyage démo',
  homeCtaHotelTask: 'Lancer une livraison hôtel démo',
  homeBannerAria: 'Scènes d’exploitation robot',
  slidePrev: 'Scène précédente',
  slideNext: 'Scène suivante',
  sceneSwitcher: 'Scénario',
  sceneRestaurant: 'Livraison restaurant',
  sceneRestaurantCap: 'Robots de service en salle pendant le service.',
  sceneCleaning: 'Nettoyage de site',
  sceneCleaningCap: 'Autolaveuses couvrant halls et couloirs.',
  sceneHotel: 'Livraison hôtel',
  sceneHotelCap: 'Robots cabine étage → porte de chambre.',
  sceneShow: 'Spectacle de salle',
  sceneShowCap: 'Agents humanoïdes sous supervision scène.',
  homeOpsTitle: 'Conçu pour les opérateurs',
  homeOpsLead: 'Ce que l’équipe regarde en vacation — pas une checklist d’ingénierie.',
  valueLiveTitle: 'Vue flotte en direct',
  valueLiveBody: 'Robots en ligne, tâches actives et plan de site sur un mur.',
  valueRecoverTitle: 'Reprise sans intuition',
  valueRecoverBody: 'Déconnexion → reconnexion → relance, avec audit.',
  valueMultiTitle: 'Quatre scènes, une console',
  valueMultiBody: 'Restaurant, nettoyage, hôtel, show — un clic pour changer.',
  valueAuditTitle: 'Preuves d’action',
  valueAuditBody: 'Qui a dispatché, annulé ou intervenu — prêt pour revue.',
  commandCenter: 'Tableau de bord flotte',
  backHome: 'Accueil',
  mockNotice: 'Démo Mock — pas un lien fabricant de production',
  kpiRobots: 'Robots en ligne',
  kpiRunning: 'Tâches actives',
  kpiSuccess: 'Réussies',
  kpiUnknown: 'Cmd inconnues',
  floorMap: 'Plan de site (mock)',
  liveTasks: 'Tâches live',
  liveEvents: 'Fil d’événements',
  demoFire: 'Démo : créer une livraison',
  openFullscreen: 'Mur immersif',
  planLink: 'Avancement plan 1.1',
  adaptersLive: 'Adaptateurs',
  reconnect: 'Reconnecter',
})

const catalogs: Record<Locale, Dict> = {
  en: { ...en },
  'zh-CN': zhCN,
  'zh-TW': zhTW,
  ja,
  fr,
}

/** Used by CI / local check — ensures every locale covers every English key. */
export function assertI18nCoverage(): string[] {
  const problems: string[] = []
  const keys = Object.keys(en) as MessageKey[]
  for (const [locale, dict] of Object.entries(catalogs) as [Locale, Dict][]) {
    for (const key of keys) {
      if (!dict[key]) problems.push(`${locale} missing ${key}`)
    }
    if (locale === 'en') continue
    for (const key of keys) {
      if (ALLOW_SAME_AS_EN.has(key)) continue
      if (dict[key] === en[key]) problems.push(`${locale} still English: ${key}`)
    }
  }
  return problems
}

// Fail fast in module init so `vite build` / `tsc` load catches drift.
const i18nProblems = assertI18nCoverage()
if (i18nProblems.length > 0) {
  throw new Error(`[i18n] locale drift:\n- ${i18nProblems.join('\n- ')}`)
}

function readStoredLocale(): Locale {
  try {
    const v = localStorage.getItem(STORAGE_KEY)
    if (v === 'zh-CN' || v === 'zh-TW' || v === 'fr' || v === 'ja' || v === 'en') return v
  } catch {
    /* ignore */
  }
  const nav = typeof navigator !== 'undefined' ? navigator.language : ''
  if (nav.startsWith('zh-TW') || nav.startsWith('zh-HK') || nav.startsWith('zh-Hant')) return 'zh-TW'
  if (nav.startsWith('zh')) return 'zh-CN'
  if (nav.startsWith('ja')) return 'ja'
  if (nav.startsWith('fr')) return 'fr'
  return 'zh-CN'
}

type Ctx = {
  locale: Locale
  setLocale: (l: Locale) => void
  t: (key: MessageKey, vars?: Record<string, string | number>) => string
}

const I18nCtx = createContext<Ctx | null>(null)

export function I18nProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>('zh-CN')

  useEffect(() => {
    const next = readStoredLocale()
    setLocaleState(next)
    document.documentElement.lang = next
  }, [])

  const setLocale = useCallback((l: Locale) => {
    localStorage.setItem(STORAGE_KEY, l)
    setLocaleState(l)
    document.documentElement.lang = l
  }, [])

  const t = useCallback(
    (key: MessageKey, vars?: Record<string, string | number>) => {
      const dict = catalogs[locale] || catalogs.en
      let s = dict[key] ?? catalogs.en[key] ?? key
      if (vars) {
        for (const [k, v] of Object.entries(vars)) {
          s = s.replaceAll(`{${k}}`, String(v))
        }
      }
      return s
    },
    [locale],
  )

  const value = useMemo(() => ({ locale, setLocale, t }), [locale, setLocale, t])
  return <I18nCtx.Provider value={value}>{children}</I18nCtx.Provider>
}

export function useI18n() {
  const ctx = useContext(I18nCtx)
  if (!ctx) throw new Error('useI18n outside provider')
  return ctx
}
