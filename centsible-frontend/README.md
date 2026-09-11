# Centsible — Frontend (Step 2)

React + Vite frontend. This step renders the first chart: a bar chart of
total spend per category, fed by the backend's new aggregation endpoint.

## Requirements
- Node.js 18+
- The backend running at http://localhost:8080 (see centsible-backend README)

## Run it
```bash
cd centsible-frontend
npm install
npm run dev
```

Then open the URL Vite prints (usually http://localhost:5173).

## What's in this step
- `src/api/client.js` — axios instance pointed at the backend
- `src/components/SpendByCategoryChart.jsx` — fetches
  `GET /api/analytics/spend-by-category` and renders it as a bar chart
  (Recharts), with loading and error states
- `src/App.jsx` — page shell

## Troubleshooting
- **"Couldn't reach the backend" message on screen** — make sure the Spring
  Boot app is running first (`mvn spring-boot:run` in `centsible-backend`).
- **CORS error in the browser console** — the backend's `AnalyticsController`
  already allows `http://localhost:5173` and `http://localhost:3000`. If Vite
  starts on a different port, add it to the `@CrossOrigin` origins list.

## Next up (not built yet)
- A second chart: spend over time (line chart)
- Filters (date range, category)
- CSV upload to replace the seeded sample data
