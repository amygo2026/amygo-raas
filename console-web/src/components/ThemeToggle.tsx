import { useTheme } from '../lib/theme'
import { useI18n } from '../lib/i18n'

/** Top-right theme switch: dark ↔ light. */
export default function ThemeToggle() {
  const { theme, toggleTheme } = useTheme()
  const { t } = useI18n()
  const isLight = theme === 'light'

  return (
    <button
      type="button"
      className={`theme-toggle ${isLight ? 'is-light' : 'is-dark'}`}
      role="switch"
      aria-checked={isLight}
      aria-label={isLight ? t('themeToDark') : t('themeToLight')}
      title={isLight ? t('themeToDark') : t('themeToLight')}
      onClick={toggleTheme}
    >
      <span className="theme-toggle-track" aria-hidden>
        <span className="theme-toggle-thumb" />
      </span>
      <span className="theme-toggle-label">{isLight ? t('themeLight') : t('themeDark')}</span>
    </button>
  )
}
