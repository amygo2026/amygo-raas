import { useCallback, useEffect, useState } from 'react'
import './App.css'

type Robot = {
  id: string
  displayName: string
  operationalStatus: string
  missionStatus: string
  connectivityStatus: string
  leaseTaskId?: string
}

type Task = {
  id: string
  status: string
  taskType: string
  assignedRobotId?: string
  payload: Record<string, unknown>
  updatedAt: string
}

const API = 'http://localhost:8080/api/v1'
const headers = {
  'Content-Type': 'application/json',
  'X-Tenant-Id': 'tenant-demo',
  'X-Site-Id': 'site-demo',
  'X-Actor-Id': 'console-user',
}

function App() {
  const [robots, setRobots] = useState<Robot[]>([])
  const [tasks, setTasks] = useState<Task[]>([])
  const [events, setEvents] = useState<unknown[]>([])
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const refresh = useCallback(async () => {
    try {
      const [r, t, e] = await Promise.all([
        fetch(`${API}/robots`, { headers }).then((x) => x.json()),
        fetch(`${API}/tasks`, { headers }).then((x) => x.json()),
        fetch(`${API}/events`, { headers }).then((x) => x.json()),
      ])
      setRobots(r)
      setTasks(t)
      setEvents(e)
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load')
    }
  }, [])

  useEffect(() => {
    refresh()
    const id = setInterval(refresh, 1500)
    return () => clearInterval(id)
  }, [refresh])

  async function createDelivery() {
    setBusy(true)
    try {
      await fetch(`${API}/tasks`, {
        method: 'POST',
        headers,
        body: JSON.stringify({
          taskType: 'DELIVERY',
          payload: {
            pickupStationId: 'pickup-1',
            dropoffStationId: 'table-12',
            note: 'MVP vertical-loop demo',
          },
        }),
      })
      await refresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Create failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page">
      <header>
        <p className="brand">AMYGO RaaS</p>
        <h1>Ops Console — MVP Loop</h1>
        <p className="sub">
          Simulator → Task → Scheduler → Adapter → Event
        </p>
      </header>

      {error && <div className="error">API error: {error}. Is control-plane on :8080?</div>}

      <section className="actions">
        <button disabled={busy} onClick={createDelivery}>
          {busy ? 'Creating…' : 'Create delivery task'}
        </button>
        <button className="ghost" onClick={refresh}>Refresh</button>
      </section>

      <section className="grid">
        <div>
          <h2>Robots</h2>
          <ul>
            {robots.map((r) => (
              <li key={r.id}>
                <strong>{r.displayName}</strong>
                <span>{r.connectivityStatus} / {r.operationalStatus} / {r.missionStatus}</span>
                {r.leaseTaskId && <em>lease: {r.leaseTaskId}</em>}
              </li>
            ))}
          </ul>
        </div>
        <div>
          <h2>Tasks</h2>
          <ul>
            {tasks.map((t) => (
              <li key={t.id}>
                <strong>{t.status}</strong>
                <span>{t.taskType} · {t.assignedRobotId || 'unassigned'}</span>
                <code>{t.id}</code>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section>
        <h2>Recent events</h2>
        <pre>{JSON.stringify(events.slice(-12), null, 2)}</pre>
      </section>
    </div>
  )
}

export default App
