// Decorative panel for the sign-in page: a small sample chart that draws itself once on load
// (bars rise, then the savings line traces across and lands on a gold marker). All motion is
// CSS - see "Sign-in showcase" in App.css - and is switched off for people who prefer reduced
// motion, in which case the finished chart is simply shown. The figures are made up.

const MONTHS = ['May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct']
const INCOME = [310, 325, 340, 335, 360, 350] // thousand LKR
const SPENDING = [210, 245, 198, 262, 228, 205]

const LEFT = 40
const RIGHT = 500
const BASELINE = 380
const BAR_SCALE = 0.55 // px per thousand LKR
const BAR_WIDTH = 20
const GROUP_WIDTH = (RIGHT - LEFT) / MONTHS.length

// Running savings balance, drawn as the line above the bars
const savings = INCOME.map((inc, i) => inc - SPENDING[i]).reduce((acc, net, i) => {
  acc.push((acc[i - 1] ?? 0) + net)
  return acc
}, [])
const SAVINGS_TOP = 66
const SAVINGS_BOTTOM = 160
const minSavings = Math.min(...savings)
const maxSavings = Math.max(...savings)

const points = savings.map((value, i) => ({
  x: LEFT + (i + 0.5) * GROUP_WIDTH,
  y:
    SAVINGS_BOTTOM -
    ((value - minSavings) / (maxSavings - minSavings)) * (SAVINGS_BOTTOM - SAVINGS_TOP),
}))

// Smooth curve through the points using horizontal tangents
const linePath = points
  .map((p, i) => {
    if (i === 0) return `M ${p.x.toFixed(1)} ${p.y.toFixed(1)}`
    const prev = points[i - 1]
    const midX = ((prev.x + p.x) / 2).toFixed(1)
    return `C ${midX} ${prev.y.toFixed(1)} ${midX} ${p.y.toFixed(1)} ${p.x.toFixed(1)} ${p.y.toFixed(1)}`
  })
  .join(' ')

const lastPoint = points[points.length - 1]
const GRID_LINES = [100, 170, 240, 310, BASELINE]

export default function AuthShowcase() {
  return (
    <aside className="auth-showcase">
      <div className="auth-showcase-text">
        <p className="auth-showcase-title">See where your money goes.</p>
        <p className="auth-showcase-copy">
          Import a bank statement and get your income, spending and savings in one clear view.
        </p>
      </div>

      <svg
        className="auth-showcase-chart"
        viewBox="0 0 520 410"
        role="img"
        aria-label="Sample chart: monthly income and spending bars with a rising savings line"
      >
        {GRID_LINES.map((y) => (
          <line key={y} x1={LEFT} x2={RIGHT} y1={y} y2={y} className="ash-grid" />
        ))}

        {MONTHS.map((month, i) => {
          const groupX = LEFT + i * GROUP_WIDTH
          const incomeX = groupX + GROUP_WIDTH / 2 - BAR_WIDTH - 2
          const spendingX = groupX + GROUP_WIDTH / 2 + 2
          const incomeH = INCOME[i] * BAR_SCALE
          const spendingH = SPENDING[i] * BAR_SCALE
          return (
            <g key={month}>
              <rect
                className="ash-bar ash-bar--income"
                style={{ '--i': i }}
                x={incomeX}
                y={BASELINE - incomeH}
                width={BAR_WIDTH}
                height={incomeH}
                rx="3"
              />
              <rect
                className="ash-bar ash-bar--spending"
                style={{ '--i': i }}
                x={spendingX}
                y={BASELINE - spendingH}
                width={BAR_WIDTH}
                height={spendingH}
                rx="3"
              />
              <text className="ash-month" x={groupX + GROUP_WIDTH / 2} y={BASELINE + 20} textAnchor="middle">
                {month}
              </text>
            </g>
          )
        })}

        <path className="ash-line" d={linePath} pathLength="1" fill="none" />

        <g className="ash-marker">
          <circle className="ash-marker-ring" cx={lastPoint.x} cy={lastPoint.y} r="9" />
          <circle className="ash-marker-dot" cx={lastPoint.x} cy={lastPoint.y} r="5" />
        </g>
        <text className="ash-callout" x={RIGHT} y="30" textAnchor="end">
          Rs. 672,000 saved
        </text>
      </svg>

      <ul className="auth-showcase-legend">
        <li><span className="ash-swatch ash-swatch--income" />Income</li>
        <li><span className="ash-swatch ash-swatch--spending" />Spending</li>
        <li><span className="ash-swatch ash-swatch--savings" />Savings balance</li>
        <li className="auth-showcase-note">Sample data</li>
      </ul>
    </aside>
  )
}
