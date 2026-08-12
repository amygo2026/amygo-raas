type Props = {
  /** Logo only — for narrow product rail. */
  compact?: boolean
  href?: string
}

/** Top-left brand: AMYGO AI wordmark. */
export default function BrandLogo({ compact = false, href = '/' }: Props) {
  return (
    <a
      href={href}
      className={`brand-row${compact ? ' brand-row-compact' : ''}`}
      aria-label="AMYGO"
    >
      <img
        src="/amygo-ai-c.svg"
        alt="AMYGO"
        width={compact ? 128 : 168}
        height={compact ? 36 : 48}
        className="logo logo-amygo"
      />
    </a>
  )
}
