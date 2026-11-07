# Centsible

[![CI](https://github.com/EnuriJ/Centsible/actions/workflows/ci.yml/badge.svg)](https://github.com/EnuriJ/Centsible/actions/workflows/ci.yml)

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
  detection and per-row parse-error reporting. PDFs are read by looking at
  the statement's table layout rather than a fixed bank template (see
  [Statement PDFs](#statement-pdfs))
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
| Testing  | JUnit 5 (backend), Vitest + Testing Library (frontend) |
| CI       | GitHub Actions |

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

On first run, the backend seeds a **read-only demo account** with synthetic
sample transactions, so the dashboard has data to show immediately. Click
**Try the demo** on the sign-in page (or log in as `demo@centsible.com` /
`Password123!`). The demo can look at everything but can't import, edit or
delete anything: the server rejects every state-changing request from that
account, so the shared data can't be altered. Set `DEMO_ENABLED=false` to skip
seeding it.

Or sign up with your own account — new users start with a default set of
categories and keyword rules, and can import their own CSV or PDF statement.

## Statement PDFs

The PDF reader doesn't hard-code any one bank. It finds the table's header
row (Date, Description/Particulars, Debit/Credit or Payments/Receipts,
Balance…), works out which words sit under which column, and rebuilds each
transaction from there. It checks its work against the running balance, so a
row that doesn't add up is reported rather than silently trusted.

- Tested against synthetic statements laid out like Commercial Bank,
  Sampath Bank and Seylan Bank (oldest-first and newest-first, separate
  debit/credit columns or a single signed column, wrapped descriptions,
  multi-page with repeated headers).
- **Not supported:** scanned/photographed statements (no OCR) and
  password-protected PDFs. If a bank's layout isn't recognised, the upload
  says so — export a CSV instead.
- Uploaded files are read in memory and never stored; only the extracted
  rows are saved.
- Real statements contain personal data, so `*.pdf` is git-ignored. Tests build
  their own invented statements in code.

## Running the tests

```bash
# Backend
cd centsible-backend
mvn test

# Frontend
cd centsible-frontend
npm test
npm run lint
```

Both run automatically on every push and pull request (GitHub Actions,
`.github/workflows/ci.yml`).

## Roadmap

- [ ] **Live deployment** — host the frontend (e.g. Vercel/Netlify) and
      backend (e.g. Render/Railway) so the app can be tried without cloning
      and running it locally

## Screenshots

*(coming soon)*
