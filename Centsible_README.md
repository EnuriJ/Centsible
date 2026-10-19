# Centsible

Smart finance made simple — a personal finance dashboard for tracking spending
and income, with secure multi-user accounts, built with a React frontend and
a Spring Boot backend.

> **Status: feature-complete.** Secure accounts, CSV/PDF statement import,
> auto-categorization, and a full analytics dashboard (spend by category,
> income by category, cash flow, and spend trends) are all working
> end-to-end. See [Roadmap](#roadmap) for what's left — mostly deployment
> and polish rather than features.

## What it does today

- **Secure accounts** — sign up / log in with JWT-based authentication; each
  user's data is fully isolated from every other user's
- **Import bank statements** — upload a CSV or PDF of transactions; they're
  parsed, auto-categorized by keyword rules, and stored, with duplicate
  detection and per-row parse-error reporting
- **Dashboard** — total income/expense/net summary, spend by category (bar
  chart), income by category (table), monthly cash flow (income vs. expense
  vs. net savings), and category spend trend over time (line chart)
- **Filters** — date range and category filters apply across every view
- **LKR currency formatting** throughout

## Tech stack

| Layer    | Stack |
|----------|-------|
| Frontend | React (Vite), Recharts, Axios |
| Backend  | Java 17, Spring Boot, Spring Data JPA |
| Security | Spring Security, JWT (JJWT), BCrypt password hashing |
| Database | H2 (file-based) |
| Testing  | JUnit 5 (backend) |

## Project structure

```
Centsible/
├── centsible-backend/    # Spring Boot API — auth, models, repositories, analytics endpoints
└── centsible-frontend/   # React + Vite UI — auth, upload, filters, charts
```

Each sub-project has its own README with detailed setup steps:
- [`centsible-backend/README.md`](./centsible-backend/README.md)
- [`centsible-frontend/README.md`](./centsible-frontend/README.md)

## Quick start

```bash
# 1. Start the backend (http://localhost:8080)
cd centsible-backend
mvn spring-boot:run

# 2. In a separate terminal, start the frontend (http://localhost:5173)
cd centsible-frontend
npm install
npm run dev
```

On first run, the backend seeds a demo account with realistic sample
transactions so the dashboard has real data to show immediately:

- **Email:** `demo@centsible.com`
- **Password:** `Password123!`

Or sign up with your own account — new users start with a default set of
categories and keyword rules, and can import their own CSV or PDF statement.

## Running the tests

```bash
cd centsible-backend
mvn test
```

## Roadmap

- [ ] **Live deployment** — host the frontend (e.g. Vercel/Netlify) and
      backend (e.g. Render/Railway) so the app can be tried without cloning
      and running it locally
- [ ] **Frontend test coverage** — the backend has a JUnit suite; the
      frontend doesn't yet have one
- [ ] **CI** — run the backend test suite and frontend build/lint on every
      push

## Screenshots

*(coming soon)*
