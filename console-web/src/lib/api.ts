/** Shared API base — follows page hostname for LAN access. */
export const API_HOST =
  (import.meta.env.VITE_API_HOST as string | undefined)?.trim() ||
  (typeof window !== 'undefined' ? window.location.hostname : '127.0.0.1')

export const API = `http://${API_HOST}:8080/api/v1`

export const apiHeaders = {
  'Content-Type': 'application/json',
  'X-Tenant-Id': 'tenant-demo',
  'X-Site-Id': 'site-demo',
  'X-Actor-Id': 'console-user',
}

export type Robot = {
  id: string
  displayName: string
  adapterType?: string
  modelProfile?: string
  operationalStatus: string
  missionStatus: string
  connectivityStatus: string
  batteryStatus?: string
  localizationStatus?: string
  safetyStatus?: string
  maintenanceStatus?: string
  leaseTaskId?: string
}

export type Task = {
  id: string
  status: string
  taskType: string
  attemptNo?: number
  assignedRobotId?: string
  payload: Record<string, unknown>
  updatedAt: string
}

export async function fetchFleet() {
  const [robots, tasks, events, audit, bindings, metrics] = await Promise.all([
    fetch(`${API}/robots`, { headers: apiHeaders }).then((x) => x.json()),
    fetch(`${API}/tasks`, { headers: apiHeaders }).then((x) => x.json()),
    fetch(`${API}/events`, { headers: apiHeaders }).then((x) => x.json()),
    fetch(`${API}/audit`, { headers: apiHeaders }).then((x) => x.json()),
    fetch(`${API}/bindings`, { headers: apiHeaders }).then((x) => x.json()),
    fetch(`${API}/ops/metrics`, { headers: apiHeaders }).then((x) => x.json()).catch(() => ({})),
  ])
  return {
    robots: robots as Robot[],
    tasks: tasks as Task[],
    events: events as unknown[],
    audit: audit as Record<string, unknown>[],
    bindings: bindings as Record<string, unknown>[],
    metrics: metrics as Record<string, unknown>,
  }
}

export async function createDeliveryTask(note = 'demo') {
  return createTask('DELIVERY', {
    pickupStationId: 'pickup-1',
    dropoffStationId: 'table-12',
    note,
  })
}

export async function createCleaningTask(note = 'demo') {
  return createTask('CLEANING', {
    zoneId: 'lobby-A',
    note,
  })
}

export async function createHotelTask(note = 'demo') {
  return createTask('HOTEL_DELIVERY', {
    roomNumber: '1208',
    floor: 12,
    compartment: 'A',
    note,
  })
}

export async function createTask(taskType: string, payload: Record<string, unknown>) {
  return fetch(`${API}/tasks`, {
    method: 'POST',
    headers: apiHeaders,
    body: JSON.stringify({ taskType, payload }),
  })
}

export async function reconnectRobot(robotId: string) {
  return fetch(`${API}/robots/${encodeURIComponent(robotId)}/reconnect`, {
    method: 'POST',
    headers: apiHeaders,
  })
}
