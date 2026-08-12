import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import BrandLogo from '../components/BrandLogo'
import LocaleSwitcher from '../components/LocaleSwitcher'
import ThemeToggle from '../components/ThemeToggle'
import { useI18n } from '../lib/i18n'
import {
  createCleaningTask,
  createDeliveryTask,
  createHotelTask,
  fetchFleet,
  reconnectRobot,
  type Robot,
  type Task,
} from '../lib/api'
import { DEMO_SCENES, type DemoSceneId } from '../lib/scenes'

function robotPoint(id: string, index: number, sceneSeed: number) {
  const seed = [...id].reduce((a, c) => a + c.charCodeAt(0), 0) + index * 17 + sceneSeed
  const x = 12 + (seed % 76)
  const y = 18 + ((seed * 3) % 64)
  return { x, y }
}

function statusTone(status: string) {
  const s = status.toUpperCase()
  if (['SUCCEEDED', 'ONLINE', 'AVAILABLE', 'IDLE', 'NORMAL'].includes(s)) return 'ok'
  if (['FAILED', 'CANCELED', 'OFFLINE', 'ERROR', 'NEEDS_INTERVENTION'].includes(s)) return 'bad'
  return 'warn'
}

function parseScene(raw: string | null): DemoSceneId {
  const hit = DEMO_SCENES.find((s) => s.id === raw)
  return hit ? hit.id : 'restaurant'
}

export default function CommandCenterPage() {
  const { t } = useI18n()
  const [params, setParams] = useSearchParams()
  const sceneId = parseScene(params.get('scene'))
  const scene = DEMO_SCENES.find((s) => s.id === sceneId) ?? DEMO_SCENES[0]

  const [robots, setRobots] = useState<Robot[]>([])
  const [tasks, setTasks] = useState<Task[]>([])
  const [events, setEvents] = useState<unknown[]>([])
  const [metrics, setMetrics] = useState<Record<string, unknown>>({})
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [clock, setClock] = useState(() => new Date())

  const refresh = useCallback(async () => {
    try {
      const data = await fetchFleet()
      setRobots(data.robots)
      setTasks(data.tasks)
      setEvents(data.events)
      setMetrics(data.metrics)
      setError(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'load failed')
    }
  }, [])

  useEffect(() => {
    refresh()
    const poll = setInterval(refresh, 1200)
    const tick = setInterval(() => setClock(new Date()), 1000)
    return () => {
      clearInterval(poll)
      clearInterval(tick)
    }
  }, [refresh])

  const sceneRobots = useMemo(() => {
    const matched = robots.filter(scene.robotMatch)
    return matched.length > 0 ? matched : robots
  }, [robots, scene])

  const sceneTasks = useMemo(() => {
    const matched = tasks.filter(scene.taskMatch)
    return matched.length > 0 ? matched : tasks
  }, [tasks, scene])

  const kpis = useMemo(() => {
    const online = sceneRobots.filter((r) => r.connectivityStatus === 'ONLINE').length
    const active = sceneTasks.filter((t) =>
      ['QUEUED', 'ASSIGNED', 'DISPATCHING', 'RUNNING', 'NEEDS_INTERVENTION'].includes(t.status),
    ).length
    const succeeded = sceneTasks.filter((t) => t.status === 'SUCCEEDED').length
    const unknown =
      typeof metrics.unknownCommands === 'number'
        ? metrics.unknownCommands
        : Number(metrics.unknownCommands || 0)
    return { online, active, succeeded, unknown, total: sceneRobots.length }
  }, [sceneRobots, sceneTasks, metrics])

  const adapters = useMemo(() => {
    const set = new Set(sceneRobots.map((r) => r.adapterType || '—'))
    return [...set]
  }, [sceneRobots])

  function setScene(id: DemoSceneId) {
    setParams({ scene: id }, { replace: true })
  }

  async function fireDemo() {
    setBusy(true)
    try {
      if (scene.taskType === 'CLEANING') await createCleaningTask('cc-clean')
      else if (scene.taskType === 'HOTEL_DELIVERY') await createHotelTask('cc-hotel')
      else if (scene.taskType === 'DELIVERY') await createDeliveryTask('cc-delivery')
      await refresh()
    } finally {
      setBusy(false)
    }
  }

  async function onReconnect(robotId: string) {
    setBusy(true)
    try {
      await reconnectRobot(robotId)
      await refresh()
    } finally {
      setBusy(false)
    }
  }

  const sceneSeed = scene.id.charCodeAt(0) * 11

  return (
    <div className="cc-page" data-scene={scene.id}>
      <div className="cc-bg" style={{ backgroundImage: `url(${scene.image})` }} aria-hidden />
      <div className="cc-bg-veil" aria-hidden />

      <header className="cc-top">
        <div className="cc-brand">
          <BrandLogo />
          <h1 className="cc-title">{t('commandCenter')}</h1>
          <span className="badge warn">{t('mockNotice')}</span>
        </div>
        <div className="cc-top-right">
          <time className="cc-clock">{clock.toLocaleTimeString()}</time>
          {scene.taskType !== 'SHOW' && (
            <button type="button" className="secondary" disabled={busy} onClick={fireDemo}>
              {busy ? t('working') : t('demoFire')}
            </button>
          )}
          <Link className="nav-text-link" to="/ops">
            {t('opsConsole')}
          </Link>
          <LocaleSwitcher />
          <ThemeToggle />
        </div>
      </header>

      <nav className="cc-scenes" aria-label={t('sceneSwitcher')}>
        {DEMO_SCENES.map((s) => (
          <button
            key={s.id}
            type="button"
            className={`cc-scene-tab${s.id === scene.id ? ' active' : ''}`}
            onClick={() => setScene(s.id)}
          >
            <span className="cc-scene-thumb" style={{ backgroundImage: `url(${s.image})` }} />
            <span className="cc-scene-meta">
              <strong>{t(s.titleKey)}</strong>
              <em>{t(s.captionKey)}</em>
            </span>
          </button>
        ))}
      </nav>

      {error && <div className="error-banner cc-error">{t('apiError', { msg: error })}</div>}

      <section className="cc-kpis">
        <div className="cc-kpi">
          <span className="cc-kpi-label">{t('kpiRobots')}</span>
          <strong>
            {kpis.online}
            <em>/{kpis.total}</em>
          </strong>
        </div>
        <div className="cc-kpi">
          <span className="cc-kpi-label">{t('kpiRunning')}</span>
          <strong>{kpis.active}</strong>
        </div>
        <div className="cc-kpi">
          <span className="cc-kpi-label">{t('kpiSuccess')}</span>
          <strong>{kpis.succeeded}</strong>
        </div>
        <div className="cc-kpi">
          <span className="cc-kpi-label">{t('kpiUnknown')}</span>
          <strong>{kpis.unknown}</strong>
        </div>
      </section>

      <div className="cc-grid">
        <section className="cc-panel cc-map">
          <div className="cc-panel-head">
            <h2>
              {t('floorMap')} · {t(scene.titleKey)}
            </h2>
            <span className="muted">site-demo / {scene.id}</span>
          </div>
          <svg className="cc-floor" viewBox="0 0 100 100" role="img" aria-label={t('floorMap')}>
            <defs>
              <pattern id="grid" width="10" height="10" patternUnits="userSpaceOnUse">
                <path d="M 10 0 L 0 0 0 10" fill="none" stroke="currentColor" strokeOpacity="0.12" />
              </pattern>
            </defs>
            <rect width="100" height="100" fill="url(#grid)" className="cc-floor-bg" />
            {scene.floor.zones.map((z) => (
              <g key={z.label}>
                <rect
                  x={z.x}
                  y={z.y}
                  width={z.w}
                  height={z.h}
                  rx="2"
                  className={`cc-zone${z.wide ? ' wide' : ''}`}
                />
                <text x={z.x + 4} y={z.y + 8} className="cc-zone-label">
                  {z.label}
                </text>
              </g>
            ))}
            {sceneRobots.map((r, i) => {
              const p = robotPoint(r.id, i, sceneSeed)
              const busyBot = Boolean(r.leaseTaskId) || r.missionStatus === 'EXECUTING'
              return (
                <g
                  key={r.id}
                  className={`cc-bot${busyBot ? ' busy' : ''}${r.connectivityStatus !== 'ONLINE' ? ' offline' : ''}`}
                >
                  <circle cx={p.x} cy={p.y} r="2.4" className="cc-bot-pulse" />
                  <circle cx={p.x} cy={p.y} r="1.6" className="cc-bot-dot" />
                  <text x={p.x + 2.2} y={p.y + 0.6} className="cc-bot-label">
                    {(r.adapterType || 'BOT').slice(0, 6)}
                  </text>
                </g>
              )
            })}
          </svg>
          <div className="cc-adapters">
            <span className="muted">{t('adaptersLive')}:</span>
            {adapters.map((a) => (
              <span key={a} className="badge ok">
                {a}
              </span>
            ))}
            {sceneRobots.some((r) => r.connectivityStatus === 'OFFLINE') && (
              <div className="cc-offline-actions">
                {sceneRobots
                  .filter((r) => r.connectivityStatus === 'OFFLINE')
                  .map((r) => (
                    <button
                      key={r.id}
                      type="button"
                      className="secondary"
                      disabled={busy}
                      onClick={() => onReconnect(r.id)}
                    >
                      {t('reconnect')}: {r.displayName}
                    </button>
                  ))}
              </div>
            )}
          </div>
        </section>

        <section className="cc-panel">
          <div className="cc-panel-head">
            <h2>{t('liveTasks')}</h2>
          </div>
          <ul className="cc-feed">
            {sceneTasks.slice(0, 14).map((task) => (
              <li key={task.id}>
                <span className={`badge ${statusTone(task.status)}`}>{task.status}</span>
                <strong>{task.taskType}</strong>
                <code>{task.assignedRobotId || '—'}</code>
              </li>
            ))}
            {sceneTasks.length === 0 && <li className="muted">{t('noTasks')}</li>}
          </ul>
        </section>

        <section className="cc-panel cc-events">
          <div className="cc-panel-head">
            <h2>{t('liveEvents')}</h2>
          </div>
          <ul className="cc-feed ticker">
            {[...events].reverse().slice(0, 18).map((ev, i) => {
              const e = ev as Record<string, unknown>
              return (
                <li key={String(e.eventId || i)}>
                  <span className="muted">{String(e.source || '')}</span>
                  <strong>{String(e.eventType || '')}</strong>
                  <code>{String(e.robotId || '').slice(-8)}</code>
                </li>
              )
            })}
          </ul>
        </section>
      </div>
    </div>
  )
}
