# Centsible — Frontend 

React + Vite frontend: sign-in (with a read-only demo), statement import, and the
analytics dashboard (summary, spend by category, income sources, cash flow, spend trend).

## Requirements
- Node.js 22.12+ (Vite 8 and Vitest need it)
- The backend running at http://localhost:8080 (see centsible-backend README)

## Run it
```bash
cd centsible-frontend
npm install
npm run dev
```

Then open the URL Vite prints (usually http://localhost:5173).

## Scripts
| Command | What it does |
|---------|--------------|
| `npm run dev` | Start the dev server |
| `npm run lint` | Lint with oxlint (CI treats warnings as failures) |
| `npm test` | Run the Vitest + Testing Library suite once |
| `npm run build` | Production build into `dist/` |

## Configuration
The backend API URL is read from the `VITE_API_URL` build-time env var (see
`.env.example`). Locally it's safe to leave unset — `src/api/client.js` falls
back to `http://localhost:8080/api`. When deploying, set `VITE_API_URL` to
the deployed backend's URL in your hosting platform's build settings.

## What's where
- `src/api/client.js` — axios instance pointed at the backend, adds the JWT
- `src/hooks/useAnalytics.js` — loads one analytics endpoint and reloads when the
  filters change (shared by every chart)
- `src/components/` — auth form, upload form, and the dashboard charts/tables
- `src/utils/` — LKR formatting and the demo-account helpers
- `*.test.js(x)` — tests sit next to the code they cover

## Troubleshooting
- **"Couldn't reach the backend" message on screen** — make sure the Spring
  Boot app is running first (`mvn spring-boot:run` in `centsible-backend`),
  and that `VITE_API_URL` (if set) actually points at it.
- **CORS error in the browser console** — the backend's `SecurityConfig`
  allows `http://localhost:5173` and `http://localhost:3000` by default via
  the `CORS_ALLOWED_ORIGINS` env var. If Vite starts on a different port, add
  it to that variable (comma-separated) and restart the backend.
