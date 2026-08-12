import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import ProductShell from '../components/ProductShell'
import ThemeToggle from '../components/ThemeToggle'
import LocaleSwitcher from '../components/LocaleSwitcher'
import { useI18n } from '../lib/i18n'
import { API, apiHeaders, createCleaningTask, createDeliveryTask, createHotelTask, reconnectRobot, type Robot, type Task } from '../lib/api'

type TimelineItem = Record<string, unknown>
type AuditRow = Record<string, unknown>

function statusBadge(status: string) {
  const s = status.toUpperCase()
  if (['SUCCEEDED', 'ONLINE', 'AVAILABLE', 'IDLE', 'NORMAL', 'LOCALIZED', 'OK'].includes(s)) return 'ok'
  if (['FAILED', 'CANCELED', 'OFFLINE', 'ERROR', 'E_STOP', 'CRITICAL', 'BLOCKED', 'LOST'].includes(s))
    return 'bad'
  return 'warn'
}

export default function OpsConsole() {
  const { t } = useI18n()
  const [section, setSection] = useState<
    'overview' | 'tasks' | 'robots' | 'bindings' | 'audit' | 'events'
  >('overview')
  const [robots, setRobots] = useState<Robot[]>([])
  const [tasks, setTasks] = useState<Task[]>([])
  const [events, setEvents] = useState<unknown[]>([])
  const [audit, setAudit] = useState<AuditRow[]>([])
  const [bindings, setBindings] = useState<Record<string, unknown>[]>([])
  const [selectedTaskId, setSelectedTaskId] = useState<string | null>(null)
  const [timeline, setTimeline] = useState<TimelineItem[]>([])
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const refresh = useCallback(async () => {
    try {
      const [r, tsk, e, a, b] = await Promise.all([
        fetch(`${API}/robots`, { headers: apiHeaders }).then((x) => x.json()),
        fetch(`${API}/tasks`, { headers: apiHeaders }).then((x) => x.json()),
        fetch(`${API}/events`, { headers: apiHeaders }).then((x) => x.json()),
        fetch(`${API}/audit`, { headers: apiHeaders }).then((x) => x.json()),
        fetch(`${API}/bindings`, { headers: apiHeaders }).then((x) => x.json()),
      ])
      setRobots(r)
      setTasks(tsk)
      setEvents(e)
      setAudit(a)
      setBindings(b)
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load')
    }
  }, [])

  const loadTimeline = useCallback(async (taskId: string) => {
    const rows = await fetch(`${API}/tasks/${taskId}/timeline`, { headers: apiHeaders }).then((x) =>
      x.json(),
    )
    setTimeline(rows)
    setSelectedTaskId(taskId)
  }, [])

  useEffect(() => {
    refresh()
    const id = setInterval(refresh, 1500)
    return () => clearInterval(id)
  }, [refresh])

  async function createDelivery() {
    setBusy(true)
    try {
      await createDeliveryTask('MVP vertical-loop demo')
      setSection('tasks')
      await refresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Create failed')
    } finally {
      setBusy(false)
    }
  }

  async function createCleaning() {
    setBusy(true)
    try {
      await createCleaningTask('MVP cleaning demo')
      setSection('tasks')
      await refresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Create cleaning failed')
    } finally {
      setBusy(false)
    }
  }

  async function createHotel() {
    setBusy(true)
    try {
      await createHotelTask('MVP hotel demo')
      setSection('tasks')
      await refresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Create hotel failed')
    } finally {
      setBusy(false)
    }
  }

  async function act(taskId: string, action: 'cancel' | 'fail' | 'restart') {
    setBusy(true)
    try {
      const res = await fetch(`${API}/tasks/${taskId}/${action}`, {
        method: 'POST',
        headers: apiHeaders,
        body: action === 'fail' ? JSON.stringify({ reason: 'console_operator' }) : undefined,
      })
      if (!res.ok) {
        const text = await res.text()
        throw new Error(text || res.statusText)
      }
      await refresh()
      await loadTimeline(taskId)
    } catch (err) {
      setError(err instanceof Error ? err.message : `${action} failed`)
    } finally {
      setBusy(false)
    }
  }

  async function onReconnect(robotId: string) {
    setBusy(true)
    try {
      const res = await reconnectRobot(robotId)
      if (!res.ok) {
        throw new Error(await res.text())
      }
      setSection('robots')
      await refresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'reconnect failed')
    } finally {
      setBusy(false)
    }
  }

  const sectionLabel = useMemo(() => {
    const map = {
      overview: t('overview'),
      tasks: t('tasks'),
      robots: t('robots'),
      bindings: t('bindings'),
      audit: t('audit'),
      events: t('events'),
    } as const
    return map[section]
  }, [section, t])

  const railLinks = useMemo(
    () => [
      { id: 'overview', label: t('overview'), active: section === 'overview', onClick: () => setSection('overview') },
      { id: 'tasks', label: t('tasks'), active: section === 'tasks', onClick: () => setSection('tasks') },
      { id: 'robots', label: t('robots'), active: section === 'robots', onClick: () => setSection('robots') },
      { id: 'bindings', label: t('bindings'), active: section === 'bindings', onClick: () => setSection('bindings') },
      { id: 'audit', label: t('audit'), active: section === 'audit', onClick: () => setSection('audit') },
      { id: 'events', label: t('events'), active: section === 'events', onClick: () => setSection('events') },
    ],
    [section, t],
  )

  const crumbs = [{ label: t('brand') }, { label: t('opsConsole') }, { label: sectionLabel }]

  return (
    <ProductShell
      crumbs={crumbs}
      railLinks={railLinks}
      actions={
        <>
          <Link className="nav-text-link" to="/">
            {t('home')}
          </Link>
          <Link className="nav-text-link" to="/demo/command-center">
            {t('commandCenter')}
          </Link>
          <span className="badge ok">tenant-demo</span>
          <LocaleSwitcher />
          <ThemeToggle />
        </>
      }
    >
      {error && <div className="error-banner">{t('apiError', { msg: error })}</div>}

      <div className="actions">
        <button type="button" disabled={busy} onClick={createDelivery}>
          {busy ? t('working') : t('createDelivery')}
        </button>
        <button type="button" className="secondary" disabled={busy} onClick={createCleaning}>
          {busy ? t('working') : t('createCleaning')}
        </button>
        <button type="button" className="secondary" disabled={busy} onClick={createHotel}>
          {busy ? t('working') : t('createHotel')}
        </button>
        <button type="button" className="secondary" onClick={refresh}>
          {t('refresh')}
        </button>
      </div>

      <div className="workspace-body">
        <div className="panel-main">
          {(section === 'overview' || section === 'tasks') && (
            <section className="card">
              <div className="section-head">
                <div>
                  <h2>{t('tasks')}</h2>
                  <p>{t('tasksHint')}</p>
                </div>
              </div>
              <ul className="list">
                {tasks.length === 0 && <li className="muted">{t('noTasks')}</li>}
                {tasks.map((task) => (
                  <li key={task.id} className="list-row">
                    <div>
                      <span className={`badge ${statusBadge(task.status)}`}>{task.status}</span>{' '}
                      <strong>{task.taskType}</strong>
                      {task.attemptNo != null && (
                        <span>
                          {' '}
                          · {t('attempt')} {task.attemptNo}
                        </span>
                      )}
                    </div>
                    <span>{task.assignedRobotId || t('unassigned')}</span>
                    <code>{task.id}</code>
                    <div className="actions" style={{ margin: '8px 0 0' }}>
                      <button type="button" className="ghost" disabled={busy} onClick={() => loadTimeline(task.id)}>
                        {t('timeline')}
                      </button>
                      <button type="button" className="secondary" disabled={busy} onClick={() => act(task.id, 'cancel')}>
                        {t('cancel')}
                      </button>
                      <button type="button" className="secondary" disabled={busy} onClick={() => act(task.id, 'fail')}>
                        {t('fail')}
                      </button>
                      <button type="button" className="secondary" disabled={busy} onClick={() => act(task.id, 'restart')}>
                        {t('restart')}
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            </section>
          )}

          {(section === 'overview' || section === 'robots') && (
            <section className="card">
              <div className="section-head">
                <div>
                  <h2>{t('robots')}</h2>
                  <p>{t('robotsHint')}</p>
                </div>
              </div>
              <ul className="list">
                {robots.map((r) => (
                  <li key={r.id} className="list-row">
                    <strong>{r.displayName}</strong>
                    <span className="muted">
                      {r.adapterType || '—'} · {r.modelProfile || '—'}
                    </span>
                    <span>
                      <span className={`badge ${statusBadge(r.connectivityStatus)}`}>{r.connectivityStatus}</span>{' '}
                      <span className={`badge ${statusBadge(r.operationalStatus)}`}>{r.operationalStatus}</span>{' '}
                      <span className={`badge ${statusBadge(r.missionStatus)}`}>{r.missionStatus}</span>
                    </span>
                    {r.leaseTaskId && (
                      <em>
                        {t('lease')}: {r.leaseTaskId}
                      </em>
                    )}
                    {r.connectivityStatus === 'OFFLINE' && (
                      <div className="row-actions">
                        <button type="button" className="secondary" disabled={busy} onClick={() => onReconnect(r.id)}>
                          {t('reconnect')}
                        </button>
                      </div>
                    )}
                  </li>
                ))}
              </ul>
            </section>
          )}

          {(section === 'overview' || section === 'bindings') && (
            <section className="card">
              <div className="section-head">
                <div>
                  <h2>{t('bindingsTitle')}</h2>
                  <p>{t('bindingsHint')}</p>
                </div>
              </div>
              <pre className="event-pre">{JSON.stringify(bindings.slice(0, 20), null, 2)}</pre>
            </section>
          )}

          {(section === 'overview' || section === 'audit') && (
            <section className="card">
              <div className="section-head">
                <div>
                  <h2>{t('audit')}</h2>
                  <p>{t('auditHint')}</p>
                </div>
              </div>
              <pre className="event-pre">{JSON.stringify(audit.slice(0, 30), null, 2)}</pre>
            </section>
          )}
        </div>

        <aside className="side-panel">
          <div>
            <h3>
              {selectedTaskId ? `${t('timeline')} · ${selectedTaskId.slice(-8)}` : t('taskTimeline')}
            </h3>
            <p className="muted" style={{ margin: '0 0 10px', fontSize: '0.85rem' }}>
              {t('timelineHint')}
            </p>
            <pre className="event-pre">
              {JSON.stringify(timeline.length ? timeline : events.slice(-12), null, 2)}
            </pre>
          </div>
          {section === 'events' && (
            <div>
              <h3>{t('allEvents')}</h3>
              <pre className="event-pre">{JSON.stringify(events, null, 2)}</pre>
            </div>
          )}
        </aside>
      </div>
    </ProductShell>
  )
}
