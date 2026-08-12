import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import BrandLogo from '../components/BrandLogo'
import LocaleSwitcher from '../components/LocaleSwitcher'
import ThemeToggle from '../components/ThemeToggle'
import { useI18n, type MessageKey } from '../lib/i18n'
import { createCleaningTask, createDeliveryTask, createHotelTask } from '../lib/api'
import { DEMO_SCENES, type DemoSceneId } from '../lib/scenes'

const VALUE_KEYS: { title: MessageKey; body: MessageKey; scene: DemoSceneId }[] = [
  { title: 'valueLiveTitle', body: 'valueLiveBody', scene: 'restaurant' },
  { title: 'valueRecoverTitle', body: 'valueRecoverBody', scene: 'cleaning' },
  { title: 'valueMultiTitle', body: 'valueMultiBody', scene: 'hotel' },
  { title: 'valueAuditTitle', body: 'valueAuditBody', scene: 'show' },
]

export default function HomePage() {
  const { t } = useI18n()
  const navigate = useNavigate()
  const [busy, setBusy] = useState(false)
  const [flash, setFlash] = useState<string | null>(null)
  const [slide, setSlide] = useState(0)

  useEffect(() => {
    const id = setInterval(() => setSlide((s) => (s + 1) % DEMO_SCENES.length), 6500)
    return () => clearInterval(id)
  }, [])

  const scene = DEMO_SCENES[slide]

  async function fireDemo(kind: 'delivery' | 'cleaning' | 'hotel') {
    setBusy(true)
    setFlash(null)
    try {
      const res =
        kind === 'cleaning'
          ? await createCleaningTask('homepage-clean-demo')
          : kind === 'hotel'
            ? await createHotelTask('homepage-hotel-demo')
            : await createDeliveryTask('homepage-demo')
      if (!res.ok) throw new Error(await res.text())
      setFlash('ok')
      navigate('/ops')
    } catch (e) {
      setFlash(e instanceof Error ? e.message : 'failed')
    } finally {
      setBusy(false)
    }
  }

  function go(delta: number) {
    setSlide((s) => (s + delta + DEMO_SCENES.length) % DEMO_SCENES.length)
  }

  function openCommand(id: DemoSceneId) {
    navigate(`/demo/command-center?scene=${id}`)
  }

  return (
    <div className="home-page home-page-banner">
      <header className="home-top home-top-over">
        <BrandLogo />
        <div className="nav-actions">
          <LocaleSwitcher />
          <ThemeToggle />
        </div>
      </header>

      <section className="home-banner" aria-roledescription="carousel" aria-label={t('homeBannerAria')}>
        <div className="home-banner-slides">
          {DEMO_SCENES.map((s, i) => (
            <div
              key={s.id}
              className={`home-banner-slide${i === slide ? ' active' : ''}`}
              style={{ backgroundImage: `url(${s.image})` }}
              role="group"
              aria-roledescription="slide"
              aria-label={t(s.titleKey)}
              aria-hidden={i !== slide}
            />
          ))}
        </div>
        <div className="home-banner-veil" />

        <p className="home-slogan" aria-label="Robotics Service">
          <span className="home-slogan-line">Robotics</span>
          <span className="home-slogan-line">Service</span>
        </p>

        <div className="home-banner-content">
          <p className="home-scene-tag">{t(scene.titleKey)}</p>
          <h1 className="home-title">{t('homeHeroTitle')}</h1>
          <p className="home-lead">{t(scene.captionKey)}</p>
          <div className="home-cta">
            <button type="button" className="home-btn primary" onClick={() => openCommand(scene.id)}>
              {t('homeCtaCommand')}
            </button>
            <Link className="home-btn" to="/ops">
              {t('homeCtaOps')}
            </Link>
            {scene.taskType === 'CLEANING' ? (
              <button type="button" className="home-btn ghost" disabled={busy} onClick={() => fireDemo('cleaning')}>
                {busy ? t('working') : t('homeCtaCleanTask')}
              </button>
            ) : scene.taskType === 'HOTEL_DELIVERY' ? (
              <button type="button" className="home-btn ghost" disabled={busy} onClick={() => fireDemo('hotel')}>
                {busy ? t('working') : t('homeCtaHotelTask')}
              </button>
            ) : scene.taskType === 'DELIVERY' ? (
              <button type="button" className="home-btn ghost" disabled={busy} onClick={() => fireDemo('delivery')}>
                {busy ? t('working') : t('homeCtaDemoTask')}
              </button>
            ) : null}
          </div>
          {flash && flash !== 'ok' && <p className="home-flash error">{flash}</p>}
        </div>

        <button type="button" className="home-banner-nav prev" aria-label={t('slidePrev')} onClick={() => go(-1)}>
          ‹
        </button>
        <button type="button" className="home-banner-nav next" aria-label={t('slideNext')} onClick={() => go(1)}>
          ›
        </button>

        <div className="home-banner-dots" role="tablist" aria-label={t('homeBannerAria')}>
          {DEMO_SCENES.map((s, i) => (
            <button
              key={s.id}
              type="button"
              role="tab"
              aria-selected={i === slide}
              className={`home-dot${i === slide ? ' active' : ''}`}
              onClick={() => setSlide(i)}
            >
              <span className="sr-only">{t(s.titleKey)}</span>
            </button>
          ))}
        </div>
      </section>

      <section className="home-ops">
        <div className="home-ops-head">
          <h2>{t('homeOpsTitle')}</h2>
          <p>{t('homeOpsLead')}</p>
        </div>
        <div className="home-ops-grid">
          {VALUE_KEYS.map((item) => (
            <button
              key={item.title}
              type="button"
              className="home-ops-card"
              onClick={() => openCommand(item.scene)}
            >
              <strong>{t(item.title)}</strong>
              <span>{t(item.body)}</span>
            </button>
          ))}
        </div>
      </section>
    </div>
  )
}
