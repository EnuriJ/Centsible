# Centsible — Frontend 

React + Vite frontend. This renders the first chart: a bar chart of
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

## Configuration
The backend API URL is read from the `VITE_API_URL` build-time env var (see
`.env.example`). Locally it's safe to leave unset — `src/api/client.js` falls
back to `http://localhost:8080/api`. When deploying, set `VITE_API_URL` to
the deployed backend's URL in your hosting platform's build settings.

## What's in this step
- `src/api/client.js` — axios instance pointed at the backend
- `src/components/SpendByCategoryChart.jsx` — fetches
  `GET /api/analytics/spend-by-category` and renders it as a bar chart
  (Recharts), with loading and error states
- `src/App.jsx` — page shell

## Troubleshooting
- **"Couldn't reach the backend" message on screen** — make sure the Spring
  Boot app is running first (`mvn spring-boot:run` in `centsible-backend`),
  and that `VITE_API_URL` (if set) actually points at it.
- **CORS error in the browser console** — the backend's `SecurityConfig`
  allows `http://localhost:5173` and `http://localhost:3000` by default via
  the `CORS_ALLOWED_ORIGINS` env var. If Vite starts on a different port, add
  it to that variable (comma-separated) and restart the backend.
