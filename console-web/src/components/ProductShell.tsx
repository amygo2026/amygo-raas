import type { ReactNode } from 'react'
import BrandLogo from './BrandLogo'
import { useI18n } from '../lib/i18n'

type RailLink = {
  id: string
  label: string
  active?: boolean
  onClick?: () => void
}

export type Crumb = {
  label: string
}

type Props = {
  crumbs: Crumb[]
  actions?: ReactNode
  railLinks: RailLink[]
  children: ReactNode
}

/**
 * Product chrome: left rail + top bar (aligned with idatalab AmyZero ProductShell).
 */
export default function ProductShell({ crumbs, actions, railLinks, children }: Props) {
  const { t } = useI18n()

  return (
    <div className="product-shell">
      <aside className="product-rail" aria-label={t('navAria')}>
        <div className="product-rail-brand">
          <BrandLogo compact />
        </div>
        <nav className="product-rail-nav">
          {railLinks.map((link) => (
            <button
              key={link.id}
              type="button"
              className={`product-rail-link${link.active ? ' active' : ''}`}
              onClick={link.onClick}
            >
              {link.label}
            </button>
          ))}
        </nav>
        <div className="product-rail-foot muted">{t('railFoot')}</div>
      </aside>

      <div className="product-main">
        <header className="workspace-nav product-topbar">
          <nav className="product-crumbs" aria-label={t('crumbAria')}>
            {crumbs.map((c, i) => {
              const last = i === crumbs.length - 1
              return (
                <span key={`${c.label}-${i}`} className="product-crumb">
                  {i > 0 && (
                    <span className="product-crumb-sep" aria-hidden>
                      /
                    </span>
                  )}
                  <span className={last ? 'product-crumb-current' : 'product-crumb-text'}>
                    {c.label}
                  </span>
                </span>
              )
            })}
          </nav>
          <div className="nav-actions">{actions}</div>
        </header>
        <div className="product-content">{children}</div>
      </div>
    </div>
  )
}
