# Centsible — Backend 

Personal finance dashboard backend. This sets up the data model (Category, Transaction),
an H2 file-based database, and seeds it with ~30 realistic sample transactions across
2 months so there's real data to build against before CSV upload exists.

## Requirements
- Java 17+
- Maven 3.6+

## Run it
```bash
cd centsible-backend
mvn spring-boot:run
```

On first run, `DataSeeder` will populate the database automatically. On later runs it
detects existing data and skips reseeding (so restarting the app is safe).

## Verify the data
Open the H2 web console: http://localhost:8080/h2-console

Login with:
- JDBC URL: `jdbc:h2:file:./data/centsible`
- User Name: `sa`
- Password: *(leave blank)*

Then run e.g. `SELECT * FROM TRANSACTIONS;` to see the seeded rows.

## Configuration
- `JWT_SECRET` — signing key for auth tokens. Safe to leave unset locally (a
  built-in dev key is used and a startup warning reminds you it's in use).
  **Must** be set to a unique, random value before deploying anywhere public —
  the dev fallback is committed to this public repo, so anyone could forge
  valid tokens with it if it's still in use on a live deployment.
- `JWT_EXPIRATION_MS` — token lifetime in ms (default 24h).
- `CORS_ALLOWED_ORIGINS` — comma-separated frontend origins allowed to call
  this API (default covers `localhost:5173`/`3000`). Set to your deployed
  frontend's URL(s) in production.

### Production (`SPRING_PROFILES_ACTIVE=prod`)
The `prod` profile (`application-prod.properties`) switches to PostgreSQL, turns
off the H2 console and SQL logging, and **requires** `DATABASE_URL` and `JWT_SECRET`
- the app refuses to start if either is missing, rather than falling back to
something insecure. The `Dockerfile` sets this profile by default. Step-by-step
hosting instructions are in [`../DEPLOY.md`](../DEPLOY.md).

| Variable | Purpose |
|----------|---------|
| `DATABASE_URL` | Either the `postgres://user:password@host/db` URL most hosts hand out (pasted as-is; the login is taken from it), or a JDBC URL such as `jdbc:postgresql://host:5432/centsible?sslmode=require` |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | Only with a JDBC `DATABASE_URL` |
| `JWT_SECRET` | Random secret, at least 32 characters |
| `CORS_ALLOWED_ORIGINS` | Optional - only when the frontend is on a different domain than the API (not needed in the single-domain Vercel setup), comma-separated |
| `PORT` | Optional - port to listen on (default 8080) |
| `DEMO_ENABLED` | Optional - `false` skips seeding the shared read-only demo account (default `true`) |

Generate a secret with `openssl rand -base64 48`. Local development needs none of
this: without the `prod` profile it uses the H2 file database and the dev defaults.

## What's in this step
- `model/Category.java`, `model/Transaction.java` — JPA entities (the SQL schema is
  auto-generated from these via Hibernate)
- `repository/` — Spring Data repositories for querying
- `config/DataSeeder.java` — seeds sample data on startup
- No REST endpoints yet — that's step 2 (aggregation endpoints + first chart)

## Note on this container
This project was scaffolded here but not build-tested in this sandbox — the sandbox
can't reach Maven Central to download Spring Boot dependencies. Run `mvn spring-boot:run`
locally in your own environment (IntelliJ, VS Code, or terminal) to actually build and
start it. If you hit any errors when you run it, paste them back here and we'll fix them.
