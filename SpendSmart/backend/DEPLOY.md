# Deploying SpendSmart Backend (Free)

This deploys the Flask API and a PostgreSQL database for free, giving you an
HTTPS URL the Android app can use from anywhere (no local server needed).

**Recommended setup:** PostgreSQL on **Aiven** + web service on **Render**.
Aiven's free PostgreSQL plan does not expire after 90 days like Render's free
database does, so it is a better long-term fit. The all-Render path is still
documented at the bottom (Option C) if you prefer a single provider.

> Free tier note: the free Render web service sleeps after ~15 min of
> inactivity. The first request after sleeping takes ~30–50 s to wake up —
> this is normal.

## Prerequisites

- The `SpendSmart` project pushed to a GitHub repo (Render deploys from GitHub).
- A free account at https://aiven.io
- A free account at https://render.com (sign in with GitHub).
- `psql` (the PostgreSQL command-line client) installed locally, for
  loading the schema.

---

# Option A — Aiven PostgreSQL + Render web service (recommended)

## Step 1 — Create the free Aiven PostgreSQL service

1. Sign in at https://aiven.io and open the **Aiven Console**.
2. Click **Create service** -> **PostgreSQL**.
3. Choose the **Free plan** (look for the plan labelled *Free* — it is a
   shared 1-CPU / small-storage tier; no credit card needed).
4. Pick a cloud region close to your Render region (e.g. the same continent)
   to keep latency low.
5. Name the service e.g. `spendsmart-pg` and click **Create service**.
6. Wait until the service status changes from *Rebuilding* to **Running**
   (this can take a few minutes).

## Step 2 — Get the Aiven Service URI (DATABASE_URL)

1. Open the `spendsmart-pg` service in the Aiven Console.
2. On the **Overview** tab, find **Connection information**.
3. Copy the **Service URI**. It looks like:
   ```
   postgres://avnadmin:PASSWORD@HOST.aivencloud.com:PORT/defaultdb?sslmode=require
   ```
   Notes:
   - The default database is `defaultdb` and the default user is `avnadmin`.
   - The URI already ends with `?sslmode=require`. **Keep that query param** —
     Aiven requires SSL, and the app relies on it. (As a safety net, `app.py`
     also appends `sslmode=require` if a non-local URL is missing it.)

Keep this Service URI handy — you will use it twice (Step 3 and Step 5).

## Step 3 — Load the database schema into Aiven

The tables must be created once.

1. Make sure `psql` is installed locally.
2. From the `SpendSmart/backend` folder, run (paste your real Service URI
   in place of `<AIVEN_SERVICE_URI>`):
   ```
   psql "<AIVEN_SERVICE_URI>" -f schema.sql
   ```
   This creates the `users`, `categories`, `expenses` tables and seeds the
   7 default categories.
3. Verify it worked:
   ```
   psql "<AIVEN_SERVICE_URI>" -c "\dt"
   ```
   You should see the three tables listed.

**No psql?** Open the database in a GUI client (DBeaver, TablePlus, pgAdmin)
using the same Service URI, then paste and run the contents of `schema.sql`.
Make sure the client connects with SSL enabled.

## Step 4 — Deploy the web service on Render

The repo includes `SpendSmart/backend/render.yaml`, a **web-service-only**
blueprint (it does not create a Render database).

1. Push the project to GitHub.
2. In Render: **New +** -> **Blueprint**.
3. Select your repo. Render reads `render.yaml` and shows the
   `spendsmart-api` web service. `SECRET_KEY` is auto-generated and
   `DATABASE_URL` is marked **sync: false** (you set it manually next).
4. Click **Apply**. The first deploy will start.

**Prefer manual setup over the blueprint?**
1. In Render: **New +** -> **Web Service**, connect your GitHub repo.
2. Settings:
   - **Root Directory:** `SpendSmart/backend`
   - **Runtime:** Python 3
   - **Build Command:** `pip install -r requirements.txt`
   - **Start Command:** `gunicorn app:app --bind 0.0.0.0:$PORT`
   - **Plan:** Free
3. Add the environment variables as described in Step 5.
4. Click **Create Web Service**.

## Step 5 — Set the Aiven DATABASE_URL in Render

1. Open the `spendsmart-api` web service in Render.
2. Go to the **Environment** tab.
3. Add / edit these variables:
   - `DATABASE_URL` = the **Aiven Service URI** from Step 2
     (the full `postgres://...?sslmode=require` string).
   - `SECRET_KEY` = a long random string (auto-set if you used the blueprint).
   - `FLASK_DEBUG` = `false`
4. Save changes. Render will redeploy automatically with the new values.

## Step 6 — Get your API URL and verify

1. Open the `spendsmart-api` web service in Render.
2. The URL is shown at the top, e.g. `https://spendsmart-api.onrender.com`.
3. Verify it works — open in a browser:
   ```
   https://spendsmart-api.onrender.com/api/health
   ```
   You should see `{"status":"ok"}` (first hit may be slow if asleep).
4. To confirm the database link, register a user from the app or via curl.
   If `/api/health` works but registration fails, re-check the
   `DATABASE_URL` value in Render.

---

# Option C — All-Render (Render PostgreSQL + Render web service)

Use this only if you want a single provider. Render's free PostgreSQL is
deleted after 90 days, so Aiven (Option A) is preferred.

### 1. Create the PostgreSQL database

1. In Render: **New +** -> **PostgreSQL**.
2. Name: `spendsmart-db`. Plan: **Free**. Region: closest to you.
3. Click **Create Database** and wait until status is **Available**.
4. Copy the **Internal Database URL** (`postgresql://...`).

### 2. Create the web service

1. In Render: **New +** -> **Web Service**, connect your GitHub repo.
2. Settings:
   - **Root Directory:** `SpendSmart/backend`
   - **Runtime:** Python 3
   - **Build Command:** `pip install -r requirements.txt`
   - **Start Command:** `gunicorn app:app --bind 0.0.0.0:$PORT`
   - **Plan:** Free
3. Under **Environment**, add:
   - `DATABASE_URL` = the Internal Database URL from step 1.4
   - `SECRET_KEY` = any long random string
   - `FLASK_DEBUG` = `false`
4. Click **Create Web Service**.

### 3. Load the schema

1. On the Render database page, copy the **External Database URL**.
2. From `SpendSmart/backend`, run:
   ```
   psql "<EXTERNAL_DATABASE_URL>" -f schema.sql
   ```

> Note: `render.yaml` in this repo is the Aiven-oriented (web-service-only)
> blueprint. For the all-Render path, do the manual steps above instead of
> using the blueprint, or re-add a `databases:` block to `render.yaml`.

---

## Step: Plug the URL into the Android app

1. Open `SpendSmart/android/app/src/main/java/com/example/spendsmart/RetrofitClient.java`.
2. Set `BASE_URL` to your Render URL **with a trailing slash**:
   ```java
   private static final String BASE_URL = "https://spendsmart-api.onrender.com/";
   ```
3. Rebuild and run the app. It now talks to the cloud API over HTTPS.

No manifest change is required — HTTPS is allowed by default.

---

## Local development (optional)

```
cd SpendSmart/backend
cp .env.example .env          # then edit DATABASE_URL to your local Postgres
pip install -r requirements.txt
psql "postgresql://postgres:postgres@localhost:5432/spendsmart" -f schema.sql
python app.py                 # serves on http://localhost:5000
```

A `localhost` / `127.0.0.1` `DATABASE_URL` is connected without SSL; any
other host gets `sslmode=require` enforced automatically by `app.py`.

For the emulator against a local server, temporarily set `BASE_URL` to
`http://10.0.2.2:5000/` (see the commented note in `RetrofitClient.java`).

---

## Other free hosts

- **Railway** — set the same `DATABASE_URL` / `SECRET_KEY` vars; the included
  `Procfile` provides the gunicorn start command automatically.
- **Neon / Supabase** — free PostgreSQL only; like Aiven, pair them with any
  free web host and point `DATABASE_URL` at their connection string. Their
  URIs also require SSL, which `app.py` handles the same way.
