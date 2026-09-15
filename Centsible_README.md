# Centsible

Smart finance made simple — a personal finance dashboard for tracking spending,
built with a React frontend and a Spring Boot backend.

> **Status: actively in development.** Expense tracking, categorisation, and
> visualisation are working end-to-end. Income tracking and an income-vs-expense
> view are in progress — see [Roadmap](#roadmap) below.

## What it does today

- Upload a CSV of transactions and have them parsed, categorised, and stored
- View total spend broken down by category (bar chart)
- View spend over time by month, filterable by category and date range
- Filter all views by date range and category

## Tech stack

| Layer    | Stack |
|----------|-------|
| Frontend | React (Vite), Recharts, Axios |
| Backend  | Java 17, Spring Boot, Spring Data JPA |
| Database | H2 (file-based) |

## Project structure

```
Centsible/
├── centsible-backend/    # Spring Boot API — models, repositories, analytics endpoints
└── centsible-frontend/   # React + Vite UI — upload, filters, charts
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

The backend seeds sample transaction data automatically on first run, so the
dashboard has real data to show without needing a CSV upload first.

## Roadmap

- [ ] **Income tracking** — the data model already supports it (positive amounts
      represent income), but there's no dedicated way to add or view income yet
- [ ] **Income vs. expense comparison view** — a summary that shows net position,
      not just spend
- [ ] **LKR as the default currency** — currently formatted as USD; switching to
      Sri Lankan Rupees
- [ ] **UI refresh** — improving the visual design, starting with the background
      and overall look of the dashboard

## Screenshots

*(coming soon)*
