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
off the H2 console and SQL logging, and **requires** the values below - the app
refuses to start if any is missing, rather than falling back to something
insecure.

| Variable | Purpose |
|----------|---------|
| `DATABASE_URL` | JDBC URL, e.g. `jdbc:postgresql://host:5432/centsible?sslmode=require` (not the `postgres://...` form some hosts hand out) |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | Database credentials |
| `JWT_SECRET` | Random secret, at least 32 characters |
| `CORS_ALLOWED_ORIGINS` | Deployed frontend URL(s), comma-separated |
| `PORT` | Optional - port to listen on (default 8080) |

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
