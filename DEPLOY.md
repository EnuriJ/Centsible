# Deploying Centsible on Vercel

One Vercel project serves everything from a single address:

- **web** — the React frontend (`centsible-frontend/`)
- **api** — the Spring Boot backend, run as a container from `centsible-backend/Dockerfile`
- requests to `/api/...` go to the backend, everything else to the frontend, so there is no
  cross-site (CORS) setup
- **Postgres** — a hosted database (Neon). The backend's own disk is wiped whenever it restarts, so
  data can't live there.

The routing is in [`vercel.json`](./vercel.json). Vercel's container runtime and "services" are both
in Beta, so if a step below doesn't behave as described, see [If something goes wrong](#if-something-goes-wrong).

## Before you start

1. A Vercel account (free "Hobby" plan is fine), signed in with the GitHub account that owns this repo.
2. A strong secret for signing logins. In a terminal:
   ```bash
   openssl rand -base64 48
   ```
   Copy the output. This is your `JWT_SECRET`. Keep it private; never commit it. (If it changes,
   everyone is signed out. Nothing else is lost.)

## 1. Create the database

1. In the Vercel dashboard open **Storage** (or the Marketplace) and create a **Neon** Postgres
   database on the free plan. Pick a region near your Vercel region (the default Vercel region is
   `iad1`, Washington DC).
2. In the Neon console open the database's **Connection details**, turn **connection pooling off**,
   and copy the connection string. It looks like
   `postgres://user:password@ep-something.region.aws.neon.tech/neondb?sslmode=require`.
   Paste it as-is later; the backend converts it itself.

## 2. Create the Vercel project

1. **Add New → Project**, import `EnuriJ/Centsible`.
2. Leave **Root Directory** as the repository root (`./`). Vercel reads `vercel.json` and
   sets up the two services.
3. Before the first deploy, add these **Environment Variables** (all environments):

   | Name | Value |
   |------|-------|
   | `JWT_SECRET` | the secret from above |
   | `DATABASE_URL` | the Neon connection string from step 1 |
   | `PORT` | `8080` |

   `PORT` tells Vercel which port the backend listens on (Vercel assumes 80 otherwise).
   Optional: `DEMO_ENABLED=false` turns the demo account off.
4. Click **Deploy**. The backend image builds with Maven, so the first build takes a few minutes.

## 3. Check it works

Open your project's address (`https://<project>.vercel.app`) and check, in order:

1. The sign-in page loads.
2. `https://<project>.vercel.app/api/health` shows `{"status":"ok"}`.
3. **Try the demo** signs in and shows charts with sample data. (The first request after a quiet
   spell can take up to a minute while the backend starts; the sign-in page says so.)
4. Create your own account, import a CSV, then a bank-statement PDF. Refresh the page and confirm
   your data is still there (this proves the database is connected).
5. Delete the test account from the header.

Then put the address in the README and add it to the repository's "About" box on GitHub.

## What to know

- **Sleeping backend.** After about 5 minutes without traffic the backend scales down; the next
  visitor waits while Java starts (the sign-in page pings `/api/health` on load to start it early).
- **Upload limit.** Vercel passes through requests up to 4.5 MB, so imports are capped at 4 MB.
- **Demo data stays fresh.** On startup the backend replaces the demo account's sample
  transactions with new ones if the newest is over 45 days old. Only the demo account is touched.
- **Free database.** Neon's free database pauses itself when idle and wakes on the next query.
- **Secrets.** Only the three variables above are needed. Never put them in the repository.
- **Changing the secret or database** means editing the variable in Vercel and redeploying.

## If something goes wrong

| What you see | What it usually means |
|--------------|-----------------------|
| The site loads but `/api/health` gives 502/504 or never answers | The backend isn't listening where Vercel expects. Check `PORT=8080` is set, then check the deployment's runtime logs. |
| Logs say `Could not resolve placeholder 'JWT_SECRET'` (or `DATABASE_URL`) | That variable is missing. Add it and redeploy. This is deliberate: the production setup refuses to start without secrets. |
| Logs show a database connection or authentication error | Re-copy the Neon connection string (pooling **off**). Make sure it starts with `postgres://`. |
| The demo or sign-up says "Too many requests" for everyone | The server can't see visitors' real addresses and treats them as one. Open an issue, or see `TRUSTED_PROXY_REGEX` in `application-prod.properties`. |
| Vercel doesn't recognise `services` / the container runtime | These are Beta. Use the two-project fallback below. |

### Fallback: two Vercel projects

If the single-project setup won't work:

1. **Backend project**: import the repo, set **Root Directory** to `centsible-backend`. Vercel builds
   the Dockerfile. Add `JWT_SECRET`, `DATABASE_URL`, `PORT=8080`, and
   `CORS_ALLOWED_ORIGINS=https://<frontend-project>.vercel.app`.
2. **Frontend project**: import the repo again, **Root Directory** `centsible-frontend`, and add
   `VITE_API_URL=https://<backend-project>.vercel.app/api`.
3. Delete (or ignore) the root `vercel.json`, since it describes the single-project layout.

Any host that runs Docker (Render, Fly.io, Railway) can run the backend instead: build
`centsible-backend/Dockerfile`, set the same variables, and point `VITE_API_URL` at it.

## Running the container locally

```bash
cd centsible-backend
docker build -t centsible-backend .
docker run --rm -p 8080:8080 \
  -e JWT_SECRET="$(openssl rand -base64 48)" \
  -e DATABASE_URL="postgres://user:password@host/db?sslmode=require" \
  centsible-backend
curl http://localhost:8080/api/health
```
