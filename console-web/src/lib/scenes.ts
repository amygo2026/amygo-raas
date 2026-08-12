import type { MessageKey } from './i18n'

export type DemoSceneId = 'restaurant' | 'cleaning' | 'hotel' | 'show'

export type DemoScene = {
  id: DemoSceneId
  image: string
  /** i18n title key */
  titleKey: MessageKey
  /** i18n caption key */
  captionKey: MessageKey
  taskType: 'DELIVERY' | 'CLEANING' | 'HOTEL_DELIVERY' | 'SHOW'
  floor: {
    zones: { x: number; y: number; w: number; h: number; label: string; wide?: boolean }[]
  }
  /** Prefer robots whose adapter/model hints match */
  robotMatch: (r: { adapterType?: string; modelProfile?: string; displayName: string }) => boolean
  taskMatch: (t: { taskType: string }) => boolean
}

export const DEMO_SCENES: DemoScene[] = [
  {
    id: 'restaurant',
    image: '/scenes/scene-restaurant.png',
    titleKey: 'sceneRestaurant',
    captionKey: 'sceneRestaurantCap',
    taskType: 'DELIVERY',
    floor: {
      zones: [
        { x: 8, y: 10, w: 34, h: 28, label: 'Kitchen' },
        { x: 48, y: 10, w: 44, h: 28, label: 'Dining' },
        { x: 8, y: 46, w: 84, h: 42, label: 'Service aisle', wide: true },
      ],
    },
    robotMatch: (r) =>
      /delivery|pudu|keenon|sim\.delivery/i.test(`${r.adapterType} ${r.modelProfile} ${r.displayName}`) &&
      !/clean|hotel|unitree|show/i.test(`${r.modelProfile}`),
    taskMatch: (t) => t.taskType === 'DELIVERY',
  },
  {
    id: 'cleaning',
    image: '/scenes/scene-cleaning.png',
    titleKey: 'sceneCleaning',
    captionKey: 'sceneCleaningCap',
    taskType: 'CLEANING',
    floor: {
      zones: [
        { x: 8, y: 8, w: 40, h: 36, label: 'Lobby' },
        { x: 52, y: 8, w: 40, h: 36, label: 'Corridor' },
        { x: 8, y: 50, w: 84, h: 40, label: 'Dock / charger', wide: true },
      ],
    },
    robotMatch: (r) => /clean/i.test(`${r.modelProfile} ${r.displayName}`),
    taskMatch: (t) => t.taskType === 'CLEANING',
  },
  {
    id: 'hotel',
    image: '/scenes/scene-hotel.png',
    titleKey: 'sceneHotel',
    captionKey: 'sceneHotelCap',
    taskType: 'HOTEL_DELIVERY',
    floor: {
      zones: [
        { x: 8, y: 8, w: 28, h: 84, label: 'Elevator' },
        { x: 42, y: 8, w: 50, h: 38, label: 'Guest rooms' },
        { x: 42, y: 52, w: 50, h: 40, label: 'Service hub' },
      ],
    },
    robotMatch: (r) => /hotel/i.test(`${r.modelProfile} ${r.displayName}`),
    taskMatch: (t) => t.taskType === 'HOTEL_DELIVERY',
  },
  {
    id: 'show',
    image: '/scenes/scene-show.png',
    titleKey: 'sceneShow',
    captionKey: 'sceneShowCap',
    taskType: 'SHOW',
    floor: {
      zones: [
        { x: 12, y: 8, w: 76, h: 32, label: 'Stage' },
        { x: 8, y: 46, w: 40, h: 42, label: 'Wings L' },
        { x: 52, y: 46, w: 40, h: 42, label: 'Wings R' },
      ],
    },
    robotMatch: (r) => /unitree|show/i.test(`${r.adapterType} ${r.modelProfile} ${r.displayName}`),
    taskMatch: () => false,
  },
]
