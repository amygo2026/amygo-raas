import { LOCALES, useI18n, type Locale } from '../lib/i18n'

/** Language selector: zh-CN / zh-TW / ja / fr / en */
export default function LocaleSwitcher() {
  const { locale, setLocale, t } = useI18n()

  return (
    <label className="locale-switcher" title={t('language')}>
      <select
        className="locale-select"
        value={locale}
        aria-label={t('language')}
        onChange={(e) => setLocale(e.target.value as Locale)}
      >
        {LOCALES.map((l) => (
          <option key={l.id} value={l.id}>
            {l.label}
          </option>
        ))}
      </select>
    </label>
  )
}
