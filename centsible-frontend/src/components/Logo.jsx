export default function Logo({ size = 36, showText = true, className = '' }) {
  return (
    <div className={`brand-logo ${className}`} style={{ display: 'inline-flex', alignItems: 'center', gap: '0.75rem' }}>
      <svg
        width={size}
        height={size}
        viewBox="0 0 48 48"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        aria-label="Centsible Logo"
      >
        {/* Soft mint circular backdrop glow */}
        <circle cx="24" cy="24" r="22" fill="#E6F7F2" />

        {/* Outer stylized 'C' arc */}
        <path
          d="M 33 13 A 15 15 0 1 0 33 35"
          stroke="#0D6E51"
          strokeWidth="3.5"
          strokeLinecap="round"
        />

        {/* Inner concentric ring / coin accent */}
        <path
          d="M 30 18.5 A 9 9 0 1 0 30 29.5"
          stroke="#1B8A70"
          strokeWidth="2.5"
          strokeLinecap="round"
        />

        {/* Minimalist central financial spine / cent-dollar fusion */}
        <line
          x1="23.5"
          y1="17"
          x2="23.5"
          y2="31"
          stroke="#0D6E51"
          strokeWidth="2.5"
          strokeLinecap="round"
        />

        {/* Upward growth micro-tick */}
        <circle cx="34.5" cy="13.5" r="2" fill="#E5A93C" />
      </svg>
      {showText && (
        <span
          style={{
            fontSize: `${size * 0.65}px`,
            fontWeight: 800,
            letterSpacing: '-0.025em',
            color: '#0F172A',
            fontFamily: 'inherit',
            lineHeight: 1,
          }}
        >
          Centsible
        </span>
      )}
    </div>
  )
}
